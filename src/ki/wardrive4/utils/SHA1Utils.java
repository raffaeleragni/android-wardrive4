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
package ki.wardrive4.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SHA1Utils
{
    private static final String SHA1 = "SHA1";

    public static String sha1(String s) throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance(SHA1);
        String result = "";
        byte[] data = digest.digest(s.getBytes());
        for (byte b: data)
        {
            String hex = Integer.toHexString(0x0FF & b);
            if (hex.length() < 2)
                hex = '0'+hex;
            result += hex.toLowerCase();
        }
        return result;
    }
}
