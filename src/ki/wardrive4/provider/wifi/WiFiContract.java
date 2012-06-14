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

import android.net.Uri;
import android.provider.BaseColumns;
import ki.wardrive4.C;

/**
 * Contract class defining the WiFi database structure.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WiFiContract
{
    // Unique identifier for this contract of content provider.
    public static final String AUTHORITY = C.PACKAGE+".provider.wifi";
    // URI for the content provider.
    public static final String BASE_URI = "content://"+AUTHORITY+"/";

    public static final class WiFi implements BaseColumns
    {
        public static final String TABLE_NAME = "wifi";

        public static final String PATH = "wifis";
        public static final String PATH_BYID = "wifi/";
        // Position in the path (0-index based) where to find the WiFi ID
        // for the BYID path
        public static final int PATH_BYID_IDPOSITION = 1;
        // Static URIs and utility methods for creating them
        public static final Uri CONTENT_URI =  Uri.parse(BASE_URI + PATH);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(BASE_URI + PATH_BYID);
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(BASE_URI + PATH_BYID + "#");
        public static Uri uriById(long id) {return Uri.parse(BASE_URI + PATH_BYID + id);}

        // Content type of many wifis (directory of items)
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd."+AUTHORITY+"."+TABLE_NAME;
        // Content type of a single wifi
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."+AUTHORITY+"."+TABLE_NAME;

        // Columns
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_SSID = "ssid";
        public static final String COLUMN_NAME_CAPABILITIES = "capabilities";
        public static final String COLUMN_NAME_LEVEL = "level";
        public static final String COLUMN_NAME_FREQUENCY = "frequency";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
        public static final String COLUMN_NAME_ALT = "alt";
        public static final String COLUMN_NAME_GEOHASH = "geohash";
        public static final String COLUMN_NAME_TIMESTAMP = "timespan";

        // Order by default
        public static final String DEFAULT_ORDER_BY = _ID + " ASC";
    }

    public static final class WiFiSpot implements BaseColumns
    {
        public static final String TABLE_NAME = "wifispot";

        public static final String PATH = "wifispots";
        public static final String PATH_BYID = "wifispot/";
        // Position in the path (0-index based) where to find the WiFi ID
        // for the BYID path
        public static final int PATH_BYID_IDPOSITION = 1;
        // Static URIs and utility methods for creating them
        public static final Uri CONTENT_URI =  Uri.parse(BASE_URI + PATH);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(BASE_URI + PATH_BYID);
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(BASE_URI + PATH_BYID + "#");
        public static Uri uriById(long id) {return Uri.parse(BASE_URI + PATH_BYID + id);}

        // Content type of many wifis (directory of items)
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd."+AUTHORITY+"."+TABLE_NAME;
        // Content type of a single wifi
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."+AUTHORITY+"."+TABLE_NAME;

        // Columns
        public static final String COLUMN_NAME_FK_WIFI = "fk_wifi";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
        public static final String COLUMN_NAME_ALT = "alt";
        public static final String COLUMN_NAME_GEOHASH = "geohash";
        public static final String COLUMN_NAME_TIMESTAMP = "timespan";

        // Order by default
        public static final String DEFAULT_ORDER_BY = _ID + " ASC";
    }
}
