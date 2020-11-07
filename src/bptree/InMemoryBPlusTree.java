package bptree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code InMemoryBPlusTree} class implements B+-trees.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 * @param <K> the type of keys
 * @param <P> the type of pointers
 */
public class InMemoryBPlusTree<K extends Comparable<K>, P> extends BPlusTree<K, P> {

	/**
	 * The root {@code Node} of this {@code InMemoryBPlusTree}.
	 */
	Node<K, ?> root;

	/**
	 * Constructs a {@code InMemoryBPlusTree}.
	 * 
	 * @param degree the maximum number of pointers that each {@code Node} of this
	 *               {@code InMemoryBPlusTree} can have
	 */
	public InMemoryBPlusTree(int degree) {
		super(degree);
	}

	/**
	 * Returns the root {@code Node} of this {@code InMemoryBPlusTree}.
	 * 
	 * @return the root {@code Node} of this {@code InMemoryBPlusTree}; {@code null}
	 *         if this {@code InMemoryBPlusTree} is empty
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public Node<K, ?> root() throws IOException {
		return root;
	}

	/**
	 * Returns the specified child {@code Node} of the specified
	 * {@code NonLeafNode}.
	 * 
	 * @param node a {@code NonLeafNode}
	 * @param i    the index of the child {@code Node}
	 * @return the specified child {@code Node} of the specified {@code NonLeafNode}
	 * @throws IOException if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Node<K, ?> child(NonLeafNode<K, ?> node, int i) throws IOException {
		return (Node<K, ?>) node.pointer(i);
	}

	/**
	 * Inserts the specified key and pointer into this {@code InMemoryBPlusTree}.
	 * 
	 * @param k the key to insert
	 * @param p the pointer to insert
	 * @throws InvalidInsertionException if a key already existent in this
	 *                                   {@code InMemoryBPlusTree} is attempted to
	 *                                   be inserted again in the
	 *                                   {@code InMemoryBPlusTree}
	 * @throws IOException               if an I/O error occurs
	 */
	@Override
	public void insert(K k, P p) throws InvalidInsertionException, IOException {
		if (root == null) {// if the tree is empty
			LeafNode<K, P> l = new LeafNode<K, P>(degree); // create an empty root node
			l.insert(k, p); // insert the specified key and pointer into leaf node l
			setRoot(l); // save node l as the new root
		} else { // if the tree is not empty
			HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent = new HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>>();
			// to remember the parent of each visited node
			LeafNode<K, P> l = find(k, root, node2parent); // find leaf node l that should contain the specified key
			if (l.contains(k)) // no duplicate keys are allowed in the tree
				throw new InvalidInsertionException("key: " + k);
			if (!l.isFull()) { // if leaf node l has room for the specified key
				l.insert(k, p); // insert the specified key and pointer into leaf node l
				save(l); // save node l
			} else { // if leaf node l is full and thus needs to be split
				LeafNode<K, P> t = new LeafNode<K, P>(degree + 1); // create a temporary leaf node t
				t.append(l, 0, degree - 2); // copy everything to temporary node t
				t.insert(k, p); // insert the key and pointer into temporary node t
				LeafNode<K, P> lp = new LeafNode<K, P>(degree); // create a new leaf node lp
				lp.setSuccessor(l.successor()); // chaining from lp to the next leaf node
				l.clear(); // clear leaf node l
				int m = (int) Math.ceil(degree / 2.0); // compute the split point
				l.append(t, 0, m - 1); // copy the first half to leaf node l
				lp.append(t, m, degree - 1); // copy the second half to leaf node lp
				save(lp); // save node lp
				l.setSuccessor(lp); // chaining from leaf node l to leaf node lp
				save(l); // save node l
				insertInParent(l, lp.key(0), lp, node2parent); // use lp's first key as the separating key
			}
		}
	}

