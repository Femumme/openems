# Implementation Plan: Extract `roundPrice` Utility

Spec: `docs/specs/extract-roundprice.md`

## Tasks

### T1 — Create EvcsPricingUtils class
- **Files**: `io.openems.edge.evcs.pricing.api/src/io/openems/edge/evcs/pricing/EvcsPricingUtils.java`
- **Depends on**: none
- **Description**: Create the new `EvcsPricingUtils` utility class with a private constructor and a public static `roundPrice` method.
- **Acceptance criteria**:
  - [ ] `EvcsPricingUtils.java` exists in the `io.openems.edge.evcs.pricing` package.
  - [ ] The class is `public final` and has a private constructor to prevent instantiation.
  - [ ] It contains `public static double roundPrice(double price)` matching the target logic (BigDecimal HALF_UP to 4 decimal places).

### T2 — Refactor Grid Controllers
- **Files**: 
  - `io.openems.edge.controller.evcs.gridpricefloor/src/io/openems/edge/controller/evcs/gridpricefloor/EvcsGridPriceFloorImpl.java`
  - `io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricingImpl.java`
- **Depends on**: T1
- **Description**: Remove the duplicated `roundPrice` method and replace usages with the new shared utility.
- **Acceptance criteria**:
  - [ ] Private `roundPrice` method is removed from both classes.
  - [ ] Method calls are updated to `EvcsPricingUtils.roundPrice`.
  - [ ] `java.math.BigDecimal` and `java.math.RoundingMode` imports are removed if no longer used.
  - [ ] `io.openems.edge.evcs.pricing.EvcsPricingUtils` is imported.

### T3 — Refactor PV and Battery Pricing Controllers
- **Files**: 
  - `io.openems.edge.controller.evcs.pvpricing/src/io/openems/edge/controller/evcs/pvpricing/ControllerEvcsPvPricingImpl.java`
  - `io.openems.edge.controller.evcs.batterypricing/src/io/openems/edge/controller/evcs/batterypricing/ControllerEvcsBatteryPricingImpl.java`
- **Depends on**: T1
- **Description**: Remove the duplicated `roundPrice` method and replace usages with the new shared utility.
- **Acceptance criteria**:
  - [ ] Private `roundPrice` method is removed from both classes.
  - [ ] Method calls are updated to `EvcsPricingUtils.roundPrice`.
  - [ ] `java.math.BigDecimal` and `java.math.RoundingMode` imports are removed if no longer used.
  - [ ] `io.openems.edge.evcs.pricing.EvcsPricingUtils` is imported.

### T4 — Refactor Fixed Pricing Controller
- **Files**: 
  - `io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImpl.java`
- **Depends on**: T1
- **Description**: Remove the duplicated `roundPrice` method and replace usages with the new shared utility.
- **Acceptance criteria**:
  - [ ] Private `roundPrice` method is removed.
  - [ ] Method calls are updated to `EvcsPricingUtils.roundPrice`.
  - [ ] `java.math.BigDecimal` and `java.math.RoundingMode` imports are removed if no longer used.
  - [ ] `io.openems.edge.evcs.pricing.EvcsPricingUtils` is imported.

## Execution Order

Sequential dependency from T1:
T1 → [T2, T3, T4]

Parallel groups: [T2, T3, T4] can run in parallel after T1 is completed.