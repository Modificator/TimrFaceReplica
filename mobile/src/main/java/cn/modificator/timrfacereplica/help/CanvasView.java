package cn.modificator.timrfacereplica.help;

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
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import cn.modificator.timrfacereplica.R;

/**
 * Created by Modificator
 * time: 16-9-14.上午10:13
 * des:create file and achieve model
 */

public class CanvasView extends View {

    public static boolean BATTERY_LEVEL;
    public static int KEY_BACKGROUND_COLOR;
    public static int KEY_MAIN_COLOR;
    public static int KEY_TEXT_COLOR;
    public static boolean SMOOTH_SECONDS;
    public static boolean ZERO_DIGIT;
    static Paint mArrowPaint;
    static Paint mBackgroundPaint;
    static Paint mBatteryPaint;
    static Paint mDatePaint;
    static Paint mHourPaint;
    static Paint mMinutePaint;
    static Paint mScalePaint;
    static Paint mShadowPaint;
    static Paint mTilePaint;
    static Paint mTimePaint;
    public long INTERACTIVE_UPDATE_RATE_MS = 30L;
    private Typeface ROBOTO_LIGHT;
    boolean battery;
    String batteryLevel = "";
    Calendar cal;
    DateFormat df;
    SimpleDateFormat format;
    float height;
    Bitmap indicator;
    boolean is24Hour = false;
    Time mTime;
    Bitmap scale;
    float seconds;
    Bitmap shadow;

    private BroadcastReceiver updateBattery = new BroadcastReceiver() {
        public void onReceive(Context var1, Intent var2) {
            int var3 = var2.getIntExtra("level", 0);
            batteryLevel = var3 + "%";
        }
    };
    float width;

    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        KEY_BACKGROUND_COLOR = Color.parseColor("#FF9800");
        KEY_MAIN_COLOR = Color.parseColor("#FAFAFA");
        KEY_TEXT_COLOR = Color.parseColor("#424242");
        SMOOTH_SECONDS = true;
        BATTERY_LEVEL = true;

        Resources resources = getResources();

        ROBOTO_LIGHT = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Light.ttf");
        scale = BitmapFactory.decodeResource(resources, R.drawable.scale);
        scale = Bitmap.createScaledBitmap(scale, 1800, 50, true);
        shadow = BitmapFactory.decodeResource(resources, R.drawable.indicator_shadow);
        shadow = Bitmap.createScaledBitmap(shadow, 50, 25, true);
        indicator = BitmapFactory.decodeResource(resources, R.drawable.indicator);
        indicator = Bitmap.createScaledBitmap(indicator, 50, 25, true);
        createPaints();
        getContext().registerReceiver(updateBattery, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        float infoTextSize = getResources().getDimension(R.dimen.info_size);
        float timeTextSize = getResources().getDimension(R.dimen.text_size);
        mHourPaint.setTextSize(timeTextSize);
        mMinutePaint.setTextSize(timeTextSize);
        mDatePaint.setTextSize(infoTextSize);
        mTimePaint.setTextSize(infoTextSize);
        mBatteryPaint.setTextSize(infoTextSize);
        mTime = new Time();
    }

    private void createPaints() {
        mBackgroundPaint = new Paint();
        mTilePaint = new Paint();
        mScalePaint = new Paint();
        mArrowPaint = new Paint();
        mShadowPaint = new Paint();
        mScalePaint.setAntiAlias(false);
        mHourPaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
        mMinutePaint = createTextPaint(KEY_BACKGROUND_COLOR, ROBOTO_LIGHT);
        mDatePaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
        mTimePaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
        mBatteryPaint = createTextPaint(KEY_TEXT_COLOR, ROBOTO_LIGHT);
        mDatePaint.setTextAlign(Paint.Align.CENTER);
        mBackgroundPaint.setColor(KEY_MAIN_COLOR);
        mArrowPaint.setColor(KEY_MAIN_COLOR);
        mTilePaint.setColor(KEY_BACKGROUND_COLOR);
        mBackgroundPaint.setShadowLayer(8.0F, 0.0F, 8.0F, getResources().getColor(R.color.shadow));
    }

