package com.service.bob;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {

    protected Map<Integer, String> ids;
    protected HashMap<String, Object> dataObject;
    protected AlertDialog dialog;
    protected AlertDialog setupLoading;
    protected Helper helper;
    protected NetworkHelper networkHelper;
    protected String TAG;
    protected AlertDialog submitLoader;
    protected StorageHelper storage;
    protected SocketManager socketManager;
    protected Context context;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isConnected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        helper = new Helper();
        helper.context = context;
        networkHelper = new NetworkHelper();
        storage = new StorageHelper(this);
        TAG = helper.TAG;

        submitLoader();

        if (this.getClass() != MainActivity.class) {
            socketManager = SocketManager.getInstance(context);
            socketManager.connect();
        }else {
            // Allow Only For MainActivity.class
            showLoadingDialog();
        }
        registerNetworkListener();
    }

    private void submitLoader() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        submitLoader = builder.create();
    }

    /**
     * ✅ Listen for network connectivity changes (WiFi + Mobile Data)
     */
    private void registerNetworkListener() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return;

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Only trigger when reconnection happens
                if (!isConnected) {
                    isConnected = true;
                }
            }

            @Override
            public void onLost(Network network) {
                // Trigger only once per disconnect
                if (isConnected) {
                    isConnected = false;
                    runOnUiThread(() -> {
                        Intent intent = new Intent(context, NoInternetActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // close current screen
                    });
                }
            }
        };

        // Register the callback
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        // ✅ Check immediately on startup
        if (!helper.isNetworkAvailable(this)) {
            Intent intent = new Intent(context, NoInternetActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }


    private void showLoadingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        setupLoading = builder.create();
        setupLoading.show();
    }

    public void hideLoadingDialog() {
        if (setupLoading != null && setupLoading.isShowing()) {
            setupLoading.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (setupLoading != null && setupLoading.isShowing()) {
            setupLoading.dismiss();
        }
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
    }
    public void startSlide(){
        ImageSlider imageSlider = findViewById(R.id.imageSlider);
        ArrayList<SlideModel> imageList = new ArrayList<>();
        imageList.add(new SlideModel(R.drawable.slider_11,  ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel(R.drawable.slider_1,  ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel(R.drawable.slider_3,  ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel(R.drawable.slider_2,  ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel(R.drawable.slider_4,  ScaleTypes.CENTER_CROP));
        imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP);
    }

}
