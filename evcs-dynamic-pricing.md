# EVCS Dynamic Pricing System

## Overview

The EVCS dynamic pricing system determines the EV charging price by combining inputs from multiple independent controllers. Each controller focuses on a single concern (PV surplus, battery state, grid energy cost, ...) and submits **constraints** or **overrides** to a central pricing core. The core resolves the final price.

This separation keeps each controller simple and testable while allowing the system to grow — adding a new pricing factor means adding a new controller, not modifying existing ones.

## Architecture

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Fixed Pricing│  │ PV Controller│  │ Bat Controller│  │Grid Price Ctl│
│  (override)  │  │ (constraint) │  │ (constraint)  │  │ (constraint) │
└──────┬───────┘  └──────┬───────┘  └──────┬────────┘  └──────┬───────┘
       │                 │                  │                   │
       ▼                 ▼                  ▼                   ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │                     EvcsPricing Core                            │
  │                                                                 │
  │  Collects: overrides, ceilings, floors                          │
  │  Resolves: final price at each interval tick                    │
  │  Exposes:  PRICE channel (locked), NEXT_INTERVAL_PRICE channel  │
  └─────────────────────────────────────────────────────────────────┘
```

## Controller Input Types

Controllers interact with the core through two distinct mechanisms.

### 1. Override (immediate, unconditional)

An override sets the price **directly and immediately**, bypassing the interval-based constraint resolution. This is intended for manual or fixed pricing where the operator wants explicit control.

| Property       | Value                                  |
|----------------|----------------------------------------|
| Effect         | Sets price immediately                 |
| Interval-aware | No — takes effect on the current cycle |
| Use case       | Fixed tariff, manual operator override  |

When an override is active, constraint-based prices are ignored. The override value becomes the locked price until the override is removed or another override replaces it.

```java
evcsPricing.setOverride("FixedPricing", 0.49);  // price is now 0.49 €/kWh
evcsPricing.removeOverride("FixedPricing");      // back to constraint-based
```

### 2. Constraints (ceilings & floors)

A constraint expresses a **bound** on the price — either a maximum (ceiling) or a minimum (floor). Multiple controllers submit constraints independently. The core resolves them into a single price at each interval tick.

| Type    | Meaning                                    | Example                                     |
|---------|--------------------------------------------|----------------------------------------------|
| Ceiling | "Price should be **at most** this value"   | PV controller: ceiling 0.35 when surplus high |
| Floor   | "Price should be **at least** this value"  | Battery controller: floor 0.45 when SoC low   |

**Resolution formula:**

```
resolved = max(highest_floor, min(all_ceilings))
```

Floors always win over ceilings — cost protection takes priority over discounts. If the highest floor exceeds the lowest ceiling, the floor wins.

```java
evcsPricing.addPriceCeiling("PvPricing", 0.35);
evcsPricing.addPriceFloor("BatteryPricing", 0.45);
// resolved = max(0.45, 0.35) = 0.45
```

## Interval-Based Price Changes

Constraint-based prices do **not** change continuously. They are resolved and locked at a fixed interval.

### Schedule (Cron)

The interval is defined by a cron expression. The default is every full hour:

```
0 0 * * * *    # default: every full hour (HH:00)
```

Other examples:

```
0 */30 * * * *  # every 30 minutes
0 */15 * * * *  # every 15 minutes
0 0 */2 * * *   # every 2 hours
```

### Lifecycle of an Interval

```
 12:00                           12:45    13:00                          14:00
   │                               │        │                              │
   ├── price LOCKED at 0.35 €/kWh ─┤────────├── price LOCKED at 0.42 €/kWh─┤
   │                               │        │                              │
   │                         data collection │                              │
   │                         window (15 min) │                              │
   │                               ◄────────►                              │
   │                          12:45 – 13:00  │                              │
   │                                         │                              │
   │  controllers collect data & submit      │  constraints resolved →      │
   │  constraints throughout the interval    │  new price locked            │
