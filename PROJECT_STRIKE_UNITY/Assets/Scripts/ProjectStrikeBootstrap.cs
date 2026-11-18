using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public sealed class ProjectStrikeBootstrap : MonoBehaviour
{
    public const int PlayersPerTeam = 5;
    public const int RoundsToWin = 5;
    public const float RoundDuration = 100f;

    public static ProjectStrikeBootstrap Instance { get; private set; }
    public StrikePlayer Player { get; private set; }
    public int BlueScore { get; private set; }
    public int RedScore { get; private set; }
    public int RoundNumber { get; private set; } = 1;
    public int Kills { get; private set; }
    public int Deaths { get; private set; }
    public float TimeLeft { get; private set; } = RoundDuration;
    public bool MatchFinished { get; private set; }
    public bool RoundTransition { get; private set; }

    private readonly List<StrikeBot> bots = new List<StrikeBot>();
    private GUIStyle titleStyle;
    private GUIStyle hudStyle;
    private GUIStyle buttonStyle;
    private GUIStyle smallStyle;
    private string centerMessage = "ROUND 1";
    private float messageUntil;

    private static readonly Vector3[] BlueSpawns =
    {
        new Vector3(-16f, 0f, -8f), new Vector3(-18f, 0f, 5f),
        new Vector3(-11f, 0f, 12f), new Vector3(-9f, 0f, -13f)
    };

    private static readonly Vector3[] RedSpawns =
    {
        new Vector3(16f, 0f, 8f), new Vector3(18f, 0f, -5f),
        new Vector3(11f, 0f, -12f), new Vector3(9f, 0f, 13f),
        new Vector3(20f, 0f, 0f)
    };

    private void Awake()
    {
        Instance = this;
        Application.targetFrameRate = 60;
        Application.runInBackground = true;
        Screen.orientation = ScreenOrientation.LandscapeLeft;
        Screen.sleepTimeout = SleepTimeout.NeverSleep;
        BuildLighting();
        BuildArena();
        SpawnPlayer();
        SpawnTeams();
        ShowMessage("PROJECT STRIKE  •  FIRST TO 5", 3f);
    }

    private void Update()
    {
        if (MatchFinished || RoundTransition) return;

        TimeLeft = Mathf.Max(0f, TimeLeft - Time.deltaTime);
        int blueAlive = CountAlive("blue") + (Player != null && Player.IsAlive ? 1 : 0);
        int redAlive = CountAlive("red");

        if (redAlive == 0) EndRound(true, "ALL RED ELIMINATED");
        else if (blueAlive == 0) EndRound(false, "ALL BLUE ELIMINATED");
        else if (TimeLeft <= 0f) EndRound(blueAlive >= redAlive, "TIME EXPIRED");
    }

    private void BuildLighting()
    {
        RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Flat;
        RenderSettings.ambientLight = new Color(0.40f, 0.45f, 0.53f);
        RenderSettings.fog = true;
        RenderSettings.fogColor = new Color(0.15f, 0.19f, 0.25f);
        RenderSettings.fogDensity = 0.006f;

        var sunObject = new GameObject("Directional Light");
        var sun = sunObject.AddComponent<Light>();
        sun.type = LightType.Directional;
        sun.intensity = 1.15f;
        sun.color = new Color(1f, 0.94f, 0.84f);
        sun.shadows = LightShadows.Soft;
        sunObject.transform.rotation = Quaternion.Euler(48f, -28f, 0f);
    }

    private Material MakeMaterial(Color color, float metallic = 0.05f, float smoothness = 0.35f)
    {
        Shader shader = Shader.Find("Standard");
        var material = new Material(shader);
        material.color = color;
        material.SetFloat("_Metallic", metallic);
        material.SetFloat("_Glossiness", smoothness);
        return material;
    }

    private void MakeBlock(string objectName, Vector3 position, Vector3 size, Color color, float metallic = 0.05f)
    {
        GameObject block = GameObject.CreatePrimitive(PrimitiveType.Cube);
        block.name = objectName;
        block.transform.position = position;
        block.transform.localScale = size;
        block.GetComponent<Renderer>().sharedMaterial = MakeMaterial(color, metallic);
        block.isStatic = true;
    }

    private void BuildArena()
    {
        MakeBlock("Arena Floor", new Vector3(0f, -0.5f, 0f), new Vector3(54f, 1f, 42f), new Color(0.24f, 0.28f, 0.32f));
        MakeBlock("North Boundary", new Vector3(0f, 2f, -21f), new Vector3(54f, 5f, 1f), new Color(0.12f, 0.17f, 0.23f));
        MakeBlock("South Boundary", new Vector3(0f, 2f, 21f), new Vector3(54f, 5f, 1f), new Color(0.12f, 0.17f, 0.23f));
        MakeBlock("West Boundary", new Vector3(-27f, 2f, 0f), new Vector3(1f, 5f, 42f), new Color(0.12f, 0.17f, 0.23f));
        MakeBlock("East Boundary", new Vector3(27f, 2f, 0f), new Vector3(1f, 5f, 42f), new Color(0.12f, 0.17f, 0.23f));

        MakeBlock("Center Cover", new Vector3(0f, 0.8f, 0f), new Vector3(2.5f, 1.6f, 6f), new Color(0.42f, 0.47f, 0.52f), 0.18f);
        MakeBlock("Blue Lane Cover", new Vector3(-8f, 0.7f, -7f), new Vector3(5f, 1.4f, 2f), new Color(0.28f, 0.38f, 0.47f));
        MakeBlock("Red Lane Cover", new Vector3(8f, 0.7f, 7f), new Vector3(5f, 1.4f, 2f), new Color(0.47f, 0.29f, 0.31f));
        MakeBlock("West Crate", new Vector3(-15f, 1f, 5f), new Vector3(2f, 2f, 2f), new Color(0.35f, 0.40f, 0.45f));
        MakeBlock("East Crate", new Vector3(15f, 1f, -5f), new Vector3(2f, 2f, 2f), new Color(0.35f, 0.40f, 0.45f));
        MakeBlock("Blue Side Cover", new Vector3(-17f, 0.6f, -9f), new Vector3(6f, 1.2f, 2f), new Color(0.22f, 0.32f, 0.40f));
        MakeBlock("Red Side Cover", new Vector3(17f, 0.6f, 9f), new Vector3(6f, 1.2f, 2f), new Color(0.40f, 0.24f, 0.27f));

        for (int i = -2; i <= 2; i++)
        {
            MakeBlock("Floor Marking " + i, new Vector3(i * 7f, 0.012f, 0f), new Vector3(0.07f, 0.02f, 34f), new Color(0.38f, 0.43f, 0.48f));
        }
    }

    private void SpawnPlayer()
    {
        GameObject playerObject = new GameObject("Player_BLUE");
        playerObject.transform.position = new Vector3(-20f, 0f, 0f);
        Player = playerObject.AddComponent<StrikePlayer>();
        Player.Initialize();
    }

    private void SpawnTeams()
    {
        bots.Clear();
        for (int i = 0; i < BlueSpawns.Length; i++)
            SpawnBot("BLUE_" + (i + 1).ToString("00"), "blue", BlueSpawns[i]);
        for (int i = 0; i < RedSpawns.Length; i++)
            SpawnBot("RED_" + (i + 1).ToString("00"), "red", RedSpawns[i]);
    }

    private void SpawnBot(string botName, string team, Vector3 spawn)
    {
        GameObject botObject = GameObject.CreatePrimitive(PrimitiveType.Capsule);
        botObject.name = botName;
        botObject.transform.position = spawn;
        Destroy(botObject.GetComponent<CapsuleCollider>());
        StrikeBot bot = botObject.AddComponent<StrikeBot>();
        bot.Initialize(team);
        bots.Add(bot);
    }

    public StrikeBot FindNearestEnemy(string team, Vector3 from)
    {
        StrikeBot best = null;
        float bestDistance = float.MaxValue;
        foreach (StrikeBot candidate in bots)
        {
            if (candidate == null || !candidate.IsAlive || candidate.Team == team) continue;
            float distance = Vector3.Distance(from, candidate.transform.position);
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        return best;
    }

    public Transform FindEnemyTarget(string team, Vector3 from, out StrikePlayer playerTarget, out StrikeBot botTarget)
    {
        playerTarget = null;
        botTarget = null;
        Transform best = null;
        float bestDistance = float.MaxValue;

        if (team == "red" && Player != null && Player.IsAlive)
        {
            best = Player.transform;
            playerTarget = Player;
            bestDistance = Vector3.Distance(from, Player.transform.position);
        }

        foreach (StrikeBot candidate in bots)
        {
            if (candidate == null || !candidate.IsAlive || candidate.Team == team) continue;
            float distance = Vector3.Distance(from, candidate.transform.position);
            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = candidate.transform;
                playerTarget = null;
                botTarget = candidate;
            }
        }
        return best;
    }

    private int CountAlive(string team)
    {
        int count = 0;
        foreach (StrikeBot bot in bots)
            if (bot != null && bot.IsAlive && bot.Team == team) count++;
        return count;
    }

    public void RegisterPlayerKill()
    {
        Kills++;
        ShowMessage("ELIMINATION", 0.8f);
    }

    public void RegisterPlayerDeath()
    {
        Deaths++;
        ShowMessage("YOU WERE ELIMINATED", 1.5f);
    }

    private void EndRound(bool blueWon, string reason)
    {
        if (RoundTransition || MatchFinished) return;
        RoundTransition = true;
        if (blueWon) BlueScore++; else RedScore++;
        ShowMessage((blueWon ? "BLUE WINS ROUND  •  " : "RED WINS ROUND  •  ") + reason, 3f);

        if (BlueScore >= RoundsToWin || RedScore >= RoundsToWin)
        {
            MatchFinished = true;
            ShowMessage(BlueScore > RedScore ? "MATCH VICTORY — BLUE" : "MATCH DEFEAT — RED", 999f);
            return;
        }
        StartCoroutine(BeginNextRound());
    }

    private IEnumerator BeginNextRound()
    {
        yield return new WaitForSeconds(3.5f);
        for (int i = 0; i < bots.Count; i++)
            if (bots[i] != null) Destroy(bots[i].gameObject);
        bots.Clear();

        Player.ResetForRound(new Vector3(-20f, 0f, 0f));
        RoundNumber++;
        TimeLeft = RoundDuration;
        RoundTransition = false;
        SpawnTeams();
        ShowMessage("ROUND " + RoundNumber + "  •  FIGHT", 2f);
    }

    public void ShowMessage(string message, float duration)
    {
        centerMessage = message;
        messageUntil = Time.time + duration;
    }

    private void InitGuiStyles()
    {
        if (titleStyle != null) return;
        titleStyle = new GUIStyle(GUI.skin.label) { fontSize = Mathf.Max(22, Screen.height /  thirty), fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter };
        hudStyle = new GUIStyle(GUI.skin.box) { fontSize = Mathf.Max(16, Screen.height /  fifty), alignment = TextAnchor.MiddleLeft, normal = { textColor = Color.white } };
        buttonStyle = new GUIStyle(GUI.skin.button) { fontSize = Mathf.Max(18, Screen.height /  thirtyFive), fontStyle = FontStyle.Bold };
        smallStyle = new GUIStyle(GUI.skin.label) { fontSize = Mathf.Max(13, Screen.height /  fiftyFive), alignment = TextAnchor.MiddleCenter };
    }

    private const int thirty = 30;
    private const int fifty = 50;
    private const int thirtyFive = 35;
    private const int fiftyFive = 55;

    private void OnGUI()
    {
        InitGuiStyles();
        float scale = Mathf.Clamp(Screen.height / 720f, 0.8f, 1.6f);
        float panelW = 510f * scale;
        float panelH = 82f * scale;
        GUI.Box(new Rect(16f * scale, 14f * scale, panelW, panelH),
            "BLUE  " + BlueScore + "     —     " + RedScore + "  RED       ROUND " + RoundNumber.ToString("00") +
            "\nTIME  " + Mathf.FloorToInt(TimeLeft / 60f).ToString("00") + ":" + Mathf.FloorToInt(TimeLeft % 60f).ToString("00") +
            "       FIRST TO " + RoundsToWin, hudStyle);

        if (Player != null)
        {
            GUI.Label(new Rect(18f * scale, 105f * scale, 570f * scale, 34f * scale),
                "HP " + Player.Health.ToString("000") + "   ARMOR " + Player.Armor.ToString("000") +
                "   AMMO " + Player.Ammo.ToString("00") + "/" + Player.ReserveAmmo.ToString("00") +
                "   K/D " + Kills + "/" + Deaths, hudStyle);
        }

        float centerX = Screen.width * 0.5f;
        float centerY = Screen.height * 0.5f;
        GUI.Label(new Rect(centerX - 18f, centerY - 28f, 36f, 42f), "+", titleStyle);

        if (Time.time < messageUntil)
            GUI.Label(new Rect(0f, 105f * scale, Screen.width, 58f * scale), centerMessage, titleStyle);

        if (Application.isMobilePlatform && Player != null && Player.IsAlive)
        {
            float fireSize = 128f * scale;
            Rect fireRect = new Rect(Screen.width - fireSize - 24f * scale, Screen.height - fireSize - 150f * scale, fireSize, fireSize);
            Rect reloadRect = new Rect(Screen.width - 270f * scale, Screen.height - 94f * scale, 112f * scale, 64f * scale);
            Rect jumpRect = new Rect(Screen.width - 142f * scale, Screen.height - 94f * scale, 112f * scale, 64f * scale);
            GUI.Box(fireRect, "FIRE", buttonStyle);
            GUI.Box(reloadRect, "RELOAD", buttonStyle);
            GUI.Box(jumpRect, "JUMP", buttonStyle);
            GUI.Label(new Rect(20f * scale, Screen.height - 56f * scale, 360f * scale, 32f * scale), "LEFT: MOVE     RIGHT: AIM", smallStyle);
        }

        if (MatchFinished && GUI.Button(new Rect(Screen.width * 0.5f - 130f, Screen.height * 0.65f, 260f, 64f), "PLAY AGAIN", buttonStyle))
        {
            BlueScore = RedScore = Kills = Deaths = 0;
            RoundNumber = 1;
            MatchFinished = false;
            RoundTransition = false;
            StartCoroutine(BeginNextRound());
        }
    }
}

