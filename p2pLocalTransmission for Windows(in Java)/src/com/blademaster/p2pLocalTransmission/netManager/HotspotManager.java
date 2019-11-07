package com.blademaster.p2pLocalTransmission.netManager;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一个热点管理类，用于获取热点的信息，用于通信
 * @see #isHotspotConnected()
 * @see #getMyIP()
 * @see #getConnecterIP()
 * @see #getConnecterMAC()
 * @see #isConnecterHDCP()
 * 
 * @author blademaster
 *
 */
public class HotspotManager {
	private boolean isHotspotConnected=false;
	private ArrayList<String> localIP=new ArrayList<>();//所有本地连接的地址
	private ArrayList<String> myIP=new ArrayList<>();//本机的ip,与connecterIP对应
	private ArrayList<String> connecterName=new ArrayList<>();//解析的主机名称
	private ArrayList<String> connecterIP=new ArrayList<>();//连接本机的设备的IP
	private ArrayList<String> connecterMAC=new ArrayList<>();//连接本机的设备的MAC
	private ArrayList<Boolean> isConnecterHDCP=new ArrayList<>();//连接本机的设备是否为HDCP动态,boolean为数据类型，不属于对象，不能作为ArrayList的类型
	
	public HotspotManager() {
		// TODO 自动生成的构造函数存根
		
		//******************由"ipconfig"获取myIP*********************
		String str=Cmd.command("ipconfig");
		str=str+"\r\n";//增加用于确定文字分段的回车
		
		//获取“无线局域网适配器 本地连接”段，由于可能存在多个本地连接，查找所有可能的本地连接
		ArrayList<String> Local_str=new ArrayList<>();
		Pattern pattern = Pattern.compile("无线局域网适配器 本地连接[\\s\\S]*?\r\n\r\n[\\s\\S]*?\r\n\r\n");//设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
		Matcher matcher = pattern.matcher(str);//对str进行正则表达式搜索
		while(matcher.find()){
			int start = matcher.start();
		    int end = matcher.end();
		    Local_str.add(str.substring(start,end));
		}
		
		// 获取myIP
		for (int i = 0; i < Local_str.size(); i++) {
			pattern = Pattern.compile("IPv4 地址 . . . . . . . . . . . . : ");// 设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
			matcher = pattern.matcher(Local_str.get(i));// 对str进行正则表达式搜索
			if (matcher.find()) {
				int end = matcher.end();
				localIP.add(Local_str.get(i).substring(end).split("\r\n")[0]);// 首先截取匹配到的字符串，然后读到回车，获取ip地址
			} 
		}
		if(localIP.isEmpty()) {
			isHotspotConnected = false;
			return;
		}
		//当热点连接时
		//******************由"ARP -a"获取connecterIP*********************
		str=Cmd.command("ARP -a");
		str=str+"\r\n";//增加用于确定文字分段的回车

		for (int i = 0; i < localIP.size(); i++) {
			// 获取connecterIP
			pattern = Pattern.compile("接口: " + localIP.get(i) + "[\\s\\S]*?类型\r\n");// 设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
			matcher = pattern.matcher(str);// 对str进行正则表达式搜索
			if (matcher.find()) {
				int start = matcher.end();
				pattern = Pattern.compile("接口: " + localIP.get(i) + "[\\s\\S]*?类型\r\n[\\s\\S]*?\r\n\r\n");// 设置正则表达形式，[\\s\\S]*?为任意数量字符非贪婪正则表达式
				matcher = pattern.matcher(str);// 对str进行正则表达式搜索
				if (matcher.find()) {//注意要先matcher.find()才能调用matcher.end()
					int end = matcher.end()-4;// 除去两个回车(一个"\r\n"两个)
					String connecter_str = str.substring(start, end);// 首先截取匹配到的字符串，然后读到回车，获取ip地址
					
					String[] connecter_tag=connecter_str.split("\r\n");
					int n=0;
					while(!connecter_tag[n].split("\\s+")[2].equals("ff-ff-ff-ff-ff-ff")) {//应该添加局域网的判断，此处省略
						myIP.add(localIP.get(i));
						connecterIP.add(connecter_tag[n].split("\\s+")[1]);// "\\s+"为多个空格的正则表达式
						connecterMAC.add(connecter_tag[n].split("\\s+")[2]);
						isConnecterHDCP.add(connecter_tag[n].split("\\s+")[3].equals("动态") ? true : false);
						
						isHotspotConnected = true;//找到至少一个真实的连接者
						n++;
					}
				}
			}
		}
		for(int i=0;i<connecterIP.size();i++) {
			connecterName.add(IP2Hostname(connecterIP.get(i)));//由连接设备的ip解析出设备名
		}
		
	}
	
	/**
	 * 判断热点是否有设备接入
	 * @return
	 */
	public boolean isHotspotConnected() {
        return isHotspotConnected;
    }
	
	/**
	 * 返回本机的本地连接ip（可能有多个）
	 * @return
	 */
    public ArrayList<String> getMyIP() {
        return myIP;
    }
    
    /**
	 * 返回连接本机热点的设备的ip（可能有多个）
	 * @return
	 */
    public ArrayList<String> getConnecterIP() {
        return connecterIP;
    }

    /**
	 * 返回连接本机热点的设备的MAC（可能有多个）
	 * @return
	 */
    public ArrayList<String> getConnecterMAC() {
        return connecterMAC;
    }

    /**
	 * 返回连接本机热点的设备的状态（静态or动态）（可能有多个）
	 * @return
	 */
    public ArrayList<Boolean> isConnecterHDCP() {
        return isConnecterHDCP;
    }
    
    /**
	 * 返回连接本机热点的设备的主机名称（可能有多个）
	 * @return
	 */
    public ArrayList<String> getConnecterName() {
        return connecterName;
    }
    
    /**
     * 将热点连接设备的ip通过tracert命令解析为主机名
     * @param ip
     * @return
     */
    
    public static String IP2Hostname(String ip) {
		String str=Cmd.command("tracert "+ip);
		if(str.split("\\s+").length < 17) {//当未跟踪到主机名时
			return null;
		}
		String hostname=str.split("\\s+")[15];//通过对cmd输出分析得到
		hostname=hostname.split("\\.")[0];//去除.mshome.net后缀
		
		return hostname;
	}
}
