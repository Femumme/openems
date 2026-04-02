# Implementation Plan: EVCS Grid Price Ceiling Controller

Spec: docs/specs/evcs-grid-price-ceiling-controller.md

## Context

New OSGi bundle `io.openems.edge.controller.evcs.gridpricing` — a pricing controller that submits a price ceiling to `EvcsPricing` when the average grid electricity price (from `TimeOfUseTariff`) falls below a configured threshold. Follows the established EVCS pricing controller pattern (batterypricing, pvpricing, fixedpricing).

**Key differences from existing controllers:**
- Depends on `TimeOfUseTariff` (optional `@Reference`) instead of `Sum`
- Uses `QuarterlyValues.getBetween()` for lookahead window averaging
- Requires unit conversion: Currency/MWh → ct/kWh (÷10) for comparison, ct/kWh → EUR/kWh (÷100) for submission
- Custom `AVERAGE_GRID_PRICE` channel (ct/kWh)

## Tasks

### T1 — Bundle scaffold: bnd.bnd

- **Files**: `openems/io.openems.edge.controller.evcs.gridpricing/bnd.bnd`
- **Depends on**: none
- **Description**: Create `bnd.bnd` following the batterypricing/pvpricing pattern. Must include `io.openems.edge.timeofusetariff.api` in both `-buildpath` and `-testpath` (unlike other EVCS pricing controllers that only need `evcs.pricing.api`).
- **Acceptance criteria**:
  - [ ] File exists at `openems/io.openems.edge.controller.evcs.gridpricing/bnd.bnd`
  - [ ] `-buildpath` includes: `${buildpath}`, `io.openems.common`, `io.openems.edge.common`, `io.openems.edge.controller.api`, `io.openems.edge.evcs.pricing.api`, `io.openems.edge.timeofusetariff.api`
  - [ ] `-testpath` includes: `${testpath}`, `io.openems.edge.controller.api`, `io.openems.edge.common`, `io.openems.edge.evcs.pricing.api`, `io.openems.edge.timeofusetariff.api`
  - [ ] Bundle-Name: `OpenEMS Edge Controller EVCS Grid Pricing`
  - [ ] Bundle-Version: `1.0.0.${tstamp}`

### T2 — Config annotation + Controller interface

- **Files**:
  - `openems/io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/Config.java`
  - `openems/io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricing.java`
