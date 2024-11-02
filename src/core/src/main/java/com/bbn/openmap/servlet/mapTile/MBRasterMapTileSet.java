/* 
 */
package com.bbn.openmap.servlet.mapTile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import com.bbn.openmap.dataAccess.mapTile.mb.RasterMapTileFactory;
import com.bbn.openmap.image.BufferedImageHelper;
import com.bbn.openmap.image.PNGImageIOFormatter;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.util.PropUtils;

/**
 * MapTileSet that reads MapBox raster mbtiles files.
 * 
 * <pre>
 * name=the-name-of-dataset
 * class=com.bbn.openmap.maptileservlet.MBRasterMapTileSet
 * rootDir=the path to the mbtiles file.
 *  * </pre>
 * 
 * As an example, a url for accessing a tile from this server would be:
 * 
 * <pre>
 * http://your.machine/ommaptile/the-name-of-dataset/z/x/y.png
 * </pre>
 * 
 * where ommaptile is the name of the servlet. You can change that in the
 * web.xml and in glassfish/tomcat.
 * 
 * @author dietrick
 */
public class MBRasterMapTileSet extends StandardMapTileSet {

    public MBRasterMapTileSet() {
    }

    public MBRasterMapTileSet(Properties props) {
        setProperties(props);
    }

    public Properties getProperties(Properties props) {
        props = super.getProperties(props);
        String prefix = PropUtils.getScopedPropertyPrefix(this);
        props.put(prefix + CLASS_ATTRIBUTE, this.getClass().getName());

        return props;
    }

    public byte[] getImageData(String pathInfo) throws IOException, FormatException {

        byte[] imageData = null;

        try {
            Class.forName(RasterMapTileFactory.DEFAULT_TEST_CLASS);
        } catch (Exception e) {
            getLogger().warning("can't locate sqlite JDBC components");
            return null;
        }

        try {

            TileInfo ti = new TileInfo(pathInfo);

            Connection conn = DriverManager.getConnection(rootDir);
            Statement stat = conn.createStatement();

            // "select zoom_level, tile_column, tile_row, tile_data from map, images where map.tile_id = images.tile_id";
            StringBuilder statement = new StringBuilder("select tile_data from map, images where");
            statement.append(" zoom_level = ").append(ti.zoomLevel);
            statement.append(" and tile_column = ").append(ti.x);
            statement.append(" and tile_row = ").append((int) (Math.pow(2, ti.zoomLevel)) - ti.y
                    - 1);
            statement.append(" and map.tile_id = images.tile_id;");

            ResultSet rs = stat.executeQuery(statement.toString());
            while (rs.next()) {
                byte[] imageBytes = rs.getBytes("tile_data");
                ImageIcon ii = new ImageIcon(imageBytes);
                BufferedImage bi = BufferedImageHelper.getBufferedImage(ii.getImage(), 0, 0, -1, -1);

                // TODO: Still have to incorporate properties or something to
                // all
                // specification of map image format.
                imageData = new PNGImageIOFormatter().formatImage(bi);
            }
            rs.close();
            conn.close();
        } catch (Exception e) {
            getLogger().warning("something went wrong fetching image from database: "
                    + e.getMessage());
            e.printStackTrace();
        }

        return imageData;
    }

    /**
     * Holder for this class's Logger. This allows for lazy initialization of
     * the logger.
     */
    private static final class LoggerHolder {
        /**
         * The logger for this class
         */
        private static final Logger LOGGER = Logger.getLogger(MapTileSet.class.getName());

        /**
         * Prevent instantiation
         */
        private LoggerHolder() {
            throw new AssertionError("This should never be instantiated");
        }
    }

    /**
     * Get the logger for this class.
     * 
     * @return logger for this class
     */
    private static Logger getLogger() {
        return LoggerHolder.LOGGER;
    }

}
