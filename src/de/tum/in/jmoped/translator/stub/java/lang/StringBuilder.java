package de.tum.in.jmoped.translator.stub.java.lang;

public class StringBuilder {
	
	java.lang.String s;

	public StringBuilder() {}
	
	public StringBuilder(java.lang.String str) {
		s = str;
	}
	
	public StringBuilder append(Object obj) {
		return this;
	}
	
	public StringBuilder append(java.lang.String str) {
		return this;
	}
	
	public java.lang.String toString() {
		return s;
	}
}
