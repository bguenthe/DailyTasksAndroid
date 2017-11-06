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
    public Date startTimestamp;
    public long duration;

    public DailyTask(String name, Date startTimestamp, long duration) {
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.duration = duration;
    }
}
