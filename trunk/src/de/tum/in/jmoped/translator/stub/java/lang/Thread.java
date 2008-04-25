package de.tum.in.jmoped.translator.stub.java.lang;

public class Thread implements Runnable {
	
	Runnable r;
	
	public Thread() {
		r = this;
	}
	
	public Thread(Runnable r) {
		this.r = r;
	}
	
	public void start() {
		r.run();
	}
	
	public void run() {
	}
}
