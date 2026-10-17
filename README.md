# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)
> **Bài tập lớn môn Lập trình nâng cao — Học kỳ II (2025–2026)**  
> Trường Đại học Công nghệ - ĐHQGHN (VNU-UET)

---

## 📌 1. TỔNG QUAN KIẾN TRÚC DỰ ÁN

Hệ thống được xây dựng theo kiến trúc **Client – Server**, áp dụng mô hình phân lớp **MVC** và cấu trúc **Maven Multi-Module**:

```text
auction-system/
├── pom.xml                   # Root Parent POM (Quản lý phiên bản & build plugins)
├── config/checkstyle.xml      # Enforce quy chuẩn Google Java Style Guide
├── .github/workflows/ci.yml   # CI/CD tự động build & test qua GitHub Actions
│
├── auction-common/            # [Module 1] Thư viện dùng chung
│   ├── model/                 # OOP Domain Entities (User, Bidder, Seller, Admin, Item, Auction...)
│   ├── factory/               # Factory Method tạo các loại sản phẩm
│   ├── enums/                 # Trạng thái phiên đấu giá, vai trò người dùng, mã lệnh
│   ├── protocol/              # Giao thức truyền tin JSON qua Socket (Request/Response/Broadcast)
│   └── exception/             # Custom Exceptions (InvalidBid, AuctionClosed...)
│
├── auction-server/            # [Module 2] Máy chủ Đa luồng & Quản lý nghiệp vụ
│   ├── core/                  # AuctionManager (Singleton), AuctionLockService, AutoBidEngine
│   ├── observer/              # Observer Pattern gửi thông báo Realtime tới Client qua Socket
│   ├── network/               # SocketServer (ServerSocket + ThreadPool) & ClientHandler
│   ├── service/               # Nghiệp vụ đặt giá, chống sniping, quản lý tài khoản
│   ├── dao/                   # Tầng truy xuất dữ liệu (Data Access Object)
│   └── test/                  # Bộ Unit Test (JUnit 5) kiểm thử logic & Concurrency
│
└── auction-client/            # [Module 3] Ứng dụng giao diện đồ họa người dùng
    ├── controller/            # JavaFX Controllers (Login, Dashboard, AuctionRoom)
    ├── network/               # SocketClient (kết nối Socket & luồng nền bắt Event)
    └── resources/             # Giao diện FXML (SceneBuilder) và CSS hiện đại
```

---

## 🎯 2. ĐÁP ỨNG TIÊU CHÍ BAREM CHẤM ĐIỂM (10 + 1.5 ĐIỂM)

| Tiêu chí | Điểm | Hiện thực trong mã nguồn |
| :--- | :---: | :--- |
| **Thiết kế lớp & Kế thừa** | 0.5đ | `Entity` $\rightarrow$ `User` (Bidder/Seller/Admin), `Item` (Electronics/Art/Vehicle), `Auction`, `BidTransaction`. |
| **4 nguyên tắc OOP** | 1.0đ | **Encapsulation** (private/getter/setter), **Inheritance** (phân cấp lớp rõ ràng), **Polymorphism** (override `printInfo()`, `getSummary()`), **Abstraction** (`Entity`, `User`, `Item`). |
| **Design Patterns** | 1.0đ | **Singleton**: `AuctionManager`, `ClientSession`.<br>**Factory Method**: `ItemFactory`.<br>**Observer**: `RealtimeNotifier` $\rightarrow$ `ClientHandler` qua Socket. |
| **Chức năng chính** | 1.0đ | Quản lý người dùng, sản phẩm, đăng ký/đăng nhập. |
| **Chức năng đấu giá** | 1.0đ | Đặt giá, kiểm tra tính hợp lệ, tự động cập nhật người dẫn đầu, chuyển trạng thái vòng đời. |
| **Xử lý lỗi & Ngoại lệ** | 1.0đ | `InvalidBidException`, `AuctionClosedException`, `AuthenticationException`. |
| **Xử lý đồng thời (Concurrency)** | 1.0đ | `AuctionLockService` dùng `ReentrantLock` theo từng Auction ID $\rightarrow$ Tránh Lost update, race condition, 2 người cùng thắng. |
| **Realtime Update (Socket)** | 0.5đ | Khi có bid mới, Server push sự kiện JSON ngay lập tức tới mọi Client trong phòng (Không dùng polling). |
| **Kiến trúc Client–Server** | 0.5đ | Tách biệt client/server; chỉ Server truy cập và lưu trữ dữ liệu. |
| **Áp dụng MVC** | 0.5đ | Client: FXML (View) + Controller + Service/Model. Server: Controller/Handler $\rightarrow$ Service $\rightarrow$ DAO. |
| **Maven & Code Quality** | 0.5đ | Chuẩn Maven Multi-Module, cấu hình `checkstyle.xml` (Google Java Style). |
| **Unit Test (JUnit 5)** | 0.5đ | Viết test kiểm tra nghiệp vụ và test đa luồng `ConcurrencyBidTest` (20 luồng đồng thời). |
| **CI/CD** | 0.5đ | Cấu hình `.github/workflows/ci.yml` tự động build và chạy test mỗi khi push code lên GitHub. |
| **[Nâng cao] Auto-Bidding** | +0.5đ | `AutoBidEngine` sử dụng `PriorityQueue` so sánh các mức `maxBid` và độ ưu tiên thời gian FIFO. |
| **[Nâng cao] Anti-sniping** | +0.5đ | `checkAndApplyAntiSniping()`: Nếu có bid trong 30 giây cuối, tự động cộng thêm 60 giây và dời lịch đóng phiên. |
| **[Nâng cao] Realtime Price Curve**| +0.5đ | `chartPriceCurve` (`LineChart` JavaFX): Tự động vẽ thêm điểm biến thiên giá ngay khi nhận `BroadcastEvent(NEW_BID)`. |