- **Depends on**: none
- **Description**: Create the OSGi `@ObjectClassDefinition` config and the controller nature interface. Config properties: `id` (default `"ctrlEvcsGridPricing0"`), `alias`, `enabled`, `priceThreshold` (double, default 0.0, ct/kWh), `ceilingPrice` (double, default 5.0, ct/kWh). The interface extends `Controller`, `OpenemsComponent`, `EvcsPricingController` and defines a custom `AVERAGE_GRID_PRICE` ChannelId (Double, unit ct/kWh — use `Unit.NONE` since there's no ct/kWh unit constant, or use the closest available).
- **Acceptance criteria**:
  - [ ] `Config.java` follows exact pattern from batterypricing/fixedpricing Config (package-private `@interface Config`)
  - [ ] `id()` default is `"ctrlEvcsGridPricing0"`
  - [ ] `priceThreshold()` returns `double`, default `0.0`
  - [ ] `ceilingPrice()` returns `double`, default `5.0`
  - [ ] `webconsole_configurationFactory_nameHint` follows pattern: `"Controller Evcs Grid Pricing [{id}]"`
  - [ ] `ControllerEvcsGridPricing` interface has `ChannelId.AVERAGE_GRID_PRICE` (Doc.of(OpenemsType.DOUBLE))
  - [ ] Interface extends `Controller`, `OpenemsComponent`, `EvcsPricingController`
  - [ ] Interface provides default getter `getAverageGridPrice()` and setter `_setAverageGridPrice(Double)`

### T3 — Controller implementation

- **Files**: `openems/io.openems.edge.controller.evcs.gridpricing/src/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricingImpl.java`
- **Depends on**: T1, T2
- **Description**: Create the `@Component(factory=true)` implementation. Key design:
  - `@Reference` to `EvcsPricing` (mandatory, target by SINGLETON_COMPONENT_ID)
  - `@Reference` to `TimeOfUseTariff` (optional: `cardinality = ReferenceCardinality.OPTIONAL`, `policy = ReferencePolicy.DYNAMIC`) — stored via volatile field
  - Constructor passes all 4 ChannelId arrays (OpenemsComponent, Controller, ControllerEvcsGridPricing, EvcsPricingController)
  - `modified()`: calls `super.modified(...)` and `applyConfig()`, then checks `!config.enabled()` — if disabled, calls `evcsPricing.removeConstraint(this.id())` to clear any stale ceiling (spec Story 2, AC 2). Note: `super.modified()` does NOT remove constraints itself.
  - `run()` algorithm per spec:
    1. Read `NEXT_PRICE_CHANGE` from `evcsPricing.getNextPriceChange()`
    2. If `TimeOfUseTariff` is null → clearChannels, return
    3. Call `timeOfUseTariff.getPrices()` → if empty → clearChannels, return
    4. Compute `now` rounded to quarter, `nextTick` from channel. If nextTick is null or <15min away → use at least current quarter (ensure minimum 1 value)
    5. `prices.getBetween(now, nextTick)` → stream of Doubles in Currency/MWh
    6. Average the stream, convert ÷10 to ct/kWh
    7. If average < `config.priceThreshold()` → submit `config.ceilingPrice() / 100.0` (EUR/kWh) via `addPriceCeiling`, set channels
    8. Else → clearChannels
  - `deactivate()` calls `removeConstraint(this.id())`
  - `roundPrice()` static helper: BigDecimal, scale 4, HALF_UP (matches existing pattern)
  - `_setAverageGridPrice()` updated each cycle with the computed average in ct/kWh
- **Acceptance criteria**:
  - [ ] `@Designate(ocd = Config.class, factory = true)` and `@Component(name = "Controller.Evcs.GridPricing", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)`
  - [ ] `TimeOfUseTariff` reference is optional+dynamic (volatile field, bind/unbind methods)
  - [ ] Conversion: TimeOfUseTariff Currency/MWh ÷ 10 = ct/kWh for threshold comparison
  - [ ] Conversion: ceilingPrice ct/kWh ÷ 100 = EUR/kWh for `addPriceCeiling()`
  - [ ] Price rounded to 4 decimal places (HALF_UP) before submission
  - [ ] When average < threshold: `ACTIVE_CEILING` set to submitted EUR/kWh value, `AVERAGE_GRID_PRICE` set to ct/kWh average
  - [ ] When average ≥ threshold: `ACTIVE_CEILING` = null, `AVERAGE_GRID_PRICE` still updated
  - [ ] When no prices available: both channels null
  - [ ] `modified()` checks `!config.enabled()` and calls `evcsPricing.removeConstraint(this.id())` when disabled via config
  - [ ] `deactivate()` calls `evcsPricing.removeConstraint(this.id())`
  - [ ] Minimum 1 quarter in lookahead window (when nextTick < 15min away)

### T4 — Test config builder (MyConfig)

- **Files**: `openems/io.openems.edge.controller.evcs.gridpricing/test/io/openems/edge/controller/evcs/gridpricing/MyConfig.java`
- **Depends on**: T2
- **Description**: Create `MyConfig extends AbstractComponentConfig implements Config` with Builder pattern. Follows exact pattern from batterypricing/pvpricing MyConfig. Builder fields: `id`, `alias`, `enabled`, `priceThreshold`, `ceilingPrice`.
- **Acceptance criteria**:
  - [ ] Extends `AbstractComponentConfig`, implements `Config`
  - [ ] Builder has `setId`, `setAlias`, `setEnabled`, `setPriceThreshold`, `setCeilingPrice` methods
  - [ ] Constructor calls `super(Config.class, builder.id)`
  - [ ] All Config methods delegated to builder fields
  - [ ] Compiles against Config interface from T2

### T5 — Unit tests

- **Files**: `openems/io.openems.edge.controller.evcs.gridpricing/test/io/openems/edge/controller/evcs/gridpricing/ControllerEvcsGridPricingImplTest.java`
- **Depends on**: T3, T4
- **Description**: JUnit 4 tests using `ControllerTest`, `DummyEvcsPricing`, `DummyTimeOfUseTariffProvider`. Tests must cover all spec acceptance criteria. Use `Clock.fixed()` to control time. Set `NEXT_PRICE_CHANGE` on DummyEvcsPricing via `_setNextPriceChange()`. Provide prices via `DummyTimeOfUseTariffProvider.fromQuarterlyPrices()`.
  
  Required test cases:
  1. **belowThreshold_setsCeiling**: avg grid price below threshold → `addPriceCeiling` called with correct EUR/kWh value, `ACTIVE_CEILING` channel set
  2. **aboveThreshold_noCeiling**: avg grid price ≥ threshold → no constraint, `ACTIVE_CEILING` null
  3. **emptyPrices_noCeiling**: no tariff data → no constraint, channels null
  4. **noTariffProvider_noCeiling**: TimeOfUseTariff ref is null → no constraint
  5. **unitConversion_currencyMwhToCtKwh**: 50.0 Currency/MWh → 5.0 ct/kWh comparison
  6. **unitConversion_ceilingSubmission**: 5 ct/kWh config → 0.05 EUR/kWh submitted
  7. **deactivate_removesConstraint**: removeConstraint called with controller ID on deactivate
  8. **disabledViaConfig_removesConstraint**: when `enabled` config changes to false via `modified()`, removeConstraint called with controller ID (spec Story 2, AC 2)
  9. **averageGridPriceChannel_reflects**: `AVERAGE_GRID_PRICE` channel reflects computed average in ct/kWh
  10. **negativeGridPrice_setsCeiling**: negative prices (e.g., -20.0 Currency/MWh) trigger ceiling
- **Acceptance criteria**:
  - [ ] All 10 test cases pass
  - [ ] Uses JUnit 4 (`@Test` from `org.junit.Test`)
  - [ ] Uses `ControllerTest` harness with `.addReference()` for EvcsPricing and TimeOfUseTariff
  - [ ] Unit conversions verified numerically (e.g., 50.0 Currency/MWh → 5.0 ct/kWh, 5.0 ct/kWh → 0.05 EUR/kWh)
  - [ ] `DummyEvcsPricing.getLastCeilingPrice()` asserted for submitted values
  - [ ] `DummyEvcsPricing.getLastRemoveConstraintSource()` asserted for both deactivation and disabled-via-config
  - [ ] `AVERAGE_GRID_PRICE` channel output verified

### T6 — EdgeApp.bndrun registration

- **Files**: `openems/io.openems.edge.application/EdgeApp.bndrun`
- **Depends on**: T1
- **Description**: Add `io.openems.edge.controller.evcs.gridpricing` to both `-runrequires` (in the EVCS pricing controller block, after batterypricing) and `-runbundles` (at end, following existing pattern). After editing, run `./gradlew resolve` from `openems/` to validate resolution.
- **Acceptance criteria**:
  - [ ] `-runrequires` contains `bnd.identity;id='io.openems.edge.controller.evcs.gridpricing'` (near line 211, after batterypricing)
  - [ ] `-runbundles` contains `io.openems.edge.controller.evcs.gridpricing;version=snapshot` (at end, after batterypricing)
  - [ ] `./gradlew resolve` completes without error (run from `openems/`)

## Execution Order

```
[T1, T2] → [T3, T4] → T5 → T6
```

- **T1 + T2** (parallel): scaffold + config/interface have no deps
- **T3 + T4** (parallel): impl needs T1+T2; MyConfig needs T2
- **T5** (sequential): tests need impl (T3) + MyConfig (T4)
- **T6** (sequential): bndrun needs bundle name (T1), best done last so resolve can verify
