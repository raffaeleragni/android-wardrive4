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
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import java.io.File;
import ki.wardrive4.R;
import ki.wardrive4.activity.tasks.ImportOldTask;

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
}
