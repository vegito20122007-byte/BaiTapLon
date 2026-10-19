# BÁO CÁO ĐÁNH GIÁ MÃ NGUỒN VÀ DANH SÁCH LỖI TỒN ĐỌNG (CODE AUDIT REPORT)
> **Dự án**: Hệ thống Đấu giá Trực tuyến (Online Auction System)  
> **Môn học**: Lập trình nâng cao — Học kỳ II (2025–2026) — VNU-UET  
> **Thời điểm đánh giá**: Tuần 7  
> **Người thực hiện rà soát**: Nhóm phát triển (Antigravity Code Audit)

---

## 📌 TỔNG QUAN ĐÁNH GIÁ (EXECUTIVE SUMMARY)

Dự án đã dựng được **bộ khung kiến trúc (Scaffolding) rất tốt**:
- Chuẩn Maven Multi-module (`auction-common`, `auction-server`, `auction-client`).
- Giao diện JavaFX hiện đại, có FXML, SceneBuilder, CSS và biểu đồ `LineChart`.
- Giao thức Socket Client-Server truyền nhận JSON và cơ chế đẩy sự kiện thời gian thực (Observer Pattern).

**TUY NHIÊN**, do quá trình tạo sinh mã nguồn ("vibe-coding") dồn toàn bộ yêu cầu trong một lần sinh, hệ thống tồn tại **nhiều lỗi logic cốt lõi, tính năng "làm cảnh" (Stubs) và lỗ hổng kiến trúc**. Nếu mang nguyên trạng đi bảo vệ cuối kỳ, nhóm sẽ rất dễ bị giảng viên "bắt bài" khi trace luồng code.

---

## 🚨 PHẦN 1: CÁC LỖI LOGIC NGHIỆP VỤ NGHIÊM TRỌNG (CRITICAL BUGS)

