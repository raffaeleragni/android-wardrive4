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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import java.io.File;

/**
 * Helper for opening/creating the WiFi database.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WiFiDatabaseHelper extends SQLiteOpenHelper
{
    public static final File DATABASE_FILE = new File(Environment.getExternalStorageDirectory(), "/wardrive4/wifi.db");
    public static final String DATABASE_FILENAME = DATABASE_FILE.getAbsolutePath();
    public static final int DATABASE_VERSION = 1;

    public WiFiDatabaseHelper(Context context)
    {
        super(context.getApplicationContext(), DATABASE_FILENAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        for (String q : CREATE_STATEMENTS)
            db.execSQL(q);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        for (int i = oldVersion + 1; i <= newVersion; i++)
            if (i >= 0 && i < ALTER_STATEMENTS.length)
                for (String q : ALTER_STATEMENTS[i])
                    db.execSQL(q);
    }

    // Statements used to create the database as new.
    // These statements always refer to the last database version.
    // In fact, it is only called when a NEW database must be created.
    private static final String[] CREATE_STATEMENTS = new String[]
    {
        // Maps the WiFi device
        "create table wifi("
        + "_id text primary key,"
        + "bssid text,"
        + "ssid text,"
        + "capabilities text,"
        + "level integer,"
        + "frequency integer,"
        + "lat real,"
        + "lon real,"
        + "alt real,"
        + "geohash text,"
        // Timestamp is the maximum of timestamps of the connected WiFiSpots
        + "timestamp integer)",
        // Maps a single WiFi measurement
        "create table wifispot("
        + "_id integer primary key,"
        + "fk_wifi text,"
        + "lat real,"
        + "lon real,"
        + "alt real,"
        + "geohash text,"
        + "timestamp integer"
        + ")"
    };

    // Statements needed to alter the database from a version to another.
    // The index of the first array matches with the version index.
    private static final String[][] ALTER_STATEMENTS = new String[][]
    {
        // Version 0: Android does not handle index 0 in this API
        null,
        // Version 1: no changes (initial version).
        new String[0]
    };
}
