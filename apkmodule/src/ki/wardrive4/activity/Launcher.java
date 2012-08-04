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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import ki.wardrive4.C;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Launcher entry point.
 * 
 * Since activities mapped as LAUNCHER will be used for shortcuts, this one has
 * been created to maintain a common static entry point for the application to
 * be launched.
 * 
 * Also, global application initialization code goes here.
 * Example: checks for WiFi to be enabled.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Launcher extends Activity
{
    private static final String PREF_FIX_OPENWIFI = "fix_openwifi";
    
    private static final String TAG = C.PACKAGE+"/"+Launcher.class.getSimpleName();
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        openWiFiFix();
        
        startActivity(new Intent(this, MapViewer.class));
        finish();
        
        Log.i(TAG, "Created activity: Launcher");
    }
    
    /**
     * Android (or someone else) changed how capabilities are printed and so
     * open stuff is not recognized anymore.
     * This fixes the already inserted record using the new logic.
     * Also update the timestamp so that the sync will transmit the data.
     */
    private void openWiFiFix()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplication());
        if (prefs.contains(PREF_FIX_OPENWIFI))
            return;
        
        ContentValues cv = new ContentValues();
        cv.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, WiFiSecurity.OPEN.ordinal());
//        cv.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, new Date().getTime());
        getContentResolver().update(
                WiFiContract.WiFi.CONTENT_URI,
                cv,
                "capabilities not like ? and capabilities not like ? and capabilities not like ? and security <> ?",
                new String[]
                {
                    "%WPA%",
                    "%WEP%",
                    "%WPS%",
                    String.valueOf(WiFiSecurity.OPEN.ordinal())
                });
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_FIX_OPENWIFI, true);
        editor.commit();
    }
}
