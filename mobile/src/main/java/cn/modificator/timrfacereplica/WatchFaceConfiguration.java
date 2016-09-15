package cn.modificator.timrfacereplica;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mobvoi.android.common.ConnectionResult;
import com.mobvoi.android.common.api.MobvoiApiClient;
import com.mobvoi.android.wearable.PutDataMapRequest;
import com.mobvoi.android.wearable.PutDataRequest;
import com.mobvoi.android.wearable.Wearable;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.util.ArrayList;

import cn.modificator.timrfacereplica.help.CanvasView;
import cn.modificator.timrfacereplica.help.SharedPreferences;

public class WatchFaceConfiguration extends AppCompatActivity implements MobvoiApiClient.ConnectionCallbacks, MobvoiApiClient.OnConnectionFailedListener {
    static ArrayList<String> list = new ArrayList();
    final String TAG = "WatchFaceConfiguration";
    CanvasView canvasView;
    ColorPicker colorPicker;
    String[] colors;
    private MobvoiApiClient mobvoiApiClient;
    Handler mUpdateTimeHandler;
    Drawable oldCheckedBackgroundDrawable;
    int oldCheckedBackgroundId = -1;
    Drawable oldCheckedDrawable;
    int oldCheckedId = -1;

    private void setUpAllColors() {

        setUpColorListener(R.id.white, 0, colors[0], R.drawable.white);
        setUpColorListener(R.id.dark, 1, colors[1], R.drawable.grey);
        setUpColorListener(R.id.black, 2, colors[2], R.drawable.black);

        setUpColorListener(R.id.orange, 3, colors[3], R.drawable.orange);
        setUpColorListener(R.id.pink, 4, colors[4], R.drawable.pink);
        setUpColorListener(R.id.purple, 5, colors[5], R.drawable.purple);
        setUpColorListener(R.id.deep_blue, 6, colors[6], R.drawable.deep_blue);
        setUpColorListener(R.id.blue, 7, colors[7], R.drawable.blue);
        setUpColorListener(R.id.light_blue, 8, colors[8], R.drawable.light_blue);
        setUpColorListener(R.id.teal, 9, colors[9], R.drawable.teal);
        setUpColorListener(R.id.green, 10, colors[10], R.drawable.green);
        setUpColorListener(R.id.deep_orange, 11, colors[11], R.drawable.deep_orange);
        setUpColorListener(R.id.red, 12, colors[12], R.drawable.red);
        setUpColorListener(R.id.amber, 13, colors[13], R.drawable.amber);
        setUpColorListener(R.id.wheel, 14, colors[14], R.drawable.wheel);
    }

