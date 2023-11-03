package com.android.server.thermal;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import android.util.Log;
import android.util.Slog;

import android.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import android.os.IThermalService;
import android.os.CoolingDevice;
import android.os.Temperature;
import android.os.RemoteException;
import android.os.ServiceManager;


public class CpuThermalService extends Service {
    private static final String TAG = "CpuThermalService";
    private IThermalService mThermalService;
    private static final int MIN_TEMP_THRESHOLD = 28000;  //下限
    private static final int MAX_TEMP_THRESHOLD_1 = 35000; //上限1
    private static final int MAX_TEMP_THRESHOLD_2 = 40000; //上限2
    private static final int CPU_LITTLE_LIMIT_1 = 1574400;  //降频
    private static final int CPU_LITTLE_LIMIT_2 = 1478400;  //降频
    private static final int CPU_BIG_LIMIT_1 = 1651200;  //降频
    private static final int CPU_BIG_LIMIT_2 = 1536000;  //降频

    private ArrayList<Integer> cpu_Little_Thresholds;
    private ArrayList<Integer> cpu_Little_ThresholdsClr;
    private ArrayList<Integer> cpu_Little_ActionInfo;

    private ArrayList<Integer> cpu_Big_Thresholds;
    private ArrayList<Integer> cpu_Big_ThresholdsClr;
    private ArrayList<Integer> cpu_Big_ActionInfo;


    //TODO
    private static final int CPU_LITTLE_DEFAULT_MAX = 1804800;  //簇1默认最大的频率
    //TODO
    private static final int CPU_BIG_DEFAULT_MAX = 2035200;  //簇2默认最大的频率

