# Tính Năng Quản Lý MQTT và Dữ liệu Cảm biến Realtime

## Tổng Quan

Ứng dụng đã được nâng cấp với hai tính năng chính:

### 1. **Quản lý Thiết bị MQTT** (`/mqtt`)
- Thêm, chỉnh sửa, xóa và khóa các thiết bị MQTT
- Xem danh sách các thiết bị hoạt động
- Quản lý username, password và broker URL

### 2. **Xem Dữ liệu Cảm biến Realtime** (`/sensor-data`)
- Kết nối WebSocket để nhận dữ liệu realtime từ cảm biến
- Hiển thị dữ liệu nhiệt độ, độ ẩm, áp suất, ánh sáng
- Lịch sử dữ liệu (100 bản ghi gần nhất)
- Chuyển đổi giữa các thiết bị

## Kiến Trúc

### Frontend Structure
```
src/
├── hooks/
│   ├── useAuth.ts (sẵn có)
│   └── useWebSocket.ts (MỚI)
├── pages/
│   ├── Dashboard.tsx (CẬP NHẬT)
│   ├── MqttManager.tsx (MỚI)
│   ├── SensorDataView.tsx (MỚI)
│   ├── Dashboard.css (CẬP NHẬT)
│   ├── MqttManager.css (MỚI)
│   └── SensorDataView.css (MỚI)
└── App.tsx (CẬP NHẬT)
```

### useWebSocket Hook

Custom hook để quản lý kết nối WebSocket STOMP với server:

```typescript
const { subscribe, send, isConnected } = useWebSocket();

// Subscribe to sensor data topic
const unsubscribe = subscribe('data/device1/sensors', (message) => {
  console.log('Sensor data:', message);
});

// Send message
send('/app/mqtt/publish', { topic: 'test', payload: {...} });

// Clean up
return unsubscribe;
```

**Features:**
- Tự động kết nối khi component mount
- JWT token authentication
- Message parsing và routing
- Cleanup tự động

## Cách Sử Dụng

### 1. Quản lý MQTT Devices

**Thêm Thiết bị:**
1. Điều hướng tới `/mqtt`
2. Nhấp "Thêm Thiết bị"
3. Nhập:
   - MQTT Username (bắt buộc)
   - MQTT Password (bắt buộc)
   - Broker URL (tùy chọn, mặc định từ server)
4. Nhấp "Thêm mới"

**Chỉnh sửa Thiết bị:**
1. Nhấp nút "Sửa" trên thẻ thiết bị
2. Cập nhật thông tin
3. Nhấp "Cập nhật"

**Tắt/Xóa Thiết bị:**
- Nhấp "Tắt" để khóa thiết bị (vẫn giữ dữ liệu)
- Nhấp "Xóa" để xóa vĩnh viễn

### 2. Xem Dữ liệu Cảm biến Realtime

**Bắt đầu:**
1. Điều hướng tới `/sensor-data`
2. Chọn thiết bị từ dropdown
3. Chờ dữ liệu từ cảm biến

**Dữ liệu Hiển thị:**
- 4 metric card hiển thị dữ liệu mới nhất:
  - 🌡️ Nhiệt độ (°C)
  - 💧 Độ ẩm (%)
  - 🔔 Áp suất (hPa)
  - ☀️ Ánh sáng (lux)

- Bảng lịch sử (100 bản ghi)
  - Timestamp
  - Giá trị từng sensor
  - Dữ liệu khác (JSON)

**Trạng thái Kết nối:**
- Indicator ở header hiển thị trạng thái WebSocket
- 🟢 Xanh = Kết nối
- 🔴 Đỏ = Ngắt kết nối

## API Endpoints (Backend)

### MQTT Management
```
POST   /api/mqtt/register              - Đăng ký thiết bị mới
GET    /api/mqtt/{id}                  - Lấy chi tiết thiết bị
GET    /api/mqtt/username/{username}   - Tìm thiết bị theo username
GET    /api/mqtt/active                - Danh sách thiết bị hoạt động
PATCH  /api/mqtt/{id}                  - Cập nhật thiết bị
DELETE /api/mqtt/{id}                  - Xóa thiết bị
POST   /api/mqtt/{id}/deactivate       - Tắt thiết bị
POST   /api/mqtt/publish               - Publish message
GET    /api/mqtt/broker-status         - Kiểm tra trạng thái broker
```

### WebSocket
```
Endpoint: /ws (STOMP)

Subscribe Topics:
- /user/queue/sensor-data     - Dữ liệu cảm biến của người dùng
- /topic/sensors/{device}     - Dữ liệu công khai của thiết bị

Destinations (Send):
- /app/mqtt/subscribe         - Subscribe to device
- /app/mqtt/unsubscribe       - Unsubscribe from device
- /app/mqtt/publish           - Publish MQTT message
```

## Mẫu Dữ liệu Cảm biến

Dữ liệu từ MQTT được chuyển đổi sang format:

```json
{
  "timestamp": "17/11/2025 10:30:45",
  "temperature": 24.5,
  "humidity": 65.3,
  "pressure": 1013.25,
  "light": 450,
  "motion": true,
  "other": {
    "co2": 450,
    "custom_field": "value"
  }
}
```

## Styling & UI

### Màu Sắc
- Primary: `#667eea` đến `#764ba2` (gradient)
- Success: `#28a745`
- Danger: `#dc3545`
- Warning: `#ffc107`
- Info: `#17a2b8`

### Responsive Design
- Desktop: Hiển thị đầy đủ
- Tablet: Lưới thích ứng
- Mobile: Ẩn các cột không cần thiết

### Components
- Card-based layout
- Gradient headers
- Status badges
- Animated connection indicator

## Lỗi Thường Gặp

### WebSocket Không Kết Nối
1. Kiểm tra server đang chạy
2. Kiểm tra token JWT hợp lệ (xem localStorage)
3. Kiểm tra console để xem lỗi chi tiết

### Không Nhận Dữ liệu Cảm biến
1. Kiểm tra thiết bị có đang hoạt động (`isActive = true`)
2. Kiểm tra cảm biến đang publish dữ liệu
3. Kiểm tra topic MQTT đúng format: `data/{username}/sensors`

### Lỗi 401 Unauthorized
1. Token hết hạn - đăng nhập lại
2. Token bị revoke - đăng nhập lại
3. Kiểm tra Authorization header trong Network tab

## Phát Triển Tiếp Theo

### Planned Features
- [ ] Chart visualization (Recharts/Chart.js)
- [ ] Data export (CSV/Excel)
- [ ] Alert/Threshold configuration
- [ ] Device groups/categories
- [ ] Data analytics dashboard
- [ ] Historical data comparison

### Performance Optimization
- [ ] Pagination cho bảng dữ liệu
- [ ] Virtual scrolling cho bảng lớn
- [ ] Data compression trong WebSocket
- [ ] Caching for MQTT devices

## Troubleshooting

### Network Issues
```typescript
// Kiểm tra WebSocket status
const { isConnected } = useWebSocket();
console.log('WebSocket connected:', isConnected);
```

### Data Not Updating
```typescript
// Kiểm tra subscription
const unsubscribe = subscribe('data/device/sensors', (msg) => {
  console.log('Received:', msg); // Nên thấy log
});
```

### UI Issues
- Xóa localStorage: `localStorage.clear()`
- Hard refresh: `Ctrl+Shift+R`
- Kiểm tra console errors

## References

- React Hooks: https://react.dev/reference/react
- WebSocket/STOMP: https://stomp-js.github.io/stomp-websocket/
- Zustand: https://github.com/pmndrs/zustand
- Axios: https://axios-http.com/

