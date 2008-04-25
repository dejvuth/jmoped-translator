package de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDD;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDDomain;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDVarSet;
import de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone.VarManager;

public class BDDSemiring {
	
	VarManager manager;
	public BDD bdd;
	
	public BDDSemiring(VarManager manager, BDD bdd) {
		
		this.manager = manager;
		this.bdd = bdd;
	}

	public BDDSemiring extend(ExprSemiring expr) {
		
		if (expr.type == ExprSemiring.PUSH) {
			return push(expr);
		}
		
		return null;
	}
	
	private BDDSemiring push(ExprSemiring expr) {
		
		// Gets the current value of stack pointer (sp)
		BDDDomain spDom = manager.getStackPointerDomain();
//		System.out.println(spDom);
		int sp = bdd.scanVar(spDom).intValue();
		BDDDomain s0dom = manager.getStackDomain(sp);
//		System.out.println(s0dom);
		
		// Abstracts the sp and the stack element at sp
		BDD cbdd = abstractVars(bdd, spDom, s0dom);
		
		// Updates the sp and the stack element at sp
		cbdd.andWith(spDom.ithVar(sp+1));
		if (expr.value != null) {	// null means ignored
			
			if (expr.aux == null) {
				int raw = ((Integer) expr.value).intValue();
				long value = VarManager.encode(raw, s0dom);
				cbdd.andWith(s0dom.ithVar(value));
			} else {	// aux not null defines a range
				int from = (Integer) expr.aux;
				int to = (Integer) expr.value;
				cbdd.andWith(manager.bddRange(s0dom, from, to));
			}	
		}
		return new BDDSemiring(manager, cbdd);
	}
	
	/**
	 * Abstract the variables specified by doms from bdd.
	 * 
	 * @param bdd
	 * @param doms
	 * @return
	 */
	private static BDD abstractVars(BDD bdd, BDDDomain... doms) {
		
		if (doms.length == 0) return bdd.id();
		
		BDDVarSet abs = doms[0].set();
		for (int i = 1; i < doms.length; i++) {
			abs.unionWith(doms[i].set());
		}
		BDD out = bdd.exist(abs);
		abs.free();
		
		return out;
	}
}
