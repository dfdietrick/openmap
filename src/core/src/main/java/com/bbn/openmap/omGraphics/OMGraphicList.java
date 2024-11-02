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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/omGraphics/OMGraphicList.java,v $
// $RCSfile: OMGraphicList.java,v $
// $Revision: 1.22 $
// $Date: 2009/02/20 17:15:27 $
// $Author: dietrick $
// 
// **********************************************************************

package com.bbn.openmap.omGraphics;

import java.awt.Paint;
import java.awt.TexturePaint;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.bbn.openmap.omGraphics.grid.OMGridGenerator;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;

/**
 * This class encapsulates a List of OMGraphics.
 * <p>
 * There are several things that this list does that make it better that any ol'
 * List. You can make several common OMGraphic modification calls on the list,
 * and the list handles the iteration and changing of all the graphics while
 * taking into account a travese order.
 * <p>
 * An additional benefit is that because the OMGraphicList extends OMGraphic it
 * can contain other instances of OMGraphicList. This way you can manage
 * groupings of graphics, (for instance, an OMGraphicList of OMGraphicLists
 * which each have an OMRaster and OMText).
 * <p>
 * Many methods, such as generate() and findClosest() traverse the items in the
 * GraphicsList recursively. The direction that the list is traversed is
 * controlled by then traverseMode variable. The traverseMode mode lets you set
 * whether the first or last object added to the list (FIRST_ADDED_ON_TOP or
 * LAST_ADDED_ON_TOP) is drawn on top of the list and considered first for
 * searches.
 */
public class OMGraphicList extends OMList<OMGraphic> implements Serializable {

    /**
     * Construct an OMGraphicList.
     */
    public OMGraphicList() {
        this(10);
    }

    /**
     * Construct an OMGraphicList with an initial capacity.
     * 
     * @param initialCapacity the initial capacity of the list
     */
    public OMGraphicList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Construct an OMGraphicList to include a Collection of OMGraphics.
     * 
     * @param c Collection of OMGraphics.
     */
    public OMGraphicList(Collection<OMGraphic> c) {
        graphics.addAll(c);
    }

    /**
     * Returns a iterator of a shallow copy of the current list, to avoid
     * concurrent modification exceptions if the list is being generated or
     * rendered while the list is being reviewed for other reasons.
     */
    public Iterator<OMGraphic> iteratorCopy() {
        return new OMGraphicList(graphics).iterator();
    }

    /**
     * Returns a iterator of a shallow copy of the current list, to avoid
     * concurrent modification exceptions if the list is being generated or
     * rendered while the list is being reviewed for other reasons.
     */
    public ListIterator<OMGraphic> listIteratorCopy() {
        return new OMGraphicList(graphics).listIterator();
    }

    /**
     * Returns a iterator of a shallow copy of the current list, to avoid
     * concurrent modification exceptions if the list is being generated or
     * rendered while the list is being reviewed for other reasons.
     */
    public ListIterator<OMGraphic> listIteratorCopy(int size) {
        return new OMGraphicList(graphics).listIterator(size);
    }

    /**
     * Add an OMGraphic to the list.
     */
    public boolean add(OMGraphic g) {
        checkForDuplicate(g);
        return graphics.add(g);
    }

    /**
     * Set the graphic at the specified location. The OMGraphic must not be
     * null.
     * 
     * @param graphic OMGraphic
     * @param index index of the OMGraphic to return
     * @exception ArrayIndexOutOfBoundsException if index is out-of-bounds
     */
    public void setOMGraphicAt(OMGraphic graphic, int index) {
        graphics.set(index, graphic);
    }

    public OMGraphic getOMGraphicAt(int index) {
        return get(index);
    }

    /**
     * Get the geometry at the location number on the list.
     * 
     * @param location the location of the OMGraphic to return
     * @return OMGraphic or null if location &gt; list size
     * @exception ArrayIndexOutOfBoundsException if <code>location &lt; 0</code>
     *            or <code>location &gt;=
     * this.size()</code>
     */
    public OMGraphic get(int location) {
        return graphics.get(location);
    }

