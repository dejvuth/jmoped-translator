package de.tum.in.jmoped.translator.stub.java.lang;

@SuppressWarnings("serial")
public class Integer extends Number implements Comparable<Integer> {
	
	int value;
	
	public Integer(int value) {
		this.value = value;
	}
	
	public static Integer valueOf(int value) {
		return new Integer(value);
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
		return value;
	}

	@Override
	public long longValue() {
		return value;
	}
	
	public boolean equals(java.lang.Object o) {
		if (o == null) return false;
//		if (!(o instanceof Integer))
//			return false;
		
		return value == ((Integer) o).value;
	}

	public int compareTo(Integer o) {
		if (value == o.value) return 0;
		if (value < o.value) return -1;
		return 1;
	}
}
