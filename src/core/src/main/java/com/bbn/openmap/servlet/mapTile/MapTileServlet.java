package com.bbn.openmap.servlet.mapTile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.util.ComponentFactory;
import com.bbn.openmap.util.PropUtils;
import com.bbn.openmap.util.http.HttpConnection;
import com.bbn.openmap.util.wanderer.Wanderer;
import com.bbn.openmap.util.wanderer.WandererCallback;

/**
 * MapTileServlet is a servlet class that fields requests for map tiles. It can
 * handle multiple MapTileSets, each one defined by a properties file. The
 * web.xml file for this servlet lets you specify the directory where these
 * properties files are, under the TileSetDefinitions attribute. The properties
 * files in that directory are automatically read and used to create
 * MapTileSets. The default deployed name and location of this directory is the
 * WEB-INF/classes/tileSetDefinitions directory, but any location can be
 * specified.
 * 
 * Each maptileset properties file should specify a name of the tile set, which
 * is used in the path to reach those tiles. The MapTileSet object is used by
 * the MapTileServlet to handle the specific configuration of the tile set, and
 * the MapTileSet object classname to use can be specified in the maptileset
 * properties under the 'class' property. The StandardMapTileSet is used by
 * default, it assumes the tile set is stored in a z/x/y file structure. The
 * TileMillMapTileSet knows how to use mbtiles files created using TileMill. The
 * RelayMapTileSet uses a local z/x/y directory structure as a cache for tiles
 * to disperse, but goes to another server location to fetch new tiles it
 * doesn't have. Each MapTileSet has configuration information in its javadoc.
 * See the web.xml file for more information about configuring this
 * MapTileServlet.
 * 
 * @author dietrick
 */
public class MapTileServlet extends HttpServlet {
    public final static String TILE_SET_DESCRIPTION_ATTRIBUTE = "TileSetDefinitions";
    public final static String LEAFLET_CSS_LOCATION_ATTRIBUTE = "leaflet_css";
    public final static String LEAFLET_JS_LOCATION_ATTRIBUTE = "leaflet_js";
    protected Map<String, MapTileSet> mapTileSets;
    
    String leafletCssLocation = "http://cdn.leafletjs.com/leaflet-0.7.2/leaflet.css";
    String leafletJsLocation = "http://cdn.leafletjs.com/leaflet-0.7.2/leaflet.js";

    /**
     * A do-nothing constructor - init does all the work.
     */
    public MapTileServlet() {
        super();

        mapTileSets = Collections.synchronizedMap(new HashMap<String, MapTileSet>());
    }

    /**
     * Called when the servlet is loaded.
     * 
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();

        Logger logger = getLogger();        
        String leafletCss = context.getInitParameter(LEAFLET_CSS_LOCATION_ATTRIBUTE);
        if (leafletCss != null) {
        	leafletCssLocation = leafletCss;
        }
        logger.log(Level.INFO, "leaflet.css located at: {0}", leafletCssLocation);
        
        String leafletJs = context.getInitParameter(LEAFLET_JS_LOCATION_ATTRIBUTE);
        if (leafletJs != null) {
        	leafletJsLocation = leafletJs;
        }
        logger.log(Level.INFO, "leaflet.js located at: {0}", leafletJsLocation);        
        
        String descriptions = context.getInitParameter(TILE_SET_DESCRIPTION_ATTRIBUTE);
        logger.log(Level.INFO, "Looking for Tile Set Descriptions at: {0}", descriptions);
        if (descriptions != null) {

            // Changing descriptions to a folder containing properties files
            // defining tile sets.
            try {

                URL descriptionFolder = PropUtils.getResourceOrFileOrURL(descriptions);
                new PropertiesWanderer(new File(descriptionFolder.getFile()));

            } catch (MalformedURLException e) {
                logger.warning("unable to open for Tile Set properties file given " + descriptions);
            } catch (NullPointerException npe) {
                logger.warning("Can't find directory holding Tile Set properties files: "
                        + descriptions);
            }
        }    
        
    }

    /**
     * Given a URL to a properties file describing a MapTileSet, create it and
     * add it to the list.
     * 
     * @param tileSetProperties
     * @throws IOException
     * @throws MalformedURLException
     */
    protected void parseAndAddMapTileSet(URL tileSetProperties)
            throws IOException, MalformedURLException {
        Properties descProps = new Properties();
        Logger logger = getLogger();

        logger.info("going to read props");
        InputStream descURLStream = tileSetProperties.openStream();
        descProps.load(descURLStream);

        logger.info("loaded " + tileSetProperties.toString() + " " + descProps.toString());

        MapTileSet mts = createMapTileSetFromProperties(descProps);

        if (mts != null && mts.allGood()) {
            String mtsName = mts.getName();
            mapTileSets.put(mts.getName(), mts);
            logger.info("Adding " + mtsName + " dataset");
        }

        descURLStream.close();
    }

