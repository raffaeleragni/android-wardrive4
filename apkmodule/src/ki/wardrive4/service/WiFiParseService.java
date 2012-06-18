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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.List;
import ki.wardrive4.C;
import ki.wardrive4.activity.tasks.ParseWiFiTask;
import ki.wardrive4.data.ScannedWiFi;

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
        new ParseWiFiTask(this).execute(wifis);
    }
}
