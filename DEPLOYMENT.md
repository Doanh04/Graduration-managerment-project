# Hướng dẫn triển khai Graduration Managerment bằng Docker

Tài liệu này hướng dẫn triển khai toàn bộ hệ thống gồm:

- Frontend React/Vite được phục vụ bằng Nginx.
- Backend Spring Boot chạy Java 21.
- MySQL 8.4.
- MongoDB 7.0.
- Thư mục `storage` dùng để lưu các tệp người dùng tải lên.

## 1. Kiến trúc sau khi triển khai

```text
Trình duyệt
    |
    v
Frontend container (Nginx :80)
    |  /graduration/*
    v
Backend container (Spring Boot :8080)
    |                    |
    v                    v
MySQL container       MongoDB container
```

Frontend dùng đường dẫn `/graduration` để gọi API. Nginx chuyển tiếp đường dẫn này đến backend nên trình duyệt và API dùng cùng origin, phù hợp với đăng nhập bằng cookie.

## 2. Yêu cầu máy chủ

Khuyến nghị Ubuntu 22.04/24.04 với tối thiểu:

- 2 CPU.
- 4 GB RAM.
- 20 GB dung lượng trống.
- Tên miền trỏ về IP máy chủ nếu muốn dùng HTTPS.

Cài Docker Engine và Docker Compose plugin trên Ubuntu:

```bash
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Đăng xuất rồi đăng nhập lại để quyền nhóm Docker có hiệu lực. Kiểm tra:

```bash
docker --version
docker compose version
```

## 3. Lấy mã nguồn

```bash
cd /opt
sudo git clone <URL_REPOSITORY> graduration-managerment-project
sudo chown -R "$USER":"$USER" /opt/graduration-managerment-project
cd /opt/graduration-managerment-project
```

Nếu mã nguồn đã được chép lên server, chỉ cần đi tới thư mục gốc chứa `docker-compose.yml`.

Kiểm tra cấu trúc tối thiểu:

```text
docker-compose.yml
server-springboot/Dockerfile
client-react/graduration-client/Dockerfile
client-react/graduration-client/nginx.conf
```

## 4. Cấu hình biến môi trường

Tạo file cấu hình thật từ file mẫu:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Ví dụ cấu hình production:

```dotenv
MYSQL_DATABASE=graduration-managerment
MYSQL_ROOT_PASSWORD=<mat-khau-mysql-manh>

MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=<mat-khau-mongodb-khong-co-ky-tu-dac-biet>
MONGO_URI=mongodb://admin:<mat-khau-mongodb-da-url-encode>@mongodb:27017/graduration_managerment?authSource=admin

BACKEND_PORT=8080
FRONTEND_PORT=80
APP_COOKIE_SECURE=true
JPA_DDL_AUTO=update
```

Lưu ý:

- Không commit `.env` lên Git.
- Nếu mật khẩu MongoDB có `@`, đổi thành `%40`; `#` thành `%23`; `/` thành `%2F` trong `MONGO_URI`.
- `MONGO_ROOT_PASSWORD` phải giống mật khẩu trong `MONGO_URI`.
- `APP_COOKIE_SECURE=true` chỉ dùng khi truy cập qua HTTPS. Nếu đang test bằng HTTP, đặt `false`.
- `JPA_DDL_AUTO=update` phù hợp giai đoạn test. Production nên chuyển sang `validate` sau khi có quy trình migration schema riêng.

## 5. Mở firewall

Nếu frontend được truy cập trực tiếp bằng HTTP:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Không cần mở cổng 3306 hoặc 27017 vì MySQL và MongoDB chỉ được dùng trong mạng Docker nội bộ.

## 6. Build và khởi động

Từ thư mục gốc dự án:

```bash
docker compose up -d --build
```

Lệnh này sẽ:

1. Tải image MySQL và MongoDB.
2. Build backend bằng Maven/Java 21.
3. Build frontend bằng Node/Vite.
4. Khởi động database.
5. Chờ database healthy.
6. Khởi động backend và frontend.

Kiểm tra trạng thái:

```bash
docker compose ps
```

