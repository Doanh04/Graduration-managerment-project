# Triển khai hệ thống lên server bằng Docker

## Kiến trúc production

```text
Internet
   |
   v
Frontend Nginx (:80 trong container)
   |  /graduration/*
   v
Backend Spring Boot (:8080, chỉ trong mạng Docker)
   |                         |
   v                         v
MySQL                     MongoDB
(không public port)        (không public port)
```

Chỉ frontend được publish ra máy chủ. Backend, MySQL và MongoDB không mở cổng trực tiếp ra Internet.

## 1. Chuẩn bị server

Khuyến nghị Ubuntu 22.04/24.04, tối thiểu 2 CPU, 4 GB RAM và 20 GB ổ đĩa. Cài Docker Engine và Docker Compose plugin, sau đó chép hoặc clone dự án vào server.

```bash
cd /opt/graduration-managerment-project
docker --version
docker compose version
```

## 2. Tạo cấu hình bí mật

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Tạo khóa JWT mới:

```bash
openssl rand -base64 48
```

Dán kết quả vào `JWT_SIGNER_KEY`. Thay toàn bộ giá trị `CHANGE_ME` trong `.env` bằng mật khẩu mạnh. Nếu mật khẩu MongoDB chứa ký tự đặc biệt, phần mật khẩu trong `MONGO_URI` phải URL-encode (`@` thành `%40`, `#` thành `%23`).

### Truy cập trực tiếp bằng IP và HTTP

```dotenv
FRONTEND_BIND=80
APP_COOKIE_SECURE=false
APP_CORS_ALLOWED_ORIGINS=
```

Truy cập bằng `http://SERVER_IP`.

### Dùng domain và HTTPS qua Nginx/Caddy trên host

```dotenv
FRONTEND_BIND=127.0.0.1:8088
APP_COOKIE_SECURE=true
APP_CORS_ALLOWED_ORIGINS=
```

Reverse proxy domain HTTPS đến `http://127.0.0.1:8088`. Frontend và API dùng cùng domain; API nằm tại `/graduration`, vì vậy không cần khai báo CORS. Không đặt `APP_COOKIE_SECURE=true` nếu người dùng vẫn truy cập bằng HTTP, nếu không trình duyệt sẽ không gửi cookie đăng nhập.

Ví dụ Nginx trên host:

```nginx
server {
    listen 80;
    server_name doan.example.edu.vn;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name doan.example.edu.vn;

    ssl_certificate /etc/letsencrypt/live/doan.example.edu.vn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/doan.example.edu.vn/privkey.pem;

    client_max_body_size 50m;

    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 3. Kiểm tra cấu hình trước khi chạy

```bash
docker compose config --quiet
```

Nếu thiếu mật khẩu hoặc khóa JWT, Compose sẽ dừng và báo đúng biến cần bổ sung.

## 4. Build và khởi động

```bash
mkdir -p storage
docker compose up -d --build
docker compose ps
```

Theo dõi log:

```bash
docker compose logs --tail=200 -f backend frontend
```

Kiểm tra từ server:

```bash
curl -I http://127.0.0.1:${FRONTEND_BIND##*:}/
```

Nếu `FRONTEND_BIND=80`, có thể kiểm tra bằng `curl -I http://127.0.0.1/`.

## 5. Firewall

Chỉ mở SSH, HTTP và HTTPS:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Không mở `3306`, `27017` hoặc `8080` trên production.

## 6. Cập nhật phiên bản

```bash
cd /opt/graduration-managerment-project
git pull
docker compose up -d --build
docker compose ps
```

Việc chạy lại `up -d --build` không xóa dữ liệu trong volumes.

## 7. Dữ liệu bền vững

- `mysql_data`: dữ liệu MySQL.
- `mongo_data`: dữ liệu MongoDB/thông báo/audit log.
- `./storage`: các file người dùng tải lên.

Không chạy `docker compose down -v` trên server nếu chưa sao lưu vì `-v` xóa volumes database.

## 8. Sao lưu

MySQL:

```bash
mkdir -p backups
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  > backups/mysql-$(date +%F-%H%M).sql
```

MongoDB:

```bash
docker compose exec -T mongodb sh -c \
  'mongodump --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --archive' \
  > backups/mongodb-$(date +%F-%H%M).archive
```

File tải lên:

```bash
tar -czf backups/storage-$(date +%F-%H%M).tar.gz storage/
```

## 9. Phát triển local khi cần

File production không public database/backend. Khi cần mở các cổng này để chạy frontend hoặc backend trực tiếp trên máy phát triển:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mongodb
```

Muốn chạy cả backend container và truy cập trực tiếp cổng `8080`:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

Các cổng development có thể đổi bằng `MYSQL_HOST_PORT`, `MONGO_HOST_PORT`, `BACKEND_HOST_PORT`.

## 10. Lưu ý khi dùng volume database cũ

Các biến `MYSQL_USER`, `MYSQL_PASSWORD`, `MONGO_INITDB_ROOT_*` chỉ khởi tạo tài khoản trong lần đầu tạo volume. Nếu mang volume đã tồn tại từ cấu hình cũ sang, cần giữ đúng mật khẩu cũ hoặc tự tạo/cập nhật tài khoản trong database trước khi đổi thông tin kết nối.
