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
import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import java.io.File;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.activity.mapoverlays.ClosedWiFiOverlay;
import ki.wardrive4.activity.mapoverlays.OpenWiFiOverlay;
import ki.wardrive4.activity.mapoverlays.WepWiFiOverlay;
import ki.wardrive4.activity.tasks.ImportOldTask;
import ki.wardrive4.service.ScanService;

/**
 * The main map viewer screen, a map showing WiFis currently in database.
 *
 * The purpose of this screen is for viewing scanned or scanning progress.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class MapViewer extends MapActivity
{
    private static final String TAG = C.PACKAGE+"/"+MapActivity.class.getSimpleName();
    
    private static final String SETTING_LAST_LAT = "last_lat";
    private static final String SETTING_LAST_LON = "last_lon";
    private static final String SETTING_LAST_ZOOM = "last_zoom";
    
    private MapView mMapView;
    private Menu mMenu = null;
    private boolean mServiceRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        
        setContentView(R.layout.mapviewer);

        mMapView = (MapView) findViewById(R.id_mapviewer.mapview);
        // Customizations like this were not possible from the XML
        mMapView.setBuiltInZoomControls(true);
        
        // Read the last map center used when the app did exit, and reset it
        // Also reset the old zoom
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        if (settings.contains(SETTING_LAST_ZOOM))
            mMapView.getController().setZoom(settings.getInt(SETTING_LAST_ZOOM, mMapView.getZoomLevel()));
        if (settings.contains(SETTING_LAST_LAT) && settings.contains(SETTING_LAST_LON))
        {
            GeoPoint point = new GeoPoint(settings.getInt(SETTING_LAST_LAT, 0), settings.getInt(SETTING_LAST_LON, 0));
            mMapView.getController().animateTo(point);
        }
        
        // Add overlays
        mMapView.getOverlays().add(new OpenWiFiOverlay(this));
        mMapView.getOverlays().add(new WepWiFiOverlay(this));
        mMapView.getOverlays().add(new ClosedWiFiOverlay(this));
        // The my-location gmaps overlay
        mMapView.getOverlays().add(new MyLocationOverlay(this, mMapView));
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanService.BROADCAST_ACTION_STARTED);
        filter.addAction(ScanService.BROADCAST_ACTION_STOPPED);
        registerReceiver(mServiceReceiver, filter);
        
        mServiceRunning = ScanService.isRunning(this);
        updateServiceButton();
        
        Log.i(TAG, "Created activity: MapViewer");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mServiceReceiver);
    }
    
    /**
     * Service may be controlled also by other future parts, so rely on the
     * broadcast receiving to know when it's started and stopped.
     */
    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ScanService.BROADCAST_ACTION_STARTED.equals(intent.getAction()))
            {
                if (C.DEBUG) Log.d(TAG, "Noticed service as started");
                
                mServiceRunning = true;
                updateServiceButton();
            }
            else if (ScanService.BROADCAST_ACTION_STOPPED.equals(intent.getAction()))
            {
                if (C.DEBUG) Log.d(TAG, "Noticed service as stopped");
                
                mServiceRunning = false;
                updateServiceButton();
            }
        }
    };
    
    @Override
    protected void onStop()
    {
        super.onStop();
        
        // Save the position of the map center for the next program opening
        // Also save the zoom used
        GeoPoint point = mMapView.getProjection().fromPixels(mMapView.getWidth() / 2, mMapView.getHeight() / 2);
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(SETTING_LAST_LAT, point.getLatitudeE6());
        editor.putInt(SETTING_LAST_LON, point.getLongitudeE6());
        editor.putInt(SETTING_LAST_ZOOM, mMapView.getZoomLevel());
        editor.commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.mapviewer, menu);
        mMenu = menu;
        updateServiceButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id_mapviewer_menu.importwifis:
                onImportMenuItemClick();
                break;
            case R.id_mapviewer_menu.scanning:
                onScanningMenuItemClick();
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
     * Update the menu button depending on the service status.
     */
    private void updateServiceButton()
    {
        if (mMenu == null)
            return;
        
        MenuItem item = mMenu.findItem(R.id_mapviewer_menu.scanning);
        if (mServiceRunning)
        {
            item.setIcon(getResources().getDrawable(R.drawable.ic_scanning_on));
            item.setTitle(R.string.mapviewer_menu_scanning);
        }
        else
        {
            item.setIcon(getResources().getDrawable(R.drawable.ic_scanning_off));
            item.setTitle(R.string.mapviewer_menu_start_scanning);
        }
    }
    
    private void onScanningMenuItemClick()
    {
        if (C.DEBUG) Log.d(TAG, "Toggling service scanning");
        
        Intent i = new Intent(this, ScanService.class);
        if (mServiceRunning)
            stopService(i);
        else
            startService(i);
    }
    
    /**
     * Launch the import of WiFis.
     */
    private void onImportMenuItemClick()
    {
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
                                // Start the background task
                                new ImportOldTask(MapViewer.this).execute(dbFile);
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
    }
    
    
}
