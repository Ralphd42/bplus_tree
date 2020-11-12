package bptree;

import bptree.BPlusTree.InvalidDeletionException;

/**
 * The {@code NonLeafNode} class implements non-leaf nodes in a B+-tree.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * @param <K> the type of keys
 * @param <P> the type of pointers
 */
public class NonLeafNode<K extends Comparable<K>, P> extends Node<K, P> {

	/**
	 * An automatically generated serial version UID.
	 */
	private static final long serialVersionUID = -5878186273639744395L;

	/**
	 * Constructs a {@code NonLeafNode}.
	 * 
	 * @param degree the degree of the {@code NonLeafNode}
	 */
	public NonLeafNode(int degree) {
		super(degree);
	}

	/**
	 * Constructs a {@code NonLeafNode} while adding the specified key and pointers.
	 * 
	 * @param degree the degree of the {@code NonLeafNode}
	 * @param n      a pointer to a {@code Node}
	 * @param key    a key
	 * @param nn     a pointer to a {@code Node}
	 */
	public NonLeafNode(int degree, P n, K key, P nn) {
		this(degree);
		pointers[0] = n;
		keys[0] = key;
		pointers[1] = nn;
		keyCount = 1;
	}

	/**
	 * Returns a pointer to the child {@code Node} of this {@code NonLeafNode} that
	 * must be responsible for this specified key.
	 * 
	 * @param k a search key
	 * @return a pointer to the child {@code Node} of this {@code NonLeafNode} that
	 *         must be responsible for this specified key
	 */
	P child(K k) {
		int i = 0;
		for (; i < keyCount; i++) {
			int c = k.compareTo(keys[i]);
			if (c == 0)
				return pointer(i + 1);
			else if (c < 0)
				return pointer(i);
		}
		return pointer(i);
	}

	private int childIndex(K k) {
		int i = 0;
		for (; i < keyCount; i++) {
			int c = k.compareTo(keys[i]);
			if (c == 0)
				return (i + 1);
			else if (c < 0)
				return (i);
		}
		return (i);
	}

	public int leftIndex(K k) {
		int idx = childIndex(k);
		int left = idx - 1;
		if (left < 0) {
			left = idx + 1;
		}
		return left;
	}

	public P leftPointer(K k) {
		int idx = childIndex(k);
		int left = idx - 1;
		if (left < 0) {
			left = idx + 1;
		}
		return pointer(left);
	}

	public K leftKey(K k) {
		int idx = childIndex(k);
		int left = idx - 1;
		if (left < 0) {
			left = idx;
		}
		return key(left);
	}

	public boolean IsleftKey(K k) {
		boolean retval = true;
		int idx = childIndex(k);
		int left = idx - 1;
		if (left < 0) {
			retval = false;
		}
		return retval;
	}

	/**
	 * Inserts the specified key and pointer after the specified pointer.
	 * 
	 * @param key     a key
	 * @param pointer a pointer to a {@code Node}
	 * @param p       a pointer after which the specified key and pointer will be
	 *                inserted
	 */
	public void insertAfter(K key, P pointer, P p) {
		int i = keyCount;
		while (!pointers[i].equals(p)) {
			keys[i] = keys[i - 1];
			pointers[i + 1] = pointers[i];
			i--;
		}
		keys[i] = key;
		pointers[i + 1] = pointer;
		keyCount++;
	}

	/**
	 * Returns the number of children that this {@code NonLeafNode} has.
	 * 
	 * @return the number of children that this {@code NonLeafNode} has
	 */
	public int childCount() {
		return keyCount + 1;
	}

	/**
	 * Copies the specified keys and pointers of the specified {@code NonLeafNode}
	 * into this {@code NonLeafNode}.
	 * 
	 * @param node       a {@code NonLeafNode}
	 * @param beginIndex the beginning index of the keys, inclusive
	 * @param endIndex   the ending index of the pointers, inclusive
	 */
	public void copy(NonLeafNode<K, P> node, int beginIndex, int endIndex) {
		clear();
		super.append(node, beginIndex, endIndex - 1);
		this.pointers[keyCount] = node.pointers[keyCount + beginIndex];
	}

