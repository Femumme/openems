package io.openems.edge.controller.evcs.batterypricing;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evcs.pricing.EvcsPricing;

/**
 * Simulated {@link EvcsPricing} component for unit tests. Records the last
 * {@link #addPriceCeiling} and {@link #addPriceFloor} calls for assertion.
 */
public class DummyEvcsPricing extends AbstractDummyOpenemsComponent<DummyEvcsPricing>
		implements EvcsPricing, OpenemsComponent {

	private String lastCeilingSource;
	private Double lastCeilingPrice;
	private String lastFloorSource;
	private Double lastFloorPrice;
	private String lastRemoveConstraintSource;

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

	/**
	 * Returns the source ID from the last {@link #addPriceFloor} call.
	 *
	 * @return source or null if never called
	 */
	public String getLastFloorSource() {
		return this.lastFloorSource;
	}

	/**
	 * Returns the price from the last {@link #addPriceFloor} call.
	 *
	 * @return price or null if never called
	 */
	public Double getLastFloorPrice() {
		return this.lastFloorPrice;
	}

	/**
	 * Returns the source ID from the last {@link #removeConstraint} call.
	 *
	 * @return source or null if never called
	 */
	public String getLastRemoveConstraintSource() {
		return this.lastRemoveConstraintSource;
	}

	@Override
	public void addPriceCeiling(String source, double maxPrice) {
		this.lastCeilingSource = source;
		this.lastCeilingPrice = maxPrice;
	}

	@Override
	public void addPriceFloor(String source, double minPrice) {
		this.lastFloorSource = source;
		this.lastFloorPrice = minPrice;
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
		this.lastRemoveConstraintSource = source;
	}
}
