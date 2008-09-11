package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import org.junit.Test;

import de.tum.in.jmoped.translator.stub.net.sf.javabdd.BDD.BDDIterator;


public class BDDTest {

	@Test public void testIterator() {
		BDDFactory factory = BDDFactory.init(null, 100, 0);
		BDDDomain[] doms = factory.extDomain(new long[] {8, 8, 8, 2, 8, 8});
		BDD init = factory.one();
		
		BDDIterator itr = init.iterator(doms[1].set());
		while (itr.hasNext()) {
			BDD bdd = itr.nextBDD();
			System.out.println(bdd.scanVar(doms[1]));
		}
		System.out.printf("factory.getNodeNum():%d%n", factory.getNodeNum());
		
		init = doms[2].ithVar(3).orWith(doms[2].ithVar(5));
		itr = init.iterator(doms[2].set());
		while (itr.hasNext()) {
			BDD bdd = itr.nextBDD();
			System.out.println(bdd.scanVar(doms[2]));
		}
		System.out.printf("factory.getNodeNum():%d%n", factory.getNodeNum());
	}
}
