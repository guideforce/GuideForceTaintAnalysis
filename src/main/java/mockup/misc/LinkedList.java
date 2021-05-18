package mockup.misc;

import mockup.Replaces;

import java.util.Collection;

@Replaces({"java.util.LinkedList", "java.util.ArrayList", "java.util.AbstractSequentialList", "java.util.AbstractCollection"})
public class LinkedList<E> {
	private E x;
	
	static {
		// java.util.LinkedList implements <clinit>. So we need to implement it as well.
		int z = 1;
	}
	
	public LinkedList() {
	}

	public LinkedList(E e) {
		x = e;
	}
	
	public boolean add(E y) {
		x = y;
		return true;
	}

	public E get(int index) {
		return x;
	}
	
	public Iterator<E> iterator() {
		Iterator<E> it = new Iterator<E>(x);
		return it;
	}
	
	void addFirst(E e) {
		x = e;
	}
	
	void addLast(E e) {
		x = e;
	}
	
	E getLast() {
		return x;
	}

	public boolean addAll(Collection<? extends E> c) {
		java.util.Iterator<? extends E> i = c.iterator();
		x = i.next();
		return true;
	}
	
	@Override
	public String toString() {
		if (x instanceof String)
			return (String)x;
		else
			return "";
	}
	
	// NOTE: actually only present in java.util.ArrayList, 
	// but since we use this class also for that class, 
	// we implement this method here; 
	// to be moved to ourlib.ArrayList, in case we add such a class
	public boolean retainAll(Collection<? extends E> c) {
		// nothing changes, because we don't copy elements from c 
		return true;
	}

	public Object[] toArray() {
		Object[] a = new Object[1];
		a[0] = x;
		return a;
	}
	
	
}

