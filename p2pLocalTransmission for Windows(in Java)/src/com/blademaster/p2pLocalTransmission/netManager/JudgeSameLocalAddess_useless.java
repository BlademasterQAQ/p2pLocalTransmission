package com.blademaster.p2pLocalTransmission.netManager;

public class JudgeSameLocalAddess_useless {
	/*public static boolean inSameLocalAddess(String IP1,String IP2,String subnetMask) {//�ж�����ip�Ƿ���ͬһ����������涨��ͬһ��������
	int int_IP1=IP_String2Int(IP1);
	int int_IP2=IP_String2Int(IP2);
	int int_subnetMask=IP_String2Int(subnetMask);
	int judge_IP1=(int_IP1&int_subnetMask);
	int judge_IP2=(int_IP2&int_subnetMask);
	System.out.println(int_IP1);
	System.out.println(int_subnetMask);
	System.out.println(judge_IP1);
	return true;
	
}

private static int IP_String2Int(String ip) {
	//ת��ע�⣺1.<<����������ȼ����ڼӼ�����Ҫ�����ţ�2.���β����Դ�ip��ַ����为����������Integer
	//3.long�ͺͷ��ŵ�int�Ͳ���������
	String[] str=ip.split("\\.");//��Ҫת�壡
	return (Integer.valueOf(str[0]).intValue()<<24) + 
		   (Integer.valueOf(str[1]).intValue()<<16) +
		   (Integer.valueOf(str[2]).intValue()<<8) +
		   (Integer.valueOf(str[3]).intValue());
}*/
}