    protected MapTileSet createMapTileSetFromProperties(Properties props) {
        String className = props.getProperty(MapTileSet.CLASS_ATTRIBUTE);
        Logger logger = getLogger();
        if (className == null) {
            MapTileSet mts = new StandardMapTileSet();
            mts.setProperties(props);
            return mts;
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Creating special map tile set: " + className);
            }
            try {
                Object obj = ComponentFactory.create(className, null, props);

                if (obj instanceof MapTileSet) {
                    return (MapTileSet) obj;
                } else {
                    logger.fine("Had trouble creating "
                            + (obj == null ? className : obj.getClass().getName())
                            + ", not a MapTileSet");
                }

            } catch (Exception e) {
                getLogger().severe("Problem creating " + className + ", " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Handles
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        OutputStream out = resp.getOutputStream();

        String pathInfo = req.getPathInfo();
        Logger logger = getLogger();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("received: " + pathInfo);
        }

        // Empty path request, let's return summary catalog, might be of some
        // help.
        if (pathInfo.length() <= 1) {
            String tilePathHeader = req.getServerName() + ":" + req.getServerPort()
                    + req.getContextPath();
            StringBuilder builder = new StringBuilder("<html><body>Map Tile Sets:<p>");
            for (MapTileSet mts : mapTileSets.values()) {
                String description = mts.getDescription();
                builder.append("Tile set name: <a href=\"http://").append(tilePathHeader).append("/").append(mts.getName()).append("/map\">");
                builder.append(mts.getName()).append("</a>, description: ");
                builder.append(description == null ? "n/a" : description).append("<br>");
            }
            builder.append("</body></html>");

            resp.setContentType(HttpConnection.CONTENT_HTML);
            OutputStreamWriter osw = new OutputStreamWriter(out);
            out.write(builder.toString().getBytes());
            osw.flush();
            return;
        }

        MapTileSet mts = getMapTileSetForRequest(pathInfo);

        if (mts != null) {

            if (pathInfo.endsWith("map")) {
                String tilePathHeader = req.getServerName() + ":" + req.getServerPort()
                        + req.getContextPath();
                String map = getMap(tilePathHeader, mts);
                resp.setContentType(HttpConnection.CONTENT_HTML);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                out.write(map.getBytes());
                osw.flush();
                return;
            }

            try {
                resp.setContentType(HttpConnection.CONTENT_PNG);
                byte[] imageData = mts.getImageData(pathInfo);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                out.write(imageData, 0, imageData.length);
                osw.flush();
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) {
                    getLogger().fine("Tile not found: " + pathInfo);
                }
                HttpConnection.writeHttpResponse(out, HttpConnection.CONTENT_PLAIN, "Problem loading "
                        + pathInfo + " from map tile set:" + mts.getName());
            }
        } else {
            HttpConnection.writeHttpResponse(out, HttpConnection.CONTENT_PLAIN, "Map Tile Set not found for request: "
                    + pathInfo);
        }
    }

