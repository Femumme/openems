# EVCS Dynamic Pricing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the EVCS dynamic pricing system by adding cron-based interval scheduling, full test coverage, and registering the battery pricing controller in EdgeApp.bndrun.

**Architecture:** The system already exists — API (`io.openems.edge.evcs.pricing.api`), core singleton (`io.openems.edge.evcs.pricing.core`), and three controllers (fixed, PV, battery) are implemented. This plan fills the gaps: cron scheduling replaces `intervalMinutes`, tests cover all bundles, and the battery controller is wired into the Edge runtime.

**Tech Stack:** Java 21, OSGi/Felix, bnd 7, JUnit 4, `ControllerTest` / `AbstractComponentTest` framework. Cron parsing via a lightweight custom implementation (no new library dependency).

---

## Gap Summary

| Gap | Location | Impact |
|-----|----------|--------|
| No test coverage | All 5 pricing bundles | Regressions undetected |
| `intervalMinutes` instead of cron | `evcs.pricing.core` Config + Impl | Cannot express arbitrary schedules |
| Battery controller not in bndrun | `EdgeApp.bndrun` | Bundle never starts on Edge |

All paths below are relative to `openems/` unless stated otherwise.

---

## File Map

| Action | File |
|--------|------|
| Modify | `io.openems.edge.evcs.pricing.core/src/.../core/Config.java` |
| Modify | `io.openems.edge.evcs.pricing.core/src/.../core/EvcsPricingCoreImpl.java` |
| Create | `io.openems.edge.evcs.pricing.api/src/.../pricing/util/CronExpression.java` |
| Create | `io.openems.edge.evcs.pricing.api/src/.../pricing/util/CronExpressionTest.java` (in `test/`) |
| Create | `io.openems.edge.evcs.pricing.core/test/.../core/EvcsPricingCoreImplTest.java` |
| Create | `io.openems.edge.evcs.pricing.core/test/.../core/MyConfig.java` |
| Create | `io.openems.edge.controller.evcs.fixedpricing/test/.../fixedpricing/ControllerEvcsFixedPricingImplTest.java` |
| Create | `io.openems.edge.controller.evcs.fixedpricing/test/.../fixedpricing/MyConfig.java` |
| Create | `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/ControllerEvcsPvPricingImplTest.java` |
| Create | `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/MyConfig.java` |
| Create | `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/ControllerEvcsBatteryPricingImplTest.java` |
| Create | `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/MyConfig.java` |
| Modify | `io.openems.edge.application/EdgeApp.bndrun` |

---

## Task 1: Cron Parsing Utility

**Files:**
- Create: `io.openems.edge.evcs.pricing.api/src/io/openems/edge/evcs/pricing/util/CronExpression.java`
- Create: `io.openems.edge.evcs.pricing.api/test/io/openems/edge/evcs/pricing/util/CronExpressionTest.java`

The cron format to support is the 6-field Spring/Quartz variant: `seconds minutes hours day-of-month month day-of-week`. The default `0 0 * * * *` means "every full hour". Support `*` (any), `*/n` (every n), and fixed values per field. No need for ranges or lists in v1.

- [ ] Write failing unit tests for `CronExpression` covering:
  - Parsing `0 0 * * * *` — next tick from mid-hour lands at next full hour
  - Parsing `0 */30 * * * *` — next tick aligns to :00 or :30
  - Parsing `0 */15 * * * *` — quarter-hour alignment
  - Parsing `0 0 */2 * * *` — every 2 hours
  - Invalid expression throws `IllegalArgumentException`
- [ ] Run tests to confirm they fail: `./gradlew :io.openems.edge.evcs.pricing.api:test --tests "*.CronExpressionTest"`
- [ ] Implement `CronExpression` with a `nextTick(Instant from, ZoneId zone): Instant` method. Keep it under ~120 lines.
- [ ] Run tests to confirm they pass.
- [ ] Commit: `feat(evcs-pricing): add CronExpression utility for interval scheduling`

---

## Task 2: Swap Core Config from intervalMinutes to cronExpression

**Files:**
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/Config.java`
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImpl.java`