public sealed class StrikePlayer : MonoBehaviour
{
    public bool IsAlive { get; private set; } = true;
    public int Health { get; private set; } = 100;
    public int Armor { get; private set; } = 100;
    public int Ammo { get; private set; } = 30;
    public int ReserveAmmo { get; private set; } = 90;

    private CharacterController controller;
    private Camera playerCamera;
    private Transform weaponRoot;
    private float pitch;
    private float verticalSpeed;
    private float fireCooldown;
    private float reloadTimer;
    private bool reloading;
    private int joystickFinger = -1;
    private int lookFinger = -1;
    private int fireFinger = -1;
    private Vector2 joystickStart;
    private Vector2 joystickValue;
    private bool jumpQueued;
    private bool reloadQueued;
    private bool fireHeld;

    public void Initialize()
    {
        controller = gameObject.AddComponent<CharacterController>();
        controller.height = 1.8f;
        controller.radius = 0.36f;
        controller.center = new Vector3(0f, 0.9f, 0f);
        controller.stepOffset = 0.3f;

        GameObject cameraObject = new GameObject("PlayerCamera");
        cameraObject.transform.SetParent(transform, false);
        cameraObject.transform.localPosition = new Vector3(0f, 1.55f, 0f);
        playerCamera = cameraObject.AddComponent<Camera>();
        playerCamera.fieldOfView = 82f;
        playerCamera.nearClipPlane = 0.04f;
        playerCamera.tag = "MainCamera";

        weaponRoot = new GameObject("FirstPersonRifle").transform;
        weaponRoot.SetParent(playerCamera.transform, false);
        weaponRoot.localPosition = new Vector3(0.31f, -0.27f, 0.58f);
        MakeWeaponPart("Receiver", new Vector3(0.14f, 0.15f, 0.58f), Vector3.zero, new Color(0.10f, 0.13f, 0.16f));
        MakeWeaponPart("Barrel", new Vector3(0.07f, 0.07f, 0.52f), new Vector3(0f, 0.04f, 0.48f), new Color(0.055f, 0.07f, 0.08f));
        MakeWeaponPart("Magazine", new Vector3(0.12f, 0.22f, 0.15f), new Vector3(0f, -0.16f, -0.05f), new Color(0.20f, 0.23f, 0.24f));
        MakeWeaponPart("Stock", new Vector3(0.15f, 0.09f, 0.18f), new Vector3(0f, 0f, -0.34f), new Color(0.18f, 0.22f, 0.24f));
        MakeWeaponPart("Optic", new Vector3(0.16f, 0.055f, 0.13f), new Vector3(0f, 0.11f, 0.02f), new Color(0.34f, 0.39f, 0.42f));

        if (!Application.isMobilePlatform)
        {
            Cursor.lockState = CursorLockMode.Locked;
            Cursor.visible = false;
        }
    }

