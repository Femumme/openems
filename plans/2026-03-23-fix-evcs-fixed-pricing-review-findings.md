# Fix EVCS Fixed-Pricing Review Findings (F1 + F2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two medium-severity code-review findings in the `feat/fixed-pricing-toggle` branch: remove the now-redundant `enabled` config field (F1) and merge the two duplicate MANUAL_OFF tests into one (F2).

**Architecture:** All changes are confined to one OSGi bundle (`io.openems.edge.controller.evcs.fixedpricing`). F1 removes a dead config field from `Config`, `MyConfig`, and `ControllerEvcsFixedPricingImpl`, then adds a test that guards against `enabled=false` leaving a stale override. F2 merges `mode_off_doesNotSetOverride` and `mode_manual_off_doesNotSetOverride` into a single test that asserts the full MANUAL_OFF contract (no `setOverride`, `removeOverride` called, channel null).

**Tech Stack:** Java 21, OSGi/bnd, JUnit 4, Gradle 9. All commands run from the worktree root: `/Users/felix/Projekte/Mumme-IT/openems/.worktrees/openems-fixed-pricing-toggle`.

---

## File Map

| File | Change |
|---|---|
| `io.openems.edge.controller.evcs.fixedpricing/src/.../Config.java` | Remove `enabled()` attribute definition |
| `io.openems.edge.controller.evcs.fixedpricing/src/.../ControllerEvcsFixedPricingImpl.java` | Pass `true` to `super.activate/modified` (hardcode enabled); remove `enabled` guard from `run()` if present |
| `io.openems.edge.controller.evcs.fixedpricing/test/.../MyConfig.java` | Remove `enabled` field and `setEnabled()` builder method; remove `enabled()` override |
| `io.openems.edge.controller.evcs.fixedpricing/test/.../ControllerEvcsFixedPricingImplTest.java` | Delete `mode_off_doesNotSetOverride`; expand `mode_manual_off_doesNotSetOverride` with the missing assertion |

---

## Task 1: Remove the dead `enabled` field from Config (F1)

These four files must change atomically — removing `enabled()` from the interface breaks compilation in `MyConfig` and `ControllerEvcsFixedPricingImpl`.

**Files:**
- Modify: `io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/Config.java:17-18`
- Modify: `io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImpl.java:57,63`
- Modify: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/MyConfig.java`

- [ ] **Step 1.1: Verify current test baseline passes**

  Run from worktree root (build deps first, then test):
  ```bash
  ./gradlew :io.openems.edge.evcs.pricing.api:build :io.openems.edge.controller.api:build \
    :io.openems.edge.controller.evcs.fixedpricing:test --console=plain --warn
  ```
  Expected: `BUILD SUCCESSFUL`, 5 tests pass. If not, stop and investigate before continuing.

- [ ] **Step 1.2: Remove `enabled` from `Config.java`**

  File: `io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/Config.java`

  Delete these two lines (lines 17–18):
  ```java
  	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
  	boolean enabled() default true;
  ```

  The file after the edit must look like:
  ```java
  package io.openems.edge.controller.evcs.fixedpricing;

  import org.osgi.service.metatype.annotations.AttributeDefinition;
  import org.osgi.service.metatype.annotations.ObjectClassDefinition;

  @ObjectClassDefinition(//
  		name = "Controller Evcs Fixed Pricing", //
  		description = "Provides a fixed EVCS price in €/kWh.")
  @interface Config {

  	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
  	String id() default "ctrlEvcsFixedPricing0";

  	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
  	String alias() default "";

  	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
  	Mode mode() default Mode.MANUAL_ON;

  	@AttributeDefinition(name = "Price [€/kWh]", description = "Fixed EVCS price in Euro per kWh (e.g. 0.35).")
  	double priceEurPerKwh() default 0.35;

  	String webconsole_configurationFactory_nameHint() default "Controller Evcs Fixed Pricing [{id}]";
  }
  ```

- [ ] **Step 1.3: Update `ControllerEvcsFixedPricingImpl` — replace `config.enabled()` with `true`**

  File: `io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImpl.java`

  Change both `activate` and `modified` to pass `true` instead of `config.enabled()`:
  ```java
  @Activate
  private void activate(ComponentContext context, Config config) {
      super.activate(context, config.id(), config.alias(), true);
      this.applyConfig(config);
  }

  @Modified
  private void modified(ComponentContext context, Config config) {
      super.modified(context, config.id(), config.alias(), true);
      this.applyConfig(config);
  }
  ```
  > **Why `true`:** OpenEMS' `AbstractOpenemsComponent.activate(…, enabled)` controls the OSGi component's enabled state. With `mode` now fully owning the on/off logic, the component must always be "active" at the OSGi level. Passing `false` would stop `run()` from being called at all, preventing `removeOverride()` from firing.

- [ ] **Step 1.4: Update `MyConfig` test helper — remove `enabled` field and methods**

  File: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/MyConfig.java`

  a. In `Builder` inner class, delete:
  ```java
  		private boolean enabled = true;
  ```
  and delete the entire `setEnabled` method:
  ```java
  		public Builder setEnabled(boolean enabled) {
  			this.enabled = enabled;
  			return this;
  		}
  ```

  b. Delete the `enabled()` override method (near the end of the class):
  ```java
  	@Override
  	public boolean enabled() {
  		return this.builder.enabled;
  	}
  ```

  The resulting `MyConfig` should have no mention of `enabled` anywhere except that it implicitly satisfies `Config` (which no longer declares `enabled()`).

  > **Why `MyConfig` must change:** `MyConfig implements Config`. Since `Config` no longer declares `enabled()`, the `@Override` annotation on `MyConfig.enabled()` will cause a compile error. The method must be removed entirely.

