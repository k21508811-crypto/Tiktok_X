# TikTok X backend (REG.RU)

## 1. Database

In REG.RU phpMyAdmin, select the database **u3696215_tiktokx**, open the **SQL** tab, paste the contents of `database/schema.sql`, and execute it. The file intentionally does not create a database: the REG.RU database already exists.

The schema targets MySQL 8.0.25 and uses ordinary `CREATE TABLE IF NOT EXISTS` statements. Do not paste Markdown fences or the explanatory text from this README into the SQL editor—paste only the contents of `schema.sql`.

## 2. PHP configuration

Requirements: PHP 8.1+, PDO MySQL, and mbstring.

Copy `config.example.php` to `config.php`. Fill in the database username and password shown in REG.RU. Keep `config.php` outside the public web directory if your hosting layout allows it. Never commit the real password to GitHub.

Known database settings:
- Host: `127.0.0.1`
- Port: `3306`
- Database: `u3696215_tiktokx`
- MySQL: `8.0.25`

## 3. Deploy the API

Upload the contents of `backend/public/` to a web-accessible directory such as `public_html/api/`, and place the private `config.php` one directory above that public folder. Keep the supplied `.htaccess` alongside `api.php`.

The API routes include:
- `GET /api/health`
- `POST /api/register` with JSON `username,email,password`
- `POST /api/login` with JSON `login,password`
- `GET /api/me` with `Authorization: Bearer TOKEN`
- `GET /api/search?q=username`
- `GET /api/feed`
- `POST /api/videos/{id}/like`
- `POST /api/videos/{id}/comments` with JSON `body`
- `POST /api/users/{id}/follow`
- `POST /api/verification-requests` with JSON `reason`
- `POST /api/logout`

This is the first backend foundation. Video upload, admin moderation UI, music upload/catalogue, and Android-to-API integration still need implementation. The Android prototype currently uses demonstration clips and local-only profile state; do not treat those screens as connected to the database yet.
