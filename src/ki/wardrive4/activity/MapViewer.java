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

import android.os.Bundle;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import ki.wardrive4.R;

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
    protected void onCreate(Bundle arg0)
    {
        super.onCreate(arg0);
        setContentView(R.layout.mapviewer);

        mMapView = (MapView) findViewById(R.id_mapviewer.mapview);
        //Customizations for the MapView that are not possible from the XML
        mMapView.setBuiltInZoomControls(true);
    }

    @Override
    protected boolean isRouteDisplayed()
    {
        // No route gets displayed in wardrive, only WiFi points.
        return false;
    }
}
