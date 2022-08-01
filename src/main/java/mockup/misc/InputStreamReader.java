package mockup.misc;

import mockup.Replaces;
import mockup.servlet.DummyServletInputStream;

import java.io.Reader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@Replaces("java.io.InputStreamReader")
abstract class InputStreamReader extends Reader {
	public String s;
	public InputStreamReader(InputStream in){
		s = ((DummyServletInputStream) in).s;
	}

	public InputStreamReader(InputStream in, String charsetName)
			throws UnsupportedEncodingException {
		s = ((DummyServletInputStream) in).s;
	}
}
