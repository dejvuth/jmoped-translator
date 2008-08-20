package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import org.junit.Assert;
import org.junit.Test;



public class BDDDomainTest {

	@Test public void testBuildEquals() {
		BDDFactory factory = BDDFactory.init(null, 35, 0);
		BDDDomain[] doms = factory.extDomain(new long[] {8, 8, 8, 2, 8, 8});
		BDD bdd = factory.one()
			.andWith(doms[3].ithVar(0))
			.andWith(doms[0].ithVar(1));
		
		BDD eq = doms[0].buildEquals(doms[4]);
		
		bdd.andWith(eq);
		Assert.assertEquals(1, bdd.scanVar(doms[4]).intValue());
	}
}