    private Handler mHandler;
    private Runnable mRunnable;
    private PowerManager mPowerManager;
    private int mLastCpu0Limit = CPU_LITTLE_DEFAULT_MAX;
    private int mLastCpu6Limit = CPU_BIG_DEFAULT_MAX;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Slog.i(TAG, "CpuThermalService--run");
        getCurrentCoolingDevices_forCPU();
        String json = ThermalUtils.readJsonFile();
        try {
            // 解析JSON数据
            JSONObject jsonObject = new JSONObject(json);
            for (String key : jsonObject.keySet()) {
                JSONObject monitorObject = jsonObject.getJSONObject(key);
                String sampling = monitorObject.getString("sampling");
                JSONArray thresholds = monitorObject.getJSONArray("thresholds");
                JSONArray thresholds_clr = monitorObject.getJSONArray("thresholds_clr");
                JSONArray action_info = monitorObject.getJSONArray("action_info");

                // 将数据写入SQLite数据库
                ContentValues values = new ContentValues();
//                values.put(ThermalDatabaseHelper.COLUMN_SAMPLING, sampling);
//                values.put(ThermalDatabaseHelper.COLUMN_THRESHOLDS, thresholds.toString());
//                values.put(ThermalDatabaseHelper.COLUMN_THRESHOLDS_CLR, thresholds_clr.toString());
//                values.put(ThermalDatabaseHelper.COLUMN_ACTIONS, actions.toString());
//                values.put(ThermalDatabaseHelper.COLUMN_ACTION_INFO, action_info.toString());
//
//                databaseHelper.insertData(key, values);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON", e);
        }

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                Slog.i(TAG, "CpuThermalService--polling");
                int temp = getTemperature(ThermalUtils.BOARD_TEMP_BATH); // 获取温度
                int cpu_Little__limit = calculate_Cpu0_Limit(temp); //计算CPU限制
                int cpu_Big__limit = calculate_Cpu6_Limit(temp); //计算CPU限制
                // if (cpu_Little__limit != mLastCpu0Limit || cpu_Big__limit != mLastCpu6Limit) { //只有当CPU限制发生变化时才会更新
                //     mLastCpu0Limit = cpu_Little__limit;
                //     mLastCpu6Limit = cpu_Big__limit;
                //     setCpuLimit(cpu_Little__limit, cpu_Big__limit); //设置CPU_BIG限制
                //     Slog.i(TAG, "CpuThermalService--onStartCommand--setCpuLimit");
                // }
                setCpuLimit(cpu_Little__limit, cpu_Big__limit); //设置CPU_BIG限制
                mHandler.postDelayed(this, 4000); //每5s执行一次
            }
        };
        mHandler.post(mRunnable);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int calculate_Cpu0_Limit(int temperature) {
        int cpu_Little__limit = 0;
        //28<x<35
        if (temperature >= MIN_TEMP_THRESHOLD && temperature < MAX_TEMP_THRESHOLD_1) {
            cpu_Little__limit = CPU_LITTLE_LIMIT_1; //CPU降频至70%
            //35<x<40
        } else if (temperature >= MAX_TEMP_THRESHOLD_1 && temperature < MAX_TEMP_THRESHOLD_2) {
            cpu_Little__limit = CPU_LITTLE_LIMIT_2; //CPU降频至70%
            //x>40
        } else if (temperature >= MAX_TEMP_THRESHOLD_2) {

        } else {
            cpu_Little__limit = CPU_LITTLE_DEFAULT_MAX; //无限制
        }
        Slog.i(TAG, "calculate_Cpu0_Limit = " + cpu_Little__limit);
        cpu_Little__limit = 1574400;
        return cpu_Little__limit;
    }

    private int calculate_Cpu6_Limit(int temperature) {
        int cpu_Big__limit = 0;
        if (temperature >= MIN_TEMP_THRESHOLD && temperature < MAX_TEMP_THRESHOLD_1) {
            cpu_Big__limit = CPU_BIG_LIMIT_1; //CPU降频至70%
        } else if (temperature >= MAX_TEMP_THRESHOLD_1 && temperature < MAX_TEMP_THRESHOLD_2) {
            cpu_Big__limit = CPU_BIG_LIMIT_2; //CPU降频至70%
        } else if (temperature >= MAX_TEMP_THRESHOLD_2) {

        } else {
            cpu_Big__limit = CPU_BIG_DEFAULT_MAX; //无限制
        }
        Slog.i(TAG, "calculate_Cpu6_Limit = " + cpu_Big__limit);
        cpu_Big__limit = 1651200;
        return cpu_Big__limit;
    }

    private void setCpuLimit(int cpu_Little__limit, int cpu_Big__limit) {
        Slog.i(TAG, "setCpuLimit ----------------------------------------------------------------->");
        ThermalUtils.writeToFile(ThermalUtils.MAX_FREQ_CPU_LITTLE, cpu_Little__limit);//pass
        ThermalUtils.writeToFile(ThermalUtils.MAX_FREQ_CPU_BIG, cpu_Big__limit);//pass
//        ThermalUtils.writeToFile(ThermalUtils.MAX_BATTERY_CDEV_LEVEL, 7);
        // ThermalUtils.writeToFile(ThermalUtils.CDEV_CPU_LITTLE, cpu_Little__limit);
        // ThermalUtils.writeToFile(ThermalUtils.CDEV_CPU_ByIG, cpu_Big__limit);
        ThermalUtils.writeToFile(ThermalUtils.CDEV_BATTERY, 4);
        ThermalUtils.writeToFile(ThermalUtils.CDEV_LCD, 3900);
    }


    private int getTemperature(String path) {
        int temperature = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            temperature = Integer.parseInt(line); //以m°C为单位
            reader.close();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        Slog.i(TAG, "getTemperature = " + temperature);
        return temperature;
    }


    public ArrayList<Integer> getCpu0Thresholds() {
        return cpu_Little_Thresholds;
    }

    public ArrayList<Integer> getCpu0ThresholdsClr() {
        return cpu_Little_ThresholdsClr;
    }

    public ArrayList<Integer> getCpu0ActionInfo() {
        return cpu_Little_ActionInfo;
    }


    public void getCurrentCoolingDevices_forCPU() {
        if (mThermalService == null) {
            mThermalService = IThermalService.Stub.asInterface(
                    ServiceManager.getService(Context.THERMAL_SERVICE));
            if (mThermalService != null) {
                try {
                    mThermalService.asBinder().linkToDeath(() -> {
                        mThermalService = null;
                    }, /* flags */ 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkToDeath with thermalService failed", e);
                    mThermalService = null;
                }
            }
        }


        try {
            CoolingDevice devices[] = mThermalService.getCurrentCoolingDevices();
            Temperature temperatures[] = mThermalService.getCurrentTemperatures();
            for (CoolingDevice device : devices) {
                Slog.i(TAG, "devices is " + device.toString());
            }
            for (Temperature temp : temperatures) {
                Slog.i(TAG, "temperatures is " + temp.toString());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "ThermalService get function failed");
        }
    }

    // private IThermalService getIThermalService() {
    //     if (mThermalService == null) {
    //         mThermalService = IThermalService.Stub.asInterface(
    //                 ServiceManager.getService(Context.THERMAL_SERVICE));
    //         if (mThermalService != null) {
    //             try {
    //                 mThermalService.asBinder().linkToDeath(() -> {
    //                     mThermalService = null;
    //                 }, /* flags */ 0);
    //             } catch (RemoteException e) {
    //                 Slog.e(TAG, "linkToDeath with thermalService failed", e);
    //                 mThermalService = null;
    //             }
    //         }
    //     }
    //     return mThermalService;
    // }
}
