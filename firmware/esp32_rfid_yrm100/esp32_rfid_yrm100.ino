/*
 * ============================================================
 *  Firmware ESP32 - Đầu đọc UHF RFID YRM100
 *  Dự án: Hệ thống quản lý thư viện
 * ============================================================
 *  Phiên bản: 4.0 (HOÀN CHỈNH - ĐÃ TEST THÀNH CÔNG)
 *
 *  Đấu nối:
 *    YRM100 GND (Pin1) --> ESP32 GND
 *    YRM100 EN  (Pin2) --> ESP32 3V3    ⭐ BẮT BUỘC!
 *    YRM100 RXD (Pin3) --> ESP32 GPIO17
 *    YRM100 TXD (Pin4) --> ESP32 GPIO4  (hoặc GPIO16)
 *    YRM100 VCC (Pin5) --> ESP32 3V3 hoặc 5V
 *
 *  Thư viện: PubSubClient, ArduinoJson
 *  Board: ESP32 Dev Module
 * ============================================================
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ========== ĐỔI CÁC GIÁ TRỊ NÀY ==========
const char* WIFI_SSID     = "TRUNGSANG";
const char* WIFI_PASSWORD = "123456788";
const char* MQTT_SERVER   = "192.168.137.1";
const int   MQTT_PORT     = 1883;
const char* DEVICE_LOCATION = "ENTRANCE";
// ============================================

const char* MQTT_TOPIC = "warehouse/rfid/scan";

// UART - Đổi MY_RX_PIN nếu cần
HardwareSerial rfidSerial(2);
const int RX_PIN = 16;   // YRM100 TXD --> ESP32 GPIO16 (RX2)
const int TX_PIN = 17;   // YRM100 RXD --> ESP32 GPIO17 (TX2)

WiFiClient espClient;
PubSubClient mqttClient(espClient);

const byte CMD_SCAN[] = { 0xBB, 0x00, 0x22, 0x00, 0x00, 0x22, 0x7E };

const int BUFFER_SIZE = 64;
byte rxBuffer[BUFFER_SIZE];
int rxIndex = 0;

String lastEPC = "";
unsigned long lastPublishTime = 0;
const unsigned long DUPLICATE_TIMEOUT_MS = 3000;

unsigned long lastScanTime = 0;
const unsigned long SCAN_INTERVAL_MS = 500;

const int LED_PIN = 2;

void setup() {
  Serial.begin(115200);
  Serial.println();
  Serial.println("========================================");
  Serial.println("  He thong quan ly thu vien");
  Serial.println("  ESP32 + RFID YRM100 v4.0");
  Serial.println("========================================");

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  rfidSerial.begin(115200, SERIAL_8N1, RX_PIN, TX_PIN);
  Serial.printf("[RFID] UART: RX=GPIO%d, TX=GPIO%d\n", RX_PIN, TX_PIN);

  // Test kết nối YRM100
  Serial.print("[RFID] Kiem tra module YRM100... ");
  delay(200);
  while (rfidSerial.available()) rfidSerial.read();
  
  rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
  rfidSerial.flush();
  
  unsigned long t = millis();
  bool detected = false;
  while (millis() - t < 1000) {
    if (rfidSerial.available()) {
      byte b = rfidSerial.read();
      if (b == 0xBB) { detected = true; break; }
    }
    delay(5);
  }
  while (rfidSerial.available()) rfidSerial.read();
  
  if (detected) {
    Serial.println("OK! Module phan hoi.");
  } else {
    Serial.println("KHONG PHAN HOI! Kiem tra day noi.");
  }

  setupWifi();
  mqttClient.setServer(MQTT_SERVER, MQTT_PORT);
  mqttClient.setBufferSize(512);

  Serial.println("[SYSTEM] San sang quet the RFID!\n");
}

void loop() {
  if (!mqttClient.connected()) reconnectMqtt();
  mqttClient.loop();

  if (millis() - lastScanTime >= SCAN_INTERVAL_MS) {
    lastScanTime = millis();
    rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
    rfidSerial.flush();
  }

  readAndProcess();
}

void setupWifi() {
  Serial.printf("[WIFI] Ket noi: %s", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int r = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500); Serial.print(".");
    if (++r > 40) { Serial.println(" FAIL!"); ESP.restart(); }
  }
  Serial.printf("\n[WIFI] OK! IP: %s\n", WiFi.localIP().toString().c_str());
}

void reconnectMqtt() {
  int a = 0;
  while (!mqttClient.connected() && a < 3) {
    Serial.print("[MQTT] Ket noi...");
    String id = "ESP32-RFID-" + String(random(0xFFFF), HEX);
    if (mqttClient.connect(id.c_str())) {
      Serial.println(" OK!");
      for (int i = 0; i < 3; i++) {
        digitalWrite(LED_PIN, HIGH); delay(100);
        digitalWrite(LED_PIN, LOW);  delay(100);
      }
    } else {
      Serial.printf(" FAIL(rc=%d)\n", mqttClient.state());
      delay(3000);
    }
    a++;
  }
}

void readAndProcess() {
  while (rfidSerial.available()) {
    byte b = rfidSerial.read();

    if (rxIndex == 0 && b != 0xBB) continue;
    rxBuffer[rxIndex++] = b;

    if (b == 0x7E && rxIndex > 5) {
      processFrame(rxBuffer, rxIndex);
      rxIndex = 0;
    }
    if (rxIndex >= BUFFER_SIZE) rxIndex = 0;
  }
}

void processFrame(byte* frame, int length) {
  // Bỏ qua frame lỗi (không có thẻ - bình thường)
  if (frame[1] == 0x01 && frame[2] == 0xFF) return;
  
  // Bỏ qua frame phản hồi lệnh
  if (frame[1] == 0x01) return;

  // Chỉ xử lý frame quét thẻ thành công: Type=0x02, Cmd=0x22
  if (frame[1] != 0x02 || frame[2] != 0x22) return;
  if (length < 24) return;

  int8_t rssi = (int8_t)frame[5];

  // Tách EPC (12 bytes, từ index 8 đến 19)
  char epcHex[25] = "";
  for (int i = 0; i < 12; i++) {
    sprintf(epcHex + (i * 2), "%02X", frame[8 + i]);
  }
  String currentEPC = String(epcHex);

  // In thông tin thẻ
  Serial.println("--------------------------------------------");
  Serial.printf("  QUET DUOC THE RFID!\n");
  Serial.printf("  EPC : %s\n", epcHex);
  Serial.printf("  RSSI: %d dBm\n", rssi);
  Serial.println("--------------------------------------------");

  // Chống trùng lặp
  if (currentEPC == lastEPC && (millis() - lastPublishTime) < DUPLICATE_TIMEOUT_MS) {
    Serial.println("  (Trung lap, bo qua)");
    return;
  }

  // Nháy LED
  digitalWrite(LED_PIN, HIGH); delay(200);
  digitalWrite(LED_PIN, LOW);

  // Gửi MQTT
  JsonDocument doc;
  doc["epc"] = epcHex;
  doc["location"] = DEVICE_LOCATION;

  char json[256];
  serializeJson(doc, json);

  Serial.printf("[MQTT] Gui: %s\n", json);
  if (mqttClient.publish(MQTT_TOPIC, json)) {
    Serial.println("[MQTT] THANH CONG!");
  } else {
    Serial.println("[MQTT] THAT BAI!");
  }

  lastEPC = currentEPC;
  lastPublishTime = millis();
}
