# EVCS Grid Price Ceiling Controller

## Overview

New EVCS pricing controller that sets a configured price ceiling when the average grid electricity price during the upcoming EvcsPricing interval falls below a configured threshold. Primary use case: when wholesale spot prices go negative (grid oversupply from PV), lower the EVCS charging price to attract EV charging instead of curtailing PV or paying to discharge into the grid.

## Unit Convention

All configuration properties, channels, and user-facing values in this controller use **cents/kWh** (ct/kWh).

Internal conversions at API boundaries:
- `TimeOfUseTariff` returns prices in Currency/MWh → divide by 10 to get ct/kWh.
- `EvcsPricing.addPriceCeiling()` expects EUR/kWh → divide ct/kWh by 100.

## User Stories

### Story 1 — Cheap ceiling on low grid prices

As an EVCS operator, I want the charging price to drop automatically when upcoming grid electricity prices are low or negative, so that EV drivers are incentivized to charge during oversupply periods instead of me curtailing PV or paying grid feed-in costs.

#### Acceptance Criteria

- Given a configured `priceThreshold` of 2.0 ct/kWh and a configured `ceilingPrice` of 5 ct/kWh, when the average grid price from now until the next EvcsPricing interval tick is below 2.0 ct/kWh, then the controller submits `ceilingPrice` (5 ct/kWh) as a price ceiling to `EvcsPricing`.
- Given the average grid price from now until the next EvcsPricing interval tick is at or above `priceThreshold`, then the controller does not submit any constraint and clears its `ACTIVE_CEILING` channel.
- Given the `TimeOfUseTariff` returns empty prices (no data available), then the controller does not submit any constraint and clears its channels.
- Given the `EvcsPricing` core's `NEXT_PRICE_CHANGE` channel provides the next interval tick timestamp, when the controller computes the lookahead window, then it averages all quarter-hourly prices from `now` (rounded to current quarter) up to (exclusive) that tick.
- Given the next interval tick is less than one quarter-hour away (< 15 min), then the controller extends the window to include at least the current quarter's price (minimum 1 value).

### Story 2 — Graceful deactivation

As an EVCS operator, I want the controller to cleanly remove its constraint when disabled or deactivated, so that it does not leave stale ceilings in the pricing core.

#### Acceptance Criteria

- Given the controller is active and has submitted a ceiling, when it is deactivated, then it calls `removeConstraint` with its component ID.
- Given the controller is active and has submitted a ceiling, when it is disabled via config, then it calls `removeConstraint` with its component ID.

### Story 3 — Unit conversion safety

As a developer, I want the controller to correctly convert between the different unit conventions at API boundaries, so that price comparisons and submissions are numerically correct.

#### Acceptance Criteria

- Given `TimeOfUseTariff` returns 50.0 Currency/MWh, when the controller converts for threshold comparison, then it uses 5.0 ct/kWh (÷ 10).
- Given the configured `ceilingPrice` is 5 ct/kWh, when the controller submits to `EvcsPricing.addPriceCeiling()`, then it passes 0.05 EUR/kWh (÷ 100).
- Given prices are rounded, then the ceiling value submitted to `EvcsPricing` is rounded to 4 decimal places (half-up), consistent with existing pricing controllers.

### Story 4 — Channel visibility

As a UI developer, I want the controller to expose its active ceiling and the computed average grid price via channels, so that operators can monitor its behavior.

#### Acceptance Criteria

- Given the controller submits a ceiling, then the `EvcsPricingController.ACTIVE_CEILING` channel reflects the submitted value in EUR/kWh (matching the `EvcsPricingController` channel contract).
- Given the controller does not submit a ceiling, then `ACTIVE_CEILING` is null.
- Given a controller-specific `AVERAGE_GRID_PRICE` channel, then it reflects the most recently computed average of the lookahead window in ct/kWh.
- Given no prices are available, then `AVERAGE_GRID_PRICE` is null.

## Affected Modules

| Module | Path | Impact |
|---|---|---|
| **New bundle** | `io.openems.edge.controller.evcs.gridpricing/` | New OSGi bundle — the controller |
| EvcsPricing API | `io.openems.edge.evcs.pricing.api/` | Read-only dependency (no changes) |
| TimeOfUseTariff API | `io.openems.edge.timeofusetariff.api/` | Read-only dependency (no changes) |
| Edge application | `io.openems.edge.application/EdgeApp.bndrun` | Must include new bundle in resolution |

## Dependencies

- External: none
- Internal:
  - `io.openems.edge.timeofusetariff.api` — `TimeOfUseTariff.getPrices()` for price forecast
  - `io.openems.edge.evcs.pricing.api` — `EvcsPricing` for constraint submission, `EvcsPricingController` nature, `NEXT_PRICE_CHANGE` channel for interval timing
  - `io.openems.edge.common` — `AbstractOpenemsComponent`, `QuarterlyValues.getBetween()`
  - `io.openems.edge.controller.api` — `Controller` interface

## Configuration Properties

| Property | Type | Default | Unit | Description |
|---|---|---|---|---|
| `id` | `String` | `"ctrlEvcsGridPricing0"` | — | Component ID |
| `alias` | `String` | `""` | — | Human-readable name |
| `enabled` | `boolean` | `true` | — | Enable/disable toggle |
| `priceThreshold` | `double` | `0.0` | ct/kWh | Average grid price below which the ceiling is activated. Default 0 targets negative prices. |
| `ceilingPrice` | `double` | `5.0` | ct/kWh | Ceiling submitted to EvcsPricing when threshold condition is met |

## Algorithm

1. Read `NEXT_PRICE_CHANGE` from `EvcsPricing` to determine the next interval tick.
2. Call `timeOfUseTariff.getPrices()` to get the current price forecast.
3. Compute the average of all quarter-hourly prices between `now` (rounded to quarter) and the next interval tick using `prices.getBetween(now, nextTick)`. Convert from Currency/MWh to ct/kWh (÷ 10).
4. If the window contains no prices, skip (no constraint).
5. If the average (ct/kWh) is below `priceThreshold` (ct/kWh), submit `ceilingPrice` to `EvcsPricing` after converting ct/kWh → EUR/kWh (÷ 100).
6. Otherwise, do not submit any constraint.
7. Update channels accordingly.

## Constraints

- Follows the established EVCS pricing controller pattern: OSGi `@Component(factory=true)`, `@Reference` to `EvcsPricing` singleton, implements `Controller + EvcsPricingController`.
- No rolling average / data collection window needed — this controller reads forecast data, not real-time sensor data.
- `TimeOfUseTariff` is an optional `@Reference` — if no tariff provider is configured, the controller remains inactive.
- Must use JUnit 4 for tests. Must follow existing test patterns (`ControllerTest`, `DummyEvcsPricing`, `MyConfig` builder).

## Out of Scope

- Interpolated ceilings based on how far below threshold the price is (future enhancement — this spec is binary: below threshold = ceiling, above = no constraint).
- Controlling charge power (start/stop charging). This controller only sets a billing price constraint.
- Fetching prices from any external API — relies on an already-configured `TimeOfUseTariff` provider.
- Changes to `EvcsPricing` core, `TimeOfUseTariff` API, or any existing controller.
- UI components for configuring or visualizing this controller.
