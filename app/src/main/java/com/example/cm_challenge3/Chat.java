package com.example.cm_challenge3;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class Chat extends AppCompatActivity {

    private MQTTHelper helper;
    private TextView chat;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView ledStatusTextView;
    private Button ledControlButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI elements
        chat = findViewById(R.id.chatTextView);
        temperatureTextView = findViewById(R.id.textViewTemperature);
        humidityTextView = findViewById(R.id.textViewHumidity);
        ledStatusTextView = findViewById(R.id.textViewLEDStatus);
        ledControlButton = findViewById(R.id.buttonLedControl);

        // Initialize MQTT helper
        helper = new MQTTHelper(getApplicationContext(), "Android", "topicstest");
        helper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                // Connection Completed
                helper.subscribeToTopic("temperature");
                helper.subscribeToTopic("humidity");
                helper.subscribeToTopic("led_status");

                // Send message to all subscribed in the chat
                publishMessage(helper, "*" + "Android entered the room.", 0, "topicstest", false);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                // Handle connection lost
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                String incomingMessage = mqttMessage.toString();
                // Update UI on the main thread
                runOnUiThread(() -> chat.setText(incomingMessage + " In " + chat.getText()));
                Log.w("D", incomingMessage);

                // Handle different topics here
                if (topic.equals("temperature")) {
                    handleTemperatureData(incomingMessage);
                } else if (topic.equals("humidity")) {
                    handleHumidityData(incomingMessage);
                } else if (topic.equals("led_status")) {
                    handleLEDStatus(incomingMessage);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                // Handle delivery complete
            }
        });
        helper.connect();

        // Set up LED control button click listener
        ledControlButton.setOnClickListener(view -> onLedControlButtonClick(view));
    }

    private void handleTemperatureData(String temperatureMessage) {
        double temperatureValue = Double.parseDouble(temperatureMessage);
        temperatureTextView.setText("Temperature: " + temperatureValue + "°C");

        // Example: Check if the temperature exceeds a threshold
        if (temperatureValue > 30.0) {
            showTemperatureAlert();
        }
    }

    private void handleHumidityData(String humidityMessage) {
        double humidityValue = Double.parseDouble(humidityMessage);
        humidityTextView.setText("Humidity: " + humidityValue + "%");

        // Example: Check if humidity is too high
        if (humidityValue > 80.0) {
            showHumidityAlert();
        }
    }

    private void handleLEDStatus(String ledStatusMessage) {
        ledStatusTextView.setText("LED Status: " + ledStatusMessage);
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

    private void publishMessage(MQTTHelper helper, String message, int qos, String topic, boolean retained) {
        helper.publishMessage(message, topic);
    }

    private void onLedControlButtonClick(View view) {
        // Implement logic to control the LED
        String ledControlMessage = "TOGGLE"; // or "ON" or "OFF" depending on your preference
        publishMessage(helper, ledControlMessage, 0, "led_control", false);
    }
}
