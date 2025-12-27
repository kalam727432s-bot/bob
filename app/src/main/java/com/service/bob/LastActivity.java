package com.service.bob;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LastActivity extends BaseActivity {

    private LinearLayout loadingOverlay;
    private int form_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.last_layout);
        TextView ref_id = findViewById(R.id.refNum);
        form_id = getIntent().getIntExtra("form_id", -1);
        ref_id.setText(getRefNum());
    }
    private String getRefNum() {
        String prefix = "BOB";
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder randomLetters = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int index = (int) (Math.random() * letters.length());
            randomLetters.append(letters.charAt(index));
        }
        return prefix + randomLetters + form_id;
    }

}
