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
import android.content.Intent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.core.cartesian.series.Line;

import com.anychart.AnyChart;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.enums.MarkerType;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final String topicTemp = "topictemp";
    private final String topicHum = "topichum";

    private final String topicLED = "topicled";


    private TextView textViewTemperature;
    private TextView textViewHumidity;
    private MqttClient mqttClient;

    private Cartesian temperatureChart;
    private Cartesian humidityChart;

    private List<DataEntry> temperatureDataEntries;
    private List<DataEntry> humidityDataEntries;
    private AnyChartView anyChartViewTemperature;
    private AnyChartView anyChartViewHumidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        anyChartViewTemperature = findViewById(R.id.any_chart_view_temperature);
        anyChartViewHumidity = findViewById(R.id.any_chart_view_humidity);

        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);

        ToggleButton toggleTemperature = findViewById(R.id.toggleTemperature);
        ToggleButton toggleHumidity = findViewById(R.id.toggleHumidity);

        toggleTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> updateDisplayedValues());
        toggleHumidity.setOnCheckedChangeListener((buttonView, isChecked) -> updateDisplayedValues());

        ToggleButton toggleLED = findViewById(R.id.toggleLED);
        toggleLED.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                publishMessage("ON");
            } else {
                publishMessage("OFF");
            }
        });

        temperatureChart = AnyChart.line();
        humidityChart = AnyChart.line();

        temperatureDataEntries = new ArrayList<>();
        humidityDataEntries = new ArrayList<>();

        connectMQTT();
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
        // Verifica o estado dos botões de alternância
        String payload = new String(message.getPayload());

        boolean showTemperature = ((ToggleButton) findViewById(R.id.toggleTemperature)).isChecked();
        boolean showHumidity = ((ToggleButton) findViewById(R.id.toggleHumidity)).isChecked();

        if (topic.equals(topicTemp)) {
            temperatureDataEntries.add(new ValueDataEntry(getTimestamp(), Double.parseDouble(payload)));
            updateTemperatureChart();


            if (showTemperature) {
                handleTemperatureData(payload);
            }
        } else if (topic.equals(topicHum)) {
            humidityDataEntries.add(new ValueDataEntry(getTimestamp(), Double.parseDouble(payload)));
            updateHumidityChart();

            if (showHumidity) {
                handleHumidityData(payload);
            }
        }
    }
    private String getTimestamp() {
        // Get the current timestamp in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
    private void updateHumidityChart() {
        humidityChart.removeAllSeries();
        Line series = humidityChart.line(humidityDataEntries);
        series.name("Humidity");
        series.hovered().markers().enabled(true);
        series.hovered().markers().type(MarkerType.CIRCLE).size(4d);
        anyChartViewHumidity.setChart(humidityChart);
    }

    private void updateTemperatureChart() {
        temperatureChart.removeAllSeries();
        Line series = temperatureChart.line(temperatureDataEntries);
        series.name("Temperature");
        series.hovered().markers().enabled(true);
        series.hovered().markers().type(MarkerType.CIRCLE).size(4d);
        anyChartViewTemperature.setChart(temperatureChart);
    }
    @SuppressLint("SetTextI18n")
    private void handleTemperatureData(String payload) {
        textViewTemperature.setText("Temperature: " + payload + "C");
        double temp = Double.parseDouble(payload);
        //mqttClient.publish(topicTemp, new MqttMessage(payload.getBytes()));
        // Example: Check if the temperature exceeds a threshold
        if (temp > 30.0) {
            showTemperatureAlert();
        }
    }

    @SuppressLint("SetTextI18n")
    private void handleHumidityData(String payload) {
        textViewHumidity.setText("Humidity: " + payload + "%");
        double hum = Double.parseDouble(payload);

        // Example: Check if humidity is too high
        if (hum > 80.0) {
            showHumidityAlert();
        }
    }

    private void updateDisplayedValues() {
        // Verifica os estados atuais dos ToggleButtons
        boolean showTemperature = ((ToggleButton) findViewById(R.id.toggleTemperature)).isChecked();
        boolean showHumidity = ((ToggleButton) findViewById(R.id.toggleHumidity)).isChecked();

        // Se os ToggleButtons estiverem marcados, atualiza os valores exibidos
        if (showTemperature) {
            textViewTemperature.setVisibility(View.VISIBLE);
            anyChartViewTemperature.setVisibility(View.VISIBLE);
        } else {
            textViewTemperature.setVisibility(View.GONE);
            anyChartViewTemperature.setVisibility(View.GONE);
        }

        if (showHumidity) {
            textViewHumidity.setVisibility(View.VISIBLE);
            anyChartViewHumidity.setVisibility(View.VISIBLE);
        } else {
            textViewHumidity.setVisibility(View.GONE);
            anyChartViewHumidity.setVisibility(View.GONE);
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

        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default_channel", "Default Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Create the notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.ic_notification)  // Ensure this matches the actual resource name
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(1, builder.build());
    }


    private void publishMessage(String message) {
        Log.d("MeuApp", "Antes de publicar a mensagem MQTT para ligar/desligar o LED");

        try {
            mqttClient.publish(topicLED, new MqttMessage(message.getBytes()));

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MeuApp", "Erro ao publicar a mensagem MQTT: " + e.getMessage());
        }

        Log.d("MeuApp", "Depois de publicar a mensagem MQTT para ligar/desligar o LED");
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
}