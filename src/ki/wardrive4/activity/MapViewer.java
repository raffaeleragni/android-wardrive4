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
package ki.wardrive4.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import java.io.File;
import ki.wardrive4.R;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;
import ki.wardrive4.utils.Geohash;
import ki.wardrive4.utils.SHA1Utils;

/**
 * The main map viewer screen, a map showing WiFis currently in database.
 *
 * The purpose of this screen is for viewing scanned or scanning progress.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class MapViewer extends MapActivity
{
    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        
        setContentView(R.layout.mapviewer);

        mMapView = (MapView) findViewById(R.id_mapviewer.mapview);
        // Customizations like this were not possible from the XML
        mMapView.setBuiltInZoomControls(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.mapviewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id_mapviewer_menu.importwifis:
                final CharSequence[] items = {getText(R.string.mapviewer_dlg_import_oldwardrive)};
                new AlertDialog.Builder(this)
                    .setTitle(R.string.mapviewer_dlg_import_title)
                    .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int item)
                        {
                            switch (item)
                            {
                                case 0:
                                    File dbFile = new File(Environment.getExternalStorageDirectory(), "wardrive.db3");
                                    if (dbFile.exists() && dbFile.isFile())
                                    {
                                        new ImportTask().execute(dbFile);
                                    }
                                    else
                                        // Alert error for not finding a correct file
                                        new AlertDialog.Builder(MapViewer.this)
                                            .setTitle(R.string.mapviewer_dlg_importold_nofilefound_title)
                                            .setNegativeButton(R.string.OK, new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface di, int i)
                                                {
                                                    di.dismiss();
                                                }
                                            })
                                            .create().show();
                                    break;
                            }
                            dialog.dismiss();
                        }
                    })
                    .create().show();
                break;
        }
        return true;
    }
    
    @Override
    protected boolean isRouteDisplayed()
    {
        // No route gets displayed in wardrive, only WiFi points.
        return false;
    }
    
    /**
     * Task for importing the old wardrive database.
     */
    private class ImportTask extends AsyncTask<File, Integer, Void>
    {
        private ProgressDialog progressDialog = null;
        
        @Override
        protected Void doInBackground(File... paramss)
        {
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
                    try{ id = SHA1Utils.sha1(id); } catch (Exception e){}
                    Cursor cCount = getContentResolver().query(WiFiContract.WiFi.uriById(id), new String[]{WiFiContract.WiFi._ID}, null, null, null);
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

                    getContentResolver().insert(WiFiContract.WiFi.CONTENT_URI, cv);

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
                progressDialog = new ProgressDialog(MapViewer.this);
                progressDialog.setTitle(R.string.mapviewer_dlg_importoldprogress_title);
                progressDialog.setMessage(getText(R.string.mapviewer_dlg_importoldprogress_message));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                // If canceled the user would not have any other way to see the progress, instead map the canceling to cancel the task
//                progressDialog.setCancelable(false);
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
}
