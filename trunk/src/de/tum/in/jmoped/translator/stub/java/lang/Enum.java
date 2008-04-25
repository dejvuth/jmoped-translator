package de.tum.in.jmoped.translator.stub.java.lang;

public class Enum {

	private final java.lang.String name;
	private final int ordinal;
	
	protected Enum(java.lang.String name, int ordinal) {
		this.name = name;
		this.ordinal = ordinal;
	}
	
	public java.lang.String name() {
		return name;
	}
	
	public int ordinal() {
		return ordinal;
	}
}
