package io.openems.edge.common.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Use this class to test if the PID filter does what it should. Test cases can
 * be generated using the Excel file in the docs directory. Just copy the
 * contents of the Excel sheet "Unit-Test" into the testhis.t() method in this
 * file.
 */
public class PidFilterTest {

	@Before
	public void prepare() {
		System.out.println(String.format("%10s  %10s  %10s", "input", "output", "expected"));
	}

	@Test
	public void test() {
		var p = new PidFilter(0.3, 0.3, 0);
		p.setLimits(-100000, 100000);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 20000, 6000);
		this.t(p, 0, 20000, 12000);
		this.t(p, 3793, 20000, 16862);
		this.t(p, 8981, 20000, 20168);
		this.t(p, 13963, 20000, 21979);
		this.t(p, 17885, 20000, 22613);
		this.t(p, 20473, 20000, 22472);
		this.t(p, 21826, 20000, 21924);
		this.t(p, 22234, 20000, 21254);
		this.t(p, 22038, 20000, 20642);
		this.t(p, 21542, 20000, 20180);
		this.t(p, 20973, 20000, 19888);
		this.t(p, 20472, 20000, 19746);
		this.t(p, 20103, 20000, 19715);
		this.t(p, 19877, 20000, 19752);
		this.t(p, 19775, 20000, 19820);
		this.t(p, 19760, 20000, 19892);
		this.t(p, 19798, 20000, 19952);
		this.t(p, 19857, 20000, 19995);
		this.t(p, 19917, 20000, 20020);
		this.t(p, 19966, 20000, 20030);
		this.t(p, 20000, 20000, 20000);
		this.t(p, 20019, 20000, 20024);
		this.t(p, 20007, 20000, 20022);
		this.t(p, 20018, 20000, 20017);
		this.t(p, 20021, 20000, 20011);
		this.t(p, 20018, 20000, 20005);
		this.t(p, 20014, 20000, 20001);
		this.t(p, 20008, 20000, 19999);
	}

	@Test
	public void testLimits() {
		var p = new PidFilter(0.3, 0.3, 0);
		p.setLimits(-10000, 10000);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 10000, 3000);
		this.t(p, 0, 10000, 6000);
		this.t(p, 1896, 10000, 8431);
		this.t(p, 4490, 10000, 10000);
		this.t(p, 6981, 10000, 10000);
		this.t(p, 8889, 10000, 10000);
		this.t(p, 9591, 10000, 10000);
		this.t(p, 9850, 10000, 10000);
		this.t(p, 9945, 10000, 10000);
		this.t(p, 9980, 10000, 10000);
		this.t(p, 9993, 10000, 10000);
		this.t(p, 9997, 10000, 10000);
		this.t(p, 9999, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
	}

	/**
	 * This test simulates a PID filter that is executed inside a Peak-Shaving
	 * Controller, but after a FixActivePower-Controller which is set to 1000 W.
	 * Peak-Shaving is expected to require 5000 W discharge.
	 */
	@Test
	public void testPriority() {
		var p = new PidFilter(0.3, 0.3, 0.1);

		// Cycle 1
		p.setLimits(-1000, 1000); // set by FixActivePower
		this.t(p, 1000, 5000, 1000);

		// Cycle 2
		// Limits and input value stay always at 1000 by FixActivePower
		this.t(p, 1000, 5000, 1000);

		// Cycle 3
		this.t(p, 1000, 5000, 1000);

		// Cycle 4
		this.t(p, 1000, 5000, 1000);

		// Cycle 5
		this.t(p, 1000, 5000, 1000);

		// Cycle 6
		this.t(p, 1000, 5000, 1000);

		// Cycle 7
		this.t(p, 1000, 5000, 1000);

		// Cycle 8
		p.setLimits(-9999, 9999); // disable FixActivePower
		this.t(p, 1000, 5000, 1200);

		// Cycle 9
		this.t(p, 1000, 5000, 2400);

		// Cycle 10
		this.t(p, 1000, 5000, 3600);
	}

	@Test
	public void testSaveRestoreIdempotency() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-100000, 100000);

		// Prime the filter to move past firstRun and accumulate some errorSum
		p.applyPidFilter(0, 10000);

		// Given: a stable mid-run state
		var snapshot = p.saveState();

		// When: apply with targetA, record output; restore; apply again with targetA
		var output1 = p.applyPidFilter(5000, 15000);
		p.restoreState(snapshot);
		var output2 = p.applyPidFilter(5000, 15000);

		// Then: same state produces same output
		assertEquals(output1, output2);
	}

	@Test
	public void testRestoreRollsBackErrorSum() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-100000, 100000);

		// Prime the filter
		p.applyPidFilter(0, 10000);

		// Given: capture state before mutation
		var snapshot = p.saveState();
		var preErrorSum = snapshot.errorSum();

		// When: apply opposing target to mutate errorSum, then restore
		p.applyPidFilter(10000, -10000);
		p.restoreState(snapshot);

		// Then: errorSum is back to pre-mutation value
		assertEquals(preErrorSum, p.saveState().errorSum(), 0.0);
	}

	@Test
	public void testRestoreRollsBackLastInput() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-100000, 100000);

		// Prime: two cycles to move past firstRun and set lastInput
		p.applyPidFilter(0, 10000);    // firstRun cleared, lastInput = 0
		p.applyPidFilter(5000, 10000); // lastInput = 5000

		// Capture state with lastInput = 5000
		var snapshot = p.saveState();

		// Mutate: cycle that changes lastInput to 8000
		p.applyPidFilter(8000, 10000);

		// Branch A: restore snapshot (lastInput back to 5000), then query
		p.restoreState(snapshot);
		var outputAfterRestore = p.applyPidFilter(8000, 10000);

		// Branch B: re-prime to same snapshot, then drive lastInput to 8000 without restoring
		p.restoreState(snapshot);
		p.applyPidFilter(8000, 10000); // lastInput now 8000
		var outputWithoutRestore = p.applyPidFilter(8000, 10000);

		// D-term = -0.1 * (input - lastInput)
		// After restore: lastInput = 5000, so D = -0.1 * (8000 - 5000) = -300
		// Without restore: lastInput = 8000, so D = -0.1 * (8000 - 8000) = 0
		assertNotEquals("lastInput restoration must affect D-term output",
				outputAfterRestore, outputWithoutRestore);
	}

	@Test
	public void testRestoreResumesFirstRunBehavior() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-100000, 100000);

		// Save fresh state (firstRun = true)
		var freshSnapshot = p.saveState();
		assertTrue("Snapshot should capture firstRun=true", freshSnapshot.firstRun());

		// Run cycles to clear firstRun and accumulate state
		p.applyPidFilter(0, 10000);
		p.applyPidFilter(5000, 10000);

		// Verify firstRun is now false
		assertFalse("After cycles, firstRun should be false", p.saveState().firstRun());

		// Restore fresh state
		p.restoreState(freshSnapshot);

		// Verify firstRun is true again
		assertTrue("After restore, firstRun should be true", p.saveState().firstRun());

		// When firstRun=true, applyPidFilter sets lastInput=input so D-term = 0.
		// A second fresh filter is in the same initial state — outputs must match.
		var fresh = new PidFilter(0.3, 0.3, 0.1);
		fresh.setLimits(-100000, 100000);

		var restoredOutput = p.applyPidFilter(3000, 10000);
		var freshOutput = fresh.applyPidFilter(3000, 10000);
		assertEquals("Restored firstRun filter must behave like a fresh filter",
				freshOutput, restoredOutput);
	}

	/**
	 * Verifies that the PID converges to the full target when target equals the
	 * output limit. Before the dynamic integrator-limit fix, the PID would lock at
	 * ~69% of the target (e.g. 505 kW out of 730 kW) due to integrator saturation.
	 */
	@Test
	public void testConvergesToTargetAtLimit() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-730_000, 0);

		// Simulate a perfect actuator: each cycle the hardware reaches the previous
		// PID output, so input = previous output.
		var power = 0;
		for (var cycle = 0; cycle < 100; cycle++) {
			power = p.applyPidFilter(power, -730_000);
		}

		// Must converge within 1% of target — the old code locked at ~505 kW (69%)
		assertTrue("PID must converge to target, got " + power,
				power <= -730_000 * 0.99);
	}

	/**
	 * Verifies that the PID still provides smooth ramping (not jumping to the
	 * target in one cycle) when limits are wide.
	 */
	@Test
	public void testSmoothRampUp() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-730_000, 0);

		// First cycle from rest: output must be a fraction of the target, not the
		// full target (PID smoothing preserved)
		var firstOutput = p.applyPidFilter(0, -730_000);
		assertTrue("First cycle must not jump to full target",
				firstOutput > -730_000);
		assertTrue("First cycle must make progress toward target",
				firstOutput < 0);
	}

	private void t(PidFilter p, int input, int output, int expectedOutput) {
		System.out.println(String.format("%10d  %10d  %10d", input, output, expectedOutput));
		assertEquals(expectedOutput, p.applyPidFilter(input, output));
	}

}
