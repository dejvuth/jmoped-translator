package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import de.tum.in.jmoped.translator.stub.java.math.BigInteger;

public class BDDDomain {

	BigInteger size;
	int[] vars;
	
	BDDDomain(long size) {
		this.size = new BigInteger(size);
		
		int bits;
		if (size <= 2) bits = 1;
		else if (size <= 4) bits = 2;
		else if (size <= 8) bits = 3;
		else if (size <= 16) bits = 4;
		else if (size <= 32) bits = 5;
		else if (size <= 64) bits = 6;
		else if (size <= 128) bits = 7;
		else bits = 8;
		
		vars = new int[bits];
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
	
	public BDDVarSet set() {
		int one = 1;
		for (int i = vars.length - 1; i >= 0; i--) {
			one = BDDFactory.mk(vars[i], 0, one);
		}
		return new BDDVarSet(one);
	}
	
	public BigInteger size() {
		return size;
	}
}
