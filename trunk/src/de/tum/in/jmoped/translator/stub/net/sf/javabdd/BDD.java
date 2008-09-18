package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDFactory.Node;

public class BDD {

	/**
	 * Node.
	 */
	int u;
	
	final static int AND = 0;
	final static int BIIMP = 1;
	final static int OR = 2;
	
	public BDD(int u) {
		this.u = u;
	}
	
	public int getNode() {
		return u;
	}
	
	static int applyWith(int op, int u1, int u2) {
		
		if (u1 <= 1 && u2 <= 1) {
			switch (op) {
			case AND: return u1 & u2;
			case BIIMP: return (u1 == u2) ? 1 : 0;
			case OR: return u1 | u2;
			}
		}
		
		Node node1 = BDDFactory.nodes[u1];
		Node node2 = BDDFactory.nodes[u2];
		int var1 = node1.var;
		int var2 = node2.var;
		
		if (var1 == var2)
			return BDDFactory.mk(var1, applyWith(op, node1.low, node2.low), 
					applyWith(op, node1.high, node2.high));
		
		if (var1 < var2)
			return BDDFactory.mk(var1, applyWith(op, node1.low, u2), 
					applyWith(op, node1.high, u2));
		
		// var1 > var2
		return BDDFactory.mk(var2, applyWith(op, u1, node2.low), 
				applyWith(op, u1, node2.high));
	}
	
	public BDD andWith(BDD that) {
		u = applyWith(AND, u, that.u);
		return this;
	}
	
	public BDD and(BDD that) {
		return new BDD(applyWith(AND, u, that.u));
	}
	
	public BDD biimpWith(BDD that) {
		u = applyWith(BIIMP, u, that.u);
		return this;
	}
	
	public BDD orWith(BDD that) {
		u = applyWith(OR, u, that.u);
		return this;
	}
	
	public BDD replaceWith(BDDPairing pairing) {
		u = applyWith(AND, u, pairing.u);
//		System.out.println("After and");
//		System.out.println(BDD.toString(u));
		u = exist(u, pairing.vs);
		return this;
	}
	
	private static int not(int index) {
		if (index == 0) return 1;
		if (index == 1) return 0;
		
		Node node = BDDFactory.nodes[index];
		return BDDFactory.mk(node.var, not(node.low), not(node.high));
		
		
//		if (node.low == 0) node.low = 1;
//		else if (node.low == 1) node.low = 0;
//		else not(node.low);
//		
//		if (node.high == 0) node.high = 1;
//		else if (node.high == 1) node.high = 0;
//		else not (node.high);
	}
	
	public BDD not() {
		return new BDD(not(u));
	}
	
	private static int exist(int bdd, int u) {
		Node[] nodes = BDDFactory.nodes;
		
		// Traverses down through high branch until node one is met
		int current = u;
		while (current != 1) {
			int var = nodes[current].var;
			int x = restrictWith(bdd, var, false);
			x = applyWith(OR, x, restrictWith(bdd, var, true));
			bdd = x;
			current = BDDVarSet.next(current);
//			
//			BDD x = bdd.restrict(var, false);
//			x.orWith(bdd.restrict(var, true));
//			bdd = x;
//			current = BDDVarSet.next(current);
		}
		return bdd;
	}
	
	public BDD exist(BDDVarSet varset) {
		return new BDD(exist(this.u, varset.u));
		
//		BDD bdd = this.id();
//		Node[] nodes = BDDFactory.nodes;
//		
//		// Traverses down through high branch until node one is met
//		int current = varset.u;
//		while (current != 1) {
//			int var = nodes[current].var;
//			BDD x = bdd.restrict(var, false);
//			x.orWith(bdd.restrict(var, true));
//			bdd = x;
//			current = BDDVarSet.next(current);
//		}
//		return bdd;
	}
	
	public BDDFactory getFactory() {
		return BDDFactory.factory;
	}
	
	public BDD id() {
		BDD bdd = new BDD(u);
		return bdd;
	}
	
	public boolean isZero() {
		return u == 0;
	}
	
	public boolean isOne() {
		return u == 1;
	}
	
	public int var() {
		return  u;
	}
	
	public BDD restrict(int var, boolean value) {
		return new BDD(restrictWith(u, var, value));
	}
	
	public BDD restrictWith(int var, boolean value) {
		u = restrictWith(u, var, value);
		return this;
	}
	
	private static int restrictWith(int u, int j, boolean b) {
		Node node = BDDFactory.nodes[u];
		int var = node.var;
		if (var > j) return u;
		if (var < j) return BDDFactory.mk(var, restrictWith(node.low, j, b), restrictWith(node.high, j, b));
		if (b) return restrictWith(node.high, j, b);
		return restrictWith(node.low, j, b);
	}
	
	public long scanVar(BDDDomain dom) {
		int[] vars = dom.vars;
		long value = 0;
		for (int i = 0; i < vars.length; i++) {
			value <<= 1;
			value += scanVar(u, vars[i]);
		}
		
		return value;
	}
	
	/**
	 * Scans this BDD beginning at <code>u</code> for an assignment to
	 * <code>var</code>. 
	 * 
	 * @param u the BDD node where the scan starts.
	 * @param var the variable to be scanned for.
	 * @return zero or one.
	 */
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
	
//	public BigInteger scanVarMax(BDDDomain dom) {
//		int[] vars = dom.vars;
//		int value = 0;
//		for (int i = 0; i < vars.length; i++) {
//			value <<= 1;
//			value += scanVarMax(u, vars[i]);
//		}
//		
//		return BigInteger.valueOf(value);
//	}
//	
//	private static int scanVarMax(int u, int var) {
//		Node thisnode = BDDFactory.nodes[u];
//		int thisvar = thisnode.var;
//		if (thisvar < var) {
//			if (thisnode.high != 0) return scanVarMax(thisnode.high, var);
//			return scanVarMax(thisnode.low, var);
//		}
//		if (thisvar == var) {
//			if (thisnode.high != 0) return 1;
//			else return 0;
//		}
//		return 1;
//	}
	
	public BDDIterator iterator(BDDVarSet var) {
		return new BDDIterator(this, var);
	}
	
	public static class BDDIterator {
		int u;
		BDDVarSet varset;
		
		BDDIterator(BDD bdd, BDDVarSet varset) {
			this.u = bdd.u;
			this.varset = varset;
		}
		
		public boolean hasNext() {
			return u != 0;
		}
		
		public BDD nextBDD() {
			Node[] nodes = BDDFactory.nodes;
			
			// Traverses down through high branch until node one is met
			int a = 1;
			int current = varset.u;
			while (current != 1) {
				int var = nodes[current].var;
				int s = scanVar(u, var);
				if (s == 0)
					a = applyWith(AND, a, BDDFactory.mk(var, 1, 0));
				else
					a = applyWith(AND, a, BDDFactory.mk(var, 0, 1));
				current = BDDVarSet.next(current);
			}
			
			u = applyWith(AND, u, not(a));
			return new BDD(a);
		}
	}
	
	void print() {
		System.out.print(u);
	}
	
	static String toString(int u) {
		StringBuilder out = new StringBuilder();
		out.append(String.format("%d%n", u));
		out.append(BDDFactory.toString(BDDFactory.nodes, BDDFactory.nodenum));
		return out.toString();
	}
	
	public String toString() {
		return toString(u);
//		StringBuilder out = new StringBuilder();
//		out.append(String.format("%d%n", u));
//		out.append(BDDFactory.toString(BDDFactory.nodes, BDDFactory.nodenum));
//		return out.toString();
	}
}
