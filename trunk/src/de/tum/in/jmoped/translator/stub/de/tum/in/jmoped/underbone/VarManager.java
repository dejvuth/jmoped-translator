package de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone;

import java.util.Collection;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDD;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDDomain;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDFactory;

import de.tum.in.jmoped.underbone.Variable;

public class VarManager {
	
	BDDFactory factory;
	
	int spDomIndex;
	int sDomIndex;
	
	BDDDomain[] doms;

	public VarManager(int bits, long[] heapSizes, Collection<Variable> g, 
			int smax, int lvmax, int nodenum) {
		
		int size = 1;
		for (int i = 0; i < bits; i++)
			size *= 2;
		
		long[] domSize = new long[smax + 1];
		
		int index = 0;
		
		// Stack
		if (smax > 0) {
			
			// Stack pointer
			spDomIndex = index;
			domSize[index++] = smax + 1;
			
			// Stack element
			sDomIndex = index;
			for (int i = 0; i < smax; i++) {
				
				domSize[index++] = size;
			}
		} else {
			
			spDomIndex = -1;
			sDomIndex = -1;
		}
		
		factory = new BDDFactory(nodenum);
		doms = factory.extDomain(domSize);
	}
	
	public BDDFactory getFactory() {
		return factory;
	}
	
	public BDD initVars() {
		return doms[spDomIndex].ithVar(0);
	}
	
	public BDDDomain getStackPointerDomain() {
		return doms[spDomIndex];
	}
	
	public BDDDomain getStackDomain(int index) {
		return doms[sDomIndex + index];
	}
	
	public BDD bddRange(BDDDomain dom, int min, int max) {
		if (min == max)
			return dom.ithVar(min);
		
		// Handles manually, because the library seems to be wrong in this case
		if (min == 0 && max == 1)
			return dom.ithVar(0).orWith(dom.ithVar(1));
		
		BDD a = factory.zero();
		for (int i = min; i <= max; i++) {
			a.orWith(dom.ithVar(encode(i, dom)));
		}
		
		return a;
	}
	
	public static long encode(int raw, BDDDomain dom) {
		
		long maxint = dom.size().longValue()/2 - 1;
		if (raw >= 0) 
			return ((long) raw) & maxint;
		
		return dom.size().longValue() + (long) raw;
	}
	
	public static int decode(long encoded, BDDDomain dom) {
		
		long size = dom.size().longValue();
		int maxint = (int) (size/2 - 1);
		if (maxint == 0 || encoded <= maxint) 
			return (int) encoded;
		
		return (int) (encoded - dom.size().longValue());
	}
	
	public static int decode(int encoded, BDDDomain dom) {
		
		int size = dom.size().intValue();
		int maxint = (int) (size/2 - 1);
		if (maxint == 0 || encoded <= maxint) 
			return encoded;
		
		return (int) (encoded - size);
	}
}
