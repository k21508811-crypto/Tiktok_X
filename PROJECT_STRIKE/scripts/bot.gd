extends CharacterBody3D

const WALK_SPEED := 1.25
const ARENA_LIMIT := 20.0

var health := 100
var move_timer := 0.0
var move_direction := Vector3.ZERO
var gravity: float = ProjectSettings.get_setting("physics/3d/default_gravity")
var body_root: Node3D
var torso_material: StandardMaterial3D
var hit_flash := 0.0
var animation_time := 0.0

func _ready() -> void:
	_build_collision()
	_build_character()

func _material(color: Color, roughness: float = 0.78, metallic: float = 0.0) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = roughness
	mat.metallic = metallic
	return mat

func _add_mesh(mesh: Mesh, pos: Vector3, mat: Material, parent: Node3D, part_name: String = "Part") -> MeshInstance3D:
	var instance := MeshInstance3D.new()
	instance.name = part_name
	instance.mesh = mesh
	instance.position = pos
	instance.material_override = mat
	parent.add_child(instance)
	return instance

func _capsule(radius: float, height: float, color: Color, pos: Vector3, part_name: String, parent: Node3D) -> MeshInstance3D:
	var mesh := CapsuleMesh.new()
	mesh.radius = radius
	mesh.height = height
	return _add_mesh(mesh, pos, _material(color), parent, part_name)

func _box(size: Vector3, color: Color, pos: Vector3, part_name: String, parent: Node3D) -> MeshInstance3D:
	var mesh := BoxMesh.new()
	mesh.size = size
	return _add_mesh(mesh, pos, _material(color, 0.64, 0.12), parent, part_name)

func _build_collision() -> void:
	var collision := CollisionShape3D.new()
	var shape := CapsuleShape3D.new()
	shape.radius = 0.38
	shape.height = 1.85
	collision.shape = shape
	collision.position.y = 0.92
	add_child(collision)

func _build_character() -> void:
	body_root = Node3D.new()
	body_root.name = "CharacterModel"
	add_child(body_root)
	torso_material = _material(Color("#365b58"), 0.72, 0.08)
	_capsule(0.31, 0.76, Color("#365b58"), Vector3(0, 1.12, 0), "ArmoredTorso", body_root)
	_box(Vector3(0.54, 0.30, 0.34), Color("#273b3e"), Vector3(0, 0.78, 0), "PelvisArmor", body_root)
	var head := SphereMesh.new()
	head.radius = 0.205
	head.height = 0.41
	_add_mesh(head, Vector3(0, 1.78, 0), _material(Color("#b68d70")), body_root, "Head")
	var helmet := SphereMesh.new()
	helmet.radius = 0.235
	helmet.height = 0.23
	_add_mesh(helmet, Vector3(0, 1.91, 0), _material(Color("#293b3b"), 0.42, 0.2), body_root, "Helmet")
	_box(Vector3(0.36, 0.09, 0.28), Color("#18282b"), Vector3(0, 1.78, -0.04), "FaceShield", body_root)
	_box(Vector3(0.42, 0.34, 0.13), Color("#52736c"), Vector3(0, 1.20, -0.19), "ChestPlate", body_root)
	_box(Vector3(0.18, 0.18, 0.08), Color("#b78c45"), Vector3(0, 1.20, -0.27), "ChestInsignia", body_root)
	for side in [-1.0, 1.0]:
		var arm := _capsule(0.115, 0.68, Color("#314d4a"), Vector3(side * 0.43, 1.14, 0), "Arm", body_root)
		arm.rotation.z = side * -0.12
		_capsule(0.13, 0.28, Color("#273a3a"), Vector3(side * 0.47, 0.83, -0.01), "Glove", body_root)
		_capsule(0.145, 0.82, Color("#263a3b"), Vector3(side * 0.17, 0.39, 0.015), "Leg", body_root)
		_box(Vector3(0.29, 0.16, 0.43), Color("#1d292b"), Vector3(side * 0.17, 0.10, -0.055), "Boot", body_root)
		_box(Vector3(0.20, 0.23, 0.30), Color("#293f40"), Vector3(side * 0.34, 1.43, 0), "ShoulderPad", body_root)
	var rifle := _box(Vector3(0.11, 0.11, 0.62), Color("#171c20"), Vector3(0.16, 1.10, -0.40), "TrainingRifle", body_root)
	rifle.rotation.x = -0.08
	_box(Vector3(0.15, 0.13, 0.22), Color("#59615e"), Vector3(0.16, 1.16, -0.22), "RifleReceiver", body_root)
	_box(Vector3(0.32, 0.12, 0.20), Color("#243535"), Vector3(0, 1.43, 0.13), "Backpack", body_root)

func _physics_process(delta: float) -> void:
	if health <= 0:
		return
	animation_time += delta
	move_timer -= delta
	if move_timer <= 0.0:
		move_timer = randf_range(1.2, 2.8)
		var angle := randf() * TAU
		move_direction = Vector3(cos(angle), 0, sin(angle)) * randf_range(0.35, 1.0)
	if position.x < -ARENA_LIMIT or position.x > ARENA_LIMIT:
		move_direction.x = -signf(position.x)
	if position.z < -ARENA_LIMIT or position.z > ARENA_LIMIT:
		move_direction.z = -signf(position.z)
	if not is_on_floor():
		velocity.y -= gravity * delta
	velocity.x = move_direction.x * WALK_SPEED
	velocity.z = move_direction.z * WALK_SPEED
	move_and_slide()
	body_root.position.y = sin(animation_time * 5.0) * 0.018
	if move_direction.length() > 0.05:
		body_root.rotation.y = lerp_angle(body_root.rotation.y, atan2(-move_direction.x, -move_direction.z), delta * 4.0)
	if hit_flash > 0.0:
		hit_flash -= delta

func take_damage(amount: int) -> void:
	if health <= 0:
		return
	health = maxi(0, health - amount)
	if health == 0:
		queue_free()