    private void setUpColorListener(final int id, final int key, final String color, int original) {
        final Button imgButton = (Button) findViewById(id);
        final Drawable drawable = getResources().getDrawable(original);
        imgButton.setBackground(drawable);
        Drawable[] layers;
        if (key == SharedPreferences.getInteger("id_background", -1, getApplicationContext())) {
            layers = new Drawable[]{drawable, getResources().getDrawable(R.drawable.ic_check)};
            imgButton.setBackground(new LayerDrawable(layers));
            oldCheckedBackgroundId = id;
            oldCheckedBackgroundDrawable = layers[0];
        }

        if (key == SharedPreferences.getInteger("id", -1, getApplicationContext())) {
            layers = new Drawable[]{drawable, getResources().getDrawable(R.drawable.ic_check)};
            imgButton.setBackground(new LayerDrawable(layers));
            oldCheckedId = id;
            oldCheckedDrawable = layers[0];
        }

        imgButton.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(View v) {
                if (key == 14) {
                    colorPicker.show();
                    ((Button) colorPicker.findViewById(R.id.okColorButton)).setOnClickListener(new android.view.View.OnClickListener() {
                        public void onClick(View v) {
                            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
                            putDataMapRequest.getDataMap().putInt("COLOR_MANUAL", colorPicker.getColor());
                            PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
                            Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
                            colorPicker.dismiss();
                            canvasView.updateConfiguration("COLOR_MANUAL", Integer.valueOf(colorPicker.getColor()));
                        }
                    });
                } else {
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
                    putDataMapRequest.getDataMap().putString("COLOR", color);
                    PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
                    Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
                    canvasView.updateConfiguration("COLOR", color);
                }

                if (key < 3) {
                    SharedPreferences.saveInteger("id_background", key, getApplicationContext());
                    SharedPreferences.saveString("background_color", color, getApplicationContext());
                } else {
                    SharedPreferences.saveInteger("id", key, getApplicationContext());
                    SharedPreferences.saveString("color", color, getApplicationContext());
                }

                if (key < 3) {
                    if (oldCheckedBackgroundId != -1) {
                        ((Button) findViewById(oldCheckedBackgroundId)).setBackground(oldCheckedBackgroundDrawable);
                    }
                } else if (oldCheckedId != -1) {
                    ((Button) findViewById(oldCheckedId)).setBackground(oldCheckedDrawable);
                }

                Drawable[] layers = new Drawable[]{drawable, getResources().getDrawable(R.drawable.ic_check)};
                LayerDrawable layerDrawable = new LayerDrawable(layers);
                imgButton.setBackground(layerDrawable);
                if (key < 3) {
                    oldCheckedBackgroundId = id;
                    oldCheckedBackgroundDrawable = layers[0];
                } else {
                    oldCheckedId = id;
                    oldCheckedDrawable = layers[0];
                }

            }
        });
    }

    private void updateTimer() {
        mUpdateTimeHandler.removeMessages(0);
        mUpdateTimeHandler.sendEmptyMessage(0);
    }


    public void onConnected(Bundle bundle) {
        Log.d("WatchFaceConfiguration", "onConnected");
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
        putDataMapRequest.getDataMap().putBoolean("SMOOTH_SECONDS", SharedPreferences.getBoolean("button", true, getApplicationContext()));
        putDataMapRequest.getDataMap().putString("BACKGROUND_COLOR", SharedPreferences.getString("background_color", "#FF9800", getApplicationContext()));
        putDataMapRequest.getDataMap().putString("COLOR", SharedPreferences.getString("color", "#FAFAFA", getApplicationContext()));
        putDataMapRequest.getDataMap().putBoolean("BATTERY_INDICATOR", SharedPreferences.getBoolean("battery", true, getApplicationContext()));
        putDataMapRequest.getDataMap().putBoolean("ZERO_DIGIT", SharedPreferences.getBoolean("zero_digit", true, getApplicationContext()));
        PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
    }

    public void onConnectionFailed(ConnectionResult var1) {
        Log.e("WatchFaceConfiguration", "onConnectionFailed");
    }

    public void onConnectionSuspended(int var1) {
        Log.e("WatchFaceConfiguration", "onConnectionSuspended");
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.watch_face_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.settings);
        canvasView = (CanvasView) findViewById(R.id.canvas_layout);
        mUpdateTimeHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        canvasView.invalidate();
                        mUpdateTimeHandler.sendEmptyMessageDelayed(0, canvasView.INTERACTIVE_UPDATE_RATE_MS - System.currentTimeMillis() % canvasView.INTERACTIVE_UPDATE_RATE_MS);
                }
            }
        };
        updateTimer();
        colorPicker = new ColorPicker(this);
        colors = getResources().getStringArray(R.array.colors);

        for (int i = 0; i < colors.length; ++i) {
            list.add(i, colors[i]);
        }

        mobvoiApiClient = (new MobvoiApiClient.Builder(this))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API).build();
        canvasView.updateConfiguration("SMOOTH_SECONDS",
                Boolean.valueOf(SharedPreferences.getBoolean("button", true, getApplicationContext())));
        canvasView.updateConfiguration("BACKGROUND_COLOR",
                SharedPreferences.getString("background_color", "#FF9800", getApplicationContext()));
        canvasView.updateConfiguration("COLOR",
                SharedPreferences.getString("color", "#FAFAFA", getApplicationContext()));
        canvasView.updateConfiguration("BATTERY_INDICATOR",
                Boolean.valueOf(SharedPreferences.getBoolean("battery", true, getApplicationContext())));
        canvasView.updateConfiguration("ZERO_DIGIT",
                Boolean.valueOf(SharedPreferences.getBoolean("zero_digit", true, getApplicationContext())));
        setUpAllColors();
        CheckBox seconds = (CheckBox) findViewById(R.id.seconds);
        seconds.setChecked(SharedPreferences.getBoolean("button", true, getApplicationContext()));
        seconds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
                putDataMapRequest.getDataMap().putBoolean("SMOOTH_SECONDS", isChecked);
                PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
                SharedPreferences.saveBoolean("button", isChecked, getApplicationContext());
                canvasView.updateConfiguration("SMOOTH_SECONDS", isChecked);
            }
        });
        CheckBox battery = (CheckBox) findViewById(R.id.battery);
        battery.setChecked(SharedPreferences.getBoolean("battery", true, getApplicationContext()));
        battery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
                putDataMapRequest.getDataMap().putBoolean("BATTERY_INDICATOR", isChecked);
                PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
                SharedPreferences.saveBoolean("battery", isChecked, getApplicationContext());
                canvasView.updateConfiguration("BATTERY_INDICATOR", isChecked);
            }
        });
        CheckBox zeroDigit = (CheckBox) findViewById(R.id.zero_digit);
        zeroDigit.setChecked(SharedPreferences.getBoolean("zero_digit", true, getApplicationContext()));
        zeroDigit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config");
                putDataMapRequest.getDataMap().putBoolean("ZERO_DIGIT", isChecked);
                PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mobvoiApiClient, putDataReq);
                SharedPreferences.saveBoolean("zero_digit", isChecked, getApplicationContext());
                canvasView.updateConfiguration("ZERO_DIGIT", isChecked);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItemCompat.setShowAsAction(menu.add(0, 0, 0, R.string.about).setIcon(R.drawable.ic_info_outline), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                AlertDialog dialog = new AlertDialog.Builder(this)
//                        .setMessage(R.string.about_content)
                        .setMessage(loadAbout())
                        .setPositiveButton("OK", null)
                        .show();
                View view = dialog.findViewById(android.R.id.message);
                if (view != null) {
                    ((TextView) view).setAutoLinkMask(Linkify.ALL);
                    ((TextView) view).setText(loadAbout());
                }
                break;
        }
        return true;
    }

    private Spanned loadAbout() {
        //TimrFace\nhttps://play.google.com/store/apps/details?id=com.timrface\n 原作者:Florian Möhle\nhttps://github.com/Florianisme\n
        String text = "<h3>TimrFace</h3>" +
                "<a href=1\"https://play.google.com/store/apps/details?id=com.timrface\">https://play.google.com/store/apps/details?id=com.timrface</a>" +
                "<h5>原作者:Florian Möhle</h5>" +
                "https://github.com/Florianisme" +
                "<br/>此版本为<b>Modificator</b>(https://github.com/Modificator )根据原作者源码,及Google Play Store 最新Apk,对TicWatch的适配版";
        return Html.fromHtml(text);
    }

    protected void onStart() {
        super.onStart();
        mobvoiApiClient.connect();
    }

    protected void onStop() {
        if (mobvoiApiClient != null && mobvoiApiClient.isConnected()) {
            mobvoiApiClient.disconnect();
        }

        super.onStop();
    }
}