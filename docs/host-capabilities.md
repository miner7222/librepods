---
opcode: 0x004D
title: Host capabilities (feature flags)
description: Sent by the host to the accessory after the initial L2CAP handshake to advertise supported features.
---

## Overview

After the [handshake](/docs/AAP%20Definitions.md#handshake) on PSM `0x1001`, LibrePods sends **`0x004D`** before [requesting notifications](/docs/AAP%20Definitions.md#requesting-notifications). The accessory may send unsolicited startup packets, including paired-device metadata `0x002B` and [device information `0x001D`](/docs/device-info.md).

This opcode advertises host capabilities. It is not a poll for device information; `0x001D` remains push-only.

## Packet shape

The packet uses the AACP prefix `04 00 04 00`, little-endian opcode `4D 00`, then feature bytes.

### Observed on macOS (PacketLogger)

The [AirPods Pro 2 capture](/docs/AAP%20Definitions.md#setting-specific-features-for-airpods-pro-2) contains:

```plaintext
04 00 04 00 4D 00 FF 00 00 00 00 00 00 00
```

### LibrePods (Android)

`AACPManager.createSetFeatureFlagsPacket()` supplies the opcode and feature bytes; `sendDataPacket()` adds the AACP prefix. The complete packet is:

```plaintext
04 00 04 00 4D 00 D7 00 00 00 00 00 00 00
```

The meaning of each capability bit is not fully documented. Startup notifications can vary with accessory firmware.

## Connection order and receive handling

1. Open L2CAP to PSM `0x1001` on a bonded device.
2. Send the handshake, `0x004D` host capabilities, then `0x000F` notification registration.
3. Process inbound packets continuously while startup retries run concurrently. `0x001D` may already be queued before notification registration completes.

LibrePods sends the initial sequence without added sleeps. Its existing retry and handshake-recovery logic handles retries independently of the reader; no fixed wait between these packets is required by this implementation.

Do not drain or discard the socket receive queue during startup: it may contain device information. Do not rely on `0x004F` to fetch serials or model data.

## Another opcode

LibrePods also sends `04 00 04 00 29 00 00 FF FF FF FF FF FF FF` during startup retries. The code associates it with enabling EQ writes. Its precise meaning and relationship to `0x004D` remain uncertain; it does not replace the host-capabilities packet.

## Related opcodes

| Opcode | Direction | Notes |
| ------ | --------- | ----- |
| `0x0029` | Host → accessory | Undocumented startup packet; see above. |
| `0x002B` | Accessory → host | Paired-device metadata seen during startup. |
| `0x001D` | Accessory → host | [Device information](/docs/device-info.md). |
