package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

public class BDDFactory {
	
	static int maxnodenum;
	static Node[] nodes;
	static int nodenum;
	
	static Node zero;
	static Node one;
	
	public BDDFactory(int max) {
		maxnodenum = max;
		nodes = new Node[maxnodenum];
	}

	public static BDDFactory init(String bddpackage, int nodenum, int cachesize) {
		return new BDDFactory(nodenum);
	}
	
	public BDDDomain[] extDomain(long[] sizes) {
		BDDDomain[] doms = new BDDDomain[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			doms[i] = new BDDDomain(sizes[i]);
		}
		
		int varnum = 0;
		for (int i = 0; i < sizes.length; i++) {
			varnum += doms[i].vars.length;
		}
		
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
		
		zero = new Node(varnum + 1);
		one = new Node(varnum + 1);
		nodes[0] = zero;
		nodes[1] = one;
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
	
	public BDD zero() {
		return new BDD(0);
	}
	
	public BDD one() {
		return new BDD(1);
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
		
		public String toString() {
			return String.format("%d %d %d", var, low, high);
		}
	}
}
