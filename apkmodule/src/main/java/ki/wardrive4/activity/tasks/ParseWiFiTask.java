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
package ki.wardrive4.activity.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import static ki.wardrive4.activity.Settings.PREF_GPSERROR;
import static ki.wardrive4.activity.Settings.PREF_MINLEVEL;
import ki.wardrive4.data.ScannedWiFi;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.data.WiFiSyncStatus;
import ki.wardrive4.provider.wifi.WiFiContract;
import ki.wardrive4.utils.Geohash;
import ki.wardrive4.utils.SHA1Utils;

/**
 * Task that processes the scanned WiFi into the database.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ParseWiFiTask extends AsyncTask<List<ScannedWiFi>, Integer, Boolean>
{
    public static final int WIFISPOT_MAX = 3;
    
    private Context mContext;
    private SharedPreferences mPreferences;

    public ParseWiFiTask(Context mContext)
    {
        this.mContext = mContext;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
    }

    @Override
    protected Boolean doInBackground(List<ScannedWiFi>... paramss)
    {
        List<ScannedWiFi> wifis = paramss[0];

        int gpserror = Integer.valueOf(mPreferences.getString(PREF_GPSERROR, "50"));
        int minlevel = Integer.valueOf(mPreferences.getString(PREF_MINLEVEL, "-99"));

        for (ScannedWiFi wifi: wifis)
        {
            // Apply filter out rules, from the settings
            if (wifi.level < minlevel || wifi.gpserror > gpserror)
                continue;

            // Generate SHA1 hash for _id (based on bssid).
            // Defaults to bssid if no hash can be computed.
            String id = wifi.bssid;
            try{ id = SHA1Utils.sha1(wifi.bssid); } catch (Exception e){}
            // Generate geohash for the location
            String geohash = new Geohash().encode(wifi.lat, wifi.lon);
            // Insert the measurement as WiFiSpot
            insertWiFiSpot(id, wifi.lat, wifi.lon, wifi.alt, geohash, wifi.level, wifi.timestamp);
            // Update WiFi header information
            updateWiFi(id, wifi.bssid, wifi.ssid, wifi.capabilities, wifi.frequency);
            // Recalculate triangulation
            calculateWiFiPosition(id);
        }
        return true;
    }

    private void updateWiFi(String id, String bssid, String ssid, String capabilities, int frequency)
    {
        // The lat/lon/alt/geohash gets calculated by the calculateWiFiPosition anyway.
        // In the case of a single Spot in fact, it just takes those values and updated them here.

        ContentValues cv = new ContentValues();
        cv.put(WiFiContract.WiFi.COLUMN_NAME_BSSID, bssid);
        cv.put(WiFiContract.WiFi.COLUMN_NAME_SSID, ssid);
        cv.put(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES, capabilities);
        cv.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, WiFiSecurity.fromCapabilities(capabilities).ordinal());
        cv.put(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY, frequency);
        cv.put(WiFiContract.WiFi.COLUMN_NAME_SYNC_STATUS, WiFiSyncStatus.TO_UPDATE_UPLOAD.ordinal());

        // Check if exists record
        boolean exists = false;
        Cursor c = mContext.getContentResolver().query(WiFiContract.WiFi.uriById(id), new String[]{WiFiContract.WiFi._ID}, null, null, null);
        try
        {
            exists = c.moveToNext();
        }
        finally
        {
            c.close();
        }

        // insert or update
        if (!exists)
        {
            cv.put(WiFiContract.WiFi._ID, id);
            mContext.getContentResolver().insert(WiFiContract.WiFi.CONTENT_URI, cv);
        }
        else
            mContext.getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
    }

    private void insertWiFiSpot(String id, double lat, double lon, double alt, String geohash, int level, long timestamp)
    {
        // insert spot
        ContentValues cv = new ContentValues();
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI, id);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LAT, lat);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LON, lon);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_ALT, alt);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH, geohash);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL, level);
        cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP, timestamp);
        mContext.getContentResolver().insert(WiFiContract.WiFiSpot.CONTENT_URI, cv);

        // Try now to keep only the 3 best needed for triangulation.
        // Select the best three measurements we've got. (higher level)
        List<Long> idsToDelete= new ArrayList<Long>();
        Cursor c = mContext.getContentResolver().query(WiFiContract.WiFiSpot.CONTENT_URI,
            new String[]{WiFiContract.WiFiSpot._ID},
            WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
            new String[]{id},
            WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL + " desc");
        try
        {
            // Roll the cursor three times or less if less records.
            for (int i = 0; i < WIFISPOT_MAX && c.moveToNext(); i++);
            // List the remaining ids into the array
            while (c.moveToNext())
                idsToDelete.add(c.getLong(c.getColumnIndex(WiFiContract.WiFiSpot._ID)));
        }
        finally
        {
            c.close();
        }

        // Delete anything that is outside of the best three of this wifi.
        for (Long spotid: idsToDelete)
            mContext.getContentResolver().delete(WiFiContract.WiFiSpot.uriById(spotid), null, null);
    }

    private void calculateWiFiPosition(String id)
    {
        // Count how many records are present for this wifi
        int count = 0;
        Cursor c = mContext.getContentResolver().query(
            WiFiContract.WiFiSpot.CONTENT_URI,
            new String[]{"count("+WiFiContract.WiFiSpot._ID+") as count"},
            WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
            new String[]{id},
            null);
        try
        {
            if (c.moveToNext())
                count = c.getInt(c.getColumnIndex("count"));
        }
        finally
        {
            c.close();
        }

        // In the case of a single record for this id being present, just transfer directly the values
        if (count == 1)
        {
            c = mContext.getContentResolver().query(
                WiFiContract.WiFiSpot.CONTENT_URI,
                new String[]
                {
                    WiFiContract.WiFiSpot.COLUMN_NAME_LAT,
                    WiFiContract.WiFiSpot.COLUMN_NAME_LON,
                    WiFiContract.WiFiSpot.COLUMN_NAME_ALT,
                    WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH,
                    WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL,
                    WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP
                },
                WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
                new String[]{id},
                null);
            try
            {
                if (c.moveToNext())
                {
                    ContentValues cv = new ContentValues();
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LAT, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_LAT)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LON, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_LON)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_ALT, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_ALT)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_GEOHASH, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LEVEL, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, c.getDouble(c.getColumnIndex(WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP)));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_SYNC_STATUS, WiFiSyncStatus.TO_UPDATE_UPLOAD.ordinal());
                    mContext.getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
                }
            }
            finally
            {
                c.close();
            }
        }
        // Or, of more records are present, proceed with the average calculation,
        // timestamp and level are taken as MAX.
        // (leave out triangulation for now)
        else if (count > 1)
        {
            c = mContext.getContentResolver().query(WiFiContract.WiFiSpot.CONTENT_URI,
                new String[]
                {
                    "avg("+WiFiContract.WiFiSpot.COLUMN_NAME_LAT+") as lat",
                    "avg("+WiFiContract.WiFiSpot.COLUMN_NAME_LON+") as lon",
                    "avg("+WiFiContract.WiFiSpot.COLUMN_NAME_ALT+") as alt",
                    "max("+WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL+") as level",
                    "max("+WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP+") as timestamp"
                },
                WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
                new String[]{id},
                null);
            try
            {
                if (c.moveToNext())
                {
                    int ct = 0;
                    double lat = c.getDouble(ct++);
                    double lon = c.getDouble(ct++);
                    double alt = c.getDouble(ct++);
                    String geohash = new Geohash().encode(lat, lon);
                    int level = c.getInt(ct++);
                    long timestamp = c.getLong(ct++);

                    ContentValues cv = new ContentValues();
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LAT, lat);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LON, lon);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_ALT, alt);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_GEOHASH, geohash);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LEVEL, level);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, timestamp);
                    mContext.getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
                }
            }
            finally
            {
                c.close();
            }
        }
    }
}
