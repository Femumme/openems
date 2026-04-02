# EVCS Pricing Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all High and Medium findings from the code review of commits `111c24f8`–`170a6b748f`.

**Architecture:** Six targeted fixes in three layers: (1) correctness bugs in `EvcsPricingCoreImpl` (deterministic override resolution, floor-only constraint handling, clock injection), (2) missing deactivation cleanup in `BatteryPricing` and `PvPricing` controllers, and (3) DRY/duplication fixes in test infrastructure and UI. Each task is independent and can be committed separately.

**Tech Stack:** Java 21, OSGi/Felix, JUnit 4, `ControllerTest` / `AbstractComponentTest`, Angular/TypeScript. All commands run from `openems/`.

---

## Findings Addressed

| Finding | Severity | Description |
|---------|----------|-------------|
| F1 | High | Arbitrary override winner (HashMap iteration order) |
| F2 | Medium | Clamp not applied on quiet (no-lock) cycles after `@Modified` |
| F4 | Medium | Three duplicated `DummyEvcsPricing` classes with diverging state |
| F5 | Medium | UI duplicates `EVCS_PRICING_ID` constant and channel-read logic |
| F8 | High | Floor-only constraint silently ignored in `resolveConstraints` |
| F9 | Medium | `Instant.now()` used directly — clock is untestable |
| F10 | Medium | `BatteryPricing`/`PvPricing` don't call `removeConstraint` on deactivate |

---

## File Map

| Action | File |
|--------|------|
| Modify | `io.openems.edge.evcs.pricing.core/src/.../core/EvcsPricingCoreImpl.java` |
| Modify | `io.openems.edge.evcs.pricing.core/test/.../core/EvcsPricingCoreImplTest.java` |
| Modify | `io.openems.edge.evcs.pricing.core/test/.../core/MyConfig.java` |
| Modify | `io.openems.edge.controller.evcs.batterypricing/src/.../batterypricing/ControllerEvcsBatteryPricingImpl.java` |
| Modify | `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/ControllerEvcsBatteryPricingImplTest.java` |
| Modify | `io.openems.edge.controller.evcs.pvpricing/src/.../pvpricing/ControllerEvcsPvPricingImpl.java` |
| Modify | `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/ControllerEvcsPvPricingImplTest.java` |
| Create | `io.openems.edge.evcs.pricing.api/test/.../pricing/test/DummyEvcsPricing.java` *(shared test fixture)* |
| Modify | `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/DummyEvcsPricing.java` *(replace with shared)* |
| Modify | `io.openems.edge.controller.evcs.fixedpricing/test/.../fixedpricing/DummyEvcsPricing.java` *(replace with shared)* |
| Modify | `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/DummyEvcsPricing.java` *(replace with shared)* |
| Create | `ui/src/app/edge/live/Controller/Evcs/evcs-pricing.constants.ts` |
| Modify | `ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.ts` |
| Modify | `ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.ts` |

All Java paths are relative to `openems/` and expand to:
- `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/`
- `io.openems.edge.evcs.pricing.api/test/io/openems/edge/evcs/pricing/`
- `io.openems.edge.controller.evcs.*/`

---

## Task 1: Fix F8 — Floor-only constraint silently ignored

**Files:**
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImpl.java` (lines 179–191)
- Modify: `io.openems.edge.evcs.pricing.core/test/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImplTest.java`

**Problem:** `resolveConstraints()` returns the current locked price whenever no ceiling exists — even if a floor is present. A `BatteryPricing` controller adding only a floor (low-SoC case, no ceiling active) has no effect on the locked price.

- [ ] **Write a failing test** in `EvcsPricingCoreImplTest`:

```java
/**
 * When only a floor is set (no ceiling), the price must rise to at least
 * the floor value, not stay at the previous locked value.
 *
 * Cycle 1: lock price=0.10 via override.
 * Cycle 2: remove override, add only floor=0.40 → price must become 0.40.
 */
