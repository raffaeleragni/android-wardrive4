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

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.data.WiFiSyncStatus;
import ki.wardrive4.provider.wifi.WiFiContract;
import ki.wardrive4.utils.Geohash;
import ki.wardrive4.utils.SHA1Utils;

/**
 * Task for importing the old wardrive database.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ImportOldTask extends AsyncTask<File, Integer, Void>
{
    private static final String TAG = C.PACKAGE+"/"+ImportOldTask.class.getSimpleName();
    
    private ProgressDialog progressDialog = null;
    
    private Context mContext;

    public ImportOldTask(Context mContext)
    {
        this.mContext = mContext;
    }

    @Override
    protected Void doInBackground(File... paramss)
    {
        Log.i(TAG, "Launching ImportOldTask");
        
        File databaseFile = paramss[0];
        int ct = 1;
        SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor c = null;
        try
        {
            c = db.query("networks", new String[]{"count(bssid)"}, null, null, null, null, null);
            c.moveToNext();
            int total = c.getInt(0);
            c.close();

            c = db.query("networks", new String[]
            {
                "bssid",
                "ssid",
                "capabilities",
                "level",
                "frequency",
                "lat",
                "lon",
                "alt",
                "timestamp"
            }, null, null, null, null, null);

            while (c.moveToNext())
            {
                String bssid = c.getString(c.getColumnIndex("bssid"));
                String id = bssid;
                try
                {
                    id = SHA1Utils.sha1(id);
                }
                catch (Exception e)
                {
                }
                Cursor cCount = mContext.getContentResolver().query(WiFiContract.WiFi.uriById(id), new String[]{WiFiContract.WiFi._ID }, null, null, null);
                try
                {
                    // Jump: record already exists
                    if (cCount.moveToNext())
                    {
                        // Although count it in the total.
                        publishProgress(ct++, total);
                        continue;
                    }
                }
                finally
                {
                    cCount.close();
                }

                ContentValues cv = new ContentValues();

                double lat = c.getDouble(c.getColumnIndex("lat"));
                double lon = c.getDouble(c.getColumnIndex("lon"));
                double alt = c.getDouble(c.getColumnIndex("alt"));
                String geohash = new Geohash().encode(lat, lon);

                String capabilities = c.getString(c.getColumnIndex("capabilities"));
                WiFiSecurity security = WiFiSecurity.fromCapabilities(capabilities);

                cv.put(WiFiContract.WiFi._ID, id);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_BSSID, c.getString(c.getColumnIndex("bssid")));
                cv.put(WiFiContract.WiFi.COLUMN_NAME_SSID, c.getString(c.getColumnIndex("ssid")));
                cv.put(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES, capabilities);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, security.ordinal());
                cv.put(WiFiContract.WiFi.COLUMN_NAME_LEVEL, c.getInt(c.getColumnIndex("level")));
                cv.put(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY, c.getInt(c.getColumnIndex("frequency")));

                cv.put(WiFiContract.WiFi.COLUMN_NAME_LAT, lat);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_LON, lon);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_ALT, alt);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_GEOHASH, geohash);
                cv.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, c.getLong(c.getColumnIndex("timestamp")));
                
                cv.put(WiFiContract.WiFi.COLUMN_NAME_SYNC_STATUS, WiFiSyncStatus.TO_UPDATE_UPLOAD.ordinal());

                mContext.getContentResolver().insert(WiFiContract.WiFi.CONTENT_URI, cv);

                publishProgress(ct++, total);

                if (isCancelled())
                    break;
            }
        }
        finally
        {
            if (c != null)
                c.close();
            db.close();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values)
    {
        int ct = values[0];
        int total = values[1];

        if (progressDialog == null)
        {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setTitle(R.string.dlg_importoldprogress_title);
            progressDialog.setMessage(mContext.getText(R.string.dlg_importoldprogress_message));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            // If canceled the user would not have any other way to see the progress, instead map the canceling to cancel the task
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface di)
                {
                    cancel(true);
                }
            });

            progressDialog.show();
        }

        progressDialog.setProgress(ct);
        progressDialog.setMax(total);
    }

    @Override
    protected void onPostExecute(Void result)
    {
        progressDialog.dismiss();
        progressDialog = null;
    }
}
