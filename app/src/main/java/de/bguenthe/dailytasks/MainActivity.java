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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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
        long duration;

        public LabelMapping(String button, String label, int color, long duration) {
            this.button = button;
            this.label = label;
            this.color = color;
            this.duration = duration;
        }

        public void setActive() {
            setDeactive();
            this.label = "*" + this.label + "*";
        }

        public void setDeactive() {
            this.label = this.label.replace("*", "");
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
        //database.dailyTaskDao().removeAllTasks();

        constbuttotolabels.put("blue", new LabelMapping("blue", "Wartung", Color.BLUE,0));
        constbuttotolabels.put("green", new LabelMapping("green", "Intrastat", Color.GREEN,0));
        constbuttotolabels.put("black", new LabelMapping("black", "Coffee", Color.BLACK,0));
        constbuttotolabels.put("white", new LabelMapping("white", "Termin", Color.WHITE,0));
        buildChart();
    }

    public void buildChart() {
        LinkedHashMap<String, Result> arrayMap = new LinkedHashMap<>();
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
        int i = 0;
        for (String key : arrayMap.keySet()) {
            Result r = arrayMap.get(key);
            float percent = ((float) r.duration / (float) maxDuration) * 100f;
            barEntries.add(new BarEntry(i, percent));
            LabelMapping l = constbuttotolabels.get(key);
            l.duration = r.duration;
            currentbuttontolabels.put(key, l);
            i++;
        }

        // Falls ein leerer Button eingefügt wurde
        if (barEntries.size() < currentbuttontolabels.size()) {
            barEntries.add(new BarEntry(currentbuttontolabels.size() -1 , 0));
        }

        IAxisValueFormatter formatter = new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                LabelMapping l = (LabelMapping) currentbuttontolabels.values().toArray()[(int) value];
                return l.label + ":" + l.duration;
            }
        };

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(formatter);

        BarDataSet barDataSet = new BarDataSet(barEntries, "Tasks");
        ArrayList<Integer> colors = new ArrayList<>();
        for (String button : currentbuttontolabels.keySet()) {
            LabelMapping l = currentbuttontolabels.get(button);
            colors.add(l.color);
        }
        if (colors.size() != 0) {
            barDataSet.setColors(colors);
        }
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
            loclabel.setDeactive();
            currentbuttontolabels.put(loclabel.button, loclabel);
            if (b.equals(button)) {
                loclabel.setActive();
                currentbuttontolabels.put(b, loclabel);
            }
        }
        buildChart();
    }

    public void writeData(String button, long duration) {
        Date d = new Date();
        DailyTask dailyTask = new DailyTask(button, d, duration);
        database.dailyTaskDao().addDailyTask(dailyTask);
    }

    @Override
    protected void onDestroy() {
        long duration = dailyTaskTimer.stop();
        writeData(oldButton, duration);
        AppDatabase.destroyInstance();
        super.onDestroy();
    }
}