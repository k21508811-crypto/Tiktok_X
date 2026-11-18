<?php
declare(strict_types=1);

/*
 * TikTok X API — PHP 8.1+ / MySQL 8.
 * Copy config.example.php to config.php and fill in REG.RU credentials.
 * Keep config.php out of the public web root and never commit real passwords.
 */
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

$configFile = dirname(__DIR__) . '/config.php';
if (!is_file($configFile)) {
    http_response_code(503);
    echo json_encode(['ok' => false, 'error' => 'API is not configured. Copy config.example.php to config.php.']);
    exit;
}
$config = require $configFile;

function respond(array $data, int $status = 200): never {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}
function input(): array {
    $raw = file_get_contents('php://input');
    $data = json_decode($raw ?: '{}', true);
    return is_array($data) ? $data : [];
}
function db(): PDO {
    static $pdo = null;
    global $config;
    if ($pdo instanceof PDO) return $pdo;
    $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
        $config['db_host'], (int)$config['db_port'], $config['db_name']);
    $pdo = new PDO($dsn, $config['db_user'], $config['db_pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    return $pdo;
}
function bearer(): ?string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/Bearer\s+([A-Za-z0-9._-]+)/i', $header, $m)) return $m[1];
    return null;
}
function currentUser(): ?array {
    $token = bearer();
    if (!$token) return null;
    $stmt = db()->prepare('SELECT u.id,u.username,u.email,p.display_name,p.bio,p.avatar_path,p.is_verified
        FROM user_tokens t JOIN users u ON u.id=t.user_id
        LEFT JOIN profiles p ON p.user_id=u.id
        WHERE t.token_hash=? AND t.expires_at>UTC_TIMESTAMP() AND u.status=\'active\' LIMIT 1');
    $stmt->execute([hash('sha256', $token)]);
    return $stmt->fetch() ?: null;
}
function requireUser(): array {
    $user = currentUser();
    if (!$user) respond(['ok'=>false,'error'=>'Authentication required'], 401);
    return $user;
}
function issueToken(int $userId): string {
    $token = bin2hex(random_bytes(32));
    $stmt = db()->prepare('INSERT INTO user_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY))');
    $stmt->execute([$userId, hash('sha256', $token)]);
    return $token;
}
function cleanString(mixed $value, int $max): string {
    return mb_substr(trim((string)$value), 0, $max);
}

