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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ki.wardrive4.R;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 * Mostly copied from examples.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
{
    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    private AccountManager mAccountManager;

    protected boolean mRequestNewAccount = false;
    private Boolean mConfirmCredentials = false;

    private TextView mMessage;
    private EditText mUsernameEdit;
    private EditText mPasswordEdit;

    private String mUsername;
    private String mPassword;

    private UserLoginTask mAuthTask = null;
    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mAccountManager = AccountManager.get(this);

        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo);

        mMessage = (TextView) findViewById(R.id_login.message);
        mUsernameEdit = (EditText) findViewById(R.id_login.username_edit);
        mPasswordEdit = (EditText) findViewById(R.id_login.password_edit);

        if (!TextUtils.isEmpty(mUsername))
            mUsernameEdit.setText(mUsername);

        mMessage.setText(getMessage());
    }

    private CharSequence getMessage()
    {
        if (TextUtils.isEmpty(mUsername))
            return "New account";
        if (TextUtils.isEmpty(mPassword))
            return "Missing password";
        return null;
    }

    public void handleRegister(View view)
    {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getResources().getText(R.string.wardrive4_weburl_register).toString()));
        startActivity(i);
    }

    public void handleLogin(View view)
    {
        if (mRequestNewAccount)
            mUsername = mUsernameEdit.getText().toString();
        mPassword = mPasswordEdit.getText().toString();
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword))
            mMessage.setText(getMessage());
        else
        {
            // Show a progress dialog, and kick off a background task to perform
            // the user login attempt.
            showProgress();
            mAuthTask = new UserLoginTask();
            mAuthTask.execute();
        }
    }

    private void finishConfirmCredentials(boolean result)
    {
        final Account account = new Account(mUsername, AuthenticationConst.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishLogin(String authToken)
    {
        final Account account = new Account(mUsername, AuthenticationConst.ACCOUNT_TYPE);
        if (mRequestNewAccount)
        {
            mAccountManager.addAccountExplicitly(account, mPassword, null);
            // automatic sync
            ContentResolver.setSyncAutomatically(account, WiFiContract.AUTHORITY, true);
            // Check data each 1h max
            ContentResolver.addPeriodicSync(account, WiFiContract.AUTHORITY, new Bundle(), 3600);
        }
        else
            mAccountManager.setPassword(account, mPassword);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AuthenticationConst.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onAuthenticationResult(String authToken)
    {
        boolean success = ((authToken != null) && (authToken.length() > 0));

        mAuthTask = null;

        hideProgress();

        if (success)
        {
            if (!mConfirmCredentials)
                finishLogin(authToken);
            else
                finishConfirmCredentials(success);
        }
        else
        {
            if (mRequestNewAccount)
                mMessage.setText("Wrong username or password");
            else
                mMessage.setText("Wrong password");
        }
    }

    public void onAuthenticationCancel()
    {
        mAuthTask = null;
        hideProgress();
    }

    private void showProgress()
    {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Authenticating");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                if (mAuthTask != null)
                    mAuthTask.cancel(true);
            }
        });
        // We save off the progress dialog in a field so that we can dismiss
        // it later. We can't just call dismissDialog(0) because the system
        // can lose track of our dialog if there's an orientation change.
        mProgressDialog = dialog;
        mProgressDialog.show();
    }

    private void hideProgress()
    {
        if (mProgressDialog != null)
        {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public class UserLoginTask extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                return AuthenticationUtils.login(AuthenticatorActivity.this, mUsername, mPassword);
            }
            catch (Exception ex)
            {
                Toast.makeText(AuthenticatorActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String authToken)
        {
            onAuthenticationResult(authToken);
        }

        @Override
        protected void onCancelled()
        {
            onAuthenticationCancel();
        }
    }
}
