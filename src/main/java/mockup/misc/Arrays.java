package mockup.misc;

import java.util.LinkedList;
import java.util.Collection;
import mockup.Replaces;

@Replaces("java.util.Arrays")
public class Arrays {
	static {
		// java.util.Arrays seems to implement <clinit>. So we need to implement it as well.
		int z = 1;
	}
	
	static <T> Collection<T> asList(T[] a) {
		LinkedList<T> l = new LinkedList<T>();
		l.add(a[0]);
		return l;
	}
}
