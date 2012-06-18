/*
 *   wardrive4 - android wardriving application (remake for the ICS)
 *   Copyright (C) 2012 Raffaele Ragni
 *   https://github.com/raffaeleragni/android-wardrive4
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ki.wardrive4.utils;

import java.util.List;

/**
 * Methods used to triangulate spots to determine the real location of a WiFi.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class TriangulationUtils
{
    private static final double EARTH_RADIUS = 6371; // km
    
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    
    /**
     * Calculate the position of the point by using the passed points position
     * and strength of signal.
     *
     * The actual calculation is called trilateration:
     * https://en.wikipedia.org/wiki/Trilateration
     * 
     * Also few parts from:
     * http://stackoverflow.com/questions/2813615/trilateration-using-3-latitude-and-longitude-points-and-3-distances
     *
     * @return the resulting point calculated.
     */
    public static double[] triangulation(double lat0, double lon0, double r0, double lat1, double lon1, double r1, double lat2, double lon2, double r2)
    {
        // Convert to cartesian
        double[] p0 = latlon2cartesian(lat0, lon0);
        double[] p1 = latlon2cartesian(lat1, lon1);
        double[] p2 = latlon2cartesian(lat2, lon2);

        // Convert so that p0 sits at (0,0)
        double[] p0a = new double[]{0, 0, 0};
        double[] p1a = new double[]{p1[X] - p0[X], p1[Y] - p0[Y], p1[Z] - p0[Z]};
        double[] p2a = new double[]{p2[X] - p0[X], p2[Y] - p0[Y], p2[Z] - p0[Z]};
        
        // All distances refers to p0, the origin
        Double p1distance = distance(p0a, p1a);
        if (p1distance == null)
            return null;
        Double p2distance = distance(p0a, p2a);
        if (p2distance == null)
            return null;
        
        // unit vector of p1a
        double[] p1a_ev = new double[]{p1a[X] / p1distance, p1a[Y] / p1distance, p1a[X] / p1distance};
        // dot product of p1a_ev with p2a
        double p2b_x = p1a_ev[X]*p2a[X] + p1a_ev[Y]*p2a[Y] + p1a_ev[Z]*p2a[Z];
        // finding the y of p2b (for same distance of p2a from p0a)
        double p2b_y = Math.sqrt(Math.abs(Math.pow(p2distance, 2) - Math.pow(p2b_x, 2)));
        
        // Convert so that p1 stays on the x line (rotates the plane)
        double[] p0b = new double[]{0, 0, 0};
        double[] p1b = new double[]{p1distance, 0, 0};
        double[] p2b = new double[]{p2b_x, p2b_y, 0};
        
        double d = p1distance , i = p2b_x, j = p2b_y;
        
        double x = (Math.pow(r0, 2) - Math.pow(r1, 2) + Math.pow(d, 2)) / (2*d);
        double y = (Math.pow(r0, 2) - Math.pow(r2, 2) + Math.pow(i, 2) + Math.pow(j, 2)) / (2*j) - (i/j)*x;

        double[] pb = new double[]{x, y, 0};
        Double pbdistance = distance(p0b, pb);
        if (pbdistance == null)
            return null;
        
        // Opposite operation done for converting points from coordinate system a to b
        double pax = pb[X]/p1a_ev[X] + pb[Y]/p1a_ev[Y] + pb[Z]/p1a_ev[Z];
        double[] pa = new double[]
        {
            pax,
            Math.sqrt(Math.abs(Math.pow(pbdistance, 2) - Math.pow(pax, 2))),
            0
        };
        
        // Opposite operation done for converting points from coordinate system to a
        double p[] = new double[]
        {
            pa[X] + p0[X],
            pa[Y] + p0[Y],
            pa[Z] + p0[Z]
        };
        
        // Reconvert to lat/lon
        return cartesian2latlon(p[X], p[Y], p[Z]);
    }

    /**
     * Converts to Cartesian points
     * 
     * @param lat latitude
     * @param lon longitude
     * 
     * @return point in x,y,z
     */
    private static double[] latlon2cartesian(double lat, double lon)
    {
        return new double[]
        {
            Math.cos(lon) * Math.cos(lat) * EARTH_RADIUS,
            Math.sin(lon) * Math.cos(lat) * EARTH_RADIUS,
            Math.sin(lat) * EARTH_RADIUS
        };
    }
    
    /**
     * Reconvert back to lat/lon.
     * 
     * @param x x value
     * @param y y value
     * @param z z value
     * 
     * @return point in lat,lon
     */
    private static double[] cartesian2latlon(double x, double y, double z)
    {
        return new double[]
        {
            Math.atan(y/x),
            Math.acos(z/EARTH_RADIUS)
        };
    }
    
    /**
     * Computes distance between points in any dimension.
     * 
     * @param p0 point 0
     * @param p1 point 1
     * 
     * @return distance between points
     */
    private static Double distance(double[] p0, double[] p1)
    {
        // Must be of same dimension
        if (p0.length != p1.length)
            return null;
        
        // Calculate distance
        double val = 0;
        for (int n = 0; n < p0.length; n++) 
           val += Math.pow(p1[n] - p0[n], 2);
        return Math.sqrt(val);
    }

    /**
     * Return only the best of &lt;num&gt; points.
     *
     * @param num the max of the best of points
     * @param wifiSpots all the points to analyze
     *
     * @return the best of &lt;num&gt; points
     */
    public static List<Object> bestOf(int num, Object... wifiSpots)
    {
        return null;
    }
}
