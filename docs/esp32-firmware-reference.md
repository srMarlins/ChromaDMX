# ChromaDMX ESP32 Reference Firmware

## Overview

This document provides a reference Arduino/PlatformIO sketch outline for an ESP32-based
DMX node that supports BLE provisioning from the ChromaDMX app. The firmware:

1. Advertises a custom BLE GATT service for provisioning
2. Accepts Wi-Fi credentials and Art-Net configuration via BLE
3. Connects to Wi-Fi and starts an Art-Net node
4. Outputs DMX data received from the Art-Net network

## Hardware Requirements

- ESP32 (any variant with BLE support: ESP32, ESP32-S3, ESP32-C3)
- RS-485 transceiver (e.g., MAX485) for DMX output
- 3.3V power supply
- Optional: status LED, provisioning button

## PlatformIO Configuration

```ini
; platformio.ini
[env:esp32]
platform = espressif32
board = esp32dev
framework = arduino
lib_deps =
    ESP32 BLE Arduino
    WiFi
    ArtnetWifi
    esp_dmx
monitor_speed = 115200
```

## Firmware Sketch Outline

```cpp
// main.cpp - ChromaDMX ESP32 Node Reference Firmware
//
// This is a REFERENCE OUTLINE, not production-ready code.
// It demonstrates the structure and flow for BLE provisioning + Art-Net DMX output.

#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <WiFi.h>
#include <ArtnetWifi.h>
#include <Preferences.h>

// ============================================================
// GATT Service & Characteristic UUIDs (match app-side spec)
// ============================================================

#define SERVICE_UUID             "4368726f-6d61-444d-5800-000000000001"
#define CHAR_NODE_NAME_UUID      "4368726f-6d61-444d-5800-000000000010"
#define CHAR_WIFI_SSID_UUID      "4368726f-6d61-444d-5800-000000000011"
#define CHAR_WIFI_PASSWORD_UUID  "4368726f-6d61-444d-5800-000000000012"
#define CHAR_UNIVERSE_UUID       "4368726f-6d61-444d-5800-000000000013"
#define CHAR_DMX_START_ADDR_UUID "4368726f-6d61-444d-5800-000000000014"
#define CHAR_PROVISIONED_UUID    "4368726f-6d61-444d-5800-000000000015"
#define CHAR_FIRMWARE_VER_UUID   "4368726f-6d61-444d-5800-000000000016"
#define CHAR_COMMAND_UUID        "4368726f-6d61-444d-5800-000000000020"

#define FIRMWARE_VERSION "1.0.0"

// ============================================================
// Provisioning Status Values
// ============================================================

#define STATUS_NOT_PROVISIONED  0x00
#define STATUS_PROVISIONED      0x01
#define STATUS_PROVISIONING     0x02
#define STATUS_WIFI_CONNECTING  0x03
#define STATUS_WIFI_CONNECTED   0x04
#define STATUS_ERROR            0xFF

// ============================================================
// Command Values
// ============================================================

#define CMD_APPLY_CONFIG   0x01
#define CMD_REBOOT         0x02
#define CMD_FACTORY_RESET  0x03
#define CMD_ENTER_DFU      0x04

// ============================================================
// Global State
// ============================================================

Preferences prefs;
ArtnetWifi artnet;

// Configuration stored in NVS
String nodeName = "ChromaDMX-Node";
String wifiSsid = "";
String wifiPassword = "";
uint16_t universe = 0;
uint16_t dmxStartAddress = 1;
uint8_t provisionedFlag = STATUS_NOT_PROVISIONED;

// BLE objects
BLEServer* bleServer = nullptr;
BLECharacteristic* charNodeName = nullptr;
BLECharacteristic* charWifiSsid = nullptr;
BLECharacteristic* charWifiPassword = nullptr;
BLECharacteristic* charUniverse = nullptr;
BLECharacteristic* charDmxStartAddr = nullptr;
BLECharacteristic* charProvisioned = nullptr;
BLECharacteristic* charFirmwareVer = nullptr;
BLECharacteristic* charCommand = nullptr;

bool deviceConnected = false;
bool bleAdvertising = true;

// DMX output buffer (512 channels)
uint8_t dmxData[512];

// ============================================================
// NVS (Non-Volatile Storage) Helpers
// ============================================================

void loadConfig() {
    prefs.begin("chromadmx", true); // read-only
    nodeName = prefs.getString("nodeName", "ChromaDMX-Node");
    wifiSsid = prefs.getString("wifiSsid", "");
    wifiPassword = prefs.getString("wifiPass", "");
    universe = prefs.getUShort("universe", 0);
    dmxStartAddress = prefs.getUShort("dmxStart", 1);
    provisionedFlag = prefs.getUChar("provisioned", STATUS_NOT_PROVISIONED);
    prefs.end();
}

void saveConfig() {
    prefs.begin("chromadmx", false); // read-write
    prefs.putString("nodeName", nodeName);
    prefs.putString("wifiSsid", wifiSsid);
    prefs.putString("wifiPass", wifiPassword);
    prefs.putUShort("universe", universe);
    prefs.putUShort("dmxStart", dmxStartAddress);
    prefs.putUChar("provisioned", provisionedFlag);
    prefs.end();
}

void factoryReset() {
    prefs.begin("chromadmx", false);
    prefs.clear();
    prefs.end();
    loadConfig();
}

// ============================================================
// BLE Callbacks
// ============================================================

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* server) override {
        deviceConnected = true;
        Serial.println("BLE client connected");
    }

    void onDisconnect(BLEServer* server) override {
        deviceConnected = false;
        Serial.println("BLE client disconnected");
        // Resume advertising
        server->getAdvertising()->start();
    }
};

class ConfigWriteCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) override {
        String uuid = characteristic->getUUID().toString();
        String value = characteristic->getValue();

        if (uuid == CHAR_NODE_NAME_UUID) {
            nodeName = value;
        } else if (uuid == CHAR_WIFI_SSID_UUID) {
            wifiSsid = value;
        } else if (uuid == CHAR_WIFI_PASSWORD_UUID) {
            wifiPassword = value;
        } else if (uuid == CHAR_UNIVERSE_UUID) {
            if (value.length() >= 2) {
                universe = (uint16_t)value[0] | ((uint16_t)value[1] << 8);
            }
        } else if (uuid == CHAR_DMX_START_ADDR_UUID) {
            if (value.length() >= 2) {
                dmxStartAddress = (uint16_t)value[0] | ((uint16_t)value[1] << 8);
            }
        }
    }
};

class CommandCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) override {
        String value = characteristic->getValue();
        if (value.length() < 1) return;

        uint8_t cmd = value[0];
        switch (cmd) {
            case CMD_APPLY_CONFIG:
                Serial.println("CMD: Apply config");
                applyConfig();
                break;
            case CMD_REBOOT:
                Serial.println("CMD: Reboot");
                ESP.restart();
                break;
            case CMD_FACTORY_RESET:
                Serial.println("CMD: Factory reset");
                factoryReset();
                updateProvisionedFlag(STATUS_NOT_PROVISIONED);
                break;
            case CMD_ENTER_DFU:
                Serial.println("CMD: Enter DFU (not implemented)");
                // TODO: Enter OTA update mode
                break;
        }
    }
};

// ============================================================
// Provisioned Flag Notification
// ============================================================

void updateProvisionedFlag(uint8_t status) {
    provisionedFlag = status;
    charProvisioned->setValue(&provisionedFlag, 1);
    charProvisioned->notify();
}

// ============================================================
// Wi-Fi + Art-Net Setup
// ============================================================

void applyConfig() {
    updateProvisionedFlag(STATUS_PROVISIONING);
    saveConfig();

    // Connect to Wi-Fi
    updateProvisionedFlag(STATUS_WIFI_CONNECTING);
    WiFi.begin(wifiSsid.c_str(), wifiPassword.c_str());

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 30) {
        delay(500);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nWi-Fi connected: " + WiFi.localIP().toString());
        updateProvisionedFlag(STATUS_WIFI_CONNECTED);

        // Start Art-Net
        startArtNet();

        updateProvisionedFlag(STATUS_PROVISIONED);
        saveConfig();
    } else {
        Serial.println("\nWi-Fi connection failed");
        updateProvisionedFlag(STATUS_ERROR);
    }
}

// ============================================================
// Art-Net Node
// ============================================================

void onArtNetFrame(uint16_t universeRecv, uint16_t length, uint8_t sequence,
                   uint8_t* data) {
    if (universeRecv != universe) return;

    // Copy received DMX data starting at our start address
    uint16_t startIdx = dmxStartAddress - 1; // DMX is 1-indexed
    for (uint16_t i = 0; i < length && (startIdx + i) < 512; i++) {
        dmxData[startIdx + i] = data[i];
    }

    // TODO: Output dmxData to RS-485 via UART/DMA
    // For reference: use the esp_dmx library or manual UART writes
    outputDmx();
}

void startArtNet() {
    artnet.begin();
    artnet.setArtDmxCallback(onArtNetFrame);
    Serial.printf("Art-Net node started on universe %d, start address %d\n",
                  universe, dmxStartAddress);
}

void outputDmx() {
    // TODO: Implement actual DMX output via RS-485 transceiver
    // Example using esp_dmx library:
    //   dmx_write(DMX_PORT, dmxData, 512);
    //   dmx_send(DMX_PORT);
}

// ============================================================
// BLE GATT Service Setup
// ============================================================

void setupBLE() {
    // Generate device name from MAC
    String macSuffix = WiFi.macAddress().substring(12);
    macSuffix.replace(":", "");
    String bleName = nodeName.length() > 0 ? nodeName : ("ChromaDMX-" + macSuffix);

    BLEDevice::init(bleName.c_str());
    bleServer = BLEDevice::createServer();
    bleServer->setCallbacks(new ServerCallbacks());

    BLEService* service = bleServer->createService(SERVICE_UUID);

    ConfigWriteCallback* configCb = new ConfigWriteCallback();
    CommandCallback* cmdCb = new CommandCallback();

    // Node Name (Read/Write)
    charNodeName = service->createCharacteristic(
        CHAR_NODE_NAME_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    charNodeName->setCallbacks(configCb);
    charNodeName->setValue(nodeName.c_str());

    // Wi-Fi SSID (Read/Write)
    charWifiSsid = service->createCharacteristic(
        CHAR_WIFI_SSID_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    charWifiSsid->setCallbacks(configCb);
    charWifiSsid->setValue(wifiSsid.c_str());

    // Wi-Fi Password (Write only)
    charWifiPassword = service->createCharacteristic(
        CHAR_WIFI_PASSWORD_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    charWifiPassword->setCallbacks(configCb);

    // Universe (Read/Write, uint16 LE)
    charUniverse = service->createCharacteristic(
        CHAR_UNIVERSE_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    charUniverse->setCallbacks(configCb);
    uint8_t univBytes[2] = {
        (uint8_t)(universe & 0xFF),
        (uint8_t)((universe >> 8) & 0xFF)
    };
    charUniverse->setValue(univBytes, 2);

    // DMX Start Address (Read/Write, uint16 LE)
    charDmxStartAddr = service->createCharacteristic(
        CHAR_DMX_START_ADDR_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    charDmxStartAddr->setCallbacks(configCb);
    uint8_t addrBytes[2] = {
        (uint8_t)(dmxStartAddress & 0xFF),
        (uint8_t)((dmxStartAddress >> 8) & 0xFF)
    };
    charDmxStartAddr->setValue(addrBytes, 2);

    // Provisioned Flag (Read/Notify)
    charProvisioned = service->createCharacteristic(
        CHAR_PROVISIONED_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    charProvisioned->addDescriptor(new BLE2902()); // CCCD for notifications
    charProvisioned->setValue(&provisionedFlag, 1);

    // Firmware Version (Read only)
    charFirmwareVer = service->createCharacteristic(
        CHAR_FIRMWARE_VER_UUID,
        BLECharacteristic::PROPERTY_READ
    );
    charFirmwareVer->setValue(FIRMWARE_VERSION);

    // Command (Write only)
    charCommand = service->createCharacteristic(
        CHAR_COMMAND_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    charCommand->setCallbacks(cmdCb);

    service->start();

    // Start advertising
    BLEAdvertising* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    advertising->setMinPreferred(0x06); // connection interval guidance
    BLEDevice::startAdvertising();

    Serial.println("BLE GATT service started, advertising...");
}

// ============================================================
// Arduino Entry Points
// ============================================================

void setup() {
    Serial.begin(115200);
    Serial.println("ChromaDMX ESP32 Node starting...");

    memset(dmxData, 0, sizeof(dmxData));

    // Load saved configuration
    loadConfig();

    // Always start BLE for provisioning
    setupBLE();

    // If already provisioned, also connect to Wi-Fi and start Art-Net
    if (provisionedFlag == STATUS_PROVISIONED && wifiSsid.length() > 0) {
        Serial.println("Previously provisioned, connecting to Wi-Fi...");
        WiFi.begin(wifiSsid.c_str(), wifiPassword.c_str());

        int attempts = 0;
        while (WiFi.status() != WL_CONNECTED && attempts < 20) {
            delay(500);
            Serial.print(".");
            attempts++;
        }

        if (WiFi.status() == WL_CONNECTED) {
            Serial.println("\nWi-Fi connected: " + WiFi.localIP().toString());
            startArtNet();
        } else {
            Serial.println("\nWi-Fi reconnect failed, BLE still active for re-provisioning");
        }
    }
}

void loop() {
    // Process incoming Art-Net packets
    if (WiFi.status() == WL_CONNECTED) {
        artnet.read();
    }

    // TODO: Add button check for entering provisioning mode
    // TODO: Add status LED updates
    // TODO: Add watchdog timer

    delay(1); // Yield to RTOS
}
```