	/**
	 * Finds the {@code LeafNode} that is a descendant of the specified {@code Node}
	 * and must be responsible for the specified key.
	 * 
	 * @param k           a search key
	 * @param n           a {@code Node}
	 * @param node2parent a {@code Map} to remember, for each visited {@code Node},
	 *                    the parent of that {@code Node}
	 * @return the {@code LeafNode} which is a descendant of the specified
	 *         {@code Node} and must be responsible for the specified key
	 * @throws IOException if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	protected LeafNode<K, P> find(K k, Node<K, ?> n, Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent)
			throws IOException {
		if (n instanceof LeafNode)
			return (LeafNode<K, P>) n;
		else {
			Node<K, ?> c = ((NonLeafNode<K, Node<K, ?>>) n).child(k);
			node2parent.put(c, (NonLeafNode<K, Node<K, ?>>) n);
			return find(k, c, node2parent);
		}
	}

	/**
	 * Inserts the specified key into the parent {@code Node} of the specified
	 * {@code Nodes}.
	 * 
	 * @param n           a {@code Node}
	 * @param k           the key between the {@code Node}s
	 * @param np          a {@code Node}
	 * @param node2parent a {@code Map} remembering, for each visited {@code Node},
	 *                    the parent of that {@code Node}
	 * @throws IOException if an I/O error occurs
	 */
	protected void insertInParent(Node<K, ?> n, K k, Node<K, ?> np,
			Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent) throws IOException {
		if (n.equals(root)) { // if n is the root of the tree
			NonLeafNode<K, Node<K, ?>> r = new NonLeafNode<K, Node<K, ?>>(degree, n, k, np);
			setRoot(r); // a new root node r containing n, k, np and save it
			return;
		}
		NonLeafNode<K, Node<K, ?>> p = node2parent.get(n); // find the parent p of n
		if (!p.isFull()) { // if parent node p has room for a new entry
			p.insertAfter(k, np, n); // insert k and np right after n
			save(p); // save node p
		} else { // if p is full and thus needs to be split
			NonLeafNode<K, Node<K, ?>> t = new NonLeafNode<K, Node<K, ?>>(degree + 1); // crate a temporary node
			t.copy(p, 0, p.keyCount()); // copy everything of p to the temporary node
			t.insertAfter(k, np, n); // insert k and np after n
			p.clear(); // clear p
			NonLeafNode<K, Node<K, ?>> pp = new NonLeafNode<K, Node<K, ?>>(degree); // create a new node pp
			int m = (int) Math.ceil((degree + 1) / 2.0); // compute the split point
			p.copy(t, 0, m - 1); // copy the first half to parent node p
			pp.copy(t, m, degree); // copy the second half to new node pp
			save(pp); // save node pp
			save(p); // save node p
			insertInParent(p, t.key(m - 1), pp, node2parent); // use the middle key as the separating key
		}
	}

	/**
	 * Saves the specified {@code Node} as the new root {@code Node}.
	 * 
	 * @param n a {@code Node}
	 * @throws IOException if an I/O error occurs
	 */
	protected void setRoot(Node<K, ?> n) throws IOException {
		this.root = n;
	}

	/**
	 * Saves the specified {@code Node}.
	 * 
	 * @param n a {@code Node}
	 * @throws IOException if an I/O error occurs
	 */
	protected void save(Node<K, ?> n) throws IOException {
	}

	/**
	 * Removes the specified {@code Node}.
	 * 
	 * @param n the {@code Node} to remove
	 * @throws IOException if an I/O error occurs
	 */
	protected void delete(Node<K, ?> n) throws IOException {
	}

	/**
	 * Removes the specified key and the corresponding pointer from this
	 * {@code InMemoryBPlusTree}.
	 * 
	 * @param k the key to delete
	 * @throws InvalidDeletionException if a key non-existent in a
	 *                                  {@code InMemoryBPlusTree} is attempted to be
	 *                                  deleted from the {@code InMemoryBPlusTree}
	 * @throws IOException              if an I/O error occurs
	 */
	@Override
	public void delete(K k) throws InvalidDeletionException, IOException {
		// please implement the body of this method
		HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent = new HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>>();
		Node<K, ?> l = find(k, root, node2parent);
		delete_entry(l, k, node2parent);
		/**
		 * if(!((LeafNode<K, ?>)l).contains(k))// check for existence of key {
		 * l.remove(k); // remove key }else { throw new InvalidDeletionException("Key
		 * doesn't exist");// throw exception. }
		 */
		//
		// this completes trivial case
		// test for underutilized leaf

	}

