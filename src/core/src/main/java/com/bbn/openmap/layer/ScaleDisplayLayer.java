/* **********************************************************************
 *
 *  ROLANDS & ASSOCIATES Corporation
 *  500 Sloat Avenue
 *  Monterey, CA 93940
 *  (831) 373-2025
 *
 *  Copyright (C) 2002, 2003 ROLANDS & ASSOCIATES Corporation. All rights reserved.
 *  Openmap is a trademark of BBN Technologies, A Verizon Company
 *
 *
 * **********************************************************************
 *
 * $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/ScaleDisplayLayer.java,v $
 * $Revision: 1.9 $
 * $Date: 2005/12/09 21:09:08 $
 * $Author: dietrick $
 *
 * **********************************************************************
 */
package com.bbn.openmap.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.bbn.openmap.omGraphics.DrawingAttributes;
import com.bbn.openmap.omGraphics.OMColor;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.proj.GreatCircle;
import com.bbn.openmap.proj.Length;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.MoreMath;
import com.bbn.openmap.util.PropUtils;
import java.awt.geom.Point2D;
import java.util.logging.Level;

/**
 * The ScaleDisplayLayer draws a scale indicator in the corner of the map,
 * showing a line and displaying its length. You can set the location of the
 * indicator, the colors, and the units being shown.
 * <p>
 *
 * <pre>
 *
 * ### Layer used by the overview handler
 * scaleLayer.class=com.bbn.openmap.layer.ScaleDisplayLayer
 * scaleLayer.prettyName=Scale
 * scaleLayer.lineColor=ff777777
 * scaleLayer.textColor=ff000000
 * scaleLayer.unitOfMeasure=nm
 * scaleLayer.locationXoffset=-10
 * scaleLayer.locationYoffset=-20
 * scaleLayer.width=150
 * scaleLayer.height=10
 *
 * # unitOfMeasure - any com.bbn.openmap.proj.Length instance returned by Length.get(string).
 * # locationXoffset - offset in pixels from left/right, positive from left edge, negative from right edge
 * # locationYoffset - offset in pixels from top/bottom, positive from top edge, negative from bottom edge
 * # width - width of scale indicator bar in pixels
 * # height - height of scale indicator bar in pixels
 *
 * </pre>
 */
public class ScaleDisplayLayer extends OMGraphicHandlerLayer {

    public ScaleDisplayLayer() {
        super();
        setProjectionChangePolicy(new com.bbn.openmap.layer.policy.ListResetPCPolicy(this));
        setUnitOfMeasure(Length.KM.toString());
    }

    protected Logger logger = Logger.getLogger("com.bbn.openmap.layer.ScaleDisplayLayer");

    // Color variables for different line types
    protected java.awt.Color lineColor = null;
    protected java.awt.Color textColor = null;

    // Default colors to use, if not specified in the properties.
    protected String defaultLineColorString = "FFFFFF";
    protected String defaultTextColorString = "FFFFFF";
    protected String defaultUnitOfMeasureString = "km";
    protected int defaultLocationXoffset = -10;
    protected int defaultLocationYoffset = -10;
    protected int defaultWidth = 150;
    protected int defaultHeight = 10;

    // property text values
    public static final String UnitOfMeasureProperty = "unitOfMeasure";
    public static final String LocationXOffsetProperty = "locationXoffset";
    public static final String LocationYOffsetProperty = "locationYoffset";
    public static final String WidthProperty = "width";
    public static final String HeightProperty = "height";

    protected String unitOfMeasure = null;
    protected Length uom = Length.get(defaultUnitOfMeasureString);
    protected String uomAbbr = uom.getAbbr();
    protected int locationXoffset = defaultLocationXoffset;
    protected int locationYoffset = defaultLocationYoffset;
    protected int width = defaultWidth;
    protected int height = defaultHeight;
    public static final float RADIANS_270 = Length.DECIMAL_DEGREE.toRadians(270);
    protected DrawingAttributes dAttributes = DrawingAttributes.getDefaultClone();

    /**
     * Sets the properties for the <code>Layer</code>. This allows
     * <code>Layer</code> s to get a richer set of parameters than the
     * <code>setArgs</code> method.
     *
     * @param prefix the token to prefix the property names
     * @param properties the <code>Properties</code> object
     */
    public void setProperties(String prefix, Properties properties) {
        super.setProperties(prefix, properties);
        prefix = PropUtils.getScopedPropertyPrefix(prefix);

        dAttributes.setProperties(prefix, properties);

        String unitOfMeasureString = properties.getProperty(prefix + UnitOfMeasureProperty);
        if (unitOfMeasureString != null) {
            setUnitOfMeasure(unitOfMeasureString);
        }

        locationXoffset = PropUtils.intFromProperties(properties, prefix + LocationXOffsetProperty,
                defaultLocationXoffset);

        locationYoffset = PropUtils.intFromProperties(properties, prefix + LocationYOffsetProperty,
                defaultLocationYoffset);

        width = PropUtils.intFromProperties(properties, prefix + WidthProperty, defaultWidth);

        height = PropUtils.intFromProperties(properties, prefix + HeightProperty, defaultHeight);
    }