    /**
     * Set the stroke of all the graphics on the list.
     * 
     * @param stroke the stroke object to use.
     */
    public void setStroke(java.awt.Stroke stroke) {
        super.setStroke(stroke);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setStroke(stroke);
            }
        }
    }

    /**
     * Set the fill paint for all the objects on the list.
     * 
     * @param paint java.awt.Paint
     */
    public void setFillPaint(Paint paint) {
        super.setFillPaint(paint);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setFillPaint(paint);
            }
        }
    }

    /**
     * Set the texture mask for the OMGraphics on the list. If not null, then it
     * will be rendered on top of the fill paint. If the fill paint is clear,
     * the texture mask will not be used. If you just want to render the texture
     * mask as is, set the fill paint of the graphic instead. This is really to
     * be used to have a texture added to the graphic, with the fill paint still
     * influencing appearance.
     */
    public void setTextureMask(TexturePaint texture) {
        super.setTextureMask(texture);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setTextureMask(texture);
            }
        }
    }

    /**
     * Set the line paint for all the objects on the list.
     * 
     * @param paint java.awt.Paint
     */
    public void setLinePaint(Paint paint) {
        super.setLinePaint(paint);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setLinePaint(paint);
            }
        }
    }

    /**
     * Set the selection paint for all the objects on the list.
     * 
     * @param paint java.awt.Paint
     */
    public void setSelectPaint(Paint paint) {
        super.setSelectPaint(paint);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setSelectPaint(paint);
            }
        }
    }

    /**
     * Set the matting paint for all the objects on the list.
     * 
     * @param paint java.awt.Paint
     */
    public void setMattingPaint(Paint paint) {
        super.setMattingPaint(paint);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setMattingPaint(paint);
            }
        }
    }

    /**
     * Set the matting flag for all the objects on the list.
     */
    public void setMatted(boolean value) {
        super.setMatted(value);
        synchronized (graphics) {
            for (OMGraphic omg : graphics) {
                omg.setMatted(value);
            }
        }
    }

    /**
     * Goes through the list, finds the OMGrid objects, and sets the generator
     * for all of them. If a projection is passed in, the generator will be used
     * to create a displayable graphic within the grid.
     * 
     * @param generator an OMGridGenerator to create a renderable graphic from
     *        the OMGrid.
     * @param proj a projection to use to generate the graphic. If null, the
     *        generator will create a renderable graphic the next time a
     *        projection is handed to the list.
     */
    public void setGridGenerator(OMGridGenerator generator,
                                              Projection proj) {
        synchronized (graphics) {
            for (OMGraphic graphic : graphics) {
                if (graphic instanceof OMGrid) {
                    ((OMGrid) graphic).setGenerator(generator);
                    if (proj != null) {
                        graphic.generate(proj);
                    }
                }
            }
        }
    }

    /**
     * Get a reference to the graphics vector. This method is meant for use by
     * methods that need to iterate over the graphics vector, or make at least
     * two invocations on the graphics vector.
     * <p>
     * HACK this method should either return a clone of the graphics list or a
     * quick reference. Currently it returns the latter for simplicity and minor
     * speed improvement. We should allow a way for the user to set the desired
     * behavior, depending on whether they want responsibility for list
     * synchronization. Right now, the user is responsible for synchronizing the
     * OMGraphicList if it's being used in two or more threads...
     * 
     * @return a reference of the graphics List.
     */
    public List<OMGraphic> getTargets() {
        return graphics;
    }

    /**
     * Set the List used to hold the OMGraphics. The OMGraphicList assumes that
     * this list contains OMGraphics. Make *SURE* this is the case. The
     * OMGraphicList will behave badly if there are non-OMGraphics on the list.
     * 
     * @param list the list of OMGraphics to replace all current OMGraphics.
     */
    public void setTargets(List<OMGraphic> list) {
        graphics.clear();
        if (list != null) {
            graphics.addAll(list);
        }
    }

    /**
     * Read a cache of OMGraphics, given an URL.
     * 
     * @param cacheURL URL of serialized graphic list.
     */
    public void readGraphics(URL cacheURL) throws IOException {

        try {
            ObjectInputStream objstream = new ObjectInputStream(cacheURL.openStream());

            if (Debug.debugging("omgraphics")) {
                Debug.output("OMGraphicList: Opened " + cacheURL.toString());
            }

            readGraphics(objstream);
            objstream.close();

            if (Debug.debugging("omgraphics")) {
                Debug.output("OMGraphicList: closed " + cacheURL.toString());
            }

        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new com.bbn.openmap.util.HandleError(aioobe);
        } catch (ClassCastException cce) {
            cce.printStackTrace();
        }
    }

    /**
     * Read a cache of OMGraphics, given a ObjectInputStream.
     * 
     * @param objstream ObjectInputStream of graphic list.
     */
    public void readGraphics(ObjectInputStream objstream)
            throws IOException {

        Debug.message("omgraphics", "OMGraphicList: Reading cached graphics");

        try {
            while (true) {
                try {
                    OMGraphic omg = (OMGraphic) objstream.readObject();
                    graphics.add(omg);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (OptionalDataException ode) {
                    ode.printStackTrace();
                }
            }
        } catch (EOFException e) {
        }
    }

    /**
     * Write the graphics out to a file
     * 
     * @param graphicsSaveFile
     */
    public void writeGraphics(String graphicsSaveFile) throws IOException {

        FileOutputStream ostream = new FileOutputStream(graphicsSaveFile);
        ObjectOutputStream objectstream = new ObjectOutputStream(ostream);
        writeGraphics(objectstream);
        objectstream.close();
    }

    /**
     * Write the graphics out to a ObjectOutputStream
     * 
     * @param objectstream ObjectOutputStream
     */
    public void writeGraphics(ObjectOutputStream objectstream)
            throws IOException {

        synchronized (graphics) {
            for (Iterator<OMGraphic> it = iterator(); it.hasNext();) {
                OMGraphic g = it.next();
                try {
                    objectstream.writeObject(g);
                } catch (IOException e) {
                    Debug.error("OMGraphicList: Couldn't write object " + g
                            + "\nOMGraphicList: Reason: " + e.toString());
                }
            }
        }
        objectstream.close();
    }

    /**
     * This sort method is a place-holder for OMGraphicList extensions to
     * implement their own particular criteria for sorting an OMGraphicList.
     * Does nothing for a generic OMGraphicList.
     */
    public void sort() {}

    /**
     * Convenience method to cast an object to an OMGraphic if it is one.
     * Returns null if it isn't.
     */
    protected OMGraphic objectToOMGraphic(Object obj) {
        if (obj instanceof OMGraphic) {
            return (OMGraphic) obj;
        } else {
            return null;
        }
    }

    /**
     * @return a duplicate list full of shallow copies of each of the OMGraphics
     *         contained on the list.
     */
    public Object clone() {
        OMGraphicList omgl = (OMGraphicList) super.clone();        
        synchronized(graphics){
        for (OMGraphic omg : graphics) {
            // If the OMGraphic doesn't provide a copy (providing a
            // SinkGraphic instead), oh well.
            if (omg instanceof OMGraphicList) {
                omgl.add((OMGraphic) ((OMGraphicList) omg).clone());
            } else {
                omgl.graphics.add(omg);
            }
        }
        }
        return omgl;
    }

    @Override
    public OMList<OMGraphic> create() {
        return new OMGraphicList();
    }

    @Override
    protected com.bbn.openmap.omGraphics.OMList.OMDist<OMGraphic> createDist() {
        return new OMDist<OMGraphic>();
    }

    public void add(int index, OMGraphic element) {
        setNeedToRegenerate(true);
        super.add(index, element);
    }

    public boolean addAll(Collection<? extends OMGraphic> c) {
        setNeedToRegenerate(true);
        return graphics.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends OMGraphic> c) {
        setNeedToRegenerate(true);
        return graphics.addAll(index, c);
    }

    public OMGraphic set(int index, OMGraphic element) {
        setNeedToRegenerate(true);
        return graphics.set(index, element);
    }

    /**
     * Remove the geometry at the location number.
     * 
     * @param location the location of the OMGeometry to remove
     */
    public OMGraphic remove(int location) {
        OMGraphic obj = super.remove(location);
        if (obj != null) {
            setNeedToRegenerate(true);
        }
        return obj;
    }
}