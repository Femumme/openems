# PV Sell-to-Grid Limit: ESS Discharge Awareness

## Overview

The PV inverter sell-to-grid limit controller ignores ESS discharge when computing the PV power limit. When consumption spikes (e.g., EV charging), the ESS balancing controller discharges the battery to cover demand, masking the spike from the sell-to-grid controller. PV stays curtailed while the battery drains unnecessarily. The controller must factor ESS discharge into its limit calculation to prefer PV production over battery discharge.

## User Stories

### Story 1 — Raise PV limit when ESS discharges due to consumption spike

As an energy system operator, I want the PV sell-to-grid limit to increase when the ESS is discharging to cover consumption, so that PV production replaces battery discharge and the battery is preserved.

#### Acceptance Criteria

- Given the sell-to-grid limit controller is configured with an `ess_id`, when the referenced ESS is actively discharging (positive ActivePower), then the PV power limit is raised by the ESS discharge amount.
- Given the sell-to-grid limit controller is configured with an `ess_id`, when the ESS is charging (negative ActivePower) or idle, then the PV power limit calculation is unchanged (ESS contribution is zero).
- Given the sell-to-grid limit controller is configured with an `ess_id`, when the ESS discharges and the PV limit rises, then the total grid export still does not exceed `maximumSellToGridPower`.
- Given the sell-to-grid limit controller has `ess_id` empty or not configured, when any consumption spike occurs, then behavior is identical to the current implementation (backwards compatible).

### Story 2 — Smooth transition respects adjustment rate

As an energy system operator, I want the PV limit adjustment to remain smooth even when ESS discharge is factored in, so that the PV inverter is not subjected to sudden large setpoint changes.

#### Acceptance Criteria

- Given the ESS-aware PV limit is calculated, when the new limit differs from the last set limit by more than `DEFAULT_MAX_ADJUSTMENT_RATE` (20%), then the limit is ramped at the existing 20% rate.
- Given the ESS stops discharging (consumption spike ends), when the next cycle runs, then the PV limit ramps down smoothly to the non-ESS-aware value.

## Dependencies

- Internal: `io.openems.edge.ess.api` — `SymmetricEss.getActivePower()` channel for reading ESS discharge power

## Constraints

- The `ess_id` config field already exists in `Config.java` (line 33) but is unused — reuse it.
- ESS ActivePower sign convention: positive = discharge, negative = charge.
- Grid meter ActivePower sign convention: positive = buy-from-grid, negative = sell-to-grid.
- PV inverter ActivePower is always positive (production).
- Must not break existing installations that have `ess_id` unset/empty.

## Current Behavior (for reference)

**Sell-to-grid limit formula:**
```
pvLimit = gridPower + pvActivePower + maximumSellToGridPower
```

**Problem sequence:**
1. PV curtailed to respect sell-to-grid limit → grid export ≈ `maximumSellToGridPower`
2. Consumption spikes (e.g., EV) → grid swings toward import
3. ESS balancing discharges battery → grid returns to ~0
4. Next cycle: sell-to-grid controller sees grid ≈ 0 → computes low PV limit
5. PV stays curtailed; battery drains to cover consumption that PV could supply

**Expected formula with ESS awareness:**
```
pvLimit = gridPower + pvActivePower + maximumSellToGridPower + max(0, essActivePower)
```

The `max(0, essActivePower)` term adds ESS discharge power to the PV headroom, allowing PV to ramp up and replace battery output.

## Out of Scope

- Changes to the ESS balancing controller.
- Changes to the PV inverter API or driver implementations.
- Asymmetric ESS awareness (per-phase ESS discharge).
- Modification of the 20% max adjustment rate constant.
- UI changes.
