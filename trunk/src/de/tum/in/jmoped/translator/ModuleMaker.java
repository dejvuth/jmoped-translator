package de.tum.in.jmoped.translator;

import de.tum.in.jmoped.underbone.Module;

/**
 * The general protocol for classes that create modules.
 * 
 * @author suwimont
 *
 */
public interface ModuleMaker {
	
	/**
	 * Gets the name of the module.
	 * 
	 * @return the name of the module
	 */
	public String getName();

	/**
	 * Makes the module.
	 * 
	 * @return the module
	 */
	public Module make(Translator translator);
	
	/**
	 * Returns <code>true</code> iff the underlying method is synchronized.
	 * 
	 * @return <code>true</code> iff the underlying method is synchronized.
	 */
	public boolean isSynchronized();
}