```

1. At 12:00, the core locks the current resolved price. This price remains active for the entire interval.
2. Between 12:00 and 13:00, controllers continuously collect data (PV production, battery SoC, grid prices, ...).
3. At 13:00, the core resolves all active constraints into a new price and locks it.
4. The `PRICE` channel reflects the locked price. The `NEXT_INTERVAL_PRICE` channel shows the currently resolved (but not yet locked) price as a preview.

**Overrides bypass this mechanism entirely** — they take effect immediately regardless of the interval.

## Data Collection Window

Controllers should not base their constraints on instantaneous values. A cloud passing over PV panels causes a momentary drop in production that does not reflect actual conditions. Instead, controllers use a **data collection window** — a rolling average of recent measurements.

### Configuration

| Parameter                  | Default  | Description                                  |
|----------------------------|----------|----------------------------------------------|
| `dataCollectionWindowMinutes` | `15`  | Length of the rolling average window          |

This is configurable per controller. Typical values:

- **10 minutes** — more reactive, suitable for fast-changing inputs
- **15 minutes** — balanced default
- **20 minutes** — more stable, smooths out longer fluctuations

### How It Works

The controller samples the relevant channel value every cycle (~1 second) and maintains a time-windowed buffer. At each cycle, it calculates the average over the configured window and uses that average to determine its constraint.

```
Raw PV:   ████▁▁████████▁████████████  (cloud at min 3-4)
Average:  ━━━━━━━━━━━━━━━━━━━━━━━━━━━  (barely dips)
```

The rolling average acts as a low-pass filter — short fluctuations are absorbed while sustained changes are reflected.

## Example: PV Surplus Controller

The PV controller lowers the charging price when there is excess solar production, encouraging EV charging during PV surplus.

### Configuration

```
component.id       = "Controller.Evcs.PvPricing0"
evcsPricing.target = "(id=EvcsPricing.Core)"
surplusThreshold   = 500       # minimum surplus (W) to activate
pvFullSurplus      = 8000      # surplus (W) considered "maximum"
minPrice           = 0.25      # ceiling at full surplus
maxPrice           = 0.55      # ceiling at threshold
dataCollectionWindowMinutes = 15
```

### Behavior

The controller calculates the average grid feed-in (PV surplus) over the last 15 minutes.

```
surplus = avg(GridActivePower, last 15 min) * -1
```

A negative `GridActivePower` means energy is being fed into the grid — i.e., more is produced than consumed.

| Avg Surplus | Action                        | Constraint         |
|-------------|-------------------------------|--------------------|
| < 500 W     | Skip — no constraint added   | —                  |
| 500 W       | Set ceiling at maxPrice       | ceiling 0.55 €/kWh |
| 4000 W      | Interpolate between max/min   | ceiling ~0.41 €/kWh |
| ≥ 8000 W    | Set ceiling at minPrice       | ceiling 0.25 €/kWh |

The interpolation is linear between the threshold and the full surplus:

```
ratio   = (surplus - surplusThreshold) / (pvFullSurplus - surplusThreshold)
ceiling = maxPrice - ratio * (maxPrice - minPrice)
```

### Interaction with the Interval

```
12:00          12:45     13:00          13:45     14:00
  │              │         │              │         │
  │ price: 0.69  │ collect │ price: 0.35  │ collect │ price: 0.55
  │ (no surplus) │ window  │ (high PV)    │ window  │ (cloud cover)
  │              │         │              │         │
  │              │ avg PV  │              │ avg PV  │
  │              │ = 7 kW  │              │ = 1 kW  │
```

- At 12:00 no surplus → PV controller skips → price stays at the fixed pricing override or base.
- Between 12:45–13:00 the 15-min average shows 7 kW surplus → PV controller submits ceiling 0.35.
- At 13:00 the core locks 0.35 for the next hour.
- By 13:45–14:00 clouds reduce the average to 1 kW → PV controller submits ceiling 0.55.
- At 14:00 the core locks 0.55.

## Example: Battery Controller

The battery controller protects the battery from being drained by cheap EV charging and encourages charging when the battery is full.

### Configuration

```
component.id       = "Controller.Evcs.BatteryPricing0"
evcsPricing.target = "(id=EvcsPricing.Core)"
lowSocThreshold    = 30        # SoC (%) below which a floor is set
highSocThreshold   = 80        # SoC (%) above which a ceiling is set
lowSocFloorPrice   = 0.45      # floor when SoC is low
highSocCeilPrice   = 0.55      # ceiling at highSocThreshold
fullCeilPrice      = 0.35      # ceiling at 100% SoC
dataCollectionWindowMinutes = 15
```

### Behavior

The controller calculates the average battery state of charge over the data collection window.

| Avg SoC     | Action                          | Constraint         |
|-------------|----------------------------------|--------------------|
| < 30%       | Set floor — protect battery     | floor 0.45 €/kWh  |
| 30% – 80%   | Skip — no constraint added     | —                  |
| 80%          | Set ceiling at highSocCeilPrice | ceiling 0.55 €/kWh |
| 100%         | Set ceiling at fullCeilPrice    | ceiling 0.35 €/kWh |

Between 80% and 100%, the ceiling is interpolated linearly.

### Conflict Resolution with PV Controller

When both controllers are active, the constraint resolution handles conflicts naturally:

**Scenario: High PV + Low Battery**
```
PV Controller:      ceiling 0.35 €/kWh   (lots of surplus)
Battery Controller: floor   0.45 €/kWh   (SoC at 20%, protect battery)

Resolved: max(0.45, min(0.35)) = 0.45 €/kWh
```

The floor wins. Even though there is PV surplus, the battery needs protection. The price stays elevated to discourage excessive EV charging that would drain the battery further.

**Scenario: High PV + Full Battery**
```
PV Controller:      ceiling 0.30 €/kWh   (lots of surplus)
Battery Controller: ceiling 0.35 €/kWh   (SoC at 100%, encourage usage)

Resolved: max(no floors, min(0.30, 0.35)) = 0.30 €/kWh
```

Both controllers agree that the price should be low. The tighter ceiling (PV) wins.

**Scenario: No PV + Low Battery**
```
PV Controller:      — skip —
Battery Controller: floor 0.45 €/kWh     (SoC at 20%)

Resolved: max(0.45, min(0.69*)) = 0.45 €/kWh

* 0.69 comes from the FixedPricing override or base ceiling
```

No surplus, low battery — price stays elevated.

## Summary

| Aspect                | Detail                                                         |
|-----------------------|----------------------------------------------------------------|
| Input types           | **Overrides** (immediate) and **Constraints** (interval-based) |
| Constraint types      | Ceilings (upper bound) and Floors (lower bound)                |
| Resolution            | `max(highest_floor, min(all_ceilings))`                        |
| Interval schedule     | Cron expression, default `0 0 * * * *` (every hour)            |
| Data collection window| Configurable per controller (default 15 minutes)               |
| Override behavior     | Bypasses interval, takes effect immediately                    |
| Conflict resolution   | Floors always win over ceilings (cost protection first)        |
