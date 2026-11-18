extends Node3D

const PLAYER_SCRIPT = preload("res://scripts/player.gd")
const BOT_SCRIPT = preload("res://scripts/bot.gd")
const MOBILE_SCRIPT = preload("res://scripts/mobile_controls.gd")
const ROUND_LENGTH := 100.0
const ROUNDS_TO_WIN := 5

var player: CharacterBody3D
var hud: CanvasLayer
var health_label: Label
var score_label: Label
var timer_label: Label
var message_label: Label
var blue_score := 0
var red_score := 0
var kills := 0
var deaths := 0
var round_number := 1
var round_time := ROUND_LENGTH
var round_over := false
var round_transition := false
var message_timer := 0.0

func _ready() -> void:
	_build_lighting()
	_build_arena()
	_build_player()
	_build_hud()
	_build_mobile_controls()
	_spawn_teams()
	_refresh_hud()

func _process(delta: float) -> void:
	message_timer = maxf(0.0, message_timer - delta)
	if message_timer <= 0.0 and is_instance_valid(message_label):
		message_label.text = ""
	if round_over or round_transition:
		return
	round_time = maxf(0.0, round_time - delta)
	if round_time <= 0.0:
		_end_round("ВРЕМЯ ВЫШЛО")
		return
	var blue_alive := _alive_count("team_blue") + (1 if is_instance_valid(player) and not player.is_dead else 0)
	var red_alive := _alive_count("team_red")
	if red_alive == 0:
		_end_round("КОМАНДА BLUE ПОБЕДИЛА")
	elif blue_alive == 0:
		_end_round("КОМАНДА RED ПОБЕДИЛА")
	_refresh_hud()

func _build_lighting() -> void:
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-48.0, -25.0, 0.0)
	sun.light_energy = 1.35
	add_child(sun)
	var env := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_SKY
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color(0.40, 0.47, 0.58)
	environment.ambient_light_energy = 0.7
	env.environment = environment
	add_child(env)

func _mat(color: Color, roughness: float = 0.82) -> StandardMaterial3D:
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = roughness
	return material

func _box(size: Vector3, pos: Vector3, color: Color) -> void:
	var body := StaticBody3D.new()
	body.position = pos
	var mesh := MeshInstance3D.new()
	var shape_mesh := BoxMesh.new()
	shape_mesh.size = size
	mesh.mesh = shape_mesh
	mesh.material_override = _mat(color)
	body.add_child(mesh)
	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = size
	collision.shape = shape
	body.add_child(collision)
	add_child(body)

func _build_arena() -> void:
	_box(Vector3(54, 1, 42), Vector3(0, -0.5, 0), Color("#555d68"))
	_box(Vector3(54, 5, 1), Vector3(0, 2, -21), Color("#303a48"))
	_box(Vector3(54, 5, 1), Vector3(0, 2, 21), Color("#303a48"))
	_box(Vector3(1, 5, 42), Vector3(-27, 2, 0), Color("#303a48"))
	_box(Vector3(1, 5, 42), Vector3(27, 2, 0), Color("#303a48"))
	# Mid-lane cover and two side routes.
	_box(Vector3(2.5, 1.6, 6.0), Vector3(0, 0.8, 0), Color("#737d88"))
	_box(Vector3(5.0, 1.4, 2.0), Vector3(-8, 0.7, -7), Color("#5c6875"))
	_box(Vector3(5.0, 1.4, 2.0), Vector3(8, 0.7, 7), Color("#5c6875"))
	_box(Vector3(2.0, 2.0, 2.0), Vector3(-15, 1, 5), Color("#66727f"))
	_box(Vector3(2.0, 2.0, 2.0), Vector3(15, 1, -5), Color("#66727f"))
	_box(Vector3(6.0, 1.2, 2.0), Vector3(-17, 0.6, -9), Color("#4c5967"))
	_box(Vector3(6.0, 1.2, 2.0), Vector3(17, 0.6, 9), Color("#4c5967"))

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.set_script(PLAYER_SCRIPT)
	player.name = "Player"
	player.position = Vector3(-20, 0, 0)
	player.add_to_group("team_blue")
	add_child(player)

func _spawn_teams() -> void:
	var blue_spawns := [Vector3(-16,0,-8), Vector3(-18,0,5), Vector3(-11,0,12), Vector3(-9,0,-13)]
	var red_spawns := [Vector3(16,0,8), Vector3(18,0,-5), Vector3(11,0,-12), Vector3(9,0,13), Vector3(20,0,0)]
	for i in range(blue_spawns.size()):
		_spawn_bot("BLUE_%02d" % (i + 1), "blue", blue_spawns[i])
	for i in range(red_spawns.size()):
		_spawn_bot("RED_%02d" % (i + 1), "red", red_spawns[i])

func _spawn_bot(bot_name: String, team: String, spawn: Vector3) -> void:
	var bot := CharacterBody3D.new()
	bot.set_script(BOT_SCRIPT)
	bot.name = bot_name
	bot.set("team_id", team)
	bot.position = spawn
	bot.add_to_group("team_" + team)
	add_child(bot)