@Test
public void floorOnly_priceMeetsFloor() throws Exception {
    var sut = new EvcsPricingCoreImpl();

    new ComponentTest(sut) //
            .activate(defaultConfig()) //
            .next(new TestCase("prime price") //
                    .onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.10))) //
            .next(new TestCase("floor only") //
                    .onExecuteControllersCallbacks(() -> {
                        sut.removeOverride("ctrl0");
                        sut.addPriceFloor("ctrl1", 0.40);
                    }) //
                    .output(EvcsPricing.ChannelId.PRICE, 0.40)) //
            .deactivate();
}
```

- [ ] **Run test to confirm it fails:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.core:test --tests "*.EvcsPricingCoreImplTest.floorOnly_priceMeetsFloor"
  ```
  Expected: FAIL (price stays at 0.10 instead of 0.40).

- [ ] **Fix `resolveConstraints()`** — when no ceiling exists, check if there is a floor, and if so return it (rather than the old locked price):

  Replace (lines 179–191):
  ```java
  private double resolveConstraints() {
      var ceiling = this.ceilings.values().stream()
              .min(Double::compareTo);
      if (ceiling.isEmpty()) {
          // No constraints at all — keep the current price
          var current = this.getPrice().asOptional();
          return current.orElse(0.0);
      }
      var floor = this.floors.values().stream()
              .max(Double::compareTo)
              .orElse(0.0);
      return Math.max(floor, ceiling.get());
  }
  ```
  With:
  ```java
  private double resolveConstraints() {
      var ceiling = this.ceilings.values().stream().min(Double::compareTo);
      var floor = this.floors.values().stream().max(Double::compareTo);

      if (ceiling.isEmpty() && floor.isEmpty()) {
          // No constraints — keep the current locked price unchanged
          return this.getPrice().asOptional().orElse(0.0);
      }
      if (ceiling.isEmpty()) {
          // Floor only — price must meet the floor
          return floor.get();
      }
      // Ceiling present (floor optional) — floor wins if higher than ceiling
      return Math.max(floor.orElse(0.0), ceiling.get());
  }
  ```

- [ ] **Run tests** to confirm the new test and all existing core tests pass:
  ```
  ./gradlew :io.openems.edge.evcs.pricing.core:test
  ```
  Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Commit:**
  ```
  git add io.openems.edge.evcs.pricing.core/
  git commit -m "fix(evcs-pricing): floor-only constraint now sets price instead of being silently ignored"
  ```

---

## Task 2: Fix F1 — Deterministic override winner

**Files:**
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImpl.java` (~line 46, field declaration + line 154)

**Problem:** `overrides` is a `HashMap` — iteration order is undefined when multiple sources call `setOverride()`. The "active" override is effectively random.

**Decision:** Highest-price override wins (most conservative — protects battery/grid). This is the least surprising rule for energy management. Document it in `EvcsPricing.setOverride` Javadoc.

- [ ] **Fix `resolveActiveOverride()`** — the `overrides` field stays as `HashMap`; the fix is purely in how we pick the winner. No field change is needed.

  Replace `resolveActiveOverride()` body (lines 148–158):
  ```java
  private Double resolveActiveOverride() {
      if (this.overrides.isEmpty()) {
          this._setActiveOverrideSource(null);
          this._setActiveOverrideValue(null);
          return null;
      }
      var entry = this.overrides.entrySet().iterator().next();
      this._setActiveOverrideSource(entry.getKey());
      this._setActiveOverrideValue(entry.getValue());
      return entry.getValue();
  }
  ```
  With:
  ```java
  /**
   * Resolves the active override: when multiple sources have set an override,
   * the highest price wins (most conservative for grid protection).
   */
  private Double resolveActiveOverride() {
      if (this.overrides.isEmpty()) {
          this._setActiveOverrideSource(null);
          this._setActiveOverrideValue(null);
          return null;
      }
      var winner = this.overrides.entrySet().stream()
              .max(Map.Entry.comparingByValue())
              .get(); // safe: overrides is non-empty
      this._setActiveOverrideSource(winner.getKey());
      this._setActiveOverrideValue(winner.getValue());
      return winner.getValue();
  }
  ```

- [ ] **Update `EvcsPricing.setOverride` Javadoc** in `io.openems.edge.evcs.pricing.api/src/io/openems/edge/evcs/pricing/EvcsPricing.java` to state: *"When multiple controllers set an override simultaneously, the highest price wins."*

- [ ] **Add a test** to `EvcsPricingCoreImplTest`:

```java
/**
 * When two overrides are set, the higher price wins.
 */