	/**
	 * Inserts a key and pointer at the specified indices.
	 * 
	 * @param k  a key
	 * @param iK the index at which the key is inserted
	 * @param p  a pointer to a {@code Node}
	 * @param iP the index at which the pointer is inserted
	 */
	public void insert(K k, int iK, P p, int iP) {
		for (int i = keyCount; i > iK; i--)
			keys[i] = keys[i - 1];
		keys[iK] = k;
		for (int i = keyCount + 1; i > iP; i--)
			pointers[i] = pointers[i - 1];
		pointers[iP] = p;
		keyCount++;
	}

	/**
	 * Removes the key and pointer at the specified indices.
	 * 
	 * @param iK the index at which the key is deleted
	 * @param iP the index at which the pointer is deleted
	 */
	public void delete(int iK, int iP) {
		for (int i = iK; i < keyCount - 1; i++)
			keys[i] = keys[i + 1];
		for (int i = iP; i < keyCount; i++)
			pointers[i] = pointers[i + 1];
		keys[keyCount - 1] = null;
		pointers[keyCount] = null;
		keyCount--;
	}

	/**
	 * Removes the specified key and a relevant pointer from this
	 * {@code NonLeafNode}.
	 * 
	 * @param key a key
	 * @throws InvalidDeletionException if a key non-existent in this
	 *                                  {@code NonLeafNode} is attempted to be
	 *                                  removed from this {@code NonLeafNode}.
	 */
	@Override
	public void remove(K key) throws InvalidDeletionException {
		int i = 0;
		for (; i < keyCount; i++) {
			if (keys[i].compareTo(key) == 0) {
				for (int j = i; j < keyCount - 1; j++) {
					keys[j] = keys[j + 1];
					pointers[j + 1] = pointers[j + 2];
				}
				break;
			}
		}
		if (i == keyCount)
			throw new InvalidDeletionException("key: " + key);
		keyCount--;
		keys[keyCount] = null;
		pointers[keyCount + 1] = null;
	}

	/**
	 * Changes the key between the specified pointers.
	 * 
	 * @param p a pointer to a {@code Node}
	 * @param n a pointer to a {@code Node}
	 * @param k a key
	 */
	public void changeKey(P p, P n, K k) {
		for (int i = 0; i < keyCount; i++)
			if (pointers[i].equals(p) && pointers[i + 1].equals(n)) {
				keys[i] = k;
				return;
			}
		throw new UnsupportedOperationException("There must be a bug in the code. This case must not happen!");
	}

	/**
	 * Determines whether or not this {@code NonLeafNode} is under-utilized and thus
	 * some action such as merging or redistribution is needed.
	 * 
	 * @return {@code true} if this {@code NonLeafNode} is under-utilized and thus
	 *         some action such as merging or redistribution is needed;
	 *         {@code false} otherwise
	 */
	@Override
	public boolean isUnderUtilized() {
		boolean retval = false;
		int min = (int) Math.ceil(pointers.length / 2.0);
		if (keyCount() < min) {
			retval = true;
		}
		return retval;
	}

	/**
	 * Determines whether or not this {@code NonLeafNode} can be merged with the
	 * specified {@code Node}. Non leafe node is detwermined slightly different
	 * since adding K in the middle
	 * 
	 * @param other another {@code Node}
	 * @return {@code true} if this {@code NonLeafNode} can be merged with the
	 *         specified {@code Node}; {@code false} otherwise
	 */
	@Override
	public boolean mergeable(Node<K, P> other) {
		boolean retval = false;
		if (keyCount() + other.keyCount() <= keys.length + 1) {
			retval = true;
		}
		return retval;
	}

	public void replaceKey(K oldKey, K newKey) {
		int i = 0;
		for (; i < keyCount; i++) {
			if (keys[i].compareTo(oldKey) == 0) {
				keys[i] = newKey;
				return;
			}
		}
	}

	/**
	 * 
	 * @param k Key to append to nonleaf node
	 */
	public void appendKey(K k) {
		keys[keyCount] = k;
		++keyCount;
	}

	public void AppendAll(NonLeafNode<K, P> nlf, K k) {
		/*
		 * public void append(Node<K, P> node, int beginIndex, int endIndex) { for (int
		 * i = 0; i <= endIndex - beginIndex; i++) { this.keys[keyCount] = node.keys[i +
		 * beginIndex]; this.pointers[keyCount] = node.pointers[i + beginIndex];
		 * keyCount++; }
		 */
		int kc = this.keyCount;
		int nlfkc = nlf.keyCount();
		for (int i = kc; i < (kc + nlfkc); i++) {
			keys[i] = nlf.keys[i - kc];
		}

	}

}
