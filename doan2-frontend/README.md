# Frontend - MQTT Sensor Dashboard

Frontend React/TypeScript cho hệ thống quản lý MQTT Sensor

## Cài Đặt

```bash
npm install
```

## Chạy Development Server

```bash
npm run dev
```

Frontend sẽ chạy tại `http://localhost:5173`

## Build

```bash
npm run build
```

## Cấu Trúc Thư Mục

```
src/
├── api/              # API clients
├── components/       # React components
├── hooks/            # Custom hooks
├── pages/            # Pages/Views
├── store/            # Zustand stores
├── types/            # TypeScript types
├── App.tsx          # Main App component
└── main.tsx         # Entry point
```

## Cấu Hình

Tạo file `.env` với nội dung:

```
VITE_API_URL=http://localhost:8080/api
```

## Tính Năng

- ✅ Đăng nhập JWT
- ✅ Dashboard hiển thị thiết bị MQTT
- ✅ Trạng thái kết nối Broker
- ✅ Quản lý thiết bị MQTT
- ✅ Protected routes

## API Endpoints

- `POST /auth/login` - Đăng nhập
- `GET /mqtt/active` - Danh sách thiết bị MQTT đang hoạt động
- `GET /mqtt/broker-status` - Trạng thái broker MQTT
