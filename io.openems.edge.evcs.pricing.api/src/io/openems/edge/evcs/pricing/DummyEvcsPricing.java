package io.openems.edge.evcs.pricing;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

/**
 * Shared test double for {@link EvcsPricing}. Records the most recent call to
 * each mutating method for assertion in unit tests.
 */
public class DummyEvcsPricing extends AbstractDummyOpenemsComponent<DummyEvcsPricing>
		implements EvcsPricing, OpenemsComponent {

	private String lastCeilingSource;
	private Double lastCeilingPrice;
	private String lastFloorSource;
	private Double lastFloorPrice;
	private String lastSetOverrideSource;
	private Double lastSetOverridePrice;
	private String lastRemoveOverrideSource;
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

	/** Resets all recorded state. Call between test cases if needed. */
	public void reset() {
		this.lastCeilingSource = null;
		this.lastCeilingPrice = null;
		this.lastFloorSource = null;
		this.lastFloorPrice = null;
		this.lastSetOverrideSource = null;
		this.lastSetOverridePrice = null;
		this.lastRemoveOverrideSource = null;
		this.lastRemoveConstraintSource = null;
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
		this.lastSetOverrideSource = source;
		this.lastSetOverridePrice = price;
	}

	@Override
	public void removeOverride(String source) {
		this.lastRemoveOverrideSource = source;
	}

	@Override
	public void removeConstraint(String source) {
		this.lastRemoveConstraintSource = source;
	}
}