- [ ] In `Config.java`: replace `int intervalMinutes()` with `String cronExpression()` (default `"0 0 * * * *"`). Keep `absoluteMinPrice` and `absoluteMaxPrice` unchanged.
- [ ] In `EvcsPricingCoreImpl.java`:
  - Replace `this.intervalMinutes` field with `CronExpression this.cronExpression`
  - In `applyConfig`: parse the cron string via `new CronExpression(config.cronExpression())`, recompute `nextIntervalTick` using `cronExpression.nextTick(Instant.now(), ZoneId.systemDefault())`
  - In `resolvePrice`: replace `computeNextTick(now, this.intervalMinutes)` with `this.cronExpression.nextTick(now, ZoneId.systemDefault())`
  - Delete the static `computeNextTick` method (logic now lives in `CronExpression`)
- [ ] Add `io.openems.edge.evcs.pricing.api` to `io.openems.edge.evcs.pricing.core/bnd.bnd` `-buildpath` if not already present (needed for `CronExpression` import).
- [ ] Build: `./gradlew :io.openems.edge.evcs.pricing.core:build --console=plain --warn`
- [ ] Commit: `feat(evcs-pricing): replace intervalMinutes with cron expression in core config`

---

## Task 3: Tests for EvcsPricingCoreImpl

**Files:**
- Create: `io.openems.edge.evcs.pricing.core/test/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImplTest.java`
- Create: `io.openems.edge.evcs.pricing.core/test/io/openems/edge/evcs/pricing/core/MyConfig.java`

The core is not a `Controller` — use `AbstractComponentTest` directly. `EvcsPricingCoreImpl` implements `EventHandler`, so the resolution cycle is driven by calling `handleEvent` with `TOPIC_CYCLE_BEFORE_CONTROLLERS` (clears ceilings/floors) then `TOPIC_CYCLE_AFTER_CONTROLLERS` (resolves price). Call `addPriceCeiling`/`addPriceFloor`/`setOverride` between the two events to simulate controller input.

- [ ] Write `MyConfig` following the `AbstractComponentConfig` builder pattern. Fields: `id`, `alias`, `enabled`, `cronExpression`, `absoluteMinPrice`, `absoluteMaxPrice`.
- [ ] Write failing tests covering:
  - **Override wins**: one override set → `PRICE` channel equals override value
  - **Ceiling resolution**: two ceilings → lowest wins
  - **Floor beats ceiling**: floor > ceiling → floor wins
  - **No constraints**: price stays at previous locked value
  - **Override removed**: after `removeOverride`, reverts to constraint-based price
  - **Absolute clamp**: price is clamped within `[absoluteMinPrice, absoluteMaxPrice]`
- [ ] Run tests to confirm they fail: `./gradlew :io.openems.edge.evcs.pricing.core:test`
- [ ] Implement any test helpers needed (e.g., a `DummyEvcsPricing` or driving the impl directly).
- [ ] Run tests to confirm they pass.
- [ ] Commit: `test(evcs-pricing): add unit tests for EvcsPricingCoreImpl`

---

## Task 4: Tests for ControllerEvcsFixedPricing

**Files:**
- Create: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImplTest.java`
- Create: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/MyConfig.java`

Use `ControllerTest`. Create a `DummyEvcsPricing` (implementing `EvcsPricing`) to capture `setOverride`/`removeOverride` calls.

- [ ] Write `MyConfig` with fields: `id`, `alias`, `enabled`, `priceEurPerKwh`.
- [ ] Write failing tests:
  - **Sets override**: after `run()`, `DummyEvcsPricing.setOverride` called with rounded price; `ACTIVE_OVERRIDE` channel equals that price
  - **Deactivate removes override**: `deactivate()` triggers `removeOverride`
  - **Disabled**: when `enabled=false`, `run()` calls neither `setOverride` nor sets `ACTIVE_OVERRIDE`
- [ ] Run to confirm failure: `./gradlew :io.openems.edge.controller.evcs.fixedpricing:test`
- [ ] Fix any compilation issues revealed by the test setup.
- [ ] Run to confirm pass.
- [ ] Commit: `test(evcs-pricing): add tests for ControllerEvcsFixedPricingImpl`

---

## Task 5: Tests for ControllerEvcsPvPricing

