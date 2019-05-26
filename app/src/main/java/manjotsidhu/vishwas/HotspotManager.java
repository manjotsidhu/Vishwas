package manjotsidhu.vishwas;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HotspotManager {
    Context context;
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private WifiManager mWifiManager;

    HotspotManager(Context context) {
        this.context = context;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    void start(String ssid, String pwd) {
        // API > 26 Doesn't support setting ssid and pwd, it always creates hotspot with random credentials
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
            //turnOnHotspot();
        } else {
            turnOnHotspot(ssid, pwd);
        }
    }

    void stop() {
        // API > 26 Doesn't support setting ssid and pwd
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
            //if (mReservation != null) {
            //    mReservation.close();
            //}
        } else {
            Method method = null;

            try {
                method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(mWifiManager, null, false);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot() {
        //WifiManager manager = (WifiManager) mContextRef.get().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d("WIFI", "Wifi Hotspot is on now");
                mReservation = reservation;
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d("WIFI", "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d("WIFI", "onFailed: ");
            }
        }, new Handler());
    }

    private void turnOnHotspot(String ssid, String pwd) {
        mWifiManager.setWifiEnabled(false);

        WifiConfiguration wifiCon = new WifiConfiguration();
        wifiCon.SSID = ssid;
        wifiCon.preSharedKey = pwd;
        wifiCon.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiCon.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        try {
            Method setWifiApMethod = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus=(Boolean) setWifiApMethod.invoke(mWifiManager, wifiCon, true);
        }
        catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
        }
    }
}
