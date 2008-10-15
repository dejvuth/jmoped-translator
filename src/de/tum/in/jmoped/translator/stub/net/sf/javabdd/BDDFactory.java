package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

public class BDDFactory {
	
	static int maxnodenum;
	static Node[] nodes;
	static int nodenum;
	
	static int[] H;
	
	static BDDFactory factory;
	
	private BDDFactory(int max) {
		maxnodenum = max;
		nodes = new Node[max];
		H = new int[max];
	}

	public static BDDFactory init(String bddpackage, int nodenum, int cachesize) {
		factory = new BDDFactory(nodenum);
		return factory;
	}
	
	public BDDDomain[] extDomain(long[] sizes) {
		// Construct BDD domains
		BDDDomain[] doms = new BDDDomain[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			doms[i] = new BDDDomain(i, sizes[i]);
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
		u = nodenum;
		nodenum++;
		
		nodes[u] = new Node(i, l, h);
		int hash = hash(i, l, h);
		while (H[hash] != 0)
			hash = (hash + 1) % maxnodenum;
		H[hash] = u;
		return u;
	}
	
	private static int lookup(int i, int l, int h) {
//		System.out.printf("lookup: %d %d %d%n", i, l, h);
		int hash = hash(i, l, h);
		int u = H[hash];
		if (u == 0)
			return -1;
		
		while (u != 0 && !nodes[u].equals(i, l, h)) {
			hash = (hash + 1) % maxnodenum;
			u = H[hash];
		}
		
		if (u == 0)
			return -1;
		return u;
		
//		for (int index = nodenum - 1; index >= 2; index--) {
//			Node node = nodes[index];
//			if (node.equals(i, l, h))
//				return index;
//		}
//		return -1;
	}
	
	private static int hash(int i, int v0, int v1) {
		return pair(i, pair(v0, v1)) % maxnodenum;
	}
	
	private static int pair(int i, int j) {
		return ((i + j) * (i + j + 1))/2 + i;
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
	
	public BDDPairing makePair() {
		return new BDDPairing();
	}
	
	public BDDPairing makePair(BDDDomain dom1, BDDDomain dom2) {
		BDDPairing pairing = new BDDPairing();
		pairing.set(dom1, dom2);
		return pairing;
	}
	
	public BDDVarSet emptySet() {
		return new BDDVarSet(1);
	}
	
	public BDDVarSet makeSet(BDDDomain[] doms) {
		if (doms.length == 0)
			return emptySet();
		int u = BDDDomain.set(doms[0]);
		for (int i = 1; i < doms.length; i++) {
			u = BDDVarSet.unionWith(u, BDDDomain.set(doms[i]));
		}
		return new BDDVarSet(u);
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
