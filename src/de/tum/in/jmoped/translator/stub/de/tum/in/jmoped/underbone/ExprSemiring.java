package de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone;

public class ExprSemiring {

	int type;
	Object value;
	Object aux;
	
	public static final int PUSH = 0;
	
	public ExprSemiring(int type) {
		this.type = type;
	}
	
	public ExprSemiring(int type, Object value) {
		this.type = type;
		this.value = value;
	}
}