---

## 🚀 3. HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY THỬ NGHIỆM

### Bước 1: Mở dự án trong IntelliJ IDEA
1. Mở **IntelliJ IDEA** $\rightarrow$ chọn **Open** $\rightarrow$ chọn thư mục gốc chứa file `pom.xml`.
2. IntelliJ sẽ tự động nhận diện **Maven Multi-module Project** và tải các dependencies.

### Bước 2: Khởi động Server
- Tìm đến file: `auction-server/src/main/java/com/auction/server/ServerApp.java`.
- Nhấp chuột phải chọn **Run 'ServerApp.main()'**.
- Server sẽ lắng nghe kết nối tại cổng mặc định `8888` và tạo sẵn dữ liệu mẫu.

### Bước 3: Khởi động Client (Đấu giá tương tác trực tiếp)
- Tìm đến file: `auction-client/src/main/java/com/auction/client/Launcher.java`.
- Nhấp chuột phải chọn **Run 'Launcher.main()'**.
- Cửa sổ đăng nhập sẽ hiển thị:
  - Tài khoản Người mua 1: `bidder1` / mật khẩu: `123456`
  - Tài khoản Người mua 2: `bidder2` / mật khẩu: `123456`
  - Tài khoản Người bán (Seller): `seller1` / mật khẩu: `123456`
  - Tài khoản Quản trị (Admin): `admin` / mật khẩu: `admin123`

### Bước 4: Thử nghiệm tính năng Người bán (Seller) đăng sản phẩm
1. Đăng nhập với tài khoản `seller1` (mật khẩu `123456`).
2. Trên màn hình Dashboard, xuất hiện nút **"➕ Đăng sản phẩm mới"** (nút này chỉ hiển thị với vai trò `SELLER` hoặc `ADMIN`).
3. Nhấp vào nút, một cửa sổ đăng bán hiện đại sẽ xuất hiện:
   - Chọn phân loại sản phẩm: **Điện tử** (*Electronics*), **Nghệ thuật** (*Art*), **Phương tiện** (*Vehicle*). Hệ thống sẽ tự động cập nhật các trường chuyên biệt tương ứng (áp dụng **Factory Method Pattern**).
   - Nhập thông tin, giá khởi điểm, bước giá và thời lượng phiên đấu giá rồi bấm **"✔ Đăng sản phẩm & Mở phiên"**.
   - Phiên đấu giá mới sẽ được lập tức kích hoạt và xuất hiện trên danh sách cho mọi người mua tham gia đấu giá!

- **Thử nghiệm đối kháng**: Bạn có thể chạy `Launcher` 2 lần để mở 2 cửa sổ độc lập (1 cửa sổ đăng nhập `bidder1`, 1 cửa sổ đăng nhập `bidder2`). Cả hai cùng vào chung một phòng đấu giá:
  - Khi `bidder1` bấm **Đặt giá**, cửa sổ của `bidder2` sẽ tự động nhảy số tiền và đường biểu đồ `LineChart` được vẽ thêm ngay tức khắc mà không cần bấm F5!

---

## 🧪 4. CHẠY BỘ KIỂM THỬ (UNIT TEST)
Trong IntelliJ IDEA, vào thư mục:
- `auction-server/src/test/java/com/auction/server/`
- Chạy `AuctionServiceTest` để xác nhận kiểm tra nghiệp vụ và bắt ngoại lệ.
- Chạy `ConcurrencyBidTest` để xác nhận kiểm thử an toàn luồng với 20 thread chạy song song.