    private void MakeWeaponPart(string partName, Vector3 size, Vector3 localPosition, Color color)
    {
        GameObject part = GameObject.CreatePrimitive(PrimitiveType.Cube);
        part.name = partName;
        part.transform.SetParent(weaponRoot, false);
        part.transform.localPosition = localPosition;
        part.transform.localScale = size;
        Collider collider = part.GetComponent<Collider>();
        if (collider != null) Destroy(collider);
        var material = new Material(Shader.Find("Standard"));
        material.color = color;
        material.SetFloat("_Metallic", 0.35f);
        material.SetFloat("_Glossiness", 0.45f);
        part.GetComponent<Renderer>().sharedMaterial = material;
    }

    private void Update()
    {
        if (!IsAlive) return;
        fireCooldown = Mathf.Max(0f, fireCooldown - Time.deltaTime);
        HandleTouchInput();

        float mouseX = Application.isMobilePlatform ? 0f : Input.GetAxis("Mouse X") * 2.2f;
        float mouseY = Application.isMobilePlatform ? 0f : Input.GetAxis("Mouse Y") * 2.2f;
        transform.Rotate(0f, mouseX, 0f);
        pitch = Mathf.Clamp(pitch - mouseY, -78f, 78f);
        playerCamera.transform.localRotation = Quaternion.Euler(pitch, 0f, 0f);

        Vector2 moveInput = Application.isMobilePlatform ? joystickValue : new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
        moveInput = Vector2.ClampMagnitude(moveInput, 1f);
        Vector3 move = (transform.right * moveInput.x + transform.forward * moveInput.y) * 6.2f;

        if (controller.isGrounded)
        {
            if (verticalSpeed < 0f) verticalSpeed = -1.5f;
            if (jumpQueued || (!Application.isMobilePlatform && Input.GetKeyDown(KeyCode.Space)))
                verticalSpeed = 4.5f;
        }
        else verticalSpeed -= 18f * Time.deltaTime;

        controller.Move((move + Vector3.up * verticalSpeed) * Time.deltaTime);
        jumpQueued = false;

        bool desktopFire = !Application.isMobilePlatform && Input.GetMouseButton(0);
        if (fireHeld || desktopFire) Fire();
        if (reloadQueued || (!Application.isMobilePlatform && Input.GetKeyDown(KeyCode.R))) BeginReload();
        reloadQueued = false;

        if (reloading)
        {
            reloadTimer -= Time.deltaTime;
            if (reloadTimer <= 0f)
            {
                int amount = Mathf.Min(30 - Ammo, ReserveAmmo);
                Ammo += amount;
                ReserveAmmo -= amount;
                reloading = false;
            }
        }

        if (!Application.isMobilePlatform && Input.GetKeyDown(KeyCode.Escape))
        {
            Cursor.lockState = CursorLockMode.None;
            Cursor.visible = true;
        }
    }

