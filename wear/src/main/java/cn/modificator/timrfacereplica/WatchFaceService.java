
package cn.modificator.timrfacereplica;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.mobvoi.android.common.ConnectionResult;
import com.mobvoi.android.common.api.MobvoiApiClient;
import com.mobvoi.android.common.api.ResultCallback;
import com.mobvoi.android.wearable.DataApi;
import com.mobvoi.android.wearable.DataEvent;
import com.mobvoi.android.wearable.DataEventBuffer;
import com.mobvoi.android.wearable.DataItem;
import com.mobvoi.android.wearable.DataItemBuffer;
import com.mobvoi.android.wearable.DataMap;
import com.mobvoi.android.wearable.DataMapItem;
import com.mobvoi.android.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public class WatchFaceService extends CanvasWatchFaceService {


    static Paint mBackgroundPaint;
    static Paint mTilePaint;
    static Paint mScalePaint;
    static Paint mHourPaint;
    static Paint mMinutePaint;
    static Paint mDatePaint;
    static Paint mArrowPaint;
    static Paint mTimePaint;
    static Paint mBatteryPaint;
    static Paint mShadowPaint;


    public static long INTERACTIVE_UPDATE_RATE_MS = 50;
    public static int AMBIENT_BACKGROUND;
    public static int AMBIENT_TEXT;
    public static boolean BATTERY_LEVEL;
    public static int KEY_BACKGROUND_COLOR;
    public static int KEY_MAIN_COLOR;
    public static int KEY_TEXT_COLOR;
    public static boolean SMOOTH_SECONDS;
    public static boolean ZERO_DIGIT;

    static boolean shouldRecieve = true;

    String batteryLevel = "";


    public static void setInteractiveBackgroundColor(int color) {
        KEY_BACKGROUND_COLOR = color;
        mTilePaint.setColor(color);
        mMinutePaint.setColor(color);
    }

    public static void setInteractiveMainColor(int color) {
        KEY_MAIN_COLOR = color;
        mBackgroundPaint.setColor(color);
        mArrowPaint.setColor(color);
    }

    public static void setInteractiveTextColor(int color) {
        KEY_TEXT_COLOR = color;
        mHourPaint.setColor(color);
        mDatePaint.setColor(color);
        mTimePaint.setColor(color);
        mBatteryPaint.setColor(color);
    }

    public static void updateUi(String color) {
        shouldRecieve = false;
        setInteractiveBackgroundColor(Color.parseColor(color));
        if (KEY_MAIN_COLOR != Color.parseColor("#FAFAFA")) {
            setInteractiveTextColor(Color.parseColor("#FAFAFA"));
        } else {
            setInteractiveTextColor(Color.parseColor("#424242"));
        }
    }


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends CanvasWatchFaceService.Engine implements MobvoiApiClient.ConnectionCallbacks, MobvoiApiClient.OnConnectionFailedListener {

        static final int MSG_UPDATE_TIME = 0;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                mTime.clear(intent.getStringExtra("time-zone"));
//                mTime.setToNow();
                mCalendar.clear();
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormat();
                invalidate();
            }
        };
        private final Typeface ROBOTO_LIGHT =
                Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
        private final Typeface ROBOTO_THIN =
                Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");

        private final String TAG = "WatchFaceService";

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        private ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                if (shouldRecieve) {
                    Iterator<DataItem> iterator = dataItems.iterator();
                    while (iterator.hasNext()) {
                        processConfigurationFor(iterator.next());
                    }
                }
                dataItems.release();
                shouldRecieve = true;
            }
        };

        private BroadcastReceiver updateBattery = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                batteryLevel = String.valueOf(level) + "%";
            }
        };

        Bitmap scale;
        Bitmap indicator;
        Bitmap shadow;
        float seconds;
        boolean mRegisteredTimeZoneReceiver = false;
        boolean is24Hour = false;
        boolean ambientMode = false;
        SimpleDateFormat amPmFormat;
        SimpleDateFormat dateFormat;
        SimpleDateFormat hourFormat;
        DateFormat df;
        Calendar cal;
        MobvoiApiClient mobvoiApiClient;
        Context context = getApplicationContext();
        Calendar mCalendar;
        private Resources resources;
        boolean battery;
        float width;
        float height;

        DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEvents) {
                Iterator<DataEvent> iterator = dataEvents.iterator();
                while (iterator.hasNext()) {
                    DataEvent dataEvent = iterator.next();
                    if (dataEvent.getType() == 1) {
                        processConfigurationFor(dataEvent.getDataItem());
                    }
                }
                dataEvents.release();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            AMBIENT_BACKGROUND = Color.parseColor("#000000");
            AMBIENT_TEXT = Color.parseColor("#FFFFFF");
            KEY_BACKGROUND_COLOR = Color.parseColor("#FF9800");
            KEY_MAIN_COLOR = Color.parseColor("#FAFAFA");
            KEY_TEXT_COLOR = Color.parseColor("#424242");
            SMOOTH_SECONDS = true;
            BATTERY_LEVEL = true;
            ZERO_DIGIT = true;

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)
                    .setStatusBarGravity(8388661)
                    .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .build());

            mobvoiApiClient = new MobvoiApiClient.Builder(WatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            resources = WatchFaceService.this.getResources();
            scale = BitmapFactory.decodeResource(resources, R.drawable.scale);
//            scale = scale.createScaledBitmap(scale, 600, 50, true);
            scale = scale.createScaledBitmap(scale, 2000, 55, true);

            shadow = BitmapFactory.decodeResource(resources, R.drawable.indicator_shadow);
            shadow = Bitmap.createScaledBitmap(shadow, 50, 25, true);

            indicator = BitmapFactory.decodeResource(resources, R.drawable.indicator);
            indicator = Bitmap.createScaledBitmap(indicator, 50, 25, true);

            initFormat();
            createPaints();
            registerReceiver(this.updateBattery,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            mCalendar = new GregorianCalendar(TimeZone.getDefault());
//            mTime = new Time();
        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            ambientMode = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            is24Hour = DateFormat.is24HourFormat(context);
            cal.setTimeInMillis(System.currentTimeMillis());
            seconds = getSeconds();
//            cal = Calendar.getInstance();
            width = bounds.exactCenterX();
            height = bounds.exactCenterY();
//            width = bounds.width() / 2;
//            height = bounds.height() / 2;

            canvas.drawRect(0,
                    height / 11.0f + height + height / 5f,
                    width * 2,
                    height * 2,
                    mTilePaint);

            if (!ambientMode) {
                canvas.drawBitmap(shadow,
                        width - 25,
                        height + height / 5f + height / 14f + 4f,
                        mShadowPaint);
            }
            canvas.drawRect(0, 0, width * 2, height + height / 5 + height / 11, mBackgroundPaint);

            if (!ambientMode) {
//                canvas.drawRect(0, height + height / 5 + height / 11, width * 2, height * 2, mTilePaint);

                canvas.drawBitmap(this.scale, this.seconds + this.width - 676.6F, this.height + this.height / 4.0F + this.height / 8.0F, mScalePaint);
                canvas.drawBitmap(this.indicator, this.width - 25.0F, this.height + this.height / 5.0F + this.height / 14.0F, mArrowPaint);

//                canvas.drawBitmap(scale, seconds + (width / 2 + width / 2 - width / 16) * -3, height + height / 4 + height / 10, mScalePaint);
//                canvas.drawBitmap(scale, seconds + (width / 2 + width / 2 - width / 16 + width / 150) * 5, height + height / 4 + height / 10, mScalePaint);
//                canvas.drawBitmap(scale, seconds + width / 2 + width / 2 - width / 20, height + height / 4 + height / 10, mScalePaint);

//                canvas.rotate(45, width, height);
//                canvas.drawRect(width + 15, height + 15, width + 45, height + height / 5 + height / 11, mArrowPaint);
//                canvas.rotate(-45, width, height);
//                canvas.drawRect(width - 30, height + 15, width + 30, height + height / 5 + height / 11, mBorderPaint);
            }

//            canvas.drawText(getHours(), width / 2 - width / 3, height + height / 15, mHourPaint);
//            canvas.drawText(getMinutes(), width / 2 + width / 2, height + height / 15, mMinutePaint);
//            canvas.drawText(getDate(), width - mDatePaint.getStrokeWidth() / 2, height / 3 + height / 15, mDatePaint);
//            canvas.drawText(getAmPm(), width * 2 - width / 2, height + height / 5, mTimePaint);

            canvas.drawText(getHours(), width - (mHourPaint.measureText(getHours()) + mHourPaint.measureText(getHours()) / 20.0F), height + height / 15.0F, mHourPaint);
            canvas.drawText(getMinutes(), width + mMinutePaint.measureText(getMinutes()) / 20.0F, height + height / 15.0F, mMinutePaint);
            canvas.drawText(getDate(), width - mDatePaint.getStrokeWidth() / 2.0F, height / 3.0F + height / 25.0F, mDatePaint);
            canvas.drawText(getAmPm(), width * 2.0F - width / 2.0F, height + height / 4.0F, mTimePaint);

            if (battery) {
                canvas.drawText(batteryLevel, width / 2 - width / 3, height + height / 4, mBatteryPaint);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.e("----", "init");
            ambientMode = inAmbientMode;

            adjustPaintColorToCurrentMode(mMinutePaint, KEY_BACKGROUND_COLOR, AMBIENT_TEXT);
            adjustPaintColorToCurrentMode(mHourPaint, KEY_TEXT_COLOR, AMBIENT_TEXT);
            adjustPaintColorToCurrentMode(mDatePaint, KEY_TEXT_COLOR, AMBIENT_TEXT);
            adjustPaintColorToCurrentMode(mTimePaint, KEY_TEXT_COLOR, AMBIENT_TEXT);
            adjustPaintColorToCurrentMode(mBackgroundPaint, KEY_MAIN_COLOR, AMBIENT_BACKGROUND);
            adjustPaintColorToCurrentMode(mBatteryPaint, KEY_TEXT_COLOR, AMBIENT_BACKGROUND);
            adjustPaintColorToCurrentMode(mTilePaint, KEY_BACKGROUND_COLOR, AMBIENT_BACKGROUND);


//            mHourPaint.setAntiAlias(!ambientMode);
//            mMinutePaint.setAntiAlias(!ambientMode);

            mHourPaint.setTypeface(ambientMode ? ROBOTO_THIN : ROBOTO_LIGHT);
            mMinutePaint.setTypeface(ambientMode ? ROBOTO_THIN : ROBOTO_LIGHT);

//            mTilePaint.setAntiAlias(!ambientMode);
//            mDatePaint.setAntiAlias(!ambientMode);
//            mTimePaint.setAntiAlias(!ambientMode);
//            mBatteryPaint.setAntiAlias(!ambientMode);
//            mBackgroundPaint.setAntiAlias(!ambientMode);
//            mArrowPaint.setAntiAlias(!ambientMode);
//            mBorderPaint.setAntiAlias(!ambientMode);

            invalidate();
            updateTimer();
        }

        private void initFormat() {
            dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, dMMMM"));
            amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
            hourFormat = new SimpleDateFormat();
            cal = Calendar.getInstance();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                                                   int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            Resources resources = WatchFaceService.this.getResources();
            float infoTextSize = resources.getDimension(R.dimen.info_size);
            float timeTextSize = resources.getDimension(R.dimen.text_size);

            Log.e("----", "setTextSize");
            mHourPaint.setTextSize(timeTextSize);
            mMinutePaint.setTextSize(timeTextSize);
            mDatePaint.setTextSize(infoTextSize);
            mTimePaint.setTextSize(infoTextSize);
            mBatteryPaint.setTextSize(infoTextSize);
        }
        private float getSeconds() {
//            mTime.set(System.currentTimeMillis());
            mCalendar.setTimeInMillis(System.currentTimeMillis());
//            return (mTime.second + (System.currentTimeMillis() % 1000) / 1000f) * (-10);
//            float f = this.mCalendar.get(13) * -11.1F;;
//            f = (this.mCalendar.get(13) + this.mCalendar.get(14) / 1000.0F) * -11.1F)


            return SMOOTH_SECONDS ? (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f) * -11.1f : mCalendar.get(Calendar.SECOND) * -11.1f;
        }

        private void initFormats() {
            dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, dMMMM"));
            amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
            hourFormat = new SimpleDateFormat();
            cal = Calendar.getInstance();
        }

        private void processConfigurationFor(DataItem item) {
            if ("/watch_face_config".equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (dataMap.containsKey("SMOOTH_SECONDS")) {
                    SMOOTH_SECONDS = dataMap.getBoolean("SMOOTH_SECONDS");
                }
                if (dataMap.containsKey("BACKGROUND_COLOR")) {
                    checkColors(dataMap.getString("BACKGROUND_COLOR"));
                }
                if (dataMap.containsKey("COLOR")) {
                    checkColors(dataMap.getString("COLOR"));
                }
                if (dataMap.containsKey("COLOR_MANUAL")) {
                    WatchFaceService.KEY_BACKGROUND_COLOR = dataMap.getInt("COLOR_MANUAL");
                }
                if (dataMap.containsKey("BATTERY_INDICATOR")) {
                    WatchFaceService.BATTERY_LEVEL = dataMap.getBoolean("BATTERY_INDICATOR", true);
                }
                if (dataMap.containsKey("ZERO_DIGIT")) {
                    WatchFaceService.ZERO_DIGIT = dataMap.getBoolean("ZERO_DIGIT", true);
                }
                updateUi(KEY_BACKGROUND_COLOR, KEY_MAIN_COLOR, SMOOTH_SECONDS, BATTERY_LEVEL);
            }
        }

        private void checkColors(String color) {
            if (color.equals("#FAFAFA") || color.equals("#424242") || color.equals("#000000")) {
                KEY_MAIN_COLOR = Color.parseColor(color);

                switch (color) {
                    case "#FAFAFA":
                        KEY_TEXT_COLOR = Color.parseColor("#424242");
                        indicator = BitmapFactory.decodeResource(resources, R.drawable.indicator);
                        break;
                    case "#424242":
                        KEY_TEXT_COLOR = Color.parseColor("#FAFAFA");
                        indicator = BitmapFactory.decodeResource(resources, R.drawable.indicator_grey);
                        break;
                    case "#000000":
                        KEY_TEXT_COLOR = Color.parseColor("#FAFAFA");
                        indicator = BitmapFactory.decodeResource(resources, R.drawable.indicator_black);
                        break;
                }
                indicator = Bitmap.createScaledBitmap(indicator, 50, 25, true);
            } else {
                KEY_BACKGROUND_COLOR = Color.parseColor(color);
            }
        }

        private String getDate() {
//            if (is24Hour) {
//                amPmFormat = new SimpleDateFormat("EEEE, d. MMMM");
//            } else {
//                amPmFormat = new SimpleDateFormat("EEEE, d MMMM");
//            }
//            return amPmFormat.format(cal.getTime());
            return dateFormat.format(cal.getTime());
        }

        private String getMinutes() {
//            return formatTwoDigits(mTime.minute,true);
            return formatTwoDigits(mCalendar.get(Calendar.MINUTE), true);
        }

        private String getAmPm() {
            if (!is24Hour) {
//                amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
                return amPmFormat.format(cal.getTime());
            } else {
                return "";
            }
        }

        private String formatTwoDigits(int number, boolean shouldFormat) {
            if (ZERO_DIGIT || shouldFormat) {
                return String.format(Locale.getDefault(), "%02d", number);
            }
            return String.valueOf(number);
        }

        private String getHours() {
            if (is24Hour) {
//                amPmFormat = new SimpleDateFormat("H");
                hourFormat.applyLocalizedPattern("H");
                return formatTwoDigits(Integer.valueOf(hourFormat.format(cal.getTime())), false);
            } else {
//                amPmFormat = new SimpleDateFormat("h");
                hourFormat.applyLocalizedPattern("h");
                return formatTwoDigits(Integer.valueOf(hourFormat.format(cal.getTime())), false);
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            releaseGoogleApiClient();
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeReceiver();
                mobvoiApiClient.connect();
                mCalendar.clear();
                mCalendar.setTimeZone(TimeZone.getDefault());
//                mTime.clear(TimeZone.getDefault().getID());
//                mTime.setToNow();
            } else {
                releaseGoogleApiClient();
                unregisterReceiver();
            }
            updateTimer();
        }

        private void registerTimeReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void releaseGoogleApiClient() {
//            Wearable.DataApi.removeListener()
            if (mobvoiApiClient != null && mobvoiApiClient.isConnected()) {
                Wearable.DataApi.removeListener(mobvoiApiClient, onDataChangedListener);
                mobvoiApiClient.disconnect();
            }
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateUi(int color, int color2, boolean key, boolean battery) {
            this.battery = battery;
            if (key) {
//                INTERACTIVE_UPDATE_RATE_MS = 100;
                INTERACTIVE_UPDATE_RATE_MS = 50;
            } else {
                INTERACTIVE_UPDATE_RATE_MS = 1000;
            }
            if (!ambientMode) {
                setInteractiveBackgroundColor(color);
                setInteractiveMainColor(color2);
                if (color2 != Color.parseColor("#FAFAFA")) {
                    setInteractiveTextColor(Color.parseColor("#FAFAFA"));
                } else {
                    setInteractiveTextColor(Color.parseColor("#424242"));
                }
            }
            invalidate();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void createPaints() {
            mBackgroundPaint = new Paint();
            mTilePaint = new Paint();
            mScalePaint = new Paint();
            mArrowPaint = new Paint();
            mShadowPaint = new Paint();

//            mScalePaint.setAntiAlias(false);
            mScalePaint.setAntiAlias(true);

            mHourPaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
            mMinutePaint = createTextPaint(KEY_BACKGROUND_COLOR, ROBOTO_LIGHT);
            mDatePaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
            mTimePaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
            mBatteryPaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
            mDatePaint.setTextAlign(Paint.Align.CENTER);
            mBackgroundPaint.setColor(KEY_MAIN_COLOR);
            mArrowPaint.setColor(KEY_MAIN_COLOR);
            mTilePaint.setColor(KEY_BACKGROUND_COLOR);

            mBackgroundPaint.setShadowLayer(8.0f, 0.0f, 4.0f, resources.getColor(R.color.shadow));
//            mArrowPaint.setShadowLayer(8.0f, 4.0f, 4.0f, resources.getColor(R.color.shadow));

        }

        public void onConnected(Bundle bundle) {
            Log.d("WatchFaceService", "connected Mobvoi Api");
            Wearable.DataApi.addListener(mobvoiApiClient, onDataChangedListener);
            Wearable.DataApi.getDataItems(mobvoiApiClient, Uri.EMPTY).setResultCallback(onConnectedResultCallback);
        }

        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e("WatchFaceService", "connectionFailed GoogleAPI: " + connectionResult.toString());
        }

        public void onConnectionSuspended(int i) {
            Log.e("WatchFaceService", "suspended GoogleAPI");
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}