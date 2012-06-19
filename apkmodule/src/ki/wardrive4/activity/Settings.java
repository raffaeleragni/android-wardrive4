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

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import java.util.List;
import ki.wardrive4.R;

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
    
    @Override
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.settings_headers, target);
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
}