### 1. `AutoBidEngine` — Tính năng "vong hồn" (+0.5đ trong barem nhưng không chạy)
- **Vị trí**: 
  - [`AutoBidEngine.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/core/AutoBidEngine.java)
  - [`ClientHandler.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/network/ClientHandler.java)
  - [`CommandType.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-common/src/main/java/com/auction/common/enums/CommandType.java)
- **Thực trạng**:
  - Class `AutoBidEngine` được viết rất bài bản với `PriorityQueue` sắp xếp theo `maxBid` và `FIFO`.
  - Nhưng trong toàn bộ dự án, phương thức `getSortedAutoBids()` **không hề được gọi ở bất cứ đâu**.
  - Lệnh `SET_AUTO_BID` có khai báo trong `CommandType`, nhưng trong `ClientHandler` **hoàn toàn không có switch-case xử lý lệnh này**.
  - Giao diện JavaFX (`auction-room.fxml`) không có nút hoặc ô nhập nào cho người dùng cài đặt giá trần tự động (`maxBid`).
  - Khi có một người đặt giá mới trong `placeBid()`, **không có cơ chế kích hoạt (Trigger)** để động cơ tự động trả giá thay cho người dùng khác.
- **Rủi ro**: Nếu giảng viên yêu cầu: *"Em hãy demo tính năng Đấu giá tự động cho thầy xem"*, hệ thống hoàn toàn bất lực.
- **Giải pháp**:
  1. Thêm trường nhập `maxBid` và nút *"Kích hoạt Auto-bid"* trên giao diện phòng đấu giá.
  2. Xử lý lệnh `SET_AUTO_BID` trong `ClientHandler`.
  3. Trong `AuctionManager.placeBid()`, sau khi cập nhật giá mới, gọi `autoBidEngine` để kiểm tra xem có ai đặt giá trần cao hơn không; nếu có thì tự động đặt tiếp một bước giá mới thay cho họ.

---

### 2. Bẫy đa hình: Tạo sản phẩm xong... ném vào sọt rác!
- **Vị trí**: 
  - [`AuctionService.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/service/AuctionService.java#L69-L74)
  - [`Auction.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-common/src/main/java/com/auction/common/model/Auction.java)
- **Đoạn mã có lỗi**:
  ```java
  // AuctionService.java - dòng 69
  Item item = ItemFactory.createItem(category, null, itemName, description, startingPrice, sellerId, extraParams);

  Auction auction = new Auction(null, item.getId(), item.getName(), sellerId, sellerUsername,
          startingPrice, increment, LocalDateTime.now(), LocalDateTime.now().plusMinutes(durationMinutes));

  auctionManager.startAuction(auction);
  return auction;
  ```
- **Thực trạng**:
  - `ItemFactory` khởi tạo rất đẹp các đối tượng con `ElectronicsItem`, `ArtItem`, `VehicleItem`.
  - Nhưng lúc truyền vào `createItem`, tham số `id` là `null`, nên `item.getId()` là `null`.
  - Hệ thống **không hề có `ItemDao`** hay nơi lưu trữ danh sách `Item`.
  - Đối tượng `Auction` chỉ lưu mỗi `itemName` và `itemId` (đang mang giá trị `null`). Ngay sau khi hàm kết thúc, đối tượng `item` (cùng các thuộc tính độc thù như `brand`, `warrantyMonths`, `artist`, `odometer`) bị bộ thu dọn rác (Garbage Collector) dọn sạch!
  - Vào giao diện phòng đấu giá, người dùng không thể xem được bất kỳ thông tin chi tiết nào về dòng sản phẩm đó.
- **Giải pháp**: 
  - Thêm thuộc tính `private Item item;` trực tiếp vào model `Auction` để lưu trữ đối tượng sản phẩm trọn vẹn, hoặc tạo `ItemDao` để lưu xuống file `data/items.json`.
  - Hiển thị thông số chi tiết (bảo hành, họa sĩ, số km đã đi) lên giao diện `auction-room.fxml`.

---

### 3. Đấu giá "tiền ảo" — Không kiểm tra số dư và không trừ tiền
- **Vị trí**:
  - [`AuctionManager.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/core/AuctionManager.java#L103)
  - [`Bidder.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-common/src/main/java/com/auction/common/model/Bidder.java)
- **Thực trạng**:
  - Model `Bidder` có trường `private double balance;` (ví dụ tài khoản có 50.000.000 VND).
  - Nhưng trong hàm `placeBid(auctionId, bidderId, bidderUsername, bidAmount)`:
    - Không hề gọi `UserDao` để kiểm tra xem `Bidder` này có đủ tiền trả hay không.
    - Một tài khoản có 10.000 VND vẫn có thể đặt giá 100.000.000.000 VND bình thường!
    - Khi phiên đấu giá kết thúc, người thắng không bị trừ tiền, người bán không nhận được tiền.
- **Giải pháp**:
  - Inject `UserService` vào `AuctionManager`.
  - Trước khi chấp nhận bid, kiểm tra `bidder.getBalance() >= bidAmount`.
  - Có cơ chế giữ tiền cọc (Hold deposit) hoặc trừ số dư khi thắng phiên.

---

### 4. Quả bom hẹn giờ trong dữ liệu mẫu (`data/auctions.json`)
- **Vị trí**:
  - [`data/auctions.json`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/data/auctions.json)
  - [`AuctionManager.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/core/AuctionManager.java#L203-L207)
- **Đoạn mã có lỗi**:
  ```java
  // AuctionManager.java - khôi phục scheduler lúc khởi động server
  long delaySeconds = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
  if (delaySeconds <= 0) {
      closeAuction(auction.getId());
      return;
  }
  ```
- **Thực trạng**:
  - File `data/auctions.json` được tạo từ ngày 17/10/2026, nên các phiên mẫu có `endTime` là `2026-10-17T11:30...` (đã trôi qua trong quá khứ).
  - Ngay khi bật Server lên, `AuctionManager` kiểm tra `delaySeconds <= 0` và **lập tức đóng toàn bộ 4 phiên mẫu sang trạng thái `FINISHED`**!
  - Khi mở Client lên, tất cả các phòng đều bị khóa, không thể đặt giá thử nghiệm được nữa.
- **Giải pháp**:
  - Không lưu thời gian quá khứ cố định vào file data.
  - Khi khởi tạo dữ liệu mẫu (Seed data), luôn dùng `LocalDateTime.now().plusMinutes(30)`.

---

### 5. Token đăng nhập sinh ra... chỉ để ngắm
- **Vị trí**:
  - [`UserService.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/service/UserService.java#L74)
  - [`ClientHandler.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/network/ClientHandler.java#L191)
- **Thực trạng**:
  - `UserService.login()` tạo UUID token và lưu vào `activeSessions`.
  - Tuy nhiên, trong tất cả các request tiếp theo (`PLACE_BID`, `CREATE_AUCTION`, `DELETE_USER`), Client **không hề gửi token lên Server**.
  - Server chỉ kiểm tra biến nội bộ của luồng `if (currentUser == null)`. Token hoàn toàn không có tác dụng bảo mật hay xác thực.
- **Giải pháp**:
  - Đưa `token` vào trường header của `RequestMessage`.
  - Server xác thực request thông qua `userService.getUserByToken(token)`.

---

## ⚡ PHẦN 2: CÁC VẤN ĐỀ HIỆU NĂNG & AN TOÀN LUỒNG (PERFORMANCE & CONCURRENCY)

### 6. Thắt cổ chai Synchronous Disk I/O trong Critical Section
- **Vị trí**: [`InMemoryAuctionDao.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/main/java/com/auction/server/dao/impl/InMemoryAuctionDao.java#L87)
- **Thực trạng**:
  - Trong phương thức `save(Auction)`:
    ```java
    storage.put(auction.getId(), auction);
    persistToFile(); // <-- Ghi toàn bộ dữ liệu ra đĩa đồng bộ!
    ```
  - Cứ mỗi 1 lượt đặt giá, Server phải serialize toàn bộ danh sách phiên ra JSON rồi ghi đè file `auctions.json` xuống ổ đĩa, ngay bên trong phạm vi khóa `ReentrantLock` của phiên đấu giá!
  - Khi có tranh chấp đặt giá ở những giây cuối (ví dụ 20-50 request/giây), thao tác Disk I/O đồng bộ này sẽ gây thắt cổ chai cực nặng và làm trễ các luồng khác.
- **Giải pháp**:
  - Tách việc ghi file ra luồng nền (Asynchronous / Debounce background thread) hoặc chỉ ghi định kỳ / khi server shutdown.

---

### 7. Nguy cơ treo UI ở `SocketClient.sendRequest()`
- **Vị trí**: [`SocketClient.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-client/src/main/java/com/auction/client/network/SocketClient.java#L94-L101)
- **Thực trạng**:
  - Hàm `sendRequest` được đánh dấu `synchronized` trên instance `SocketClient` và gọi `responseQueue.take()`.
  - `responseQueue.take()` là hàm blocking vô hạn (không có timeout).
  - Nếu Server bị quá tải, gặp ngoại lệ hoặc đứt kết nối mà không kịp trả lời, Client sẽ bị treo vĩnh viễn ở lời gọi này, đồng thời khóa cứng toàn bộ các controller khác muốn gửi request qua `SocketClient`.
- **Giải pháp**:
  - Sử dụng `responseQueue.poll(5, TimeUnit.SECONDS)`. Nếu quá thời gian thì ném ngoại lệ TimeoutException để UI thông báo cho người dùng.

---

### 8. File `build.bat` bị hardcode đường dẫn cục bộ
- **Vị trí**: [`build.bat`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/build.bat#L8)
- **Thực trạng**:
  - File script biên dịch lấy thư viện bằng cách trỏ thẳng vào `%USERPROFILE%\.m2\repository\...`.
  - Nếu thành viên khác trong nhóm hoặc giảng viên vừa clone dự án về máy mới chưa từng build qua Maven, thư mục `.m2` sẽ chưa có thư viện $\rightarrow$ chạy `build.bat` sẽ báo lỗi thiếu file `.jar` ngay lập tức.
- **Giải pháp**:
  - Bổ sung Maven Wrapper (`mvnw.cmd`) ở thư mục gốc để tự động tải Maven và dependency độc lập với môi trường máy.

---

## 🧪 PHẦN 3: LỖ HỔNG KIỂM THỬ (UNIT TEST)

### 9. Chiếc test Concurrency "bất tử" (Luôn xanh dù code sai)
- **Vị trí**: [`ConcurrencyBidTest.java`](file:///c:/Users/Hiep%20Pham/Desktop/mid%20term%20ltnc/test_demo/auction-server/src/test/java/com/auction/server/ConcurrencyBidTest.java#L65)
- **Đoạn mã có vấn đề**:
  ```java
  // Số bid thành công + số bid thất bại phải bằng tổng số request
  assertEquals(numberOfThreads, successfulBids.get() + failedBids.get());
  ```
- **Thực trạng**:
  - Trong vòng lặp 20 luồng: luồng nào chạy xong thì tăng `successfulBids` (trong khối `try`), luồng nào lỗi thì tăng `failedBids` (trong khối `catch`).
  - Tổng số `successful + failed` luôn luôn bằng 20 trong mọi trường hợp! Test này được AI viết ra để đảm bảo chạy trên GitHub Actions luôn báo xanh (Green test) chứ không hề kiểm tra tính đúng đắn của logic cạnh tranh.
- **Giải pháp**:
  - Thiết kế kịch bản test thực sự: 20 luồng cùng cố gắng đặt **cùng một mức giá khởi điểm**. Kết quả mong đợi: **Chỉ duy nhất 1 luồng thành công** (`successfulBids == 1`), 19 luồng còn lại phải nhận ngoại lệ `InvalidBidException` (`failedBids == 19`).

---