- [ ] **Step 1.5: Verify compilation**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:compileJava \
    :io.openems.edge.controller.evcs.fixedpricing:compileTestJava --console=plain --warn
  ```
  Expected: `BUILD SUCCESSFUL`, zero errors. If compile errors mention `enabled`, re-check steps 1.2–1.4 for any remaining references.

- [ ] **Step 1.6: Run tests**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:test \
    --console=plain --warn --rerun-tasks
  ```
  Expected: `BUILD SUCCESSFUL`, 5 tests pass.

  > If any test fails with `cannot find symbol: method enabled()` you missed a reference in `MyConfig`. Search the file for "enabled" and remove it.

- [ ] **Step 1.7: Run checkstyle**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:checkstyleMain \
    :io.openems.edge.controller.evcs.fixedpricing:checkstyleTest --console=plain --warn
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 1.8: Commit**

  ```bash
  git add \
    io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/Config.java \
    io.openems.edge.controller.evcs.fixedpricing/src/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImpl.java \
    io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/MyConfig.java
  git commit -m "refactor(evcs): remove redundant enabled field, mode owns on/off semantics"
  ```

---

## Task 2: Merge duplicate MANUAL_OFF tests (F2)

**Files:**
- Modify: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImplTest.java`

- [ ] **Step 2.1: Delete `mode_off_doesNotSetOverride` entirely**

  File: `io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImplTest.java`

  Remove the entire method including its Javadoc (lines 63–84 in the current file):
  ```java
  	/**
  	 * When {@code mode=MANUAL_OFF}, {@code run()} must not call {@code setOverride}
  	 * and {@code ACTIVE_OVERRIDE} must remain null.
  	 */
  	@Test
  	public void mode_off_doesNotSetOverride() throws Exception {
  		var dummy = new DummyEvcsPricing();

  		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
  				.addReference("evcsPricing", dummy) //
  				.activate(MyConfig.create() //
  						.setId(CTRL_ID) //
  						.setMode(Mode.MANUAL_OFF) //
  						.setPriceEurPerKwh(PRICE) //
  						.build()) //
  				.next(new TestCase() //
  						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
  				.deactivate();

  		assertNull(dummy.getLastSetOverrideSource());
  		assertNull(dummy.getLastSetOverridePrice());
  	}
  ```

- [ ] **Step 2.2: Expand `mode_manual_off_doesNotSetOverride` to cover the full contract**

  Replace the existing `mode_manual_off_doesNotSetOverride` method (currently missing the `getLastSetOverrideSource` and `getLastSetOverridePrice` assertions) with this version that asserts all three behavioral guarantees:

  ```java
  	/**
  	 * When {@code mode=MANUAL_OFF}, {@code run()} must call {@code removeOverride},
  	 * must not call {@code setOverride}, and {@code ACTIVE_OVERRIDE} must be null.
  	 */
  	@Test
  	public void mode_off_removesOverride_doesNotSetOverride() throws Exception {
  		var dummy = new DummyEvcsPricing();

  		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
  				.addReference("evcsPricing", dummy) //
  				.activate(MyConfig.create() //
  						.setId(CTRL_ID) //
  						.setMode(Mode.MANUAL_OFF) //
  						.setPriceEurPerKwh(PRICE) //
  						.build()) //
  				.next(new TestCase() //
  						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
  				.deactivate();

  		assertNull(dummy.getLastSetOverrideSource());
  		assertNull(dummy.getLastSetOverridePrice());
  		assertEquals(CTRL_ID, dummy.getLastRemoveOverrideSource());
  	}
  ```

  > **Why rename:** The new name `mode_off_removesOverride_doesNotSetOverride` describes both sides of the contract (what IS called and what is NOT called), eliminating the ambiguity of the old "doesNotSetOverride" name.

- [ ] **Step 2.3: Run tests — expect 4 tests, all pass**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:test \
    --console=plain --warn --rerun-tasks
  ```
  Expected output includes:
  ```
  BUILD SUCCESSFUL
  ```
  And in `--info` output you should see 4 test methods: `setsOverride_onRun`, `deactivate_removesOverride`, `mode_off_removesOverride_doesNotSetOverride`, `mode_switch_to_manual_on_setsOverride`.

  > If the new test fails with `AssertionError: expected:<ctrlEvcsFixedPricing0> but was:<null>` on `getLastRemoveOverrideSource`, it means `deactivate()` is setting `lastRemoveOverrideSource` but `run()` is not — check the `MANUAL_OFF` branch in `ControllerEvcsFixedPricingImpl.run()`.

- [ ] **Step 2.4: Run checkstyle**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:checkstyleTest \
    --console=plain --warn --rerun-tasks
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2.5: Commit**

  ```bash
  git add \
    io.openems.edge.controller.evcs.fixedpricing/test/io/openems/edge/controller/evcs/fixedpricing/ControllerEvcsFixedPricingImplTest.java
  git commit -m "test(evcs): merge duplicate MANUAL_OFF tests into single full-contract test"
  ```

---

## Final Verification

- [ ] **Step 3.1: Full test + checkstyle pass**

  ```bash
  ./gradlew :io.openems.edge.controller.evcs.fixedpricing:test \
    :io.openems.edge.controller.evcs.fixedpricing:checkstyleMain \
    :io.openems.edge.controller.evcs.fixedpricing:checkstyleTest \
    --console=plain --warn --rerun-tasks
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.2: Confirm git log**

  ```bash
  git log --oneline -3
  ```
  Expected (newest first):
  ```
  <hash> test(evcs): merge duplicate MANUAL_OFF tests into single full-contract test
  <hash> refactor(evcs): remove redundant enabled field, mode owns on/off semantics
  <hash> fix(ui): use strict equality in fixed-pricing templates
  ```
