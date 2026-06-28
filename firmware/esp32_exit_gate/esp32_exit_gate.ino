/*
 * ============================================================
 *  Firmware ESP32 - Cổng an ninh thư viện (EXIT GATE)
 *  Dự án: Hệ thống quản lý thư viện
 * ============================================================
 *
 *  Chức năng:
 *  - Quét thẻ RFID sách đi qua cổng ra
 *  - Gửi EPC lên Backend qua MQTT để kiểm tra trạng thái mượn
 *  - Nếu sách CHƯA được mượn → Backend gửi lệnh báo động
 *  - ESP32 kích hoạt còi cảnh báo
 *
 *  Đấu nối:
 *    YRM100 GND (Pin1) --> ESP32 GND
 *    YRM100 EN  (Pin2) --> ESP32 3V3     ⭐ BẮT BUỘC
 *    YRM100 RXD (Pin3) --> ESP32 GPIO17  (TX2)
 *    YRM100 TXD (Pin4) --> ESP32 GPIO16  (RX2)
 *    YRM100 VCC (Pin5) --> ESP32 3V3 hoặc 5V
 *    CÒI BUZZER (+)    --> ESP32 GPIO2   (D2)
 *    CÒI BUZZER (-)    --> ESP32 GND
 *
 *  Thư viện: PubSubClient, ArduinoJson
 *  Board: ESP32 Dev Module
 * ============================================================
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ========== CẤU HÌNH ==========
const char* WIFI_SSID     = "TRUNGSANG";
const char* WIFI_PASSWORD = "123456788";
const char* MQTT_SERVER   = "192.168.137.1";
const int   MQTT_PORT     = 1883;
const char* DEVICE_ID     = "EXIT_GATE_01";      // ID thiết bị cổng ra
// ===============================

// MQTT Topics
const char* TOPIC_GATE_SCAN = "warehouse/rfid/gate";        // Gửi EPC lên Backend
const char* TOPIC_ALARM     = "warehouse/rfid/alarm";        // Nhận lệnh báo động từ Backend

// UART - YRM100
HardwareSerial rfidSerial(2);
const int RX_PIN = 16;
const int TX_PIN = 17;

// Buzzer (còi cảnh báo) - Chân D2
const int BUZZER_PIN = 2;

// LED báo trạng thái (dùng chung với buzzer nếu D2, hoặc đổi chân)
const int LED_PIN = 2;

WiFiClient espClient;
PubSubClient mqttClient(espClient);

const byte CMD_SCAN[] = { 0xBB, 0x00, 0x22, 0x00, 0x00, 0x22, 0x7E };

const int BUFFER_SIZE = 64;
byte rxBuffer[BUFFER_SIZE];
int rxIndex = 0;

// Chống quét trùng
String lastEPC = "";
unsigned long lastScanTime = 0;
const unsigned long SCAN_INTERVAL_MS = 500;
const unsigned long DUPLICATE_TIMEOUT_MS = 5000;  // 5 giây chống trùng
unsigned long lastPublishTime = 0;

// Trạng thái còi
bool alarmActive = false;
unsigned long alarmStartTime = 0;
const unsigned long ALARM_DURATION_MS = 5000;  // Còi kêu 5 giây

void setup() {
  Serial.begin(115200);
  Serial.println();
  Serial.println("========================================");
  Serial.println("  CONG AN NINH THU VIEN - EXIT GATE");
  Serial.println("  ESP32 + RFID YRM100 + Buzzer");
  Serial.println("========================================");

  // Cấu hình chân còi
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);

  // Test còi khi khởi động (2 tiếng bíp ngắn)
  Serial.println("[BUZZER] Test coi...");
  for (int i = 0; i < 2; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(100);
    digitalWrite(BUZZER_PIN, LOW);
    delay(100);
  }
  Serial.println("[BUZZER] OK!");

  // Khởi tạo UART cho YRM100
  rfidSerial.begin(115200, SERIAL_8N1, RX_PIN, TX_PIN);
  Serial.printf("[RFID] UART: RX=GPIO%d, TX=GPIO%d\n", RX_PIN, TX_PIN);

  // Test module YRM100
  Serial.print("[RFID] Kiem tra YRM100... ");
  delay(200);
  while (rfidSerial.available()) rfidSerial.read();
  rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
  rfidSerial.flush();
  unsigned long t = millis();
  bool detected = false;
  while (millis() - t < 1000) {
    if (rfidSerial.available()) {
      if (rfidSerial.read() == 0xBB) { detected = true; break; }
    }
    delay(5);
  }
  while (rfidSerial.available()) rfidSerial.read();
  Serial.println(detected ? "OK!" : "KHONG PHAN HOI!");

  // WiFi
  setupWifi();

  // MQTT
  mqttClient.setServer(MQTT_SERVER, MQTT_PORT);
  mqttClient.setBufferSize(512);
  mqttClient.setCallback(mqttCallback);  // Đăng ký hàm nhận tin nhắn

  Serial.println("[SYSTEM] Cong an ninh san sang!\n");
}

void loop() {
  if (!mqttClient.connected()) reconnectMqtt();
  mqttClient.loop();

  // Quét RFID định kỳ
  if (millis() - lastScanTime >= SCAN_INTERVAL_MS) {
    lastScanTime = millis();
    rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
    rfidSerial.flush();
  }

  // Đọc phản hồi
  readAndProcess();

  // Tắt còi sau thời gian báo động
  if (alarmActive && (millis() - alarmStartTime >= ALARM_DURATION_MS)) {
    digitalWrite(BUZZER_PIN, LOW);
    alarmActive = false;
    Serial.println("[BUZZER] Tat coi.");
  }
}

// ===================== NHẬN LỆNH TỪ BACKEND =====================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg = "";
  for (unsigned int i = 0; i < length; i++) {
    msg += (char)payload[i];
  }

  Serial.printf("[MQTT] Nhan tu topic '%s': %s\n", topic, msg.c_str());

  // Kiểm tra lệnh báo động
  if (String(topic) == TOPIC_ALARM) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, msg);
    if (err) {
      Serial.printf("[MQTT] Loi parse JSON: %s\n", err.c_str());
      return;
    }

    bool alarm = doc["alarm"] | false;
    String epc = doc["epc"] | "";
    String bookTitle = doc["bookTitle"] | "Khong ro";

    if (alarm) {
      Serial.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      Serial.println("  CANH BAO: SACH CHUA DUOC MUON!");
      Serial.printf("  EPC: %s\n", epc.c_str());
      Serial.printf("  Sach: %s\n", bookTitle.c_str());
      Serial.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

      // Kích hoạt còi báo động
      triggerAlarm();
    } else {
      Serial.printf("[OK] Sach '%s' da duoc muon hop le.\n", bookTitle.c_str());
    }
  }
}

// ===================== KÍCH HOẠT CÒI =====================
void triggerAlarm() {
  alarmActive = true;
  alarmStartTime = millis();

  // Pattern còi: BÍP BÍP BÍP liên tục
  for (int i = 0; i < 10; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(200);
    digitalWrite(BUZZER_PIN, LOW);
    delay(100);
  }

  // Giữ còi kêu liên tục sau đó
  digitalWrite(BUZZER_PIN, HIGH);
}

// ===================== WIFI =====================
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

// ===================== MQTT =====================
void reconnectMqtt() {
  int a = 0;
  while (!mqttClient.connected() && a < 3) {
    Serial.print("[MQTT] Ket noi...");
    String id = "ESP32-GATE-" + String(random(0xFFFF), HEX);
    if (mqttClient.connect(id.c_str())) {
      Serial.println(" OK!");

      // Subscribe topic nhận lệnh báo động từ Backend
      mqttClient.subscribe(TOPIC_ALARM);
      Serial.printf("[MQTT] Subscribe: %s\n", TOPIC_ALARM);

      // Bíp 3 lần báo kết nối OK
      for (int i = 0; i < 3; i++) {
        digitalWrite(BUZZER_PIN, HIGH); delay(50);
        digitalWrite(BUZZER_PIN, LOW);  delay(50);
      }
    } else {
      Serial.printf(" FAIL(rc=%d)\n", mqttClient.state());
      delay(3000);
    }
    a++;
  }
}

// ===================== ĐỌC RFID =====================
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

// ===================== XỬ LÝ FRAME =====================
void processFrame(byte* frame, int length) {
  if (frame[1] == 0x01) return;  // Bỏ qua frame lỗi/phản hồi lệnh
  if (frame[1] != 0x02 || frame[2] != 0x22) return;
  if (length < 24) return;

  int8_t rssi = (int8_t)frame[5];
  char epcHex[25] = "";
  for (int i = 0; i < 12; i++) {
    sprintf(epcHex + (i * 2), "%02X", frame[8 + i]);
  }
  String currentEPC = String(epcHex);

  Serial.println("--------------------------------------------");
  Serial.printf("  [GATE] Phat hien sach di qua cong!\n");
  Serial.printf("  EPC : %s\n", epcHex);
  Serial.printf("  RSSI: %d dBm\n", rssi);
  Serial.println("--------------------------------------------");

  // Chống trùng
  if (currentEPC == lastEPC && (millis() - lastPublishTime) < DUPLICATE_TIMEOUT_MS) {
    Serial.println("  (Trung lap, bo qua)");
    return;
  }

  // Gửi EPC lên Backend để kiểm tra
  JsonDocument doc;
  doc["epc"] = epcHex;
  doc["deviceId"] = DEVICE_ID;

  char json[256];
  serializeJson(doc, json);

  Serial.printf("[MQTT] Gui kiem tra: %s\n", json);
  if (mqttClient.publish(TOPIC_GATE_SCAN, json)) {
    Serial.println("[MQTT] Da gui len Backend kiem tra.");
  } else {
    Serial.println("[MQTT] GUI THAT BAI!");
  }

  lastEPC = currentEPC;
  lastPublishTime = millis();
}
