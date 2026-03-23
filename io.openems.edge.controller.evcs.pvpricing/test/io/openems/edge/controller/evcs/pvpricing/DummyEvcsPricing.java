package io.openems.edge.controller.evcs.pvpricing;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evcs.pricing.EvcsPricing;

/**
 * Simulated {@link EvcsPricing} component for unit tests. Records the last
 * {@link #addPriceCeiling} call for assertion.
 */
public class DummyEvcsPricing extends AbstractDummyOpenemsComponent<DummyEvcsPricing>
		implements EvcsPricing, OpenemsComponent {

	private String lastCeilingSource;
	private Double lastCeilingPrice;

	public DummyEvcsPricing() {
		super(EvcsPricing.SINGLETON_COMPONENT_ID, //
				OpenemsComponent.ChannelId.values(), //
				EvcsPricing.ChannelId.values());
	}

	@Override
	protected DummyEvcsPricing self() {
		return this;
	}

	/**
	 * Returns the source ID from the last {@link #addPriceCeiling} call.
	 *
	 * @return source or null if never called
	 */
	public String getLastCeilingSource() {
		return this.lastCeilingSource;
	}

	/**
	 * Returns the price from the last {@link #addPriceCeiling} call.
	 *
	 * @return price or null if never called
	 */
	public Double getLastCeilingPrice() {
		return this.lastCeilingPrice;
	}

	/** Resets recorded state so tests start clean. */
	public void reset() {
		this.lastCeilingSource = null;
		this.lastCeilingPrice = null;
	}

	@Override
	public void addPriceCeiling(String source, double maxPrice) {
		this.lastCeilingSource = source;
		this.lastCeilingPrice = maxPrice;
	}

	@Override
	public void addPriceFloor(String source, double minPrice) {
		// no-op for tests
	}

	@Override
	public void setOverride(String source, double price) {
		// no-op for tests
	}

	@Override
	public void removeOverride(String source) {
		// no-op for tests
	}

	@Override
	public void removeConstraint(String source) {
		// no-op for tests
	}
}
