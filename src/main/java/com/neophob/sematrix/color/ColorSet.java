/**
 * Copyright (C) 2011 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neophob.sematrix.color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class defines a color set
 * 
 * @author michu
 *
 */
public class ColorSet {

    private static final Logger LOG = Logger.getLogger(ColorSet.class.getName());

    private String name;

    private int[] colors;

    private int boarderCount;

    /**
     * 
     * @param name
     * @param colors
     */
    public ColorSet(String name, int[] colors) {
        this.name = name;
        this.colors = colors.clone();
        this.boarderCount = 255 / colors.length;
    }

    /**
     * get ColorSet name
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * returns a random color of this set
     * @return
     */
    public int getRandomColor() {
        Random r = new Random();
        return this.colors[r.nextInt(colors.length)];
    }

    /**
     * return a color defined in this color set 
     * 
     * @param pos
     * @return
     */
    public int getSmoothColor(int pos) {
        try {
            pos %= 255;
            int ofs=0;
            while (pos > boarderCount) {
                pos -= boarderCount;
                ofs++;
            }

            int targetOfs = ofs+1;
            return calcSmoothColor(colors[targetOfs%colors.length], colors[ofs%colors.length], pos);			
        } catch (Exception e) {			
            //if we switch to another smooth color, an exception must be catched here
            return 0;
        }
    }

    /**
     * 
     * @param color
     * @return
     */
    public int getInvertedColor(int color) {

        int b= color&255;
        int g=(color>>8)&255;
        int r=(color>>16)&255;        
        
        //convert it to greyscale, not really correct
        int val = (int)(r*0.3f+g*0.59f+b*0.11f);
    
        return getSmoothColor(val+128);
    }
        
    
    /**
     * 
     * @param col1
     * @param col2
     * @param pos which position (which color offset)
     * @return
     */
    private int calcSmoothColor(int col1, int col2, int pos) {
        int b= col1&255;
        int g=(col1>>8)&255;
        int r=(col1>>16)&255;
        int b2= col2&255;
        int g2=(col2>>8)&255;
        int r2=(col2>>16)&255;

        int mul=pos*colors.length;
        int oppositeColor = 255-mul;

        r=(r*mul + r2*oppositeColor) >> 8;
        g=(g*mul + g2*oppositeColor) >> 8;
        b=(b*mul + b2*oppositeColor) >> 8;

        return (r << 16) | (g << 8) | (b);
    }

    /**
     * convert entries from the properties file into colorset objects
     * @param palette
     * @return
     */
    public static List<ColorSet> loadAllEntries(Properties palette) {
        List<ColorSet> ret = new ArrayList<ColorSet>();

        for (Entry<Object, Object> entry : palette.entrySet()) {
            try {
                String setName = (String)entry.getKey();
                String setColors = (String)entry.getValue();
                String[] colorsAsString = setColors.split(",");

                //convert hex string into int
                int[] colorsAsInt = new int[colorsAsString.length];
                int ofs=0;
                for (String s: colorsAsString) {
                    colorsAsInt[ofs++] = Integer.decode(s.trim());
                }
                ColorSet cs = new ColorSet(setName, colorsAsInt);
                ret.add(cs);				
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to load Palette entry!", e);
            }
        }

        return ret;
    }


    /**
     * colorize an image buffer
     * 
     * @param buffer 8bpp image
     * @param cs ColorSet to apply
     * @return 24 bpp image
     */
    public static int[] convertToColorSetImage(int[] buffer, ColorSet cs) {

        int[] ret = new int[buffer.length];

        for (int i=0; i<buffer.length; i++){
            //use only 8bpp here!
            ret[i]=cs.getSmoothColor(buffer[i]&255);
        }

        return ret;	
    }
}
