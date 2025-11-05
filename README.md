<div align="center">
<h1>JavaSMS - Ứng dụng Chat</h1>
<p>
Đây là một dự án ứng dụng chat Client-Server đầy đủ tính năng, được viết bằng Java. Toàn bộ giao tiếp giữa máy khách và máy chủ đều được mã hóa bằng SSL/TLS để đảm bảo an toàn.





Ứng dụng sử dụng JavaFX cho giao diện người dùng, máy chủ đa luồng (multi-threaded) để xử lý nhiều kết nối đồng thời, và cơ sở dữ liệu MySQL để lưu trữ thông tin người dùng, tin nhắn và các nhóm chat.
</p>
</div>

 <h2>Tính năng chính</h2>

<h3>1. Bảo mật</h3>
<ul>
<li><b>Mã hóa SSL/TLS:</b> Toàn bộ dữ liệu gửi đi (tin nhắn, mật khẩu, v.v.) đều được mã hóa an toàn.</li>
<li><b>Yêu cầu TrustStore/KeyStore:</b> Máy khách và máy chủ sử dụng tệp <code>.jks</code> để xác thực và thiết lập kết nối an toàn.</li>
</ul>

<h3>2. Xác thực &amp; Người dùng</h3>
<ul>
<li>Đăng ký tài khoản mới.</li>
<li>Đăng nhập an toàn.</li>
<li>Đăng xuất.</li>
<li>Hiển thị danh sách người dùng (Online/Offline) và trạng thái tùy chỉnh.</li>
<li>Cập nhật trạng thái (ví dụ: "Đang rảnh", "Đang bận").</li>
<li>Ghi nhận thời gian "last seen" khi đăng xuất.</li>
</ul>

<h3>3. Nhắn tin</h3>
<ul>
<li>Nhắn tin riêng tư (1-1): Chat riêng tư với người dùng khác.</li>
<li>Nhắn tin nhóm: Gửi và nhận tin nhắn trong các nhóm chat.</li>
<li>Lịch sử trò chuyện: Tải lại lịch sử tin nhắn khi mở một tab chat (cả riêng tư và nhóm).</li>
<li>Thông báo <b>"Đang gõ..."</b>: Hiển thị khi đối tác đang soạn tin nhắn.</li>
<li>Bộ chọn Emoji: Gửi emoji trong cuộc trò chuyện.</li>
<li>Thông báo tin nhắn chưa đọc: Các tab chat có tin nhắn mới sẽ được tô sáng.</li>
</ul>

<h3>4. Quản lý Nhóm</h3>
<ul>
<li>Tạo nhóm mới (người tạo tự động làm admin).</li>
<li>Tham gia một nhóm có sẵn.</li>
<li>Rời khỏi một nhóm.</li>
<li>Mời người dùng khác vào nhóm (<b>chỉ admin</b>).</li>
<li>Xem danh sách thành viên và admin của nhóm.</li>
<li>Kick thành viên: Xóa thành viên khỏi nhóm (<b>chỉ admin</b>).</li>
<li>Quản lý vai trò: Thăng/giáng cấp người dùng thành admin hoặc member (<b>chỉ admin</b>).</li>
</ul>

 <h2>Kiến trúc & Công nghệ</h2>

<ul>
<li><b>Ngôn ngữ:</b> Java</li>
<li><b>Giao diện Client (UI):</b> JavaFX</li>
<li><b>Phía Server:</b> Java đa luồng (Multi-threaded), SSLSockets</li>
<li><b>Phía Client (Logic):</b> Kiến trúc Controller (bộ não trung tâm) ủy quyền cho các lớp Logic chuyên biệt (<code>AuthenticationLogic</code>, <code>MessagingLogic</code>, v.v.).</li>
<li><b>Phía Server (Logic):</b> Kiến trúc ClientHandler (bộ não cho mỗi client) ủy quyền cho các lớp Handler chuyên biệt (<code>AuthenticationHandler</code>, <code>MessagingHandler</code>, v.v.).</li>
<li><b>Cơ sở dữ liệu:</b> MySQL (sử dụng JDBC).</li>
<li><b>Giao thức (Protocol):</b> Giao thức tùy chỉnh dựa trên Chuỗi (String) qua <code>PrintWriter</code> / <code>BufferedReader</code>. (ví dụ: <code>LOGIN:user:pass</code>, <code>GROUP_MSG:groupname:hello</code>).</li>
</ul>

 <h2>Hướng dẫn Cài đặt & Chạy</h2>

<details>
<summary><b>Nhấn vào đây để xem Hướng dẫn Cài đặt Chi tiết</b></summary>





Để chạy dự án này, bạn cần thiết lập cả Cơ sở dữ liệu, SSL và chạy Server trước khi chạy Client.

