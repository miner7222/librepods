/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

private const val TAG = "ATTManager"
private val requestTimeouts = ScheduledThreadPoolExecutor(1) { task ->
    Thread(task, "ATT-Timeout").apply { isDaemon = true }
}.apply {
    removeOnCancelPolicy = true
    setKeepAliveTime(1, TimeUnit.SECONDS)
    allowCoreThreadTimeOut(true)
}

enum class ATTHandles(val value: Int) {
    TRANSPARENCY(0x18),
    LOUD_SOUND_REDUCTION(0x1B),
    HEARING_AID(0x2A)
}

enum class ATTCCCDHandles(val value: Int) {
    TRANSPARENCY(ATTHandles.TRANSPARENCY.value + 1),
    //    LOUD_SOUND_REDUCTION(ATTHandles.LOUD_SOUND_REDUCTION.value + 1), // doesn't work
    HEARING_AID(ATTHandles.HEARING_AID.value + 1)
}

/** Stream seam for exercising the real reader/request code without an Android BluetoothSocket. */
internal interface AttTransport {
    val identity: Any get() = this
    val input: InputStream
    val output: OutputStream
    fun close()
}

private class BluetoothAttTransport(private val socket: BluetoothSocket) : AttTransport {
    override val identity: Any get() = socket
    override val input: InputStream get() = socket.inputStream
    override val output: OutputStream get() = socket.outputStream
    override fun close() = socket.close()
}

