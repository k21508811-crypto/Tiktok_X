extends Control
## On-screen controls for landscape Android builds.
var player: CharacterBody3D
var joystick_center := Vector2.ZERO
var joystick_vector := Vector2.ZERO
var joystick_pointer := -1
var look_pointer := -1
var look_sensitivity := 0.004

func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_build_buttons()
	queue_redraw()

func _build_buttons() -> void:
	var size := get_viewport_rect().size
	_add_action_button("FIRE", "fire", Vector2(size.x - 174, size.y - 164), Vector2(112, 112))
	_add_action_button("RELOAD", "reload", Vector2(size.x - 282, size.y - 105), Vector2(92, 58))
	_add_action_button("JUMP", "jump", Vector2(size.x - 154, size.y - 48), Vector2(92, 48))

func _add_action_button(caption: String, action: String, pos: Vector2, button_size: Vector2) -> void:
	var button := Button.new()
	button.text = caption
	button.position = pos
	button.size = button_size
	button.focus_mode = Control.FOCUS_NONE
	button.modulate = Color(1, 1, 1, 0.88)
	button.button_down.connect(func(): Input.action_press(action))
	button.button_up.connect(func(): Input.action_release(action))
	add_child(button)

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		var screen := get_viewport_rect().size
		if event.pressed:
			if event.position.x < screen.x * 0.43 and event.position.y > screen.y * 0.48 and joystick_pointer == -1:
				joystick_pointer = event.index
				joystick_center = event.position
				joystick_vector = Vector2.ZERO
				queue_redraw()
			elif event.position.x > screen.x * 0.48 and event.position.y < screen.y - 190 and look_pointer == -1:
				look_pointer = event.index
		else:
			if event.index == joystick_pointer:
				joystick_pointer = -1
				joystick_vector = Vector2.ZERO
				_release_movement()
				queue_redraw()
			if event.index == look_pointer:
				look_pointer = -1
	elif event is InputEventScreenDrag:
		if event.index == joystick_pointer:
			var offset := event.position - joystick_center
			joystick_vector = offset.limit_length(76.0) / 76.0
			_update_movement()
			queue_redraw()
		elif event.index == look_pointer and is_instance_valid(player):
			player.rotate_y(-event.relative.x * look_sensitivity)
			var camera := player.get_node_or_null("Camera3D") as Camera3D
			if camera:
				camera.rotate_x(-event.relative.y * look_sensitivity)
				camera.rotation.x = clampf(camera.rotation.x, -1.4, 1.4)

func _update_movement() -> void:
	_set_action("move_left", joystick_vector.x < -0.18)
	_set_action("move_right", joystick_vector.x > 0.18)
	_set_action("move_forward", joystick_vector.y < -0.18)
	_set_action("move_back", joystick_vector.y > 0.18)

func _release_movement() -> void:
	for action in ["move_left", "move_right", "move_forward", "move_back"]:
		Input.action_release(action)

func _set_action(action: String, active: bool) -> void:
	if active:
		Input.action_press(action)
	else:
		Input.action_release(action)

func _draw() -> void:
	var center := joystick_center if joystick_pointer != -1 else Vector2(112, get_viewport_rect().size.y - 112)
	draw_circle(center, 76.0, Color(0.08, 0.10, 0.14, 0.30))
	draw_arc(center, 76.0, 0.0, TAU, 48, Color(0.8, 0.86, 0.95, 0.55), 3.0)
	draw_circle(center + joystick_vector * 48.0, 29.0, Color(0.82, 0.88, 0.96, 0.48))
