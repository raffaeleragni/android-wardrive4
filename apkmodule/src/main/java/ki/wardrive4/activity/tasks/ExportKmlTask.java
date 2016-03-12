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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.data.WiFiSecurity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Export all WiFis to a kml file format.
 * 
 * KML files are opened by Google Earth application.
 * Imported some stuff from the old wardrive.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ExportKmlTask extends AsyncTask<File, Integer, Void>
{
    public interface OnFinish{void onFinish();}

    private static final String TAG = C.PACKAGE+"/"+ExportKmlTask.class.getSimpleName();
    
    private static final String STYLE_RED = "<styleUrl>#red</styleUrl>";
	private static final String STYLE_YELLOW = "<styleUrl>#yellow</styleUrl>";
	private static final String STYLE_GREEN = "<styleUrl>#green</styleUrl>";
	private static final String ROOT_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Style id=\"red\"><IconStyle><Icon><href>http://maps.google.com/mapfiles/ms/icons/red-dot.png</href></Icon></IconStyle></Style> <Style id=\"yellow\"><IconStyle><Icon><href>http://maps.google.com/mapfiles/ms/icons/yellow-dot.png</href></Icon></IconStyle></Style><Style id=\"green\"><IconStyle><Icon><href>http://maps.google.com/mapfiles/ms/icons/green-dot.png</href></Icon></IconStyle></Style>";
	private static final String ROOT_END = "\n</Document></kml>";
	private static final String MARK_START = "\n\t<Placemark>";
	private static final String MARK_END = "\n\t</Placemark>";
	private static final String NAME_START = "\n\t\t<name><![CDATA[";
	private static final String NAME_END = "]]></name>";
	private static final String DESCRIPTION_START = "\n\t\t<description><![CDATA[";
	private static final String DESCRIPTION_END = "]]></description>";
	private static final String POINT_START = "\n\t\t<Point>";
	private static final String POINT_END = "\n\t\t</Point>";
	private static final String COORDINATES_START = "\n\t\t<coordinates>";
	private static final String COORDINATES_END = "</coordinates>";
	private static final String GENERICS_INFO_1 = "BSSID: <b>";
	private static final String GENERICS_INFO_2 = "</b><br/>Capabilities: <b>";
	private static final String GENERICS_INFO_3 = "</b><br/>Frequency: <b>";
	private static final String GENERICS_INFO_4 = "</b><br/>Level: <b>";
	private static final String GENERICS_INFO_5 = "</b><br/>Timestamp: <b>";
	private static final String GENERICS_INFO_6 = "</b><br/>Date: <b>";
	private static final String GENERICS_INFO_END = "</b>";
	private static final String FOLDER_1 = "\n<Folder><name>Open WiFis</name>";
	private static final String FOLDER_2 = "\n</Folder><Folder><name>WEP WiFis</name>";
	private static final String FOLDER_3 = "\n</Folder><Folder><name>Closed WiFis</name>";
	private static final String FOLDER_END = "\n</Folder>";
    
	private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    
    private ProgressDialog progressDialog = null;
    
    private Context mContext;
    
    private OnFinish mOnFinish;
    
    public ExportKmlTask(Context mContext, OnFinish mOnFinish)
    {
        this.mContext = mContext;
        this.mOnFinish = mOnFinish;
    }
    
    @Override
    protected Void doInBackground(File... paramss)
    {
        try
        {
            Log.i(TAG, "Starting export task");
            
            File outputFile = paramss[0];
            if (outputFile.exists())
                outputFile.delete();
            outputFile.createNewFile();
            
            String[] projection = new String[]
            {
                WiFiContract.WiFi.COLUMN_NAME_BSSID,
                WiFiContract.WiFi.COLUMN_NAME_SSID,
                WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES,
                WiFiContract.WiFi.COLUMN_NAME_FREQUENCY,
                WiFiContract.WiFi.COLUMN_NAME_LEVEL,
                WiFiContract.WiFi.COLUMN_NAME_LAT,
                WiFiContract.WiFi.COLUMN_NAME_LON,
                WiFiContract.WiFi.COLUMN_NAME_ALT,
                WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP
            };
            
            FileWriter fw = new FileWriter(outputFile);
            try
            {
                fw.append(ROOT_START);

                Cursor c_open = null, c_wep = null, c_closed = null;

                try
                {
                    c_open = mContext.getContentResolver().query(WiFiContract.WiFi.CONTENT_URI,
                        projection,
                        WiFiContract.WiFi.COLUMN_NAME_SECURITY + " = ?",
                        new String[] {String.valueOf(WiFiSecurity.OPEN.ordinal())},
                        WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP + " asc");
                    c_wep = mContext.getContentResolver().query(WiFiContract.WiFi.CONTENT_URI,
                        projection,
                        WiFiContract.WiFi.COLUMN_NAME_SECURITY + " = ?",
                        new String[] {String.valueOf(WiFiSecurity.WEP.ordinal())},
                        WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP + " asc");
                    c_closed = mContext.getContentResolver().query(WiFiContract.WiFi.CONTENT_URI,
                        projection,
                        WiFiContract.WiFi.COLUMN_NAME_SECURITY + " = ?",
                        new String[] {String.valueOf(WiFiSecurity.CLOSED.ordinal())},
                        WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP + " asc");
                    
                    int ct = 1;
                    int total = c_open.getCount() + c_wep.getCount() + c_closed.getCount();
                    
                    fw.append(FOLDER_1);
                    
                    while (c_open.moveToNext())
                    {
                        writeWiFi(c_open, fw);
                        publishProgress(ct++, total);
                    }
                    
                    fw.append(FOLDER_2);
                    
                    while (c_wep.moveToNext())
                    {
                        writeWiFi(c_wep, fw);
                        publishProgress(ct++, total);
                    }
                    
                    fw.append(FOLDER_3);
                    
                    while (c_closed.moveToNext())
                    {
                        writeWiFi(c_closed, fw);
                        publishProgress(ct++, total);
                    }
                    
                    fw.append(FOLDER_END);
                }
                finally
                {
                    if (c_open != null) c_open.close();
                    if (c_wep != null) c_wep.close();
                    if (c_closed != null) c_closed.close();
                }
                
                fw.append(ROOT_END);
            }
            finally
            {
                fw.close();
            }
            
            return null;
        }
        catch (Exception e)
        {
            Log.e(TAG, "Export KML error", e);
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
            progressDialog.setTitle(R.string.dlg_exportkmlprogress_title);
            progressDialog.setMessage(mContext.getText(R.string.dlg_exportkmlprogress_message));
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
        
        if (mOnFinish != null)
            mOnFinish.onFinish();
    }

	private static void writeWiFi(Cursor c, FileWriter fw) throws IOException
	{
		String cap = c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES));
		boolean open = cap == null || cap.length() == 0;
		boolean wep = cap != null && cap.contains("WEP");
		fw.append(MARK_START);
		fw.append(NAME_START);
		fw.append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_SSID)));
		fw.append(NAME_END);
		fw.append(DESCRIPTION_START);
		fw.append(GENERICS_INFO_1);
		fw.append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_BSSID)));
		fw.append(GENERICS_INFO_2);
		fw.append(cap); // CAPABILITIES
		fw.append(GENERICS_INFO_3);
		fw.append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY)));
		fw.append(GENERICS_INFO_4);
		fw.append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LEVEL)));
		fw.append(GENERICS_INFO_5);
		fw.append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP)));
		fw.append(GENERICS_INFO_6);
		fw.append(dateFormat.format(new Date(c.getLong(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP)))));
		fw.append(GENERICS_INFO_END);
		fw.append(DESCRIPTION_END);
		fw.append(open ? STYLE_GREEN : (wep ? STYLE_YELLOW : STYLE_RED)); // Dot color
		fw.append(POINT_START);
		fw.append(COORDINATES_START);
		fw.append(
            c.getDouble(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LON)) + "," +
            c.getDouble(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LAT)) + "," +
            c.getDouble(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_ALT)));
		fw.append(COORDINATES_END);
		fw.append(POINT_END);
		fw.append(MARK_END);
	}
}
