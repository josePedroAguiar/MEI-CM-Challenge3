package com.example.cm_challenge3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText topicEditText;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize EditText fields and SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        Button a = findViewById(R.id.button2);

        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Chat.class);

                //String name_temp = nameEditText.getText().toString();
                //String topic_temp = topicEditText.getText().toString();

                // Pass information to new Activity

                // intent.putExtra("name", name_temp);
                // intent.putExtra("topic", topic_temp);

                // Store new preferences
                editor.putString("name", "aaa");
                editor.putString("topic", "aaa");
                editor.apply(); // Use apply() instead of commit() for efficiency

                // Start YourNextActivity
                startActivity(intent);
            }
        });
    }
}