@Test
public void multipleOverrides_highestPriceWins() throws Exception {
    var sut = new EvcsPricingCoreImpl();

    new ComponentTest(sut) //
            .activate(defaultConfig()) //
            .next(new TestCase("two overrides") //
                    .onExecuteControllersCallbacks(() -> {
                        sut.setOverride("ctrl0", 0.25);
                        sut.setOverride("ctrl1", 0.75);
                    }) //
                    .output(EvcsPricing.ChannelId.PRICE, 0.75) //
                    .output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_SOURCE, "ctrl1") //
                    .output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_VALUE, 0.75)) //
            .deactivate();
}
```

- [ ] **Run tests:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.core:test
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Commit:**
  ```
  git add io.openems.edge.evcs.pricing.core/ io.openems.edge.evcs.pricing.api/
  git commit -m "fix(evcs-pricing): highest override price wins when multiple sources are active"
  ```

---

## Task 3: Fix F9 — Inject a Clock for testability

**Files:**
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImpl.java`
- Modify: `io.openems.edge.evcs.pricing.core/test/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImplTest.java`

**Problem:** `Instant.now()` and `ZoneId.systemDefault()` are called directly in `applyConfig` and `lockPriceIfNeeded`. The test cannot advance time, so interval-tick promotion after the first cycle is never tested.

**Approach:** Add a package-private `setClock(Clock clock)` method (test-only setter). OSGi does not support constructor injection for the no-arg OSGi component constructor, so a setter is the idiomatic pattern here. `java.time.Clock` already combines instant and zone.

- [ ] **Add `Clock` field and setter** to `EvcsPricingCoreImpl`:

  After the field declarations (~line 61), add:
  ```java
  // Replaceable for testing; production uses the system clock
  private Clock clock = Clock.systemDefaultZone();
  ```

  After `deactivate()` (~line 95), add:
  ```java
  /** Replaces the clock for unit tests. Package-private. */
  void setClock(Clock clock) {
      this.clock = clock;
  }
  ```

  Add import: `import java.time.Clock;`

- [ ] **Replace `Instant.now()` / `ZoneId.systemDefault()` calls** with `this.clock.instant()` / `this.clock.getZone()`:

  In `applyConfig` (~line 86):
  ```java
  // Before:
  this.nextIntervalTick = this.cronExpression.nextTick(Instant.now(), ZoneId.systemDefault());
  // After:
  this.nextIntervalTick = this.cronExpression.nextTick(this.clock.instant(), this.clock.getZone());
  ```

  In `lockPriceIfNeeded` (~line 166):
  ```java
  // Before:
  var now = Instant.now();
  // After:
  var now = this.clock.instant();
  ```

