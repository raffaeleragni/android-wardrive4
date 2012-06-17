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
package ki.wardrive4.provider.wifi;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import java.util.HashMap;

/**
 * Content provider for the WiFi database.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WiFiContentProvider extends ContentProvider
{
    private static final UriMatcher sUriMatcher;
    private static final HashMap<String, String> sProjectionMap = new HashMap<String, String>();
    private static final HashMap<String, String> sProjectionMapSpots = new HashMap<String, String>();

    private static final int URIMATCH_WIFIS = 1;
    private static final int URIMATCH_WIFI_BYID = URIMATCH_WIFIS + 1;
    private static final int URIMATCH_WIFISPOTS = URIMATCH_WIFI_BYID + 1;
    private static final int URIMATCH_WIFISPOT_BYID = URIMATCH_WIFISPOTS + 1;

    static
    {
        // Maps the URIs to match the constant for the switch selection
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(WiFiContract.AUTHORITY, WiFiContract.WiFi.PATH, URIMATCH_WIFIS);
        sUriMatcher.addURI(WiFiContract.AUTHORITY, WiFiContract.WiFi.PATH_BYID + "*", URIMATCH_WIFI_BYID);
        sUriMatcher.addURI(WiFiContract.AUTHORITY, WiFiContract.WiFiSpot.PATH, URIMATCH_WIFISPOTS);
        sUriMatcher.addURI(WiFiContract.AUTHORITY, WiFiContract.WiFiSpot.PATH_BYID + "#", URIMATCH_WIFISPOT_BYID);

        // Maps a wide catalogue selection projection
        sProjectionMap.put(WiFiContract.WiFi._ID, WiFiContract.WiFi._ID);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_BSSID, WiFiContract.WiFi.COLUMN_NAME_BSSID);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_SSID, WiFiContract.WiFi.COLUMN_NAME_SSID);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES, WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, WiFiContract.WiFi.COLUMN_NAME_SECURITY);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_LEVEL, WiFiContract.WiFi.COLUMN_NAME_LEVEL);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY, WiFiContract.WiFi.COLUMN_NAME_FREQUENCY);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_LAT, WiFiContract.WiFi.COLUMN_NAME_LAT);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_LON, WiFiContract.WiFi.COLUMN_NAME_LON);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_ALT, WiFiContract.WiFi.COLUMN_NAME_ALT);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_GEOHASH, WiFiContract.WiFi.COLUMN_NAME_GEOHASH);
        sProjectionMap.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP);

        // Spots columns
        sProjectionMapSpots.put(WiFiContract.WiFiSpot._ID, WiFiContract.WiFiSpot._ID);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI, WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_LAT, WiFiContract.WiFiSpot.COLUMN_NAME_LAT);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_LON, WiFiContract.WiFiSpot.COLUMN_NAME_LON);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_ALT, WiFiContract.WiFiSpot.COLUMN_NAME_ALT);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH, WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP, WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP);
        sProjectionMapSpots.put(WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL, WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL);
    }

    private WiFiDatabaseHelper mWiFiDatabaseHelper;

    @Override
    public boolean onCreate()
    {
        mWiFiDatabaseHelper = new WiFiDatabaseHelper(getContext());
        return true;
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        mWiFiDatabaseHelper.close();
    }

    @Override
    public String getType(Uri uri)
    {
        switch (sUriMatcher.match(uri))
        {
            case URIMATCH_WIFIS:
                return WiFiContract.WiFi.CONTENT_TYPE;
            case URIMATCH_WIFI_BYID:
                return WiFiContract.WiFi.CONTENT_ITEM_TYPE;
            case URIMATCH_WIFISPOTS:
                return WiFiContract.WiFiSpot.CONTENT_TYPE;
            case URIMATCH_WIFISPOT_BYID:
                return WiFiContract.WiFiSpot.CONTENT_ITEM_TYPE;
            default:
                // Do not handle foreign URIs
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Change projection and table name depending on URI
        switch (sUriMatcher.match(uri))
        {
            // Table catalogue
            case URIMATCH_WIFI_BYID:
                qb.appendWhere(WiFiContract.WiFi._ID + "='" + uri.getPathSegments().get(WiFiContract.WiFi.PATH_BYID_IDPOSITION)+"'");
            case URIMATCH_WIFIS:
                qb.setTables(WiFiContract.WiFi.TABLE_NAME);
                qb.setProjectionMap(sProjectionMap);
                break;
            // Table catalogue
            case URIMATCH_WIFISPOT_BYID:
                qb.appendWhere(WiFiContract.WiFiSpot._ID + "=" + uri.getPathSegments().get(WiFiContract.WiFiSpot.PATH_BYID_IDPOSITION));
            case URIMATCH_WIFISPOTS:
                qb.setTables(WiFiContract.WiFiSpot.TABLE_NAME);
                qb.setProjectionMap(sProjectionMapSpots);
                break;
            default:
                // Do not handle foreign URIs
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder))
        {
            switch (sUriMatcher.match(uri))
            {
                case URIMATCH_WIFI_BYID:
                case URIMATCH_WIFIS:
                    orderBy = WiFiContract.WiFi.DEFAULT_ORDER_BY;
                    break;
                case URIMATCH_WIFISPOT_BYID:
                case URIMATCH_WIFISPOTS:
                    orderBy = WiFiContract.WiFiSpot.DEFAULT_ORDER_BY;
                    break;
                default:
                    // Do not handle foreign URIs
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        else
           orderBy = sortOrder;

        SQLiteDatabase db = mWiFiDatabaseHelper.getReadableDatabase();

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Watch for changes to the URI
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues)
    {
        // Can insert only in the full list URIs
        if (sUriMatcher.match(uri) != URIMATCH_WIFIS &&
            sUriMatcher.match(uri) != URIMATCH_WIFISPOTS)
            throw new IllegalArgumentException("Unknown URI " + uri);

        ContentValues values = initialValues != null ? new ContentValues(initialValues) : new ContentValues();

        SQLiteDatabase db = mWiFiDatabaseHelper.getWritableDatabase();

        long rowId = 0;
        if (sUriMatcher.match(uri) == URIMATCH_WIFIS)
            rowId = db.insert(
                WiFiContract.WiFi.TABLE_NAME,
                WiFiContract.WiFi._ID,
                values);
        else if (sUriMatcher.match(uri) == URIMATCH_WIFISPOTS)
            rowId = db.insert(
                WiFiContract.WiFiSpot.TABLE_NAME,
                WiFiContract.WiFiSpot._ID,
                values);

        if (rowId == 0)
            throw new SQLException("Failed to insert row into " + uri);

        Uri insertedUri = null;
        if (sUriMatcher.match(uri) == URIMATCH_WIFIS)
            insertedUri = ContentUris.withAppendedId(WiFiContract.WiFi.CONTENT_ID_URI_BASE, rowId);
        else if (sUriMatcher.match(uri) == URIMATCH_WIFISPOTS)
            insertedUri = ContentUris.withAppendedId(WiFiContract.WiFiSpot.CONTENT_ID_URI_BASE, rowId);

        getContext().getContentResolver().notifyChange(insertedUri, null);
        return insertedUri;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs)
    {
        SQLiteDatabase db = mWiFiDatabaseHelper.getWritableDatabase();
        String finalWhere;
        int count;

        switch (sUriMatcher.match(uri))
        {
            case URIMATCH_WIFIS:
                count = db.delete(
                    WiFiContract.WiFi.TABLE_NAME,
                    where,
                    whereArgs);
                break;
            case URIMATCH_WIFI_BYID:
                finalWhere = WiFiContract.WiFi._ID + "='" + uri.getPathSegments().get(WiFiContract.WiFi.PATH_BYID_IDPOSITION) + "'";
                finalWhere = where != null ? finalWhere + " AND " + where : finalWhere;
                count = db.delete(
                    WiFiContract.WiFi.TABLE_NAME,
                    finalWhere,
                    whereArgs);
                break;
            case URIMATCH_WIFISPOTS:
                count = db.delete(
                    WiFiContract.WiFiSpot.TABLE_NAME,
                    where,
                    whereArgs);
                break;
            case URIMATCH_WIFISPOT_BYID:
                finalWhere = WiFiContract.WiFiSpot._ID + "=" + uri.getPathSegments().get(WiFiContract.WiFiSpot.PATH_BYID_IDPOSITION);
                finalWhere = where != null ? finalWhere + " AND " + where : finalWhere;
                count = db.delete(
                    WiFiContract.WiFiSpot.TABLE_NAME,
                    finalWhere,
                    whereArgs);
                break;
            default:
                // Do not handle foreign URIs
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
    {
        SQLiteDatabase db = mWiFiDatabaseHelper.getWritableDatabase();
        int count;
        String finalWhere;

        switch (sUriMatcher.match(uri))
        {
            case URIMATCH_WIFIS:
                count = db.update(
                    WiFiContract.WiFi.TABLE_NAME,
                    values,
                    where,
                    whereArgs);
                break;
            case URIMATCH_WIFI_BYID:
                finalWhere = WiFiContract.WiFi._ID + "='" + uri.getPathSegments().get(WiFiContract.WiFi.PATH_BYID_IDPOSITION) + "'";
                finalWhere = where != null ? finalWhere + " AND " + where : finalWhere;
                count = db.update(
                    WiFiContract.WiFi.TABLE_NAME,
                    values,
                    finalWhere,
                    whereArgs);
                break;
            case URIMATCH_WIFISPOTS:
                count = db.update(
                    WiFiContract.WiFiSpot.TABLE_NAME,
                    values,
                    where,
                    whereArgs);
                break;
            case URIMATCH_WIFISPOT_BYID:
                finalWhere = WiFiContract.WiFiSpot._ID + "=" + uri.getPathSegments().get(WiFiContract.WiFiSpot.PATH_BYID_IDPOSITION);
                finalWhere = where != null ? finalWhere + " AND " + where : finalWhere;
                count = db.update(
                    WiFiContract.WiFiSpot.TABLE_NAME,
                    values,
                    finalWhere,
                    whereArgs);
                break;
            default:
                // Do not handle foreign URIs
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
