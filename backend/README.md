# TikTok X — real backend

The project uses PHP 8+ and MySQL 8 on REG.RU.

1. Create/select database `u3696215_tiktokx`.
2. Import only `database/schema.sql` in phpMyAdmin.
3. Copy `backend/config.example.php` to a private `config.php` and enter the REG.RU database credentials there.
4. Deploy `backend/public/api.php` and `.htaccess` into the web root.
5. Never put the database password in Android code or commit it to GitHub.

API:
GET /api/health
POST /api/register — username, email, password
POST /api/login — login, password
GET /api/me — Bearer token
GET /api/feed — Bearer token
GET /api/search?q=... — Bearer token
POST /api/videos/{id}/like — Bearer token
POST /api/videos/{id}/comments — body
POST /api/users/{id}/follow — Bearer token
POST /api/logout — Bearer token

The server is the source of truth. The Android app must not contain database credentials.