    private void HandleTouchInput()
    {
        fireHeld = false;
        if (!Application.isMobilePlatform) return;

        foreach (Touch touch in Input.touches)
        {
            Vector2 pos = touch.position;
            if (touch.phase == TouchPhase.Began)
            {
                if (pos.x < Screen.width * 0.42f && pos.y < Screen.height * 0.72f)
                {
                    joystickFinger = touch.fingerId;
                    joystickStart = pos;
                    joystickValue = Vector2.zero;
                }
                else if (pos.x > Screen.width * 0.42f && pos.y > Screen.height * 0.28f && pos.x < Screen.width * 0.82f)
                {
                    lookFinger = touch.fingerId;
                }
                else if (pos.x > Screen.width * 0.82f && pos.y > Screen.height * 0.18f && pos.y < Screen.height * 0.38f)
                {
                    fireFinger = touch.fingerId;
                }
                else if (pos.x > Screen.width * 0.60f && pos.x < Screen.width * 0.80f && pos.y < Screen.height * 0.24f)
                    reloadQueued = true;
                else if (pos.x >= Screen.width * 0.80f && pos.y < Screen.height * 0.24f)
                    jumpQueued = true;
            }

            if (touch.fingerId == joystickFinger && (touch.phase == TouchPhase.Moved || touch.phase == TouchPhase.Stationary))
                joystickValue = Vector2.ClampMagnitude((pos - joystickStart) / 100f, 1f);

            if (touch.fingerId == lookFinger && touch.phase == TouchPhase.Moved)
            {
                transform.Rotate(0f, touch.deltaPosition.x * 0.16f, 0f);
                pitch = Mathf.Clamp(pitch + touch.deltaPosition.y * 0.16f, -78f, 78f);
                playerCamera.transform.localRotation = Quaternion.Euler(pitch, 0f, 0f);
            }

            if (touch.fingerId == fireFinger && touch.phase != TouchPhase.Ended && touch.phase != TouchPhase.Canceled)
                fireHeld = true;

            if (touch.phase == TouchPhase.Ended || touch.phase == TouchPhase.Canceled)
            {
                if (touch.fingerId == joystickFinger) { joystickFinger = -1; joystickValue = Vector2.zero; }
                if (touch.fingerId == lookFinger) lookFinger = -1;
                if (touch.fingerId == fireFinger) fireFinger = -1;
            }
        }
    }

