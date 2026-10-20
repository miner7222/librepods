---
opcode: 0x001D
title: Device Information
description: Information about AirPods, such as model, firmware version, and serial number. This can not be requested from the accessory; it is only sent by the accessory to the host upon connection.
---

## Device information

The device information packet is sent by the accessory to the host upon connection. It contains various details about the AirPods, including model number, software version, and serial number.

Each string is UTF-8 and normally terminated by a NUL byte. In the documented envelope, consecutive NUL bytes represent empty fields; do not skip them or drop the first field.

The data is in this order:
- Name
- Model number
- Manufacturer (always "Apple Inc.")
- Serial number
- Version 1
- Version 2
- Hardware revision (?) (I have `1.0.0`)
- Updater app version (?) (I have `com.apple.accessory.updater.app.71`)
- Serial number (Left Bud)
- Serial number (Right Bud)
- Version (?) (I have `8454371`)
- A few more bytes, I don't know what they are

## Framing and parsing

The [captured packet](/docs/AAP%20Definitions.md#metadata) uses this length-prefixed envelope (offsets are zero-based):

| Offset | Size | Value / meaning |
| ------ | ---- | --------------- |
| 0 | 4 | AACP prefix `04 00 04 00` |
| 4 | 2 | Opcode `1D 00` |
| 6 | 1 | Envelope type `02` |
| 7 | 2 | Little-endian length of the nested record, including its type |
| 9 | 2 | Nested type `04 00` |
| 11 | Variable | Eleven NUL-terminated UTF-8 strings, in the order above |
| After the eleventh terminator | Variable | Opaque tail; do not interpret it as more strings |

The Android parser uses this envelope when both headers match and the nested length equals the remaining packet size (`packet.size - 9`). It preserves empty field positions on this path. Fewer than eleven fields are accepted, with missing trailing fields set to empty strings. An unterminated last field ends at the payload boundary. UTF-8 decoding accepts non-ASCII names and replaces malformed sequences with replacement characters.

When the envelope does not match, the receiver logs a warning and falls back to the legacy scan after the six-byte AACP header: skip the leading word up to NUL, skip empty separators and the first legacy record, then read the nonempty NUL-separated fields. This fallback preserves the previous interpretation of unfamiliar framing. Every scan is bounded by the received packet; only packets shorter than the six-byte header are ignored. The parser does not search arbitrary offsets for plausible ASCII strings or nested AACP headers.

## Push-only

Hosts cannot request this opcode. Opcode `0x004F` does not provide a reliable way to read serials or model numbers from the accessory.

After L2CAP connect, send the [handshake](/docs/AAP%20Definitions.md#handshake) and [host capabilities `0x004D`](/docs/host-capabilities.md). Device information can arrive in the unsolicited startup burst; keep the reader active while startup packets are retried.

## Battery report (`0x0004`)

Unsolicited battery packets may arrive before or after `0x001D`. Receiving device information does not guarantee a battery report in the same startup burst.
