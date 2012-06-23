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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import ki.wardrive4.C;
import ki.wardrive4.R;
import ki.wardrive4.activity.tasks.ParseWiFiTask;
import ki.wardrive4.data.WiFiSyncStatus;
import ki.wardrive4.provider.wifi.WiFiContract;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SyncUtils
{
    private static final String TAG = C.PACKAGE+"/"+WiFiSyncAdapter.class.getSimpleName();
    
    private static final int PAGE_LIMIT = 250;
    
    /**
     * @return the max of the marker, to be kept for later user
     */
    public static long fetch(Context ctx, String username, String password, long marker) throws UnsupportedEncodingException, IOException, ParserConfigurationException, SAXException
    {
        HttpClient client = new DefaultHttpClient();
        login(ctx, client, username, password);
        
        String url = ctx.getResources().getText(R.string.wardrive4_weburl_ajaxsync).toString();
        HttpPost post = new HttpPost(url);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("action", "fetch"));
        postParams.add(new BasicNameValuePair("mark", String.valueOf(marker)));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
        post.setEntity(entity);
        
        HttpResponse resp = client.execute(post);
        int status = resp.getStatusLine().getStatusCode();
        if (status != 200)
        {
            Log.e(TAG, "Sync error while fetching, status: " + status);
            return marker;
        }
        
        String xml = EntityUtils.toString(resp.getEntity());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        Document doc = db.parse(is);
        
        int ct = 0;
        long maxMarker = marker;
        NodeList list = doc.getDocumentElement().getElementsByTagName("wifi");
        for (int i = 0; i < list.getLength(); i++)
        {
            Node node = list.item(i);
            if (node instanceof Element)
            {
                Element e = (Element) node;
                String id = e.getAttribute("id");
                // Check existance...
                Cursor c = ctx.getContentResolver().query(WiFiContract.WiFi.uriById(id),
                    new String[]{WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP},
                    null,
                    null,
                    null);
                try
                {
                    ContentValues cv = new ContentValues();
                    double lat = Double.parseDouble(e.getElementsByTagName("lat").item(0).getTextContent());
                    double lon = Double.parseDouble(e.getElementsByTagName("lon").item(0).getTextContent());
                    double alt = Double.parseDouble(e.getElementsByTagName("alt").item(0).getTextContent());
                    String geohash = e.getElementsByTagName("geohash").item(0).getTextContent();
                    int level = Integer.parseInt(e.getElementsByTagName("level").item(0).getTextContent());
                    long tstamp = Long.parseLong(e.getElementsByTagName("timestamp").item(0).getTextContent());
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_BSSID, e.getElementsByTagName("bssid").item(0).getTextContent());
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_SSID, e.getElementsByTagName("ssid").item(0).getTextContent());
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_CAPABILITIES, e.getElementsByTagName("capabilities").item(0).getTextContent());
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_SECURITY, Integer.parseInt(e.getElementsByTagName("security").item(0).getTextContent()));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LEVEL, level);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_FREQUENCY, Integer.parseInt(e.getElementsByTagName("frequency").item(0).getTextContent()));
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LAT, lat);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_LON, lon);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_ALT, alt);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_GEOHASH, geohash);
                    cv.put(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP, tstamp);
                    
                    if (!c.moveToNext())
                    {
                        cv.put(WiFiContract.WiFi._ID, id);
                        ctx.getContentResolver().insert(WiFiContract.WiFi.CONTENT_URI, cv);
                        ct++;
                    }
                    // Update only if timestamp is higher - to be sure
                    else if (c.getInt(c.getColumnIndex(WiFiContract.WiFi.COLUMN_NAME_TIMESTAMP)) < tstamp)
                    {
                        ctx.getContentResolver().update(WiFiContract.WiFi.uriById(id), cv, null, null);
                        ct++;
                    }
                    
                    // In the case this is a new one or there are still no records in the wifispot
                    // fill them with these data, so that a measurement would not overwrite them.
                    int spotct = ParseWiFiTask.WIFISPOT_MAX;
                    Cursor c2 = ctx.getContentResolver().query(WiFiContract.WiFiSpot.CONTENT_URI,
                        new String[]{WiFiContract.WiFiSpot._ID},
                        WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI + " = ?",
                        new String[]{id},
                        WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL + " desc");
                    try
                    {
                        spotct -= c2.getCount();
                        // Insert as many as the empty spots are still available
                        for (int j = 0; j < spotct; j++)
                        {
                            cv = new ContentValues();
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_FK_WIFI, id);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LAT, lat);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LON, lon);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_ALT, alt);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_GEOHASH, geohash);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_LEVEL, level);
                            cv.put(WiFiContract.WiFiSpot.COLUMN_NAME_TIMESTAMP, tstamp);
                            ctx.getContentResolver().insert(WiFiContract.WiFiSpot.CONTENT_URI, cv);
                        }
                    }
                    finally
                    {
                        c2.close();
                    }
                    
                    // marker becomes the max tstamp
                    maxMarker = maxMarker < tstamp ? tstamp : maxMarker;
                }
                finally
                {
                    c.close();
                }
            }
        }
        
        if (ct > 0)
            Log.i(TAG, "Sync fetch: received " + ct + " items.");

        return maxMarker;
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

        // Nothing to send, do not print any log.
        if (ids.isEmpty())
            return;
        
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
            Log.e(TAG, "Sync push error, status: " + status);
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
