package de.bguenthe.dailytasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String TAG = "de.bguenthe.dailytasks";

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

    MqttAndroidClient mqttAndroidClient;
    final String publishTopic = "dailytask";
    private JSONObject data = new JSONObject();

    final String HOST = "odroidnas.wkjihn0g7rs5uxq0.myfritz.net";
    private final int PORT = 1883;
    private final String uri = "tcp://" + HOST + ":" + PORT;
    private final int MINTIMEOUT = 2000;
    private final int MAXTIMEOUT = 32000;
    private int timeout = MINTIMEOUT;

    String clientId = "DailyTaskClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Config.setFlicCredentials();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

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

        constbuttotolabels.put("blue", new LabelMapping("blue", "Wartung", Color.BLUE, 0));
        constbuttotolabels.put("green", new LabelMapping("green", "Intrastat", Color.GREEN, 0));
        constbuttotolabels.put("black", new LabelMapping("black", "Coffee", Color.BLACK, 0));
        constbuttotolabels.put("white", new LabelMapping("white", "Termin", Color.WHITE, 0));
        buildChart();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), uri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    // Because Clean Session is true, we need to re-subscribe
                } else {
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logMessage("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            logMessage("Connecting to " + uri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }

        DailyTaskSender task = new DailyTaskSender();
        task.execute(new String[]{"http://www.vogella.com/index.html"});
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
            barEntries.add(new BarEntry(currentbuttontolabels.size() - 1, 0));
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
                writeData(oldButton, currentbuttontolabels.get(oldButton).label, duration);
                dailyTaskTimer.start();
            }
        }
        oldButton = button;
        setActive(button);
    }

    private void stopCurrentTask() {
        if (dailyTaskTimer.isRunning) {
            long duration = dailyTaskTimer.stop();
            writeData(oldButton, currentbuttontolabels.get(oldButton).label, duration);
            for (String b : currentbuttontolabels.keySet()) {
                LabelMapping loclabel = currentbuttontolabels.get(b);
                loclabel.setDeactive();
            }
            oldButton = "";
            buildChart();
        }
    }

    private void setActive(String button) {
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

    public void writeData(String button, String task, long duration) {
        Date d = new Date();
        DailyTask dailyTask = new DailyTask(button, task, d, duration, false);
        try {
            database.dailyTaskDao().addDailyTask(dailyTask);
            publishMessage(dailyTask);
            dailyTask.mqttsend = true;
            database.dailyTaskDao().update(dailyTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (dailyTaskTimer.isRunning) {
            long duration = dailyTaskTimer.stop();
            writeData(oldButton, currentbuttontolabels.get(oldButton).label, duration);
            AppDatabase.destroyInstance();
            super.onDestroy();
        }
    }

    public void logMessage(String message) {
        Log.d(TAG, message);
    }

    public void publishMessage(DailyTask dt) throws JSONException, MqttException {
        JSONObject object = new JSONObject();
        object.put("button", dt.button);
        object.put("duration", dt.duration);
        object.put("id", dt.id);
        object.put("name", dt.name);
        object.put("startTimestamp", dt.startTimestamp.getTime());

        MqttMessage message = new MqttMessage();
        message.setPayload(object.toString().getBytes());
        mqttAndroidClient.publish(publishTopic, message);
        logMessage("Message Published");
        if (!mqttAndroidClient.isConnected()) {
            logMessage(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
        }
    }

    private class DailyTaskSender extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            List<DailyTask> dt = database.dailyTaskDao().getAllDailyTaskNotSend();
            for (int i = 0; i < dt.size(); ++i) {
                try {
                    publishMessage(dt.get(i));
                    DailyTask day = dt.get(i);
                    day.mqttsend = true;
                    database.dailyTaskDao().update(day);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return "";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.button_blue:
                simulateButton("blue");
                return true;

            case R.id.button_green:
                simulateButton("green");
                return true;

            case R.id.button_white:
                simulateButton("white");
                return true;

            case R.id.button_black:
                simulateButton("black");
                return true;

            case R.id.action_stop:
                stopCurrentTask();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void simulateButton(String button) {
        Context context = getApplicationContext();
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("button", button);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
}