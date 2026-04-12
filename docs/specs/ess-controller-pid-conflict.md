# ESS Controller PID Conflict Fix

## Overview

When Peakshaving and Balancing controllers target the same ESS simultaneously, ESS power oscillates rapidly. Root cause: both controllers share a **single PID filter instance** owned by the `EssPowerImpl` singleton. The first controller (by scheduler order) wins the EQUALS constraint; the second controller's constraint is rejected — but its `applyPidFilter()` call has already mutated the shared integrator state. The PID accumulates error from two opposing setpoints every cycle, causing overshoot, integrator windup, and rapid power swings.

## User Stories

### Story 1 — Eliminate PID integrator pollution from rejected controllers

As an energy system operator, I want ESS power to remain stable when Peakshaving and Balancing are both active on the same ESS, so that the battery does not oscillate between charge and discharge.

#### Acceptance Criteria

- Given Peakshaving and Balancing controllers both target the same ESS, when both run in the same cycle, then only the winning controller's setpoint affects the PID filter state.
- Given a controller's EQUALS constraint is rejected by the power solver, when the cycle completes, then the PID filter's `errorSum` and `lastInput` reflect only successfully applied setpoints — not the rejected controller's target.
- Given the scheduler runs Peakshaving before Balancing and Peakshaving's constraint is accepted, when Balancing's constraint is rejected, then the PID integrator does not accumulate error toward the Balancing setpoint.
- Given controllers alternate winning across cycles (e.g., due to changing grid conditions), when the winning controller switches, then the PID filter resets or transitions smoothly without carrying stale integrator state from the previous winner.
- Given only one ESS controller is active (Peakshaving or Balancing, not both), when it runs each cycle, then PID behavior is unchanged from current behavior.

### Story 2 — Stable ESS power output under concurrent controllers

As an energy system operator, I want the ESS active power to converge to a stable setpoint within a bounded number of cycles, so that the battery hardware is not stressed by rapid power reversals.

#### Acceptance Criteria

- Given Peakshaving targets +3000 W discharge and Balancing targets −500 W charge on the same ESS, when both controllers run for 30 consecutive cycles, then the ESS active power does not oscillate by more than 10% of the winning controller's setpoint after initial convergence (≤5 cycles).
- Given the power solver rejects a controller's constraint, when the `NOT_SOLVED` channel is checked, then it remains in OK state (the system is solved with the winning controller's constraint).
- Given debug mode is enabled on `Ess.Power`, when a constraint rejection occurs, then the log clearly identifies which controller's constraint was rejected and why.

## Dependencies

- External: None.
- Behavioral: The ESS Power solver's constraint-accumulation and validation mechanism (`addConstraintAndValidate`) must continue to reject infeasible EQUALS+EQUALS combinations. The fix targets only the PID filter lifecycle — not the solver itself.

## Constraints

- The fix must not change the power solver's constraint resolution algorithm or its LP/Simplex behavior.
- The fix must not alter the scheduler's controller execution order semantics (first-caller-wins remains valid).
- The PID filter's smoothing behavior for single-controller scenarios must remain identical.
- The fix must be backward-compatible with existing OSGi configurations — no mandatory config migration.
- `PidFilter` default parameters (P=0.3, I=0.3, D=0.1) must remain unchanged.

## Out of Scope

- Merging or composing EQUALS constraints from multiple controllers (e.g., weighted averaging). The architectural decision that EQUALS constraints are mutually exclusive is preserved.
- Adding explicit controller priority to the power solver.
- Changing the scheduler to prevent concurrent Peakshaving + Balancing configurations.
- Refactoring the power solver's constraint types or LP objective function.
- Per-phase (asymmetric) PID filter isolation.
