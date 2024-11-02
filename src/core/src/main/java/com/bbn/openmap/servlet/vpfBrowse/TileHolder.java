// **********************************************************************
// <copyright>
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// </copyright>
// **********************************************************************
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/TileHolder.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwCrossTileID;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.layer.vpf.TilingAdapter;
import com.bbn.openmap.layer.vpf.VPFUtil;

/**
 * This class provides easy access to tiled data.
 */
public class TileHolder {
    /** the name of the primitive file */
    private final String fileName;
    /** the current open tile */
    private DcwRecordFile currentTileFile;
    /** the index (into tileStuff) of the current open tile */
    private int curTile;
    /** an array of tile paths */
    private String tileStuff[];
    /** the directory which has tile subdirectories */
    private final File basepath;

    /**
     * Construct a TileHolder. close() should be called when you are
     * done with the object.
     * 
     * @param basepath the directory which has tile subdirectories
     * @param fileName the name of the primitive file
     * @param isTiled if the coverage is tiled or not. If it is,
     *        tiling information is assumed to be located in
     *        $basepath/../tileref
     * @throws FormatException something went wrong
     */
    public TileHolder(File basepath, String fileName, boolean isTiled)
            throws FormatException {
        this.fileName = fileName;
        this.basepath = basepath;
        curTile = -1;
        if (isTiled) {
            tileStuff = doTileRefStuff(basepath);
        } else {
            currentTileFile = new DcwRecordFile(basepath + File.separator
                    + fileName);
        }
    }

    /**
     * Gets a row from a primitive file in a coverage.
     * 
     * @param tileColumn the column index used to get the tile id
     * @param rowColumn the column index used to get the row id
     * @param tileRow the row to retrieve the tile and row IDs from
     * @param retrow the row gotten from the file
     * @return true if the row was fetched, false otherwise
     * @throws FormatException
     */
    public boolean getRow(int tileColumn, int rowColumn, List tileRow,
                          List retrow) throws FormatException {
        int tileId = (tileColumn == -1) ? -1
                : VPFUtil.objectToInt(tileRow.get(tileColumn));
        int rowId = VPFUtil.objectToInt(tileRow.get(rowColumn));
        return getRow(tileId, rowId, retrow);
    }

    public boolean getRow(TilingAdapter ta, List tileRow, List retrow)
            throws FormatException {
        return getRow(ta.getTileId(tileRow), ta.getTilePrimId(tileRow), retrow);
    }

    public boolean getRow(DcwCrossTileID prim, List retrow)
            throws FormatException {
        return getRow(prim.nextTileID, prim.nextTileKey, retrow);
    }

    /**
     * Gets a row from a primitive file in a coverage.
     * 
     * @param tileId the tile identifier
     * @param rowId the row identifier
     * @param retrow the row gotten from the file
     * @return true if the row was fetched, false otherwise
     * @throws FormatException
     */
    public boolean getRow(int tileId, int rowId, List retrow)
            throws FormatException {
        if (rowId <= 0) {
            return false;
        }
        if (tileId != curTile) {
            File joinfile = new File(basepath + File.separator
                    + tileStuff[tileId]);
            close();
            currentTileFile = new DcwRecordFile(joinfile + File.separator
                    + fileName);
            curTile = tileId;
        }
        return currentTileFile.getRow(retrow, rowId);
    }

    /**
     * Closes any related tables.
     */
    public void close() {
        if (currentTileFile != null) {
            currentTileFile.close();
        }
        currentTileFile = null;
    }

    /**
     * Loads tiling information for the coverage.
     * 
     * @param path the path to the coverage. tiling info is in
     *        ../tileref
     * @return an array of tile paths
     */
    private static String[] doTileRefStuff(File path) throws FormatException {
        File pathname = new File(path.getParentFile(), "tileref");
        String faceIDColumnName = null;
        // read fcs to figure out what column in tileref.aft we need
        // to use to
        // read the fbr (face bounding rectangle) table
        File fcsFile = new File(pathname, "fcs");
        if (!fcsFile.canRead()) {
            fcsFile = new File(pathname, "fcs.");
        }
        DcwRecordFile fcs = new DcwRecordFile(fcsFile.toString());
        Vector fcsv = new Vector(fcs.getColumnCount());
        while (fcs.parseRow(fcsv)) {
            String fclass = ((String) fcsv.elementAt(1)).toLowerCase();
            String table1 = ((String) fcsv.elementAt(2)).toLowerCase();
            if ((fclass.equals("tileref")) && (table1.equals("tileref.aft"))) {
                faceIDColumnName = (String) fcsv.elementAt(3);
                break;
            }
        }
        fcs.close();

        if (faceIDColumnName == null) {
            throw new FormatException("no faceIDColumn");
            // won't be able to read the tiling info. abort
        }

        // Okay, we've got info on what column we use from tileref.aft
        // to index
        // into the fbr.
        DcwRecordFile aft = new DcwRecordFile(pathname + File.separator
                + "tileref.aft");
        int faceIDColumn = aft.whatColumn(faceIDColumnName.toLowerCase());
        int tileNameColumn = aft.whatColumn("tile_name");

        if ((faceIDColumn == -1) || (tileNameColumn == -1)) {
            aft.close();
            throw new FormatException("no faceIDColumn");
        }

        Vector aftv = new Vector(aft.getColumnCount());

        // set the array size to record count + 1, to be able to
        // use the tileID as the index into the array
        String containedTiles[] = new String[aft.getRecordCount() + 1];

        int tileid = 1;
        while (aft.parseRow(aftv)) {
            //int fac_num = ((Number) aftv.elementAt(faceIDColumn)).intValue();
            String tilename = (String) aftv.elementAt(tileNameColumn);

            char chs[] = tilename.toCharArray();
            boolean goodTile = false;
            for (int i = 0; i < chs.length; i++) {
                if ((chs[i] != '\\') && (chs[i] != ' ')) {
                    goodTile = true;
                    chs[i] = Character.toLowerCase(chs[i]);
                }
                if (chs[i] == '\\') {
                    chs[i] = File.separatorChar;
                }
            }
            containedTiles[tileid++] = (goodTile) ? new String(chs) : null;
        }
        aft.close();
        return containedTiles;
    }
}
