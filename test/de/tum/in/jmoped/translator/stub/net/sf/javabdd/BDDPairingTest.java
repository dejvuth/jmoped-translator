package de.tum.in.jmoped.translator.stub.net.sf.javabdd;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;


public class BDDPairingTest {

	@Test public void test0() {
		BDDFactory factory = BDDFactory.init(null, 40, 0);
		BDDDomain[] doms = factory.extDomain(new long[] {8, 8, 8, 2, 8, 8});
		BDD bdd = factory.one()
			.andWith(doms[4].ithVar(0))
			.andWith(doms[0].ithVar(1));
		System.out.println(Arrays.toString(doms[0].vars()));
		System.out.println(bdd);
		
		System.out.println(Arrays.toString(doms[1].vars()));
		BDDPairing pairing = factory.makePair(doms[0], doms[1]);
		System.out.println(pairing);
		bdd.replaceWith(pairing);
		System.out.println(bdd);
		
		Assert.assertEquals(0, bdd.scanVar(doms[0]));
		Assert.assertEquals(1, bdd.scanVar(doms[1]));
	}
}
