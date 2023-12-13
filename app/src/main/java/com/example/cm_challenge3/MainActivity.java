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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MainActivity extends AppCompatActivity {

    private final String topicTemp = "topictemp";
    private final String topicHum = "topichum";

    private final String topicLED = "topicled";


    private TextView textViewTemperature;
    private TextView textViewHumidity;
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);

        // Button to navigate to Chat activity
        Button btnOpenChat = findViewById(R.id.btnOpenChat);
        btnOpenChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Chat.class);
            startActivity(intent);
        });

        // Button to control LED
        Button btnControlLED = findViewById(R.id.btnControlLED);
        btnControlLED.setOnClickListener(v -> {
            // Implement LED control logic here
            // You might want to send a message to the Arduino to control the LED
        });

        ToggleButton toggleTemperature = findViewById(R.id.toggleTemperature);
        ToggleButton toggleHumidity = findViewById(R.id.toggleHumidity);

        // listener para os ToggleButtons
        toggleTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Atualiza os valores ao mudar o estado do ToggleButton de temperatura
            updateDisplayedValues();
        });

        toggleHumidity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Atualiza os valores ao mudar o estado do ToggleButton de humidade
            updateDisplayedValues();
        });
        ToggleButton toggleLED = findViewById(R.id.toggleLED);
        toggleLED.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Verifique o estado do ToggleButton e envie a mensagem MQTT adequada para controlar o LED
            if (isChecked) {
                publishMessage("ON");
            } else {
                publishMessage("OFF");
            }
        });

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

        if (topic.equals(topicTemp) && showTemperature) {
            handleTemperatureData(payload);
        }
        if (topic.equals(topicHum) && showHumidity) {
            handleHumidityData(payload);
        }
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