    private void Fire()
    {
        if (reloading || fireCooldown > 0f || Ammo <= 0) return;
        Ammo--;
        fireCooldown = 0.105f;
        if (weaponRoot != null)
        {
            weaponRoot.localPosition = new Vector3(0.31f, -0.27f, 0.50f);
        }

        Ray ray = new Ray(playerCamera.transform.position, playerCamera.transform.forward);
        RaycastHit[] hits = Physics.RaycastAll(ray, 120f);
        System.Array.Sort(hits, (a, b) => a.distance.CompareTo(b.distance));
        foreach (RaycastHit hit in hits)
        {
            if (hit.transform == transform || hit.transform.IsChildOf(transform)) continue;
            StrikeBot bot = hit.collider.GetComponentInParent<StrikeBot>();
            if (bot != null && bot.IsAlive) bot.TakeDamage(34, true);
            break;
        }
    }

    private void BeginReload()
    {
        if (reloading || Ammo >= 30 || ReserveAmmo <= 0) return;
        reloading = true;
        reloadTimer = 1.45f;
    }

    public void TakeDamage(int damage)
    {
        if (!IsAlive) return;
        int absorbed = Mathf.Min(Armor, Mathf.CeilToInt(damage * 0.65f));
        Armor -= absorbed;
        Health = Mathf.Max(0, Health - (damage - absorbed));
        if (Health <= 0)
        {
            IsAlive = false;
            if (weaponRoot != null) weaponRoot.gameObject.SetActive(false);
            ProjectStrikeBootstrap.Instance.RegisterPlayerDeath();
        }
    }

