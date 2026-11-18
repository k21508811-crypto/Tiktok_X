extends CharacterBody3D

var health := 100
var move_timer := 0.0
var move_direction := Vector3.ZERO
var gravity: float = ProjectSettings.get_setting("physics/3d/default_gravity")
var body_mesh: MeshInstance3D
var hit_flash := 0.0

func _ready() -> void:
	var capsule := CapsuleMesh.new()
	capsule.radius = 0.38
	capsule.height = 1.75
	body_mesh = MeshInstance3D.new()
	body_mesh.mesh = capsule
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color("#3e8c78")
	mat.roughness = 0.72
	body_mesh.material_override = mat
	body_mesh.position.y = 0.9
	add_child(body_mesh)
	var collision := CollisionShape3D.new()
	var shape := CapsuleShape3D.new()
	shape.radius = 0.38
	shape.height = 1.75
	collision.shape = shape
	collision.position.y = 0.9
	add_child(collision)

func _physics_process(delta: float) -> void:
	if health <= 0:
		return
	move_timer -= delta
	if move_timer <= 0.0:
		move_timer = randf_range(1.0, 2.5)
		var angle := randf() * TAU
		move_direction = Vector3(cos(angle), 0, sin(angle)) * randf_range(0.7, 1.8)
	if not is_on_floor():
		velocity.y -= gravity * delta
	velocity.x = move_direction.x
	velocity.z = move_direction.z
	move_and_slide()
	if hit_flash > 0.0:
		hit_flash -= delta

func take_damage(amount: int) -> void:
	if health <= 0:
		return
	health -= amount
	if health <= 0:
		health = 0
		queue_free()
