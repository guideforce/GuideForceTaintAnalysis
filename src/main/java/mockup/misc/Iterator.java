package mockup.misc;

import mockup.Replaces;

@Replaces("java.util.Iterator")
public class Iterator<E> implements java.util.Iterator {
	private E x;
	public Iterator(E e) {
		x = e;
	}
	public E next() {
		return x;
	}

	public boolean hasNext() {
		return false;
	}

	public void remove() {
	}
}
