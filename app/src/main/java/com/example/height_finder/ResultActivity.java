package com.example.height_finder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    TextView resultView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        resultView = (TextView)findViewById(R.id.txt_Value);
        Intent intent = getIntent();
        String str = intent.getStringExtra("Height") + " m";
        resultView.setText(str);
    }
}