<h3>1. Yêu cầu</h3>
<ul>
<li>JDK 11 hoặc mới hơn (đã bao gồm JavaFX).</li>
<li>MySQL Server.</li>
<li>Một IDE (ví dụ: IntelliJ, Eclipse) hoặc Maven.</li>
</ul>

<h3>2. Thiết lập Cơ sở dữ liệu (MySQL)</h3>
<ol>
<li>Mở MySQL server của bạn.</li>
<li>Tạo một database mới:
<pre><code>CREATE DATABASE IF NOT EXISTS chat_db;
</code></pre>
</li>
<li>Chọn database:
<pre><code>USE chat_db;
</code></pre>
</li>
<li>Chạy toàn bộ các lệnh trong tệp <code>schema.sql</code> để tạo các bảng (users, messages, chat_groups, v.v.).</li>
<li><b>Quan trọng:</b> Mở tệp <code>ChatServer.java</code>, tìm đến phương thức <code>main</code>. Sửa đổi thông tin đăng nhập CSDL cho phù hợp với máy của bạn:
<pre lang="java">
// Sửa "root" và "123456" nếu cần
DBHelper db = new DBHelper("jdbc:mysql://localhost:3306/chat_db", "root", "123456");
</pre>
</li>
</ol>

<h3>3. Thiết lập SSL (KeyStores)</h3>
<p>Dự án này yêu cầu hai tệp KeyStore để chạy SSL. Bạn phải tự tạo chúng (ví dụ: sử dụng <code>keytool</code> của Java).</p>
<ul>
<li><code>server.jks</code>: Một KeyStore chứa khóa riêng (private key) và chứng chỉ của server.</li>
<li><code>client.jks</code>: Một TrustStore (kho lưu trữ tin cậy) chứa chứng chỉ công khai (public certificate) của server.</li>
</ul>
<p>Cả hai tệp này phải được đặt ở <b>thư mục gốc</b> của dự án (cùng cấp với <code>pom.xml</code> hoặc thư mục <code>src</code>). Mật khẩu được mã hóa cứng (hardcode) trong <code>ChatServer.java</code> và <code>ChatNetworkService.java</code> là <b>"123456"</b>.</p>

<h3>4. Chạy ứng dụng</h3>
<ol>
<li><b>Chạy Server:</b> Chạy phương thức <code>main</code> trong tệp <code>com.example.Server.ChatServer.java</code>. Bạn sẽ thấy thông báo "Chat server (SSL) started on port 5000" trên console.</li>
<li><b>Chạy Client:</b> Chạy phương thức <code>main</code> trong tệp <code>com.example.Client.ChatClientGUI.java</code>.</li>
<li>Bạn có thể chạy <code>ChatClientGUI.java</code> nhiều lần để mở nhiều cửa sổ chat và thử nghiệm.</li>
</ol>
</details>

 <h2>Cấu trúc Dự án</h2>

<pre>
JavaSMS/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/example/
│   │   │   │   ├── Client/
│   │   │   │   │   ├── ChatClientGUI.java        (Lớp UI JavaFX chính)
│   │   │   │   │   ├── ChatNetworkService.java   (Xử lý kết nối SSL & I/O)
│   │   │   │   │   └── ChatController/
│   │   │   │   │       ├── ChatController.java       (Bộ não trung tâm của Client)
│   │   │   │   │       ├── AuthenticationLogic.java  (Xử lý logic Đăng nhập/Ký)
│   │   │   │   │       ├── GroupLogic.java           (Xử lý logic Nhóm)
│   │   │   │   │       ├── MessagingLogic.java       (Xử lý logic Tin nhắn)
│   │   │   │   │       └── UtilityLogic.java         (Xử lý logic Trạng thái, Tabs)
│   │   │   │   │
│   │   │   │   ├── Server/
│   │   │   │   │   ├── ChatServer.java           (Lớp Server chính, lắng nghe kết nối)
│   │   │   │   │   └── DBHelper.java             (Lớp truy cập CSDL qua JDBC)
│   │   │   │   │
│   │   │   │   └── ClientHandler/
│   │   │   │       ├── ClientHandler.java        (Bộ não cho mỗi client trên server)
│   │   │   │       ├── AuthenticationHandler.java(Xử lý nghiệp vụ Đăng nhập/Ký)
│   │   │   │       ├── GroupManagementHandler.java (Xử lý nghiệp vụ Nhóm)
│   │   │   │       ├── MessagingHandler.java       (Xử lý nghiệp vụ Tin nhắn)
│   │   │   │       └── UtilityHandler.java         (Xử lý nghiệp vụ Trạng thái, Lịch sử)
│   │   │
│   │   └── resources/
│
├── client.jks  (TrustStore của Client)
├── server.jks  (KeyStore của Server)
├── schema.sql  (Script thiết lập CSDL)
└── pom.xml     (Nếu bạn dùng Maven)
</pre>
