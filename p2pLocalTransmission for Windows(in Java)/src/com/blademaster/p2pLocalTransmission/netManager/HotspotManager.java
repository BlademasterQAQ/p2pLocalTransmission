package com.blademaster.p2pLocalTransmission.netManager;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * һ���ȵ�����࣬���ڻ�ȡ�ȵ����Ϣ������ͨ��
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
	private ArrayList<String> localIP=new ArrayList<>();//���б������ӵĵ�ַ
	private ArrayList<String> myIP=new ArrayList<>();//������ip,��connecterIP��Ӧ
	private ArrayList<String> connecterName=new ArrayList<>();//��������������
	private ArrayList<String> connecterIP=new ArrayList<>();//���ӱ������豸��IP
	private ArrayList<String> connecterMAC=new ArrayList<>();//���ӱ������豸��MAC
	private ArrayList<Boolean> isConnecterHDCP=new ArrayList<>();//���ӱ������豸�Ƿ�ΪHDCP��̬,booleanΪ�������ͣ������ڶ��󣬲�����ΪArrayList������
	
	public HotspotManager() {
		// TODO �Զ����ɵĹ��캯�����
		
		//******************��"ipconfig"��ȡmyIP*********************
		String str=Cmd.command("ipconfig");
		str=str+"\r\n";//��������ȷ�����ֶַεĻس�
		
		//��ȡ�����߾����������� �������ӡ��Σ����ڿ��ܴ��ڶ���������ӣ��������п��ܵı�������
		ArrayList<String> Local_str=new ArrayList<>();
		Pattern pattern = Pattern.compile("���߾����������� ��������[\\s\\S]*?\r\n\r\n[\\s\\S]*?\r\n\r\n");//������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
		Matcher matcher = pattern.matcher(str);//��str����������ʽ����
		while(matcher.find()){
			int start = matcher.start();
		    int end = matcher.end();
		    Local_str.add(str.substring(start,end));
		}
		
		// ��ȡmyIP
		for (int i = 0; i < Local_str.size(); i++) {
			pattern = Pattern.compile("IPv4 ��ַ . . . . . . . . . . . . : ");// ������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
			matcher = pattern.matcher(Local_str.get(i));// ��str����������ʽ����
			if (matcher.find()) {
				int end = matcher.end();
				localIP.add(Local_str.get(i).substring(end).split("\r\n")[0]);// ���Ƚ�ȡƥ�䵽���ַ�����Ȼ������س�����ȡip��ַ
			} 
		}
		if(localIP.isEmpty()) {
			isHotspotConnected = false;
			return;
		}
		//���ȵ�����ʱ
		//******************��"ARP -a"��ȡconnecterIP*********************
		str=Cmd.command("ARP -a");
		str=str+"\r\n";//��������ȷ�����ֶַεĻس�

		for (int i = 0; i < localIP.size(); i++) {
			// ��ȡconnecterIP
			pattern = Pattern.compile("�ӿ�: " + localIP.get(i) + "[\\s\\S]*?����\r\n");// ������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
			matcher = pattern.matcher(str);// ��str����������ʽ����
			if (matcher.find()) {
				int start = matcher.end();
				pattern = Pattern.compile("�ӿ�: " + localIP.get(i) + "[\\s\\S]*?����\r\n[\\s\\S]*?\r\n\r\n");// ������������ʽ��[\\s\\S]*?Ϊ���������ַ���̰��������ʽ
				matcher = pattern.matcher(str);// ��str����������ʽ����
				if (matcher.find()) {//ע��Ҫ��matcher.find()���ܵ���matcher.end()
					int end = matcher.end()-4;// ��ȥ�����س�(һ��"\r\n"����)
					String connecter_str = str.substring(start, end);// ���Ƚ�ȡƥ�䵽���ַ�����Ȼ������س�����ȡip��ַ
					
					String[] connecter_tag=connecter_str.split("\r\n");
					int n=0;
					while(!connecter_tag[n].split("\\s+")[2].equals("ff-ff-ff-ff-ff-ff")) {//Ӧ����Ӿ��������жϣ��˴�ʡ��
						myIP.add(localIP.get(i));
						connecterIP.add(connecter_tag[n].split("\\s+")[1]);// "\\s+"Ϊ����ո��������ʽ
						connecterMAC.add(connecter_tag[n].split("\\s+")[2]);
						isConnecterHDCP.add(connecter_tag[n].split("\\s+")[3].equals("��̬") ? true : false);
						
						isHotspotConnected = true;//�ҵ�����һ����ʵ��������
						n++;
					}
				}
			}
		}
		for(int i=0;i<connecterIP.size();i++) {
			connecterName.add(IP2Hostname(connecterIP.get(i)));//�������豸��ip�������豸��
		}
		
	}
	
	/**
	 * �ж��ȵ��Ƿ����豸����
	 * @return
	 */
	public boolean isHotspotConnected() {
        return isHotspotConnected;
    }
	
	/**
	 * ���ر����ı�������ip�������ж����
	 * @return
	 */
    public ArrayList<String> getMyIP() {
        return myIP;
    }
    
    /**
	 * �������ӱ����ȵ���豸��ip�������ж����
	 * @return
	 */
    public ArrayList<String> getConnecterIP() {
        return connecterIP;
    }

    /**
	 * �������ӱ����ȵ���豸��MAC�������ж����
	 * @return
	 */
    public ArrayList<String> getConnecterMAC() {
        return connecterMAC;
    }

    /**
	 * �������ӱ����ȵ���豸��״̬����̬or��̬���������ж����
	 * @return
	 */
    public ArrayList<Boolean> isConnecterHDCP() {
        return isConnecterHDCP;
    }
    
    /**
	 * �������ӱ����ȵ���豸���������ƣ������ж����
	 * @return
	 */
    public ArrayList<String> getConnecterName() {
        return connecterName;
    }
    
    /**
     * ���ȵ������豸��ipͨ��tracert�������Ϊ������
     * @param ip
     * @return
     */
    
    public static String IP2Hostname(String ip) {
		String str=Cmd.command("tracert "+ip);
		if(str.split("\\s+").length < 17) {//��δ���ٵ�������ʱ
			return null;
		}
		String hostname=str.split("\\s+")[15];//ͨ����cmd��������õ�
		hostname=hostname.split("\\.")[0];//ȥ��.mshome.net��׺
		
		return hostname;
	}
}