    public void ResetForRound(Vector3 spawn)
    {
        transform.position = spawn;
        Health = 100;
        Armor = 100;
        Ammo = 30;
        ReserveAmmo = 90;
        IsAlive = true;
        reloading = false;
        verticalSpeed = 0f;
        if (weaponRoot != null) weaponRoot.gameObject.SetActive(true);
    }
}

public sealed class StrikeBot : MonoBehaviour
{
    public string Team { get; private set; }
    public bool IsAlive { get; private set; } = true;
    public bool KillerWasPlayer { get; private set; }
    private int health = 100;
    private CharacterController controller;
    private Transform model;
    private float fireTimer;
    private float animationClock;

    public void Initialize(string team)
    {
        Team = team;
        controller = gameObject.AddComponent<CharacterController>();
        controller.height = 1.85f;
        controller.radius = 0.36f;
        controller.center = new Vector3(0f, 0.92f, 0f);
        controller.stepOffset = 0.25f;
        BuildCharacter();
        fireTimer = Random.Range(0.35f, 1.2f);
    }

    private Material Mat(Color color, float metallic = 0.08f)
    {
        var material = new Material(Shader.Find("Standard"));
        material.color = color;
        material.SetFloat("_Metallic", metallic);
        material.SetFloat("_Glossiness", 0.38f);
        return material;
    }

