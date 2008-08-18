package de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone;

import java.util.Set;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDD;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDDomain;
import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDDVarSet;
import de.tum.in.jmoped.translator.stub.de.tum.in.jmoped.underbone.VarManager;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.expr.Value;
import de.tum.in.wpds.CancelMonitor;
import de.tum.in.wpds.Sat;
import de.tum.in.wpds.Semiring;

public class BDDSemiring implements Semiring {
	
	VarManager manager;
	public BDD bdd;
	
	public BDDSemiring(VarManager manager, BDD bdd) {
		
		this.manager = manager;
		this.bdd = bdd;
	}

	public Semiring extend(Semiring a, CancelMonitor monitor) {
		
		ExprSemiring A = (ExprSemiring) a;
		if (A.type == de.tum.in.jmoped.underbone.ExprType.PUSH) {
			return push(A);
		}
		
		return null;
	}
	
	private BDDSemiring push(ExprSemiring A) {
		
		// Gets the current value of stack pointer
		BDDDomain spdom = manager.getStackPointerDomain();
		int sp = bdd.scanVar(spdom).intValue();
		BDDDomain s0dom = manager.getStackDomain(sp);
		
		// Abstracts the stack
		Value value = (Value) A.value;
		int category = value.getCategory().intValue();
		BDDDomain[] sdoms = new BDDDomain[category + 1];
		sdoms[0] = spdom;
		sdoms[1] = s0dom;
		if (category == 2) 
			sdoms[2] = manager.getStackDomain(sp + 1);
		BDD c = abstractVars(bdd, sdoms);
		
		// Updates the stack
		c.andWith(spdom.ithVar(sp + category));
		c.andWith(bddOf(value, s0dom));
		if (category == 2)
			c.andWith(sdoms[2].ithVar(0));
		return new BDDSemiring(manager, c);
	}
	
	/**
	 * Returns the BDD with variables specified by <code>dom</code>
	 * representing <code>value</code>.
	 * 
	 * @param value the value.
	 * @param dom the BDD domain.
	 * @return the BDD representing the value.
	 */
	private BDD bddOf(Value value, BDDDomain dom) {
		
		// All values
		if (value.all()) {
			return manager.getFactory().one();
		} 
		
		// Deterministic values
		if (value.deterministic()) {
			if (value.isInteger()) {
				return dom.ithVar(VarManager.encode(value.intValue(), dom));
			} 
//			else if (value.isReal()) {
//				return dom.ithVar(manager.encode(value.floatValue(), dom));
//			} else {	// value.isString();
//				return dom.ithVar(manager.encode(value.stringValue(), dom));
//			}
		} 
		
		return manager.bddRange(dom, value.intValue(), value.to.intValue());
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

	public Semiring andWith(Semiring arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring combine(Semiring arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring extendDynamic(Semiring arg0, CancelMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring extendPop(Semiring arg0, CancelMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring extendPush(Semiring arg0, CancelMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public void free() {
		// TODO Auto-generated method stub
		
	}

	public Semiring getEqClass(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring getEqRel(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring getGlobal() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Semiring> getGlobals() {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring id() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isZero() {
		// TODO Auto-generated method stub
		return false;
	}

	public Semiring lift(Semiring arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring orWith(Semiring arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Semiring restrict(Semiring arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void sliceWith(Semiring arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public String toRawString() {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateGlobal(Semiring arg0) {
		// TODO Auto-generated method stub
		
	}
}
