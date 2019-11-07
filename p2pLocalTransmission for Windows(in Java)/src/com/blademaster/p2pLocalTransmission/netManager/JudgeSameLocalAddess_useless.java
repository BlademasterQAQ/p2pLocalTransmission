package com.blademaster.p2pLocalTransmission.netManager;

public class JudgeSameLocalAddess_useless {
	/*public static boolean inSameLocalAddess(String IP1,String IP2,String subnetMask) {//判断两个ip是否在同一个子网掩码规定的同一个网段下
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
	//转化注意：1.<<运算符的优先级低于加减，需要用括号，2.整形不足以存ip地址（会变负数），需用Integer
	//3.long型和符号的int型不能与运算
	String[] str=ip.split("\\.");//需要转义！
	return (Integer.valueOf(str[0]).intValue()<<24) + 
		   (Integer.valueOf(str[1]).intValue()<<16) +
		   (Integer.valueOf(str[2]).intValue()<<8) +
		   (Integer.valueOf(str[3]).intValue());
}*/
}
