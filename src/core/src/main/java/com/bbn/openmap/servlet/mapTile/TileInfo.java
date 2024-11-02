/* 
 * <copyright>
 *  Copyright 2012 BBN Technologies
 * </copyright>
 */
package com.bbn.openmap.servlet.mapTile;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.bbn.openmap.dataAccess.mapTile.EmptyTileHandler;
import com.bbn.openmap.dataAccess.mapTile.MapTileCoordinateTransform;
import com.bbn.openmap.dataAccess.mapTile.OSMMapTileCoordinateTransform;
import com.bbn.openmap.dataAccess.mapTile.SimpleEmptyTileHandler;
import com.bbn.openmap.proj.Mercator;
import com.bbn.openmap.proj.coords.LatLonPoint;

/**
 * TileInfo can look at a path string and figure out zoom level, x, y for tiles.
 * 
 * @author dietrick
 */
public class TileInfo {
    int x;
    int y;
    int zoomLevel;
    String format;
    String pathInfo;
    MapTileCoordinateTransform mtcTransform;

    boolean valid = false;

    Logger logger = Logger.getLogger("com.bbn.openmap.maptileservlet");

    public TileInfo(String pathInfo) {
        this.pathInfo = pathInfo;

        if (pathInfo == null) {
            return;
        }

        int dotIndex = pathInfo.lastIndexOf('.');
        if (dotIndex > 0) {
            format = pathInfo.substring(dotIndex + 1);

            int ySlashIndex = pathInfo.lastIndexOf('/');
            if (ySlashIndex > 0) {
                String yString = pathInfo.substring(ySlashIndex + 1, dotIndex);

                int xSlashIndex = pathInfo.lastIndexOf('/', ySlashIndex - 1);
                if (xSlashIndex > 0) {
                    String xString = pathInfo.substring(xSlashIndex + 1, ySlashIndex);

                    int zSlashIndex = pathInfo.lastIndexOf('/', xSlashIndex - 1);
                    if (zSlashIndex >= 0) {
                        String zString = pathInfo.substring(zSlashIndex + 1, xSlashIndex);

                        // OK, we're here!
                        x = Integer.parseInt(xString);
                        y = Integer.parseInt(yString);
                        zoomLevel = Integer.parseInt(zString);
                        valid = true;
                        return;
                    }
                }
            }
        }

        logger.info("can't decode " + pathInfo);
    }

    public BufferedImage getBufferedImage(EmptyTileHandler eth) {
        if (eth != null) {

            LatLonPoint center = mtcTransform.tileUVToLatLon(new Point2D.Double(x + .5, y + .5), zoomLevel, new LatLonPoint.Double());
            Mercator merc = new Mercator(center, mtcTransform.getScaleForZoom(zoomLevel), SimpleEmptyTileHandler.TILE_SIZE, SimpleEmptyTileHandler.TILE_SIZE);
            logger.fine("going to create empty tile: " + pathInfo + " from "
                    + eth.getClass().getName());

            return eth.getImageForEmptyTile(pathInfo, x, y, zoomLevel, getMtcTransform(), merc);
        }
        return null;
    }

    public MapTileCoordinateTransform getMtcTransform() {
        if (mtcTransform == null) {
            mtcTransform = new OSMMapTileCoordinateTransform();
        }
        return mtcTransform;
    }

    public void setMtcTransform(MapTileCoordinateTransform mtcTransform) {
        this.mtcTransform = mtcTransform;
    }
}