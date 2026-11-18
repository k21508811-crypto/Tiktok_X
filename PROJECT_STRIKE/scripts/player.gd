extends CharacterBody3D

const SPEED := 6.2
const JUMP_VELOCITY := 4.5
const MOUSE_SENSITIVITY := 0.0022
const MAGAZINE_SIZE := 30

var health := 100
var armor := 100
var ammo := MAGAZINE_SIZE
var reserve_ammo := 90
var reloading := false
var gravity: float = ProjectSettings.get_setting("physics/3d/default_gravity")
var camera: Camera3D
var fire_cooldown := 0.0

func _ready() -> void:
	var collision := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.38
	capsule.height = 1.8
	collision.shape = capsule
	collision.position.y = 0.9
	add_child(collision)
	camera = Camera3D.new()
	camera.position.y = 1.55
	camera.current = true
	camera.name = "Camera3D"
	add_child(camera)
	if not DisplayServer.is_touchscreen_available():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		rotate_y(-event.relative.x * MOUSE_SENSITIVITY)
		camera.rotate_x(-event.relative.y * MOUSE_SENSITIVITY)
		camera.rotation.x = clampf(camera.rotation.x, -1.45, 1.45)
	if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _physics_process(delta: float) -> void:
	fire_cooldown = maxf(0.0, fire_cooldown - delta)
	if not is_on_floor():
		velocity.y -= gravity * delta
	if Input.is_action_just_pressed("jump") and is_on_floor():
		velocity.y = JUMP_VELOCITY
	var input_dir := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	var direction := (transform.basis * Vector3(input_dir.x, 0, input_dir.y)).normalized()
	if direction:
		velocity.x = direction.x * SPEED
		velocity.z = direction.z * SPEED
	else:
		velocity.x = move_toward(velocity.x, 0, SPEED * 7 * delta)
		velocity.z = move_toward(velocity.z, 0, SPEED * 7 * delta)
	move_and_slide()
	if Input.is_action_pressed("fire"):
		_fire()
	if Input.is_action_just_pressed("reload"):
		_reload()
	_update_hud()

func _fire() -> void:
	if reloading or fire_cooldown > 0.0 or ammo <= 0:
		return
	ammo -= 1
	fire_cooldown = 0.105
	var query := PhysicsRayQueryParameters3D.create(
		camera.global_position,
		camera.global_position - camera.global_basis.z * 120.0
	)
	query.exclude = [self]
	var result := get_world_3d().direct_space_state.intersect_ray(query)
	if result and result.collider.has_method("take_damage"):
		result.collider.take_damage(34)

func _reload() -> void:
	if reloading or ammo == MAGAZINE_SIZE or reserve_ammo <= 0:
		return
	reloading = true
	await get_tree().create_timer(1.45).timeout
	var needed := MAGAZINE_SIZE - ammo
	var loaded: int = mini(needed, reserve_ammo)
	ammo += loaded
	reserve_ammo -= loaded
	reloading = false

func _update_hud() -> void:
	var root := get_tree().current_scene
	if root.has_method("update_hud"):
		root.update_hud("PROJECT STRIKE  |  TRAINING\nHP %d   ARMOR %d   AMMO %d/%d%s" % [
			health, armor, ammo, reserve_ammo, "  RELOADING" if reloading else ""
		])
