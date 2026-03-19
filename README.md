# TravelMate - Nền tảng Booking Khách sạn & Homestay (Đồ án cơ sở)

Đây là repository lưu trữ mã nguồn Website Đặt phòng Khách sạn (TravelMate) - Sản phẩm Đồ án cơ sở của nhóm.

## 👥 Thành viên nhóm
1. [Tên bạn 1] - Đảm nhiệm HTML/CSS/UI
2. [Tên bạn 2]
3. [Tên bạn 3]

## 🛠️ Công nghệ sử dụng (Giai đoạn 1: Frontend)
- HTML5 / CSS3 thuần
- Giao diện thân thiện, sử dụng CSS Grid/Flexbox
- Cấu trúc thư mục chia Component & Layout chuẩn mực để dễ dàng ghép với Backend sau này.

## 📂 Tổ chức mã nguồn
- `/assets/`: Nơi chứa toàn bộ tài nguyên chung (css, images, js).
- `/user/`: Các trang giao diện dành cho UI của khách hàng.
- `/admin/`: Các trang giao diện cho luồng Quản trị viên (Dashboard).
- `index.html`: Landing Page của toàn bộ dự án.

## 🚀 Hướng dẫn chạy dự án (Local)
1. Cài đặt **Visual Studio Code**.
2. Cài đặt Extension **Live Server** (của nhà phát triển Ritwick Dey).
3. Mở thư mục gốc của dự án này trong VS Code.
4. Chuột phải vào file `index.html` và chọn **"Open with Live Server"**.

## 💡 Quy tắc làm việc nhóm (Git Workflow)
Để tránh code đè lên nhau (conflict), cả team hãy làm theo luồng này:
1. Lúc nào cũng pull code mới nhất trước khi làm việc: `git pull origin main`
2. Tạo nhánh riêng để làm tính năng (vd: tính năng đăng nhập): `git checkout -b feature/login-page`
3. Sau khi làm xong:
   ```bash
   git add .
   git commit -m "feat: hoàn thiện UI trang đăng nhập"
   git push origin feature/login-page
   ```
4. Cuối cùng, mở Pull Request trên GitHub để gộp vào nhánh `main`.

---
*Developed with ❤️  by Team TravelMate*
