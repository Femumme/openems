# Implementation Plan: EVCS Grid Price Floor Controller

Spec: docs/specs/evcs-grid-price-floor-controller.md

## Tasks

### T1 — Create OSGi Bundle and Configuration
- **Files**: `io.openems.edge.controller.evcs.gridpricefloor/bnd.bnd`, `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/Config.java`
- **Depends on**: none
- **Description**: Set up the OSGi bundle configuration (`bnd.bnd`) importing necessary APIs (`evcs.pricing.api`, `timeofusetariff.api`). Create the `@ObjectClassDefinition` in `Config.java` with properties `id`, `alias`, `enabled`, and `margin`.
- **Acceptance criteria**:
  - [ ] `bnd.bnd` correctly exposes the bundle and includes buildpath dependencies.
  - [ ] `Config.java` contains all attributes with their required defaults and types.

### T2 — Define Component Interface and Channels
- **Files**: `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloor.java`
- **Depends on**: T1
- **Description**: Define the `EvcsGridPriceFloor` interface extending `Controller` and `EvcsPricingController`. Define the `ACTIVE_FLOOR` and `AVERAGE_GRID_PRICE` channels according to the spec.
- **Acceptance criteria**:
  - [ ] Interface correctly defines the channels.
  - [ ] Channels are properly typed (EUR/kWh for ACTIVE_FLOOR, ct/kWh for AVERAGE_GRID_PRICE).

### T3 — Implement Controller Logic
- **Files**: `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloorImpl.java`
- **Depends on**: T2
- **Description**: Implement `EvcsGridPriceFloorImpl`. Inject `EvcsPricing` and optionally `TimeOfUseTariff`. Compute the forecast average grid price up to `NEXT_PRICE_CHANGE`, add the configured `margin`, convert to EUR/kWh, and call `EvcsPricing.addPriceFloor()`. Clean up constraints on deactivate.
- **Acceptance criteria**:
  - [ ] The average price calculation extends to at least 1 value if the next tick is < 15min away.
  - [ ] Removes constraint cleanly when disabled or deactivated.
  - [ ] Properly updates the custom channels (`ACTIVE_FLOOR`, `AVERAGE_GRID_PRICE`).
  - [ ] Converts Currency/MWh to ct/kWh (÷ 10) and then ct/kWh to EUR/kWh (÷ 100) before submission.

### T4 — Test Configuration and Unit Tests
- **Files**: `io.openems.edge.controller.evcs.gridpricefloor/test/io/openems/edge/controller/evcs/gridpricefloor/MyConfig.java`, `io.openems.edge.controller.evcs.gridpricefloor/test/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloorImplTest.java`
- **Depends on**: T3
- **Description**: Provide `MyConfig` builder for tests. Implement JUnit 4 tests simulating the component lifecycle with `ControllerTest` and a dummy `EvcsPricing` / `TimeOfUseTariff`.
- **Acceptance criteria**:
  - [ ] Tests verify margin behavior, handling of negative prices, and zero/empty prices.
  - [ ] Tests assert proper unit conversions based on input values.

### T5 — Add Bundle to Edge App Resolution
- **Files**: `io.openems.edge.application/EdgeApp.bndrun`
- **Depends on**: T1
- **Description**: Add the newly created `io.openems.edge.controller.evcs.gridpricefloor` bundle to the resolution list of the Edge Application to be packaged during the build.
- **Acceptance criteria**:
  - [ ] The bundle is present in `EdgeApp.bndrun`.

## Execution Order

Sequential: T1 → T2 → T3 → T4
Parallel: [T5] can run after T1
