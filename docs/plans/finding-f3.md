# Implementation Plan: Finding F3 - Deduplicate resolveNextTick

Spec: docs/specs/finding-f3.md

## Tasks

### T1 — Add resolveNextTick to EvcsPricingUtils
- **Files**: `io.openems.edge.evcs.pricing.api/src/io/openems/edge/evcs/pricing/EvcsPricingUtils.java`
- **Depends on**: none
- **Description**: Add a new public static method `resolveNextTick(ZonedDateTime nowRounded, EvcsPricing evcsPricing)` containing the logic for next tick resolution. Add necessary imports (`java.time.Instant`, `java.time.ZonedDateTime`).
- **Acceptance criteria**:
  - [ ] `resolveNextTick` method is added as a public static utility.
  - [ ] Compiles successfully with the required imports.

### T2 — Refactor EvcsGridPriceFloorImpl
- **Files**: `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloorImpl.java`
- **Depends on**: T1
- **Description**: Remove the duplicated local `resolveNextTick()` method. Replace its call site `this.resolveNextTick(nowRounded)` with `EvcsPricingUtils.resolveNextTick(nowRounded, this.evcsPricing)`. Ensure the `EvcsPricingUtils` import exists.
- **Acceptance criteria**:
  - [ ] Local `resolveNextTick` method is removed.
  - [ ] `EvcsGridPriceFloorImpl` correctly calls the shared utility method instead.
  - [ ] Bundle compiles successfully without errors.

### T3 — Refactor ControllerEvcsGridPricingImpl
- **Files**: `io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricingImpl.java`
- **Depends on**: T1
- **Description**: Remove the duplicated local `resolveNextTick()` method. Replace its call site `this.resolveNextTick(nowRounded)` with `EvcsPricingUtils.resolveNextTick(nowRounded, this.evcsPricing)`. Ensure the `EvcsPricingUtils` import exists.
- **Acceptance criteria**:
  - [ ] Local `resolveNextTick` method is removed.
  - [ ] `ControllerEvcsGridPricingImpl` correctly calls the shared utility method instead.
  - [ ] Bundle compiles successfully without errors.

## Execution Order

Sequential: T1 → [T2, T3]
Parallel groups: T2 and T3 can be executed in parallel after T1 completes.