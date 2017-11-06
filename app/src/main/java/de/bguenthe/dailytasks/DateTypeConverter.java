package de.bguenthe.dailytasks;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

class DateTypeConverter {
    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long toTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