- [ ] **Add a test** that exercises the interval-tick promotion path (currently untestable):

  Add a helper `MutableClock` inside `EvcsPricingCoreImplTest` (or use `Clock.fixed`):

  ```java
  /**
   * After the interval tick is reached, the locked price is updated even with
   * no override or override change.
   *
   * Strategy:
   * - Activate with a fixed clock set to T0.
   * - nextIntervalTick is computed as the next hourly tick after T0 (e.g. T0+1h).
   * - Advance clock to T0+1h+1s (past the tick).
   * - Next cycle: intervalReached=true → price is locked to the current resolved price.
   */
  @Test
  public void intervalTick_locksPrice() throws Exception {
      var baseTime = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC).toInstant();
      var tickTime = ZonedDateTime.of(2024, 6, 15, 11, 0, 1, 0, ZoneOffset.UTC).toInstant();

      var sut = new EvcsPricingCoreImpl();
      sut.setClock(Clock.fixed(baseTime, ZoneOffset.UTC));

      new ComponentTest(sut) //
              .activate(defaultConfig()) //
              // Cycle 1: before tick — prime a ceiling (no lock expected, interval not reached, no override)
              // Note: first activate sets nextIntervalTick = 11:00:00 (next hourly tick)
              // We need to NOT trigger a lock on cycle 1: no override, no override-change, interval not yet reached.
              // To avoid the Instant.MIN initial-lock, setClock BEFORE activate so nextIntervalTick is in future.
              .next(new TestCase("before tick — ceiling set but not yet locked") //
                      .onBeforeControllersCallbacks(() -> sut.setClock(Clock.fixed(baseTime, ZoneOffset.UTC))) //
                      .onExecuteControllersCallbacks(() -> sut.addPriceCeiling("ctrl0", 0.30))) //
              // Cycle 2: advance clock past tick — price should now be locked to 0.30
              .next(new TestCase("after tick — price locked") //
                      .onBeforeControllersCallbacks(() -> sut.setClock(Clock.fixed(tickTime, ZoneOffset.UTC))) //
                      .onExecuteControllersCallbacks(() -> sut.addPriceCeiling("ctrl0", 0.30)) //
                      .output(EvcsPricing.ChannelId.PRICE, 0.30)) //
              .deactivate();
  }
  ```

  > **Note:** If `AbstractComponentTest` does not support `onBeforeControllersCallbacks`, use `onExecuteControllersCallbacks` to set the clock before adding the ceiling in the same callback. Check the `AbstractComponentTest` API first — if there is no before-hook, drive the clock change by calling `sut.setClock(...)` directly before the `TestCase.next()` call, wrapping in a lambda if needed.

- [ ] **Run tests:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.core:test
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Commit:**
  ```
  git add io.openems.edge.evcs.pricing.core/
  git commit -m "fix(evcs-pricing): inject Clock into EvcsPricingCoreImpl for testability"
  ```

---

## Task 4: Fix F2 — Clamp not applied after `@Modified` until next tick

**Files:**
- Modify: `io.openems.edge.evcs.pricing.core/src/io/openems/edge/evcs/pricing/core/EvcsPricingCoreImpl.java` (`applyConfig`, ~line 82)

**Problem:** When the operator changes `absoluteMinPrice`/`absoluteMaxPrice` via OSGi config, the locked `PRICE` channel is not updated until the next cron interval tick. If the new max is below the current locked price, the system continues charging at an out-of-bounds price.

**Fix:** After updating config fields in `applyConfig`, immediately re-clamp and re-lock the current price.

- [ ] **Add a re-clamp call at the end of `applyConfig`:**

  ```java
  private void applyConfig(Config config) {
      this.cronExpression = new CronExpression(config.cronExpression());
      this.absoluteMinPrice = config.absoluteMinPrice();
      this.absoluteMaxPrice = config.absoluteMaxPrice();
      this.nextIntervalTick = this.cronExpression.nextTick(this.clock.instant(), this.clock.getZone());
      this._setNextPriceChange(this.nextIntervalTick.toEpochMilli());
      this.log.info("EVCS Pricing Core: cron={}, absolute=[{}, {}]",
              config.cronExpression(), this.absoluteMinPrice, this.absoluteMaxPrice);
      this.reclampLockedPrice();
  }

  /**
   * Re-clamps the currently locked PRICE channel to the new absolute bounds.
   * Called after config changes to take effect immediately without waiting
   * for the next cron tick.
   */
  private void reclampLockedPrice() {
      this.getPrice().asOptional().ifPresent(currentPrice -> {
          var clamped = clamp(currentPrice, this.absoluteMinPrice, this.absoluteMaxPrice);
          if (Double.compare(clamped, currentPrice) != 0) {
              this._setPrice(clamped);
          }
      });
  }
  ```