Xem log:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql mongodb
```

## 7. Kiểm tra sau triển khai

Kiểm tra frontend:

```bash
curl -I http://SERVER_IP/
```

Kiểm tra backend qua Nginx:

```bash
curl -I http://SERVER_IP/graduration/
```

Mở trình duyệt:

```text
http://SERVER_IP/
```

Đăng nhập bằng tài khoản quản trị đã được cấu hình trong database. Nếu frontend chạy cổng khác, dùng:

```text
http://SERVER_IP:<FRONTEND_PORT>/
```

## 8. Dùng tên miền và HTTPS

Trỏ bản ghi DNS `A` của tên miền về IP server. Nếu dùng `FRONTEND_PORT=80`, frontend container đã lắng nghe cổng 80 của máy chủ.

Có thể đặt Nginx/Caddy/Traefik ở phía trước để cấp TLS. Khi HTTPS hoạt động:

```dotenv
APP_COOKIE_SECURE=true
```

Sau khi đổi `.env`, khởi động lại backend:

```bash
docker compose up -d --force-recreate backend frontend
```

Nếu dùng reverse proxy ngoài Docker, proxy toàn bộ request đến `http://127.0.0.1:80` và giữ nguyên đường dẫn `/graduration`.

## 9. Dữ liệu được lưu ở đâu

Compose sử dụng các vùng lưu trữ bền vững:

```text
mysql_data  -> dữ liệu MySQL
mongo_data  -> dữ liệu MongoDB/audit log
./storage   -> tệp tải lên của ứng dụng
```

Không dùng `docker compose down -v` trên production nếu chưa sao lưu, vì tùy chọn `-v` sẽ xóa các named volume database.

## 10. Sao lưu database

### MySQL

```bash
mkdir -p backups
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  > backups/mysql-$(date +%F-%H%M).sql
```

### MongoDB

```bash
mkdir -p backups
docker compose exec -T mongodb mongodump \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --archive \
  > backups/mongodb-$(date +%F-%H%M).archive
```

### Tệp tải lên

```bash
tar -czf backups/storage-$(date +%F-%H%M).tar.gz storage/
```

Nên sao chép thư mục `backups` sang một máy hoặc object storage khác, không chỉ lưu trên cùng server.

## 11. Cập nhật phiên bản

```bash
cd /opt/graduration-managerment-project
git pull
docker compose up -d --build
docker compose ps
```

Theo dõi log sau cập nhật:

```bash
docker compose logs --tail=200 -f backend frontend
```

## 12. Dừng và khởi động lại

Khởi động lại container, giữ nguyên dữ liệu:

```bash
docker compose restart
```

Dừng hệ thống, giữ nguyên volumes:

```bash
docker compose down
```

Không chạy lệnh sau nếu chưa có bản sao lưu:

```bash
docker compose down -v
```

## 13. Xử lý lỗi thường gặp

### Cổng đã được sử dụng

```bash
sudo ss -ltnp | grep -E ':80|:8080|:5173'
```

Đổi `FRONTEND_PORT` hoặc `BACKEND_PORT` trong `.env`, rồi chạy lại:

```bash
docker compose up -d
```

### Backend không kết nối được MySQL/MongoDB

```bash
docker compose ps
docker compose logs mysql mongodb backend
```

Kiểm tra backend phải dùng hostname Docker là `mysql` và `mongodb`, không dùng `localhost`.

### MongoDB báo lỗi xác thực

Kiểm tra `MONGO_ROOT_PASSWORD` và `MONGO_URI`. Nếu mật khẩu chứa ký tự đặc biệt, phải URL-encode trong URI. Nếu volume MongoDB đã được tạo trước đó, việc đổi biến môi trường không tự đổi mật khẩu cũ; cần dùng đúng mật khẩu ban đầu hoặc đổi mật khẩu trực tiếp trong MongoDB.

### Frontend gọi API về localhost

Frontend production phải được build với:

```text
VITE_API_BASE_URL=/graduration
```

Build lại frontend:

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Upload file bị vượt kích thước

Backend và Nginx hiện cho phép tối đa 50 MB. Nếu có reverse proxy bên ngoài, cần đặt giới hạn request tối thiểu 50 MB ở proxy đó.

## 14. Kiểm tra image và dọn image cũ

```bash
docker image ls
docker system df
```

Chỉ dọn image không còn dùng sau khi chắc chắn không cần rollback:

```bash
docker image prune
```

## 15. Checklist production

- [ ] Đã đổi mật khẩu MySQL và MongoDB.
- [ ] `.env` có quyền `600` và không commit lên Git.
- [ ] DNS trỏ đúng IP server.
- [ ] HTTPS đã bật nếu hệ thống chạy Internet.
- [ ] `APP_COOKIE_SECURE=true` khi dùng HTTPS.
- [ ] Đã sao lưu MySQL, MongoDB và `storage`.
- [ ] Đã kiểm tra `docker compose ps` đều ở trạng thái running/healthy.
- [ ] Đã kiểm tra đăng nhập và upload file.
- [ ] Đã thiết lập lịch sao lưu tự động.

