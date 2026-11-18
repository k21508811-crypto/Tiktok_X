extends Node3D

const PLAYER_SCENE_SCRIPT = preload("res://scripts/player.gd")
const BOT_SCENE_SCRIPT = preload("res://scripts/bot.gd")
const MOBILE_CONTROLS_SCRIPT = preload("res://scripts/mobile_controls.gd")

var player: CharacterBody3D
var hud: CanvasLayer
var status_label: Label

func _ready() -> void:
	_build_lighting()
	_build_arena()
	_spawn_player()
	_spawn_bots()
	_build_hud()
	_build_mobile_controls()

func _build_lighting() -> void:
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-52.0, -28.0, 0.0)
	sun.light_energy = 1.25
	add_child(sun)
	var env := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_SKY
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color(0.42, 0.48, 0.58)
	environment.ambient_light_energy = 0.65
	env.environment = environment
	add_child(env)

func _material(color: Color, roughness: float = 0.85) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = roughness
	return mat

func _box(parent: Node3D, size: Vector3, pos: Vector3, color: Color) -> StaticBody3D:
	var body := StaticBody3D.new()
	body.position = pos
	var mesh := MeshInstance3D.new()
	var box := BoxMesh.new()
	box.size = size
	mesh.mesh = box
	mesh.material_override = _material(color)
	body.add_child(mesh)
	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = size
	collision.shape = shape
	body.add_child(collision)
	parent.add_child(body)
	return body

func _build_arena() -> void:
	_box(self, Vector3(48, 1, 48), Vector3(0, -0.5, 0), Color("#555d68"))
	_box(self, Vector3(48, 5, 1), Vector3(0, 2, -24), Color("#343c49"))
	_box(self, Vector3(48, 5, 1), Vector3(0, 2, 24), Color("#343c49"))
	_box(self, Vector3(1, 5, 48), Vector3(-24, 2, 0), Color("#343c49"))
	_box(self, Vector3(1, 5, 48), Vector3(24, 2, 0), Color("#343c49"))
	for i in range(6):
		var x := -15.0 + float(i) * 6.0
		_box(self, Vector3(2.4, 1.8, 2.4), Vector3(x, 0.9, -5), Color("#707b89"))
		_box(self, Vector3(3.2, 1.2, 1.4), Vector3(-x * 0.65, 0.6, 8), Color("#596574"))

func _spawn_player() -> void:
	player = CharacterBody3D.new()
	player.set_script(PLAYER_SCENE_SCRIPT)
	player.position = Vector3(0, 1.2, 15)
	player.name = "Player"
	add_child(player)

func _spawn_bots() -> void:
	for i in range(5):
		var bot := CharacterBody3D.new()
		bot.set_script(BOT_SCENE_SCRIPT)
		bot.position = Vector3(-12 + i * 6, 1, -12)
		bot.name = "TrainingBot_%d" % i
		add_child(bot)

func _build_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var panel := PanelContainer.new()
	panel.position = Vector2(18, 18)
	panel.custom_minimum_size = Vector2(270, 76)
	hud.add_child(panel)
	status_label = Label.new()
	status_label.text = "PROJECT STRIKE  |  TRAINING\nHP 100   ARMOR 100   AMMO 30/90"
	panel.add_child(status_label)
	var hint := Label.new()
	hint.text = "WASD move • Mouse look • LMB fire • R reload"
	hint.position = Vector2(18, 104)
	hud.add_child(hint)

func _build_mobile_controls() -> void:
	if not DisplayServer.is_touchscreen_available():
		return
	var controls_layer := CanvasLayer.new()
	controls_layer.layer = 2
	add_child(controls_layer)
	var controls := Control.new()
	controls.set_script(MOBILE_CONTROLS_SCRIPT)
	controls.set("player", player)
	controls_layer.add_child(controls)

func update_hud(text_value: String) -> void:
	if is_instance_valid(status_label):
		status_label.text = text_value
