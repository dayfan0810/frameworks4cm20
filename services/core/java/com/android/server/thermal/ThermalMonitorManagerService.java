package com.android.server.thermal;

import android.app.ActivityManager;
import android.app.IThermalMonitorManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.UserHandle;

import com.android.server.SystemService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ThermalMonitorManagerService extends IThermalMonitorManager.Stub {
    private static String TAG = "ThermalMonitorManagerService";
    private static final boolean DEBUG = true;
    private final Context mContext;

    public ThermalMonitorManagerService(Context context) {
        mContext = context;
    }

    private Handler mHandler = new Handler();

    // private Runnable mRunnable = new Runnable() {
    //     @Override
    //     public void run() {
    //         // 执行需要轮询的操作
    //         Slog.i(TAG, "ThermalMonitorManagerService--mRunnable--run");
    //         // 延迟下一次轮询
    //         mHandler.postDelayed(this, 5000);
    //     }
    // };

    @Override
    public int plus(int a, int b) {
        Slog.i(TAG, "plus - a=" + a + ", b=" + b);
        return a + b;
    }


    private void startMonitorServiceIfNeed() {
        Slog.i(TAG, "ThermalMonitorManagerService--start");
        // mHandler.post(mRunnable);
        String json = ThermalUtils.readJsonFile();
        if (json == null) {
            Slog.i(TAG, "json == null");
            return;
        }
        Slog.i(TAG, "json is not null");
        try {
            // 解析JSON数据
            JSONObject jsonObject = new JSONObject(json);
            for (String key : jsonObject.keySet()) {
                Slog.i(TAG, "key = " + key);
                if (key.startsWith("CPU")) {
                    if (isServiceRunning("com.android.server.thermal.CpuThermalService", mContext)) {
                        Slog.i(TAG, "CpuThermalService is already running , return");
                        return;
                    }
                    Intent intent = new Intent(mContext, CpuThermalService.class);
                    mContext.startServiceAsUser(intent, UserHandle.SYSTEM);
                } else if (key.startsWith("Battery")) {
                } else if (key.startsWith("WIFI")) {
                } else if (key.startsWith("CAMERA")) {
                } else if (key.startsWith("LCD")) {
                }
            }
        } catch (JSONException e) {
            Slog.e(TAG, "Failed to parse JSON", e);
        }

    }


    public static final class Lifecycle extends SystemService {
        private ThermalMonitorManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            // if (Settings.Global.getInt(getContext().getContentResolver(),
            //         Settings.Global.NETWORK_WATCHLIST_ENABLED, 1) == 0) {
            //     // Watchlist service is disabled
            //     Slog.i(TAG, "Network Watchlist service is disabled");
            //     return;
            // }
            mService = new ThermalMonitorManagerService(getContext());
            publishBinderService(Context.THERMAL_MONITOR_SERVICE, mService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == PHASE_BOOT_COMPLETED) {
                mService.startMonitorServiceIfNeed();
            }

//            if (SystemService.PHASE_SYSTEM_SERVICES_READY == phase) {
//                final IntentFilter filter = new IntentFilter();
//                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
//                filter.addAction(Intent.ACTION_SCREEN_ON);
//                filter.addAction(Intent.ACTION_SCREEN_OFF);
//                filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
//                getContext().registerReceiver(mBroadcastReceiver, filter);
//                mDeviceState.setCharging(queryIsCharging());
//                mDeviceState.setScreenInteractive(queryScreenInteractive(getContext()));
//            }
        }

//        private boolean queryIsCharging() {
//            final BatteryManagerInternal batteryManager =
//                    LocalServices.getService(BatteryManagerInternal.class);
//            if (batteryManager == null) {
//                Slog.wtf(TAG, "BatteryManager null while starting CachedDeviceStateService");
//                // Default to true to not collect any data.
//                return true;
//            } else {
//                return batteryManager.getPlugType() != OsProtoEnums.BATTERY_PLUGGED_NONE;
//            }
//        }
//
//        private boolean queryScreenInteractive(Context context) {
//            final PowerManager powerManager = context.getSystemService(PowerManager.class);
//            if (powerManager == null) {
//                Slog.wtf(TAG, "PowerManager null while starting CachedDeviceStateService");
//                return false;
//            } else {
//                return powerManager.isInteractive();
//            }
//        }


        public ThermalMonitorManagerService getService() {
            return mService;
        }

    }

    /**
     * 判断服务是否运行
     */
    private boolean isServiceRunning(final String className, Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (info == null || info.size() == 0) return false;
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if (className.equals(aInfo.service.getClassName())) return true;
        }
        return false;
    }


}
