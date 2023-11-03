package android.app;

import android.annotation.SystemService;

import android.content.Context;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import android.annotation.NonNull;
import android.annotation.Nullable;

@SystemService(Context.THERMAL_MONITOR_SERVICE)
public class ThermalMonitorManager {
    private static String TAG = "ThermalMonitorManager";

    /**
     * @hide
     */
    @UnsupportedAppUsage
    @NonNull
    public static IThermalMonitorManager getService() {
        return IThermalMonitorManagerSingleton.get();
    }

    @UnsupportedAppUsage
    private static final Singleton<IThermalMonitorManager> IThermalMonitorManagerSingleton =
            new Singleton<IThermalMonitorManager>() {
                @Override
                protected IThermalMonitorManager create() {
                    final IBinder b = ServiceManager.getService(Context.THERMAL_MONITOR_SERVICE);
                    final IThermalMonitorManager am = IThermalMonitorManager.Stub.asInterface(b);
                    return am;
                }
            };

    /**
     * @hide
     */
    @NonNull
    public static int plus(@Nullable int a, @Nullable int b) {
        Log.d(TAG, "plus");
        try {
            return getService().plus(a, b);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    @NonNull
    public int plusA(@Nullable int a, @Nullable int b) {
        Log.d(TAG, "plusA");
        try {
            return getService().plus(a, b);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }


    /**
     * @hide
     */
    public ThermalMonitorManager() {
    }
}

