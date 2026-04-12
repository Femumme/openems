# Implementation Plan: ESS Controller PID Conflict Fix

Spec: docs/specs/ess-controller-pid-conflict.md

## Context

When Peakshaving and Balancing controllers target the same ESS, both call `ess.setActivePowerEqualsWithPid()` which mutates the shared `PidFilter` singleton in `EssPowerImpl` before constraint validation. The second controller's constraint is rejected by `addConstraintAndValidate`, but its PID call has already polluted `errorSum`/`lastInput`. Fix: snapshot PID state before mutation, restore on constraint rejection.

## Tasks

### T1 — Add snapshot/restore capability to PidFilter
- **Files**: `io.openems.edge.common/src/io/openems/edge/common/filter/PidFilter.java`, `io.openems.edge.common/test/io/openems/edge/common/filter/PidFilterTest.java`
- **Depends on**: none
- **Description**: Add a `PidSnapshot` record (inner type) capturing `errorSum`, `lastInput`, `firstRun`. Add `saveState()` returning a `PidSnapshot` and `restoreState(PidSnapshot)` applying it. These enable callers to roll back PID state when a downstream operation fails. `DisabledPidFilter` inherits these methods — its `applyPidFilter` never mutates meaningful state, so inherited save/restore is safe (no override needed).
- **Acceptance criteria**:
  - [ ] `PidFilter` has a public `record PidSnapshot(double errorSum, double lastInput, boolean firstRun)`
  - [ ] `saveState()` returns a `PidSnapshot` reflecting current internal state
  - [ ] `restoreState(PidSnapshot)` sets `errorSum`, `lastInput`, `firstRun` from the snapshot
  - [ ] Unit test: save → applyPidFilter with target A → restore → applyPidFilter with target A again → second output equals first output (state was fully rolled back)
  - [ ] Unit test: save → applyPidFilter with opposing target → restore → verify `errorSum` is back to pre-mutation value
  - [ ] Existing PidFilterTest tests pass unchanged (no regression)

### T2 — Guard PID mutation in setActivePowerEqualsWithPid
- **Files**: `io.openems.edge.ess.api/src/io/openems/edge/ess/api/ManagedSymmetricEss.java`
- **Depends on**: T1
- **Description**: In the static `setActivePowerEqualsWithPid(ManagedSymmetricEss, Integer, PidFilter)` method: save PID state before calling `applyPidFilter`, wrap `ess.setActivePowerEquals(pidOutput)` in try/catch, restore PID state on any `OpenemsNamedException` then re-throw. This ensures that when `addConstraintAndValidate` rejects a second EQUALS constraint, the PID filter's `errorSum`/`lastInput` are rolled back to reflect only the winning controller's setpoint. The `pidFilter.setLimits()` call is idempotent and does not need rollback.
- **Acceptance criteria**:
  - [ ] `pidFilter.saveState()` is called before `pidFilter.applyPidFilter()`
  - [ ] On exception from `ess.setActivePowerEquals()`, `pidFilter.restoreState()` is called before re-throwing
  - [ ] On success, PID state reflects the applied setpoint (no change from current behavior)
  - [ ] Both code paths (power's own PidFilter and fallback PidFilter) are protected — the guard applies to whichever `pidFilter` instance is selected
  - [ ] Single-controller scenarios are unaffected (no try/catch overhead beyond method calls)
  - [ ] All existing tests in `io.openems.edge.controller.ess.balancing`, `io.openems.edge.controller.symmetric.peakshaving`, and `io.openems.edge.controller.ess.timeofusetariff` pass unchanged

## Execution Order

Sequential: T1 → T2
