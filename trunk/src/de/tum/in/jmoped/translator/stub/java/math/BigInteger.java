package de.tum.in.jmoped.translator.stub.java.math;

public class BigInteger {

	long value;
	
	public BigInteger(int value) {
		this.value = value;
	}
	
	public BigInteger(long value) {
		this.value = value;
	}
	
	public int intValue() {
		return (int) value;
	}
	
	public long longValue() {
		return value;
	}
}
