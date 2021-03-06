package test.com.blootoothtester.network.hotspot;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.lang.reflect.*;


public class WifiUtils {

    //check whether wifi hotspot on or off
    public static boolean isHotspotOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {
        }
        return false;
    }

    // toggle wifi hotspot on or off
    public static boolean enableHotspot(Context context, String newName) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it off
//            if (isHotspotOn(context)) {
//                wifiManager.setWifiEnabled(false);
//            }
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            wifiConfig.SSID = newName;

            Method method = wifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            // we need to turn the hotspot on first as if it is already on somtimes the name
            // does not change
            method.invoke(wifiManager, null, false);
            method.invoke(wifiManager, wifiConfig, true);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean disableWifi(Context context) {
        // commented out method DOES NOT WORK :( tested on OPO
//        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
//                .getSystemService(Context.WIFI_SERVICE);
//        return wifiManager.setWifiEnabled(false);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        try {
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            Method method = wifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            // we need to turn the hotspot on first as if it is already on somtimes the name
            // does not change
            method.invoke(wifiManager, null, false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    public static boolean setHotspotName(String newName, Context context) {
//        try {
//            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(
//                    Context.WIFI_SERVICE);
//            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
//            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);
//
//            wifiConfig.SSID = newName;
////            wifiConfig.preSharedKey
//
//            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
//            setConfigMethod.invoke(wifiManager, wifiConfig);
//
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public static boolean connectToWifi(String ssid, Context context) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        // we use netId to actually connect to the network via enableNetwork
        int netId = wifiManager.addNetwork(wifiConfig);
        if (!wifiManager.disconnect()) {
            Toast.makeText(context, "Failed to disconnect!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!wifiManager.enableNetwork(netId, true)) {
            Toast.makeText(context, "Failed to enable network!", Toast.LENGTH_SHORT).show();
            return false;
        }
//        if (!wifiManager.reconnect()) {
//            Toast.makeText(context, "Failed to reconnect to network!", Toast.LENGTH_SHORT).show();
//            return false;
//        }

        return true;
    }

    public static void startWifiScan(WifiManager wifiManager) {
        wifiManager.startScan();
    }
}
