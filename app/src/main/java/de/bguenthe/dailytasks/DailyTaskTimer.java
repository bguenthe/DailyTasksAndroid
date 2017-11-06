package de.bguenthe.dailytasks;

import java.util.Date;

public class DailyTaskTimer {
    Date startTime;
    Date stopTime;

    Date start() {
        startTime = new Date();
        return startTime;
    }

    long stop() {
        stopTime = new Date();
        return stopTime.getTime() - startTime.getTime();
    }
}