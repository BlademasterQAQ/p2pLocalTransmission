package com.blademaster.p2pLocalTransmission;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Target;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * һ������������������ն�ʹ����Ӧ�����������������Խ����ض����������ʽ
 * @author blademaster
 */
public class InputStreamManager {
	/**
     * ������ݵ�����
     */
    public static class Type{
        public static String Integer = "Integer";
        public static String Long = "Long";
        public static String Boolean = "Boolean";
        public static String Double = "Double";
        public static String Float = "Float";
        public static String String = "String";
        public static String File = "File";
        public static String FileInputStream = "FileInputStream";
    }
    private int currentLocal;//����ĳ���ļ��ĵ�ǰλ��
    private int file_length;//����ĳ���ļ��ĳ���
    private int currentFileCount;//��ǰ���䵽�ڼ����ļ�

    private int dataCount;
    private ArrayList<String> dataType = new ArrayList<>();//��������
    private ArrayList<String> dataName = new ArrayList<>();//�����������ո����ݵ���;��
    private ArrayList<String> dependDataName = new ArrayList<>();//����������ϵ
    private int fileCount;
    private ArrayList<ArrayList<Object>> data = new ArrayList<>();//Object���Դ����������͵�����
    
    public InputStreamManager(){
    }

    /**
     * ��ȡ���������������������
     * @param in
     */
    public void read(DataInputStream in) throws IOException {

        //�������ݵ�������data��������
    	dataCount = in.readInt();

        //������������
        for(int i=0;i<dataCount;i++) {
    		int byteLength = in.readInt();
    		byte[] bytes = new byte[byteLength];
    		in.readFully(bytes);
    		String string = new String(bytes, "UTF-8");  
    		dataType.add(string);
        }

        //����������
        for(int i=0;i<dataCount;i++) {
    		int byteLength = in.readInt();
    		byte[] bytes = new byte[byteLength];
    		in.readFully(bytes);
    		String string = new String(bytes, "UTF-8");  
    		dataName.add(string);
        }
        
        //����������ϵ
        for(int i=0;i<dataCount;i++) {
    		int byteLength = in.readInt();
    		byte[] bytes = new byte[byteLength];
    		in.readFully(bytes);
    		String string = new String(bytes, "UTF-8"); 
    		if(string == "null") {
    			dependDataName.add(null);
    		}else {
        		dependDataName.add(string);
			}
        }

        //�����ļ��ĸ�����data��������
        fileCount = in.readInt();
        
    	ArrayList<Object> arrayList = new ArrayList<>();//��ʱ�洢
        for(int i=0;i<dataCount;i++) {

            //Integer����
            if (dataType.get(i).equals(Type.Integer)) {
            	
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readInt());
                }
                data.add(arrayList);
            }

            //Long����
            else if (dataType.get(i).equals(Type.Long)) {
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readLong());
                }
                data.add(arrayList);
            }

            else if (dataType.get(i).equals(Type.Boolean)) {
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readBoolean());
                }
                data.add(arrayList);
            }

            else if (dataType.get(i).equals(Type.Double)) {
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readDouble());
                }
                data.add(arrayList);
            }

            else if (dataType.get(i).equals(Type.Float)) {
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readFloat());
                }
                data.add(arrayList);
            }

            //�����ⳤ�ȵ����ͣ���String��file��FileInputStream������
            else {
                if (dataType.get(i).equals(Type.String)) {
                    for (int j = 0; j < fileCount; j++) {
                    	int byteLength = in.readInt();
                		byte[] bytes = new byte[byteLength];
                		in.readFully(bytes);
                		String string = new String(bytes, "UTF-8");  
                		arrayList.add(string);
                    }
                    data.add(arrayList);
                }

                if (dataType.get(i).equals(Type.File) || dataType.get(i).equals(Type.FileInputStream)) {
                	int dependLocal = dataName.indexOf(dependDataName.get(i));//����������λ��

                    for (int j = 0; j < fileCount; j++) {
                    	//System.err.println((String) data.get(dependLocal).get(j));
                    	File file = new File((String) data.get(dependLocal).get(j));//�Ѱ���.png
            			FileOutputStream fileOutputStream = new FileOutputStream(file);//�ӳ���������ļ���
            			file_length = (int) file.length();
            			System.err.println(file_length);//�ļ�����Ҳ��һ�������������ķ�װ�Լ۱Ƚϵ�
            			
            			currentLocal = 0;
            			while(currentLocal<file_length-1024) {
            				byte[] bytes=new byte[1024];//1M1M�ķ�
            				in.readFully(bytes);//��������1M����readFully��ȡ�Ĵ�Сȷʵ��bytes�Ĵ�С
            				fileOutputStream.write(bytes,0,1024);//д�ļ�
            				currentLocal = currentLocal+1024;
            			}
            			byte[] bytes=new byte[file_length-currentLocal];//ʣ��С�ڵ���1024�Ĳ���
            			in.readFully(bytes);//��������1M
            			fileOutputStream.write(bytes,0,file_length-currentLocal);//д�ļ�1M
            			currentLocal = file_length;
            			
            			fileOutputStream.close();//�ر��ļ������ļ�д�����
            			
                		arrayList.add(file);
                    }
                    data.add(arrayList);
                }
                
            }
        }
    }
}
