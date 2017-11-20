package de.bguenthe.dailytasks;

import java.util.Date;

public class DailyTaskTimer {
    Date startTime;
    Date stopTime;
    boolean isRunning = false;

    Date start() {
        startTime = new Date();
        isRunning = true;
        return startTime;
    }

    long stop() {
        stopTime = new Date();
        isRunning = false;
        return stopTime.getTime() - startTime.getTime();
    }
}