# Implementation Plan: Deduplicate clearConstraintChannels

Spec: docs/specs/f4-clear-constraint-channels.md

## Tasks

### T1 — Add default method and update GridPriceFloor controller
- **Files**: `io.openems.edge.evcs.pricing.api/src/io/openems/edge/evcs/pricing/EvcsPricingController.java`, `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloorImpl.java`
- **Depends on**: none
- **Description**: Add `default void clearConstraintChannels()` to `EvcsPricingController` interface with the standard body (setting ceiling, floor, and override to null). In `EvcsGridPriceFloorImpl.java`, remove its local `clearConstraintChannels()` method entirely so that the class delegates to the new interface default.
- **Acceptance criteria**:
  - [ ] `EvcsPricingController` contains the default `clearConstraintChannels()` method implementation.
  - [ ] `EvcsGridPriceFloorImpl` no longer defines a private `clearConstraintChannels()` method but successfully uses the default method.

### T2 — Update GridPricing controller
- **Files**: `io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricingImpl.java`
- **Depends on**: T1
- **Description**: Remove the local `clearConstraintChannels()` method from `ControllerEvcsGridPricingImpl.java` so it delegates to the interface default method instead.
- **Acceptance criteria**:
  - [ ] The local `clearConstraintChannels()` is removed from `ControllerEvcsGridPricingImpl`.

### T3 — Update PvPricing and BatteryPricing controllers
- **Files**: `io.openems.edge.controller.evcs.pvpricing/src/io/openems/edge/controller/evcs/pvpricing/ControllerEvcsPvPricingImpl.java`, `io.openems.edge.controller.evcs.batterypricing/src/io/openems/edge/controller/evcs/batterypricing/ControllerEvcsBatteryPricingImpl.java`
- **Depends on**: T1
- **Description**: In both `ControllerEvcsPvPricingImpl` and `ControllerEvcsBatteryPricingImpl`, update the body of their local `clearChannels()` methods to simply call `this.clearConstraintChannels();` (delegating to the default interface method) instead of duplicating the logic.
- **Acceptance criteria**:
  - [ ] `ControllerEvcsPvPricingImpl.clearChannels()` calls `this.clearConstraintChannels()`.
  - [ ] `ControllerEvcsBatteryPricingImpl.clearChannels()` calls `this.clearConstraintChannels()`.
  - [ ] Both controllers compile and correctly clear their channels.

## Execution Order

Sequential: T1 → [T2, T3]
Parallel groups: T2 and T3 can run in parallel after T1 completes.