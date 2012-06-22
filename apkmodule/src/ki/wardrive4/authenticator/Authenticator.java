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

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ki.wardrive4.C;

/**
 * Authentication interfacing to system.
 *
 * System will call methods in this implementation when an account is created,
 * deleted or updated.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Authenticator extends AbstractAccountAuthenticator
{
    private static final String TAG = C.PACKAGE+"/"+Authenticator.class.getSimpleName();
    
    private static final String ERR_INVALID_TOKEN_TYPE = "invalid authTokenType";

    private final Context mContext;

    public Authenticator(Context context)
    {
        super(context);
        mContext = context;
    }

    /**
     * Returns the token for authentication saved for this account, or prompt
     * for a new login if none is found.
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) throws NetworkErrorException
    {
        // Make sure only this application's type is used
        if (!AuthenticationConst.AUTHTOKEN_TYPE.equals(authTokenType))
        {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, ERR_INVALID_TOKEN_TYPE);
            return result;
        }

        // Obtains the token
        // The token here is the password itself, in the case the server will
        // not be able to implement a token mechanism.
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (!TextUtils.isEmpty(password))
        {
            // Token was saved, reauthenticate to make sure it is correct and
            // still valid.
            String authToken = null;
            try
            {
                authToken = AuthenticationUtils.login(mContext, account.name, password);
            }
            catch (IOException ex)
            {
                Log.e(TAG, ex.getMessage(), ex);
            }
            
            if (!TextUtils.isEmpty(authToken))
            {
                Log.i(TAG, "Token Authenticated for user " + account.name);
                
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, AuthenticationConst.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                return result;
            }
        }

        // No token saved, prompt for login.
        // Prompt for the username and password using the login activity.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException
    {
        final AccountManager am = AccountManager.get(mContext);
        // Check how many accounts are already registered.
        Account[] accounts = am.getAccountsByType(AuthenticationConst.ACCOUNT_TYPE);
        // Allow only one account for this type.
        if (accounts.length > 0)
        {
            if (C.DEBUG)
                Log.d(TAG, "Tried to add an account with one already existing.");
            return null;
        }

        // Intent that will prompt for a username and password.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        // Wrapper returning the intent.
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse aar, Account acnt, String[] strings) throws NetworkErrorException
    {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse aar, String string)
    {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse aar, Account acnt, Bundle bundle) throws NetworkErrorException
    {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String string)
    {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse aar, Account acnt, String string, Bundle bundle) throws NetworkErrorException
    {
        return null;
    }
}
