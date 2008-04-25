package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDFactory.Node;

public class BDDVarSet {
	
	int u;
	
	public BDDVarSet(int u) {
		this.u = u;
	}

	public BDDVarSet unionWith(BDDVarSet that) {
		
		Node node = lastNode();
		
		if (node.high == 1)
			node.high = that.u;
		else
			node.low = that.u;
		
		return this;
	}
	
	public int getNode() {
		return u;
	}
	
	public void free() {
		
	}
	
	static int next(int index) {
		
		Node node = BDDFactory.nodes[index];
		if (node.low == 0) return node.high;
		return node.low;
	}
	
	Node lastNode() {
		
		Node[] nodes = BDDFactory.nodes;
		int current;
		Node node;
		int next = u;
		do {
			current = next;
			node = nodes[current];
			if (node.low == 0) next = node.high;
			else next = node.low;
		} while (next != 1);
		
		return node;
	}
	
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append(String.format("%d%n", u));
		out.append(BDDFactory.toString(BDDFactory.nodes, BDDFactory.nodenum));
		return out.toString();
	}
}
