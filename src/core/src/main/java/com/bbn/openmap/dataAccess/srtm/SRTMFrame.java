// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/dataAccess/dted/SRTMFrame.java,v $
// $RCSfile: SRTMFrame.java,v $
// $Revision: 1.7 $
// $Date: 2008/02/29 00:51:10 $
// $Author: dietrick $
// 
// **********************************************************************
package com.bbn.openmap.dataAccess.srtm;

import com.bbn.openmap.dataAccess.dted.*;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.bbn.openmap.io.BinaryBufferedFile;
import com.bbn.openmap.io.BinaryFile;
import com.bbn.openmap.io.Closable;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGrid;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.grid.OMGridData;
import com.bbn.openmap.omGraphics.grid.SlopeGenerator;
import com.bbn.openmap.proj.CADRG;
import com.bbn.openmap.proj.Length;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.Debug;

/**
 * The SRTMFrame is the representation of the SRTM (Shuttle Radar Topography
 * Mission) data from a single srtm data file. It keeps track of all the
 * attribute information of its data. It can return an OMGrid object that can be
 * configured to create a visual representation of the data, depending on what
 * OMGridGenerators are used on the OMGrid object.
 */
public class SRTMFrame
        implements Closable {

    public final static int SRTM1_DIMENSION = 3601;
    public final static int SRTM3_DIMENSION = 1201;

    /**
     * The binary buffered file to read the data from the file.
     */
    protected BinaryFile binFile;
    /**
     * The path to the frame, including the frame name.
     */
    protected String path;
    /**
     * The number of rows and values in the x, y dimension.
     */
    int dimension;

    LatLonPoint frameLoc;

    /**
     * The array of elevation posts. Note: the 0 index of the array in both
     * directions is in the lower left corner of the matrix. As you increase
     * indexes in both dimensions, you go up-right.
     */
    protected short[][] elevations; // elevation posts

    /**
     * Validity flag for the quality of the data file.
     */
    public boolean frame_is_valid = false;

    // ////////////////
    // Administrative methods
    // ////////////////
    /**
     * Simplest constructor.
     *
     * @param filePath complete path to the DTED frame.
     */
    public SRTMFrame(String filePath) {
        this(filePath, false);
    }

    /**
     * Constructor with colortable and presentation information.
     *
     * @param filePath complete path to the DTED frame.
     * @param readWholeFile If true, all of the elevation data will be read at
     * load time. If false, elevation post data will be read in per longitude
     * column depending on the need. False is recommended for SRTM level 1 and
     * 2.
     */
    public SRTMFrame(String filePath, boolean readWholeFile) {
        try {

            frameLoc = parseLocation(filePath);

            binFile = new BinaryBufferedFile(filePath);
            dimension = SRTM3_DIMENSION;

            read(binFile, readWholeFile);
            if (readWholeFile) {
                close(true);
            } else {
                BinaryFile.addClosable(this);
            }

        } catch (FileNotFoundException e) {
            Debug.error("SRTMFrame: file " + filePath + " not found");
        } catch (IOException e) {
            Debug.error("SRTMFrame: File IO Error!\n" + e.toString());
        }

        path = filePath;
    }

    /**
     * Reads the DTED frame file. Assumes that the File f is valid/exists.
     *
     * @param binFile the binary buffered file opened on the DTED frame file
     * @param readWholeFile flag controlling whether all the row data is read at
     * this time. Otherwise, the rows are read as needed.
     */
    private void read(BinaryFile binFile, boolean readWholeFile) {
        binFile.byteOrder(true); // boolean msbfirst
        // Allocate just the columns now - we'll do the rows as
        // needed...
        elevations = new short[dimension][];
        if (readWholeFile) {
            readDataRecords();
        }
        frame_is_valid = true;
    }

    private LatLonPoint parseLocation(String filePath) {
        int startIndex = filePath.lastIndexOf('/') + 1;

        String name = filePath.substring(startIndex, startIndex + 7).toUpperCase();
        int sign = name.charAt(0) == 'S' ? -1 : 1;
        double lat = Double.parseDouble(name.substring(1, 3)) * sign;
        sign = name.charAt(3) == 'W' ? -1 : 1;
        double lon = Double.parseDouble(name.substring(4)) * sign;

        LatLonPoint loc = new LatLonPoint.Double(lat, lon);
        return loc;
    }

    public LatLonPoint getLocation() {
        return frameLoc;
    }

    /**
     * This must get called to break a reference cycle that prevents the garbage
     * collection of frames.
     */
    public void dispose() {
        // System.out.println("DTED Frame Disposed " + me);
        this.close(true);
        BinaryFile.removeClosable(this);
    }

    /**
     * Part of the Closable interface. Closes the BinaryFile pointer, because
     * someone else needs another file open, and the system needs a file
     * pointer. Sets the binFile variable to null.
     */
    public boolean close(boolean done) {
        try {
            if (binFile != null) {
                binFile.close();
                binFile = null;
            }
            return true;
        } catch (IOException e) {
            Debug.error("SRTMFrame close(): File IO Error!\n" + e.toString());
            return false;
        }
    }

    /**
     * If the BinaryBufferedFile was closed, this method attempts to reopen it.
     *
     * @return true if the opening was successful.
     */
    protected boolean reopen() {
        try {
            binFile = new BinaryBufferedFile(path);
            return true;
        } catch (FileNotFoundException e) {
            Debug.error("SRTMFrame reopen(): file " + path + " not found");
            return false;
        } catch (IOException e) {
            Debug.error("SRTMFrame close(): File IO Error!\n" + e.toString());
            return false;
        }
    }

    // ////////////////
    // These functions can be called from the outside,
    // as queries about the data
    // ////////////////
    /**
     * The elevation at the closest SW post to the given lat/lon. This is just a
     * go-to-the-closest-post solution.
     *
     * @param lat latitude in decimal degrees.
     * @param lon longitude in decimal degrees.
     * @return elevation at lat/lon in meters.
     */
    public int elevationAt(float lat, float lon) {
        if (frame_is_valid == true) {
            LatLonPoint loc = getLocation();
            double fLatitude = loc.getLatitude();
            double fLongitude = loc.getLongitude();
            if (lat >= fLatitude && lat <= fLatitude + 1.0 && lon >= fLongitude && lon <= fLongitude + 1.0) {

                // lat/lon_post_intervals are *10 too big -
                // extra 0 in 36000 to counteract
                int lat_index = (int) Math.round((lat - fLatitude) * (dimension - 1));
                int lonIndex = (int) Math.round((lon - fLongitude) * (dimension - 1));

                if (elevations[lonIndex] == null) {
                    readDataRecord(lonIndex);
                }

                return (int) elevations[lonIndex][lat_index];
            }
        }
        return -32767; // Considered a null elevation value
    }

    /**
     * Interpolated elevation at a given lat/lon - should be more precise than
     * elevationAt(), but that depends on the resolution of the data.
     *
     * @param lat latitude in decimal degrees.
     * @param lon longitude in decimal degrees.
     * @return elevation at lat/lon in meters.
     */
    public int interpElevationAt(float lat, float lon) {
        if (frame_is_valid == true) {
            LatLonPoint loc = getLocation();
            double fLatitude = loc.getLatitude();
            double fLongitude = loc.getLongitude();
            if (lat >= fLatitude && lat <= fLatitude + 1.0 && lon >= fLongitude && lon <= fLongitude + 1.0) {

                // lat/lon_post_intervals are *10 too big -
                // extra 0 in 36000 to counteract
                int numIndexes = dimension - 1;
                double latIndex = (lat - fLatitude) * numIndexes;
                double lonIndex = (lon - fLongitude) * numIndexes;

                int lflonIndex = (int) Math.floor(lonIndex);
                int lclonIndex = (int) Math.ceil(lonIndex);
                /* int lflat_index = (int) Math.floor(lat_index); */
                int lclat_index = (int) Math.ceil(latIndex);

                if (elevations[lflonIndex] == null) {
                    readDataRecord(lflonIndex);
                }
                if (elevations[lclonIndex] == null) {
                    readDataRecord(lclonIndex);
                }

                // ////////////////////////////////////////////////////
                // Print out grid of 20x20 elevations with
                // the "asked for" point being in the middle
                // System.out.println("***Elevation Map***");
                // for(int l = lclat_index + 5; l > lflat_index - 5;
                // l--) {
                // System.out.println();
                // for(int k = lflonIndex - 5; k < lclonIndex + 5;
                // k++) {
                // if (elevations[k]==null) readDataRecord(k);
                // System.out.print(elevations[k][l] + " ");
                // }
                // }
                // System.out.println();System.out.println();
                // ////////////////////////////////////////////////////
                int ul = elevations[lflonIndex][lclat_index];
                int ur = elevations[lclonIndex][lclat_index];
                int ll = elevations[lflonIndex][lclat_index];
                int lr = elevations[lclonIndex][lclat_index];

                double answer = resolveFourPoints(ul, ur, lr, ll, latIndex, lonIndex);
                return (int) Math.round(answer);
            }
        }
        return -32767; // Considered a null elevation value
    }

    /**
     * Return an index of ints representing the starting x, y and ending x, y of
     * elevation posts given a lat lon box. It does check to make sure that the
     * upper lat is larger than the lower, and left lon is less than the right.
     *
     * @param ullat upper latitude in decimal degrees.
     * @param ullon left longitude in decimal degrees.
     * @param lrlat lower latitude in decimal degrees.
     * @param lrlon right longitude in decimal degrees.
     * @return int[4] array of start x, start y, end x, and end y.
     */
    public int[] getIndexesFromLatLons(float ullat, float ullon, float lrlat, float lrlon) {
        float upper = ullat;
        float lower = lrlat;
        float right = lrlon;
        float left = ullon;

        // Since matrix indexes depend on these being in the right
        // order, we'll double check and flip values, just to make
        // sure lower is lower, and higher is higher.
        if (ullon > lrlon) {
            right = ullon;
            left = lrlon;
        }

        if (lrlat > ullat) {
            upper = lrlat;
            lower = ullat;
        }

        int[] ret = new int[4];
        double swLat = frameLoc.getLatitude();
        double swLon = frameLoc.getLongitude();

        double numIndexes = dimension - 1;
        double ullat_index = (upper - swLat) * numIndexes;
        double ullonIndex = (left - swLon) * numIndexes;
        double lrlat_index = (lower - swLat) * numIndexes;
        double lrlonIndex = (right - swLon) * numIndexes;

        ret[0] = (int) Math.round(ullonIndex);
        ret[1] = (int) Math.round(lrlat_index);
        ret[2] = (int) Math.round(lrlonIndex);
        ret[3] = (int) Math.round(ullat_index);

        if (ret[0] < 0) {
            ret[0] = 0;
        }
        if (ret[0] > dimension - 2) {
            ret[0] = dimension - 2;
        }
        if (ret[1] < 0) {
            ret[1] = 0;
        }
        if (ret[1] > dimension - 2) {
            ret[1] = dimension - 2;
        }
        if (ret[2] < 0) {
            ret[2] = 0;
        }
        if (ret[2] > dimension - 2) {
            ret[2] = dimension - 2;
        }
        if (ret[3] < 0) {
            ret[3] = 0;
        }
        if (ret[3] > dimension - 2) {
            ret[3] = dimension - 2;
        }
        return ret;

    }

    /**
     * Return a two dimensional array of posts between lat lons.
     *
     * @param ullat upper latitude in decimal degrees.
     * @param ullon left longitude in decimal degrees.
     * @param lrlat lower latitude in decimal degrees.
     * @param lrlon right longitude in decimal degrees.
     * @return array of elevations in meters. The spacing of the posts depends
     * on the DTED level.
     */
    public short[][] getElevations(float ullat, float ullon, float lrlat, float lrlon) {
        int[] indexes = getIndexesFromLatLons(ullat, ullon, lrlat, lrlon);
        return getElevations(indexes[0], indexes[1], indexes[2], indexes[3]);
    }

    /**
     * Return a two dimensional array of posts between lat lons. Assumes that
     * the indexes are checked to not exceed their bounds as defined in the
     * file. getIndexesFromLatLons() checks this.
     *
     * @param startx starting index (left) of the greater matrix to make the
     * left side of the returned matrix.
     * @param starty starting index (lower) of the greater matrix to make the
     * bottom side of the returned matrix.
     * @param endx ending index (right) of the greater matrix to make the left
     * side of the returned matrix.
     * @param endy ending index (top) of the greater matrix to make the top side
     * of the returned matrix.
     * @return array of elevations in meters. The spacing of the posts depends
     * on the DTED level.
     */
    public short[][] getElevations(int startx, int starty, int endx, int endy) {
        int upper = endy;
        int lower = starty;
        int right = endx;
        int left = startx;

        // Since matrix indexes depend on these being in the right
        // order, we'll double check and flip values, just to make
        // sure lower is lower, and higher is higher.
        if (startx > endx) {
            right = startx;
            left = endx;
        }

        if (starty > endy) {
            upper = starty;
            lower = endy;
        }

        short[][] matrix = new short[right - left + 1][upper - lower + 1];
        int matrixColumn = 0;
        for (int x = left; x <= right; x++) {
            if (elevations[x] == null) {
                readDataRecord(x);
            }
            System.arraycopy(elevations[x], lower, matrix[matrixColumn], 0, (upper - lower + 1));
            matrixColumn++;
        }
        return matrix;
    }

    // ////////////////
    // Internal methods
    // ////////////////
    /**
     * A try at interpolating the corners of the surrounding posts, given a lat
     * lon. Called from a function where the data for the lon has been read in.
     */
    private double resolveFourPoints(int ul, int ur, int lr, int ll, double latIndex, double lonIndex) {
        double top_avg = ((lonIndex - Math.floor(lonIndex)) * (float) (ur - ul)) + ul;
        double bottom_avg = ((lonIndex - Math.floor(lonIndex)) * (float) (lr - ll)) + ll;
        double right_avg = ((latIndex - Math.floor(latIndex)) * (float) (ur - lr)) + lr;
        double left_avg = ((latIndex - Math.floor(latIndex)) * (float) (ul - ll)) / 100.0F + ll;

        double lonAvg = ((latIndex - Math.floor(latIndex)) * (top_avg - bottom_avg)) + bottom_avg;
        double latAvg = ((lonIndex - Math.floor(lonIndex)) * (right_avg - left_avg)) + left_avg;

        double result = (lonAvg + latAvg) / 2.0;
        return result;
    }

    /**
     * Reads one longitude line of posts. Assumes that the binFile is valid.
     *
     * @param lonIndex the column of data to read
     * @return true if the column of data was successfully read
     */
    protected boolean readDataRecord(int lonIndex) {
        try {
            if (binFile == null) {
                if (!reopen()) {
                    return false;
                }
            }

            binFile.seek((lonIndex * (2 * dimension)));
            // Allocate the rows of the row
            elevations[lonIndex] = new short[dimension];
            for (int j = 0; j < dimension; j++) {
                elevations[lonIndex][j] = binFile.readShortData();
            }

        } catch (IOException e3) {
            Debug.error("SRTMFrame.RDR: Error reading file.");
            e3.printStackTrace();
            elevations[lonIndex] = null;
            return false;
        } catch (FormatException f) {
            Debug.error("SRTMFrame.RDR: File IO Format error!");
            elevations[lonIndex] = null;
            return false;
        }
        return true;
    }

    /**
     * Read all the elevation posts, at one time. Assumes that the file is open
     * and ready.
     *
     * @return true if the elevation columns were read.
     */
    protected boolean readDataRecords() {
        boolean ret = true;
        for (int lonIndex = 0; lonIndex < dimension; lonIndex++) {
            if (readDataRecord(lonIndex) == false) {
                ret = false;
            }
        }
        return ret;
    }

    public OMGrid getOMGrid() {
        // vResolution decimal degrees per row
        double resolution = 1.0 / (dimension - 1);

        if (Debug.debugging("grid")) {
            Debug.output("SRTMFrame creating OMGrid with resolution: " + resolution + ", created from:" + frameLoc + ", dimension: " + dimension);
        }

        LatLonPoint fLoc = getLocation();
        double lat = fLoc.getLatitude();
        double lon = fLoc.getLongitude();

        OMDTEDGrid omg
                = new OMDTEDGrid(lat, lon, lat + 1.0, lon + 1.0, (float) resolution, (float) resolution,
                        new OMGridData.Short(elevations));
        omg.setUnits(Length.METER);
        return omg;
    }

    /**
     * If you just want to get an image for the SRTMFrame, then call this. One
     * image in an OMGraphic for the entire SRTMFrame will be returned, with the
     * default rendering parameters (Colored shading) and the default
     * colortable. Use the other getImage method if you want something
     * different. This method actually calls that other method, so read the
     * documentation for that as well.
     *
     * @param proj EqualArc projection to use to create image.
     * @return raster image OMGraphic to display in OpenMap.
     */
    public OMGraphic getImage(Projection proj) {
        OMGrid grid = getOMGrid();
        grid.generate(proj);
        SlopeGenerator sg = new SlopeGenerator();
        return sg.generateRasterForProjection(grid, proj);
    }

    public static void main(String args[]) {
        Debug.init();
        if (args.length < 1) {
            System.out.println("SRTMFrame:  Need a path/filename");
            System.exit(0);
        }

        String fileName = args[0];
        System.out.println("SRTMFrame: " + fileName);
        SRTMFrame df = new SRTMFrame(fileName, true);

        CADRG crg = new CADRG(df.getLocation(), 1500000, 600, 600);

        final OMGraphic ras = df.getImage(crg);

        java.awt.Frame window = new java.awt.Frame(fileName) {
            public void paint(java.awt.Graphics g) {
                if (ras instanceof OMRaster) {
                    OMRaster raster = (OMRaster) ras;
                    g.translate(-100, 100);
                    ras.render(g);
                }
            }
        };

        window.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                // need a shutdown event to notify other gui beans and
                // then exit.
                System.exit(0);
            }
        });

        if (ras instanceof OMRaster) {
            OMRaster raster = (OMRaster) ras;
            System.out.println("Setting window to " + raster.getWidth() + ", " + raster.getHeight());
            window.setSize(raster.getWidth(), raster.getHeight());
        } else {
            window.setSize(250, 250);
        }
        window.setVisible(true);
        window.repaint();
    }
}
