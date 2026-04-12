package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

/**
 * Tests for {@link ManagedSymmetricEss#setActivePowerEqualsWithPid}.
 */
public class ManagedSymmetricEssTest {

	/**
	 * A {@link DummyManagedSymmetricEss} that always throws on
	 * {@link #setActivePowerEquals(Integer)}, simulating a constraint rejection.
	 */
	private static class ThrowingManagedSymmetricEss extends DummyManagedSymmetricEss {

		private ThrowingManagedSymmetricEss(String id) {
			super(id);
		}

		@Override
		public void setActivePowerEquals(Integer value) throws OpenemsNamedException {
			throw new OpenemsException("Simulated constraint rejection");
		}
	}

	/**
	 * Verifies that when {@code setActivePowerEquals} throws
	 * {@link OpenemsNamedException}, the PID filter state is restored to its
	 * pre-call snapshot so the integrator does not drift on rejected commands.
	 */
	@Test
	public void testPidStateRestoredOnRejection() {
		// Given: a real PidFilter primed with non-trivial integrator state
		var pidFilter = new PidFilter(0.3, 0.3, 0.1);
		var power = new DummyPower(10000, pidFilter);
		var ess = new ThrowingManagedSymmetricEss("ess0");
		ess.setPower(power);

		pidFilter.setLimits(-10000, 10000);
		pidFilter.applyPidFilter(0, 5000); // advances errorSum / lastInput / firstRun

		var snapshotBefore = pidFilter.saveState();

		// When: the static method is called and setActivePowerEquals throws
		try {
			ManagedSymmetricEss.setActivePowerEqualsWithPid(ess, 8000, null);
			fail("Expected OpenemsNamedException to be thrown");
		} catch (OpenemsNamedException e) {
			// expected — exception must propagate
		}

		// Then: PID state equals the pre-call snapshot (rollback occurred)
		var snapshotAfter = pidFilter.saveState();
		assertEquals("errorSum must be restored", snapshotBefore.errorSum(), snapshotAfter.errorSum(), 0.0);
		assertEquals("lastInput must be restored", snapshotBefore.lastInput(), snapshotAfter.lastInput(), 0.0);
		assertEquals("firstRun must be restored", snapshotBefore.firstRun(), snapshotAfter.firstRun());
	}

	/**
	 * Verifies that when {@code setActivePowerEquals} succeeds, the PID filter
	 * state is NOT rolled back — the integrator advances as expected.
	 *
	 * @throws OpenemsNamedException should not be thrown in the success path
	 */
	@Test
	public void testPidStateAdvancesOnSuccess() throws OpenemsNamedException {
		// Given: a real PidFilter primed with non-trivial integrator state
		var pidFilter = new PidFilter(0.3, 0.3, 0.1);
		var power = new DummyPower(10000, pidFilter);
		var ess = new DummyManagedSymmetricEss("ess0");
		ess.setPower(power);

		pidFilter.setLimits(-10000, 10000);
		pidFilter.applyPidFilter(0, 5000); // advances errorSum / lastInput / firstRun

		var snapshotBefore = pidFilter.saveState();

		// When: the static method is called and setActivePowerEquals succeeds
		ManagedSymmetricEss.setActivePowerEqualsWithPid(ess, 8000, null);

		// Then: PID state has advanced (applyPidFilter ran inside the method)
		var snapshotAfter = pidFilter.saveState();
		var stateUnchanged = snapshotBefore.errorSum() == snapshotAfter.errorSum()
				&& snapshotBefore.lastInput() == snapshotAfter.lastInput()
				&& snapshotBefore.firstRun() == snapshotAfter.firstRun();
		assertEquals("PID state must advance on a successful call", false, stateUnchanged);
	}
}
