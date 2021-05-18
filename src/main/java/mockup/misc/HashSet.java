package mockup.misc;

import mockup.Replaces;
import java.util.Collection;
import java.util.Iterator;

@Replaces({"java.util.HashSet", "java.util.TreeSet"})
public class HashSet<E> {
	private E x;

	public Iterator<E> iterator() {
		Iterator<E> it = new mockup.misc.Iterator<E>(x);
		return it;
	}

	public boolean add(E y) {
		x = y;
		return true;
	}

	public boolean addAll(Collection<? extends E> c) {
		Iterator<? extends E> i = c.iterator();
		x = i.next();
		return true;
	}

	public Object[] toArray() {
		Object[] a = new Object[1];
		a[0] = x;
		return a;
	}

	public E get() {
		return x;
	}

}
