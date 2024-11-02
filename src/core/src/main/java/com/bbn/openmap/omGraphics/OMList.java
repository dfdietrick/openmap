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

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.ComponentFactory;
import com.bbn.openmap.util.Debug;

/**
 * This class encapsulates a List of OMGraphics.
 * <p>
 * There are several things that this list does that make it better that any ol'
 * List. You can make several common OMGraphic modification calls on the list,
 * and the list handles the iteration and changing of all the graphics while
 * taking into account a traverse order.
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
 * 
 * @param <T> extends OMGeometry
 */
public abstract class OMList<T extends OMGeometry> extends OMGraphicAdapter implements List<T>,
        OMGraphic {

    /**
     * Used to set the order in which the list is traversed to draw or search
     * the objects. This means that the last things on the list will be on top
     * of the map because they are drawn last, on top of everything else. For
     * searches, objects added last to the list will be considered first for a
     * search match.
     */
    public final transient static int LAST_ADDED_ON_TOP = 0;

    /**
     * Used to set the order in which the list is traversed to draw or search
     * the objects. This means that the first things on the list will appear on
     * top because they are drawn last, on top of everything else. For searches,
     * objects added first to the list will be considered first for a search
     * match. This is the default mode for the list.
     */
    public final transient static int FIRST_ADDED_ON_TOP = 1;

    /**
     * Used for searches, when OMDist doesn't have a graphic. The index of a
     * null graphic is NONE. If you try to remove or insert a graphic at NONE,
     * an exception will be thrown. If you try to get a graphic at NONE, you'll
     * get null;
     */
    public final static int NONE = -1;

    /**
     * List traversal mode. The default is FIRST_ADDED_ON_TOP.
     */
    protected int traverseMode = FIRST_ADDED_ON_TOP;

    /**
     * Flag to adjust behavior of OMGraphicList for certain queries. If
     * OMGraphicList should act as OMGraphic, the entire list will be treated as
     * one object. Otherwise, the list will act as a pass-through container, and
     * internal OMGraphics will be returned. This applies to distance(),
     * selectClosest(), findClosest(), getOMGraphicThatContains(), etc. This
     * flag becomes really helpful for embedded OMGraphicLists, not so much for
     * top-level OMGraphicLists.
     */
    protected boolean vague = false;

    /**
     * Flag used to allow duplicates in the OMGraphicList. True by default -
     * this prevents the list from doing the extra work for checking for
     * duplicates at addition time.
     */
    protected boolean allowDuplicates = true;

    /**
     * The List that actually contains the the OMGeometry/OMGraphic objects.
     */
    protected final List<T> graphics;

    /**
     * Construct an OMGraphicList.
     */
    public OMList() {
        graphics = Collections.synchronizedList(new ArrayList<T>());
    }

    public OMList(int initialCapacity) {
        graphics = Collections.synchronizedList(new ArrayList<T>(initialCapacity));
    }

    /**
     * OMGraphicList method for returning a simple description of the list. This
     * is really a debugging method.
     */
    public String getDescription() {
        return getDescription(0);
    }

    /**
     * OMGraphic method, for returning a simple description if the contents of
     * the list. This method handles the spacing of sub-member descriptions.
     * This is really a debugging method.
     * 
     * @return String that represents the structure of the OMGraphicList.
     */
    public String getDescription(int level) {
        StringBuffer sb = new StringBuffer();

        if (level > 0) {
            sb.append("|--> ");
        }

        sb.append("OMList with ").append(size()).append(" object").append((size() == 1 ? "\n"
                : "s\n"));

        synchronized (graphics) {
            StringBuffer sb1 = new StringBuffer();

            for (int i = 0; i < level; i++) {
                sb1.append("     ");
            }
            String spacer = sb1.toString();

            String levelHeader = level == 0 ? "" : "|--> ";

            for (OMGeometry omg : graphics) {
                String description = "";
                if (omg instanceof OMList<?>) {
                    description = ((OMList<? extends OMGeometry>) omg).getDescription(level + 1);
                } else {
                    description = levelHeader + omg.getDescription();
                }

                sb.append(spacer).append(description).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Set whether the list returns the specific OMGraphic in response to a
     * query, or itself.
     */
    public void setVague(boolean value) {
        vague = value;
    }

    /**
     * Get whether the list returns the specific OMGraphic in response to a
     * query, or itself.
     */
    public boolean isVague() {
        return vague;
    }

    public Iterator<T> iterator() {
        return getCopy().iterator();
    }

    public ListIterator<T> listIterator() {
        return getCopy().listIterator();
    }

    public ListIterator<T> listIterator(int size) {
        return getCopy().listIterator(size);
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return graphics.subList(fromIndex, toIndex);
    }

    public int size() {
        return graphics.size();
    }

    public boolean isEmpty() {
        return graphics.isEmpty();
    }

    public void clear() {
        graphics.clear();
    }

    public int indexOf(Object o) {
        return graphics.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return graphics.lastIndexOf(o);
    }

    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object o : c) {
            if (remove(o)) {
                ret = true;
            }
        }
        return ret;
    }

    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        for (Object o : c) {
            if (!contains(o)) {
                remove(o);
                ret = true;
            }
        }
        return ret;
    }

    public boolean contains(Object o) {
        boolean ret = false;
        if (o != null) {
            synchronized (graphics) {
                for (T omg : graphics) {

                    if (o == omg
                            || (omg instanceof OMList<?> && ((OMList<? extends OMGeometry>) omg).contains(o))) {

                        ret = true;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an OMGraphic to the list.
     */
    public boolean add(T g) {
        checkForDuplicate(g);
        return graphics.add(g);
    }

    public void add(int index, T g) {
        checkForDuplicate(g);
        graphics.add(index, g);
    }

    public Object[] toArray() {
        return graphics.toArray();
    }

    public <E> E[] toArray(E[] a) {
        return graphics.toArray(a);
    }

    /**
     * Set the order in which the list is traversed to draw or search the
     * objects. The possible modes for the list are FIRST_ADDED_ON_TOP or
     * LAST_ADDED_ON_TOP.
     * 
     * @param mode traversal mode
     */
    public void setTraverseMode(int mode) {
        traverseMode = mode;
    }

    /**
     * Get the order in which the list is traversed to draw or search the
     * objects. The possible modes for the list are FIRST_ADDED_ON_TOP or
     * LAST_ADDED_ON_TOP.
     * 
     * @return int traversal mode
     */
    public int getTraverseMode() {
        return traverseMode;
    }

    /**
     * Remove the graphic. If this list is not vague, it will also ask
     * sub-OMGraphicLists to remove it if the geometry isn't found on this
     * OMGraphicList.
     * 
     * @param geometry the object to remove.
     * @return true if geometry was on the list, false if otherwise.
     */
    public boolean remove(Object geometry) {
        boolean found = false;

        synchronized (graphics) {
            found = graphics.remove(geometry);

            if (!found && !isVague()) {
                for (OMGeometry graphic : graphics) {
                    if (graphic instanceof OMList<?>) {
                        found = ((OMList<? extends OMGeometry>) graphic).remove(geometry);
                    }
                }
            }
        }

        return found;
    }

    public T remove(int index) {
        return graphics.remove(index);
    }

    /**
     * @return an unmodifiable copy of this list.
     */
    public final List<T> getCopy() {
        final List<T> listCopy;
        listCopy = new ArrayList<T>(graphics);
        return Collections.unmodifiableList(listCopy);
    }

    /**
     * Moves the graphic at the given index to the part of the list where it
     * will be drawn on top of one of the other graphics which is its neighbor
     * on the list. This method does check to see what the traverseMode of the
     * list is, and calls either moveIndexedToLast or moveIndexedToFirst,
     * depending on what is appropriate.
     * 
     * @param location the index location of the graphic to move.
     * @see #moveIndexedOneToFront(int)
     * @see #moveIndexedOneToBack(int)
     */
    public void moveIndexedOneToTop(int location) {
        if (traverseMode == FIRST_ADDED_ON_TOP) {
            moveIndexedOneToFront(location);
        } else {
            moveIndexedOneToBack(location);
        }
    }

    /**
     * Moves the graphic at the given index to the part of the list where it
     * will be drawn on top of the other graphics. This method does check to see
     * what the traverseMode of the list is, and calls either moveIndexedToLast
     * or moveIndexedToFirst, depending on what is appropriate.
     * 
     * @param location the index location of the graphic to move.
     */
    public void moveIndexedToTop(int location) {
        if (traverseMode == FIRST_ADDED_ON_TOP) {
            moveIndexedToFirst(location);
        } else {
            moveIndexedToLast(location);
        }
    }

    /**
     * Moves the graphic at the given index to the part of the list where it
     * will be drawn under one of the other graphics, its neighbor on the list.
     * This method does check to see what the traverseMode of the list is, and
     * calls either moveIndexedOneToBack or moveIndexedOneToFront, depending on
     * what is appropriate.
     * 
     * @param location the index location of the graphic to move.
     * @see #moveIndexedOneToFront(int)
     * @see #moveIndexedOneToBack(int)
     */
    public void moveIndexedOneToBottom(int location) {
        if (traverseMode == FIRST_ADDED_ON_TOP) {
            moveIndexedOneToBack(location);
        } else {
            moveIndexedOneToFront(location);
        }
    }

    /**
     * Moves the graphic at the given index to the part of the list where it
     * will be drawn under all of the other graphics. This method does check to
     * see what the traverseMode of the list is, and calls either
     * moveIndexedToLast or moveIndexedToFirst, depending on what is
     * appropriate.
     * 
     * @param location the index location of the graphic to move.
     */
    public void moveIndexedToBottom(int location) {
        if (traverseMode == FIRST_ADDED_ON_TOP) {
            moveIndexedToLast(location);
        } else {
            moveIndexedToFirst(location);
        }
    }

    /**
     * Moves the graphic at the given index to the front of the list, sliding
     * the other graphics back on in the list in order. If the location is
     * already at the beginning or beyond the end, nothing happens.
     * 
     * @param location the index of the graphic to move.
     * @see #moveIndexedToBottom(int)
     * @see #moveIndexedToTop(int)
     */
    public void moveIndexedToFirst(int location) {
        int listSize = size();
        if (location > 0 && location < listSize) {
            T tmpGraphic = get(location);
            for (int i = location; i > 0; i--) {
                set(i, get(i - 1));
            }
            graphics.set(0, tmpGraphic);
        }
    }

    /**
     * Moves the graphic at the given index toward the front of the list by one
     * spot, sliding the other graphic back on in the list in order. If the
     * location is already at the beginning or beyond the end, nothing happens.
     * 
     * @param location the index of the graphic to move.
     */
    public void moveIndexedOneToFront(int location) {
        int listSize = size();
        if (location > 0 && location < listSize) {
            T tmpGraphic = get(location);
            graphics.set(location, get(location - 1));
            graphics.set(location - 1, tmpGraphic);
        }
    }

    /**
     * Moves the graphic at the given index to the end of the list, sliding the
     * other graphics up on in the list in order. If the location is already at
     * the end or less than zero, nothing happens.
     * 
     * @param location the index of the graphic to move.
     * @see #moveIndexedToBottom(int)
     * @see #moveIndexedToTop(int)
     */
    public void moveIndexedToLast(int location) {
        int listSize = size();
        if (location < listSize - 1 && location >= 0) {
            T tmpGraphic = get(location);
            for (int i = location; i < listSize - 1; i++) {
                set(i, get(i + 1));
            }
            graphics.set(listSize - 1, tmpGraphic);
        }
    }

    /**
     * Moves the graphic at the given index toward the back of the list by one
     * spot, sliding the other graphic up on in the list in order. If the
     * location is already at the end or less than zero, nothing happens.
     * 
     * @param location the index of the graphic to move.
     */
    public void moveIndexedOneToBack(int location) {
        int listSize = size();
        if (location < listSize - 1 && location >= 0) {
            T tmpGraphic = get(location);
            graphics.set(location, get(location + 1));
            graphics.set(location + 1, tmpGraphic);
        }
    }

    /**
     * Projects any graphics needing projection. Use this method to project any
     * new or changed OMGraphics before painting. to re-project the whole list,
     * use <code>generate(Projection, boolean)</code> with
     * <code>forceProjectAll</code> set to <code>true</code>. This is the same
     * as calling <code> generate(p, false)</code>
     * 
     * @param p a <code>Projection</code>
     * @see #generate(Projection, boolean)
     */
    public void project(Projection p) {
        generate(p, false);
    }

    /**
     * Projects the OMGraphics on the list. This is the same as calling
     * <code>generate(p, forceProjectAll)</code>.
     * 
     * @param p a <code>Projection</code>
     * @param forceProjectAll if true, all the graphics on the list are
     *        generated with the new projection. If false they are only
     *        generated if getNeedToRegenerate() returns true
     * @see #generate(Projection, boolean)
     */
    public void project(Projection p, boolean forceProjectAll) {
        generate(p, forceProjectAll);
    }

    /**
     * Prepare the graphics for rendering. This is the same as calling
     * <code>project(p, true)</code>.
     * 
     * @param p a <code>Projection</code>
     * @return boolean true
     * @see #generate(Projection, boolean)
     */
    public boolean generate(Projection p) {
        return generate(p, true);
    }

    /**
     * Prepare the graphics for rendering. This must be done before calling
     * <code>render()</code>! This recursively calls generate() on the
     * OMGraphics on the list.
     * 
     * @param p a <code>Projection</code>
     * @param forceProjectAll if true, all the graphics on the list are
     *        generated with the new projection. If false they are only
     *        generated if getNeedToRegenerate() returns true
     * @return true if generation was successful for all objects on list.
     * @see OMGraphic#generate
     * @see OMGraphic#regenerate
     */
    public boolean generate(Projection p, boolean forceProjectAll) {
        boolean ret = true;
        synchronized (graphics) {
            Iterator<T> iterator = iterator();
            // Check forceProjectAll outside the loop for slight
            // performance improvement.
            if (forceProjectAll) {
                while (iterator.hasNext()) {
                    ret &= iterator.next().generate(p);
                }
            } else {
                while (iterator.hasNext()) {
                    ret &= iterator.next().regenerate(p);
                }
            }
        }
        return ret;
    }

    /**
     * Renders all the objects in the list a graphics context. This is the same
     * as <code>paint()</code> for AWT components. The graphics are rendered in
     * the order of traverseMode. Any graphics where <code>isVisible()</code>
     * returns false are not rendered.
     * 
     * @param gr the AWT Graphics context
     */
    public void render(Graphics gr) {

        if (isVague() && !isVisible())
            return;

        synchronized (graphics) {
            if (traverseMode == FIRST_ADDED_ON_TOP) {
                ListIterator<? extends OMGeometry> iterator = graphics.listIterator(size());
                while (iterator.hasPrevious()) {
                    OMGeometry graphic = iterator.previous();
                    if (shouldProcess(graphic)) {
                        graphic.render(gr);
                    }
                }

            } else {
                ListIterator<? extends OMGeometry> iterator = graphics.listIterator();
                while (iterator.hasNext()) {
                    OMGeometry graphic = iterator.next();
                    if (shouldProcess(graphic)) {
                        graphic.render(gr);
                    }
                }
            }
        }

        renderLabel(gr);
    }

    /**
     * Renders all the objects in the list a graphics context, in their
     * 'selected' mode. This is the same as <code>paint()</code> for AWT
     * components. The graphics are rendered in the order of traverseMode. Any
     * graphics where <code>isVisible()</code> returns false are not rendered.
     * All of the graphics on the list are returned to their deselected state.
     * 
     * @param gr the AWT Graphics context
     */
    public void renderAllAsSelected(Graphics gr) {

        synchronized (graphics) {
            if (traverseMode == FIRST_ADDED_ON_TOP) {
                ListIterator<? extends OMGeometry> iterator = graphics.listIterator(size());
                while (iterator.hasPrevious()) {
                    OMGeometry graphic = iterator.previous();
                    if (shouldProcess(graphic)) {
                        graphic.select();
                        graphic.render(gr);
                        graphic.deselect();
                    }
                }

            } else {
                ListIterator<? extends OMGeometry> iterator = graphics.listIterator();
                while (iterator.hasNext()) {
                    OMGeometry graphic = iterator.next();
                    if (shouldProcess(graphic)) {
                        graphic.select();
                        graphic.render(gr);
                        graphic.deselect();
                    }
                }
            }
        }
    }

    /**
     * Override flag for shouldProcess method. The setting will override the
     * OMGraphicList from using the OMGraphic's visibility settings in
     * determining which OMGraphics should be used in different distance,
     * generate and render methods.
     */
    protected boolean processAllGeometries = true;

    /**
     * This method is called internally for those methods where skipping
     * invisible OMGeometries would save processing time and effort. If you
     * don't want visibility to be considered when processing
     * OMGeometries/OMGraphics, override this method and return true.
     */
    protected boolean shouldProcess(OMGeometry graphic) {
        return processAllGeometries || graphic.isVisible();
    }

    /**
     * Set the programmatic override for shouldProcess method to always process
     * geometries.
     */
    public void setProcessAllGeometries(boolean set) {
        processAllGeometries = set;
    }

    /**
     * Get the settings for the programmatic override for shouldProcess method
     * to always process geometries.
     */
    public boolean getProcessAllGeometries() {
        return processAllGeometries;
    }

    /**
     * Finds the distance to the closest OMGraphic.
     * 
     * @param x x coordinate
     * @param y y coordinate
     * @return float distance
     * @see #findClosest(double, double, float)
     */
    public float distance(double x, double y) {
        return findClosest(x, y, Float.MAX_VALUE, false).d;
    }

    /**
     * RetVal for closest object/distance calculations.
     */
    protected static class OMDist<T> {
        public T omg = null;
        public float d = Float.POSITIVE_INFINITY;
        public int index = NONE; // unknown

        public String toString() {
            return "OMDist: omg=" + (omg == null ? "null" : omg.getClass().getName()) + ", d=" + d
                    + ", index=" + index;
        }
    }

    protected abstract OMDist<T> createDist();

    /**
     * Find the closest Object and its distance. The search is always conducted
     * from the topmost graphic to the bottom-most, depending on the
     * traverseMode.
     * 
     * @param x x coordinate
     * @param y y coordinate
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @param resetSelect deselect any OMGraphic touched.
     * @return OMDist
     */
    public OMDist<T> findClosest(double x, double y, float limit, boolean resetSelect) {
        OMDist<T> omd = new OMDist<T>();
        OMDist<T> tomd;
        int i;

        synchronized (graphics) {
            if (!isEmpty()) {
                if (traverseMode == FIRST_ADDED_ON_TOP) {
                    i = 0;
                    ListIterator<T> iterator = graphics.listIterator();
                    while (iterator.hasNext()) {
                        tomd = findClosestTest(omd, i++, iterator.next(), x, y, limit, resetSelect);
                        if (tomd == null)
                            continue;
                        omd = tomd; // for style
                        if (omd.d == 0)
                            break;
                    }
                } else {
                    i = size();
                    ListIterator<T> iterator = graphics.listIterator(i);
                    while (iterator.hasPrevious()) {
                        tomd = findClosestTest(omd, i--, iterator.previous(), x, y, limit, resetSelect);
                        if (tomd == null)
                            continue;
                        omd = tomd; // for style
                        if (omd.d == 0)
                            break;
                    }
                }
            }

        }
        if (Debug.debugging("omgraphics")) {
            int size = size();
            if (omd.omg != null && isVague()) {
                omd.omg = (T) this;
                Debug.output(this.getClass().getName() + "(" + size
                        + ") detecting hit and vagueness, returning " + omd);
            } else if (omd.omg != null && !isVague()) {
                Debug.output(this.getClass().getName() + "(" + size
                        + ") detecting hit, no vagueness, returning contained " + omd);
            } else {
                Debug.output(this.getClass().getName() + "(" + size + ") omd.omg "
                        + (omd.omg == null ? "== null" : "!= null"));
            }
        }

        return omd;
    }

    /**
     * Test the graphic distance away from the x, y point, and compare it to the
     * current OMDist passed in. If the graphic is the new closest, return the
     * same OMDist object filled in with the new value. Otherwise, return null.
     * 
     * @param current the OMDist that contains the current best result of a
     *        search.
     * @param index the index in the graphic list of the provided OMGraphic
     * @param graphic the OMGraphic to test
     * @param x the window horizontal pixel value.
     * @param y the window vertical pixel value.
     * @param resetSelect flag to call deselect on any OMGraphic contacted. Used
     *        here to pass on in case the OMGraphic provided is an
     *        OMGraphicList, and to use to decide if deselect should be called
     *        on the provided graphic.
     * @return OMDist with an OMGraphic if the graphic passed in is the current
     *         closest. OMDist.graphic could be null, OMDist.d could be
     *         Infinity.
     */
    protected OMDist<T> findClosestTest(OMDist<T> current, int index, OMGeometry graphic, double x,
                                        double y, float limit, boolean resetSelect) {

        if (current == null) {
            current = createDist();
        }

        OMList<? extends OMGeometry> omgl;
        float currentDistance = Float.MAX_VALUE;

        // cannot select a graphic which isn't visible
        if (!shouldProcess(graphic)) {
            return current;
        }

        if (graphic instanceof OMList<?>) {
            omgl = (OMList<T>) graphic;
            OMDist<T> dist = (OMDist<T>) omgl.findClosest(x, y, limit, resetSelect);
            if (dist.omg != null) {
                currentDistance = dist.d;
                graphic = dist.omg;
            }
        } else {
            if (resetSelect)
                graphic.deselect();
            currentDistance = graphic.distance(x, y);
        }

        if (currentDistance < limit && currentDistance < current.d) {
            if (!isVague()) {
                current.omg = (T) graphic;
            } else {
                current.omg = (T) this;
            }
            current.index = index;
            current.d = currentDistance;
        }

        return current;
    }

    /**
     * Finds the object located the closest to the point, if the object distance
     * away is within the limit. The search is always conducted from the topmost
     * graphic to the bottom-most, depending on the traverseMode. Any graphics
     * where <code>isVisible()</code> returns false are not considered.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @return OMGraphic the closest on the list within the limit, or null if
     *         not found.
     */
    public T findClosest(double x, double y, float limit) {
        return findClosest(x, y, limit, false).omg;
    }

    /**
     * Find all of the OMGraphics on this list that are located within the pixel
     * limit of the x, y pixel location.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @return OMGraphicList containing all of the OMGraphics within the limit.
     */
    public OMList<T> findAll(int x, int y, float limit) {
        return findAll(x, y, limit, false, null);
    }

    /**
     * Find all of the OMGraphics on this list that are located within the pixel
     * limit of the x, y pixel location.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @param resetSelect call deselect on OMGraphics not within limit.
     * @param addTo OMGraphicList to add found OMGraphics to, if null a list
     *        will be created.
     * @return OMGraphicList containing all of the OMGraphics within the limit,
     *         empty if none are found.
     */
    public OMList<T> findAll(int x, int y, float limit, boolean resetSelect,
                                          OMList<T> addTo) {
        if (addTo == null) {
            addTo = create();
        }

        OMDist<T> omd = createDist();
        if (!isEmpty()) {
            synchronized (graphics) {

                if (traverseMode == FIRST_ADDED_ON_TOP) {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator();
                    while (iterator.hasNext()) {
                        if (!findAllTest(x, y, limit, resetSelect, addTo, iterator.next(), omd)) {
                            break;
                        }
                    }
                } else {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator(size());
                    while (iterator.hasPrevious()) {
                        if (!findAllTest(x, y, limit, resetSelect, addTo, iterator.previous(), omd)) {
                            break;
                        }

                    }
                }
            }
        }

        if (Debug.debugging("omgraphics")) {
            Debug.output(this.getClass().getName() + "(" + size()
                    + ") detecting hits and vagueness, returning list with " + addTo.size()
                    + " graphics.");
        }

        return addTo;
    }

    public abstract OMList<T> create();

    /**
     * Test to find out if an OMGraphic is located within the pixel limit of the
     * x, y pixel location.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @param resetSelect call deselect on OMGraphic not within limit.
     * @param addTo OMGraphicList to add found OMGeometries to, if null a list
     *        will be created.
     * @param geometry OMGraphic to test.
     * @param omd OMDist to use for test, provided to avoid recurring memory
     *        allocations for loops.
     * @return true of this method should still be called again in a loop, false
     *         of this list is vague and we have a hit.
     */
    protected boolean findAllTest(int x, int y, float limit, boolean resetSelect, OMList<T> addTo,
                                  OMGeometry geometry, OMDist<T> omd) {

        if (geometry instanceof OMList<?>) {
            OMList<T> tempList = create();
            ((OMList<T>) geometry).findAll(x, y, limit, resetSelect, tempList);

            if (!tempList.isEmpty()) {
                if (isVague()) {
                    addTo.add((T) this);
                    // Vague with hit, no need to check further on this list...
                    return false;
                } else {
                    addTo.addAll(tempList);
                    // Move on to check next T on this list...
                    return true;
                }
            }

        } else {

            omd = findClosestTest(omd, 0 /* doesn't matter */, geometry, x, y, limit, resetSelect);

            if (omd == null || omd.omg == null) {
                // no hit, but continue testing...
                return true;
            }

            // Measurements passed, add OMGraphic to addTo list and
            // continue
            if (isVague()) {
                addTo.add((T) this);
                return false;
            }

            addTo.add(omd.omg);
            omd.d = Float.MAX_VALUE; // reset for next OMGraphic
            omd.omg = null;
        }
        return true;
    }

    /**
     * Finds the object located the closest to the point, regardless of how far
     * away it is. This method returns null if the list is not valid. The search
     * starts at the first-added graphic. <br>
     * This is the same as calling
     * <code>findClosest(x, y, Float.MAX_VALUE)</code>.
     * 
     * @param x the horizontal pixel position of the window, from the left of
     *        the window.
     * @param y the vertical pixel position of the window, from the top of the
     *        window.
     * @return the closest graphic to the xy window point.
     * @see #findClosest(double, double, float)
     */
    public T findClosest(int x, int y) {
        return findClosest(x, y, Float.MAX_VALUE);
    }

    /**
     * Finds the object located the closest to the point, if the object distance
     * away is within the limit. The search is always conducted from the topmost
     * graphic to the bottom-most, depending on the traverseMode. Any graphics
     * where <code>isVisible()</code> returns false are not considered.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @return index of the closest on the list within the limit, or
     *         OMGraphicList.NONE if not found.
     */
    public int findIndexOfClosest(int x, int y, float limit) {
        return findClosest(x, y, limit, false).index;
    }

    /**
     * Finds the object located the closest to the point, regardless of how far
     * away it is. This method returns null if the list is not valid. The search
     * starts at the first-added graphic. <br>
     * This is the same as calling
     * <code>findClosest(x, y, Float.MAX_VALUE)</code>.
     * 
     * @param x the horizontal pixel position of the window, from the left of
     *        the window.
     * @param y the vertical pixel position of the window, from the top of the
     *        window.
     * @return index of the closest graphic to the xy window point, or
     *         OMGraphicList.NONE if not found.
     * @see #findIndexOfClosest(int, int, float)
     */
    public int findIndexOfClosest(int x, int y) {
        return findClosest(x, y, Float.MAX_VALUE, false).index;
    }

    /**
     * Finds the object located the closest to the coordinates, regardless of
     * how far away it is. Sets the select paint of that object, and resets the
     * paint of all the other objects. The search starts at the first-added
     * graphic.
     * 
     * @param x the x coordinate on the component the graphics are displayed on.
     * @param y the y coordinate on the component the graphics are displayed on.
     * @return the closest OMGraphic on the list, with selected having been
     *         called on that OMGraphics. This OMGraphic will be within the
     *         limit or null if none found. Will return this list if this list
     *         is set to be vague.
     */
    public T selectClosest(int x, int y) {
        return selectClosest(x, y, Float.MAX_VALUE);
    }

    /**
     * Finds the object located the closest to the point, if the object distance
     * away is within the limit, and sets the paint of that graphic to its
     * select paint. It sets the paints to all the other objects to the regular
     * paint. The search starts at the first-added graphic. Any graphics where
     * <code>isVisible()</code> returns false are not considered.
     * 
     * @param x the horizontal pixel position of the window, from the left of
     *        the window.
     * @param y the vertical pixel position of the window, from the top of the
     *        window.
     * @param limit the max distance that a graphic has to be within to be
     *        returned, in pixels.
     * @return the closest OMGraphic on the list, with selected having been
     *         called on that OMGraphics. This OMGraphic will be within the
     *         limit or null if none found. Will return this list if this list
     *         is set to be vague.
     */
    public T selectClosest(int x, int y, float limit) {
        OMDist<T> omd = null;
        OMDist<T> tomd;
        T ret = null;

        // Handle vagueness.
        if (isVague()) {
            omd = findClosest(x, y, limit, true);
            if (omd != null) {
                select();
                return (T) this;
            }
        }

        synchronized (graphics) {
            if (!isEmpty()) {
                if (traverseMode == FIRST_ADDED_ON_TOP) {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator();
                    while (iterator.hasNext()) {
                        tomd = selectClosestTest(omd, 0, iterator.next(), x, y, limit);
                        if (tomd == null)
                            continue;
                        omd = tomd; // for style
                        if (omd.d == 0)
                            break;
                    }
                } else {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator(size());
                    while (iterator.hasPrevious()) {
                        tomd = selectClosestTest(omd, 0, iterator.previous(), x, y, limit);
                        if (tomd == null)
                            continue;
                        omd = tomd; // for style
                        if (omd.d == 0)
                            break;
                    }
                }
            }
        }

        if (omd != null) {
            ret = omd.omg;
        }

        return ret;
    }

    /**
     * A variation on findClosestTest, manages select() and deselect().
     * 
     * @param current the OMDist that contains the current best result of a
     *        search.
     * @param index the index in the graphic list of the provided OMGraphic
     * @param graphic the OMGraphic to test
     * @param x the window horizontal pixel value.
     * @param y the window vertical pixel value.
     * @return OMDist if the graphic passed in is the current closest. OMGraphic
     *         will be set in OMDist and selected(). OMGraphic will be
     *         de-selected if not the closest, and the OMDist will be null. This
     *         method will return this list if it is set to be vague and one of
     *         its children meet the criteria.
     */
    protected OMDist<T> selectClosestTest(OMDist<T> current, int index, OMGeometry graphic, int x,
                                          int y, float limit) {
        if (current == null) {
            current = createDist();
        }

        T oldGraphic = current.omg;
        OMDist<T> ret = findClosestTest(current, index, graphic, x, y, limit, true);

        // Test for the OMDist still holding the same OMGraphicList,
        // which will be the case if this list is vague. The distance
        // will be updated, though.
        if (ret != null && oldGraphic != ret.omg) {
            if (oldGraphic != null) {
                oldGraphic.deselect();
            }
            ret.omg.select();
        }

        return ret;
    }

    /**
     * Finds the first OMGraphic (the one on top) that is under this pixel. If
     * an OMGraphic is an OMGraphicList, its contents will be checked. If that
     * check is successful and OMGraphicList is not vague, its OMGraphic will be
     * returned - otherwise the list will be returned.
     * 
     * @param x the horizontal pixel position of the window, from the left of
     *        the window.
     * @param y the vertical pixel position of the window, from the top of the
     *        window.
     * @return the graphic that contains the pixel, NONE (null) if none are
     *         found.
     */
    public T getContains(int x, int y) {

        T ret = null;

        synchronized (graphics) {

            if (!isEmpty()) {
                if (traverseMode == FIRST_ADDED_ON_TOP) {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator();
                    while (iterator.hasNext()) {
                        OMGeometry graphic = iterator.next();

                        // cannot select a graphic which isn't visible
                        if (!shouldProcess(graphic))
                            continue;

                        if (graphic instanceof OMList<?>) {
                            OMList<? extends OMGeometry> tomgl = (OMList<? extends OMGeometry>) graphic;
                            ret = (T) tomgl.getContains(x, y);
                            if (ret != null) {
                                if (tomgl.isVague()) {
                                    ret = (T) graphic;
                                }
                                break;
                            }
                        } else if (graphic.contains(x, y)) {
                            ret = (T) graphic;
                            break;
                        }
                    }
                } else {
                    ListIterator<? extends OMGeometry> iterator = graphics.listIterator(size());
                    while (iterator.hasPrevious()) {
                        OMGeometry graphic = iterator.previous();

                        // cannot select a graphic which isn't visible
                        if (!shouldProcess(graphic))
                            continue;

                        if (graphic instanceof OMList<?>) {
                            OMList<? extends OMGeometry> tomgl = (OMList<? extends OMGeometry>) graphic;
                            ret = (T) tomgl.getContains(x, y);
                            if (ret != null) {
                                if (tomgl.isVague()) {
                                    ret = (T) graphic;
                                }
                                break;
                            }
                        } else if (graphic.contains(x, y)) {
                            ret = (T) graphic;
                            break;
                        }
                    }
                }
            }
        }
        if (ret != null && this.isVague()) {
            ret = (T) this;
        }

        return ret;
    }

    /**
     * If you call deselect() on an OMGraphicList, it calls deselect() all the
     * graphics it contains, as well as the deselect method on it's super class.
     */
    public void deselect() {
        super.deselect();
        synchronized (graphics) {
            for (OMGeometry omg : graphics) {
                omg.deselect();
            }
        }
    }

    /**
     * Calls select() on all the items on the graphic list, as well as select()
     * on the super class.
     */
    public void select() {
        super.select();
        synchronized (graphics) {
            for (OMGeometry omg : graphics) {
                omg.select();
            }
        }
    }

    /**
     * Perform an action on the provided geometry. If the geometry is not
     * currently on the list, it is added (if the action doesn't say to delete
     * it). If the geometry is null, the list checks for an action to take on
     * the list (deselectAll).
     */
    public void doAction(T graphic, OMAction action) {

        Debug.message("omgl", "OMList.doAction()");

        if (graphic == null) {
            return;
        }

        int i = indexOf(graphic);

        boolean alreadyOnList = (i != -1);

        if (action == null || action.getValue() == 0 && !alreadyOnList) {
            Debug.message("omgl", "OMGraphicList.doAction: adding graphic with null action");
            add(graphic);
            return;
        }

        if (action.isMask(ADD_GRAPHIC_MASK) || action.isMask(UPDATE_GRAPHIC_MASK) && !alreadyOnList) {
            Debug.message("omgl", "OMGraphicList.doAction: adding graphic");
            add(graphic);
        }

        if (action.isMask(DELETE_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: removing graphic");
            remove(graphic);
        }

        if (action.isMask(RAISE_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: raising graphic");
            moveIndexedOneToTop(i);
        }

        if (action.isMask(RAISE_TO_TOP_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: raising graphic to top");
            moveIndexedToTop(i);
        }

        if (action.isMask(LOWER_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: lowering graphic");
            moveIndexedOneToBottom(i);
        }

        if (action.isMask(LOWER_TO_BOTTOM_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: lowering graphic to bottom");
            moveIndexedToBottom(i); // -> Corrected by Jose M. Torres
        }

        if (action.isMask(DESELECTALL_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: deselecting all graphics.");
            deselect();
        }

        if (action.isMask(SORT_GRAPHICS_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: sorting the list");
            sort();
        }

        if (action.isMask(SELECT_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: selecting graphic");
            graphic.select();
        }

        if (action.isMask(DESELECT_GRAPHIC_MASK)) {
            Debug.message("omgl", "OMGraphicList.doAction: deselecting graphic");
            graphic.deselect();
        }
    }

    /**
     * Set the visibility variable. NOTE: <br>
     * This is checked by the OMGraphicList when it iterates through its list
     * for render and gesturing. It is not checked by the internal OMGraphic
     * methods, although maybe it should be...
     * 
     * @param visible boolean
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!isVague()) {
            synchronized (graphics) {
                for (Iterator<? extends OMGeometry> it = iterator(); it.hasNext();) {
                    it.next().setVisible(visible);
                }
            }
        }
    }

    /**
     * Get the visibility variable. For the OMGraphicList, if any part of it is
     * visible, then it is considered visible.
     * 
     * @return boolean
     */
    public boolean isVisible() {
        if (!isVague()) {
            synchronized (graphics) {
                for (T omg : graphics) {
                    if (omg.isVisible()) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return super.isVisible();
        }
    }

    /**
     * Set whether the list will allow duplicate entries added. If not, then the
     * copy will be added, and the previous version removed.
     */
    public void setAllowDuplicates(boolean set) {
        allowDuplicates = set;
    }

    /**
     * Get whether the list will allow duplicate entries added. If not, then the
     * copy will be added, and the previous version removed.
     */
    public boolean getAllowDuplicates() {
        return allowDuplicates;
    }

    /**
     * Convenience function for methods that may add a OMGraphic. Method checks
     * to see if duplicates are allowed, and if they are not, it will remove the
     * OMGraphic from the list. The calling method can be confident that it will
     * be adding a unique OMGraphic. Internal methods that call this method
     * should be synchronized on the graphics list.
     */
    protected void checkForDuplicate(T g) {
        if (!allowDuplicates) {
            // Why check first, just remove it if it's found?!
            // if (graphics.contains(g)) {
            remove(g);
            // }
        }
    }

    /**
     * Checks if an OMGraphic is on this list. Checks sublists, too.
     */
    public boolean contains(OMGraphic g) {
        if (g != null) {
            synchronized (graphics) {
                for (OMGeometry omg : graphics) {
                    if (g == omg
                            || (omg instanceof OMList<?> && ((OMList<? extends OMGeometry>) omg).contains(g))) {

                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * This sort method is a place-holder for OMGraphicList extensions to
     * implement their own particular criteria for sorting an OMGraphicList.
     * Does nothing for a generic OMGraphicList.
     */
    public void sort() {
    }

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
     * You need to make sure that the Generic type of the source matches the
     * generic type of this list. Will fail silently. Not sure if this is the
     * right way to handle it, though.
     */
    public void restore(OMGeometry source) {
        super.restore(source);
        try {
            if (source instanceof OMList<?>) {
                OMList<T> list = (OMList<T>) source;
                for (T omg : list) {
                    T newCopy = (T) ComponentFactory.create(omg.getClass().getName());
                    if (newCopy != null) {
                        newCopy.restore(source);
                    }
                }
            }
        } catch (ClassCastException cce) {
            // Should really check better. Duh.
        }
    }
}