- [ ] **Make `applyConfig` package-private** (change `private` to the default access modifier) so the test can call it directly:

  In `EvcsPricingCoreImpl.java`:
  ```java
  // Before:
  private void applyConfig(Config config) {
  // After:
  void applyConfig(Config config) {  // package-private for testing
  ```

  The test is in the same package (`io.openems.edge.evcs.pricing.core`) so this is sufficient.
  `AbstractComponentTest` has no `.modify()` method, so calling `applyConfig` directly is the right approach.

- [ ] **Add a test** to `EvcsPricingCoreImplTest`:

  ```java
  /**
   * When absoluteMaxPrice is tightened via config change, the locked PRICE is
   * immediately clamped — it must not wait until the next cron tick.
   *
   * Cycle 1: lock price=0.80 via override.
   * applyConfig: change absoluteMaxPrice to 0.50.
   * Expected: PRICE channel is immediately clamped to 0.50.
   */
  @Test
  public void configChange_clampsTightensLockedPrice() throws Exception {
      var sut = new EvcsPricingCoreImpl();

      new ComponentTest(sut) //
              .activate(defaultConfig()) // max=9.99
              .next(new TestCase("lock price at 0.80") //
                      .onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.80)) //
                      .output(EvcsPricing.ChannelId.PRICE, 0.80)) //
              .deactivate();

      // Simulate @Modified: apply a config with lower max price
      var tightConfig = MyConfig.create() //
              .setId("_evcsPricing") //
              .setAlias("EVCS Pricing") //
              .setEnabled(true) //
              .setCronExpression("0 0 * * * *") //
              .setAbsoluteMinPrice(0.00) //
              .setAbsoluteMaxPrice(0.50) //
              .build();
      sut.applyConfig(tightConfig); // package-private, same package

      assertEquals(0.50, sut.getPrice().asOptional().orElse(-1.0), 1e-9);
  }
  ```

