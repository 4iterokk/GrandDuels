# Role & Mandate
You are an expert Senior Minecraft Systems Engineer and Java Developer specializing in the Paper 1.21.11 API.
Your goal is to build a modern, high-performance, robust, and scalable Minecraft Dueling plugin named **GrandDuels**.
You must adhere strictly to clean code practices (SOLID principles, OOP, modern Java 21 features), thread safety, and Paper 1.21.11 API standards.
**You have access to full Git repo of this plugin to see whole project to improve context.**

---

## Technical Specifications & Stack
- **Target Platform:** Paper API 1.21.11 (Java 21)
- **Build System:** Apache Maven (`pom.xml`)
- **Main Package:** `org.chiterok.grandDuels`
- **Utility Target:** MUST use `org.chiterok.grandDuels.utils.ColorUtil` for all text, titles, action bars, and lore colorization.

---

## Architectural & Technical Requirements

### 1. Build & Project Setup (`pom.xml`)
- Use Java 21 compiler source/target compatibility.
- Depend on `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`.
- Add essential dependencies:
    - FastBoard or custom packet-based Scoreboard library
    - An internal/shadowed Config library or Bukkit Configuration API wrapper
- Include Maven Compiler and Shade plugins properly configured.

### 2. Modern 1.21+ Features & Item Meta Components (`DataComponentType`)
- **Full DataComponentType Support:** Item kits MUST natively support 1.21+ Data Components instead of deprecated NBT/Custom Model Data workarounds (e.g., custom food components, potion attributes, specialized armor trims, maces with heavy density/breeze charges, wind charges, wolf armor).
- **Kit Customization Engine:** Kits should allow fully dynamic serialization and deserialization (inventory, offhand, armor, status effects) via `kits.yml`.
- **Modern PvP Mechanics:** Support modern combat elements introduced in 1.21+ (Wind Charge cooldowns, Mace smash knockback calculations, Spear charge attacks, and custom potion effects).

### 3. Dedicated Combat Cooldown & PvP Rules System
Implement an isolated `PvPCooldownManager` and event listener handling stateful match rules:
- **Enchanted Golden Apple & Golden Apple Cooldown:** Configurable per-item consumption cooldowns (e.g., 30s delay between eating Gapples) with customizable action bar alerts.
- **Trident & Ranged Weapon Cooldowns:** Custom throwable cooldowns for Tridents (Impaling/Riptide restrictions), Wind Charges, Ender Pearls, and Chorus Fruit inside duel arenas.
- **Combat Tagging & Anti-Exploit:** Disable elytra gliding during duels, block command execution (except `/duel cancel` or custom whitelist), prevent item dropping/picking up outside kit parameters, and stop enderpearl exploits outside arena boundaries.

### 4. Colorization & Messaging Architecture (`ColorUtil`)
- Place class at `org.chiterok.grandDuels.utils.ColorUtil.java`.
- Must process gradient codes, Hex colors (e.g. `<#RRGGBB>`, `&#RRGGBB`), and standard legacy color codes (`&a`, `&c`, etc.) translating into Kyori `Component`.
- All user-facing strings in Chat, Titles, Subtitles, Actionbars, Bossbars, and Item Lores must pass through `ColorUtil.colorize(...)`.
- **Categorized Messaging (`messages.yml`):**
    - `prefix`: Plugin prefix.
    - `duels.*`: Invites, accept, decline, ongoing status.
    - `cooldowns.*`: Specific messages for Gapple/Trident/Pearls cooldown.
    - `arena.*`: Setup, missing arenas, bound errors.
    - `kits.*`: Creation, loading, selection errors.
    - `admin.*`: Reloads, forced ends, debug commands.

### 5. Core Arena & Match Engine
- **Arena Management:**
    - Dual pos (`pos1`, `pos2`) setup per arena using native location setters.
    - Dynamic arena state handling (`WAITING`, `STARTING`, `IN_GAME`, `RESETTING`).
    - Automatic arena rollback / snapshot system (resets broken blocks if building is enabled, or simply prevents block updates, fire, and liquid flow).
- **Match Lifecycle:**
    - **Countdown Phase:** 5-second frozen countdown with Titles/Sounds (`BLOCK_NOTE_BLOCK_PLING`).
    - **In-Game Phase:** Real-time damage tracking, health displays (Scoreboard/Nameplates), spectator handling.
    - **Post-Match Phase:** Winner celebration (fireworks, sound effect), instant inventory restoration of pre-duel state, teleportation back to original locations (`lobby` or previous location).
    - **Stats Tracking:** Track Wins, Losses, K/D Ratio, Current Win Streak, and Highest Win Streak stored asynchronously (YAML/SQLite abstract manager).

### 6. Command & GUI Framework
- **Commands Structure:**
    - `/duel <player>` - Send duel request (opens Kit Selector GUI or uses default kit).
    - `/duel accept [player]` - Accept pending request.
    - `/duel deny [player]` - Deny request.
    - `/duel cancel` - Cancel sent request.
    - `/duel stats [player]` - View player duel statistics.
    - `/duels <arena|reload|savekit> <arena:createarena|setspawn1|setspawn2>` - Admin operations.
- **Interactive GUIs:**
    - **Kit Selector GUI:** Paginated or clean inventory layout showing available kits with custom icons, lore, and component indicators.
    - **Duel Settings / Request GUI:** Optionally toggle sub-rules (e.g., "Allow Gapples: ON/OFF", "Custom Cooldowns: ON/OFF").

---

## Deliverables Required
Please provide a complete, well-structured, production-ready codebase including:
1. `pom.xml` with all necessary dependencies and repositories.
2. `plugin.yml` with modern Paper metadata, permissions, and command aliases.
3. `ColorUtil.java` inside `org.chiterok.grandDuels.utils`.
4. `config.yml`, `messages.yml`, and sample `kits.yml`.
5. Modern event handling (using Kyori Adventure components, Paper-specific events, and non-deprecated methods).

---

## Code rules & Project architecture
1. Write clean, supportable, fully implemented Java code without skipping methods or using placeholders like `// TODO: Implement this`.
2. Simple and supportable architecture (especially for coding agents, other AI models) for implementing new features and not only.
3. SOLID paradigm (each class handles one feature)
4. Categorized/sorted Java classes by `packages`. For example: runtimes/listeners in "runtime" package, all commands/subcommands in "command" package, data handling in "data" package and so on.
5. Implement all pre-maded classes (if they are), cuz I have other  seeing (maybe) unlike you  of project architecture, class categorizing, naming, etc.

 