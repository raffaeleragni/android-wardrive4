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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import java.util.List;
import ki.wardrive4.R;
import ki.wardrive4.authenticator.AuthenticationConst;
import ki.wardrive4.authenticator.AuthenticatorActivity;
import ki.wardrive4.provider.wifi.WiFiContract;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Settings extends PreferenceActivity
{
    public static final String PREF_GPSERROR = "gpserror";
    public static final String PREF_MINLEVEL = "minlevel";
    public static final String PREF_MAPFOLLOWME = "mapfollowme";
    public static final String PREF_MAPSHOWSAT = "mapshowsat";
    public static final String PREF_MAPSHOWLABELS = "mapshowlabels";
    public static final String PREF_MAPSHOWOPEN = "mapshowopen";
    public static final String PREF_MAPSHOWCLOSED = "mapshowclosed";
    public static final String PREF_MAPSHOWWEP = "mapshowwep";
    public static final String PREF_MAPSHOWSTATS = "mapshowstats";
    public static final String PREF_FILTERFROMDATECHECK = "filterfromdatecheck";
    public static final String PREF_FILTERFROMDATE = "filterfromdate";
    public static final String PREF_WIGLE_USERNAME = "wigleUsername";
    public static final String PREF_WIGLE_PASSWORD = "wiglePassword";
    public static final String PREF_AUTO_START_SCAN = "autostartscan";
    
    @Override
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    public static class SettingsAccount extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_account);

            Preference accountPrefs = findPreference("accountPrefs");
            accountPrefs.setOnPreferenceClickListener(onAccountClick);
        }
        
        Preference.OnPreferenceClickListener onAccountClick = new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference prfrnc)
            {
                final AccountManager am = AccountManager.get(getActivity());
                Account[] accounts = am.getAccountsByType(AuthenticationConst.ACCOUNT_TYPE);
                boolean newAccount = accounts.length == 0;

                if (newAccount)
                    startActivity(new Intent(getActivity(), AuthenticatorActivity.class));
                else
                    startActivity(new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS));

                return true;
            }
        };
    }

    public static class SettingsGPS extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_gps);
        }
    }

    public static class SettingsMap extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_map);
        }
    }

    public static class SettingsData extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_data);
            
            Preference accountPrefs = findPreference("erasealldata");
            accountPrefs.setOnPreferenceClickListener(onEraseAllClick);
        }
        
        Preference.OnPreferenceClickListener onEraseAllClick = new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference prfrnc)
            {
                new AlertDialog.Builder(SettingsData.this.getActivity())
                    .setTitle(R.string.settings_erasealldata_dlg_title)
                    .setMessage(R.string.settings_erasealldata_dlg_message)
                    .setNegativeButton(R.string.Cancel, null)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface di, int i)
                        {
                            ContentResolver contentResolver = SettingsData.this.getActivity().getContentResolver();
                            contentResolver.delete(WiFiContract.WiFiSpot.CONTENT_URI, null, null);
                            contentResolver.delete(WiFiContract.WiFi.CONTENT_URI, null, null);
                        }
                    })
                    .create().show();

                return true;
            }
        };
    }
}