## Pin Connections (Reference)

| ESP32 Pin | Connection        | Notes                              |
|-----------|-------------------|------------------------------------|
| GPIO 17   | MAX485 DI         | DMX data out (UART2 TX)           |
| GPIO 4    | MAX485 DE/RE      | Direction control (HIGH = transmit)|
| GPIO 2    | Status LED        | Built-in LED on many boards       |
| GPIO 0    | Provisioning Btn  | Pull-up, press to force BLE mode  |

## Build and Flash

```bash
# PlatformIO
pio run -t upload
pio device monitor

# Arduino IDE
# Select board: ESP32 Dev Module
# Upload Speed: 921600
# Flash via USB
```

## Production Considerations

1. **Security**: Enable BLE pairing with MITM protection for production.
   The reference sketch has no authentication.
2. **OTA Updates**: Implement the `CMD_ENTER_DFU` command to support
   over-the-air firmware updates via ArduinoOTA or ESP-IDF OTA.
3. **Watchdog**: Add a hardware watchdog timer to recover from crashes.
4. **Power Management**: Use ESP32 light sleep between DMX frames if
   battery-powered.
5. **DMX Output**: The reference uses a placeholder `outputDmx()` function.
   For production, use the `esp_dmx` library with DMA-backed UART for
   reliable 250kbaud DMX512 output.
6. **Multiple Universes**: For multi-universe nodes, create additional
   Art-Net universe handlers and map to separate DMX ports.