try {
    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $path = trim((string)(parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/'), '/');
    $body = input();

    if ($method === 'GET' && $path === 'api/health') {
        db()->query('SELECT 1');
        respond(['ok'=>true,'service'=>'TikTok X API','database'=>'connected']);
    }

    if ($method === 'POST' && $path === 'api/register') {
        $username = strtolower(cleanString($body['username'] ?? '', 30));
        $email = strtolower(cleanString($body['email'] ?? '', 190));
        $password = (string)($body['password'] ?? '');
        if (!preg_match('/^[a-z0-9_]{3,30}$/', $username)) respond(['ok'=>false,'error'=>'Username must be 3–30 characters: letters, numbers, underscore'], 422);
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) respond(['ok'=>false,'error'=>'Enter a valid email'], 422);
        if (strlen($password) < 10 || strlen($password) > 200) respond(['ok'=>false,'error'=>'Password must be at least 10 characters'], 422);
        $pdo = db();
        $pdo->beginTransaction();
        try {
            $stmt = $pdo->prepare('INSERT INTO users(username,email,password_hash) VALUES(?,?,?)');
            $stmt->execute([$username,$email,password_hash($password,PASSWORD_DEFAULT)]);
            $id = (int)$pdo->lastInsertId();
            $pdo->prepare('INSERT INTO profiles(user_id,display_name) VALUES(?,?)')->execute([$id,$username]);
            $pdo->commit();
        } catch (Throwable $e) {
            $pdo->rollBack();
            if ($e instanceof PDOException && (string)$e->getCode() === '23000') respond(['ok'=>false,'error'=>'Username or email is already registered'], 409);
            throw $e;
        }
        respond(['ok'=>true,'token'=>issueToken($id),'user'=>['id'=>$id,'username'=>$username,'email'=>$email]], 201);
    }

    if ($method === 'POST' && $path === 'api/login') {
        $login = strtolower(cleanString($body['login'] ?? '',190));
        $password = (string)($body['password'] ?? '');
        $stmt = db()->prepare('SELECT id,username,email,password_hash FROM users WHERE (username=? OR email=?) AND status=\'active\' LIMIT 1');
        $stmt->execute([$login,$login]);
        $user = $stmt->fetch();
        if (!$user || !password_verify($password,$user['password_hash'])) respond(['ok'=>false,'error'=>'Invalid login or password'],401);
        respond(['ok'=>true,'token'=>issueToken((int)$user['id']),'user'=>['id'=>(int)$user['id'],'username'=>$user['username'],'email'=>$user['email']]]);
    }

    if ($method === 'GET' && $path === 'api/me') {
        $user = requireUser();
        respond(['ok'=>true,'user'=>$user]);
    }

    if ($method === 'GET' && $path === 'api/search') {
        $q = cleanString($_GET['q'] ?? '',60);
        if ($q === '') respond(['ok'=>true,'users'=>[]]);
        $stmt = db()->prepare('SELECT u.id,u.username,p.display_name,p.avatar_path,p.is_verified
            FROM users u LEFT JOIN profiles p ON p.user_id=u.id
            WHERE u.status=\'active\' AND (u.username LIKE ? OR p.display_name LIKE ?)
            ORDER BY u.username LIMIT 30');
        $like = '%'.$q.'%'; $stmt->execute([$like,$like]);
        respond(['ok'=>true,'users'=>$stmt->fetchAll()]);
    }

    if ($method === 'GET' && $path === 'api/feed') {
        $limit = max(1,min(30,(int)($_GET['limit'] ?? 15)));
        $stmt = db()->prepare('SELECT v.id,v.user_id,u.username,p.display_name,p.avatar_path,p.is_verified,
            v.caption,v.video_path,v.thumbnail_path,v.views_count,v.created_at,
            (SELECT COUNT(*) FROM likes l WHERE l.video_id=v.id) AS likes_count,
            (SELECT COUNT(*) FROM comments c WHERE c.video_id=v.id AND c.status=\'visible\') AS comments_count
            FROM videos v JOIN users u ON u.id=v.user_id LEFT JOIN profiles p ON p.user_id=u.id
            WHERE v.status=\'published\' AND v.visibility=\'public\'
            ORDER BY v.created_at DESC LIMIT '.$limit);
        $stmt->execute();
        respond(['ok'=>true,'videos'=>$stmt->fetchAll()]);
    }

    if ($method === 'POST' && preg_match('#^api/videos/(\d+)/like$#',$path,$m)) {
        $user = requireUser(); $videoId=(int)$m[1];
        $check=db()->prepare('SELECT id FROM videos WHERE id=? AND status=\'published\'');
        $check->execute([$videoId]); if(!$check->fetch()) respond(['ok'=>false,'error'=>'Video not found'],404);
        $pdo=db(); $exists=$pdo->prepare('SELECT 1 FROM likes WHERE user_id=? AND video_id=?');
        $exists->execute([$user['id'],$videoId]);
        if($exists->fetchColumn()){$pdo->prepare('DELETE FROM likes WHERE user_id=? AND video_id=?')->execute([$user['id'],$videoId]);$liked=false;}
        else{$pdo->prepare('INSERT INTO likes(user_id,video_id) VALUES(?,?)')->execute([$user['id'],$videoId]);$liked=true;}
        respond(['ok'=>true,'liked'=>$liked]);
    }

    if ($method === 'POST' && preg_match('#^api/videos/(\d+)/comments$#',$path,$m)) {
        $user=requireUser(); $videoId=(int)$m[1]; $comment=cleanString($body['body']??'',1000);
        if($comment==='') respond(['ok'=>false,'error'=>'Comment cannot be empty'],422);
        $check=db()->prepare('SELECT id FROM videos WHERE id=? AND status=\'published\'');$check->execute([$videoId]);
        if(!$check->fetch()) respond(['ok'=>false,'error'=>'Video not found'],404);
        $stmt=db()->prepare('INSERT INTO comments(user_id,video_id,body) VALUES(?,?,?)');
        $stmt->execute([$user['id'],$videoId,$comment]);
        respond(['ok'=>true,'comment_id'=>(int)db()->lastInsertId()],201);
    }

    if ($method === 'POST' && preg_match('#^api/users/(\d+)/follow$#',$path,$m)) {
        $user=requireUser(); $target=(int)$m[1];
        if($target===(int)$user['id']) respond(['ok'=>false,'error'=>'You cannot follow yourself'],422);
        $check=db()->prepare('SELECT id FROM users WHERE id=? AND status=\'active\'');$check->execute([$target]);
        if(!$check->fetch()) respond(['ok'=>false,'error'=>'User not found'],404);
        $pdo=db();$q=$pdo->prepare('SELECT 1 FROM follows WHERE follower_id=? AND following_id=?');$q->execute([$user['id'],$target]);
        if($q->fetchColumn()){$pdo->prepare('DELETE FROM follows WHERE follower_id=? AND following_id=?')->execute([$user['id'],$target]);$following=false;}
        else{$pdo->prepare('INSERT INTO follows(follower_id,following_id) VALUES(?,?)')->execute([$user['id'],$target]);$following=true;}
        respond(['ok'=>true,'following'=>$following]);
    }

    if ($method === 'POST' && $path === 'api/verification-requests') {
        $user=requireUser();$reason=cleanString($body['reason']??'',3000);
        if($reason==='') respond(['ok'=>false,'error'=>'Describe why you are requesting verification'],422);
        $stmt=db()->prepare('INSERT INTO verification_requests(user_id,reason) VALUES(?,?)');
        $stmt->execute([$user['id'],$reason]);
        respond(['ok'=>true,'request_id'=>(int)db()->lastInsertId()],201);
    }

    if ($method === 'POST' && $path === 'api/logout') {
        $token=bearer(); if($token) db()->prepare('DELETE FROM user_tokens WHERE token_hash=?')->execute([hash('sha256',$token)]);
        respond(['ok'=>true]);
    }

    respond(['ok'=>false,'error'=>'Endpoint not found'],404);
} catch (Throwable $e) {
    error_log('TikTok X API: '.$e->getMessage());
    respond(['ok'=>false,'error'=>'Internal server error'],500);
}
