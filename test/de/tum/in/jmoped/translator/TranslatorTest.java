package de.tum.in.jmoped.translator;

import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.MethodInfo;
import org.junit.Test;


public class TranslatorTest {

	@Test public void testAbstractList() throws Exception {
		ClassFile cf = TranslatorUtils.findClassFile("java/util/AbstractList", null);
		MethodInfo[] methods = cf.getMethods();
		for (int i = 0; i < methods.length; i++) {
			System.out.println(methods[i].getName());
		}
	}
}
