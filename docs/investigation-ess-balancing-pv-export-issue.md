# ESS Balancing / PV Export Issue — Investigation Report

**Date:** 2026-04-19  
**Status:** Fix implemented — dynamic integrator limit in `PidFilter.java`, all tests green

---

## Symptom

PV production capped at ~505 kW. ESS does not fully absorb surplus; grid export stays near zero. Expected: ESS charges at up to 730 kW.

---

## Root Cause: PID Integrator Saturation (Mathematically Proven)

The 505 kW cap is **not** a config parameter. It is a deterministic mathematical property of the PID filter's integrator anti-windup design.

### The PID filter (`PidFilter.java`)

```
output = P*error + I*errorSum + D*(-deltaInput)     // line 102
errorSum clamped to ±(ERROR_SUM_LIMIT_FACTOR × |limit|)  // lines 180-204
```

With default gains `P=0.3, I=0.3, D=0.1` and `ERROR_SUM_LIMIT_FACTOR = 2` (hardcoded, line 15):

### Steady-state equilibrium proof

At steady state: D-term → 0, `output = input` (system at rest), errorSum saturated at `-(2 × 730000) = -1,460,000`:

```
input = P × (target - input) + I × errorSum_saturated
input = 0.3 × (-730000 - input) + 0.3 × (-1460000)
input = -219000 - 0.3×input - 438000
1.3×input = -657000
input = -505,384 W  ≈  -505 kW  ✓
```

The integrator saturates during ramp-up and **can never drain back** because the error remains non-zero at the false equilibrium. The system locks permanently.

### 10-cycle simulation confirms convergence to ~505 kW

| Cycle | input (W) | error | errorSum | output (W) |
|-------|-----------|-------|----------|------------|
| 1 | 0 | -730000 | 0 | -219000 |
| 2 | -219000 | -511000 | -730000 | -350400 |
| 3 | -350400 | -379600 | -1241000 | -473040 |
| 4 | -473040 | -256960 | -1460000† | -503724 |
| 5 | -503724 | -226276 | -1460000† | -502815 |
| 6+ | oscillates → converges | | -1460000† | **~-505 kW** |

†errorSum saturated at cycle 4; system trapped at false equilibrium from cycle 5.

---

## Why the Previous Hypothesis Was Wrong

The earlier investigation claimed `outputMaxSurplus = 505,000 W` was a PID filter config parameter. This is incorrect:

| Claim | Reality |
|---|---|
| `outputMaxSurplus` is a PID config property | Does not exist anywhere in the codebase (zero matches across all `.java`, `.cfg`, `.config`, `.json`) |
| `openems-backup` contains the config | Directory does not exist in the repo |
| Raising `outputMaxSurplus` to 730000 fixes it | No such config to change; the 505 kW emerges from PID math |
| PID `highLimit` is set from config | `highLimit` is set **dynamically each cycle** from `power.getMinPower()`/`getMaxPower()` (`ManagedSymmetricEss.java:424-429`) |

---

## Evidence Chain

| Source | Finding | Implication |
|---|---|---|
| `PidFilter.java:15` | `ERROR_SUM_LIMIT_FACTOR = 2` hardcoded | Integrator cap = 2×730000 = 1,460,000 |
| `PidFilter.java:102` | `output = P*error + I*errorSum + D*(-Δinput)` — **no input feedforward term** | Output is pure correction signal, not "input + delta" |
| `PidFilter.java:106` | `errorSum = applyErrorSumLimit(errorSum + error)` — **no conditional anti-windup** | Integrator accumulates even when output is clamped |
| `PidFilter.java:96` | D-term: `-d × (input - lastInput)` | Actively resists convergence (pushes opposite to movement direction) |
| `ManagedSymmetricEss.java:431` | `pidFilter.applyPidFilter(currentActivePower, target)` | `input` = measured ESS power, not target |
| `EssPowerImpl.java:69,253` | Single `PidFilter` instance (singleton) | Shared state across all ESS — not a factor here if only one ESS |
| `ControllerEssBalancingImpl.java:97-105` | `calculatedPower = gridPower + essPower - targetGridSetpoint` → `setActivePowerEqualsWithPid()` | No clamping in the controller itself |
| Live channels | `AllowedChargePower = 730 kW`, `MaxApparentPower = 730 kW` | Hardware is not the bottleneck |
| Log traces | "setpoint oscillates, converges to ~505 kW and stabilises" | Matches PID simulation exactly |

---

## Ruled-Out Causes

- **`outputMaxSurplus` config** — property does not exist in codebase
- **Hardware limits** — `AllowedChargePower` and `MaxApparentPower` both 730 kW
- **Other controllers** — no additional power constraints active (checked `LimitTotalDischarge`, `GridOptimizedCharge`, `TimeOfUseTariff`, `SellToGridLimit`)
- **`AllowedChargePower` ramp** — 5%/s ramp (`AbstractAllowedChargeDischargeHandler`) reaches 730 kW within seconds; observed cap is persistent
- **Battery BMS derating** — channels show full 730 kW available when battery has headroom; observed AllowedChargePower drop at high SOC is normal EssProtection voltage derating (PT1 filter in `AbstractAllowedChargeDischargeHandler`) — not the bug
- **Power solver constraints** — `ConstraintUtil.createGenericEssConstraints` only reads `AllowedChargePower`, `AllowedDischargePower`, `MaxApparentPower` — all at 730 kW

---

## Implemented Fix: Dynamic Integrator Limit

**File:** `io.openems.edge.common/src/io/openems/edge/common/filter/PidFilter.java`

The old hardcoded `ERROR_SUM_LIMIT_FACTOR = 2` capped `errorSum` at `2 × max(|limits|)`. At steady state the integrator must supply `target / I` to maintain output, requiring `errorSum = target / 0.3 = 3.33 × target`. The factor of 2 was too small — the integrator saturated and the PID locked at ~69% of target.

**Change:** Replace the fixed factor with a gain-aware dynamic limit:

```java
// Old: errorSumLimit = max(|limits|) × 2
// New: errorSumLimit = max(|limits|) / I × 1.5
```

This ensures the integrator can always accumulate enough to drive the output to the full target, regardless of gains. Falls back to `× 4` when `I ≈ 0`.

**Tests added:**
- `testConvergesToTargetAtLimit` — 730 kW scenario, verifies convergence within 1% over 100 cycles
- `testSmoothRampUp` — verifies PID still ramps smoothly (first cycle is a fraction of target)

**All 9 PidFilter tests pass.**

---

## Power Constraint Chain (Reference)

```
[Grid Meter] getActivePower()
       ↓
ControllerEssBalancingImpl.run()                  [ControllerEssBalancingImpl.java:97-105]
  calculatedPower = gridPower + essPower - targetGridSetpoint
       ↓
ManagedSymmetricEss.setActivePowerEqualsWithPid() [ManagedSymmetricEss.java:412-440]
  1. minPower = power.getMinPower(ess, ALL, ACTIVE)   → LP solve over constraints
  2. maxPower = power.getMaxPower(ess, ALL, ACTIVE)
  3. pidFilter.setLimits(minPower, maxPower)           → [-730000, +discharge]
  4. pidFilter.applyPidFilter(currentPower, target)    → FIXED: integrator limit now
       ↓                                                    gain-aware, converges to target
  5. ess.setActivePowerEquals(pidOutput)
       ↓
EssPowerImpl → Solver.solve()                     [Solver.java]
  LP solver with AllowedCharge/Discharge/MaxApparent constraints
       ↓
ess.applyPower(activePower, reactivePower)         → Hardware setpoint
```
