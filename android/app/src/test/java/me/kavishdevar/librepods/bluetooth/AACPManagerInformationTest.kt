package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AACPManagerInformationTest {
    private val manager = AACPManager()
    private val fields = listOf(
        "AirPods Pro", "A3048", "Apple Inc.", "QXNRHHYXP6",
        "61.1868040002000000.2713", "61.1868040002000000.2713", "1.0.0",
        "com.apple.accessory.updater.app.71", "HY9LT2EF26JY", "H3PLWHDJ26K0", "6357536",
    )

    private fun packet(values: List<String> = fields, tail: ByteArray = byteArrayOf()): ByteArray {
        val payload = byteArrayOf(0x04, 0x00) +
            values.joinToString(separator = "\u0000", postfix = "\u0000").encodeToByteArray() + tail
        return byteArrayOf(
            0x04, 0x00, 0x04, 0x00, 0x1D, 0x00, 0x02,
            payload.size.toByte(), (payload.size ushr 8).toByte(),
        ) + payload
    }

    private fun withLength(packet: ByteArray, length: Int): ByteArray = packet.copyOf().apply {
        this[7] = length.toByte()
        this[8] = (length ushr 8).toByte()
    }

    @Test fun wellFormedPacketKeepsTheFirstFieldAndAllIdentificationFields() {
        val info = manager.parseInformationPacket(packet())
        assertEquals(fields, listOf(
            info.name, info.modelNumber, info.manufacturer, info.serialNumber,
            info.version1, info.version2, info.hardwareRevision, info.updaterIdentifier,
            info.leftSerialNumber, info.rightSerialNumber, info.version3,
        ))
    }

    @Test fun documentedCaptureParsesWithoutTreatingTheOpaqueTailAsStrings() {
        val hex = "040004001d0002d5000400416972506f64732050726f004133303438004170706c6520496e632e0051584e524848595850360036312e313836383034303030323030303030302e323731330036312e313836383034303030323030303030302e3237313300312e302e3000636f6d2e6170706c652e6163636573736f72792e757064617465722e6170702e3731004859394c5432454632364a59004833504c5748444a32364b3000363335373533360089312a6567a5400f84a3ca234947efd40b90d78436ae5946748d70273e66066a2589300035333935303630363400"
        val capture = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertEquals(manager.parseInformationPacket(packet()), manager.parseInformationPacket(capture))
    }

    @Test fun onlyPrefixesShorterThanTheHeaderAreRejected() {
        val complete = packet()
        for (size in 0 until complete.size) {
            if (size < 6) {
                assertThrows("prefix length $size", IllegalArgumentException::class.java) {
                    manager.parseInformationPacket(complete.copyOf(size))
                }
            } else {
                assertNotNull("prefix length $size", manager.parseInformationPacket(complete.copyOf(size)))
            }
        }
    }

    @Test fun wrongNestedLengthsUseTheLegacyScan() {
        val complete = packet()
        for (length in listOf(0, 1, complete.size - 10, complete.size - 8)) {
            assertEquals(
                "nested length $length", manager.parseInformationPacket(complete),
                manager.parseInformationPacket(withLength(complete, length)),
            )
        }
        assertNotNull(manager.parseInformationPacket(withLength(complete, 0xFFFF)))
    }

    @Test fun truncatedFieldsAreRetainedWhenTheEnvelopeLengthMatches() {
        val complete = packet()
        for (size in 11 until complete.size) {
            val truncated = withLength(complete.copyOf(size), size - 9)
            assertNotNull("field payload length ${size - 11}", manager.parseInformationPacket(truncated))
        }
        val unterminated = withLength(complete.copyOf(complete.size - 1), complete.size - 10)
        assertEquals(manager.parseInformationPacket(complete), manager.parseInformationPacket(unterminated))
        assertEquals(
            manager.parseInformationPacket(complete),
            manager.parseInformationPacket(unterminated.copyOf().apply { this[6] = 0x03 }),
        )
    }

    @Test fun fallbackLayoutUsesTheLegacyLeadingWordAndRecordScan() {
        val legacyPacket = byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x1D, 0x00, 0x03, 0x00, 0x04, 0x00) +
            fields.joinToString(separator = "\u0000", postfix = "\u0000").encodeToByteArray()
        assertEquals(manager.parseInformationPacket(packet()), manager.parseInformationPacket(legacyPacket))
        assertEquals(
            manager.parseInformationPacket(packet()),
            manager.parseInformationPacket(packet().apply { this[6] = 0x03 }),
        )
    }

    @Test fun nineFieldsKeepModelDetectionAndDefaultTheMissingTrailingFields() {
        val complete = packet(fields.take(9))
        for (input in listOf(complete, complete.copyOf().apply { this[6] = 0x03 })) {
            val info = manager.parseInformationPacket(input)
            assertEquals(fields[0], info.name)
            assertEquals(fields[1], info.modelNumber)
            assertEquals(fields[8], info.leftSerialNumber)
            assertEquals("", info.rightSerialNumber)
            assertEquals("", info.version3)
        }
    }

    @Test fun headerOnlyPacketReturnsEmptyInformation() {
        val info = manager.parseInformationPacket(packet().copyOf(6))
        assertEquals("", info.name)
        assertEquals("", info.modelNumber)
        assertEquals("", info.version3)
    }

    @Test fun utf8NameAndMultiByteNestedLengthAreSupported() {
        val name = "지수의 耳机 🎧 é".repeat(25)
        val info = manager.parseInformationPacket(packet(listOf(name) + fields.drop(1)))
        assertEquals(name, info.name)
        assertEquals(fields[1], info.modelNumber)
        assertEquals(fields[10], info.version3)
    }

    @Test fun emptyFieldsDoNotShiftSubsequentFields() {
        val values = fields.toMutableList().apply {
            this[0] = ""
            this[4] = ""
            this[8] = ""
        }
        val info = manager.parseInformationPacket(packet(values))
        assertEquals("", info.name)
        assertEquals(fields[1], info.modelNumber)
        assertEquals("", info.version1)
        assertEquals(fields[5], info.version2)
        assertEquals("", info.leftSerialNumber)
        assertEquals(fields[9], info.rightSerialNumber)
    }

    @Test fun unexpectedFramingFallsBackWithoutThrowing() {
        for (offset in listOf(0, 1, 2, 3, 4, 5, 6, 9, 10)) {
            val invalid = packet().apply { this[offset] = (this[offset].toInt() xor 1).toByte() }
            assertNotNull("header offset $offset", manager.parseInformationPacket(invalid))
        }
    }

    @Test fun malformedUtf8ProducesReplacementCharactersAndKeepsModelDetection() {
        val invalid = packet().apply {
            this[11] = 0xC3.toByte()
            this[12] = 0x28
        }
        for (input in listOf(invalid, invalid.copyOf().apply { this[6] = 0x03 })) {
            val info = manager.parseInformationPacket(input)
            assertEquals("\uFFFD(" + fields[0].drop(2), info.name)
            assertEquals(fields[1], info.modelNumber)
        }
    }

    @Test fun opaqueTailDoesNotNeedToBeUtf8OrNullTerminated() {
        assertEquals(
            manager.parseInformationPacket(packet()),
            manager.parseInformationPacket(packet(tail = byteArrayOf(0xFF.toByte(), 0x00, 0xC3.toByte()))),
        )
    }
}
