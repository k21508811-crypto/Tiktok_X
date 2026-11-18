extends Node3D

const PLAYER_SCENE_SCRIPT = preload("res://scripts/player.gd")
const BOT_SCENE_SCRIPT = preload("res://scripts/bot.gd")
const MOBILE_CONTROLS_SCRIPT = preload("res://scripts/mobile_controls.gd")
const BOT_COUNT := 5

var player: CharacterBody3D
var hud: CanvasLayer
var status_label: Label
var score_label: Label
var message_label: Label
var kills := 0
var deaths := 0
var wave := 1
var round_time := 0.0
var message_timer := 0.0

func _ready() -> void:
	_build_lighting()
	_build_arena()
	_spawn_player()
	_spawn_bots()
	_build_hud()
	_build_mobile_controls()
	_update_hud(100, 100, 30, 90, false, false)

func _process(delta: float) -> void:
	round_time += delta
	message_timer = maxf(0.0, message_timer - delta)
	if message_timer <= 0.0 and is_instance_valid(message_label):
		message_label.text = ""
	if get_tree().get_nodes_in_group("enemy_bots").is_empty():
		wave += 1
		_spawn_bots()
		show_message("WAVE %02d — ENEMIES INBOUND" % wave, 2.5)

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
	player.position = Vector3(0, 0.0, 15)
	player.name = "Player"
	add_child(player)

func _spawn_bots() -> void:
	for i in range(BOT_COUNT):
		var bot := CharacterBody3D.new()
		bot.set_script(BOT_SCENE_SCRIPT)
		var angle := TAU * float(i) / float(BOT_COUNT)
		bot.position = Vector3(cos(angle) * 13.0, 0.0, -cos(angle * 0.7) * 12.0)
		bot.name = "TrainingBot_%d" % i
		bot.add_to_group("enemy_bots")
		add_child(bot)

func _build_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var panel := PanelContainer.new()
	panel.position = Vector2(18, 18)
	panel.custom_minimum_size = Vector2(300, 104)
	hud.add_child(panel)
	var info := VBoxContainer.new()
	panel.add_child(info)
	status_label = Label.new()
	status_label.text = "HP 100   ARMOR 100   AMMO 30/90"
	info.add_child(status_label)
	score_label = Label.new()
	score_label.text = "KILLS 0   DEATHS 0   WAVE 01"
	info.add_child(score_label)
	var hint := Label.new()
	hint.text = "WASD MOVE • MOUSE LOOK • LMB FIRE • R RELOAD"
	hint.position = Vector2(18, 130)
	hud.add_child(hint)
	message_label = Label.new()
	message_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	message_label.position = Vector2(300, 35)
	message_label.size = Vector2(680, 48)
	message_label.add_theme_font_size_override("font_size", 24)
	hud.add_child(message_label)
	var crosshair := Label.new()
	crosshair.text = "+"
	crosshair.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	crosshair.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	crosshair.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	crosshair.position = Vector2(-12, -18)
	crosshair.size = Vector2(24, 36)
	crosshair.add_theme_font_size_override("font_size", 28)
	hud.add_child(crosshair)

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

func update_hud(hp: int, vest: int, current_ammo: int, reserve: int, is_reloading: bool, is_dead: bool) -> void:
	if is_instance_valid(status_label):
		var state := "  RELOADING" if is_reloading else ""
		if is_dead:
			state = "  ELIMINATED"
		status_label.text = "HP %03d   ARMOR %03d   AMMO %02d/%02d%s" % [hp, vest, current_ammo, reserve, state]
	if is_instance_valid(score_label):
		score_label.text = "KILLS %d   DEATHS %d   WAVE %02d" % [kills, deaths, wave]

func register_kill() -> void:
	kills += 1
	_update_hud(player.health, player.armor, player.ammo, player.reserve_ammo, player.reloading, player.is_dead)
	show_message("TARGET ELIMINATED", 0.8)

func player_died() -> void:
	deaths += 1
	show_message("YOU WERE ELIMINATED — RESPAWNING", 2.5)
	if is_instance_valid(player):
		_update_hud(player.health, player.armor, player.ammo, player.reserve_ammo, false, true)

func show_message(value: String, duration: float = 1.5) -> void:
	if is_instance_valid(message_label):
		message_label.text = value
		message_timer = duration
