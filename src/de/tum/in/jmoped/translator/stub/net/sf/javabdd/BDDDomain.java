package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import de.tum.in.jmoped.translator.stub.java.math.BigInteger;

public class BDDDomain {

	int index;
	BigInteger size;
	int[] vars;
	
	BDDDomain(int index, long size) {
		this.index = index;
		this.size = BigInteger.valueOf(size);
		
		int bits;
		if (size <= 2) bits = 1;
		else if (size <= 4) bits = 2;
		else if (size <= 8) bits = 3;
		else if (size <= 16) bits = 4;
		else if (size <= 32) bits = 5;
		else if (size <= 64) bits = 6;
		else if (size <= 128) bits = 7;
		else if (size <= 256) bits = 8;
		else if (size <= 512) bits = 9;
		else bits = 10;
		
		vars = new int[bits];
	}
	
	public int getIndex() {
		return index;
	}
	
	static int buildEquals(BDDDomain dom1, BDDDomain dom2) {
		int[] vars1 = dom1.vars;
		int[] vars2 = dom2.vars;
		int one = 1;
		for (int i = vars1.length - 1; i >= 0; i--) {
			// Finds the greater var
			int greater = vars2[i];
			int less = vars1[i];
			if (greater < less) {
				int tmp = greater;
				greater = less;
				less = tmp;
			}
			
			// Puts the greater var closer to the node one
			int left = BDDFactory.mk(greater, one, 0);
			int right = BDDFactory.mk(greater, 0, one);
			
			// Puts the less var
			one = BDDFactory.mk(less, left, right);
		}
		return one;
	}
	
	public BDD buildEquals(BDDDomain that) {
		return new BDD(buildEquals(this, that));
//		int[] thatvars = that.vars;
//		int one = 1;
//		for (int i = vars.length - 1; i >= 0; i--) {
//			// Finds the greater var
//			int greater = thatvars[i];
//			int less = vars[i];
//			if (greater < less) {
//				int tmp = greater;
//				greater = less;
//				less = tmp;
//			}
//			
//			// Puts the greater var closer to the node one
//			int left = BDDFactory.mk(greater, one, 0);
//			int right = BDDFactory.mk(greater, 0, one);
//			
//			// Puts the less var
//			one = BDDFactory.mk(less, left, right);
//		}
//		return new BDD(one);
	}
	
	public BDDFactory getFactory() {
		return BDDFactory.factory;
	}
	
	public int[] vars() {
		return vars;
	}
	
	public int varNum() {
		return vars.length;
	}
	
	public BDD ithVar(long val) {
		long now = val;
		int zero = 0, one = 1;
		for (int i = vars.length - 1; i >= 0; i--) {
			// Mods
			int mod = (int) (now % 2);
			
			// Creates new node depending on the result of mod
			if (mod == 1) one = BDDFactory.mk(vars[i], zero, one);
			else one = BDDFactory.mk(vars[i], one, zero);
			
			// Updates now
			now /= 2;
		}
		return new BDD(one);
	}
	
	static int set(BDDDomain dom) {
		int[] vars = dom.vars;
		int one = 1;
		for (int i = vars.length - 1; i >= 0; i--) {
			one = BDDFactory.mk(vars[i], 0, one);
		}
		return one;
	}
	
	public BDDVarSet set() {
		return new BDDVarSet(set(this));
//		int one = 1;
//		for (int i = vars.length - 1; i >= 0; i--) {
//			one = BDDFactory.mk(vars[i], 0, one);
//		}
//		return new BDDVarSet(one);
	}
	
	public BigInteger size() {
		return size;
	}
}
