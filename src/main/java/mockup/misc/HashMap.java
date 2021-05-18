package mockup.misc;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import mockup.Replaces;

@Replaces("java.util.HashMap")
public class HashMap<K,V> {
	private K k;
	private V v;
	
	private HashSet<Map.Entry<K, V>> entrySet;
	
	static {
		// java.util.LinkedList seems to implement <clinit>. So we need to implement it as well.
		int z = 1;
	}
	
	public HashMap() {
		entrySet = new HashSet<>();
	}

	public V put(K key, V value) {
		entrySet.add(new Node(key, value));
		k = key;
		v = value;
		return value;
	}

	public V get(Object key) {
		return v;
	}
	
	public boolean containsKey(Object key) {
		return true;
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return entrySet;
	}

	@Replaces("java.util.HashMap$Node")
	static class Node<K,V> implements Map.Entry<K,V> {
		K key; 
		V val;
		
		Node(K key, V val){
			this.key = key;
			this.val = val;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return val;
		}

		@Override
		public V setValue(V value) {
			V oldVal = this.val;
			this.val = value;
			return oldVal;
		}
	}
}
