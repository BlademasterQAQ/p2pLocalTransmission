package com.blademaster.p2plocaltransmission;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 用于获取手机WiFi信息，如手机是否开启WiFi，手机是否连上WiFi，WiFi的名称和ip
 *
 * @author blademaster
 */
public class MobileWiFiInfo {
    private boolean isWiFiOpened=false;
    private boolean isWiFiConnected=false;
    private String WiFiName=null;
    private String WiFiIP=null;
    private String MobileIP=null;

    private boolean isHotspotOpened=false;
    private boolean isHotspotConnected=false;
    private int hotspotConnectedCount;

    public static final int WIFI_AP_STATE_DISABLING = 10;//在WiFiManager中不可视（被注明@SystemAPI @hide）
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    public void getMobileWiFiInfo(Context context){
        //获取“WiFi管理者”对象
        WifiManager wifiManager=(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifiManager.getConnectionInfo();//用于获取手机的信息
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();//用于获取WiFi（网关）的信息

        if(wifiManager.isWifiEnabled()){//手机WiFi是否开启
            isWiFiOpened=true;
            if(wifiinfo.getIpAddress()!=0){//已连上WiFi,此时手机的ip地址不为0（与流量进行区分）
                isWiFiConnected=true;
                WiFiName= wifiinfo.getSSID();
                WiFiIP=IP2String(dhcpInfo.gateway);//dhcpInfo.serverAddress亦可
                MobileIP=IP2String(wifiinfo.getIpAddress());
            }
            else{
                isWiFiConnected=false;
            }
        }
        else {
            isWiFiOpened=false;
        }

        if(isApEnabled(context)){
            isHotspotOpened=true;
            ArrayList<String> connectedHotIP= getConnectedHotspotIP();
            if(connectedHotIP.size()>1){//第一个元素为"IP"，需要剔除
                isHotspotConnected=true;
                hotspotConnectedCount =connectedHotIP.size()-1;
            }
            else {
                isHotspotConnected=false;
            }
        }
        else {
            isHotspotOpened=false;
        }
    }

    private int getWifiApState(Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            int i = (Integer) method.invoke(wifiManager);
            //Log.i("hotspot","wifi state:  " + i);
            return i;
        } catch (Exception e) {
            Log.e("hotspot","Cannot get WiFi AP state" + e);
            return WIFI_AP_STATE_FAILED;
        }
    }

    private boolean isApEnabled(Context mContext) {
        int state = getWifiApState(mContext);
        return WIFI_AP_STATE_ENABLING == state || WIFI_AP_STATE_ENABLED == state;
    }

    /**
     * 第一个元素为字符"IP"，后面的元素才是热点的ip地址
     * @return
     */

    public ArrayList<String> getConnectedHotspotIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }



    public boolean isWiFiOpened() {
        return isWiFiOpened;
    }

    public boolean isWiFiConnected() {
        return isWiFiConnected;
    }

    public String getWiFiName() {
        return WiFiName;
    }

    public String getWiFiIP() {
        return WiFiIP;
    }

    public String getMobileIP() {
        return MobileIP;
    }

    public boolean isHotspotOpened() {
        return isHotspotOpened;
    }

    public boolean isHotspotConnected() {
        return isHotspotConnected;
    }

    public int getHotspotConnectedCount() {
        return hotspotConnectedCount;
    }

    private String IP2String(int i) {//将int型的IPv4转为String输出

        return ( (i >> 0)  & 0xFF ) + "." +
                ( (i >> 8)  & 0xFF ) + "." +
                ( (i >> 16) & 0xFF ) + "." +
                ( (i >> 24) & 0xFF ) ;
    }
}

