package util;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import bptree.BPlusTree;
import bptree.BPlusTree.InvalidDeletionException;
import bptree.BPlusTree.InvalidInsertionException;
import bptree.InMemoryBPlusTree;
import storage.StorageManager;
import bptree.LeafNode;
import bptree.Node;
import bptree.NonLeafNode;
import bptree.PersistentBPlusTree;

/**
 * A {@code BPlusTreeVisualizer} can display a collection of {@code BPlusTree}s, one at a time.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 */
public class BPlusTreeVisualizer extends util.ZoomView implements java.awt.event.KeyListener {

	/**
	 * Automatically generated serial version ID.
	 */
	private static final long serialVersionUID = -3741562432319457809L;

	/**
	 * The width on the display for each key.
	 */
	protected static int keyWidth = 50;

	/**
	 * The height on the display for each key.
	 */
	protected static int keyHeight = 20;

	/**
	 * The width on the display for each pointer.
	 */
	protected static int pointerWidth = 15;

	/**
	 * A collection of drawings.
	 */
	protected java.util.Vector<util.Pair<LinkedList<Runnable>, String>> drawings = new java.util.Vector<util.Pair<LinkedList<Runnable>, String>>();

	/**
	 * The index of the drawing to show.
	 */
	protected int currentDrawing = 0;

	/**
	 * A {@code PrintStream}.
	 */
	protected PrintStream out;

	/**
	 * Constructs a {@code BPlusTreeVisualizer}.
	 * 
	 * @param out
	 *            a {@code PrintStream}
	 */
	public BPlusTreeVisualizer(PrintStream out) {
		addKeyListener(this); // This class has its own key listeners.
		setFocusable(true); // Allow panel to get focus
		zoomGraphics.setMinXY(-keyWidth, -2 * keyHeight);
		this.out = out;
	}

