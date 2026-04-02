# OpenEMS (Mumme-IT Fork)

OpenEMS is an open-source energy management platform for edge devices (Raspberry Pi) and a cloud backend; this fork adds EVCS dynamic pricing controllers and custom OSGi bundles on top of the upstream codebase.

## Repository Layout

| Path | What it is |
|---|---|
| `io.openems.edge.*/`, `io.openems.backend.*/` | OSGi bundles (Edge + Backend) |
| `io.openems.edge.controller.evcs.*/` | EVCS dynamic pricing controllers (Mumme-IT custom) |
| `io.openems.edge.evcs.pricing.*/` | EVCS pricing providers (core, eaaze, mumaxdisplay) |
| `ui/` | Angular/Ionic progressive web app |
| `docs/` | Mumme-IT specs and implementation plans |
| `plans/` | Legacy implementation plans |
| `edge-data-dir/` | OSGi config for local dev edge (gitignored) |
| `mise.toml`, `.mise/tasks/` | Build, run, deploy, and ops tasks (via [mise](https://mise.jdx.dev)) |
| `evcs-dynamic-pricing.md` | Architecture doc for the EVCS pricing feature |

## Architecture

- Pattern: OSGi monorepo — Edge runtime + Backend runtime + Angular UI, resolved via bnd/bndtools
- Language(s): Java 21 (OSGi bundles), TypeScript/Angular 17 + Ionic (UI)
- Key frameworks: OSGi (Apache Felix), bnd 7, Gradle 9, Angular + Ionic, JUnit 4, Karma/Jasmine
- Data layer: InfluxDB / TimescaleDB (time-series), Odoo (metadata/CRM), optional file-based metadata

## Modules / Components

| Module | Path | Responsibility |
|---|---|---|
| Edge application | `io.openems.edge.application/` | OSGi Edge runtime entry point (`EdgeApp.bndrun`) |
| Backend application | `io.openems.backend.application/` | OSGi Backend runtime entry point (`BackendApp.bndrun`) |
| Edge common | `io.openems.edge.common/` | Shared Edge APIs, channel framework, component lifecycle |
| Backend common | `io.openems.backend.common/` | Shared Backend APIs and abstractions |
| Common | `io.openems.common/` | Shared DTOs, JSON-RPC protocol, constants |
| ESS bundles | `io.openems.edge.ess.*/` | Energy Storage System drivers (Fenecon, Samsung, BYD, ...) |
| Battery bundles | `io.openems.edge.battery.*/` | Battery drivers (Fenecon Home, BYD, Soltaro, BMW, ...) |
| EVCS bundles | `io.openems.edge.evcs.*/` | EV Charging Station drivers (go-e, KEBA, OCPP, Alpitronic, ...) |
| EVCS pricing controllers | `io.openems.edge.controller.evcs.*/` | EVCS dynamic pricing (fixed, PV, battery, grid-price strategies) |
| ESS controllers | `io.openems.edge.controller.ess.*/` | ESS control strategies (balancing, peak-shaving, ToU tariff, ...) |
| Bridge bundles | `io.openems.edge.bridge.*/` | Protocol bridges (Modbus, HTTP, M-Bus, 1-Wire) |
| Timedata backends | `io.openems.backend.timedata.*/` | Time-series storage (InfluxDB, TimescaleDB, aggregated) |
| Metadata backends | `io.openems.backend.metadata.*/` | Device/user metadata (Odoo, file, dummy) |
| UI | `ui/` | Angular/Ionic progressive web app for monitoring and configuration |

## Data Flow

- Edge reads hardware (batteries, inverters, EVCS, meters) via bridge bundles over Modbus/HTTP/OCPP
- Controllers compute setpoints each cycle and write back via channels
- Edge API controller streams channel data to Backend via WebSocket (JSON-RPC)
- Backend stores time-series in InfluxDB/TimescaleDB; UI reads via Backend WebSocket

## Key Entry Points

| Entrypoint | Path | Purpose |
|---|---|---|
| Edge bndrun | `io.openems.edge.application/EdgeApp.bndrun` | Bundle resolution manifest for Edge |
| Backend bndrun | `io.openems.backend.application/BackendApp.bndrun` | Bundle resolution manifest for Backend |
| Edge JAR | `build/openems-edge.jar` | Built OSGi Edge runtime |
| Backend JAR | `build/openems-backend.jar` | Built OSGi Backend runtime |
| UI dev server | `ui/` | `ng serve -c openems-edge-dev` |
| Docker Edge | `Dockerfile` | Edge container image (OpenJDK 21) |

## External Dependencies

- **InfluxDB** -- time-series storage for channel data
- **TimescaleDB** -- alternative/aggregate time-series storage
- **Odoo** -- ERP metadata backend (edges, users, devices)
- **OCPP server** -- protocol endpoint for OCPP-compatible EV chargers
- **Codecov** -- coverage reporting (CI)

## Developer Notes

All commands run from the **repo root**.

### Build & Test

| Command | What it does |
|---|---|
| `mise run build:edge` | Build the Edge JAR (`./gradlew buildEdge`) |
| `mise run build:ui` | Production UI build |
| `mise run test` | Run all Java tests |
| `mise run test:ui` | Run UI tests with coverage |
| `mise run lint` | Checkstyle on all Java bundles |
| `mise run lint:ui` | ESLint on UI |
| `mise run resolve` | Re-resolve OSGi bndrun files after bundle changes |

### Local Dev

| Command | What it does |
|---|---|
| `mise run run:edge` | Run Edge locally (requires built JAR + `edge-data-dir/`) |
| `mise run run:ui` | Start UI dev server with hot reload |
| `mise run start:remote-edge` | Start remote Edge via Docker Compose |

```bash
# Terminal 1
mise run run:edge

# Terminal 2
mise run run:ui
```

**Default credentials** (Edge only): username `admin`, password `admin`.
UI dev config auto-connects as GUEST; navigate to `http://localhost:4200/login` to log in.

**Restart Edge after JAR rebuild or config changes** -- OSGi ConfigAdmin does not hot-reload files.
Override the data dir: `EDGE_DATA_DIR=/path/to/dir mise run run:edge`

### Deploy

| Command | What it does |
|---|---|
| `mise run deploy-edge` | Deploy Edge JAR to Pi (interactive confirmation) |
| `mise run deploy-ui` | Deploy UI build to Pi (interactive confirmation) |
| `mise run copy-config` | Copy OSGi config between edges |
| `mise run publish-builds` | Publish Edge + UI builds to openems-build repo |

Deploy targets: `pi` (default, 192.168.89.204) or `revpi` (revpi134791.local).
Pass target as argument: `mise run deploy-edge revpi`

### Toolchain

- Java 21 (source + target); configured in `gradle.properties`
- After adding/removing bundles: `mise run resolve` and commit updated bndrun files
