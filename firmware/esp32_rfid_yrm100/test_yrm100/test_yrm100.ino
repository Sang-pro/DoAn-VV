/*
 * TEST NHANH - Thu nhieu GPIO khac nhau
 * 
 * Day noi:
 *   YRM100 GND (Pin1) --> ESP32 GND
 *   YRM100 EN  (Pin2) --> ESP32 3V3
 *   YRM100 RXD (Pin3) --> ESP32 GPIO17
 *   YRM100 TXD (Pin4) --> ESP32 GPIO4  <-- THU CHAN NAY
 *   YRM100 VCC (Pin5) --> ESP32 3V3 hoac 5V
 *
 * Neu khong duoc, doi sang GPIO13, GPIO27, GPIO14
 */

// ====== DOI CHAN NHAN DU LIEU O DAY ======
#define MY_RX_PIN  4    // Thu: 4, 13, 27, 14
#define MY_TX_PIN  17   // Giu nguyen 17
// =========================================

HardwareSerial rfidSerial(2);
const byte CMD_SCAN[] = { 0xBB, 0x00, 0x22, 0x00, 0x00, 0x22, 0x7E };
int cnt = 0;

void setup() {
  Serial.begin(115200);
  Serial.println();
  Serial.printf("=== TEST YRM100 - RX=GPIO%d, TX=GPIO%d ===\n", MY_RX_PIN, MY_TX_PIN);
  
  rfidSerial.begin(115200, SERIAL_8N1, MY_RX_PIN, MY_TX_PIN);
  delay(200);
  while (rfidSerial.available()) rfidSerial.read();
  
  Serial.println("Gui lenh scan dau tien...");
  rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
  rfidSerial.flush();
  
  unsigned long t = millis();
  bool got = false;
  while (millis() - t < 2000) {
    if (rfidSerial.available()) {
      if (!got) { Serial.print("[DATA] "); got = true; }
      Serial.printf("%02X ", rfidSerial.read());
    }
    delay(2);
  }
  
  if (got) {
    Serial.println("\n\n*** THANH CONG! GPIO nay hoat dong! ***\n");
  } else {
    Serial.printf("\n[X] GPIO%d khong nhan duoc gi.\n", MY_RX_PIN);
    Serial.println("Doi MY_RX_PIN sang so khac (13, 27, 14) roi upload lai.\n");
  }
}

void loop() {
  // Gui lenh scan moi 2 giay
  if (millis() > (cnt + 1) * 2000) {
    cnt++;
    rfidSerial.write(CMD_SCAN, sizeof(CMD_SCAN));
    rfidSerial.flush();
    if (cnt % 5 == 0) Serial.printf("[...] Lan %d - van dang cho...\n", cnt);
  }
  
  // In bat ky byte nao nhan duoc
  while (rfidSerial.available()) {
    Serial.printf("[!!!] 0x%02X ", rfidSerial.read());
  }
}
