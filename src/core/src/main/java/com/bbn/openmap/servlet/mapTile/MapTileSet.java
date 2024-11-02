package com.bbn.openmap.servlet.mapTile;

import com.bbn.openmap.PropertyConsumer;
import com.bbn.openmap.io.FormatException;
import java.io.IOException;

/**
 * The MapTileSet contains all the information for handling a specific set of tiles. It contains a description, key to
 * use in the request, the location of the data directory, and how to handle empty tiles.
 *
 * @author dietrick
 */
public interface MapTileSet
	extends PropertyConsumer {

	/**
	 * Property used for MapTileSet class.
	 */
	public final static String CLASS_ATTRIBUTE = "class";

	/**
	 * Check for MapTileServlet to see if the MapTileSet is configured properly.
	 *
	 * @return true if configured.
	 */
	boolean allGood();

	/**
	 * byte array image data for path.
	 *
	 * @param pathInfo path for file, in z/x/y format.
	 * @return byte[] for image data, null if not found.
	 * @throws IOException
	 * @throws FormatException
	 */
	byte[] getImageData(String pathInfo)
		throws IOException, FormatException;

	/**
	 * Return name to use in URL to tell MapTileServlet to get frames from this MapTileSet.
	 *
	 * @return name of this map tile set.
	 */
	public String getName();

	/**
	 * Set the name of this map tile set.
	 *
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Get a description of this tile set.
	 *
	 * @return string description.
	 */
	public String getDescription();

	/**
	 * Set the description of this tile set.
	 *
	 * @param description
	 */
	public void setDescription(String description);
}
