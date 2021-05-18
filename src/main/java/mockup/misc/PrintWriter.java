package mockup.misc;

import java.util.Collection;
import java.io.IOException;
import java.io.Writer;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

@Replaces("java.io.PrintWriter")
public class PrintWriter extends Writer {

	public PrintWriter(String s) {}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		// TODO Auto-generated method stub

	}
	
	public void println(Object obj) {
		if (obj == null) {
			TaintAPI.outputString("null");
		} else if (obj instanceof String) {
			TaintAPI.outputString((String)obj); 
		}
		else if (obj instanceof Collection<?>) {
			Collection<?> c = (Collection<?>)obj;
			println(c.iterator().next());
		}
	}

	public void println(String str) {
		if (str == null) {
			TaintAPI.outputString("null");
		} else {
			TaintAPI.outputString(str);
		}
	}
}