- [ ] **Run tests:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.core:test
  ```

- [ ] **Commit:**
  ```
  git add io.openems.edge.evcs.pricing.core/
  git commit -m "fix(evcs-pricing): re-clamp locked price immediately after @Modified config change"
  ```

---

## Task 5: Fix F10 — Controllers don't call `removeConstraint` on deactivate

**Files:**
- Modify: `io.openems.edge.controller.evcs.batterypricing/src/.../batterypricing/ControllerEvcsBatteryPricingImpl.java`
- Modify: `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/ControllerEvcsBatteryPricingImplTest.java`
- Modify: `io.openems.edge.controller.evcs.pvpricing/src/.../pvpricing/ControllerEvcsPvPricingImpl.java`
- Modify: `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/ControllerEvcsPvPricingImplTest.java`

**Problem:** When either controller is deactivated at runtime, its ceiling/floor lingers in the core's map until the next `BEFORE_CONTROLLERS` clear. This causes a single-cycle stale price lock.

- [ ] **Add test to `ControllerEvcsBatteryPricingImplTest`** before fixing the implementation:

  ```java
  /**
   * On deactivate(), removeConstraint must be called with the controller ID.
   */
  @Test
  public void deactivate_removesConstraint() throws Exception {
      var dummy = new DummyEvcsPricing();
      var sum = new DummySum().withEssSoc(80);

      new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
              .addReference("evcsPricing", dummy) //
              .addReference("sum", sum) //
              .activate(baseConfig()) //
              .next(new TestCase()) //
              .deactivate();

      assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
  }
  ```

  Add `getLastRemoveConstraintSource()` to `DummyEvcsPricing` (battery) — see next task for the shared dummy refactor, but for now add it directly:

  In `DummyEvcsPricing` (batterypricing):
  ```java
  private String lastRemoveConstraintSource;

  public String getLastRemoveConstraintSource() {
      return this.lastRemoveConstraintSource;
  }

  @Override
  public void removeConstraint(String source) {
      this.lastRemoveConstraintSource = source;
  }
  ```

- [ ] **Run battery test to confirm failure:**
  ```
  ./gradlew :io.openems.edge.controller.evcs.batterypricing:test --tests "*.deactivate_removesConstraint"
  ```

- [ ] **Fix `ControllerEvcsBatteryPricingImpl.deactivate()`:**

  Replace (~line 77):
  ```java
  @Override
  @Deactivate
  protected void deactivate() {
      super.deactivate();
  }
  ```
  With:
  ```java
  @Override
  @Deactivate
  protected void deactivate() {
      this.evcsPricing.removeConstraint(this.id());
      super.deactivate();
  }
  ```

- [ ] **Add test to `ControllerEvcsPvPricingImplTest`** (same pattern):

  ```java
  /**
   * On deactivate(), removeConstraint must be called with the controller ID.
   */
  @Test
  public void deactivate_removesConstraint() throws Exception {
      var dummy = new DummyEvcsPricing();
      var sum = new DummySum().withProductionActivePower(1000);

      new ControllerTest(new ControllerEvcsPvPricingImpl()) //
              .addReference("evcsPricing", dummy) //
              .addReference("sum", sum) //
              .activate(baseConfig()) //
              .next(new TestCase()) //
              .deactivate();

      assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
  }
  ```

  Add `getLastRemoveConstraintSource()` and the recording `removeConstraint` to `DummyEvcsPricing` (pvpricing).

- [ ] **Fix `ControllerEvcsPvPricingImpl.deactivate()`** (same pattern as battery above).

- [ ] **Run all pricing controller tests:**
  ```
  ./gradlew :io.openems.edge.controller.evcs.batterypricing:test :io.openems.edge.controller.evcs.pvpricing:test
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Commit:**
  ```
  git add io.openems.edge.controller.evcs.batterypricing/ io.openems.edge.controller.evcs.pvpricing/
  git commit -m "fix(evcs-pricing): remove constraint on deactivate in BatteryPricing and PvPricing controllers"
  ```

---

## Task 6: Fix F4 — Extract shared `DummyEvcsPricing` test fixture

**Files:**
- Create: `io.openems.edge.evcs.pricing.api/test/io/openems/edge/evcs/pricing/DummyEvcsPricing.java`
- Modify: `io.openems.edge.controller.evcs.batterypricing/test/.../batterypricing/DummyEvcsPricing.java`
- Modify: `io.openems.edge.controller.evcs.fixedpricing/test/.../fixedpricing/DummyEvcsPricing.java`
- Modify: `io.openems.edge.controller.evcs.pvpricing/test/.../pvpricing/DummyEvcsPricing.java`
- Modify: `io.openems.edge.controller.evcs.batterypricing/bnd.bnd` (add test dependency)
- Modify: `io.openems.edge.controller.evcs.fixedpricing/bnd.bnd`
- Modify: `io.openems.edge.controller.evcs.pvpricing/bnd.bnd`

**Problem:** Three near-identical `DummyEvcsPricing` classes exist, each recording only the calls relevant to its own controller tests. They diverge silently (e.g., `reset()` present in pvpricing but not batterypricing).

**Approach:** Create a single comprehensive `DummyEvcsPricing` in the `io.openems.edge.evcs.pricing.api` test jar that records all calls. Each controller test's local `DummyEvcsPricing.java` becomes a thin subclass or is deleted and replaced by the shared one via bnd `-testpath`.

