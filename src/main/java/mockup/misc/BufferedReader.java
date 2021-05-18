package mockup.misc;

import mockup.Replaces;

import java.io.Reader;

@Replaces("java.io.BufferedReader")
public class BufferedReader{

	public String s;

		public BufferedReader(Reader in) {
			s = ((InputStreamReader) in).s;
		}
		
		public String readLine() {
			return s;
		}
}
