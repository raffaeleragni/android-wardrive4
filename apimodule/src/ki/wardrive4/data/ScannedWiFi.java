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
package ki.wardrive4.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A WiFi that has been scanned by the service, thus including also the location.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ScannedWiFi implements Parcelable
{
    public String bssid;
    public String ssid;
    public String capabilities;
    public int level;
    public int frequency;
    public double lat;
    public double lon;
    public double alt;
    public long timestamp;
    public float gpserror;

    public ScannedWiFi()
    {
    }

    public ScannedWiFi(String bssid, String ssid, String capabilities, int level, int frequency, double lat, double lon, double alt, long timestamp, float gpserror)
    {
        this.bssid = bssid;
        this.ssid = ssid;
        this.capabilities = capabilities;
        this.level = level;
        this.frequency = frequency;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.timestamp = timestamp;
        this.gpserror = gpserror;
    }

    public static final Parcelable.Creator<ScannedWiFi> CREATOR = new Parcelable.Creator<ScannedWiFi>()
    {
        @Override
        public ScannedWiFi createFromParcel(Parcel parcel)
        {
            return new ScannedWiFi(parcel);
        }

        @Override
        public ScannedWiFi[] newArray(int i)
        {
            return new ScannedWiFi[i];
        }
    };

    private ScannedWiFi(Parcel in)
    {
        bssid = in.readString();
        ssid = in.readString();
        capabilities = in.readString();
        level = in.readInt();
        frequency = in.readInt();
        lat = in.readDouble();
        lon = in.readDouble();
        alt = in.readDouble();
        timestamp = in.readLong();
        gpserror = in.readFloat();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(bssid);
        parcel.writeString(ssid);
        parcel.writeString(capabilities);
        parcel.writeInt(level);
        parcel.writeInt(frequency);
        parcel.writeDouble(lat);
        parcel.writeDouble(lon);
        parcel.writeDouble(alt);
        parcel.writeLong(timestamp);
        parcel.writeFloat(gpserror);
    }

    @Override
    public String toString()
    {
        return "ScannedWiFi{" + "bssid=" + bssid + ", ssid=" + ssid + ", capabilities=" + capabilities + ", level=" + level + ", frequency=" + frequency + ", lat=" + lat + ", lon=" + lon + ", alt=" + alt + ", timestamp=" + timestamp + ", gpserror=" + gpserror + '}';
    }
}