- [ ] **Create `io.openems.edge.evcs.pricing.api/test/io/openems/edge/evcs/pricing/DummyEvcsPricing.java`:**

```java
package io.openems.edge.evcs.pricing;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

/**
 * Shared test double for {@link EvcsPricing}. Records the most recent call to
 * each mutating method for assertion in unit tests.
 */
public class DummyEvcsPricing extends AbstractDummyOpenemsComponent<DummyEvcsPricing>
        implements EvcsPricing, OpenemsComponent {

    private String lastCeilingSource;
    private Double lastCeilingPrice;
    private String lastFloorSource;
    private Double lastFloorPrice;
    private String lastSetOverrideSource;
    private Double lastSetOverridePrice;
    private String lastRemoveOverrideSource;
    private String lastRemoveConstraintSource;

    public DummyEvcsPricing() {
        super(EvcsPricing.SINGLETON_COMPONENT_ID,
                OpenemsComponent.ChannelId.values(),
                EvcsPricing.ChannelId.values());
    }

    @Override
    protected DummyEvcsPricing self() {
        return this;
    }

    /** Resets all recorded state. Call between test cases if needed. */
    public void reset() {
        this.lastCeilingSource = null;
        this.lastCeilingPrice = null;
        this.lastFloorSource = null;
        this.lastFloorPrice = null;
        this.lastSetOverrideSource = null;
        this.lastSetOverridePrice = null;
        this.lastRemoveOverrideSource = null;
        this.lastRemoveConstraintSource = null;
    }

    public String getLastCeilingSource() { return this.lastCeilingSource; }
    public Double getLastCeilingPrice() { return this.lastCeilingPrice; }
    public String getLastFloorSource() { return this.lastFloorSource; }
    public Double getLastFloorPrice() { return this.lastFloorPrice; }
    public String getLastSetOverrideSource() { return this.lastSetOverrideSource; }
    public Double getLastSetOverridePrice() { return this.lastSetOverridePrice; }
    public String getLastRemoveOverrideSource() { return this.lastRemoveOverrideSource; }
    public String getLastRemoveConstraintSource() { return this.lastRemoveConstraintSource; }

    @Override
    public void addPriceCeiling(String source, double maxPrice) {
        this.lastCeilingSource = source;
        this.lastCeilingPrice = maxPrice;
    }

    @Override
    public void addPriceFloor(String source, double minPrice) {
        this.lastFloorSource = source;
        this.lastFloorPrice = minPrice;
    }

    @Override
    public void setOverride(String source, double price) {
        this.lastSetOverrideSource = source;
        this.lastSetOverridePrice = price;
    }

    @Override
    public void removeOverride(String source) {
        this.lastRemoveOverrideSource = source;
    }

    @Override
    public void removeConstraint(String source) {
        this.lastRemoveConstraintSource = source;
    }
}
```

- [ ] **Add `io.openems.edge.evcs.pricing.api` to the `-testpath`** of each controller's `bnd.bnd`. This repo uses bundle names without version qualifiers in `-testpath` (e.g., `io.openems.edge.common` without `;version=snapshot`). Open each controller's `bnd.bnd`, find the `-testpath` block, and add `io.openems.edge.evcs.pricing.api,\` to it:
  ```
  -testpath: \
      io.openems.edge.common,\
      io.openems.edge.evcs.pricing.api,\
      ...
  ```

- [ ] **Replace each local `DummyEvcsPricing.java`** with a file that simply imports from the shared location (or delete the local file and update imports). The simplest approach is to delete the local `DummyEvcsPricing.java` files and update all test imports to `io.openems.edge.evcs.pricing.DummyEvcsPricing`.

- [ ] **Update all test imports** in:
  - `ControllerEvcsBatteryPricingImplTest.java`
  - `ControllerEvcsFixedPricingImplTest.java`
  - `ControllerEvcsPvPricingImplTest.java`

- [ ] **Build all affected modules:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.api:build \
      :io.openems.edge.controller.evcs.batterypricing:build \
      :io.openems.edge.controller.evcs.fixedpricing:build \
      :io.openems.edge.controller.evcs.pvpricing:build \
      --console=plain --warn
  ```

