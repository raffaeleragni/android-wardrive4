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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.activity.Settings;

/**
 * Task to upload to WiGlE.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WigleUploaderTask extends AsyncTask<File, Integer, Void>
{
    private static final String TAG = C.PACKAGE+"/"+WigleUploaderTask.class.getSimpleName();
    	
	private static final String _URL = "http://www.wigle.net/gps/gps/main/confirmfile/";
	
	private static final String BOUNDARY = "----MultiPartBoundary";
	
	private static final String NL = "\r\n";
    
    private ProgressDialog progressDialog = null;
    
    private Activity mContext;

    public WigleUploaderTask(Activity mContext)
    {
        this.mContext = mContext;
    }
    
    @Override
    protected Void doInBackground(File... paramss)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
        
        File file = paramss[0];
        String username = prefs.getString(Settings.PREF_WIGLE_USERNAME, null);
        String password = prefs.getString(Settings.PREF_WIGLE_PASSWORD, null);
        if (username == null || username.length() == 0 || password == null || password.length() == 0)
        {
            mContext.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(mContext, R.string.dlg_exportwigleprogress_err_no_user_and_pass, Toast.LENGTH_LONG).show();
                }
            });
			return null;
        }
		
        try
        {
            URL url = new URL(_URL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(1);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent","wardrive");
            conn.setRequestProperty("Content-Type","multipart/form-data;boundary="+BOUNDARY);
            conn.connect();
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes("--"+BOUNDARY+NL+"Content-Disposition: form-data; name=\"observer\""+NL+NL+username+NL);
            dos.writeBytes("--"+BOUNDARY+NL+"Content-Disposition: form-data; name=\"password\""+NL+NL+password+NL);
            dos.writeBytes("--"+BOUNDARY+NL+"Content-Disposition: form-data; name=\"stumblefile\";filename=\"wardrive.kml\""+NL+"Content-Type: application/octet-stream"+NL+NL);
            int ct;
            long readbytes = 0;
            long filelength = file.length();
            byte[] buf = new byte[1024];
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file), 1024);
            publishProgress(0, (int) filelength);
            while(fis.available() > 0)
            {
                ct = fis.read(buf);
                dos.write(buf, 0, ct);
                dos.flush();

                readbytes += ct;
                publishProgress((int) readbytes, (int) filelength);
            }
            fis.close();
            dos.writeBytes(NL+"--"+BOUNDARY+NL+"Content-Disposition: form-data; name=\"Send\""+NL+NL+"Send");
            dos.writeBytes(NL+"--"+BOUNDARY+"--"+NL);
            dos.flush();
            dos.close();
            DataInputStream dis = new DataInputStream(conn.getInputStream());
            byte[] data = new byte[10240];
            dis.read(data);
            dis.close();
            conn.disconnect();

            return null;
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
    
    @Override
    protected void onProgressUpdate(Integer... values)
    {
        int ct = values[0];
        int total = values[1];

        if (progressDialog == null)
        {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setTitle(R.string.dlg_exportwigleprogress_title);
            progressDialog.setMessage(mContext.getText(R.string.dlg_exportwigleprogress_message));
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
        if (progressDialog != null)
            progressDialog.dismiss();
        progressDialog = null;
    }
}