	@SuppressWarnings("unchecked")
	protected void delete_entry(Node<K, ?> N, K k, Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent)
			throws InvalidDeletionException, IOException {

		/*
		 * if( N instanceof LeafNode) // check if N is a leaf node { LeafNode<K,P> lnN =
		 * (LeafNode<K,P>)N; if(!(lnN.contains(k)))// check for existence of key {
		 * lnN.remove(k); // remove the key } // this completes trivial deletion at leaf
		 * nodes }
		 */
		N.remove(k);// this completes trivial deletion at leaf nodes

		if (N.equals(root)) { // Handle the root case
			if ((N instanceof NonLeafNode) && ((NonLeafNode<K, ?>) N).childCount() == 1) {
				setRoot(child(((NonLeafNode<K, ?>) N), 0));
			} else {
				save(N);
			}
		} else if (N.isUnderUtilized()) {
			NonLeafNode<K, ?> P = (NonLeafNode<K, ?>) node2parent.get(N);// get parent
			Node<K, P> NPrime = (Node<K, P>) P.leftPointer(k); // get left node. If left isn't available get right node
			K KPrime = P.leftKey(k);// get key between the nodes
			// check for merge able
			if (((Node<K, P>) N).mergeable((Node<K, P>) NPrime)) {// nodes can be merged
				if (P.IsleftKey(k)) {
					merge(NPrime, KPrime, (Node<K, P>) N, node2parent);

				} else {
					merge((Node<K, P>) N, KPrime, NPrime, node2parent);
				}
			} else {// nodes must be redistributed
				if (P.IsleftKey(k)) {// NPrime is predecessor of N
					if (N instanceof NonLeafNode) {// non leaf node case
						int m = ((NonLeafNode<K, ?>) NPrime).childCount() - 1; // index of last pointer in NPrime
						((NonLeafNode<K, P>) N).insert(KPrime, 0, (P) ((NonLeafNode<K, ?>) NPrime).pointer(m), 0);
						K toremove = ((NonLeafNode<K, P>) NPrime).key(m - 1);
						((NonLeafNode<K, P>) NPrime).delete(m - 1, m);
						P.replaceKey(KPrime, toremove);
					} else {// leaf node case
						int m = NPrime.keyCount() - 1; // index of last pointer/key pair
						LeafNode<K, P> nl = (LeafNode<K, P>) N;
						LeafNode<K, P> nPl = (LeafNode<K, P>) NPrime;
						nl.insert(0, nPl.key(m), nPl.pointer(m));
						nPl.remove(nPl.key(m));

					}

				} else {// N is predecessor of NPrime
					// NPrime is predecessor of N 
					if (NPrime instanceof NonLeafNode) {// non leaf node case
						int m = ((NonLeafNode<K, ?>) N).childCount() - 1; // index of last pointer in NPrime
						((NonLeafNode<K, P>) NPrime).insert(KPrime, 0, (P) ((NonLeafNode<K, ?>) N).pointer(m), 0);
						K toremove = ((NonLeafNode<K, P>) N).key(m - 1);
						((NonLeafNode<K, P>) N).delete(m - 1, m);
						P.replaceKey(KPrime, toremove);
					} else {// leaf node case
						int m = N.keyCount() - 1; // index of last pointer/key pair
						LeafNode<K, P> nl = (LeafNode<K, P>) NPrime;
						LeafNode<K, P> nPl = (LeafNode<K, P>) N;
						nl.insert(0, nPl.key(m), nPl.pointer(m));
						nPl.remove(nPl.key(m));

					}

				}
			}

		} else {
			save(N);
		}

	}

	 
	public void merge(Node<K, P> NPrime, K KPrime, Node<K, P> N,
			Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent) throws InvalidDeletionException, IOException {

		if (N instanceof LeafNode) {
			LeafNode<K, P> lnN = (LeafNode<K, P>) N;
			LeafNode<K, P> lnNPrime = (LeafNode<K, P>) NPrime;
			NPrime.append(N, 0, N.keyCount());
			lnNPrime.setSuccessor(lnN.successor());
		} else {
			//NonLeafNode<K, P> nlf = (NonLeafNode<K, P>) N;
			NonLeafNode<K, P> nprimelf = (NonLeafNode<K, P>) NPrime;
			nprimelf.insert(KPrime, nprimelf.keyCount(), null, nprimelf.keyCount() + 1);
		}
		save(NPrime);
		NonLeafNode<K, Node<K, ?>> p = node2parent.get(NPrime);
		delete_entry(p, KPrime, node2parent);

	}
}