    private void Part(PrimitiveType type, string partName, Vector3 position, Vector3 scale, Color color)
    {
        GameObject part = GameObject.CreatePrimitive(type);
        part.name = partName;
        part.transform.SetParent(model, false);
        part.transform.localPosition = position;
        part.transform.localScale = scale;
        Collider collider = part.GetComponent<Collider>();
        if (collider != null) Destroy(collider);
        part.GetComponent<Renderer>().sharedMaterial = Mat(color);
    }

    private void BuildCharacter()
    {
        model = new GameObject("ArmoredSoldier").transform;
        model.SetParent(transform, false);
        Color armor = Team == "blue" ? new Color(0.16f, 0.31f, 0.42f) : new Color(0.43f, 0.19f, 0.22f);
        Color dark = new Color(0.09f, 0.12f, 0.14f);
        Color skin = new Color(0.55f, 0.39f, 0.29f);

        Part(PrimitiveType.Capsule, "Armored torso", new Vector3(0f, 1.12f, 0f), new Vector3(0.72f, 0.62f, 0.48f), armor);
        Part(PrimitiveType.Cube, "Chest plate", new Vector3(0f, 1.22f, 0.25f), new Vector3(0.46f, 0.34f, 0.13f), dark);
        Part(PrimitiveType.Sphere, "Head", new Vector3(0f, 1.79f, 0f), new Vector3(0.37f, 0.42f, 0.36f), skin);
        Part(PrimitiveType.Sphere, "Helmet", new Vector3(0f, 1.93f, 0f), new Vector3(0.46f, 0.25f, 0.43f), dark);
        Part(PrimitiveType.Cube, "Visor", new Vector3(0f, 1.79f, 0.19f), new Vector3(0.34f, 0.10f, 0.05f), new Color(0.07f, 0.18f, 0.22f));
        Part(PrimitiveType.Cube, "Backpack", new Vector3(0f, 1.28f, -0.28f), new Vector3(0.36f, 0.42f, 0.18f), dark);
        Part(PrimitiveType.Cube, "Rifle", new Vector3(0.25f, 1.16f, 0.40f), new Vector3(0.12f, 0.12f, 0.72f), dark);
        Part(PrimitiveType.Cube, "Magazine", new Vector3(0.24f, 0.99f, 0.34f), new Vector3(0.10f, 0.22f, 0.15f), new Color(0.24f, 0.27f, 0.28f));

        for (int side = -1; side <= 1; side += 2)
        {
            Part(PrimitiveType.Capsule, "Arm", new Vector3(side * 0.43f, 1.15f, 0.02f), new Vector3(0.25f, 0.40f, 0.25f), armor);
            Part(PrimitiveType.Capsule, "Leg", new Vector3(side * 0.17f, 0.43f, 0f), new Vector3(0.28f, 0.46f, 0.29f), dark);
            Part(PrimitiveType.Cube, "Boot", new Vector3(side * 0.17f, 0.10f, 0.08f), new Vector3(0.30f, 0.16f, 0.42f), dark);
            Part(PrimitiveType.Cube, "Shoulder pad", new Vector3(side * 0.43f, 1.42f, 0f), new Vector3(0.27f, 0.22f, 0.30f), dark);
        }
    }

