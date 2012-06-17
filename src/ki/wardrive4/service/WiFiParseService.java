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
package ki.wardrive4.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import java.util.List;
import ki.wardrive4.C;
import ki.wardrive4.data.ScannedWiFi;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;
import ki.wardrive4.utils.Geohash;
import ki.wardrive4.utils.SHA1Utils;

/**
 * Service to handle the parsing of a WiFi or a list of WiFis connected to a
 * location.
 *
 * It can handle a single WiFi or a list of ones, depending on how the caller
 * service wants to optimize the parallel processing.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WiFiParseService extends IntentService
{
    private static final String TAG = C.PACKAGE+"/"+WiFiParseService.class.getSimpleName();
    
    public static final String PAR_WIFIS = "wifis";

    public WiFiParseService()
    {
        super(WiFiParseService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle bundle = intent.getExtras();
        if (bundle == null)
            return;

        List<ScannedWiFi> wifis = bundle.<ScannedWiFi>getParcelableArrayList(PAR_WIFIS);
        if (wifis == null || wifis.isEmpty())
            return;
        
        Log.i(TAG, "Received "+wifis.size()+" wifis for parsing.");

        // Launch parsing on the background thread
        new ParseWiFiAsyncTask().execute(wifis);
    }

    private class ParseWiFiAsyncTask extends AsyncTask<List<ScannedWiFi>, Integer, Boolean>
    {
        @Override
        protected Boolean doInBackground(List<ScannedWiFi>... paramss)
        {
            List<ScannedWiFi> wifis = paramss[0];

            for (ScannedWiFi wifi: wifis)
            {
                // Generate SHA1 hash for _id (based on bssid).
                // Defaults to bssid if no hash can be computed.
                String id = wifi.bssid;
                try{ id = SHA1Utils.sha1(wifi.bssid); } catch (Exception e){}
                // Generate geohash for the location
                String geohash = new Geohash().encode(wifi.lat, wifi.lon);
                // Insert the measurement as WiFiSpot
                insertWiFiSpot(id, wifi.lat, wifi.lon, wifi.alt, geohash, wifi.timestamp);
                // Update WiFi header information
                updateWiFi(id, wifi.bssid, wifi.ssid, wifi.capabilities, wifi.level, wifi.frequency);
                // Recalculate triangulation
                calculateWiFiPosition(id);
            }
            return true;
        }

        private void updateWiFi(String id, String bssid, String ssid, String capabilities, int level, int frequency)
        {
            // The lat/lon/alt/geohash gets calculated by the calculateWiFiPosition anyway.
            // In the case of a single Spot in fact, it just takes those values and updated them here.

            ContentValues cv = new ContentValues();
            cv.put(WiFiContract.WiFi.COLUMN_NAME_BSSID, bssid);
            cv.put(WiFiContract.WiFi.COLUMN_NAME_SSID, ssid);
            cv.put(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES, capabilities);
            cv.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, WiFiSecurity.fromCapabilities(capabilities).ordinal());
            cv.put(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY, frequency);

            // Check if exists record
            boolean exists = false;
            Cursor c = getContentResolver().query(WiFiContract.WiFi.uriById(id), new String[]{WiFiContract.WiFi._ID}, null, null, null);
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
                getContentResolver().insert(WiFiContract.WiFi.CONTENT_URI, cv);
            }
            else
                getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
        }

        private void calculateWiFiPosition(String id)
        {
            // Count how many records are present for this wifi
            int count = 0;
            Cursor c = getContentResolver().query(
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
                c = getContentResolver().query(
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
                        getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
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
                c = getContentResolver().query(WiFiContract.WiFiSpot.CONTENT_URI,
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
                        int ct = 1;
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
                        getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
                    }
                }
                finally
                {
                    c.close();
                }
            }
        }

        private void insertWiFiSpot(String id, double lat, double lon, double alt, String geohash, long timestamp)
        {
            // insert spot
            ContentValues cv = new ContentValues();
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI, id);
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LAT, lat);
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LON, lon);
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_ALT, alt);
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH, geohash);
            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP, timestamp);
            getContentResolver().insert(WiFiContract.WiFiSpot.CONTENT_URI, cv);
            
            // Try now to keep only the 3 best needed for triangulation.
            // Select the best three measurements we've got. (higher level)
            String bestIds = null;
            Cursor c = getContentResolver().query(WiFiContract.WiFiSpot.CONTENT_URI,
                new String[]{WiFiContract.WiFiSpot._ID},
                WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
                new String[]{id},
                WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL + " desc");
            try
            {
                for (int i = 0; i < 3 && c.moveToNext(); i++)
                    bestIds = bestIds == null
                        ? String.valueOf(c.getInt(c.getColumnIndex(WiFiContract.WiFiSpot._ID)))
                        : ","+c.getInt(c.getColumnIndex(WiFiContract.WiFiSpot._ID));
            }
            finally
            {
                c.close();
            }
            
            // Delete anything that is outside of the best three of this wifi.
            getContentResolver().delete(WiFiContract.WiFiSpot.CONTENT_URI,
                WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ? and " + WiFiContract.WiFiSpot._ID + " not in (?)",
                new String[]{id, bestIds});
        }
    }
}
