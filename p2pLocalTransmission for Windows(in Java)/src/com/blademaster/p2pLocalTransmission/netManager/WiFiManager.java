package com.blademaster.p2pLocalTransmission.netManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 一个WiFi管理类，用于获取WiFi的信息，用于通信
 * @see #isWiFiConnected()
 * @see #getMyIP()
 * @see #getConnecterIP()
 * @see #getConnecterMAC()
 * @see #isConnecterHDCP()
 * 
 * @author blademaster
 *
 */
public class WiFiManager {
	private boolean isWiFiConnected=false;
	private String myIP=null;//本机的WiFi ip
	private String connecterIP=null;//本机连接的路由的IP
	private String connecterMAC=null;//本机连接的路由的MAC
	private boolean isConnecterHDCP=false;//本机连接的路由是否为HDCP动态
	
	public WiFiManager() {
		// TODO 自动生成的构造函数存根
		
		//******************由"ipconfig"获取myIP*********************
		String str=Cmd.command("ipconfig");
		str=str+"\r\n";//增加用于确定文字分段的回车
		
		//获取“无线局域网适配器 WLAN”段
		String WLAN_str=null;
		Pattern pattern = Pattern.compile("无线局域网适配器 WLAN:\r\n\r\n[\\s\\S]*?\r\n\r\n");//设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
		Matcher matcher = pattern.matcher(str);//对str进行正则表达式搜索
		if(matcher.find()){
			int start = matcher.start();
		    int end = matcher.end();
		    WLAN_str=str.substring(start,end);
		}
		if(WLAN_str==null) {
			return;
		}
		
		//获取myIP
		pattern = Pattern.compile("IPv4 地址 . . . . . . . . . . . . : ");//设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
		matcher = pattern.matcher(WLAN_str);//对str进行正则表达式搜索
		if(matcher.find()){
		    int end = matcher.end();
		    myIP=WLAN_str.substring(end).split("\r\n")[0];//首先截取匹配到的字符串，然后读到回车，获取ip地址
		    isWiFiConnected=true;
		}
		else {
			isWiFiConnected=false;
			return;
		}
		
		//当WiFi连接时
		//******************由"ARP -a"获取connecterIP*********************
		str=Cmd.command("ARP -a");
		str=str+"\r\n";//增加用于确定文字分段的回车
		
		//获取connecterIP
		pattern = Pattern.compile("接口: "+myIP+"[\\s\\S]*?类型\r\n");//设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
		matcher = pattern.matcher(str);//对str进行正则表达式搜索
		if(matcher.find()){
		    int end = matcher.end();
		    String connecter_str=str.substring(end).split("\r\n")[0];//首先截取匹配到的字符串，然后读到回车，获取ip地址
		    connecterIP=connecter_str.split("\\s+")[1];//"\\s+"为多个空格的正则表达式
		    connecterMAC=connecter_str.split("\\s+")[2];
		   	isConnecterHDCP=connecter_str.split("\\s+")[3].equals("动态") ? true : false;
		}
	}
	
	/**
	 * 判断WiFi是否连接
	 * @return
	 */
	public boolean isWiFiConnected() {
        return isWiFiConnected;
    }
	
	/**
	 * 返回本机的WLAN ip
	 * @return
	 */
    public String getMyIP() {
        return myIP;
    }

    /**
   	 * 返回本机连接的设备的ip
   	 * @return
   	 */
    public String getConnecterIP() {
        return connecterIP;
    }

    /**
   	 * 返回本机连接的设备的MAC
   	 * @return
   	 */
    public String getConnecterMAC() {
        return connecterMAC;
    }

    /**
   	 * 返回本机连接的设备的状态（静态or动态）
   	 * @return
   	 */
    public boolean isConnecterHDCP() {
        return isConnecterHDCP;
    }
    
    /**
     * 获取正在连接的WiFi的名称（SSID）
     * @return
     */
    public String getConnectingWiFiSSID() {
    	String str=Cmd.command("netsh wlan show interfaces");
    	Pattern pattern = Pattern.compile("SSID[\\s\\S]*?: ");//设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
		Matcher matcher = pattern.matcher(str);//对str进行正则表达式搜索
		if(matcher.find()){
		    int end = matcher.end();
		    String SSID=str.substring(end).split("\r\n")[0];//首先截取匹配到的字符串，然后读到回车，获取ip地址
		    return SSID;
		}
		return null;
    }
}