class ATTManagerv2 internal constructor(
    private val socketProvider: () -> AttTransport?,
    private val logger: (Int, String, Throwable?) -> Unit = { _, _, _ -> }
) {
    constructor() : this(
        { BluetoothConnectionManager.attSocket?.let(::BluetoothAttTransport) },
        { level, message, error ->
            Log.println(level, TAG, message + (error?.let { "\n${Log.getStackTraceString(it)}" } ?: ""))
        }
    )

    val characteristicList: MutableMap<ATTHandles, ByteArray> = ConcurrentHashMap()
    private val requestLock = ReentrantLock(true)
    private val latestWrites = ConcurrentHashMap<Int, Any>()
    private val readerLock = Any()

    private class Pending(val requestOpcode: Byte, val responseOpcode: Byte, val handle: Int) {
        val ready = CountDownLatch(1)
        @Volatile var response: ByteArray? = null
        private var completed = false

        @Synchronized fun complete(data: ByteArray?) {
            if (completed) return
            completed = true
            response = data
            ready.countDown()
        }

        fun matches(data: ByteArray): Boolean = when (data.firstOrNull()) {
            responseOpcode -> responseOpcode != 0x13.toByte() || data.size == 1
            0x01.toByte() -> data.size == 5 && data[1] == requestOpcode &&
                ((data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)) == handle
            else -> false
        }
    }

    private class ReaderSession(val socket: AttTransport) {
        val running = AtomicBoolean(true)
        val pending = AtomicReference<Pending?>()
        var thread: Thread? = null
    }

    private data class Reply(val session: ReaderSession, val data: ByteArray)
    @Volatile private var activeReader: ReaderSession? = null
    private var closedSocketIdentity: Any? = null

    @Volatile
    private var onNotificationReceived: ((handle: Byte, value: ByteArray) -> Unit)? = null
    @Volatile
    private var onSocketRetired: ((socketIdentity: Any) -> Unit)? = null
    private val notificationRequests = mutableSetOf<ATTCCCDHandles>()

    fun setOnSocketRetired(listener: ((socketIdentity: Any) -> Unit)?) {
        onSocketRetired = listener
    }

    fun startReader() {
        val old = synchronized(readerLock) {
            val socket = socketProvider() ?: return
            if (activeReader?.socket?.identity === socket.identity || closedSocketIdentity === socket.identity) return
            val previous = activeReader
            val session = ReaderSession(socket)
            characteristicList.clear()
            activeReader = session
            session.thread = Thread {
                try {
                    runReaderLoop(session)
                } catch (e: Exception) {
                    logger(Log.ERROR, "reader thread crashed: ${e.message}", e)
                } finally {
                    retire(session, recover = true)
                    logger(Log.DEBUG, "reader thread stopped", null)
                }
            }.also { it.name = "ATT-Reader"; it.isDaemon = true; it.start() }
            previous
        }
        old?.let { retire(it) }
        logger(Log.DEBUG, "reader started", null)
    }

    fun stopReader() {
        val session = synchronized(readerLock) {
            val previous = activeReader ?: return
            closedSocketIdentity = previous.socket.identity
            activeReader = null
            previous
        }
        retire(session)
    }

    private fun isCurrent(session: ReaderSession): Boolean = activeReader === session &&
        session.running.get() && socketProvider()?.identity === session.socket.identity

    private fun retire(
        session: ReaderSession, expectedPending: Pending? = null,
        timedOut: Boolean = false, recover: Boolean = timedOut
    ) {
        synchronized(session) {
            // Reply delivery and deadline resolution compete for this exact slot.
            if (expectedPending != null && !session.pending.compareAndSet(expectedPending, null)) return
            if (!session.running.getAndSet(false)) return
            (expectedPending ?: session.pending.getAndSet(null))?.complete(null)
        }
        if (timedOut) logger(Log.WARN, "ATT request timed out; retiring its socket", null)
        synchronized(readerLock) {
            if (activeReader === session) closedSocketIdentity = session.socket.identity
        }
        session.thread?.interrupt()
        try {
            session.socket.close()
        } catch (e: Exception) {
            logger(Log.WARN, "error closing reader socket: ${e.message}", e)
        }
        val listener = synchronized(readerLock) {
            if (recover && activeReader === session && socketProvider()?.identity === session.socket.identity) {
                onSocketRetired
            } else null
        }
        try {
            listener?.invoke(session.socket.identity)
        } catch (e: Exception) {
            logger(Log.ERROR, "onSocketRetired threw: ${e.message}", e)
        }
    }

    fun setOnNotificationReceived(listener: ((handle: Byte, value: ByteArray) -> Unit)?) {
        onNotificationReceived = listener
    }

    fun enableNotification(handle: ATTCCCDHandles) {
        synchronized(readerLock) { notificationRequests += handle }
        writeCharacteristic(handle.value.toByte(), byteArrayOf(0x01))
    }

    internal fun restoreNotifications(socketIdentity: Any) {
        val handles = synchronized(readerLock) { notificationRequests.toList() }
        for (handle in handles) {
            try {
                sendRequest(
                    byteArrayOf(0x12, handle.value.toByte(), 0x00, 0x01), 0x13, 2000,
                    expectedSocket = socketIdentity
                )
            } catch (e: Exception) {
                logger(Log.ERROR, "error restoring notifications: ${e.message}", e)
            }
        }
    }

    fun getCharacteristic(handle: ATTHandles): ByteArray? {
        val storedValue = synchronized(readerLock) {
            val session = activeReader ?: return null
            if (!isCurrent(session)) return null
            characteristicList[handle]
        }
        return if (storedValue?.isNotEmpty() != true) {
            readCharacteristic(handle)
        } else storedValue
    }

    fun readCharacteristic(handle: ATTHandles, timeoutMillis: Long = 2000): ByteArray? {
        try {
            val pdu = byteArrayOf(0x0A, handle.value.toByte(), 0x00)
            val reply = sendRequest(pdu, 0x0B, timeoutMillis, handle) ?: return null
            return synchronized(readerLock) {
                if (!isCurrent(reply.session)) return@synchronized null
                reply.data.copyOfRange(1, reply.data.size)
            }
        } catch (e: Exception) {
            logger(Log.ERROR, "error reading characteristic: ${e.message}", e)
            return null
        }
    }

    fun writeCharacteristic(handle: ATTHandles, data: ByteArray, timeoutMillis: Long = 2000) {
        writeCharacteristic(handle.value.toByte(), data, timeoutMillis, cacheHandle = handle)
    }

    fun writeCharacteristic(handle: Byte, data: ByteArray, timeoutMillis: Long = 2000) {
        writeCharacteristic(handle, data, timeoutMillis, cacheHandle = null)
    }

    private fun writeCharacteristic(handle: Byte, data: ByteArray, timeoutMillis: Long, cacheHandle: ATTHandles?) {
        val key = handle.toInt() and 0xFF
        val token = Any()
        val pdu = byteArrayOf(0x12, handle, 0x00) + data // 0x00 for LE; copy caller data
        synchronized(readerLock) {
            latestWrites[key] = token
            if (cacheHandle != null && activeReader?.let(::isCurrent) == true) {
                characteristicList[cacheHandle] = pdu.copyOfRange(3, pdu.size)
            }
        }
        try {
            sendRequest(pdu, 0x13, timeoutMillis, cacheHandle, writeToken = token)
        } catch (e: Exception) {
            logger(Log.ERROR, "error writing characteristic: ${e.message}", e)
        } finally {
            latestWrites.remove(key, token)
        }
    }

    fun disconnected() {
        val (session, published) = synchronized(readerLock) {
            val session = activeReader
            val published = socketProvider()
            activeReader = null
            closedSocketIdentity = published?.identity ?: session?.socket?.identity ?: closedSocketIdentity
            characteristicList.clear()
            notificationRequests.clear()
            session to published
        }
        session?.let { retire(it) }
        // Publication precedes reader startup; teardown must also cover failure in that gap.
        if (published != null && published.identity !== session?.socket?.identity) {
            try {
                published.close()
            } catch (e: Exception) {
                logger(Log.WARN, "error closing socket: ${e.message}", e)
            }
        }
        logger(Log.DEBUG, "ATT disconnected", null)
    }

    private fun runReaderLoop(session: ReaderSession) {
        val input = session.socket.input
        val buffer = ByteArray(512)

        while (isCurrent(session)) {
            try {
                val len = input.read(buffer)
                if (len == -1) {
                    logger(Log.WARN, "ATT input stream ended", null)
                    break
                }
                if (!isCurrent(session)) break
                val data = buffer.copyOfRange(0, len)
                if (data.isEmpty()) continue

                val opcode = data[0]
                logger(Log.DEBUG, "pdu received ${data.joinToString(" ") { String.format("%02X", it) }}", null)

                if (opcode == 0x1B.toByte() || opcode == 0x1D.toByte()) {
                    if (data.size >= 3) {
                        if (opcode == 0x1D.toByte()) {
                            val output = session.socket.output
                            synchronized(output) {
                                if (isCurrent(session)) {
                                    output.write(byteArrayOf(0x1E))
                                    output.flush()
                                }
                            }
                        }
                        val handle = data[1]
                        val value = if (data.size > 3) data.copyOfRange(3, data.size) else ByteArray(0)
                        try {
                            if (isCurrent(session)) onNotificationReceived?.invoke(handle, value)
                        } catch (t: Throwable) {
                            logger(Log.ERROR, "onNotificationReceived threw: ${t.message}", t)
                        }
                    } else {
                        logger(Log.WARN, "notification PDU too short", null)
                    }
                } else {
                    val pending = session.pending.get()
                    synchronized(session) {
                        if (pending != null && pending.matches(data) &&
                            session.pending.compareAndSet(pending, null)) pending.complete(data)
                    }
                }
            } catch (e: Exception) {
                logger(Log.ERROR, "error in reader loop: ${e.message}", e)
                break
            }
        }
    }

    private fun sendRequest(
        pdu: ByteArray, responseOpcode: Byte, timeoutMillis: Long, cacheHandle: ATTHandles? = null,
        expectedSocket: Any? = null, writeToken: Any? = null
    ): Reply? {
        if (timeoutMillis <= 0) return null
        val session = activeReader?.takeIf(::isCurrent) ?: return null
        if (expectedSocket != null && session.socket.identity !== expectedSocket) return null
        val acquired = try {
            requestLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        }
        if (!acquired) return null
        val pending = Pending(pdu[0], responseOpcode, (pdu[1].toInt() and 0xFF) or ((pdu[2].toInt() and 0xFF) shl 8))
        var timeout: ScheduledFuture<*>? = null
        try {
            synchronized(session) {
                if (!isCurrent(session) ||
                    (writeToken != null && latestWrites[pending.handle] !== writeToken)) return null
                check(session.pending.compareAndSet(null, pending)) { "An ATT request is already pending" }
            }
            val output = session.socket.output
            synchronized(output) {
                if (!isCurrent(session)) return null
                output.write(pdu)
                output.flush()
            }
            // Waiting for the fair request lock has its own budget. Only a reply
            // timeout after the PDU was written makes this bearer's replies ambiguous.
            timeout = requestTimeouts.schedule(
                { retire(session, pending, timedOut = true) }, timeoutMillis, TimeUnit.MILLISECONDS
            )
            if (!pending.ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                retire(session, pending, timedOut = true)
                // If delivery won the slot, complete() ran under the same session lock.
                synchronized(session) { }
            }
            val response = pending.response ?: return null
            return synchronized(readerLock) {
                if (!isCurrent(session) || response[0] == 0x01.toByte()) return@synchronized null
                // Commit before releasing the request slot, so an older read cannot overwrite a newer one.
                if (cacheHandle != null) {
                    if (writeToken == null) {
                        characteristicList[cacheHandle] = response.copyOfRange(1, response.size)
                    } else if (latestWrites[pending.handle] === writeToken) {
                        characteristicList[cacheHandle] = pdu.copyOfRange(3, pdu.size)
                    }
                }
                Reply(session, response)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            retire(session)
            return null
        } catch (e: Exception) {
            retire(session, recover = true)
            throw e
        } finally {
            timeout?.cancel(false)
            session.pending.compareAndSet(pending, null)
            requestLock.unlock()
        }
    }
}
