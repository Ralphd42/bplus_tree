package bptree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import bptree.BPlusTree.InvalidDeletionException;
import storage.StorageManager;
import storage.StorageManager.InvalidLocationException;

/**
 * The {@code PersistentBPlusTree} class implements B+-trees. Each {@code PersistentBPlusTree} stores its {@code Node}s
 * using a {@code StorageManager}.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 * @param <K>
 *            the type of keys
 * @param <P>
 *            the type of pointers
 */
public class PersistentBPlusTree<K extends Comparable<K>, P> extends BPlusTree<K, P> {

	/**
	 * The {@code StorageManager} used for this {@code PersistentBPlusTree}.
	 */
	protected StorageManager<P, Object> sm;

	/**
	 * The ID of the file used for this {@code PersistentBPlusTree}.
	 */
	protected int fileID;

	/**
	 * Constructs a {@code PersistentBPlusTree}.
	 * 
	 * @param degree
	 *            the maximum number of pointers that each {@code Node} of this {@code PersistentBPlusTree} can have
	 * @param sm
	 *            {@code StorageManager} used for this {@code PersistentBPlusTree}
	 * @param fileID
	 *            the identifier of the file used for this {@code PersistentBPlusTree}
	 */
	public PersistentBPlusTree(int degree, StorageManager<P, Object> sm, int fileID) {
		super(degree);
		this.sm = sm;
		this.fileID = fileID;
	}

