package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

public class BDDPairing {

	int u;
	int vs;
	
	public void set(BDDDomain dom1, BDDDomain dom2) {
		u = BDDDomain.buildEquals(dom1, dom2);
		vs = BDDDomain.set(dom1);
	}
	
	public void set(BDDDomain[] doms1, BDDDomain[] doms2) {
		if (doms1.length == 0) {
			u = 1;
			vs = 1;
			return;
		}
		
		u = BDDDomain.buildEquals(doms1[0], doms2[0]);
		vs = BDDDomain.set(doms1[0]);
		for (int i = 1; i < doms1.length; i++) {
			u = BDD.applyWith(BDD.AND, u, BDDDomain.buildEquals(doms1[i], doms2[i]));
			vs = BDD.applyWith(BDD.AND, vs, BDDDomain.set(doms1[i]));
		}
	}
	
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("u - ");
		out.append(BDD.toString(u));
		out.append("vs - ");
		out.append(BDD.toString(vs));
		return out.toString();
	}
}
