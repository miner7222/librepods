package me.kavishdevar.librepods.bluetooth

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ATTManagerTest {
    private val current = AtomicReference<AttTransport?>()
    private val manager = ATTManagerv2({ current.get() })
    private val workers = Executors.newCachedThreadPool()
    private val sockets = mutableListOf<FakeSocket>()

    /** Bluetooth L2CAP reads deliver one PDU at a time, unlike an unframed byte stream. */
    private class FakeSocket(private val stubbornReader: Boolean = false) : AttTransport {
        private val incoming = LinkedBlockingQueue<ByteArray>()
        val writes = LinkedBlockingQueue<ByteArray>()
        val closed = AtomicBoolean(false)
        val closeObserved = CountDownLatch(1)
        val writeGate = CountDownLatch(1)
        @Volatile var readerThread: Thread? = null
        @Volatile var onWrite: ((ByteArray) -> Unit)? = null
        @Volatile var blockWrites = false
        @Volatile var failWrites = false

        override val input = object : InputStream() {
            override fun read(): Int = error("Use framed reads")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                readerThread = Thread.currentThread()
                val pdu = if (stubbornReader) {
                    var next: ByteArray? = null
                    while (next == null) {
                        try { next = incoming.take() } catch (_: InterruptedException) { }
                    }
                    next
                } else incoming.take()
                if (pdu.isEmpty()) return -1
                check(pdu.size <= length)
                pdu.copyInto(buffer, offset)
                return pdu.size
            }
        }

        override val output = object : OutputStream() {
            override fun write(value: Int) = write(byteArrayOf(value.toByte()))
            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                if (closed.get() || failWrites) throw IOException("Fake socket write failed")
                val pdu = buffer.copyOfRange(offset, offset + length)
                writes.offer(pdu)
                if (blockWrites) writeGate.await()
                if (closed.get()) throw IOException("Fake socket closed during write")
                onWrite?.invoke(pdu)
            }
        }

        fun receive(vararg bytes: Int) { incoming.offer(bytes.map(Int::toByte).toByteArray()) }
        fun eof() { incoming.offer(byteArrayOf()) }
        override fun close() {
            closed.set(true)
            closeObserved.countDown()
            writeGate.countDown()
            if (!stubbornReader) eof()
        }

        fun awaitWrite(vararg expected: Int) {
            val pdu = writes.poll(2, TimeUnit.SECONDS)
            assertNotNull("No outgoing PDU", pdu)
            assertArrayEquals(expected.map(Int::toByte).toByteArray(), pdu)
        }
    }

    private fun connect(stubborn: Boolean = false): FakeSocket = FakeSocket(stubborn).also {
        sockets += it
        current.set(it)
        manager.startReader()
    }

    private fun read(handle: ATTHandles = ATTHandles.TRANSPARENCY, timeout: Long = 2_000): Future<ByteArray?> =
        workers.submit(Callable { manager.readCharacteristic(handle, timeout) })

    private fun write(handle: ATTHandles = ATTHandles.HEARING_AID, timeout: Long = 2_000): Future<*> =
        workers.submit { manager.writeCharacteristic(handle, byteArrayOf(0x55), timeout) }

    private fun assertWaiting(request: Future<*>) {
        assertThrows(TimeoutException::class.java) { request.get(50, TimeUnit.MILLISECONDS) }
    }

    /** A notification behind the injected replies proves the reader consumed them first. */
    private fun barrier(socket: FakeSocket) {
        val received = CountDownLatch(1)
        manager.setOnNotificationReceived { _, _ -> received.countDown() }
        socket.receive(0x1B, 0x18, 0, 0x77)
        assertTrue("Reader did not reach the barrier", received.await(2, TimeUnit.SECONDS))
        manager.setOnNotificationReceived(null)
    }

    @After fun cleanup() {
        manager.stopReader()
        sockets.forEach { it.close(); it.eof() }
        workers.shutdownNow()
        assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun readsForDifferentHandlesAreSerializedAndKeepTheirOwnValues() {
        val socket = connect()
        val first = read(ATTHandles.HEARING_AID)
        socket.awaitWrite(0x0A, 0x2A, 0)
        val second = read(ATTHandles.TRANSPARENCY)
        assertNull(socket.writes.poll(50, TimeUnit.MILLISECONDS))
        socket.receive(0x0B, 0x11)
        assertArrayEquals(byteArrayOf(0x11), first.get(2, TimeUnit.SECONDS))
        socket.awaitWrite(0x0A, 0x18, 0)
        assertWaiting(second)
        socket.receive(0x0B, 0x22)
        assertArrayEquals(byteArrayOf(0x22), second.get(2, TimeUnit.SECONDS))
        assertArrayEquals(byteArrayOf(0x11), manager.getCharacteristic(ATTHandles.HEARING_AID))
        assertArrayEquals(byteArrayOf(0x22), manager.getCharacteristic(ATTHandles.TRANSPARENCY))
    }

    @Test(timeout = 5_000) fun readsAndWritesShareTheOneOutstandingRequestSlot() {
        val socket = connect()
        val first = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        val second = write()
        socket.receive(0x13) // Wrong opcode for the read, even though a write is queued.
        barrier(socket)
        assertWaiting(first)
        assertNull(socket.writes.poll())
        socket.receive(0x0B, 0x11)
        assertArrayEquals(byteArrayOf(0x11), first.get(2, TimeUnit.SECONDS))
        socket.awaitWrite(0x12, 0x2A, 0, 0x55)
        assertWaiting(second)
        socket.receive(0x13)
        second.get(2, TimeUnit.SECONDS)
    }

    @Test(timeout = 5_000) fun unsolicitedRepliesAreNotSavedForAFutureRequest() {
        val socket = connect()
        socket.receive(0x0B, 0x7F)
        socket.receive(0x13)
        socket.receive(0x01, 0x0A, 0x18, 0, 0x0E)
        barrier(socket)
        val request = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        assertWaiting(request)
        socket.receive(0x0B, 0x33)
        assertArrayEquals(byteArrayOf(0x33), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun aFastResponseDuringWriteIsNotLostBeforeAwaitStarts() {
        val socket = connect()
        val notification = CountDownLatch(1)
        manager.setOnNotificationReceived { _, _ -> notification.countDown() }
        socket.onWrite = {
            socket.receive(0x0B, 0x44)
            socket.receive(0x1B, 0x18, 0, 1)
            assertTrue(notification.await(2, TimeUnit.SECONDS))
        }
        assertArrayEquals(byteArrayOf(0x44), read().get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun errorsMustMatchBothTheRequestOpcodeAndFullHandle() {
        val socket = connect()
        val request = read(ATTHandles.HEARING_AID)
        socket.awaitWrite(0x0A, 0x2A, 0)
        socket.receive(0x01, 0x12, 0x2A, 0, 0x0E) // Wrong request opcode.
        socket.receive(0x01, 0x0A, 0x18, 0, 0x0E) // Wrong low handle byte.
        socket.receive(0x01, 0x0A, 0x2A, 1, 0x0E) // Wrong high handle byte.
        socket.receive(0x01, 0x0A, 0x2A) // Truncated error.
        barrier(socket)
        assertWaiting(request)
        socket.receive(0x01, 0x0A, 0x2A, 0, 0x0E)
        assertNull(request.get(2, TimeUnit.SECONDS))
        assertFalse(manager.characteristicList.containsKey(ATTHandles.HEARING_AID))
        // A matched error completes the transaction, so the same bearer can accept another request.
        val next = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        socket.receive(0x0B, 0x66)
        assertArrayEquals(byteArrayOf(0x66), next.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun writeErrorsAndMalformedWriteResponsesCannotCompleteTheWrongRequest() {
        val socket = connect()
        val first = write()
        socket.awaitWrite(0x12, 0x2A, 0, 0x55)
        socket.receive(0x01, 0x0A, 0x2A, 0, 0x0E)
        socket.receive(0x13, 0x01) // Write Response has no payload.
        barrier(socket)
        assertWaiting(first)
        socket.receive(0x01, 0x12, 0x2A, 0, 0x0E)
        first.get(2, TimeUnit.SECONDS)
        assertFalse(socket.closed.get())
    }

    @Test(timeout = 5_000) fun notificationsAndIndicationsBypassThePendingRequest() {
        val socket = connect()
        val delivered = LinkedBlockingQueue<Pair<Byte, ByteArray>>()
        manager.setOnNotificationReceived { handle, value -> delivered.offer(handle to value) }
        socket.receive(0x1B, 0x18, 0, 0x11)
        assertEquals(0x18.toByte(), delivered.poll(2, TimeUnit.SECONDS)?.first)
        val request = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        socket.receive(0x1D, 0x2A, 0, 0x22)
        socket.awaitWrite(0x1E)
        val indication = delivered.poll(2, TimeUnit.SECONDS)
        assertEquals(0x2A.toByte(), indication?.first)
        assertArrayEquals(byteArrayOf(0x22), indication?.second)
        assertWaiting(request)
        socket.receive(0x0B, 0x33)
        assertArrayEquals(byteArrayOf(0x33), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun listenerFailuresDoNotStopRequestHandling() {
        val socket = connect()
        manager.setOnNotificationReceived { _, _ -> throw IllegalStateException("Listener failed") }
        val request = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        socket.receive(0x1B, 0x18) // Truncated notification is ignored too.
        socket.receive(0x1B, 0x18, 0, 1)
        socket.receive(0x0B, 0x11)
        assertArrayEquals(byteArrayOf(0x11), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun timeoutRetiresTheBearerSoALateReplyCannotAnswerTheNextRead() {
        val old = connect(stubborn = true)
        val timedOut = read(timeout = 100)
        old.awaitWrite(0x0A, 0x18, 0)
        assertNull(timedOut.get(2, TimeUnit.SECONDS))
        assertTrue(old.closeObserved.await(2, TimeUnit.SECONDS))
        manager.startReader() // The same timed-out socket must never be rearmed.
        assertNull(read(ATTHandles.HEARING_AID).get(2, TimeUnit.SECONDS))
        assertNull(old.writes.poll())
        val fresh = connect()
        val request = read(ATTHandles.HEARING_AID)
        fresh.awaitWrite(0x0A, 0x2A, 0)
        old.receive(0x0B, 0x7F)
        old.readerThread?.join(1_000)
        assertFalse(old.readerThread?.isAlive == true)
        assertWaiting(request)
        fresh.receive(0x0B, 0x55)
        assertArrayEquals(byteArrayOf(0x55), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun replacementCancelsOldWaitersAndOldReaderCannotFeedOrStopTheNewSession() {
        val old = connect(stubborn = true)
        val oldRequest = read()
        old.awaitWrite(0x0A, 0x18, 0)
        val fresh = connect()
        assertNull(oldRequest.get(2, TimeUnit.SECONDS))
        val request = read(ATTHandles.HEARING_AID)
        fresh.awaitWrite(0x0A, 0x2A, 0)
        old.receive(0x0B, 0x7F)
        old.readerThread?.join(1_000)
        barrier(fresh)
        assertWaiting(request)
        assertFalse(fresh.closed.get())
        fresh.receive(0x0B, 0x55)
        assertArrayEquals(byteArrayOf(0x55), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun stopClosesItsCapturedSocketAndCancelsTheWaitWithoutClosingANewPublication() {
        val old = connect()
        val request = read()
        old.awaitWrite(0x0A, 0x18, 0)
        val published = FakeSocket().also { sockets += it; current.set(it) }
        manager.stopReader()
        assertNull(request.get(2, TimeUnit.SECONDS))
        assertTrue(old.closeObserved.await(2, TimeUnit.SECONDS))
        assertFalse(published.closed.get())
        manager.startReader()
        val next = read()
        published.awaitWrite(0x0A, 0x18, 0)
        published.receive(0x0B, 0x66)
        assertArrayEquals(byteArrayOf(0x66), next.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun eofWakesThePendingRequestWithoutWaitingForItsDeadline() {
        val socket = connect()
        val request = read(timeout = 10_000)
        socket.awaitWrite(0x0A, 0x18, 0)
        socket.eof()
        assertNull(request.get(1, TimeUnit.SECONDS))
        assertTrue(socket.closeObserved.await(1, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun waitingForTheRequestSlotAlsoHasABoundedTimeout() {
        val socket = connect()
        val first = read()
        socket.awaitWrite(0x0A, 0x18, 0)
        assertNull(read(ATTHandles.HEARING_AID, timeout = 50).get(1, TimeUnit.SECONDS))
        assertFalse(socket.closed.get())
        assertNull(socket.writes.poll())
        socket.receive(0x0B, 0x44)
        assertArrayEquals(byteArrayOf(0x44), first.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun aStalledWriteIsUnblockedByTheRequestDeadlineClosingItsSocket() {
        val socket = connect()
        socket.blockWrites = true
        val request = read(timeout = 100)
        socket.awaitWrite(0x0A, 0x18, 0)
        assertNull(request.get(2, TimeUnit.SECONDS))
        assertTrue(socket.closed.get())
    }

    @Test(timeout = 5_000) fun writeFailureRetiresTheSessionAndCannotLeaveAPendingReplyBehind() {
        val socket = connect()
        socket.failWrites = true
        assertNull(read().get(2, TimeUnit.SECONDS))
        assertTrue(socket.closed.get())
        socket.failWrites = false
        assertNull(read().get(2, TimeUnit.SECONDS))
        assertNull(socket.writes.poll())
    }

    @Test(timeout = 5_000) fun disconnectAlsoClosesAPublishedSocketWhoseReaderHasNotStarted() {
        val published = FakeSocket().also { sockets += it; current.set(it) }
        manager.disconnected()
        assertTrue(published.closed.get())
    }

    @Test(timeout = 5_000) fun aNewSocketCannotUseThePreviousSessionsCharacteristicCache() {
        val old = connect()
        val first = read()
        old.awaitWrite(0x0A, 0x18, 0)
        old.receive(0x0B, 0x11)
        assertArrayEquals(byteArrayOf(0x11), first.get(2, TimeUnit.SECONDS))
        val fresh = connect()
        assertFalse(manager.characteristicList.containsKey(ATTHandles.TRANSPARENCY))
        val next = workers.submit(Callable { manager.getCharacteristic(ATTHandles.TRANSPARENCY) })
        fresh.awaitWrite(0x0A, 0x18, 0)
        fresh.receive(0x0B, 0x22)
        assertArrayEquals(byteArrayOf(0x22), next.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun cccdWritesMatchTheDescriptorHandleRatherThanTheCharacteristicHandle() {
        val socket = connect()
        val request = workers.submit { manager.enableNotification(ATTCCCDHandles.HEARING_AID) }
        socket.awaitWrite(0x12, 0x2B, 0, 1)
        socket.receive(0x01, 0x12, 0x2A, 0, 0x0E)
        barrier(socket)
        assertWaiting(request)
        socket.receive(0x01, 0x12, 0x2B, 0, 0x0E)
        request.get(2, TimeUnit.SECONDS)
        assertFalse(socket.closed.get())
    }

    @Test(timeout = 5_000) fun aTimeoutNotifiesOnceAfterClosingTheRetiredSocket() {
        val retired = LinkedBlockingQueue<Any>()
        manager.setOnSocketRetired { identity -> retired.offer(identity) }
        val socket = connect()
        val request = read(timeout = 100)
        socket.awaitWrite(0x0A, 0x18, 0)
        assertNull(request.get(2, TimeUnit.SECONDS))
        assertTrue(retired.poll(2, TimeUnit.SECONDS) === socket)
        assertTrue(socket.closed.get())
        assertNull(read(timeout = 10_000).get(1, TimeUnit.SECONDS)) // Recovery gap fails fast.
        manager.disconnected()
        assertNull(retired.poll(150, TimeUnit.MILLISECONDS))
    }

    @Test(timeout = 5_000) fun explicitDisconnectCancelsTheDeadlineWithoutRequestingRecovery() {
        val retired = LinkedBlockingQueue<Any>()
        manager.setOnSocketRetired { retired.offer(it) }
        val socket = connect()
        val request = read(timeout = 200)
        socket.awaitWrite(0x0A, 0x18, 0)
        manager.disconnected()
        assertNull(request.get(2, TimeUnit.SECONDS))
        assertTrue(socket.closed.get())
        assertNull(retired.poll(300, TimeUnit.MILLISECONDS))
    }

    @Test(timeout = 5_000) fun stopEofWriteFailureAndReplacementDoNotRequestTimeoutRecovery() {
        val retired = LinkedBlockingQueue<Any>()
        manager.setOnSocketRetired { retired.offer(it) }
        connect()
        manager.stopReader()
        val eofSocket = connect()
        val eofRequest = read()
        eofSocket.awaitWrite(0x0A, 0x18, 0)
        eofSocket.eof()
        assertNull(eofRequest.get(2, TimeUnit.SECONDS))
        val failed = connect()
        failed.failWrites = true
        assertNull(read().get(2, TimeUnit.SECONDS))
        val old = connect()
        val oldRequest = read()
        old.awaitWrite(0x0A, 0x18, 0)
        connect()
        assertNull(oldRequest.get(2, TimeUnit.SECONDS))
        assertNull(retired.poll(150, TimeUnit.MILLISECONDS))
    }

    @Test(timeout = 5_000) fun recoveryCallbackCanInstallANewSessionWithoutAnOldReaderStoppingIt() {
        val old = connect(stubborn = true)
        val fresh = FakeSocket().also { sockets += it }
        val recovered = CountDownLatch(1)
        manager.setOnSocketRetired { identity ->
            assertTrue(identity === old)
            current.set(fresh)
            manager.startReader()
            recovered.countDown()
        }
        val failed = read(timeout = 100)
        old.awaitWrite(0x0A, 0x18, 0)
        assertNull(failed.get(2, TimeUnit.SECONDS))
        assertTrue(recovered.await(2, TimeUnit.SECONDS))
        val request = read(ATTHandles.HEARING_AID)
        fresh.awaitWrite(0x0A, 0x2A, 0)
        old.receive(0x0B, 0x7F)
        old.readerThread?.join(1_000)
        fresh.receive(0x0B, 0x33)
        assertArrayEquals(byteArrayOf(0x33), request.get(2, TimeUnit.SECONDS))
        assertFalse(fresh.closed.get())
    }

    @Test(timeout = 5_000) fun anOldSocketTimeoutDoesNotRequestRecoveryForANewerPublication() {
        val retired = LinkedBlockingQueue<Any>()
        manager.setOnSocketRetired { retired.offer(it) }
        val old = connect()
        val failed = read(timeout = 100)
        old.awaitWrite(0x0A, 0x18, 0)
        val fresh = FakeSocket().also { sockets += it; current.set(it) }
        assertNull(failed.get(2, TimeUnit.SECONDS))
        assertNull(retired.poll(150, TimeUnit.MILLISECONDS))
        assertFalse(fresh.closed.get())
        manager.startReader()
        val request = read()
        fresh.awaitWrite(0x0A, 0x18, 0)
        fresh.receive(0x0B, 0x33)
        assertArrayEquals(byteArrayOf(0x33), request.get(2, TimeUnit.SECONDS))
    }

    @Test(timeout = 5_000) fun recoveryRestoresRequestedSubscriptionsWithoutReplayingTheTimedOutRead() {
        val old = connect()
        val enable = workers.submit { manager.enableNotification(ATTCCCDHandles.HEARING_AID) }
        old.awaitWrite(0x12, 0x2B, 0, 1)
        old.receive(0x13)
        enable.get(2, TimeUnit.SECONDS)
        val failed = read(timeout = 100)
        old.awaitWrite(0x0A, 0x18, 0)
        assertNull(failed.get(2, TimeUnit.SECONDS))
        val fresh = connect()
        val restore = workers.submit { manager.restoreNotifications(fresh) }
        fresh.awaitWrite(0x12, 0x2B, 0, 1)
        fresh.receive(0x13)
        restore.get(2, TimeUnit.SECONDS)
        assertNull(fresh.writes.poll())
        manager.restoreNotifications(old)
        assertNull(fresh.writes.poll())
        manager.disconnected()
        val next = connect()
        manager.restoreNotifications(next)
        assertNull(next.writes.poll())
    }
}