    protected MapTileSet getMapTileSetForRequest(String pathInfo) {
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }

        String key = pathInfo;

        // That first part of the path is the MapTileSet name.
        int slash = pathInfo.indexOf('/');
        if (slash > 0) {
            key = pathInfo.substring(0, slash);
        }

        return mapTileSets.get(key);
    }

    /**
     * Given a starting directory, look for properties files that describe
     * MapTileSets.
     * 
     * @author dietrick
     */
    private class PropertiesWanderer extends Wanderer implements WandererCallback {

        public PropertiesWanderer(File startingDirectory) {
            setCallback(this);
            handleEntry(startingDirectory);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.bbn.openmap.util.wanderer.WandererCallback#handleDirectory(java
         * .io.File)
         */
        public boolean handleDirectory(File directory) {
            // Do nothing to directories
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.bbn.openmap.util.wanderer.WandererCallback#handleFile(java.io
         * .File)
         */
        public boolean handleFile(File file) {
            getLogger().fine("Checking " + file);
            try {
                String name = file.getName();
                if (name.endsWith("properties")) {
                    parseAndAddMapTileSet(file.toURI().toURL());
                }
            } catch (MalformedURLException murle) {
                getLogger().warning("Unable to read/load " + file + ", murle");
            } catch (IOException e) {
                getLogger().warning("Unable to read/load " + file + ", ioe");
            }
            return true;
        }

    }

    /**
     * Holder for this class's Logger. This allows for lazy initialization of
     * the logger.
     */
    private static final class LoggerHolder {
        /**
         * The logger for this class
         */
        private static final Logger LOGGER = Logger.getLogger(MapTileServlet.class.getName());

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

    /**
     * Creates a HTML string that will display a Leaflet map with the map tiles
     * for the MapTileSet.
     * 
     * @param tileReqHeader the server:port/context string of this servlet.
     * @param mts the MapTileSet to display.
     * @return html text.
     */
    protected String getMap(String tileReqHeader, MapTileSet mts) {
        String name = mts.getName();

        List<String> nameList = new ArrayList<String>();
        nameList.add(name);
        for (MapTileSet set : mapTileSets.values()) {
            if (!name.equals(set.getName())) {
                nameList.add(set.getName());
            }
        }

        StringBuilder ret = new StringBuilder();

        ret.append("<html><head><link rel=\"stylesheet\" href=\"").append(leafletCssLocation).append("\" />");
        ret.append("<script src=\"").append(leafletJsLocation).append("\"></script></head><body>");
        ret.append("<div id=\"map\" style=\"position:absolute; top:20px; left:20px; right:20px; bottom:20px;overflow:hidden;min-height;200px\"></div>");
        ret.append("<script>");

        StringBuilder layerControlList = null;
        for (String mtsName : nameList) {
            ret.append("var ").append(mtsName).append("Url=\'http://").append(tileReqHeader).append("/").append(mtsName).append("/{z}/{x}/{y}.png\';");
            ret.append("var ").append(mtsName).append("=L.tileLayer(").append(mtsName).append("Url);");
            if (layerControlList == null) {
                layerControlList = new StringBuilder("var baseMaps={");
                layerControlList.append("\"").append(mtsName).append("\":").append(mtsName);
            } else {
                layerControlList.append(",\"").append(mtsName).append("\":").append(mtsName);
            }
        }

        if (layerControlList != null) {
            layerControlList.append("};");
            ret.append(layerControlList.toString());
        }
        ret.append("var map = new L.Map('map', {center:new L.LatLng(0, 0), zoom:1, maxZoom:20, minZoom:0, layers:[").append(name).append("]});");
        ret.append("L.control.scale().addTo(map);");
        ret.append("L.control.layers(baseMaps).addTo(map);");

        ret.append("</script></body></html>");

        return ret.toString();
    }
}
