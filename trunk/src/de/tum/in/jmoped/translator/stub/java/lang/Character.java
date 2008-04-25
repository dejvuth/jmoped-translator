package de.tum.in.jmoped.translator.stub.java.lang;

public class Character implements Comparable<Character> {

	private char value;
	
	public Character(char value) {
		this.value = value;
	}

	public int compareTo(Character o) {
		if (value < o.value) return -1;
		if (value == o.value) return 0;
		return 1;
	}
	
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Character)) return false;
		return value == ((Character) o).value;
	}
}
