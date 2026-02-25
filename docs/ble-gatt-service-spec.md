# ChromaDMX BLE GATT Service Specification

## Overview

This document defines the custom BLE GATT service used for provisioning ChromaDMX
ESP32 DMX nodes. The app discovers ESP32 nodes advertising this service, connects
via BLE, and writes Wi-Fi credentials and Art-Net configuration so the node can
join the local network and begin outputting DMX data.

## Service UUID

| Item            | UUID                                     |
|-----------------|------------------------------------------|
| **Service**     | `4368726f-6d61-444d-5800-000000000001`   |

The service UUID is derived from the ASCII hex encoding of "ChromaDMX" (`43 68 72 6f 6d 61 44 4d 58`).

## Characteristics

All characteristics belong to the ChromaDMX provisioning service listed above.

| Characteristic        | UUID                                     | Properties      | Max Length | Description |
|-----------------------|------------------------------------------|-----------------|------------|-------------|
| **Node Name**         | `4368726f-6d61-444d-5800-000000000010`   | Read, Write     | 32 bytes   | Human-readable node name (UTF-8) |
| **Wi-Fi SSID**        | `4368726f-6d61-444d-5800-000000000011`   | Read, Write     | 32 bytes   | Wi-Fi network SSID (UTF-8) |
| **Wi-Fi Password**    | `4368726f-6d61-444d-5800-000000000012`   | Write           | 64 bytes   | Wi-Fi network password (UTF-8, write-only for security) |
| **Universe**          | `4368726f-6d61-444d-5800-000000000013`   | Read, Write     | 2 bytes    | Art-Net universe number (uint16 LE, 0-32767) |
| **DMX Start Address** | `4368726f-6d61-444d-5800-000000000014`   | Read, Write     | 2 bytes    | DMX start address within universe (uint16 LE, 1-512) |
| **Provisioned Flag**  | `4368726f-6d61-444d-5800-000000000015`   | Read, Notify    | 1 byte     | 0x00 = not provisioned, 0x01 = provisioned, 0x02 = provisioning in progress |
| **Firmware Version**  | `4368726f-6d61-444d-5800-000000000016`   | Read            | 16 bytes   | Firmware version string (UTF-8, e.g. "1.0.0") |
| **Command**           | `4368726f-6d61-444d-5800-000000000020`   | Write           | 1 byte     | Command byte (see below) |

## Command Characteristic Values

| Value  | Command          | Description |
|--------|------------------|-------------|
| `0x01` | APPLY_CONFIG     | Apply the written configuration and connect to Wi-Fi |
| `0x02` | REBOOT           | Reboot the node |
| `0x03` | FACTORY_RESET    | Clear all configuration and reset to defaults |
| `0x04` | ENTER_DFU        | Enter Device Firmware Update mode |

## Provisioned Flag Values

| Value  | State                  | Description |
|--------|------------------------|-------------|
| `0x00` | NOT_PROVISIONED        | No valid Wi-Fi + Art-Net config stored |
| `0x01` | PROVISIONED            | Config stored and applied; node operational |
| `0x02` | PROVISIONING           | Configuration write in progress |
| `0x03` | WIFI_CONNECTING        | Node is attempting Wi-Fi connection |
| `0x04` | WIFI_CONNECTED         | Wi-Fi connected, Art-Net starting |
| `0xFF` | ERROR                  | Configuration error (check node logs) |

## Provisioning Workflow

1. **Scan**: App scans for BLE peripherals advertising the ChromaDMX service UUID.
2. **Connect**: App connects to the selected node and discovers services.
3. **Read Current Config**: App reads Node Name, Wi-Fi SSID, Universe, DMX Start Address,
   Firmware Version, and Provisioned Flag to display current state.
4. **Write New Config**: App writes new values to each writable characteristic:
   - Node Name
   - Wi-Fi SSID
   - Wi-Fi Password
   - Universe
   - DMX Start Address
5. **Apply**: App writes `0x01` (APPLY_CONFIG) to the Command characteristic.
6. **Monitor**: App subscribes to Provisioned Flag notifications to track progress:
   `PROVISIONING` -> `WIFI_CONNECTING` -> `WIFI_CONNECTED` -> `PROVISIONED`
7. **Verify**: App reads back configuration to confirm values were stored correctly.
8. **Disconnect**: App disconnects BLE. The node is now operational on Wi-Fi/Art-Net.

## Data Encoding

- **Strings** (Node Name, SSID, Password, Firmware Version): UTF-8 encoded, null-terminated
- **Integers** (Universe, DMX Start Address): unsigned 16-bit little-endian (uint16 LE)
- **Flags/Commands** (Provisioned Flag, Command): single unsigned byte (uint8)

## Advertising Data

The ESP32 node advertises with:
- **Service UUID**: `4368726f-6d61-444d-5800-000000000001`
- **Local Name**: Current node name (or "ChromaDMX-XXXX" where XXXX = last 4 hex of MAC)
- **Manufacturer Data** (optional):
  - Company ID: `0xFFFF` (reserved for development)
  - Data: `[provisioned_flag, firmware_major, firmware_minor]`

## Security Considerations

- Wi-Fi Password is **write-only** -- it cannot be read back via BLE
- The GATT service should only be advertised during a configurable window after
  power-on or when the user presses a physical button on the node
- For production deployments, consider BLE pairing with MITM protection
- The provisioning flag notification allows the app to confirm Wi-Fi connectivity
  before disconnecting BLE

## MTU Considerations

- Default BLE MTU is 23 bytes (20 bytes payload after ATT overhead)
- Negotiate higher MTU (247+) on connection for efficient string transfers
- All characteristics are designed to fit within standard MTU limits
