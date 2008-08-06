package de.tum.in.jmoped.translator;

/**
 * Translator error.
 * 
 * @author suwimont
 *
 */
public class TranslatorError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5413470765589301404L;
	
	/**
	 * Constructs a translator error.
	 * 
	 * @param message the error description.
	 * @param args the arguments of the message.
	 */
	public TranslatorError(String message, Object... args) {
		this(String.format(message, args));
	}

	/**
	 * Constructs a translator error.
	 * 
	 * @param message the error description.
	 */
	public TranslatorError(String message) {
		super(message);
	}
}