	/**
	 * Returns the root {@code Node} of this {@code PersistentBPlusTree}.
	 * 
	 * @return the root {@code Node} of this {@code PersistentBPlusTree}; {@code null} if this
	 *         {@code PersistentBPlusTree} is empty
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public Node<K, ?> root() throws IOException {
		return root(new HashMap<Node<K, P>, P>());
	}

	/**
	 * Returns the specified child {@code Node} of the specified {@code NonLeafNode}.
	 * 
	 * @param node
	 *            a {@code NonLeafNode}
	 * @param i
	 *            the index of the child {@code Node}
	 * @return the specified child {@code Node} of the specified {@code NonLeafNode}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Node<K, ?> child(NonLeafNode<K, ?> node, int i) throws IOException {
		P p = (P) node.pointer(i);
		if (p == null)
			return null;
		else
			try {
				return (Node<K, ?>) sm.get(fileID, p);
			} catch (InvalidLocationException e) {
				e.printStackTrace();
				return null;
			}
	}

	/**
	 * Inserts the specified key and pointer into this {@code PersistentBPlusTree}.
	 * 
	 * @param k
	 *            the key to insert
	 * @param p
	 *            the pointer to insert
	 * @throws InvalidInsertionException
	 *             if a key already existent in this {@code PersistentBPlusTree} is attempted to be inserted again in
	 *             the {@code PersistentBPlusTree}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public void insert(K k, P p) throws InvalidInsertionException, IOException {
		Map<Node<K, P>, P> node2pointer = new HashMap<Node<K, P>, P>();
		Node<K, P> root = root(node2pointer);
		if (root == null) {// if the tree is empty
			LeafNode<K, P> l = new LeafNode<K, P>(degree); // create an empty root node
			l.insert(k, p); // insert the specified key and pointer into leaf node l
			setRoot(l); // save node l as the new root
		} else { // if the tree is not empty
			HashMap<Node<K, P>, NonLeafNode<K, P>> node2parent = new HashMap<Node<K, P>, NonLeafNode<K, P>>();
			// to remember the parent of each visited node
			LeafNode<K, P> l = find(k, root, node2parent, node2pointer); // find leaf node l that should contain the
																			// specified key
			if (l.contains(k)) // no duplicate keys are allowed in the tree
				throw new InvalidInsertionException("key: " + k);
			if (!l.isFull()) { // if leaf node l has room for the specified key
				l.insert(k, p); // insert the specified key and pointer into leaf node l
				save(l, node2pointer); // save node l on storage
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
				P _lp = save(lp, node2pointer); // save node lp on storage and also get a pointer to node lp
				l.setSuccessor(_lp); // chaining from leaf node l to leaf node lp
				save(l, node2pointer); // save node l on storage
				insertInParent(l, lp.key(0), lp, root, node2parent, node2pointer); // use lp's first key as the
																					// separating key
			}
		}
	}

	/**
	 * Returns the root {@code Node}.
	 * 
	 * @return the root {@code Node}; {@code null} if this {@code PersistentBPlusTree} is empty
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	protected Node<K, P> root(Map<Node<K, P>, P> node2pointer) throws IOException {
		try {
			P first = sm.first();
			first = (P) sm.get(fileID, first);
			Node<K, P> root = (Node<K, P>) sm.get(fileID, first);
			if (root == null)
				return null;
			node2pointer.put(root, first);
			return root;
		} catch (InvalidLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Finds the {@code LeafNode} that is a descendant of the specified {@code Node} and must be responsible for the
	 * specified key.
	 * 
	 * @param k
	 *            a search key
	 * @param n
	 *            a {@code Node}
	 * @param node2parent
	 *            a {@code Map} to remember, for each visited {@code Node}, the parent of that {@code Node}
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @return the {@code LeafNode} which is a descendant of the specified {@code Node} and must be responsible for the
	 *         specified key
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected LeafNode<K, P> find(K k, Node<K, P> n, Map<Node<K, P>, NonLeafNode<K, P>> node2parent,
			Map<Node<K, P>, P> node2pointer) throws IOException {
		if (n instanceof LeafNode)
			return (LeafNode<K, P>) n;
		else {
			Node<K, P> c = node(((NonLeafNode<K, P>) n).child(k), node2pointer);
			node2parent.put(c, (NonLeafNode<K, P>) n);
			return find(k, c, node2parent, node2pointer);
		}
	}

	/**
	 * Inserts the specified key into the parent {@code Node} of the specified {@code Nodes}.
	 * 
	 * @param n
	 *            a {@code Node}
	 * @param k
	 *            the key between the {@code Node}s
	 * @param np
	 *            a {@code Node}
	 * @param root
	 *            the root {@code Node}
	 * @param node2parent
	 *            a {@code Map} remembering, for each visited {@code Node}, the parent of that {@code Node}
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void insertInParent(Node<K, P> n, K k, Node<K, P> np, Node<K, P> root,
			Map<Node<K, P>, NonLeafNode<K, P>> node2parent, Map<Node<K, P>, P> node2pointer) throws IOException {
		if (n.equals(root)) { // if n is the root of the tree
			NonLeafNode<K, P> r = new NonLeafNode<K, P>(degree, node2pointer.get(n), k, node2pointer.get(np));
			setRoot(r); // a new root node r containing n, k, np and save it on storage
			return;
		}
		NonLeafNode<K, P> p = node2parent.get(n); // find the parent p of n
		if (!p.isFull()) { // if parent node p has room for a new entry
			p.insertAfter(k, node2pointer.get(np), node2pointer.get(n)); // insert k and np right after n
			save(p, node2pointer); // save node p on storage
		} else { // if p is full and thus needs to be split
			NonLeafNode<K, P> t = new NonLeafNode<K, P>(degree + 1); // crate a temporary node
			t.copy(p, 0, p.keyCount()); // copy everything of p to the temporary node
			t.insertAfter(k, node2pointer.get(np), node2pointer.get(n)); // insert k and np after n
			p.clear(); // clear p
			NonLeafNode<K, P> pp = new NonLeafNode<K, P>(degree); // create a new node pp
			int m = (int) Math.ceil((degree + 1) / 2.0); // compute the split point
			p.copy(t, 0, m - 1); // copy the first half to parent node p
			pp.copy(t, m, degree); // copy the second half to new node pp
			save(pp, node2pointer); // save node pp
			save(p, node2pointer); // save node p on storage
			insertInParent(p, t.key(m - 1), pp, root, node2parent, node2pointer); // use the middle key as the
																					// separating key
		}
	}

	/**
	 * Saves the specified {@code Node} as the new root {@code Node} on storage.
	 * 
	 * @param n
	 *            a {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void setRoot(Node<K, P> n) throws IOException {
		P p = sm.add(fileID, n);
		try {
			sm.put(fileID, sm.first(), p);
		} catch (InvalidLocationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Saves the specified pointer on storage as a pointer to the root {@code Node}.
	 * 
	 * @param p
	 *            a pointer to a {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void setRoot(P p) throws IOException {
		try {
			sm.put(fileID, sm.first(), p);
		} catch (InvalidLocationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Saves the specified {@code Node} on storage.
	 * 
	 * @param n
	 *            a {@code Node}
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected P save(Node<K, P> n, Map<Node<K, P>, P> node2pointer) throws IOException {
		P p = node2pointer.get(n);
		if (p == null) {
			p = sm.add(fileID, n);
			node2pointer.put(n, p);
		} else
			try {
				sm.put(fileID, p, n);
			} catch (InvalidLocationException e) {
				throw new IOException(e);
			}
		return p;
	}

	/**
	 * Removes the specified {@code Node} on storage.
	 * 
	 * @param n
	 *            the {@code Node} to remove
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void delete(Node<K, P> n, Map<Node<K, P>, P> node2pointer) throws IOException {
		try {
			sm.remove(fileID, node2pointer.get(n));
		} catch (InvalidLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the specified {@code Node}.
	 * 
	 * @param p
	 *            a pointer to the {@code Node} to reference
	 * @param node2pointer
	 *            a {@code Map} remembering, for each visited {@code Node}, a pointer to that {@code Node}
	 * @return the specified {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	protected Node<K, P> node(P p, Map<Node<K, P>, P> node2pointer) throws IOException {
		Node<K, P> n;
		try {
			n = (Node<K, P>) sm.get(fileID, p);
			if (n == null)
				return null;
			else {
				node2pointer.put(n, p);
				return n;
			}
		} catch (InvalidLocationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Removes the specified key and the corresponding pointer from this {@code PersistentBPlusTree}.
	 * 
	 * @param k
	 *            the key to delete
	 * @throws InvalidDeletionException
	 *             if a key non-existent in a {@code PersistentBPlusTree} is attempted to be deleted from the
	 *             {@code PersistentBPlusTree}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public void delete(K k) throws InvalidDeletionException, IOException {
		// please implement the body of this method
		HashMap<Node<K, P>, NonLeafNode<K, P>> node2parent = new HashMap<Node<K, P>, NonLeafNode<K, P>>();
		Map<Node<K, P>, P> node2pointer = new HashMap<Node<K, P>, P>();
		Node<K, P> root = root(node2pointer);
		LeafNode<K, P> l = find(k, root, node2parent, node2pointer);
		delete_entry((Node<K, ?>)l,k, node2parent, node2pointer);
//delete_entry(Node<K, ?> N, K k, Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent,Map<Node<K, P>, P> node2pointer)
	}

	 
	 
	
	public void merge(Node<K, P> NPrime, K KPrime, Node<K, P> N,
			Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent,Map<Node<K, P>, P> node2pointer) throws InvalidDeletionException, IOException {

		if (N instanceof LeafNode) {
			LeafNode<K, P> lnN = (LeafNode<K, P>) N;
			LeafNode<K, P> lnNPrime = (LeafNode<K, P>) NPrime;
			NPrime.append(N, 0, N.keyCount());
			lnNPrime.setSuccessor(lnN.successor());
		} else {
			NonLeafNode<K, P> nprimelf = (NonLeafNode<K, P>) NPrime;
			nprimelf.insert(KPrime, nprimelf.keyCount(), null, nprimelf.keyCount() + 1);
			nprimelf.append(N, 0, N.keyCount());
		}
		save(NPrime, node2pointer);
		NonLeafNode<K, Node<K, ?>> p = node2parent.get(NPrime);
		delete_entry(p, KPrime, node2parent,node2pointer);
		delete(N,   node2pointer);
	}
	
	@SuppressWarnings("unchecked")
	protected void delete_entry(Node<K, ?> N, K k, Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent,Map<Node<K, P>, P> node2pointer)
			throws InvalidDeletionException, IOException {
			N.remove(k);// this completes trivial deletion at leaf nodes
			
			Node<K, P> root = root(node2pointer);  // get the root
			if (N.equals(root)) { // Handle the root case
			if ((N instanceof NonLeafNode) && ((NonLeafNode<K, ?>) N).childCount() == 1) {// if root has 1 child
				  
				setRoot((Node<K,P>)child(((NonLeafNode<K, ?>) N), 0));// set root to child
			} else {
				save((Node<K,P>)N,node2pointer); // save N
			}
		} else if (N.isUnderUtilized()) {
			NonLeafNode<K, ?> P = (NonLeafNode<K, ?>) node2parent.get(N);// get parent
			Node<K, P> NPrime = (Node<K, P>) P.leftPointer(k); // get left node. If left isn't available get right node
			K KPrime = P.leftKey(k);// get key between the nodes
			// check for merge able
			if (((Node<K, P>) N).mergeable((Node<K, P>) NPrime)) {// nodes can be merged
				if (P.IsleftKey(k)) {
					merge(NPrime, KPrime, (Node<K, P>) N, node2parent,node2pointer);

				} else {
					merge((Node<K, P>) N, KPrime, NPrime, node2parent,node2pointer);
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
			save((Node<K,P>)N, node2pointer);
		}

	}

	
	
	
	
	
	

}
