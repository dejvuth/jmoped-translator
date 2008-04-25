package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import de.tum.in.jmoped.translator.stub.java.math.BigInteger;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDFactory.Node;

public class BDD {

	int u;
	
	private final static int AND = 0;
	private final static int OR = 1;
	
	public BDD(int u) {
		this.u = u;
	}
	
	public int getNode() {
		return u;
	}
	
	private static int applyWith(int op, int u1, int u2) {
		
		if (u1 <= 1 && u2 <= 1) {
			if (op == AND) return u1 & u2;
			
			// OR
			return u1 | u2;
		}
		
		Node node1 = BDDFactory.nodes[u1];
		Node node2 = BDDFactory.nodes[u2];
		int var1 = node1.var;
		int var2 = node2.var;
		
		if (var1 == var2)
			return BDDFactory.mk(var1, applyWith(op, node1.low, node2.low), 
					applyWith(op, node1.high, node2.high));
		
		if (var1 < var2)
			return BDDFactory.mk(var1, applyWith(op, node1.low, u2), applyWith(op, node1.high, u2));
		
		// var1 > var2
		return BDDFactory.mk(var2, applyWith(op, u1, node2.low), applyWith(op, u1, node2.high));
	}
	
	public BDD andWith(BDD that) {
		u = applyWith(AND, u, that.u);
		return this;
	}
	
	public BDD orWith(BDD that) {
		u = applyWith(OR, u, that.u);
		return this;
	}
	
	public BDD exist(BDDVarSet varset) {
		BDD bdd = this.id();
		Node[] nodes = BDDFactory.nodes;
		
		// Traverses down through high branch until node one is met
		int current = varset.u;
		while (current != 1) {
			int var = nodes[current].var;
			BDD x = bdd.restrict(var, false);
			x.orWith(bdd.restrict(var, true));
			bdd = x;
			current = BDDVarSet.next(current);
		}
		return bdd;
	}
	
	public BDD id() {
		BDD bdd = new BDD(u);
		return bdd;
	}
	
	public BDD restrict(int var, boolean value) {
		return new BDD(restrictWith(u, var, value));
	}
	
	public BDD restrictWith(int var, boolean value) {
		u = restrictWith(u, var, value);
		return this;
	}
	
	private int restrictWith(int u, int j, boolean b) {
		Node node = BDDFactory.nodes[u];
		int var = node.var;
		if (var > j) return u;
		if (var < j) return BDDFactory.mk(var, restrictWith(node.low, j, b), restrictWith(node.high, j, b));
		if (b) return restrictWith(node.high, j, b);
		return restrictWith(node.low, j, b);
	}
	
	public BigInteger scanVar(BDDDomain dom) {
		int[] vars = dom.vars;
		int value = 0;
		for (int i = 0; i < vars.length; i++) {
			value <<= 1;
			value += scanVar(u, vars[i]);
		}
		
		return new BigInteger(value);
	}
	
	private static int scanVar(int u, int var) {
		Node thisnode = BDDFactory.nodes[u];
		int thisvar = thisnode.var;
		if (thisvar < var) {
			if (thisnode.low == 0) return scanVar(thisnode.high, var);
			return scanVar(thisnode.low, var);
		}
		if (thisvar == var) {
			if (thisnode.low == 0) return 1;
			else return 0;
		}
		return 0;
	}
	
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append(String.format("%d%n", u));
		out.append(BDDFactory.toString(BDDFactory.nodes, BDDFactory.nodenum));
		return out.toString();
	}
}
