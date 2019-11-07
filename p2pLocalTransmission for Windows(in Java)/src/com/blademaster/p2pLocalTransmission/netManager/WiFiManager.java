package com.blademaster.p2pLocalTransmission.netManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * һ��WiFi�����࣬���ڻ�ȡWiFi����Ϣ������ͨ��
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
	private String myIP=null;//������WiFi ip
	private String connecterIP=null;//�������ӵ�·�ɵ�IP
	private String connecterMAC=null;//�������ӵ�·�ɵ�MAC
	private boolean isConnecterHDCP=false;//�������ӵ�·���Ƿ�ΪHDCP��̬
	
	public WiFiManager() {
		// TODO �Զ����ɵĹ��캯�����
		
		//******************��"ipconfig"��ȡmyIP*********************
		String str=Cmd.command("ipconfig");
		str=str+"\r\n";//��������ȷ�����ֶַεĻس�
		
		//��ȡ�����߾����������� WLAN����
		String WLAN_str=null;
		Pattern pattern = Pattern.compile("���߾����������� WLAN:\r\n\r\n[\\s\\S]*?\r\n\r\n");//������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
		Matcher matcher = pattern.matcher(str);//��str����������ʽ����
		if(matcher.find()){
			int start = matcher.start();
		    int end = matcher.end();
		    WLAN_str=str.substring(start,end);
		}
		if(WLAN_str==null) {
			return;
		}
		
		//��ȡmyIP
		pattern = Pattern.compile("IPv4 ��ַ . . . . . . . . . . . . : ");//������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
		matcher = pattern.matcher(WLAN_str);//��str����������ʽ����
		if(matcher.find()){
		    int end = matcher.end();
		    myIP=WLAN_str.substring(end).split("\r\n")[0];//���Ƚ�ȡƥ�䵽���ַ�����Ȼ������س�����ȡip��ַ
		    isWiFiConnected=true;
		}
		else {
			isWiFiConnected=false;
			return;
		}
		
		//��WiFi����ʱ
		//******************��"ARP -a"��ȡconnecterIP*********************
		str=Cmd.command("ARP -a");
		str=str+"\r\n";//��������ȷ�����ֶַεĻس�
		
		//��ȡconnecterIP
		pattern = Pattern.compile("�ӿ�: "+myIP+"[\\s\\S]*?����\r\n");//������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
		matcher = pattern.matcher(str);//��str����������ʽ����
		if(matcher.find()){
		    int end = matcher.end();
		    String connecter_str=str.substring(end).split("\r\n")[0];//���Ƚ�ȡƥ�䵽���ַ�����Ȼ������س�����ȡip��ַ
		    connecterIP=connecter_str.split("\\s+")[1];//"\\s+"Ϊ����ո��������ʽ
		    connecterMAC=connecter_str.split("\\s+")[2];
		   	isConnecterHDCP=connecter_str.split("\\s+")[3].equals("��̬") ? true : false;
		}
	}
	
	/**
	 * �ж�WiFi�Ƿ�����
	 * @return
	 */
	public boolean isWiFiConnected() {
        return isWiFiConnected;
    }
	
	/**
	 * ���ر�����WLAN ip
	 * @return
	 */
    public String getMyIP() {
        return myIP;
    }

    /**
   	 * ���ر������ӵ��豸��ip
   	 * @return
   	 */
    public String getConnecterIP() {
        return connecterIP;
    }

    /**
   	 * ���ر������ӵ��豸��MAC
   	 * @return
   	 */
    public String getConnecterMAC() {
        return connecterMAC;
    }

    /**
   	 * ���ر������ӵ��豸��״̬����̬or��̬��
   	 * @return
   	 */
    public boolean isConnecterHDCP() {
        return isConnecterHDCP;
    }
    
    /**
     * ��ȡ�������ӵ�WiFi�����ƣ�SSID��
     * @return
     */
    public String getConnectingWiFiSSID() {
    	String str=Cmd.command("netsh wlan show interfaces");
    	Pattern pattern = Pattern.compile("SSID[\\s\\S]*?: ");//������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
		Matcher matcher = pattern.matcher(str);//��str����������ʽ����
		if(matcher.find()){
		    int end = matcher.end();
		    String SSID=str.substring(end).split("\r\n")[0];//���Ƚ�ȡƥ�䵽���ַ�����Ȼ������س�����ȡip��ַ
		    return SSID;
		}
		return null;
    }
}