    public Properties getProperties(Properties props) {
        props = super.getProperties(props);
        String prefix = PropUtils.getScopedPropertyPrefix(this);

        dAttributes.setProperties(props);

        props.put(prefix + LocationXOffsetProperty, Integer.toString(locationXoffset));
        props.put(prefix + LocationYOffsetProperty, Integer.toString(locationYoffset));
        props.put(prefix + WidthProperty, Integer.toString(width));
        props.put(prefix + HeightProperty, Integer.toString(height));

        props.put(prefix + UnitOfMeasureProperty, unitOfMeasure);

        return props;
    }

    /**
     * prepare the graphics for the layer
     *
     * @return
     */
    @Override
    public synchronized OMGraphicList prepare() {
        int w, h, left_x = 0, right_x = 0, lower_y = 0, upper_y = 0;
        Projection projection = getProjection();
        OMGraphicList graphics = new OMGraphicList();

        w = projection.getWidth();
        h = projection.getHeight();
        // FIXME: Use the center since it's always real

        /**
         * Since the pixel space for the component has nothing to do with the
         * pixel space of the projection, we'll just use the projection pixel
         * space to find out how long the line should be. Then, we'll move that
         * length into component pixel space.
         */
        lower_y = h / 2;
        right_x = w / 2;
        left_x = right_x - width;

        LatLonPoint loc1 = projection.inverse(left_x, lower_y, new LatLonPoint.Double());
        LatLonPoint loc2 = projection.inverse(right_x, lower_y, new LatLonPoint.Double());

        double dist = GreatCircle.sphericalDistance(loc1.getRadLat(), loc1.getRadLon(), loc2.getRadLat(), loc2.getRadLon());

        // Round the distance to one of the preferred values.
        dist = uom.fromRadians(dist);
        double new_dist = scopeDistance(dist);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "modifying {0} to new distance: {1}", new Object[]{dist, new_dist});
        }

        left_x = getPtAtDistanceFromLatLon(loc2, new_dist, projection, uom);

        int lineLength = right_x - left_x;

        // If the length of the distance line is longer than the width of the
        // panel, divide it in half.
        int maxWidth = Math.max(getWidth() / 4 - Math.abs(locationXoffset) * 2, 50);
        while (lineLength > maxWidth) {

            lineLength /= 3;
            new_dist /= 3.0;

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "length of line too long, halving to [0]", lineLength);
            }
            double testDist = scopeDistance(new_dist);
            if (!MoreMath.approximately_equal(testDist, new_dist) && !(new_dist <= .01)) {
                lineLength = right_x - getPtAtDistanceFromLatLon(loc2, testDist, projection, uom);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "needed to rescope distance to {0} from {1}", new Object[]{testDist, new_dist});
                }
                new_dist = testDist;
            }

        }

        // Now, check the units and try to avoid fractions
        Length cur_uom = uom;

        if (new_dist < 1) {
            if (uom.equals(Length.KM)) {
                new_dist *= 1000;
                cur_uom = Length.METER;
            } else if (uom.equals(Length.MILE) || uom.equals(Length.DM) || uom.equals(Length.NM)) {
                new_dist = Length.FEET.fromRadians(uom.toRadians(new_dist));
                cur_uom = Length.FEET;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "modified UOM to {0}, value: {1}", new Object[]{cur_uom.getAbbr(), new_dist});
            }

            double testDist = scopeDistance(new_dist);
            if (!MoreMath.approximately_equal(testDist, new_dist)) {
                lineLength = right_x
                        - getPtAtDistanceFromLatLon(loc2, testDist, projection, cur_uom);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "needed to rescope distance to {0} from {1}", new Object[]{testDist, new_dist});
                }
                new_dist = testDist;
            }
        }

        /**
         * Now, figure out where OMGraphics go in the component space.
         */
        if (locationXoffset < 0) {
            int cw = getWidth();
            left_x = cw + locationXoffset - lineLength;
            right_x = cw + locationXoffset;
        } else if (locationXoffset >= 0) {
            left_x = locationXoffset;
            right_x = locationXoffset + lineLength;
        }
        if (locationYoffset < 0) {
            int ch = getHeight();
            upper_y = ch + locationYoffset - height;
            lower_y = ch + locationYoffset;
        } else if (locationYoffset >= 0) {
            upper_y = locationYoffset;
            lower_y = locationYoffset + height;
        }

        // Draw the lines and the rounded distance string.
        OMLine line = new OMLine(left_x, lower_y, right_x, lower_y);
        dAttributes.setTo(line);
        graphics.add(line);

        line = new OMLine(left_x, lower_y, left_x, upper_y);
        dAttributes.setTo(line);
        graphics.add(line);

        line = new OMLine(right_x, lower_y, right_x, upper_y);
        dAttributes.setTo(line);
        graphics.add(line);

        String outtext = String.format("%.0f %s", new_dist, cur_uom.getAbbr());

        OMText text = new OMText((left_x + right_x) / 2, lower_y - 3, "" + outtext, OMText.JUSTIFY_CENTER);

        Font font = text.getFont();
        text.setFont(font.deriveFont(font.getStyle(), font.getSize() + 2));

        dAttributes.setTo(text);
        text.setTextMatteColor((Color) dAttributes.getMattingPaint());
        text.setTextMatteStroke(new BasicStroke(5));
        text.setMattingPaint(OMColor.clear);
        graphics.add(text);
        graphics.generate(projection);

        return graphics;
    }

    protected int getPtAtDistanceFromLatLon(LatLonPoint loc2, double unitDist,
            Projection projection, Length uom) {
        double lineWidthInRadians = uom.toRadians(unitDist);
        LatLonPoint newX = GreatCircle.sphericalBetween(loc2.getRadLat(), loc2.getRadLon(), lineWidthInRadians, RADIANS_270);
        Point2D newLoc1 = projection.forward(newX);
        return (int) Math.round(newLoc1.getX());
    }

    /**
     * Take a given distance and round it down to the nearest 1, 2, or 5 (or
     * tens/hundreds version of those increments) multiple of that number.
     *
     * @param dist
     * @return scoped value of distance, incremented properly
     */
    protected double scopeDistance(double dist) {
        double new_dist;
        if (dist <= .01) {
            new_dist = .01;
        } else if (dist <= .02) {
            new_dist = .02;
        } else if (dist <= .05) {
            new_dist = .05;
        } else if (dist <= .1) {
            new_dist = .1;
        } else if (dist <= .2) {
            new_dist = .2;
        } else if (dist <= .5) {
            new_dist = .5;
        } else if (dist <= 1) {
            new_dist = 1;
        } else if (dist <= 2) {
            new_dist = 2;
        } else if (dist <= 5) {
            new_dist = 5;
        } else if (dist <= 10) {
            new_dist = 10;
        } else if (dist <= 20) {
            new_dist = 20;
        } else if (dist <= 50) {
            new_dist = 50;
        } else if (dist <= 100) {
            new_dist = 100;
        } else if (dist <= 200) {
            new_dist = 200;
        } else if (dist <= 500) {
            new_dist = 500;
        } else {
            new_dist = 1000;
        }
        return new_dist;
    }

    /**
     * Getter for property unitOfMeasure.
     *
     * @return Value of property unitOfMeasure.
     */
    public String getUnitOfMeasure() {
        return this.unitOfMeasure;
    }

    /**
     * Setter for property unitOfMeasure.
     *
     * @param unitOfMeasure New value of property unitOfMeasure.
     */
    public void setUnitOfMeasure(String unitOfMeasure) {
        if (unitOfMeasure == null) {
            unitOfMeasure = Length.KM.toString();
        }
        this.unitOfMeasure = unitOfMeasure;

        // There is a bug in the Length.get() method that will not
        // return
        // the correct (or any value) for a requested uom.
        // This does not work:
        // uom = com.bbn.openmap.proj.Length.get(unitOfMeasure);
        // Therefore, The following code correctly obtains the proper
        // Length object.
        Length[] choices = Length.values();
        uom = null;
        for (int i = 0; i < choices.length; i++) {
            if (unitOfMeasure.equalsIgnoreCase(choices[i].toString())
                    || unitOfMeasure.equalsIgnoreCase(choices[i].getAbbr())) {
                uom = choices[i];
                break;
            }
        }

        // of no uom is found assign Kilometers as the default.
        if (uom == null) {
            uom = Length.KM;
        }

        uomAbbr = uom.getAbbr();

    }

    JPanel palettePanel;
    ButtonGroup uomButtonGroup;
    Vector<JRadioButton> buttons = new Vector<JRadioButton>();

    /**
     * Creates the interface palette.
     */
    public java.awt.Component getGUI() {

        if (palettePanel == null) {

            logger.fine("creating palette.");

            palettePanel = new JPanel();
            uomButtonGroup = new ButtonGroup();

            palettePanel.setLayout(new javax.swing.BoxLayout(palettePanel, javax.swing.BoxLayout.Y_AXIS));
            palettePanel.setBorder(new javax.swing.border.TitledBorder("Unit Of Measure"));

            java.awt.event.ActionListener al = new ActionListener() {
                // We don't have to check for action commands or anything like
                // that.
                // We know this listener is going to be added to JRadioButtons
                // that are labeled with abbreviations for length.
                public void actionPerformed(ActionEvent e) {
                    JRadioButton jrb = (JRadioButton) e.getSource();
                    setUnitOfMeasure(jrb.getText());
                }
            };

            for (Length lengthType : Length.values()) {
                JRadioButton jrb = new JRadioButton();
                jrb.setText(lengthType.getAbbr());
                jrb.setToolTipText(lengthType.toString());
                uomButtonGroup.add(jrb);
                palettePanel.add(jrb);

                jrb.addActionListener(al);

                jrb.setSelected(unitOfMeasure.equalsIgnoreCase(lengthType.getAbbr()));
                buttons.add(jrb);
            }

        } else {
            for (JRadioButton button : buttons) {
                button.setSelected(uom.getAbbr().equalsIgnoreCase(button.getText()));
            }
        }

        return palettePanel;
    }
}
