package io.openems.edge.controller.evcs.fixedpricing;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evcs.pricing.EvcsPricing;

/**
 * Simulated {@link EvcsPricing} component for unit tests. Records calls to
 * {@link #setOverride} and {@link #removeOverride} for assertion.
 */
public class DummyEvcsPricing extends AbstractDummyOpenemsComponent<DummyEvcsPricing>
		implements EvcsPricing, OpenemsComponent {

	private String lastSetOverrideSource;
	private Double lastSetOverridePrice;
	private String lastRemoveOverrideSource;

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
	 * Returns the source ID from the last {@link #setOverride} call.
	 *
	 * @return source or null if never called
	 */
	public String getLastSetOverrideSource() {
		return this.lastSetOverrideSource;
	}

	/**
	 * Returns the price from the last {@link #setOverride} call.
	 *
	 * @return price or null if never called
	 */
	public Double getLastSetOverridePrice() {
		return this.lastSetOverridePrice;
	}

	/**
	 * Returns the source ID from the last {@link #removeOverride} call.
	 *
	 * @return source or null if never called
	 */
	public String getLastRemoveOverrideSource() {
		return this.lastRemoveOverrideSource;
	}

	@Override
	public void addPriceCeiling(String source, double maxPrice) {
		// no-op for tests
	}

	@Override
	public void addPriceFloor(String source, double minPrice) {
		// no-op for tests
	}

	@Override
	public void setOverride(String source, double price) {
		this.lastSetOverrideSource = source;
		this.lastSetOverridePrice = price;
	}

	@Override
	public void removeOverride(String source) {
		this.lastRemoveOverrideSource = source;
	}

	@Override
	public void removeConstraint(String source) {
		// no-op for tests
	}
}
