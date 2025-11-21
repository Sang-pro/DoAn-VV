# Dự án DoAn2 - Hệ thống Giám sát Dữ liệu Cảm biến

Đây là một dự án full-stack bao gồm một backend Spring Boot và một frontend React, được thiết kế để giám sát dữ liệu từ các cảm biến trong thời gian thực thông qua MQTT và WebSockets.

## Tính năng chính

- **Xác thực người dùng:** Đăng ký, đăng nhập, và quản lý phiên làm việc sử dụng JWT (JSON Web Tokens).
- **Quản lý người dùng:** Chức năng cho quản trị viên (admin) để xem và quản lý trạng thái người dùng.
- **Tích hợp MQTT:** Cho phép các thiết bị (sensors) gửi dữ liệu đến một MQTT broker.
- **Hiển thị thời gian thực:** Dữ liệu từ MQTT được đẩy đến frontend qua WebSockets để hiển thị trực tiếp mà không cần tải lại trang.
- **Giao diện người dùng:** Giao diện được xây dựng bằng React và Tailwind CSS để hiển thị dữ liệu cảm biến và quản lý hệ thống.
- **Bảo mật:** Sử dụng Spring Security để phân quyền và bảo vệ các API endpoints.

## Công nghệ sử dụng

**Backend (Thư mục: `doan2`)**
- **Framework:** Spring Boot
- **Ngôn ngữ:** Java
- **Database:** MySQL
- **ORM:** Spring Data JPA (Hibernate)
- **Bảo mật:** Spring Security, JWT
- **Real-time:** Spring WebSocket, Eclipse Paho (MQTT Client)
- **API Documentation:** SpringDoc (Swagger UI)

**Frontend (Thư mục: `doan2-frontend`)**
- **Framework:** React
- **Ngôn ngữ:** TypeScript
- **Build Tool:** Vite
- **Styling:** Tailwind CSS
- **State Management:** Zustand
- **Real-time:** `paho-mqtt` (for WebSocket connection to the backend)

## Yêu cầu hệ thống

- **Java Development Kit (JDK)**: phiên bản 17 hoặc mới hơn.
- **Maven**: để quản lý các dependency cho backend.
- **Node.js và npm**: phiên bản 18 hoặc mới hơn.
- **MySQL**: cơ sở dữ liệu.
- **MQTT Broker**: một MQTT broker đang hoạt động (ví dụ: Mosquitto, HiveMQ).

## Hướng dẫn cài đặt và khởi chạy

### 1. Backend (Spring Boot)

1.  **Di chuyển vào thư mục backend:**
    ```bash
    cd doan2
    ```

2.  **Cấu hình ứng dụng:**
    Mở file `src/main/resources/application.properties` và cập nhật các thông tin sau cho phù hợp với môi trường của bạn:
    - **Thông tin kết nối database:**
      ```properties
      spring.datasource.url=jdbc:mysql://localhost:3306/doan2
      spring.datasource.username=your_username
      spring.datasource.password=your_password
      ```
    - **Thông tin MQTT Broker:**
      ```properties
      mqtt.broker.url=tcp://your_mqtt_broker_address:1883
      mqtt.username=your_mqtt_username
      mqtt.password=your_mqtt_password
      ```
    - **JWT Secret:**
      Bạn có thể giữ nguyên hoặc thay đổi `app.jwt.secret` thành một chuỗi bí mật khác.

3.  **Chạy ứng dụng:**
    Sử dụng Maven Wrapper được cung cấp sẵn:
    ```bash
    ./mvnw spring-boot:run
    ```
    Backend sẽ khởi động trên cổng `8080`.

4.  **Kiểm tra API:**
    Sau khi khởi động thành công, bạn có thể truy cập Swagger UI để xem tài liệu và thử nghiệm các API tại:
    `http://localhost:8080/swagger-ui`

### 2. Frontend (React)

1.  **Di chuyển vào thư mục frontend:**
    ```bash
    cd doan2-frontend
    ```

2.  **Cài đặt các dependency:**
    ```bash
    npm install
    ```

3.  **Cấu hình biến môi trường:**
    Tạo một file `.env` trong thư mục `doan2-frontend` và chỉ định địa chỉ của backend API:
    ```env
    VITE_API_BASE_URL=http://localhost:8080
    ```

4.  **Chạy ứng dụng:**
    ```bash
    npm run dev
    ```
    Frontend sẽ khởi động và có thể truy cập tại `http://localhost:5173` (hoặc một cổng khác nếu 5173 đã được sử dụng).

## Cấu trúc thư mục

```
.
├── doan2/                # Thư mục backend Spring Boot
│   ├── src/main/java/    # Mã nguồn Java
│   └── pom.xml           # File cấu hình Maven
├── doan2-frontend/       # Thư mục frontend React
│   ├── src/              # Mã nguồn React/TypeScript
│   └── package.json      # File cấu hình npm
└── README.md             # File này
```
