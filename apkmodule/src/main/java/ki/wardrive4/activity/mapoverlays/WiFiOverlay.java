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
package ki.wardrive4.activity.mapoverlays;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import ki.wardrive4.activity.Settings;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Abstract overlay for WiFis.
 * 
 * Stuff taken from the old wardrive.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public abstract class WiFiOverlay extends Overlay
{
    private static final int CIRCLE_RADIUS = 20;
    
    private boolean mShowLabels;

    /**
     * Returns the timestamp to filter for, or null if not to filter for it.
     */
    private Long getTimestampFilter(Context ctx)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        boolean isUsing = prefs.getBoolean(Settings.PREF_FILTERFROMDATECHECK, false);
        long filterfromdate = prefs.getLong(Settings.PREF_FILTERFROMDATE, -1);
        if (!isUsing || filterfromdate < 0)
            return null;
        return filterfromdate;
    }
    
    public Cursor getCursor(Context ctx, WiFiSecurity type, GeoPoint topLeft, GeoPoint bottomRight, int max)
    {
        String [] between = composeBetween(topLeft, bottomRight);
        String[] projection = new String[]
        {
            WiFiContract.WiFi.COLUMN_NAME_SSID,
            WiFiContract.WiFi.COLUMN_NAME_LEVEL,
            WiFiContract.WiFi.COLUMN_NAME_LAT,
            WiFiContract.WiFi.COLUMN_NAME_LON
        };
        String filterQuery = "security = ? and lat between ? and ? and lon between ? and ?";
        String[] filterParams = new String[]
        {
            String.valueOf(type.ordinal()),
            between[0],
            between[1],
            between[2],
            between[3]
        };
        
        Long timestampFilter = getTimestampFilter(ctx);
        if (timestampFilter != null)
        {
            filterQuery = "security = ? and lat between ? and ? and lon between ? and ? and timestamp > ?";
            filterParams = new String[]
            {
                String.valueOf(type.ordinal()),
                between[0],
                between[1],
                between[2],
                between[3],
                String.valueOf(timestampFilter)
            };
        }
        
        Cursor cursor = ctx.getContentResolver().query(
            WiFiContract.WiFi.CONTENT_URI,
            projection,
            filterQuery,
            filterParams, WiFiContract.WiFi.COLUMN_NAME_LEVEL + " desc limit " + max);
                
        return cursor;
    }
    
    public boolean isShowLabels()
    {
        return mShowLabels;
    }

    public void setShowLabels(boolean mShowLabels)
    {
        this.mShowLabels = mShowLabels;
    }
    
    protected void drawSingleWiFi(Canvas canvas, MapView mapView, GeoPoint geoPoint, String title, int level, Paint stroke, Paint fill, Paint text, float density)
    {
        Point point = mapView.getProjection().toPixels(geoPoint, new Point());
        // The bigness of the circle. Decrement the circle radius so that a weak
        // measured wifi results in a smaller circle.
        int bigness = (int) (CIRCLE_RADIUS) - (int)(((double)-level/99d)*(double)CIRCLE_RADIUS);
        // Circle must be at least 2 pixel big, but also at max CIRCLE_RADIUS-1.
        bigness = bigness < 2 ? 2 : bigness;
        bigness = bigness > CIRCLE_RADIUS-1 ? CIRCLE_RADIUS-1 : bigness;

        // Text size is already density-applied
        float textSize = text.getTextSize();
        
        canvas.drawCircle(point.x, point.y, CIRCLE_RADIUS*density, stroke);
        canvas.drawCircle(point.x, point.y, bigness*density, fill);

        if (mShowLabels && title != null && title.length() > 0)
        {
            RectF rect = new RectF(0, 0, getTextWidth(title, text) + 8, textSize + 4);
            rect.offset(point.x + CIRCLE_RADIUS - CIRCLE_RADIUS/4, point.y + CIRCLE_RADIUS - CIRCLE_RADIUS/4);
            canvas.drawRoundRect(rect, textSize/(3*density), textSize/(3*density), fill);
            canvas.drawText(title, rect.left + 2*density, rect.top + textSize, text);
        }
    }
    
    private int getTextWidth(String text, Paint paint)
    {
        int _count = text.length();
        float[] widths = new float[_count];
        paint.getTextWidths(text, widths);
        int textWidth = 0;
        for (int i = 0; i < _count; i++)
        {
            textWidth += widths[i];
        }
        return textWidth;
    }
    
    protected String[] composeBetween(GeoPoint topLeft, GeoPoint bottomRight)
    {
        double latFrom = ((double) topLeft.getLatitudeE6()) / 1E6;
        double latTo = ((double) bottomRight.getLatitudeE6()) / 1E6;
        double lonFrom = ((double) topLeft.getLongitudeE6()) / 1E6;
        double lonTo = ((double) bottomRight.getLongitudeE6()) / 1E6;
        if (latFrom > latTo)
        {
            double x = latTo;
            latTo = latFrom;
            latFrom = x;
        }
        if (lonFrom > lonTo)
        {
            double x = lonTo;
            lonTo = lonFrom;
            lonFrom = x;
        }
        
        return new String[]
        {
            String.valueOf(latFrom), String.valueOf(latTo), String.valueOf(lonFrom), String.valueOf(lonTo)
        };
    }
}