    private void Update()
    {
        if (!IsAlive || ProjectStrikeBootstrap.Instance == null || ProjectStrikeBootstrap.Instance.RoundTransition) return;
        animationClock += Time.deltaTime;
        fireTimer -= Time.deltaTime;

        StrikePlayer playerTarget;
        StrikeBot botTarget;
        Transform target = ProjectStrikeBootstrap.Instance.FindEnemyTarget(Team, transform.position, out playerTarget, out botTarget);
        if (target == null)
        {
            controller.SimpleMove(Vector3.zero);
            return;
        }

        Vector3 toTarget = target.position - transform.position;
        Vector3 flat = new Vector3(toTarget.x, 0f, toTarget.z);
        float distance = flat.magnitude;
        if (distance > 0.05f)
            transform.rotation = Quaternion.Slerp(transform.rotation, Quaternion.LookRotation(flat.normalized), Time.deltaTime * 5f);

        if (distance > 7f)
            controller.SimpleMove(flat.normalized * 2.7f);
        else
            controller.SimpleMove(Vector3.zero);

        if (distance < 19f && fireTimer <= 0f)
            TryAttack(target, playerTarget, botTarget);

        if (model != null)
            model.localPosition = new Vector3(0f, Mathf.Sin(animationClock * 5f) * 0.015f, 0f);
    }

    private void TryAttack(Transform target, StrikePlayer playerTarget, StrikeBot botTarget)
    {
        fireTimer = 1.15f + Random.Range(0f, 0.35f);
        Vector3 origin = transform.position + Vector3.up * 1.42f;
        Vector3 destination = target.position + Vector3.up * 1.15f;
        Vector3 direction = destination - origin;
        RaycastHit hit;
        if (!Physics.Raycast(origin, direction.normalized, out hit, direction.magnitude + 0.25f)) return;

        if (playerTarget != null && hit.collider.GetComponentInParent<StrikePlayer>() == playerTarget)
            playerTarget.TakeDamage(9);
        else if (botTarget != null && hit.collider.GetComponentInParent<StrikeBot>() == botTarget)
            botTarget.TakeDamage(9, false);
    }

    public void TakeDamage(int amount, bool byPlayer)
    {
        if (!IsAlive) return;
        health = Mathf.Max(0, health - amount);
        if (health > 0) return;
        IsAlive = false;
        KillerWasPlayer = byPlayer;
        if (byPlayer) ProjectStrikeBootstrap.Instance.RegisterPlayerKill();
        Destroy(gameObject);
    }
}
