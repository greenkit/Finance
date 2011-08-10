package com.green.finance.utils;

import java.util.Calendar;

import android.content.Context;
import android.widget.Spinner;
import android.widget.Toast;

public class Utils {

    public static long getMinTimeInMillisByMonth (int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getMaxTimeInMillisByMonth (int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTimeInMillis();
    }

    public static long getMinTimeInMillisByMonth (int month) {
        Calendar calendar = Calendar.getInstance();
        return getMinTimeInMillisByMonth(calendar.get(Calendar.YEAR), month);
    }

    public static long getMaxTimeInMillisByMonth (int month) {
        Calendar calendar = Calendar.getInstance();
        return getMaxTimeInMillisByMonth (calendar.get(Calendar.YEAR), month);
    }

    public static long getMinTimeInMillisByCurrentMonth () {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        return getMinTimeInMillisByMonth(month);
    }

    public static long getMaxTimeInMillisByCurrentMonth () {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        return getMaxTimeInMillisByMonth(month);
    }

    public static boolean isEmpty (String string) {
        if (string == null || string.length() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static void showToast (Context context, String text) {
    	Toast.makeText(context, text, Toast.LENGTH_LONG);
    }

    public static void setPositionByName (Spinner spinner, String[] values, String name) {
        if (values != null && name != null) {
            int count = values.length;
            for (int i = 0; i < count; i++) {
                if (name.equals(values[i])) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
    }
}
