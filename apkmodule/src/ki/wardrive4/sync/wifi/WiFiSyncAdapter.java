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
package ki.wardrive4.sync.wifi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.net.ParseException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import ki.wardrive4.C;
import ki.wardrive4.authenticator.AuthenticationConst;
import org.xml.sax.SAXException;

/**
 * Remotely synchronizes the WiFi resource.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class WiFiSyncAdapter extends AbstractThreadedSyncAdapter
{
    private static final String TAG = C.PACKAGE + "/" + WiFiSyncAdapter.class.getSimpleName();
    
    private static final String SYNC_MARKER_KEY = C.PACKAGE+".sync.wifi.marker";
    
    private final AccountManager mAccountManager;
    private final Context mContext;

    public WiFiSyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult sr)
    {
        try
        {
            String password = mAccountManager.blockingGetAuthToken(account, AuthenticationConst.AUTHTOKEN_TYPE, true);
            
            // Push all the wifis that need to be uploaded
            SyncUtils.push(mContext, account.name, password);
            
            // Use the mark technique and ask the new items since the last fetch.
            long marker = getServerSyncMarker(account);
            marker = SyncUtils.fetch(mContext, account.name, password, marker);
            setServerSyncMarker(account, marker);
        }
        catch (final OperationCanceledException e)
        {
            Log.e(TAG, e.getMessage(), e);
        }
        catch (final AuthenticatorException e)
        {
            Log.e(TAG, e.getMessage(), e);
            sr.stats.numAuthExceptions++;
        }
        catch (final IOException e)
        {
            Log.e(TAG, e.getMessage(), e);
            sr.stats.numIoExceptions++;
        }
        catch (final SAXException e)
        {
            Log.e(TAG, e.getMessage(), e);
            sr.stats.numParseExceptions++;
        }
        catch (final ParserConfigurationException e)
        {
            Log.e(TAG, e.getMessage(), e);
            sr.stats.numParseExceptions++;
        }
        catch (final ParseException e)
        {
            Log.e(TAG, e.getMessage(), e);
            sr.stats.numParseExceptions++;
        }
    }

    /**
     * This helper function fetches the last known high-water-mark
     * we received from the server - or 0 if we've never synced.
     * @param account the account we're syncing
     * @return the change high-water-mark
     */
    private long getServerSyncMarker(Account account) {
        String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
        if (!TextUtils.isEmpty(markerString)) {
            return Long.parseLong(markerString);
        }
        return 0;
    }

    /**
     * Save off the high-water-mark we receive back from the server.
     * @param account The account we're syncing
     * @param marker The high-water-mark we want to save.
     */
    private void setServerSyncMarker(Account account, long marker) {
        mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
    }
}
