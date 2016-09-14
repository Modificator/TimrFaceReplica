package cn.modificator.timrfacereplica.help;

import android.app.Activity;
import android.content.Context;

/**
 * Created by Modificator
 * time: 16-9-14.上午10:43
 * des:create file and achieve model
 */

public class SharedPreferences {

    public static boolean getBoolean(String Name, boolean DefaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences("MyPreferences", 0).getBoolean(Name, DefaultValue);
    }

    public static int getInteger(String Name, int DefaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences("MyPreferences", 0).getInt(Name, DefaultValue);
    }

    public static String getString(String Name, String DefaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences("MyPreferences", 0).getString(Name, DefaultValue);
    }

    public static void saveBoolean(String Name, boolean Value, Context context) {
        android.content.SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences("MyPreferences", 0).edit();
        editor.putBoolean(Name, Value);
        editor.commit();
    }

    public static void saveInteger(String Name, int Value, Context context) {
        android.content.SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences("MyPreferences", 0).edit();
        editor.putInt(Name, Value);
        editor.commit();
    }

    public static void saveString(String Name, String Value, Context context) {
        android.content.SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences("MyPreferences", 0).edit();
        editor.putString(Name, Value);
        editor.commit();
    }
}