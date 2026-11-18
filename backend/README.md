# TikTok X — backend deployment (REG.RU)

## Database

Database details for this project:
- Host: `127.0.0.1`
- Port: `3306`
- Database: `u3696215_tiktokx`
- User: `u3696215_tiktokx_user`

Import `database/schema.sql` in phpMyAdmin. It uses `CREATE TABLE IF NOT EXISTS`, so it can also add the music table to an existing installation.

## PHP deployment

1. Copy the contents of `backend/public/` to the website document root (or a dedicated subfolder).
2. Copy `backend/config.example.php` to `config.php` exactly one directory above the folder containing `api.php`. If you upload the contents of `backend/public/` directly into `public_html/`, put `config.php` one level above `public_html/`. If you keep the `backend/public/` folder structure, put it in `backend/config.php`.
3. Set the actual database password only in that server-side `config.php`. Never commit it to GitHub and never put it in the APK.
4. Enable PHP 8+, PDO MySQL, mbstring and fileinfo. Enable HTTPS.
5. For video upload, set PHP `upload_max_filesize` and `post_max_size` to at least `105M`; the API itself limits each video to 100 MB.
6. Ensure `backend/public/uploads/` is writable by PHP.

## Create the first administrator

After registering your own account in the app, run this SQL in phpMyAdmin, replacing `YOUR_USERNAME` with that exact username:

```sql
INSERT INTO admin_roles (user_id, role_name)
SELECT id, 'admin' FROM users WHERE username = 'YOUR_USERNAME'
ON DUPLICATE KEY UPDATE role_name = 'admin';
```

Admin page: `/admin.html`. Enter the API base URL (for example `https://your-domain.example/api`) and the token from your own login session.

## API routes

- `GET /api/health`
- `POST /api/register` — username, email, password (minimum 10 characters)
- `POST /api/login` — login, password
- `GET /api/me` — Bearer token
- `GET /api/feed` — Bearer token
- `GET /api/search?q=...` — Bearer token
- `POST /api/upload-video` — multipart fields `video` and `caption`, Bearer token
- `POST /api/videos/{id}/like` — Bearer token
- `GET /api/videos/{id}/comments` — Bearer token
- `POST /api/videos/{id}/comments` — body, Bearer token
- `POST /api/users/{id}/follow` — Bearer token
- `POST /api/verification` — reason, Bearer token
- `POST /api/reports` — target_type, target_id, reason, Bearer token
- `GET /api/admin/overview` — admin Bearer token
- `POST /api/admin/videos/{id}/status` — status, admin Bearer token
- `POST /api/admin/verification/{id}` — status, admin Bearer token
- `POST /api/admin/reports/{id}` — status, admin Bearer token
- `POST /api/logout` — Bearer token

The Android client asks for the API base URL on the first launch because the exact TikTok X domain has not been supplied in the project configuration yet.
