package de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone;

import java.util.Collection;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDD;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDDomain;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDFactory;
import de.tum.in.jmoped.underbone.Variable;

public class VarManager {
	
	// BDDFactory
	private BDDFactory factory;
	
	// Starting domain index of locals
	private int l0;
	
	// Stack pointer domain index
	private int spDomIndex;
	
	// Stack domain index
	private int sDomIndex;
	
	// Local variable domain index
	private int lvDomIndex;
	
	BDDDomain[] doms;
	
	private static final int varcopy = 3;
	private static final int globalcopy = 3;
	

	public VarManager(String bddpackage, int nodenum, int cachesize, 
			int bits, long[] heapSizes, Collection<Variable> g, 
			int smax, int lvmax, int tbound, boolean lazy) {
		
		int size = 1 << bits;
//		for (int i = 0; i < bits; i++)
//			size *= 2;
		
		// Prepares array for domains
		int s = smax + varcopy*lvmax;
		if (smax > 0) s++;	// for stack pointer
		int heapLength = (heapSizes == null) ? 0 : heapSizes.length;
		if (heapLength > 1) s += globalcopy*(heapLength + 1);
		if (g != null && !g.isEmpty()) {
			s += globalcopy*g.size();
		}
		s++;	// ret var
		long[] domSize = new long[s];
		
		int index = 0;
		
		// Local vars
		l0 = index;
		if (lvmax > 0) {
			
			lvDomIndex = index;
			for (int i = 0; i < lvmax; i++) {
				for (int j = 0; j < varcopy; j++)
					domSize[index++] = size;
			}
		} else {
			lvDomIndex = -1;
		}
		
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
		
		factory = BDDFactory.init(null, nodenum, 0);
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
	
	/**
	 * Gets the <code>BDDDomain</code> of the stack element at <code>index</code>.
	 * 
	 * @param index the stack element index.
	 * @return the <code>BDDDomain</code> of the stack element.
	 */
	public BDDDomain getStackDomain(int index) {
		return doms[sDomIndex + index];
	}
	
	/**
	 * Gets the <code>BDDDomain</code> of the local variable at <code>index</code>.
	 * 
	 * @param index the stack element index.
	 * @return the <code>BDDDomain</code> of the local variable.
	 */
	public BDDDomain getLocalVarDomain(int index) {
		return doms[lvDomIndex + varcopy*index];
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
