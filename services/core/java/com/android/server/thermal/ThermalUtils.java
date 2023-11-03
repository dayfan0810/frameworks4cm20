package com.android.server.thermal;

import android.util.Log;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ThermalUtils {


    private static final String TAG = "ThermalUtils";
    public static final String THERMAL_CONFIG_PATH = "/system/etc/thermalconf.json";
    public static final String BOARD_TEMP_BATH = "/sys/class/thermal/thermal_zone84/temp";

    /*
    /sys/class/thermal/cooling_device13/type:thermal-devfreq-0
    /sys/class/thermal/cooling_device15/type:battery
    /sys/class/thermal/cooling_device16/type:panel0-backlight
    /sys/class/thermal/cooling_device18/type:cdsp
    /sys/class/thermal/cooling_device3/type:thermal-cpufreq-0
    /sys/class/thermal/cooling_device4/type:thermal-cpufreq-6
    */
    public static final String CDEV_LCD = "/sys/class/thermal/cooling_device16/cur_state";
    public static final String CDEV_BATTERY = "/sys/class/thermal/cooling_device15/cur_state";
    public static final String CDEV_CPU_LITTLE = "/sys/class/thermal/cooling_device3/cur_state";
    public static final String CDEV_CPU_BIG = "/sys/class/thermal/cooling_device4/cur_state";
    public static final String MAX_FREQ_CPU_LITTLE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String MAX_FREQ_CPU_BIG = "/sys/devices/system/cpu/cpu6/cpufreq/scaling_max_freq";
    public static final String MAX_BATTERY_CONTROL = "/sys/class/power_supply/battery/charge_control_limit";
    public static final String MAX_BATTERY_CDEV_LEVEL = "/sys/class/thermal/cooling_device15/cur_state";

    public static String readJsonFile() {
        String json = null;
        File file = new File(THERMAL_CONFIG_PATH);
        if (file.exists()) {
            Log.i(TAG, "vendor/etc/thermalconf.json is exist");
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);

                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                json = new String(buffer, StandardCharsets.UTF_8);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "vendor/etc/thermalconf.json is not exist");
        }
        return json;
    }

    // public static void writeCoolingDevice(String path, int value) {
    //     File file = new File(path);
    //     FileOutputStream fos = null;
    //     try {
    //         fos = new FileOutputStream(file);
    //         fos.write(value);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } finally {
    //         if (fos != null) {
    //             try {
    //                 fos.close();
    //             } catch (IOException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     }
    // }


    public static void writeToFile(String path, int num) {
        String value = Integer.toString(num);
        Slog.i(TAG, "write node is: " + path + " , write value is " + value);
        BufferedWriter bufferedWriter;
        try {
            bufferedWriter = new BufferedWriter((new FileWriter(path)));
            bufferedWriter.write(value);
            bufferedWriter.flush();
        } catch (IOException e) {
            Log.i("deng", "writeToFile fail");
            e.printStackTrace();
        }
    }

    public static String readFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream fin = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                String config = reader.readLine();
                fin.close();
                if (config == null)
                    return "  ";
                else
                    return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "  ";
    }
}

