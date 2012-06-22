/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ki.wardrive4.sync.wifi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.data.WiFiSyncStatus;
import ki.wardrive4.provider.wifi.WiFiContract;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SyncUtils
{
    private static final String TAG = C.PACKAGE+"/"+WiFiSyncAdapter.class.getSimpleName();
    
    private static final int PAGE_LIMIT = 10;
    
    public static void fetch(Context ctx, String username, String password) throws UnsupportedEncodingException, IOException
    {
        HttpClient client = new DefaultHttpClient();
        login(ctx, client, username, password);
    }
    
    public static void push(Context ctx, String username, String password) throws UnsupportedEncodingException, IOException
    {
        HttpClient client = new DefaultHttpClient();
        int status = login(ctx, client, username, password);
        if (status != 200)
        {
            Log.e(TAG, "Sync error while logging in, status: " + status);
            return;
        }
        
        List<String> ids = new ArrayList<String>();
        String data = "<wifis></wifis>";
        Cursor c = ctx.getContentResolver().query(WiFiContract.WiFi.CONTENT_URI, 
            new String[]
            {
                WiFiContract.WiFi._ID,
                WiFiContract.WiFi.COLUMN_NAME_BSSID,
                WiFiContract.WiFi.COLUMN_NAME_SSID,
                WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES,
                WiFiContract.WiFi.COLUMN_NAME_SECURITY,
                WiFiContract.WiFi.COLUMN_NAME_LEVEL,
                WiFiContract.WiFi.COLUMN_NAME_FREQUENCY,
                WiFiContract.WiFi.COLUMN_NAME_LAT,
                WiFiContract.WiFi.COLUMN_NAME_LON,
                WiFiContract.WiFi.COLUMN_NAME_ALT,
                WiFiContract.WiFi.COLUMN_NAME_GEOHASH,
                WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP
            },
                
            WiFiContract.WiFi.COLUMN_NAME_SYNC_STATUS + " = ?",
            new String[]{String.valueOf(WiFiSyncStatus.TO_UPDATE_UPLOAD.ordinal())},
            WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP + " asc");
        try
        {
            //<wifis>
            //    <wifi id="XX">
            //        <bssid>YY</bssid>
            //        <ssid>ZZ</ssid>
            //        <capabilities></capabilities>
            //        <security>0</security>
            //        <level>-30</level>
            //        <frequency>2345</frequency>
            //        <lat>55.6</lat>
            //        <lon>88.9</lon>
            //        <alt>11.2</alt>
            //        <geohash>XXXXXXXXX</geohash>
            //        <timestamp>1340394526146</timestamp>
            //    </wifi>
            //</wifis>
            StringBuilder sb = new StringBuilder();
            sb.append("<wifis>");
            while (c.moveToNext() && ids.size() < PAGE_LIMIT)
            {
                String id = c.getString(c.getColumnIndex(WiFiContract.WiFi._ID));
                sb.append("<wifi id=\"").append(id).append("\">");
                sb.append("<bssid>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_BSSID))).append("</bssid>");
                sb.append("<ssid>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_SSID))).append("</ssid>");
                sb.append("<capabilities>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES))).append("</capabilities>");
                sb.append("<security>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_SECURITY))).append("</security>");
                sb.append("<level>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LEVEL))).append("</level>");
                sb.append("<frequency>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY))).append("</frequency>");
                sb.append("<lat>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LAT))).append("</lat>");
                sb.append("<lon>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_LON))).append("</lon>");
                sb.append("<alt>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_ALT))).append("</alt>");
                sb.append("<geohash>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_GEOHASH))).append("</geohash>");
                sb.append("<timestamp>").append(c.getString(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP))).append("</timestamp>");
                sb.append("</wifi>");
                ids.add(id);
            }
            sb.append("</wifis>");
            data = sb.toString();
        }
        finally
        {
            c.close();
        }
        
        String url = ctx.getResources().getText(R.string.wardrive4_weburl_ajaxsync).toString();
        HttpPost post = new HttpPost(url);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("action", "push"));
        postParams.add(new BasicNameValuePair("data", data));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
        post.setEntity(entity);
        
        HttpResponse resp = client.execute(post);
        status = resp.getStatusLine().getStatusCode();
        
        // Set the sent data to OK
        if (status == 200)
        {
            for (String id: ids)
            {
                ContentValues cv = new ContentValues();
                cv.put(WiFiContract.WiFi.COLUMN_NAME_SYNC_STATUS, WiFiSyncStatus.UPDATED.ordinal());
                ctx.getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
            }
            Log.i(TAG, "Sync push: sent " + ids.size() + " items.");
        }
        else
            Log.e(TAG, "Sync error, status: " + status);
    }
    
    /**
     * Shots a login prior to any request.
     */
    public static int login(Context ctx, HttpClient client, String username, String password) throws UnsupportedEncodingException, IOException
    {
        String url = ctx.getResources().getText(R.string.wardrive4_weburl_ajaxlogin).toString();
        HttpPost post = new HttpPost(url);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("username", username));
        postParams.add(new BasicNameValuePair("password", password));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
        post.setEntity(entity);
        
        HttpResponse resp = client.execute(post);
        return resp.getStatusLine().getStatusCode();
    }
}
