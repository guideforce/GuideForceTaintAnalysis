package mockup.misc;

import mockup.Replaces;

@Replaces("java.lang.StringBuilder")
public class StringBuilder {
	private String x;

	public StringBuilder(String y) {
		x = y;
	}

	public StringBuilder append(String y) {
		x = y;
		return this;
	}
	
	public StringBuilder append(int y) {
		return this;
	}
	
	public StringBuilder append(char y) {
		return this;
	}

	public String toString() {
		return x;
	}
}
