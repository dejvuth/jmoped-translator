package de.tum.in.jmoped.translator.stub.java.util.logging;

public class Logger {

	public Logger(String name, String resourceBundleName) {
		
	}
	
	public static Logger getLogger(String name) {
		return new Logger(name, null);
	}
}
