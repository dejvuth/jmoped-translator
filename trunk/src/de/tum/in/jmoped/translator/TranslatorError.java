package de.tum.in.jmoped.translator;

public class TranslatorError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5413470765589301404L;
	
	public TranslatorError(String message, Object... args) {
		this(String.format(message, args));
	}

	public TranslatorError(String message) {
		super(message);
	}
}
