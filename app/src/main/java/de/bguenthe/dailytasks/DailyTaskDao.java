package de.bguenthe.dailytasks;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;


@Dao
public interface DailyTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addDailyTask(DailyTask dailyTask);

    @Query("select * from dailytask order by startTimestamp")
    public List<DailyTask> getAllDailyTask();

    @Query("select * from dailytask where id = :taskId")
    public List<DailyTask> getDailyTask(long taskId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(DailyTask dailyTask);

    @Query("delete from dailytask")
    void removeAllTasks();

    @Query("select * from dailytask where mqttsend = 0")
    public List<DailyTask> getAllDailyTaskNotSend();
}