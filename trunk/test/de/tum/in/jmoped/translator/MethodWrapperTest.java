package de.tum.in.jmoped.translator;

import java.util.Arrays;

import org.junit.Test;

import de.tum.in.jmoped.translator.MethodWrapper.Range;


public class MethodWrapperTest {

	@Test public void wrapArray() {
		Range length = new Range(new Integer[] { 0, 3 });
		Range elem = new Range(2);
		
		// Creates array for all lengths
		for (int i = length.min; i < length.max; i++) {
			int[] a = new int[i];
			
			// Elements initially get min values
			for (int j = 0; j < a.length; j++)
				a[j] = elem.min;
			
			// Updates element values
			label: while (true) {
				System.out.println(Arrays.toString(a));
				if (a.length == 0) break;
				for (int j = a.length - 1; j >= 0; j--) {
					if (a[j] != elem.max) {
						a[j]++;
						break;
					} else {
						if (j == 0) break label;
						a[j] = elem.min;
					}
				}
			}
		}
	}
}
