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
package ki.wardrive4.authenticator;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ki.wardrive4.C;
import ki.wardrive4.R;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * Utilities and low level implementations.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class AuthenticationUtils
{
    private static final String TAG = C.PACKAGE+"/"+AuthenticationUtils.class.getSimpleName();
    
    /**
     * Logs in into the web part of wardrive4.
     * 
     * @param username the username for login
     * @param password the password for login
     * 
     * @return the token for the log in
     */
    public static String login(Context ctx, String username, String password) throws IOException
    {
        String url = ctx.getResources().getText(R.string.wardrive4_weburl_ajaxlogin).toString();
        HttpPost post = new HttpPost(url);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("username", username));
        postParams.add(new BasicNameValuePair("password", password));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
        post.setEntity(entity);
        
        HttpClient client = new DefaultHttpClient();
        HttpResponse resp = client.execute(post);
        int status = resp.getStatusLine().getStatusCode();
        
        if (status != 200)
        {
            Log.d(TAG, "Authentication error into " + url + ": status " + status);
            return null;
        }
        
        return password;
    }
}
