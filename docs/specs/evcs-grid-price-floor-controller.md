# EVCS Grid Price Floor Controller

## Overview

New EVCS pricing controller that sets the forecasted grid electricity price as a price floor in `EvcsPricing`, ensuring the operator never sells EV charging below grid cost. An optional configurable margin covers taxes, wear, and distribution costs on top of the raw grid price.

## Unit Convention

All configuration properties, channels, and user-facing values use **cents/kWh** (ct/kWh).

Internal conversions at API boundaries:
- `TimeOfUseTariff` returns prices in Currency/MWh → divide by 10 to get ct/kWh.
- `EvcsPricing.addPriceFloor()` expects EUR/kWh → divide ct/kWh by 100.

## User Stories

### Story 1 — Grid cost as price floor

As an EVCS operator, I want the minimum charging price to automatically track the grid electricity cost, so that I never sell energy to EV drivers at a loss.

#### Acceptance Criteria

- Given a configured `margin` of 2.0 ct/kWh, when the average `TimeOfUseTariff` price from now until the next EvcsPricing interval tick is 8.0 ct/kWh, then the controller submits a floor of 10.0 ct/kWh (grid price + margin) to `EvcsPricing`.
- Given the average grid price is -3.0 ct/kWh and `margin` is 2.0 ct/kWh, then the controller submits a floor of -1.0 ct/kWh. The `EvcsPricing` core's `absoluteMinPrice` clamp handles any further bounding.
- Given the `TimeOfUseTariff` returns empty prices (no data available), then the controller does not submit any constraint and clears its channels.
- Given the `EvcsPricing` core's `NEXT_PRICE_CHANGE` channel provides the next interval tick timestamp, when the controller computes the lookahead window, then it averages all quarter-hourly prices from `now` (rounded to current quarter) up to (exclusive) that tick.
- Given the next interval tick is less than one quarter-hour away (< 15 min), then the controller extends the window to include at least the current quarter's price (minimum 1 value).

### Story 2 — Graceful deactivation

As an EVCS operator, I want the controller to cleanly remove its constraint when disabled or deactivated, so that it does not leave stale floors in the pricing core.

#### Acceptance Criteria

- Given the controller is active and has submitted a floor, when it is deactivated, then it calls `removeConstraint` with its component ID.
- Given the controller is active and has submitted a floor, when it is disabled via config, then it calls `removeConstraint` with its component ID.

### Story 3 — Unit conversion safety

As a developer, I want the controller to correctly convert between the different unit conventions at API boundaries, so that floor values are numerically correct.

#### Acceptance Criteria

- Given `TimeOfUseTariff` returns 80.0 Currency/MWh, when the controller converts for floor computation, then it uses 8.0 ct/kWh (÷ 10).
- Given the computed floor is 10.0 ct/kWh, when the controller submits to `EvcsPricing.addPriceFloor()`, then it passes 0.10 EUR/kWh (÷ 100).
- Given prices are rounded, then the floor value submitted to `EvcsPricing` is rounded to 4 decimal places (half-up), consistent with existing pricing controllers.

### Story 4 — Channel visibility

As a UI developer, I want the controller to expose its active floor and the computed average grid price via channels, so that operators can monitor its behavior.

#### Acceptance Criteria

- Given the controller submits a floor, then the `EvcsPricingController.ACTIVE_FLOOR` channel reflects the submitted value in EUR/kWh (matching the `EvcsPricingController` channel contract).
- Given the controller does not submit a floor (no price data), then `ACTIVE_FLOOR` is null.
- Given a controller-specific `AVERAGE_GRID_PRICE` channel, then it reflects the most recently computed average of the lookahead window in ct/kWh.
- Given no prices are available, then `AVERAGE_GRID_PRICE` is null.

## Affected Modules

| Module | Path | Impact |
|---|---|---|
| **New bundle** | `io.openems.edge.controller.evcs.gridpricefloor/` | New OSGi bundle — the controller |
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
| `id` | `String` | `"ctrlEvcsGridPriceFloor0"` | — | Component ID |
| `alias` | `String` | `""` | — | Human-readable name |
| `enabled` | `boolean` | `true` | — | Enable/disable toggle |
| `margin` | `double` | `0.0` | ct/kWh | Added on top of the grid price to cover taxes, wear, distribution. |

## Algorithm

1. Read `NEXT_PRICE_CHANGE` from `EvcsPricing` to determine the next interval tick.
2. Call `timeOfUseTariff.getPrices()` to get the current price forecast.
3. Compute the average of all quarter-hourly prices between `now` (rounded to quarter) and the next interval tick using `prices.getBetween(now, nextTick)`. Convert from Currency/MWh to ct/kWh (÷ 10).
4. If the window contains no prices, skip (no constraint).
5. Compute floor = average (ct/kWh) + `margin` (ct/kWh).
6. Submit floor to `EvcsPricing` after converting ct/kWh → EUR/kWh (÷ 100).
7. Update channels accordingly.

## Constraints

- Follows the established EVCS pricing controller pattern: OSGi `@Component(factory=true)`, `@Reference` to `EvcsPricing` singleton, implements `Controller + EvcsPricingController`.
- No rolling average / data collection window needed — this controller reads forecast data, not real-time sensor data.
- `TimeOfUseTariff` is an optional `@Reference` — if no tariff provider is configured, the controller remains inactive.
- The controller always submits a floor when price data is available. There is no threshold — the grid price is always relevant as a cost basis.
- Must use JUnit 4 for tests. Must follow existing test patterns (`ControllerTest`, `DummyEvcsPricing`, `MyConfig` builder).

## Interaction with Grid Price Ceiling Controller

These two controllers are designed to work together:

| Scenario | Grid Price Floor | Grid Price Ceiling | Resolved by EvcsPricing Core |
|---|---|---|---|
| Normal prices (8 ct/kWh grid) | floor 10 ct/kWh (8 + 2 margin) | no ceiling (above threshold) | floor wins → 10 ct/kWh minimum |
| Cheap prices (-2 ct/kWh grid) | floor 0 ct/kWh (-2 + 2 margin) | ceiling 5 ct/kWh (below threshold) | ceiling 5 ct/kWh (above floor) |
| Very negative (-10 ct/kWh grid) | floor -8 ct/kWh (-10 + 2 margin) | ceiling 5 ct/kWh (below threshold) | ceiling 5 ct/kWh (above floor) |

The floor protects against selling below cost; the ceiling incentivizes charging during oversupply. When both are active, the core's `max(highest_floor, min(all_ceilings))` formula naturally resolves them.

## Out of Scope

- Different margin for different times of day or price ranges.
- Using the maximum price in the interval instead of the average (conservative floor).
- Controlling charge power (start/stop charging). This controller only sets a billing price constraint.
- Fetching prices from any external API — relies on an already-configured `TimeOfUseTariff` provider.
- Changes to `EvcsPricing` core, `TimeOfUseTariff` API, or any existing controller.
- UI components for configuring or visualizing this controller.
