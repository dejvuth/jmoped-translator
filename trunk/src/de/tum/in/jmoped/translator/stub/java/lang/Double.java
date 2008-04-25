package de.tum.in.jmoped.translator.stub.java.lang;

public class Double extends Number {

	double value;
	
	public Double(double value) {
		this.value = value;
	}
	
	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return (float) value;
	}

	@Override
	public int intValue() {
		return (int) value;
	}

	@Override
	public long longValue() {
		return (long) value;
	}

	public boolean equals(java.lang.Object o) {
//		if (!(o instanceof Float))
//			return false;
		
		return value == ((Float) o).value;
	}
}
