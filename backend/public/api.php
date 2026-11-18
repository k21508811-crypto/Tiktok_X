<?php
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
$configFile=dirname(__DIR__).'/config.php';
if(!is_file($configFile)){http_response_code(503);echo json_encode(['ok'=>false,'error'=>'Server is not configured']);exit;}
$config=require $configFile;
function out(array $d,int $s=200):never{http_response_code($s);echo json_encode($d,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);exit;}
function db():PDO{static $p;global $config;if($p)return $p;$p=new PDO("mysql:host={$config['db_host']};port={$config['db_port']};dbname={$config['db_name']};charset=utf8mb4",$config['db_user'],$config['db_pass'],[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]);return $p;}
function body():array{$x=json_decode(file_get_contents('php://input')?:'{}',true);return is_array($x)?$x:[];}
function token():?string{$h=$_SERVER['HTTP_AUTHORIZATION']??'';return preg_match('/Bearer\s+([A-Za-z0-9]+)$/i',$h,$m)?$m[1]:null;}
function user():?array{$t=token();if(!$t)return null;$q=db()->prepare("SELECT u.id,u.username,u.email,p.display_name,p.bio,p.avatar_path,p.is_verified FROM user_tokens t JOIN users u ON u.id=t.user_id LEFT JOIN profiles p ON p.user_id=u.id WHERE t.token_hash=? AND t.expires_at>UTC_TIMESTAMP() AND u.status='active' LIMIT 1");$q->execute([hash('sha256',$t)]);return $q->fetch()?:null;}
function auth():array{$u=user();if(!$u)out(['ok'=>false,'error'=>'Authentication required'],401);return $u;}
function issue(int $id):string{$t=bin2hex(random_bytes(32));db()->prepare("INSERT INTO user_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 30 DAY))")->execute([$id,hash('sha256',$t)]);return $t;}
$m=$_SERVER['REQUEST_METHOD']??'GET';$path=trim(parse_url($_SERVER['REQUEST_URI']??'/',PHP_URL_PATH),'/');$b=body();
try{
if($m==='GET'&&$path==='api/health'){db()->query('SELECT 1');out(['ok'=>true,'service'=>'TikTok X API']);}
if($m==='POST'&&$path==='api/register'){
 $u=strtolower(trim((string)($b['username']??'')));$e=strtolower(trim((string)($b['email']??'')));$pw=(string)($b['password']??'');
 if(!preg_match('/^[a-z0-9_]{3,30}$/',$u)||!filter_var($e,FILTER_VALIDATE_EMAIL)||strlen($pw)<10)out(['ok'=>false,'error'=>'Username/email/password invalid'],422);
 try{db()->beginTransaction();$q=db()->prepare("INSERT INTO users(username,email,password_hash) VALUES(?,?,?)");$q->execute([$u,$e,password_hash($pw,PASSWORD_DEFAULT)]);$id=(int)db()->lastInsertId();db()->prepare("INSERT INTO profiles(user_id,display_name) VALUES(?,?)")->execute([$id,$u]);db()->commit();out(['ok'=>true,'token'=>issue($id),'user'=>['id'=>$id,'username'=>$u,'email'=>$e]],201);}catch(Throwable $x){if(db()->inTransaction())db()->rollBack();out(['ok'=>false,'error'=>'Username or email already exists'],409);}
}
if($m==='POST'&&$path==='api/login'){
 $login=strtolower(trim((string)($b['login']??'')));$pw=(string)($b['password']??'');$q=db()->prepare("SELECT id,username,email,password_hash FROM users WHERE (username=? OR email=?) AND status='active' LIMIT 1");$q->execute([$login,$login]);$u=$q->fetch();if(!$u||!password_verify($pw,$u['password_hash']))out(['ok'=>false,'error'=>'Invalid login or password'],401);out(['ok'=>true,'token'=>issue((int)$u['id']),'user'=>['id'=>(int)$u['id'],'username'=>$u['username'],'email'=>$u['email']]]);
}
if($m==='GET'&&$path==='api/me'){out(['ok'=>true,'user'=>auth()]);}
if($m==='GET'&&$path==='api/feed'){auth();$q=db()->query("SELECT v.id,u.username,p.display_name,p.avatar_path,p.is_verified,v.caption,v.video_path,v.thumbnail_path,v.views_count,v.created_at,(SELECT COUNT(*) FROM likes l WHERE l.video_id=v.id) likes_count,(SELECT COUNT(*) FROM comments c WHERE c.video_id=v.id AND c.status='visible') comments_count FROM videos v JOIN users u ON u.id=v.user_id LEFT JOIN profiles p ON p.user_id=u.id WHERE v.status='published' AND v.visibility='public' ORDER BY v.created_at DESC LIMIT 50");out(['ok'=>true,'videos'=>$q->fetchAll()]);}
if($m==='GET'&&$path==='api/search'){auth();$q='%'.mb_substr(trim((string)($_GET['q']??'')),0,60).'%';$s=db()->prepare("SELECT u.id,u.username,p.display_name,p.avatar_path,p.is_verified FROM users u LEFT JOIN profiles p ON p.user_id=u.id WHERE u.status='active' AND (u.username LIKE ? OR p.display_name LIKE ?) ORDER BY u.username LIMIT 30");$s->execute([$q,$q]);out(['ok'=>true,'users'=>$s->fetchAll()]);}
if($m==='POST'&&preg_match('#^api/videos/(\d+)/like$#',$path,$x)){ $u=auth();$id=(int)$x[1];$q=db()->prepare('SELECT 1 FROM likes WHERE user_id=? AND video_id=?');$q->execute([$u['id'],$id]);if($q->fetchColumn()){db()->prepare('DELETE FROM likes WHERE user_id=? AND video_id=?')->execute([$u['id'],$id]);$liked=false;}else{db()->prepare('INSERT INTO likes(user_id,video_id) VALUES(?,?)')->execute([$u['id'],$id]);$liked=true;}out(['ok'=>true,'liked'=>$liked]);}
if($m==='POST'&&preg_match('#^api/users/(\d+)/follow$#',$path,$x)){ $u=auth();$id=(int)$x[1];if($id===$u['id'])out(['ok'=>false,'error'=>'Cannot follow yourself'],422);$q=db()->prepare('SELECT 1 FROM follows WHERE follower_id=? AND following_id=?');$q->execute([$u['id'],$id]);if($q->fetchColumn()){db()->prepare('DELETE FROM follows WHERE follower_id=? AND following_id=?')->execute([$u['id'],$id]);$following=false;}else{db()->prepare('INSERT INTO follows(follower_id,following_id) VALUES(?,?)')->execute([$u['id'],$id]);$following=true;}out(['ok'=>true,'following'=>$following]);}
if($m==='POST'&&preg_match('#^api/videos/(\d+)/comments$#',$path,$x)){ $u=auth();$text=trim((string)($b['body']??''));if($text==='')out(['ok'=>false,'error'=>'Empty comment'],422);$q=db()->prepare('INSERT INTO comments(user_id,video_id,body) VALUES(?,?,?)');$q->execute([$u['id'],(int)$x[1],mb_substr($text,0,1000)]);out(['ok'=>true,'id'=>(int)db()->lastInsertId()],201);}
if($m==='POST'&&$path==='api/logout'){if($t=token())db()->prepare('DELETE FROM user_tokens WHERE token_hash=?')->execute([hash('sha256',$t)]);out(['ok'=>true]);}
out(['ok'=>false,'error'=>'Not found'],404);
}catch(Throwable $e){error_log($e->getMessage());out(['ok'=>false,'error'=>'Internal server error'],500);}
