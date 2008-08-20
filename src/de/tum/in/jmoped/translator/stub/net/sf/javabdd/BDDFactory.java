package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

public class BDDFactory {
	
	static int maxnodenum;
	static Node[] nodes;
	static int nodenum;
	
//	static Node zero;
//	static Node one;
	
//	static BDD zero;
//	static BDD one;
	
	static BDDFactory factory;
	
	private BDDFactory(int max) {
		maxnodenum = max;
		nodes = new Node[maxnodenum];
	}

	public static BDDFactory init(String bddpackage, int nodenum, int cachesize) {
		factory = new BDDFactory(nodenum);
		return factory;
	}
	
	public BDDDomain[] extDomain(long[] sizes) {
		// Construct BDD domains
		BDDDomain[] doms = new BDDDomain[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			doms[i] = new BDDDomain(sizes[i]);
		}
		
		// Counts the number of variables
		int varnum = 0;
		for (int i = 0; i < sizes.length; i++) {
			varnum += doms[i].vars.length;
		}
		
		// Initializes the variables of the domains
		int var = 1;
		int round = 0;
		while (var <= varnum) {
			for (int i = 0; i < sizes.length; i++) {
				int[] vars = doms[i].vars;
				if (round < vars.length)
					vars[round] = var++;
			}
			round++;
		}
		
//		zero = new Node(varnum + 1);
//		one = new Node(varnum + 1);
//		nodes[0] = zero;
//		nodes[1] = one;
		
//		zero = new BDD(0);
//		one = new BDD(1);
		nodes[0] = new Node(varnum + 1);
		nodes[1] = new Node(varnum + 1);
		nodenum = 2;
		
		return doms;
	}
	
	public int getNodeNum() {
		return nodenum;
	}
	
	public int varNum() {
		return nodes[0].var - 1;
	}
	
	static int mk(int i, int l, int h) {
		if (l == h) return l;
		
		int u = lookup(i, l, h);
		if (u != -1) return u;
		
		assert(nodenum < maxnodenum);
		u = nodenum++;
		nodes[u] = new Node(i, l, h);
		return u;
	}
	
	private static int lookup(int i, int l, int h) {
		for (int index = 0; index < nodenum; index++) {
			Node node = nodes[index];
			if (node.equals(i, l, h))
				return index;
		}
		return -1;
	}
	
	public BDD ithVar(int var) {
		return new BDD(mk(var, 0, 1));
	}
	
	public BDD nithVar(int var) {
		return new BDD(mk(var, 1, 0));
	}
	
	public BDD zero() {
		return new BDD(0);
//		return zero;
	}
	
	public BDD one() {
		return new BDD(1);
//		return one;
	}
	
	public void printTable(BDD bdd) {
		bdd.print();
		System.out.println();
		for (int i = 0; i < nodenum; i++) {
			System.out.print(i);
			System.out.print(" : ");
			nodes[i].print();
			System.out.println();
		}
	}
	
	public String toString() {
		return toString(nodes, nodenum);
	}
	
	static String toString(Node[] nodes, int nodenum) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < nodenum; i++) {
			out.append(String.format("%d : %s%n", i, nodes[i]));
		}
		return out.toString();
	}
	
	public static class Node {
		
		int var;
		int low;
		int high;
		
		Node(int var) {
			this.var = var;
		}
		
		Node(int var, int low, int high) {
			this.var = var;
			this.low = low;
			this.high = high;
		}
		
		boolean equals(int i, int l, int h) {
			return var == i && low == l && high == h;
		}
		
		Node id() {
			return new Node(var, low, high);
		}
		
		public void print() {
			System.out.print(var);
			System.out.print(" ");
			System.out.print(low);
			System.out.print(" ");
			System.out.print(high);
		}
		
		public String toString() {
			return String.format("%d %d %d", var, low, high);
		}
	}
}
