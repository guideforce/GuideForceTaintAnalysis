package mockup.misc;

import mockup.Replaces;

@Replaces("java.util.StringTokenizer")
public class StringTokenizer {
	private String x;
	
	public StringTokenizer(String y) {
		x = y;
	}
	
	public StringTokenizer(String y, String d) {
		x = y;
	}

	public StringTokenizer(String y, String d, boolean b) {
		x = y;
	}
	
	public String nextToken() {
		return x;
	}

	public Object nextElement() {
		return x;
	}

	public boolean hasMoreTokens() {
		return true;
	}
	
	public boolean hasMoreElements() {
		return true;
	}
}
