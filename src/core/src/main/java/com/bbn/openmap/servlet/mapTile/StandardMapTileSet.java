package com.bbn.openmap.servlet.mapTile;

import com.bbn.openmap.dataAccess.mapTile.StandardMapTileFactory;
import com.bbn.openmap.image.PNGImageIOFormatter;
import com.bbn.openmap.io.BinaryBufferedFile;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.util.PropUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * The StandardMapTileSet contains all the information for handling a specific set of tiles. It contains a description,
 * key to use in the request, the location of the data directory or jar, and how to handle empty tiles. The
 * StandardMapTileSet properties file would look like this:
 *
 * <pre>
 * name=the-name-of-dataset
 * # default, don't really need to specify the class property
 * #class=com.bbn.openmap.maptileservlet.StandardMapTileSet
 * rootDir=the path to the root of the tile directory, the parent of the z-level directory.
 * </pre>
 *
 * As an example, a url for accessing a tile from this server would be:
 *
 * <pre>
 * http://your.machine/ommaptile/the-name-of-dataset/z/x/y.png
 * </pre>
 *
 * where ommaptile is the name of the servlet. You can change that in the web.xml and in glassfish/tomcat.
 *
 * @author dietrick
 */
public class StandardMapTileSet
	extends StandardMapTileFactory
	implements MapTileSet {

	public final static String NAME_ATTRIBUTE = "name";
	public final static String DESCRIPTION_ATTRIBUTE = "description";

	protected String name;
	protected String description = null;

	// To allow the component factory to create it.
	public StandardMapTileSet() {
	}

	public boolean allGood() {
		return name != null && rootDir != null;
	}

	public void setProperties(String prefix, Properties props) {
		super.setProperties(prefix, props);
		prefix = PropUtils.getScopedPropertyPrefix(prefix);

		name = props.getProperty(prefix + NAME_ATTRIBUTE, name);
		description = props.getProperty(prefix + DESCRIPTION_ATTRIBUTE, description);
	}

	public Properties getProperties(Properties props) {
		props = super.getProperties(props);
		String prefix = PropUtils.getScopedPropertyPrefix(this);
		props.put(prefix + NAME_ATTRIBUTE, PropUtils.unnull(name));
		props.put(prefix + DESCRIPTION_ATTRIBUTE, PropUtils.unnull(description));
		return props;
	}

	public byte[] getImageData(String pathInfo)
		throws IOException, FormatException {
		byte[] imageData = null;

        // We're assuming that all queries are coming in with the name in front,
		// along with a slash
		if (name != null) {
			int index = pathInfo.indexOf(name, 1);
			pathInfo = pathInfo.substring(index + name.length());
		}

		String filePath = rootDir + pathInfo;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("looking for " + filePath);
		}

		try {

			BinaryBufferedFile file = new BinaryBufferedFile(filePath);
			imageData = file.readBytes(100000, true);
			file.close();

		} catch (IOException ioe) {
			logger.fine("Problem fetching the file");
			// The file wasn't found.
			if (emptyTileHandler != null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Creating " + filePath + " since it wasn't found from the server.");
				}

				TileInfo ti = new TileInfo(filePath);// FPBT: used to be
				// pathInfo
				ti.setMtcTransform(getMtcTransform());
				BufferedImage bufferedImage = ti.getBufferedImage(emptyTileHandler);

				if (bufferedImage != null) {
					imageData = new PNGImageIOFormatter().formatImage(bufferedImage);
					logger.fine("buffered image created, writing file to disk too");
					File newFile = new File(filePath);
					newFile.getParentFile().mkdirs();
					// Write the image data to the local cache location
					FileOutputStream fos = new FileOutputStream(newFile);
					fos.write(imageData);
					fos.flush();
					fos.close();
				} else {
					logger.fine("null buffered image back from EmptyTileHandler");
				}
			} else {
				logger.fine("no empty file handler");
			}
		}

		return imageData;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
