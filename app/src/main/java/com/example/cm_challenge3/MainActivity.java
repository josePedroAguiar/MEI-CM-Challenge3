package com.example.cm_challenge3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // TextViews to display sensor data
    private TextView textViewTemperature;
    private TextView textViewHumidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);

        // Button to navigate to Chat activity
        Button btnOpenChat = findViewById(R.id.btnOpenChat);
        btnOpenChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Chat.class);
                startActivity(intent);
            }
        });

        // Button to control LED
        Button btnControlLED = findViewById(R.id.btnControlLED);
        btnControlLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement LED control logic here
                // You might want to send a message to the Arduino to control the LED
            }
        });
    }
}
