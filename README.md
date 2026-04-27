# LedgerMem for JetBrains IDEs

Persistent memory for AI coding workflows in **IntelliJ IDEA, WebStorm, PyCharm, GoLand, RubyMine, PhpStorm, CLion, and Rider**.

## Features

- `Tools > LedgerMem > Search Memory` — quick popup search across your workspace.
- `Tools > LedgerMem > Add Selection` — save the highlighted code with file/line metadata.
- Right-side **LedgerMem** tool window — recent memories, refresh, delete.
- Keymap shortcuts: `Cmd+Alt+M` / `Ctrl+Alt+M` (search), `Cmd+Alt+A` / `Ctrl+Alt+A` (add).

## Install

### From the JetBrains Marketplace

Open *Settings > Plugins > Marketplace* and search for `LedgerMem`.

### From a local zip

```bash
git clone https://github.com/ledgermem/ledgermem-jetbrains
cd ledgermem-jetbrains
./gradlew buildPlugin
```

The packaged distribution lands in `build/distributions/LedgerMem-<version>.zip`. In your IDE, *Settings > Plugins > gear icon > Install Plugin from Disk...* and select that zip.

## Configuration

Open *Tools > LedgerMem > Search Memory* once and you will be prompted for setup. Or set the values via *Help > Edit Custom Properties...*:

| Key | Description |
| --- | --- |
| `ledgermem.apiKey` | Personal API key |
| `ledgermem.workspaceId` | Workspace to read/write |
| `ledgermem.endpoint` | API base URL (default: `https://api.ledgermem.dev`) |
| `ledgermem.defaultLimit` | Result count for search and tool window |

Values are stored via IntelliJ's `PropertiesComponent` and are scoped to the application install.

## Build from source

Requires JDK 17.

```bash
./gradlew test           # unit tests
./gradlew runIde         # launch a sandbox IDE with the plugin loaded
./gradlew buildPlugin    # produce the distributable
./gradlew verifyPlugin   # IntelliJ Plugin Verifier
```

CI runs `./gradlew check buildPlugin` against the platform version pinned in `gradle.properties` (single Java 17 build, no matrix).

## License

MIT — see [LICENSE](./LICENSE).
