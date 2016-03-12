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

import android.app.*;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.activity.MapViewer;
import ki.wardrive4.data.ScannedWiFi;

/**
 * Service for scanning.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ScanService extends Service
{
    private static final String TAG = C.PACKAGE+"/"+ScanService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1;

    public static final String BROADCAST_ACTION_STARTED = C.PACKAGE+".ScanService.STARTED";
    public static final String BROADCAST_ACTION_STOPPED = C.PACKAGE+".ScanService.STOPPED";

//    Do not expose a binder for now
//    private ScanServiceBinder mScanServiceBinder = new ScanServiceBinder();
//    public class ScanServiceBinder extends Binder
//    {
//        public ScanService getService(){ return ScanService.this; }
//    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        start();

        Log.i(TAG, "Service started");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stop();

        Log.i(TAG, "Service stopped");
    }

    /**
     * Static way to know if this service is running.
     *
     * This is a 'running' as intended in Android: service being instantiated.
     * Useful when the service is not controlled by the binder.
     *
     * @return true for running
     */
    public static boolean isRunning(Context ctx)
    {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (ScanService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
    }

    // -------------------------------------------------------------------------

    // Lock used to prevent overwriting of the mCurrentLocation pointer while
    // the parsing of the wifis is reading it's properties.
    private final Object CURRENT_LOCATION_LOCK = new Object();

    // Current location to be read when wifi scan is ready to parse data.
    private volatile Location mCurrentLocation = null;

    private void start()
    {
        // Reset the location
        mCurrentLocation = null;

        // Register the broadcast receiver for the wifi scanning.
        IntentFilter wifiIntent = new IntentFilter();
        wifiIntent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mWiFiScanBroadcastReceiver, wifiIntent);

        // Notified each 10 meters
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, mLocationListener);

        // Registers itself in the nofitication area
        Intent notificationIntent = new Intent(this, MapViewer.class);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notitication = new Notification.Builder(this)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(getText(R.string.app_name))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT))
            .getNotification();
        notificationManager.notify(NOTIFICATION_ID, notitication);

        // Sends a broadcast, so that if some third party wants to know.
        // This also allows to other parts of this application to start/stop the
        // service and let every part know of it.
        sendBroadcast(new Intent(BROADCAST_ACTION_STARTED));
    }

    private void stop()
    {
        // Stops location notifications
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        lm.removeUpdates(mLocationListener);

        // Stops wifi scanning receiver
        unregisterReceiver(mWiFiScanBroadcastReceiver);

        // Cancels the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        // Sends a broadcast, so that if some third party wants to know.
        // This also allows to other parts of this application to start/stop the
        // service and let every part know of it.
        sendBroadcast(new Intent(BROADCAST_ACTION_STOPPED));
    }

    private LocationListener mLocationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            if (location == null)
                return;

            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            synchronized (CURRENT_LOCATION_LOCK)
            {
                mCurrentLocation = location;
            }
            wm.startScan();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // TODO: notify user/use a status icon?
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            // TODO: notify user/use a status icon?
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            // TODO: notify user/use a status icon?
        }
    };

    private BroadcastReceiver mWiFiScanBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            double lat, lon, alt;
            float gpserror;
            synchronized (CURRENT_LOCATION_LOCK)
            {
                if (mCurrentLocation == null)
                    return;

                lat = mCurrentLocation.getLatitude();
                lon = mCurrentLocation.getLongitude();
                alt = mCurrentLocation.getAltitude();
                gpserror = mCurrentLocation.getAccuracy();
            }

            ArrayList<ScannedWiFi> scannedWiFis = new ArrayList<ScannedWiFi>();

            // For each wifi load a ScannedWiFi, adding the location.
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            List<ScanResult> results = wm.getScanResults();
            for (ScanResult result: results)
                scannedWiFis.add(new ScannedWiFi(
                    result.BSSID,
                    result.SSID,
                    result.capabilities,
                    result.level,
                    result.frequency,
                    lat,
                    lon,
                    alt,
                    System.currentTimeMillis(),
                    gpserror));

            // Push it through the intent service to insert them.
            Intent i = new Intent(context, WiFiParseService.class);
            Bundle b = new Bundle();
            b.putParcelableArrayList(WiFiParseService.PAR_WIFIS, scannedWiFis);
            i.putExtras(b);
            startService(i);
        }
    };
}
