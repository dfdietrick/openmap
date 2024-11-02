package com.bbn.openmap.servlet.mapTile;

import com.bbn.openmap.dataAccess.mapTile.ServerMapTileFactory;
import com.bbn.openmap.io.BinaryBufferedFile;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.util.PropUtils;
import java.io.IOException;
import java.util.Properties;

/**
 * The RelayMapTileSet contains all the information for handling a specific set of tiles. It contains a description, key
 * to use in the request, the location of the data directory or jar. This MapTileSet is able to contact a remote server
 * for tiles if the tile file isn't found locally.
 * <p>
 * These properties should be in the properties file referenced in the web.xml file for this tile set:
 *
 * <pre>
 * name=the-name-of-dataset
 * class=com.bbn.openmap.maptileservlet.RelayMapTileSet
 * rootDir=the formatted URL for remote tiles, i.e. http://server.com/{z}/{x}/{y}.png
 * localCacheRootDir=the local path of the cached tiles, i.e. /data/tiles/{z}/{x}/{y}.png
 * </pre>
 *
 * As an example, a url for accessing a tile from this server would be:
 * <pre>
 * http://your.machine/ommaptile/the-name-of-dataset/z/x/y.png
 * </pre> where ommaptile is the name of the servlet. You can change that in the web.xml and in glassfish/tomcat.
 *
 * @author dietrick
 */
public class RelayMapTileSet
	extends ServerMapTileFactory
	implements MapTileSet {

	protected String name;
	protected String description = null;

	// To allow the component factory to create it.
	public RelayMapTileSet() {
	}

	public boolean allGood() {
		return name != null && rootDir != null;
	}

	public void setProperties(String prefix, Properties props) {
		super.setProperties(prefix, props);
		prefix = PropUtils.getScopedPropertyPrefix(prefix);

		name = props.getProperty(prefix + StandardMapTileSet.NAME_ATTRIBUTE, name);
		description = props.getProperty(prefix + StandardMapTileSet.DESCRIPTION_ATTRIBUTE, description);
	}

	public Properties getProperties(Properties props) {
		props = super.getProperties(props);
		String prefix = PropUtils.getScopedPropertyPrefix(this);
		props.put(prefix + StandardMapTileSet.NAME_ATTRIBUTE, PropUtils.unnull(name));
		props.put(prefix + StandardMapTileSet.DESCRIPTION_ATTRIBUTE, PropUtils.unnull(name));		
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

        // We need to build and check for local file. If not found, then call
		// getImageBytes to fetch from other server and cache locally
		String localFilePath = null;
		TileInfo tInfo = new TileInfo(pathInfo);

		if (!tInfo.valid) {
			return imageData;
		}

		if (localCacheDir != null) {
			localFilePath = buildLocalFilePath(tInfo.x, tInfo.y, tInfo.zoomLevel, getFileExt());

			try {

				BinaryBufferedFile file = new BinaryBufferedFile(localFilePath);
				imageData = file.readBytes(100000, true);
				file.close();
				return imageData;

			} catch (IOException ioe) {
				// Didn't find local version of file, that's OK. Continue on...
			}
		}

        // The file wasn't found.
		String remoteFilePath = buildFilePath(tInfo.x, tInfo.y, tInfo.zoomLevel, getFileExt());
		imageData = getImageBytes(remoteFilePath, localFilePath);

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