**Files:**
- Create: `io.openems.edge.controller.evcs.pvpricing/test/io/openems/edge/controller/evcs/pvpricing/ControllerEvcsPvPricingImplTest.java`
- Create: `io.openems.edge.controller.evcs.pvpricing/test/io/openems/edge/controller/evcs/pvpricing/MyConfig.java`

Use `ControllerTest` with a `DummySum` and `DummyEvcsPricing`.

- [ ] Write `MyConfig` with fields: `id`, `alias`, `enabled`, `maxCeiling`, `minCeiling`, `pvThreshold`, `pvFullProduction`, `dataCollectionWindowMinutes`.
- [ ] Write failing tests:
  - **Below threshold**: PV production < `pvThreshold` → no `addPriceCeiling` call, `ACTIVE_CEILING` is null
  - **At threshold**: PV = `pvThreshold` → ceiling equals `maxCeiling`
  - **At full production**: PV = `pvFullProduction` → ceiling equals `minCeiling`
  - **Interpolated**: PV midpoint → ceiling is interpolated value (verify formula)
  - **Missing PV value**: `getProductionActivePower()` returns `null`/error → channels cleared, no exception
- [ ] Run to confirm failure: `./gradlew :io.openems.edge.controller.evcs.pvpricing:test`
- [ ] Run to confirm pass.
- [ ] Commit: `test(evcs-pricing): add tests for ControllerEvcsPvPricingImpl`

---

## Task 6: Tests for ControllerEvcsBatteryPricing

**Files:**
- Create: `io.openems.edge.controller.evcs.batterypricing/test/io/openems/edge/controller/evcs/batterypricing/ControllerEvcsBatteryPricingImplTest.java`
- Create: `io.openems.edge.controller.evcs.batterypricing/test/io/openems/edge/controller/evcs/batterypricing/MyConfig.java`

Use `ControllerTest` with a `DummySum` and `DummyEvcsPricing`.

- [ ] Write `MyConfig` with fields: `id`, `alias`, `enabled`, `lowSocThreshold`, `highSocThreshold`, `lowSocFloorPrice`, `highSocCeilPrice`, `fullCeilPrice`, `dataCollectionWindowMinutes`.
- [ ] Write failing tests:
  - **Low SoC**: SoC < `lowSocThreshold` → `addPriceFloor` called with `lowSocFloorPrice`; `ACTIVE_FLOOR` set
  - **Mid SoC**: SoC between thresholds → no constraint, channels cleared
  - **At high SoC threshold**: SoC = `highSocThreshold` → ceiling equals `highSocCeilPrice`
  - **At 100% SoC**: ceiling equals `fullCeilPrice`
  - **Interpolated high SoC**: midpoint between thresholds and 100% → interpolated ceiling value
  - **Missing SoC**: `getEssSoc()` returns error → channels cleared, no exception
- [ ] Run to confirm failure: `./gradlew :io.openems.edge.controller.evcs.batterypricing:test`
- [ ] Run to confirm pass.
- [ ] Commit: `test(evcs-pricing): add tests for ControllerEvcsBatteryPricingImpl`

---

## Task 7: Register Battery Pricing in EdgeApp.bndrun

**Files:**
- Modify: `io.openems.edge.application/EdgeApp.bndrun`

- [ ] Add `bnd.identity;id='io.openems.edge.controller.evcs.batterypricing',\` to `-runrequires` alongside the other EVCS pricing bundles (around line 207).
- [ ] Run bnd resolution: `./gradlew :io.openems.edge.application:resolve --console=plain --warn`
- [ ] Verify `io.openems.edge.controller.evcs.batterypricing;version=snapshot` appears in `-runbundles`.
- [ ] Commit: `fix(evcs-pricing): add battery pricing controller to EdgeApp.bndrun`

---

## Task 8: Full Build and Test

- [ ] Run full build: `./gradlew build --console=plain --warn` from `openems/`
- [ ] Confirm zero test failures and zero compilation errors.
- [ ] Run checkstyle: `./gradlew checkstyleAll --console=plain --warn`
- [ ] Fix any checkstyle violations.
- [ ] Commit any fixup: `fix(evcs-pricing): checkstyle cleanup`
