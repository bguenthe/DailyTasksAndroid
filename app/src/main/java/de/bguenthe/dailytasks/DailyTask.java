package de.bguenthe.dailytasks;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;


@Entity
public class DailyTask {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String button;
    public Date startTimestamp;
    public long duration;
    public boolean mqttsend = false;

    public DailyTask(String name, String button, Date startTimestamp, long duration, boolean mqttsend) {
        this.name = name;
        this.button = button;
        this.startTimestamp = startTimestamp;
        this.duration = duration;
        this.mqttsend = mqttsend;
    }
}
