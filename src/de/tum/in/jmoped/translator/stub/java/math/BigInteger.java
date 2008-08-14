package de.tum.in.jmoped.translator.stub.java.math;

public class BigInteger {

	long value;
	
	private BigInteger(long value) {
		this.value = value;
	}
	
	public static BigInteger valueOf(long val) {
		return new BigInteger(val);
	}
	
	public int intValue() {
		return (int) value;
	}
	
	public long longValue() {
		return value;
	}
}