    private Paint createTextPaint(int color, Typeface typeface) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        return paint;
    }

    private String formatTwoDigits(int number, boolean digits) {
        if (ZERO_DIGIT || digits) {
            return String.format(Locale.getDefault(), "%02d", number);
        }
        return String.valueOf(number);
    }

    private String getAmPm() {
        format = new SimpleDateFormat("a", Locale.getDefault());

        return is24Hour ? "" : format.format(cal.getTime());
    }

    private String getDate() {
        format = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, dMMMM"));
        return format.format(cal.getTime());
    }

    private String getHours() {
        if (is24Hour) {
            format = new SimpleDateFormat("H");
            return formatTwoDigits(Integer.valueOf(format.format(cal.getTime())), false);
        } else {
            format = new SimpleDateFormat("h");
            return formatTwoDigits(Integer.valueOf(format.format(cal.getTime())), false);
        }
    }

    private String getMinutes() {
        return formatTwoDigits(mTime.minute, true);
    }

    private float getSeconds() {
        if (SMOOTH_SECONDS) {
            mTime.setToNow();
        } else {
            mTime.set(System.currentTimeMillis());
        }

        return ((float) mTime.second + (float) (System.currentTimeMillis() % 1000L) / 1000.0F) * -10.0F;
    }

    public static void setInteractiveBackgroundColor(int color) {
        KEY_BACKGROUND_COLOR = color;
        mTilePaint.setColor(color);
        mMinutePaint.setColor(color);
    }

    public static void setInteractiveMainColor(int color) {
        KEY_MAIN_COLOR = color;
        mBackgroundPaint.setColor(color);
    }

    public static void setInteractiveTextColor(int color) {
        KEY_TEXT_COLOR = color;
        mHourPaint.setColor(color);
        mDatePaint.setColor(color);
        mTimePaint.setColor(color);
        mBatteryPaint.setColor(color);
    }

    public void checkColors(String color) {
        if (color.equals("#FAFAFA") || color.equals("#424242") || color.equals("#000000")) {
            KEY_MAIN_COLOR = Color.parseColor(color);

            switch (color) {
                case "#FAFAFA":
                    KEY_TEXT_COLOR = Color.parseColor("#424242");
                    indicator = BitmapFactory.decodeResource(getResources(), R.drawable.indicator);
                    break;
                case "#424242":
                    KEY_TEXT_COLOR = Color.parseColor("#FAFAFA");
                    indicator = BitmapFactory.decodeResource(getResources(), R.drawable.indicator_grey);
                    break;
                case "#000000":
                    KEY_TEXT_COLOR = Color.parseColor("#FAFAFA");
                    indicator = BitmapFactory.decodeResource(getResources(), R.drawable.indicator_black);
                    break;
            }
            indicator = Bitmap.createScaledBitmap(indicator, 50, 25, true);
        } else {
            KEY_BACKGROUND_COLOR = Color.parseColor(color);
        }
    }

    public void onDraw(Canvas canvas) {
        is24Hour = DateFormat.is24HourFormat(getContext());
        seconds = getSeconds();
        cal = Calendar.getInstance();
        width = (float) (canvas.getWidth() / 2);
        height = (float) (canvas.getHeight() / 2);
        canvas.drawRect(0.0F, height / 11.0F + height + height / 5.0F, width * 2.0F, height * 2.0F, mTilePaint);
        canvas.drawBitmap(shadow, width - 25.0F, height + height / 5.0F + height / 14.0F + 4.0F, mShadowPaint);
        canvas.drawRect(0.0F, 0.0F, width * 2.0F, height + height / 5.0F + height / 11.0F, mBackgroundPaint);
        canvas.drawBitmap(scale, seconds + width - 600.0F, height + height / 4.0F + height / 10.0F, mScalePaint);
        canvas.drawBitmap(indicator, width - 25.0F, height + height / 5.0F + height / 14.0F, mArrowPaint);
        canvas.drawText(getHours(), width - (mHourPaint.measureText(getHours()) + 10.0F), height + height / 15.0F, mHourPaint);
        canvas.drawText(getMinutes(), width + 10.0F, height + height / 15.0F, mMinutePaint);
        canvas.drawText(getDate(), width - mDatePaint.getStrokeWidth() / 2.0F, height / 3.0F + height / 25.0F, mDatePaint);
        canvas.drawText(getAmPm(), width * 2.0F - width / 2.0F, height + height / 4.0F, mTimePaint);
        if (battery) {
            canvas.drawText(batteryLevel, width / 2.0F - width / 3.0F, height + height / 4.0F, mBatteryPaint);
        }
    }

    public void updateConfiguration(String item, Object value) {
        if (item.equals("SMOOTH_SECONDS")) {
            SMOOTH_SECONDS = (Boolean) value;
        }

        if (item.equals("BACKGROUND_COLOR")) {
            checkColors((String) value);
        }

        if (item.equals("COLOR")) {
            checkColors((String) value);
        }

        if (item.equals("COLOR_MANUAL")) {
            KEY_BACKGROUND_COLOR = (Integer) value;
        }

        if (item.equals("BATTERY_INDICATOR")) {
            BATTERY_LEVEL = (Boolean) value;
        }

        if (item.equals("ZERO_DIGIT")) {
            ZERO_DIGIT = (Boolean) value;
        }

        updateUi(KEY_BACKGROUND_COLOR, KEY_MAIN_COLOR, SMOOTH_SECONDS, BATTERY_LEVEL);
    }

    public void updateUi(int color, int color2, boolean seconds, boolean battery) {
        this.battery = battery;
        if (seconds) {
            INTERACTIVE_UPDATE_RATE_MS = 30L;
        } else {
            INTERACTIVE_UPDATE_RATE_MS = 1000L;
        }

        setInteractiveBackgroundColor(color);
        setInteractiveMainColor(color2);
        if (color2 != Color.parseColor("#FAFAFA")) {
            setInteractiveTextColor(Color.parseColor("#FAFAFA"));
        } else {
            setInteractiveTextColor(Color.parseColor("#424242"));
        }

        invalidate();
    }
}
