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
package ki.wardrive4.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class DatePreference extends DialogPreference
{
    private DateValue value = new DateValue();
    private DateValue tmpValue = new DateValue();
    
    public DatePreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DatePreference(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateDialogView()
    {
        DatePicker datePicker = new DatePicker(getContext());
        datePicker.init(value.year, value.month, value.day, mOnDateChangedListener);
        return datePicker;
    }
    
    DatePicker.OnDateChangedListener mOnDateChangedListener = new DatePicker.OnDateChangedListener()
    {
        @Override
        public void onDateChanged(DatePicker dp, int y, int m, int d)
        {
            tmpValue.year = y;
            tmpValue.month = m;
            tmpValue.day = d;
        }
    };
    
    private void updateDate()
    {
        // Reobtain the timestamp
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, value.year);
        c.set(Calendar.MONTH, value.month);
        c.set(Calendar.DAY_OF_MONTH, value.day);
        long tstamp = c.getTimeInMillis();
        // Save the value and show it in the summary
        persistLong(tstamp);
        setSummary(DateFormat.getDateInstance().format(new Date(tstamp)).toString());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        if (positiveResult)
        {
            value = tmpValue;
            // Change pointer or we'll be changing the real values the next round.
            tmpValue = new DateValue();
            updateDate();
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return Long.parseLong(a.getString(index));
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
    {
        // Date is expressed as timestamp in the setting.
        // When no value is given, defaults to now.
        Date date;
        if (restorePersistedValue)
            date = new Date(getPersistedLong(new Date().getTime()));
        else
            date = defaultValue == null ? new Date() : new Date((Long) defaultValue);

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        value.year = c.get(Calendar.YEAR);
        value.month = c.get(Calendar.MONTH);
        value.day = c.get(Calendar.DAY_OF_MONTH);
        updateDate();
        
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        if (isPersistent())
            return super.onSaveInstanceState();
        else
            return value;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state != null && state.getClass().equals(DateValue.class))
        {
            value = (DateValue) state;
        }
        else
            super.onRestoreInstanceState(state);
    }
    
    public static class DateValue implements Parcelable
    {
        public int year;
        public int month;
        public int day;
        
        public static final Parcelable.Creator<DateValue> CREATOR = new Creator<DateValue>()
        {
            @Override
            public DateValue createFromParcel(Parcel parcel)
            {
                DateValue v = new DateValue();
                v.year = parcel.readInt();
                v.month = parcel.readInt();
                v.day = parcel.readInt();
                return v;
            }

            @Override
            public DateValue[] newArray(int i)
            {
                return new DateValue[i];
            }
        };
        
        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel p, int flags)
        {
            p.writeInt(year);
            p.writeInt(month);
            p.writeInt(day);
        }
    }
}
