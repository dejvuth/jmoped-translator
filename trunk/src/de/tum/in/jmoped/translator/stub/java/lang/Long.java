package de.tum.in.jmoped.translator.stub.java.lang;

@SuppressWarnings("serial")
public class Long extends Number {

	long value;
	
	public Long(long value) {
		this.value = value;
	}
	
	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public int intValue() {
		return (int) value;
	}

	@Override
	public long longValue() {
		return value;
	}
}
