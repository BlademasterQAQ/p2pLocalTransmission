package com.blademaster.p2pLocalTransmission.netManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Cmd {
	public static String command(String cmd){//��ȡcmd���
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			
			//�ر����ͷ���Դ
	    	if(process != null){
	    		process.getOutputStream().close();
	    	}
	    	InputStream in = process.getInputStream();
	    	BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    	
	    	StringBuilder result = new StringBuilder();
	    	String tmp = null;
	    	while ((tmp = br.readLine()) != null) {
	    		result.append(tmp+"\r\n");//��tmp���ݸ��ӣ�append����StringBuilder����
	    	}
	    	return result.toString();
	    	
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
			return null;
		}
	}
}
