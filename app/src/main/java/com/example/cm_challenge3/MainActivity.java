package com.example.cm_challenge3;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;

import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.TooltipPositionMode;

import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String topicTemp = "topictemp";
    private final String topicHum = "topichum";

    private final String topicLED = "topicled";

    private TextView textViewTemperature;
    private TextView textViewHumidity;
    private MqttClient mqttClient;

    private Cartesian cartesian;
    private List<DataEntry> seriesData;
    private Line series;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);

        ToggleButton toggleTemperature = findViewById(R.id.toggleTemperature);
        ToggleButton toggleHumidity = findViewById(R.id.toggleHumidity);

        // Listener for ToggleButtons
        toggleTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update values when the temperature ToggleButton state changes
            updateDisplayedValues();
        });

        toggleHumidity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update values when the humidity ToggleButton state changes
            updateDisplayedValues();
        });

        ToggleButton toggleLED = findViewById(R.id.toggleLED);
        toggleLED.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Check the state of the LED ToggleButton and send the appropriate MQTT message to control the LED
            if (isChecked) {
                publishMessage("ON");
            } else {
                publishMessage("OFF");
            }
        });

        AnyChartView anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        cartesian = AnyChart.line();

        cartesian.animation(true);
        cartesian.padding(10d, 20d, 5d, 20d);
        cartesian.crosshair().enabled(true);
        cartesian.crosshair().yLabel(true);
        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("Trend of Sales of the Most Popular Products of ACME Corp.");
        cartesian.yAxis(0).title("Number of Bottles Sold (thousands)");
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        seriesData = new ArrayList<>();
        series = cartesian.line(seriesData);

        anyChartView.setChart(cartesian);
        anyChartView.setChart(cartesian);

        connectMQTT();
    }

    private void updateGraphData(String x, String value, String value2) {
        CustomDataEntry newDataEntry = new CustomDataEntry(x, Double.parseDouble(value), Double.parseDouble(value2), 0);
        seriesData.add(newDataEntry);

        Set set = Set.instantiate();
        set.data(seriesData);

        Mapping seriesMapping = set.mapAs("{ x: 'x', value: 'value' }");
        series.data(seriesMapping);
    }

    private void handleTemperatureData(String payload) {
        textViewTemperature.setText("Temperature: " + payload + "C");
        double temp = Double.parseDouble(payload);
        if (temp > 30.0) {
            showTemperatureAlert();
        }

        // Update the graph data when new temperature data is received
        updateGraphData(String.valueOf(System.currentTimeMillis()), payload, "0");
    }

    private void handleHumidityData(String payload) {
        textViewHumidity.setText("Humidity: " + payload + "%");
        double hum = Double.parseDouble(payload);

        if (hum > 80.0) {
            showHumidityAlert();
        }

        // Update the graph data when new humidity data is received
        updateGraphData(String.valueOf(System.currentTimeMillis()), "0", payload);
    }

    private void connectMQTT() {
        try {
            String broker = "tcp://broker.hivemq.com:1883";
            mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    runOnUiThread(() -> {
                        try {
                            handleIncomingMessage(topic, message);
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            mqttClient.connect(options);
            mqttClient.subscribe(topicTemp);
            mqttClient.subscribe(topicHum);
            mqttClient.subscribe(topicLED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingMessage(String topic, MqttMessage message) throws MqttException {
        String payload = new String(message.getPayload());

        boolean showTemperature = ((ToggleButton) findViewById(R.id.toggleTemperature)).isChecked();
        boolean showHumidity = ((ToggleButton) findViewById(R.id.toggleHumidity)).isChecked();

        if (topic.equals(topicTemp) && showTemperature) {
            handleTemperatureData(payload);
        }
        if (topic.equals(topicHum) && showHumidity) {
            handleHumidityData(payload);
        }
    }

    private void updateDisplayedValues() {
        boolean showTemperature = ((ToggleButton) findViewById(R.id.toggleTemperature)).isChecked();
        boolean showHumidity = ((ToggleButton) findViewById(R.id.toggleHumidity)).isChecked();

        if (showTemperature) {
            textViewTemperature.setVisibility(View.VISIBLE);
        } else {
            textViewTemperature.setVisibility(View.GONE);
        }

        if (showHumidity) {
            textViewHumidity.setVisibility(View.VISIBLE);
        } else {
            textViewHumidity.setVisibility(View.GONE);
        }
    }

    private void showTemperatureAlert() {
        showNotification("Temperature Alert", "Temperature is too high!");
    }

    private void showHumidityAlert() {
        showNotification("Humidity Alert", "Humidity is too high!");
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default_channel", "Default Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    private void publishMessage(String message) {
        Log.d("MeuApp", "Before publishing MQTT message to turn on/off the LED");

        try {
            mqttClient.publish(topicLED, new MqttMessage(message.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MeuApp", "Error publishing MQTT message: " + e.getMessage());
        }

        Log.d("MeuApp", "After publishing MQTT message to turn on/off the LED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CustomDataEntry extends ValueDataEntry {
        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }
    }
}
