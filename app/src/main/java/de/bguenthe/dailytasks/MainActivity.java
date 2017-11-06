package de.bguenthe.dailytasks;

import android.content.Intent;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class MainActivity extends AppCompatActivity {
    class Result {
        public Date startTimestamp;
        public long duration;
    }

    class LabelMapping {
        String button;
        String label;
        int color;

        public LabelMapping(String button, String label, int color) {
            this.button = button;
            this.label = label;
            this.color = color;
        }

        public String getActive() {
            return "*" + this.label + "*";
        }
    }

    private AppDatabase database;
    BarChart barChart;
    DailyTaskTimer dailyTaskTimer = new DailyTaskTimer();
    String oldButton = "";

    final ArrayMap<String, LabelMapping> constbuttotolabels = new ArrayMap<>();
    LinkedHashMap<String, LabelMapping> currentbuttontolabels = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Config.setFlicCredentials();

        barChart = findViewById(R.id.barchart);

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        // barChart.setMaxVisibleValueCount(50);
        YAxis yAxis = barChart.getAxisLeft();
        YAxis yAxisr = barChart.getAxisRight();
        barChart.getAxisRight().setEnabled(false); // no right axis

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        yAxis.setAxisMinimum(0f); // start at zero
        yAxis.setAxisMaximum(100f); // the axis maximum is 100
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(true);

// Auskommentiert, da ich alle Buttons bereits fange
//        try {
//            FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
//                @Override
//                public void onInitialized(FlicManager manager) {
//                    manager.initiateGrabButton(MainActivity.this);
//                }
//            });
//        } catch (FlicAppNotInstalledException err) {
//            Toast.makeText(this, "Flic App is not installed", Toast.LENGTH_SHORT).show();
//        }

        database = AppDatabase.getDatabase(getApplicationContext());
        //database.dailyTaskDao().removeAllDailyTasks();

        constbuttotolabels.put("blue", new LabelMapping("blue", "Wartung", Color.BLUE));
        constbuttotolabels.put("green", new LabelMapping("green", "Intrastat", Color.GREEN));
        constbuttotolabels.put("black", new LabelMapping("black", "Coffee", Color.BLACK));
        constbuttotolabels.put("white", new LabelMapping("white", "Termin", Color.WHITE));
        buildChart();
    }

    public void buildChart() {
        ArrayMap<String, Result> arrayMap = new ArrayMap<>();
        long maxDuration = 0;
        List<DailyTask> dt = database.dailyTaskDao().getAllDailyTask();
        Date current = new Date();
        DateFormat dateformatter = new SimpleDateFormat("yyyyMMdd");
        String yyyymmdd = dateformatter.format(current);
        for (int i = 0; i < dt.size(); ++i) {
            Date startTimestamp = dt.get(i).startTimestamp;
            String locyyyymmdd = dateformatter.format(startTimestamp);
            if (!yyyymmdd.equals(locyyyymmdd)) {
                continue;
            }
            String task = dt.get(i).name;
            Result r = new Result();
            maxDuration += dt.get(i).duration;
            long duration = dt.get(i).duration;
            if (arrayMap.get(task) != null) {
                r = arrayMap.get(task);
                r.duration += duration;
            } else {
                r.duration = duration;
            }
            arrayMap.put(task, r);
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < arrayMap.size(); i++) {
            String key = arrayMap.keyAt(i);
            Result r = arrayMap.valueAt(i);
            float percent = ((float) r.duration / (float) maxDuration) * 100f;
            barEntries.add(new BarEntry(i + 1, percent));
            currentbuttontolabels.put(key, constbuttotolabels.get(key));
        }

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                LabelMapping l = (LabelMapping) currentbuttontolabels.values().toArray()[(int) value - 1];
                return l.label;
            }
        };

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(formatter);

        BarDataSet barDataSet = new BarDataSet(barEntries, "Tasks");
        ArrayList<Integer> colors = new ArrayList<>();
        for (String button : currentbuttontolabels.keySet()) {
            LabelMapping l  = currentbuttontolabels.get(button);
            colors.add(l.color);
        }
        barDataSet.setColors(colors);
        barDataSet.setValueTextSize(12f);

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);
        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                if (button != null) {
                    button.registerListenForBroadcast(FlicBroadcastReceiverFlags.UP_OR_DOWN | FlicBroadcastReceiverFlags.REMOVED);
                    Toast.makeText(MainActivity.this, "Grabbed a button", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Did not grab any button", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String button = intent.getStringExtra("button");
        long duration = 0;

        if (!button.equals(oldButton)) {
            if (oldButton.equals("")) {
                dailyTaskTimer.start();
            } else {
                duration = dailyTaskTimer.stop();
                writeData(oldButton, duration);
                dailyTaskTimer.start();
            }
        }
        oldButton = button;
        setActive(button);
    }

    private void setActive(String button) {
        XAxis xAxis = barChart.getXAxis();
        int count = currentbuttontolabels.size();
        LabelMapping labelMapping = currentbuttontolabels.get(button);
        if (labelMapping == null) {
            currentbuttontolabels.put(button, constbuttotolabels.get(button));
        }
        for (String b : currentbuttontolabels.keySet()) {
            LabelMapping loclabel = currentbuttontolabels.get(b);
            loclabel.label = loclabel.label.replace("*", "");
            currentbuttontolabels.put(loclabel.button, loclabel);
            if (b.equals(button)) {
                loclabel.label = "*" + loclabel.label + "*";
                currentbuttontolabels.put(loclabel.label, loclabel);
            }
        }
        barChart.invalidate();
    }

    public void writeData(String button, long duration) {
        Date d = new Date();
        DailyTask dailyTask = new DailyTask(button, d, duration);
        database.dailyTaskDao().addDailyTask(dailyTask);
        buildChart();
    }

    @Override
    protected void onDestroy() {
        long duration = dailyTaskTimer.stop();
        writeData(oldButton, duration);
        AppDatabase.destroyInstance();
        super.onDestroy();
    }
}