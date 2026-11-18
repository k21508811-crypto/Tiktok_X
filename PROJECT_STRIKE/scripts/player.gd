extends CharacterBody3D

const SPEED := 6.2
const JUMP_VELOCITY := 4.5
const MOUSE_SENSITIVITY := 0.0022
const MAGAZINE_SIZE := 30
const RELOAD_TIME := 1.45
const RESPAWN_TIME := 3.0

var health := 100
var armor := 100
var ammo := MAGAZINE_SIZE
var reserve_ammo := 90
var reloading := false
var is_dead := false
var gravity: float = ProjectSettings.get_setting("physics/3d/default_gravity")
var camera: Camera3D
var collision_shape: CollisionShape3D
var fire_cooldown := 0.0
var weapon_root: Node3D
var hit_flash := 0.0

func _ready() -> void:
	_build_collision()
	_build_camera()
	_build_weapon()
	if not DisplayServer.is_touchscreen_available():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _build_collision() -> void:
	collision_shape = CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.38
	capsule.height = 1.8
	collision_shape.shape = capsule
	collision_shape.position.y = 0.9
	add_child(collision_shape)

func _build_camera() -> void:
	camera = Camera3D.new()
	camera.position.y = 1.55
	camera.current = true
	camera.name = "Camera3D"
	camera.fov = 82.0
	add_child(camera)

func _weapon_material(color: Color, metallic: float = 0.0) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.metallic = metallic
	mat.roughness = 0.48
	return mat

func _weapon_box(parent: Node3D, size: Vector3, pos: Vector3, color: Color, part_name: String) -> MeshInstance3D:
	var mesh := BoxMesh.new()
	mesh.size = size
	var instance := MeshInstance3D.new()
	instance.name = part_name
	instance.mesh = mesh
	instance.position = pos
	instance.material_override = _weapon_material(color, 0.18)
	parent.add_child(instance)
	return instance

func _build_weapon() -> void:
	weapon_root = Node3D.new()
	weapon_root.name = "FirstPersonRifle"
	weapon_root.position = Vector3(0.30, -0.28, -0.56)
	camera.add_child(weapon_root)
	_weapon_box(weapon_root, Vector3(0.13, 0.15, 0.62), Vector3(0, 0, 0), Color("#202831"), "Receiver")
	_weapon_box(weapon_root, Vector3(0.075, 0.075, 0.50), Vector3(0, 0.055, -0.48), Color("#151b22"), "Barrel")
	_weapon_box(weapon_root, Vector3(0.12, 0.23, 0.16), Vector3(0, -0.17, 0.08), Color("#343b3e"), "Magazine")
	_weapon_box(weapon_root, Vector3(0.09, 0.20, 0.13), Vector3(0, -0.16, 0.28), Color("#27313a"), "Grip")
	_weapon_box(weapon_root, Vector3(0.19, 0.045, 0.14), Vector3(0, 0.105, -0.02), Color("#59636c"), "Optic")
	_weapon_box(weapon_root, Vector3(0.15, 0.08, 0.13), Vector3(0, 0, 0.34), Color("#353c43"), "Stock")

func _unhandled_input(event: InputEvent) -> void:
	if is_dead:
		return
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		rotate_y(-event.relative.x * MOUSE_SENSITIVITY)
		camera.rotate_x(-event.relative.y * MOUSE_SENSITIVITY)
		camera.rotation.x = clampf(camera.rotation.x, -1.45, 1.45)
	if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _physics_process(delta: float) -> void:
	if is_dead:
		return
	fire_cooldown = maxf(0.0, fire_cooldown - delta)
	hit_flash = maxf(0.0, hit_flash - delta)
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
	weapon_root.position.z = -0.50
	var tween := create_tween()
	tween.tween_property(weapon_root, "position:z", -0.56, 0.09)
	var query := PhysicsRayQueryParameters3D.create(
		camera.global_position,
		camera.global_position - camera.global_basis.z * 120.0
	)
	query.exclude = [self]
	var result := get_world_3d().direct_space_state.intersect_ray(query)
	if not result.is_empty():
		var target = result.get("collider")
		if target and target.has_method("take_damage"):
			target.take_damage(34)
	_update_hud()

func _reload() -> void:
	if reloading or is_dead or ammo == MAGAZINE_SIZE or reserve_ammo <= 0:
		return
	reloading = true
	_update_hud()
	await get_tree().create_timer(RELOAD_TIME).timeout
	if is_dead:
		return
	var needed := MAGAZINE_SIZE - ammo
	var loaded: int = mini(needed, reserve_ammo)
	ammo += loaded
	reserve_ammo -= loaded
	reloading = false
	_update_hud()

func take_damage(amount: int) -> void:
	if is_dead:
		return
	var absorbed: int = mini(armor, int(ceil(float(amount) * 0.65)))
	armor -= absorbed
	health = maxi(0, health - (amount - absorbed))
	hit_flash = 0.18
	_update_hud()
	if health <= 0:
		_die()

func _die() -> void:
	is_dead = true
	velocity = Vector3.ZERO
	if is_instance_valid(weapon_root):
		weapon_root.visible = false
	if is_instance_valid(collision_shape):
		collision_shape.set_deferred("disabled", true)
	set_physics_process(false)
	set_process_unhandled_input(false)
	var root := get_tree().current_scene
	if root.has_method("player_died"):
		root.player_died()
	await get_tree().create_timer(RESPAWN_TIME).timeout
	global_position = Vector3(0, 1.2, 15)
	health = 100
	armor = 100
	ammo = MAGAZINE_SIZE
	reserve_ammo = 90
	reloading = false
	is_dead = false
	if is_instance_valid(weapon_root):
		weapon_root.visible = true
	if is_instance_valid(collision_shape):
		collision_shape.set_deferred("disabled", false)
	set_physics_process(true)
	set_process_unhandled_input(true)
	_update_hud()

func _update_hud() -> void:
	var root := get_tree().current_scene
	if root.has_method("update_hud"):
		root.update_hud(health, armor, ammo, reserve_ammo, reloading, is_dead)
