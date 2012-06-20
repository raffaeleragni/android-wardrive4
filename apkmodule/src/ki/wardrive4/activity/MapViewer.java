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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import static ki.wardrive4.activity.Settings.*;
import ki.wardrive4.activity.tasks.ExportKmlTask;

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
    
    private static final int REQ_SETTINGS = 1;

    private static final String SETTING_LAST_LAT = "last_lat";
    private static final String SETTING_LAST_LON = "last_lon";
    private static final String SETTING_LAST_ZOOM = "last_zoom";

    private MapView mMapView;
    private Menu mMenu = null;
    private boolean mServiceRunning = false;
    private MyLocationOverlay mMyLocationOverlay;
    private OpenWiFiOverlay mOpenWiFiOverlay;
    private WepWiFiOverlay mWepWiFiOverlay;
    private ClosedWiFiOverlay mClosedWiFiOverlay;
    
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);

        setContentView(R.layout.mapviewer);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
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

        // The my-location gmaps overlay
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);

        // Center the map the first time a location is obtained
        mMyLocationOverlay.runOnFirstFix(new Runnable()
        {
            @Override
            public void run()
            {
                mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
            }
        });

        // Add wifi overlays
        mOpenWiFiOverlay = new OpenWiFiOverlay(this);
        mWepWiFiOverlay = new WepWiFiOverlay(this);
        mClosedWiFiOverlay = new ClosedWiFiOverlay(this);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanService.BROADCAST_ACTION_STARTED);
        filter.addAction(ScanService.BROADCAST_ACTION_STOPPED);
        registerReceiver(mServiceReceiver, filter);

        mServiceRunning = ScanService.isRunning(this);
        updateServiceButton();

        onReloadSettings();
        
        Log.i(TAG, "Created activity: MapViewer");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mServiceReceiver);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
        mMyLocationOverlay.enableCompass();
        if (mPreferences.getBoolean(PREF_MAPFOLLOWME, false))
        {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, mPositionListener);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mMyLocationOverlay.disableMyLocation();
        mMyLocationOverlay.disableCompass();
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        lm.removeUpdates(mPositionListener);
    }

    /**
     * Listener to center the map while ongoing, if the setting was enabled.
     */
    private LocationListener mPositionListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location lctn)
        {
            mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
        }

        @Override
        public void onStatusChanged(String string, int i, Bundle bundle)
        {
        }

        @Override
        public void onProviderEnabled(String string)
        {
        }

        @Override
        public void onProviderDisabled(String string)
        {
        }
    };
    
    /**
     * Called when returning from a settings activity.
     */
    private void onReloadSettings()
    {
        if (mPreferences.getBoolean(PREF_MAPSHOWOPEN, true))
        {
            if (!mMapView.getOverlays().contains(mOpenWiFiOverlay))
                mMapView.getOverlays().add(mOpenWiFiOverlay);
        }
        else
        {
            if (mMapView.getOverlays().contains(mOpenWiFiOverlay))
                mMapView.getOverlays().remove(mOpenWiFiOverlay);
        }
        
        if (mPreferences.getBoolean(PREF_MAPSHOWWEP, true))
        {
            if (!mMapView.getOverlays().contains(mWepWiFiOverlay))
                mMapView.getOverlays().add(mWepWiFiOverlay);
        }
        else
        {
            if (mMapView.getOverlays().contains(mWepWiFiOverlay))
                mMapView.getOverlays().remove(mWepWiFiOverlay);
        }
        
        if (mPreferences.getBoolean(PREF_MAPSHOWCLOSED, true))
        {
            if (!mMapView.getOverlays().contains(mClosedWiFiOverlay))
                mMapView.getOverlays().add(mClosedWiFiOverlay);
        }
        else
        {
            if (mMapView.getOverlays().contains(mClosedWiFiOverlay))
                mMapView.getOverlays().remove(mClosedWiFiOverlay);
        }
        
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (mPreferences.getBoolean(PREF_MAPFOLLOWME, false))
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, mPositionListener);
        else
            lm.removeUpdates(mPositionListener);
        
        boolean showlabels = mPreferences.getBoolean(PREF_MAPSHOWLABELS, true);
        mOpenWiFiOverlay.setShowLabels(showlabels);
        mWepWiFiOverlay.setShowLabels(showlabels);
        mClosedWiFiOverlay.setShowLabels(showlabels);
        
        mMapView.setSatellite(mPreferences.getBoolean(PREF_MAPSHOWSAT, false));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQ_SETTINGS:
                onReloadSettings();
                break;
        }
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
            case R.id_mapviewer_menu.mapmode:
                onMapmodeMenuItemClick();
                break;
            case R.id_mapviewer_menu.importwifis:
                onImportMenuItemClick();
                break;
            case R.id_mapviewer_menu.export:
                onExportMenuItemClick();
                break;
            case R.id_mapviewer_menu.scanning:
                onScanningMenuItemClick();
                break;
            case R.id_mapviewer_menu.settings:
                onSettingsMenuItemClick();
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
            item.setTitle(R.string.mapviewer_menu_scanning);
        else
            item.setTitle(R.string.mapviewer_menu_start_scanning);
    }

    private void onMapmodeMenuItemClick()
    {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PREF_MAPSHOWSAT, !mPreferences.getBoolean(PREF_MAPSHOWSAT, false));
        editor.commit();
        mMapView.setSatellite(mPreferences.getBoolean(PREF_MAPSHOWSAT, false));
    }
    
    private void onSettingsMenuItemClick()
    {
        Intent i = new Intent(this, Settings.class);
        startActivityForResult(i, REQ_SETTINGS);
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
        final CharSequence[] items = {getText(R.string.dlg_import_oldwardrive)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_import_title)
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
                                    .setTitle(R.string.dlg_importold_nofilefound_title)
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

    /**
     * Launch the export of WiFis.
     */
    private void onExportMenuItemClick()
    {
        final CharSequence[] items = {getText(R.string.dlg_export_kml)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_export_title)
            .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int item)
                {
                    switch (item)
                    {
                        case 0:
                            File outFile = new File(Environment.getExternalStorageDirectory(), "wardrive.kml");
                            new ExportKmlTask(MapViewer.this).execute(outFile);
                            break;
                    }
                    dialog.dismiss();
                }
            })
            .create().show();
    }


}
