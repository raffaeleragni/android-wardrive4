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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Service for periodic triggering of scanning
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ScanService extends Service
{
    private ScanServiceBinderImpl mBinder = new ScanServiceBinderImpl();

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    private class ScanServiceBinderImpl extends Binder implements ScanServiceBinder
    {
        @Override
        public void start()
        {
            ScanService.this.start();
        }

        @Override
        public void stop()
        {
            ScanService.this.stop();
        }

        @Override
        public boolean isRunning()
        {
            return mRunning;
        }
    }

    private boolean mRunning = false;
    
    private void start()
    {
        mRunning = true;
    }
    
    private void stop()
    {
        mRunning = false;
    }
}
