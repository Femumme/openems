package io.openems.edge.evcs.technagon;

public enum ChargePoint {
	CP1(0x1000), //
	CP2(0x2000);

	private final int offset;

	private ChargePoint(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return this.offset;
	}
}
