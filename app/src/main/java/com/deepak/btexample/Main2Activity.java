package com.deepak.btexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Main2Activity extends AppCompatActivity {

    private Button discoveryButton;
    private Button messageButton;

    private Piconet piconet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

            piconet = new Piconet(getApplicationContext());

            messageButton = (Button) findViewById(R.id.messageButton);
            messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    piconet.bluetoothBroadcastMessage("Hello World---*Gaby Bou Tayeh*");
                }
            });

            discoveryButton = (Button) findViewById(R.id.discoveryButton);
            discoveryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    piconet.startPiconet();
                }
            });

        }
}