func get_enemy_for(actor: Node3D) -> Node3D:
	var team := str(actor.get("team_id"))
	var enemy_group := "team_red" if team == "blue" else "team_blue"
	var best: Node3D
	var best_distance := INF
	for candidate in get_tree().get_nodes_in_group(enemy_group):
		if not is_instance_valid(candidate) or candidate == actor:
			continue
		if candidate.get("is_dead") == true:
			continue
		var distance := actor.global_position.distance_to(candidate.global_position)
		if distance < best_distance:
			best_distance = distance
			best = candidate
	if team == "red" and is_instance_valid(player) and not player.is_dead:
		var player_distance := actor.global_position.distance_to(player.global_position)
		if player_distance < best_distance:
			best = player
	return best

func _alive_count(group_name: String) -> int:
	var count := 0
	for node in get_tree().get_nodes_in_group(group_name):
		if is_instance_valid(node) and node.get("is_dead") != true:
			count += 1
	return count

func _end_round(reason: String) -> void:
	if round_over:
		return
	round_over = true
	if reason.contains("BLUE") or (reason == "ВРЕМЯ ВЫШЛО" and _alive_count("team_blue") + (1 if not player.is_dead else 0) > _alive_count("team_red")):
		blue_score += 1
		show_message("РАУНД ЗА BLUE  •  " + reason, 3.0)
	else:
		red_score += 1
		show_message("РАУНД ЗА RED  •  " + reason, 3.0)
	_refresh_hud()
	if blue_score >= ROUNDS_TO_WIN or red_score >= ROUNDS_TO_WIN:
		show_message("ПОБЕДА BLUE" if blue_score > red_score else "ПОБЕДА RED", 6.0)
		return
	_start_next_round()

func _start_next_round() -> void:
	round_transition = true
	await get_tree().create_timer(4.0).timeout
	for group_name in ["team_blue", "team_red"]:
		for unit in get_tree().get_nodes_in_group(group_name):
			if unit != player and is_instance_valid(unit):
				unit.queue_free()
	if is_instance_valid(player):
		player.reset_for_round(Vector3(-20, 0, 0))
	_spawn_teams()
	round_number += 1
	round_time = ROUND_LENGTH
	round_over = false
	round_transition = false
	show_message("РАУНД %02d — БОЙ!" % round_number, 2.5)
	_refresh_hud()

func _build_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var top := PanelContainer.new()
	top.position = Vector2(16, 14)
	top.custom_minimum_size = Vector2(390, 92)
	hud.add_child(top)
	var stack := VBoxContainer.new()
	top.add_child(stack)
	score_label = Label.new()
	score_label.add_theme_font_size_override("font_size", 22)
	stack.add_child(score_label)
	timer_label = Label.new()
	timer_label.add_theme_font_size_override("font_size", 18)
	stack.add_child(timer_label)
	health_label = Label.new()
	health_label.position = Vector2(18, 116)
	health_label.add_theme_font_size_override("font_size", 17)
	hud.add_child(health_label)
	message_label = Label.new()
	message_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	message_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
	message_label.position.y = 112
	message_label.size = Vector2(700, 54)
	message_label.add_theme_font_size_override("font_size", 26)
	hud.add_child(message_label)
	var crosshair := Label.new()
	crosshair.text = "+"
	crosshair.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	crosshair.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	crosshair.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	crosshair.position = Vector2(-12, -18)
	crosshair.size = Vector2(24, 36)
	crosshair.add_theme_font_size_override("font_size", 30)
	hud.add_child(crosshair)
	var hint := Label.new()
	hint.text = "MOVE  •  AIM  •  FIRE  •  RELOAD"
	hint.position = Vector2(18, 150)
	hud.add_child(hint)

func _build_mobile_controls() -> void:
	if not DisplayServer.is_touchscreen_available():
		return
	var layer := CanvasLayer.new()
	layer.layer = 2
	add_child(layer)
	var controls := Control.new()
	controls.set_script(MOBILE_SCRIPT)
	controls.set("player", player)
	layer.add_child(controls)

func _refresh_hud() -> void:
	if is_instance_valid(score_label):
		score_label.text = "BLUE  %d     —     %d  RED     |     ROUND %02d" % [blue_score, red_score, round_number]
	if is_instance_valid(timer_label):
		timer_label.text = "TIME  %02d:%02d     |     FIRST TO %d" % [int(round_time) / 60, int(round_time) % 60, ROUNDS_TO_WIN]
	if is_instance_valid(health_label) and is_instance_valid(player):
		health_label.text = "HP %03d   ARMOR %03d   AMMO %02d/%02d   K/D %d/%d" % [player.health, player.armor, player.ammo, player.reserve_ammo, kills, deaths]

func update_hud(hp: int, vest: int, current_ammo: int, reserve: int, is_reloading: bool, is_dead: bool) -> void:
	_refresh_hud()
	if is_instance_valid(health_label):
		if is_reloading:
			health_label.text += "   RELOADING"
		elif is_dead:
			health_label.text += "   ELIMINATED"

func register_kill() -> void:
	kills += 1
	_refresh_hud()
	show_message("ELIMINATION", 0.7)

func player_died() -> void:
	deaths += 1
	_refresh_hud()
	show_message("YOU WERE ELIMINATED", 1.8)

func bot_eliminated(team: String) -> void:
	if team == "red":
		kills += 1
	_refresh_hud()

func show_message(value: String, duration: float = 1.5) -> void:
	if is_instance_valid(message_label):
		message_label.text = value
		message_timer = duration
