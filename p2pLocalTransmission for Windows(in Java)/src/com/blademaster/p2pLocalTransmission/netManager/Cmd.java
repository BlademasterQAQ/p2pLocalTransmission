package com.blademaster.p2pLocalTransmission.netManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Cmd {
	public static String command(String cmd){//获取cmd输出
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			
			//关闭流释放资源
	    	if(process != null){
	    		process.getOutputStream().close();
	    	}
	    	InputStream in = process.getInputStream();
	    	BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    	
	    	StringBuilder result = new StringBuilder();
	    	String tmp = null;
	    	while ((tmp = br.readLine()) != null) {
	    		result.append(tmp+"\r\n");//将tmp内容附加（append）到StringBuilder类中
	    	}
	    	return result.toString();
	    	
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return null;
		}
	}
}
