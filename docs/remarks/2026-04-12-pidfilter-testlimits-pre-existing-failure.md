# Remark: PidFilterTest.testLimits pre-existing test failure

- **Discovered by**: tech-reviewer during build/test check
- **Date**: 2026-04-12
- **Story**: `docs/specs/ess-controller-pid-conflict.md`
- **Severity**: High

## Problem

`PidFilterTest.testLimits` fails at line 84 (`this.t(p, 6981, 10000, 10000)`) — the PID filter returns a value other than the expected clamped `10000`. The test expects the output to be clamped to the upper limit after sufficient error accumulation, but the filter produces a different value. This failure reproduces on the original code (before any story changes) and is not caused by the current story.

## Discovery Context

Running `./gradlew io.openems.edge.common:test` as part of the ESS Controller PID Conflict tech review. The failure was confirmed pre-existing by stashing the story's changes and re-running the test on the original `main` branch code. Root cause is likely a mismatch between the test's expected values (possibly generated from an older Excel model) and the current `applyPidFilter` + `applyErrorSumLimit` behavior — the `ERROR_SUM_LIMIT_FACTOR` was changed from 10 to 2 in commit `269af0ed0` but the test expectations may not have been updated.

## Suggested Action

Regenerate expected values for `testLimits` using the current PID filter parameters (P=0.3, I=0.3, D=0, limits=[-10000, 10000], ERROR_SUM_LIMIT_FACTOR=2) or fix the `ERROR_SUM_LIMIT_FACTOR` if 2 is incorrect. This blocks CI for all PRs touching `io.openems.edge.common`.
