package de.tum.in.jmoped.translator;

import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("unused")
public class ArrayTest {
	
	class Test0 {
		Object[] a = new Object[3];
		byte[][] y = new byte[3][4];
		void test() {
			Object[][] b = new Object[3][2];
			int[] x = new int[2];
			Vector[][][] z = new Vector[6][7][8];
		}
	}
	
	/**
	 * Tests whether array types are included properly.
	 * 
	 * @throws Exception
	 */
	@Test public void test0() throws Exception {
		Translator translator = new Translator(
				"de/tum/in/jmoped/translator/ArrayTest$Test0",
				new String[] { "bin" },
				"test",
				"()V");
		Assert.assertNotNull(translator.getClassTranslator("[Ljava/lang/Object;"));
		Assert.assertNotNull(translator.getClassTranslator("[[Ljava/lang/Object;"));
		Assert.assertNotNull(translator.getClassTranslator("[I"));
		Assert.assertNotNull(translator.getClassTranslator("[[B"));
		Assert.assertNotNull(translator.getClassTranslator("[[[Ljava/util/Vector;"));
		
		Assert.assertNull(translator.getClassTranslator("[[I"));
		Assert.assertNull(translator.getClassTranslator("[Z"));
	}
	
	@Test public void test1() {
		Object[][] x = new Object[2][3];
		Assert.assertTrue(x.getClass() == Object[][].class);
		Assert.assertTrue(x[0].getClass() == Object[].class);
		
		Integer[][] y = new Integer[2][3];
		Object[] z = (Object[]) y[1];
		Assert.assertTrue(z.getClass() == Integer[].class);
		Assert.assertFalse(z.getClass() == Object[].class);
		
		Object[] a = new Object[2];
		a = (Integer[]) a;
	}
}
