package com.example.diabetespredictorapp;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private EditText etGlucose, etBmi;
    private Button btnPredict, btnClear;
    private TextView tvResult;
    private LinearLayout cardResult;

    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etGlucose = findViewById(R.id.etGlucose);
        etBmi = findViewById(R.id.etBmi);
        btnPredict = findViewById(R.id.btnPredict);
        btnClear = findViewById(R.id.btnClear);
        tvResult = findViewById(R.id.tvResult);
        cardResult = findViewById(R.id.cardResult);

        try {
            tflite = new Interpreter(loadModelFile("diabetes_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading AI model!", Toast.LENGTH_LONG).show();
        }

        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runPrediction();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etGlucose.setText("");
                etBmi.setText("");
                tvResult.setText("Enter Glucose & BMI above and tap Predict");
                tvResult.setTextColor(Color.parseColor("#1F2937"));
                cardResult.setBackgroundResource(R.drawable.bg_card_white);
                etGlucose.requestFocus();
            }
        });
    }

    private MappedByteBuffer loadModelFile(String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void runPrediction() {
        String glucoseStr = etGlucose.getText().toString().trim();
        String bmiStr = etBmi.getText().toString().trim();

        if (glucoseStr.isEmpty() || bmiStr.isEmpty()) {
            Toast.makeText(this, "Please enter both Glucose and BMI values", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tflite == null) {
            Toast.makeText(this, "Model not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float glucose = Float.parseFloat(glucoseStr);
            float bmi = Float.parseFloat(bmiStr);

            float[][] input = new float[1][2];
            input[0][0] = glucose / 200.0f;
            input[0][1] = bmi / 50.0f;

            float[][] output = new float[1][1];
            tflite.run(input, output);

            float probability = output[0][0];
            float percentage = probability * 100.0f;

            if (probability >= 0.5f) {
                tvResult.setText(String.format("HIGH RISK OF DIABETES\nConfidence: %.1f%%", percentage));
                tvResult.setTextColor(Color.parseColor("#B91C1C"));
                cardResult.setBackgroundColor(Color.parseColor("#FEE2E2"));
            } else {
                tvResult.setText(String.format("LOW RISK OF DIABETES\nRisk Probability: %.1f%%", percentage));
                tvResult.setTextColor(Color.parseColor("#15803D"));
                cardResult.setBackgroundColor(Color.parseColor("#DCFCE7"));
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid numeric input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}