# Implementation Plan: PV Sell-to-Grid Limit: ESS Discharge Awareness

Spec: docs/specs/pv-sell-to-grid-limit-ess-awareness.md

## Tasks

### T1 — Update sell-to-grid limit calculation to include ESS discharge
- **Files**: `io.openems.edge.controller.pvinverter.selltogridlimit/src/io/openems/edge/controller/pvinverter/selltogridlimit/ControllerPvInverterSellToGridLimitImpl.java`, `io.openems.edge.controller.pvinverter.selltogridlimit/bnd.bnd`
- **Depends on**: none
- **Description**: Add `io.openems.edge.ess.api` to the `-buildpath` in `bnd.bnd` to expose the `SymmetricEss` API. Modify `run()` and `calculateRequiredPower()` to optionally retrieve a `SymmetricEss` component via `config.ess_id()`. If present and valid, read its `ActivePower` channel. Add `Math.max(0, essActivePower)` to the calculated power limit. Handle missing/empty `ess_id` gracefully to maintain backwards compatibility.
- **Acceptance criteria**:
  - [ ] `bnd.bnd` `-buildpath` contains `io.openems.edge.ess.api`.
  - [ ] If `ess_id` is configured and ESS is discharging, PV power limit calculation includes the ESS active power.
  - [ ] If `ess_id` is empty or ESS is charging, the limit calculation uses 0 for ESS contribution.
  - [ ] Smooth adjustment rate logic applies cleanly to the newly calculated power limit.

### T2 — Update test config and add ESS awareness test cases
- **Files**: `io.openems.edge.controller.pvinverter.selltogridlimit/test/io/openems/edge/controller/pvinverter/selltogridlimit/MyConfig.java`, `io.openems.edge.controller.pvinverter.selltogridlimit/test/io/openems/edge/controller/pvinverter/selltogridlimit/ControllerPvInverterSellToGridLimitImplTest.java`
- **Depends on**: T1
- **Description**: Add `essId` support to `MyConfig.java`. Add new test cases in `ControllerPvInverterSellToGridLimitImplTest.java` using a dummy ESS component. Simulate consumption spikes and verify the computed PV limit increases correctly by the ESS discharge amount and verify the adjustment rate applies smoothly.
- **Acceptance criteria**:
  - [ ] `MyConfig` builder supports setting and retrieving `essId`.
  - [ ] Tests verify PV limit calculation logic when ESS is discharging, charging, and idle.
  - [ ] Tests verify the maximum adjustment rate (20%) is respected when the limit fluctuates due to ESS discharge.

## Execution Order

Sequential: T1 → T2