	@Override
	public void draw() {
		try {
			if (currentDrawing < drawings.size()) {
				LinkedList<Runnable> commands = drawings.elementAt(currentDrawing).first();
				zoomGraphics.setColor(Color.BLACK);
				for (Runnable command : commands)
					command.run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void paint(java.awt.Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		super.paint(g);
		if (currentDrawing < drawings.size())
			g.drawString("" + currentDrawing + " : " + drawings.elementAt(currentDrawing).second(), 10,
					g.getFontMetrics().getHeight());
	}

	/**
	 * The main program.
	 * 
	 * @param args
	 *            the String argument
	 * @throws Exception
	 *             if an error occurs
	 */
	public static void main(String[] args) throws Exception {
		startVisualizer(new InMemoryBPlusTree<String, Integer>(3), "input.txt", System.out, 0, 0);
		startVisualizer(new PersistentBPlusTree<String, Integer>(3, storageManager(), 0), "input.txt", System.out, 100,
		 		100);
	}

	/**
	 * Starts a {@code BPlusTreeVisualizer}
	 * 
	 * @param degree
	 *            the degree of the {@code BPlusTreeVisualizer} to create
	 * @param inputFile
	 *            an input file defining insertion and deletion operations
	 * @param out
	 *            a {@code PrintStream}
	 * @param x
	 *            the x-coordinate of the top-left corner of this {BPlusTreeVisualizer}
	 * @param y
	 *            the y-coordinate of the top-left corner of this {BPlusTreeVisualizer}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void startVisualizer(BPlusTree<String, Integer> tree, String inputFile, PrintStream out, int x, int y)
			throws IOException {
		JFrame frame = new JFrame("B+Tree Visualizer (degree: " + tree.degree() + ", input: " + inputFile + ")");
		BPlusTreeVisualizer panel = new BPlusTreeVisualizer(out);
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setLocation(x, y);
		java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(inputFile));
		out.println(inputFile);
		String line = "";
		try {
			while ((line = reader.readLine()) != null) {
				out.println();
				out.println("% " + line);
				String[] tokens = line.split(" ");
				try {
					if (tokens[0].equals("insert"))
						tree.insert(tokens[1], Integer.parseInt(tokens[2]));
					else if (tokens[0].equals("delete"))
						tree.delete(tokens[1]);
				} catch (InvalidInsertionException | InvalidDeletionException e) {
					out.println(e.getClass().getSimpleName());
				}
				panel.capture(tree, line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			out.println("last input line: " + line);
		} finally {
			reader.close();
		}
		frame.setVisible(true);
		panel.repaint();
	}

	static StorageManager<Integer, Object> storageManager() {
		return new StorageManager<Integer, Object>() {

			Map<Integer, TreeMap<Integer, byte[]>> buffer = new HashMap<Integer, TreeMap<Integer, byte[]>>();

			@Override
			public Object get(int fileID, Integer loc) throws IOException {
				Map<Integer, byte[]> m = buffer.get(fileID);
				if (m == null) {
					return null;
				} else {
					Object o = toObject(m.get(loc));
					return o;
				}
			}

			@Override
			public Object put(int fileID, Integer loc, Object o) throws IOException {
				TreeMap<Integer, byte[]> m = buffer.get(fileID);
				if (m == null) {
					m = new TreeMap<Integer, byte[]>();
					buffer.put(fileID, m);
				}
				byte[] b = m.put(loc, toByteArray(o));
				return toObject(b);
			}

			@Override
			public Integer add(int fileID, Object o) throws IOException {
				TreeMap<Integer, byte[]> m = buffer.get(fileID);
				if (m == null) {
					m = new TreeMap<Integer, byte[]>();
					buffer.put(fileID, m);
				}
				int loc = m.isEmpty() ? first() + 1 : m.lastKey() + 1;
				m.put(loc, toByteArray(o));
				return loc;
			}

			@Override
			public Object remove(int fileID, Integer loc) throws IOException {
				TreeMap<Integer, byte[]> m = buffer.get(fileID);
				if (m == null)
					return null;
				Object o = toObject(m.remove(loc));
				return o;
			}

			/**
			 * Serializes the specified object into a byte array.
			 * 
			 * @param o
			 *            an object
			 * @return a byte array representing the specified object
			 * @throws IOException
			 *             if an I/O error occurs
			 */
			protected byte[] toByteArray(Object o) throws IOException {
				if (o == null)
					return null;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				new ObjectOutputStream(out).writeObject(o);
				return out.toByteArray();
			}

			/**
			 * Deserializes an object from the specified byte array.
			 * 
			 * @param b
			 *            a byte array
			 * @return an object deserialized from the specified byte array
			 * @throws IOException
			 *             if an I/O error occurs
			 */
			protected Object toObject(byte[] b) throws IOException {
				try {
					if (b == null)
						return null;
					return new ObjectInputStream(new ByteArrayInputStream(b)).readObject();
				} catch (IOException e) {
					throw e;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public Integer first() {
				return 0;
			}

			@Override
			public Iterator<Object> iterator(int fileID) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear(int fileID) throws IOException {
				throw new UnsupportedOperationException();
			}

		};
	}

	/**
	 * Captures the specified {@code BPlusTree}.
	 * 
	 * @param tree
	 *            a {@code BPlusTree}
	 * @param caption
	 *            a {@code String} caption
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	void capture(BPlusTree<String, Integer> tree, String caption) throws IOException {
		LinkedList<Runnable> commands = new LinkedList<Runnable>();
		Node<String, ?> root = tree.root();
		if (root != null) {
			draw(root, tree, 1, 0, tree.degree(), commands, new HashMap<Object, Integer>());
			drawings.add(new util.Pair<LinkedList<Runnable>, String>(commands, caption));
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		switch (key) {
		case KeyEvent.VK_LEFT:
			if (e.isControlDown()) {
				zoomGraphics.moveBy(keyWidth, 0);
			} else {
				currentDrawing = Math.max(0, currentDrawing - 1);
			}
			repaint();
			break;
		case KeyEvent.VK_RIGHT:
			if (e.isControlDown()) {
				zoomGraphics.moveBy(-keyWidth, 0);
			} else {
				currentDrawing = Math.min(drawings.size() - 1, currentDrawing + 1);
			}
			repaint();
			break;
		case KeyEvent.VK_UP: // zoom in
			if (e.isControlDown()) {
				zoomGraphics.moveBy(0, keyHeight);
			} else {
				zoomGraphics.changeScale(1.2);
			}
			repaint();
			break;
		case KeyEvent.VK_DOWN:
			if (e.isControlDown()) {
				zoomGraphics.moveBy(0, -keyHeight);
			} else {
				zoomGraphics.changeScale(1 / 1.2);
			}
			repaint();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * Draws the specified node on the screen.
	 * 
	 * @param node
	 *            the {@code Node} to display
	 * @param tree
	 *            a {@code BPlusTree}
	 * @param level
	 *            the level of the tree
	 * @param leafNodes
	 *            the number of known {@code LeafNode}s.
	 * @param degree
	 *            the degree of the tree
	 * @param commands
	 *            the commands for drawing the nodes in the tree
	 * @return the number of known {@code LeafNode}s and the location of the node on the screen
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected util.Pair<Integer, Integer> draw(Node<String, ?> node, BPlusTree<String, Integer> tree, int level,
			int leafNodes, int degree, LinkedList<Runnable> commands, HashMap<Object, Integer> m) throws IOException {
		out.println(String.format("%" + level + "s", "") + node.toString(m));
		Integer[] childrenPos = null;
		int x = (leafNodes) * keyWidth * (degree);
		int y = (level - 1) * 2 * keyHeight;
		if (node instanceof NonLeafNode) {
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			childrenPos = new Integer[degree];
			for (int i = 0; i < degree; i++) {
				Node<String, ?> child = tree.child((NonLeafNode<String, ?>) node, i);
				if (child != null) {
					util.Pair<Integer, Integer> widthPos = draw(child, tree, level + 1, leafNodes, degree, commands, m);
					leafNodes = widthPos.first();
					childrenPos[i] = widthPos.second();
					if (child instanceof LeafNode)
						leafNodes++;
					minX = Math.min(minX, childrenPos[i]);
					maxX = Math.max(maxX, childrenPos[i]);
				}
			}
			x = (minX + maxX) / 2;
		} else { // if leaf node
			if (((LeafNode<String, ?>) node).successor() != null) {// if there is a next leaf node
				commands.add(drawLine(x + keyWidth * (degree - 1), y + keyHeight / 2,
						x + keyWidth * (degree) - pointerWidth / 2, y + keyHeight / 2));
			}
		}
		commands.add(() -> zoomGraphics.setColor(Color.LIGHT_GRAY));
		commands.add(() -> zoomGraphics.setColor(Color.WHITE));
		commands.add(fillRect(x - pointerWidth / 2, y, keyWidth * (degree - 1) + pointerWidth, keyHeight));
		for (int i = 0; i < degree; i++) {
			commands.add(() -> zoomGraphics.setColor(Color.LIGHT_GRAY));
			commands.add(fillRect(x + i * keyWidth - pointerWidth / 2, y, pointerWidth, keyHeight));
			commands.add(() -> zoomGraphics.setColor(Color.BLACK));
			commands.add(drawRect(x + i * keyWidth - pointerWidth / 2, y, pointerWidth, keyHeight));
			if (childrenPos != null && childrenPos[i] != null) { // draw a line to the child
				commands.add(drawLine(x + i * keyWidth, y + keyHeight - 4, childrenPos[i] + keyWidth * (degree - 1) / 2,
						level * 2 * keyHeight));
			}
			if (i < degree - 1 && node.key(i) != null) {
				commands.add(drawStrings(new String[] { node.key(i).toString() },
						x + i * keyWidth + pointerWidth / 2 + 5, y, keyWidth - pointerWidth, keyHeight));
			}
			if (i < degree - 1 && node instanceof LeafNode && node.pointer(i) != null) {
				commands.add(drawStrings(new String[] { node.pointer(i).toString() },
						x + i * keyWidth - pointerWidth / 2 + 5, y, pointerWidth, keyHeight));
			}
		}
		commands.add(drawRect(x - pointerWidth / 2, y, keyWidth * (degree - 1) + pointerWidth, keyHeight));
		return new util.Pair<Integer, Integer>(leafNodes, x);
	}

	/**
	 * Creates a command for drawing the text given by the specified strings.
	 * 
	 * @param s
	 *            the text string
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param width
	 *            the width of the text area
	 * @param height
	 *            the height of the text area
	 * @return a command for drawing the text given by the specified strings
	 */
	Runnable drawStrings(String[] s, double x, double y, double width, double height) {
		return () -> zoomGraphics.drawStrings(s, x, y, width, height);
	}

	/**
	 * Creates a command for drawing the outline of the specified rectangle. The left and right edges of the rectangle
	 * are at <code>x</code> and <code>x&nbsp;+&nbsp;width</code>. The top and bottom edges are at <code>y</code> and
	 * <code>y&nbsp;+&nbsp;height</code>. The rectangle is drawn using the graphics context's current color.
	 * 
	 * @param x
	 *            the <i>x</i> coordinate of the rectangle to be drawn
	 * @param y
	 *            the <i>y</i> coordinate of the rectangle to be drawn
	 * @param width
	 *            the width of the rectangle to be drawn
	 * @param height
	 *            the height of the rectangle to be drawn
	 * @return a command for drawing the outline of the specified rectangle
	 */
	Runnable drawRect(double x, double y, double width, double height) {
		return () -> zoomGraphics.drawRect(x, y, width, height);
	}

	/**
	 * Creates a command for filling the specified rectangle. The left and right edges of the rectangle are at
	 * <code>x</code> and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>. The top and bottom edges are at <code>y</code>
	 * and <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>. The resulting rectangle covers an area <code>width</code>
	 * pixels wide by <code>height</code> pixels tall. The rectangle is filled using the graphics context's current
	 * color.
	 * 
	 * @param x
	 *            the <i>x</i> coordinate of the rectangle to be filled
	 * @param y
	 *            the <i>y</i> coordinate of the rectangle to be filled
	 * @param width
	 *            the width of the rectangle to be filled
	 * @param height
	 *            the height of the rectangle to be filled
	 * @return a command for filling the specified rectangle
	 */
	Runnable fillRect(double x, double y, double width, double height) {
		return () -> zoomGraphics.fillRect(x, y, width, height);
	}

	/**
	 * Creates a command for drawing a line, using the current color, between the points <code>(x1,&nbsp;y1)</code> and
	 * <code>(x2,&nbsp;y2)</code> in this graphics context's coordinate system.
	 * 
	 * @param x1
	 *            the first point's <i>x</i> coordinate
	 * @param y1
	 *            the first point's <i>y</i> coordinate
	 * @param x2
	 *            the second point's <i>x</i> coordinate
	 * @param y2
	 *            the second point's <i>y</i> coordinate
	 * @return a command for drawing a line
	 */
	Runnable drawLine(double x1, double x2, double y1, double y2) {
		return () -> zoomGraphics.drawLine(x1, x2, y1, y2);
	}

}
