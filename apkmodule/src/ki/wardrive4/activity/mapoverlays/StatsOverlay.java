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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import ki.wardrive4.R;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Overlay for WiFi statistics, such as total count of them.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class StatsOverlay extends Overlay
{
    private static final long UPDATE_DELAY_MS = 3000;
    
    private static final int TEXT_SIZE = 14;
    private static final int LINE_PADDING = 3;
    private static final Paint TEXT;
    static
    {
        TEXT = new Paint();
        TEXT.setStyle(Paint.Style.FILL);
        TEXT.setAntiAlias(true);
        TEXT.setColor(Color.WHITE);
        TEXT.setTextAlign(Paint.Align.LEFT);
        TEXT.setShadowLayer(2, 0, 0, Color.BLACK);
        TEXT.setFakeBoldText(true);
    }
    
    private Context mContext;
    
    private int mLinePadding = LINE_PADDING;
    
    private long mLastUpdateStamp;
    
    private Stats mStats = new Stats();

    public StatsOverlay(Context mContext)
    {
        this.mContext = mContext;
        
        // Apply screen density to text
        float density = mContext.getApplicationContext().getResources().getDisplayMetrics().density;
        TEXT.setTextSize(TEXT_SIZE * density);
        mLinePadding = (int) (LINE_PADDING * density);
    }
    
    @Override
    public void draw(Canvas c, MapView mapView, boolean shadow)
    {
        // Update data only each delay max
        if (System.currentTimeMillis() - mLastUpdateStamp > UPDATE_DELAY_MS)
            updateData();
        
        Rect bounds = c.getClipBounds();
        float width = Math.abs(bounds.right - bounds.left);
        
        String txt;
        float textLength;
        float textSize = TEXT.getTextSize();
        int ct = 1;
        
        txt = mContext.getText(R.string.stats_total_wifi) + " " + mStats.count;
        textLength = TEXT.measureText(txt);
        c.drawText(txt, bounds.left + width/2 - textLength/2, bounds.top + (textSize+mLinePadding)*(ct++), TEXT);
        
        txt = mContext.getText(R.string.stats_total_open_wifi) + " " + mStats.countOpen;
        textLength = TEXT.measureText(txt);
        c.drawText(txt, bounds.left + width/2 - textLength/2, bounds.top + (textSize+mLinePadding)*(ct++), TEXT);
        
        txt = mContext.getText(R.string.stats_total_wep_wifi) + " " + mStats.countWEP;
        textLength = TEXT.measureText(txt);
        c.drawText(txt, bounds.left + width/2 - textLength/2, bounds.top + (textSize+mLinePadding)*(ct++), TEXT);
        
        txt = mContext.getText(R.string.stats_total_closed_wifi) + " " + mStats.countClosed;
        textLength = TEXT.measureText(txt);
        c.drawText(txt, bounds.left + width/2 - textLength/2, bounds.top + (textSize+mLinePadding)*(ct++), TEXT);
    }
    
    private void updateData()
    {
        Cursor cursor;
        String filterQuery;
        String[] filterParams;
        
        cursor = mContext.getContentResolver().query(
            WiFiContract.WiFi.CONTENT_URI,
            new String[]{WiFiContract.WiFi._ID},
            null, null, null);
        try
        {
            mStats.count = cursor.getCount();
        }
        finally
        {
            cursor.close();
        }
        
        filterQuery = "security = ?";
        filterParams = new String[] {String.valueOf(WiFiSecurity.OPEN.ordinal())};
        cursor = mContext.getContentResolver().query(
            WiFiContract.WiFi.CONTENT_URI,
            new String[]{WiFiContract.WiFi._ID},
            filterQuery, filterParams, null);
        try
        {
            mStats.countOpen = cursor.getCount();
        }
        finally
        {
            cursor.close();
        }
        
        filterQuery = "security = ?";
        filterParams = new String[] {String.valueOf(WiFiSecurity.WEP.ordinal())};
        cursor = mContext.getContentResolver().query(
            WiFiContract.WiFi.CONTENT_URI,
            new String[]{WiFiContract.WiFi._ID},
            filterQuery, filterParams, null);
        try
        {
            mStats.countWEP = cursor.getCount();
        }
        finally
        {
            cursor.close();
        }
        
        filterQuery = "security = ?";
        filterParams = new String[] {String.valueOf(WiFiSecurity.CLOSED.ordinal())};
        cursor = mContext.getContentResolver().query(
            WiFiContract.WiFi.CONTENT_URI,
            new String[]{WiFiContract.WiFi._ID},
            filterQuery, filterParams, null);
        try
        {
            mStats.countClosed = cursor.getCount();
        }
        finally
        {
            cursor.close();
        }

        mLastUpdateStamp = System.currentTimeMillis();
    }
    
    private static class Stats
    {
        public long count;
        public long countOpen;
        public long countWEP;
        public long countClosed;
    }
}
