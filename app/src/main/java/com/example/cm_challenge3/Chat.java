package com.example.cm_challenge3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Chat extends AppCompatActivity {

    private MQTTHelper helper; // Assuming MQTTHelper is implemented
    private String name= "Android";
    private String topic="topicstest";
    private TextView chat; // Assuming this is the chat TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Replace with your chat layout

        // Initialize your TextView (chat)

        // Assuming 'name' and 'topic' are passed via Intent extras from the previous activity
       /* Intent intent = getIntent();
        if (intent != null) {
            name = intent.getStringExtra("name");

            topic = intent.getStringExtra("topic");
        }*/

        connectingAndStartingChat();
    }

    private void connectingAndStartingChat() {
        helper = new MQTTHelper(getApplicationContext(), "Android", topic);
        helper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                // Connection Completed
                helper.subscribeToTopic(topic);
                // Send message to all subscribed in the chat
                publishMessage(helper, "*" +   "Android entered the room.", 0, "topicstest", false);
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
                Log.w("D",incomingMessage);

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                // Handle delivery complete
            }
        });
        helper.connect();
    }

    // Method for publishing messages (assuming you have a method like this)
    private void publishMessage(MQTTHelper helper, String message, int qos, String topic, boolean retained) {
        // Implement MQTT message publishing logic
    }
}
