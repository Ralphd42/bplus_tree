package storage;

import java.io.IOException;
import java.util.Iterator;

/**
 * A {@code StorageManager} manages a storage space.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 * @param <L>
 *            the type of locations of objects in the {@code StorageManager}
 * @param <O>
 *            the type of objects managed by the {@code StorageManager}
 */
public interface StorageManager<L, O> {

	/**
	 * Returns the first location in any file.
	 * 
	 * @return the first location in any file.
	 */
	L first();

	/**
	 * Adds the specified object in the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 * @param o
	 *            the object to add
	 * @return the location of the object in the specified file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	L add(int fileID, O o) throws IOException;

	/**
	 * Puts the specified object at the specified location in the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 * @param location
	 *            the location of the object
	 * @param o
	 *            the object to put
	 * @return the object stored previously at the specified location in the specified file; {@code null} if no such
	 *         object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InvalidLocationException
	 *             if an invalid location is given
	 */
	O put(int fileID, L location, O o) throws IOException, InvalidLocationException;

	/**
	 * Returns the object at the specified location in the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 * @param location
	 *            the location of the object
	 * @return the object at the specified location in the specified file
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InvalidLocationException
	 *             if an invalid location is given
	 */
	O get(int fileID, L location) throws IOException, InvalidLocationException;

	/**
	 * Removes the specified object at the specified location in the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 * @param location
	 *            the location of the object
	 * @return the object stored previously at the specified location in the specified file; {@code null} if no such
	 *         object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InvalidLocationException
	 *             if an invalid location is given
	 */
	O remove(int fileID, L location) throws IOException, InvalidLocationException;

	/**
	 * Returns an iterator over all objects stored in the the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 */
	Iterator<Object> iterator(int fileID);

	/**
	 * Removes all data from the specified file.
	 * 
	 * @param fileID
	 *            the ID of the file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	void clear(int fileID) throws IOException;

	/**
	 * An {@code InvalidLocationException} is thrown if an invalid location is used.
	 * 
	 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
	 */
	public class InvalidLocationException extends Exception {

		/**
		 * Automatically generated serial version UID.
		 */
		private static final long serialVersionUID = 597318656808631892L;

	}

}