- [ ] **Run all tests:**
  ```
  ./gradlew :io.openems.edge.evcs.pricing.api:test \
      :io.openems.edge.controller.evcs.batterypricing:test \
      :io.openems.edge.controller.evcs.fixedpricing:test \
      :io.openems.edge.controller.evcs.pvpricing:test
  ```
  Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Commit:**
  ```
  git add io.openems.edge.evcs.pricing.api/ \
      io.openems.edge.controller.evcs.batterypricing/ \
      io.openems.edge.controller.evcs.fixedpricing/ \
      io.openems.edge.controller.evcs.pvpricing/
  git commit -m "refactor(evcs-pricing): extract shared DummyEvcsPricing test fixture to api bundle"
  ```

---

## Task 7: Fix F5 — UI: extract shared constant and channel-read logic

**Files:**
- Create: `ui/src/app/edge/live/Controller/Evcs/evcs-pricing.constants.ts`
- Modify: `ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.ts`
- Modify: `ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.ts`

**Problem:** `flat.ts` and `modal.ts` each define `EVCS_PRICING_ID = "_evcsPricing"` as a private static and both repeat the `getChannelAddresses()` + `onCurrentData()` logic for reading `_evcsPricing/Price`.

- [ ] **Create `evcs-pricing.constants.ts`:**

```typescript
/** Singleton component ID of the EvcsPricing core component. */
export const EVCS_PRICING_COMPONENT_ID = "_evcsPricing";

/** Channel key for the locked EVCS price. */
export const EVCS_PRICE_CHANNEL = `${EVCS_PRICING_COMPONENT_ID}/Price`;
```

- [ ] **Update `flat.ts`** to use the constant:

  Remove:
  ```typescript
  private static readonly EVCS_PRICING_ID = "_evcsPricing";
  ```

  Replace `getChannelAddresses()`:
  ```typescript
  protected override getChannelAddresses(): ChannelAddress[] {
    return [new ChannelAddress(EVCS_PRICING_COMPONENT_ID, "Price")];
  }
  ```

  Replace `onCurrentData()`:
  ```typescript
  protected override onCurrentData(currentData: CurrentData) {
    this.currentPrice = currentData.allComponents[EVCS_PRICE_CHANNEL];
  }
  ```

  Add import at top:
  ```typescript
  import { EVCS_PRICE_CHANNEL, EVCS_PRICING_COMPONENT_ID } from "../../evcs-pricing.constants";
  ```

- [ ] **Update `modal.ts`** the same way (same constants, same import path).

- [ ] **Run UI lint** to confirm no TypeScript errors:
  ```
  cd ui && node_modules/.bin/ng lint
  ```
  Expected: no errors.

- [ ] **Commit:**
  ```
  git add ui/src/app/edge/live/Controller/Evcs/
  git commit -m "refactor(evcs-pricing): extract EVCS_PRICING_COMPONENT_ID constant to shared UI file"
  ```

---

## Task 8: Final Build and Lint

- [ ] **Run full Java build:**
  ```
  ./gradlew build --console=plain --warn
  ```
  Expected: BUILD SUCCESSFUL, zero failures.

- [ ] **Run checkstyle:**
  ```
  ./gradlew checkstyleAll --console=plain --warn
  ```
  Expected: no violations.

- [ ] **Run UI lint:**
  ```
  cd ui && node_modules/.bin/ng lint
  ```

- [ ] **Fix any violations found**, then commit:
  ```
  git add -A
  git commit -m "fix(evcs-pricing): checkstyle and lint cleanup"
  ```
