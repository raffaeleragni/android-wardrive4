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
import android.database.Cursor;
import android.database.CursorWindow;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Maps overlay for the open WiFis.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class OpenWiFiOverlay extends WiFiOverlay
{
    private static final int MAX_DRAW = 20;
    
    private static final Paint STROKE;
    private static final Paint FILL;
    private static final Paint TEXT;
    static
    {
        STROKE = new Paint();
        FILL = new Paint();
        TEXT = new Paint();
        
        STROKE.setStyle(Paint.Style.STROKE);
        FILL.setStyle(Paint.Style.FILL);
        TEXT.setStyle(Paint.Style.FILL);
        
        STROKE.setStrokeWidth(1);
        TEXT.setStrokeWidth(3);
        
        STROKE.setAntiAlias(true);
        FILL.setAntiAlias(true);
        TEXT.setAntiAlias(true);
        
        STROKE.setARGB(192, 0, 200, 0);
        FILL.setARGB(192, 0, 200, 0);
        TEXT.setColor(Color.WHITE);
        
        TEXT.setTextSize(16);
        TEXT.setTextAlign(Paint.Align.LEFT);
    }
    
    private Context mContext;
    
    public static CursorWindow cursorWindow = new CursorWindow("WepWiFiOverlay-CursorWindow"); 

    public OpenWiFiOverlay(Context mContext)
    {
        this.mContext = mContext;
    }

    @Override
    public void draw(Canvas c, MapView mapView, boolean shadow)
    {
        GeoPoint topLeft = mapView.getProjection().fromPixels(0, 0);
        GeoPoint bottomRight = mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight());
        Cursor cur = getCursor(mContext, cursorWindow, WiFiSecurity.OPEN, topLeft, bottomRight);
        try
        {
            int ct = 0;
            while (cur.moveToNext())
            {
                String ssid = cur.getString(cur.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_SSID));
                int level = cur.getInt(cur.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LEVEL));
                double lat = cur.getDouble(cur.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LAT));
                double lon = cur.getDouble(cur.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LON));
                GeoPoint gp = new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));
                
                drawSingleWiFi(c, mapView, gp, ssid, level, STROKE, FILL, TEXT);
                
                ct++;
                if (ct > MAX_DRAW)
                    break;
            }
        }
        finally
        {
            cur.close();
        }
    }
}