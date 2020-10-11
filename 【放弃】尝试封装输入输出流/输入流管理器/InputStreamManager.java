package com.blademaster.p2pLocalTransmission;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Target;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * 一个输出流管理器，接收端使用相应的输入流管理器可以接收特定的输出流格式
 * @author blademaster
 */
public class InputStreamManager {
	/**
     * 输出数据的类型
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
    private int currentLocal;//传输某个文件的当前位置
    private int file_length;//传输某个文件的长度
    private int currentFileCount;//当前传输到第几个文件

    private int dataCount;
    private ArrayList<String> dataType = new ArrayList<>();//数据类型
    private ArrayList<String> dataName = new ArrayList<>();//数据名（接收该数据的用途）
    private ArrayList<String> dependDataName = new ArrayList<>();//数据依赖关系
    private int fileCount;
    private ArrayList<ArrayList<Object>> data = new ArrayList<>();//Object可以储存任意类型的数据
    
    public InputStreamManager(){
    }

    /**
     * 获取输出流，由输出流输出数据
     * @param in
     */
    public void read(DataInputStream in) throws IOException {

        //接收数据的种数（data的行数）
    	dataCount = in.readInt();

        //接收数据类型
        for(int i=0;i<dataCount;i++) {
    		int byteLength = in.readInt();
    		byte[] bytes = new byte[byteLength];
    		in.readFully(bytes);
    		String string = new String(bytes, "UTF-8");  
    		dataType.add(string);
        }

        //接收数据名
        for(int i=0;i<dataCount;i++) {
    		int byteLength = in.readInt();
    		byte[] bytes = new byte[byteLength];
    		in.readFully(bytes);
    		String string = new String(bytes, "UTF-8");  
    		dataName.add(string);
        }
        
        //接收依赖关系
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

        //接收文件的个数（data的列数）
        fileCount = in.readInt();
        
    	ArrayList<Object> arrayList = new ArrayList<>();//临时存储
        for(int i=0;i<dataCount;i++) {

            //Integer类型
            if (dataType.get(i).equals(Type.Integer)) {
            	
                for (int j = 0; j < fileCount; j++) {
                	arrayList.add(in.readInt());
                }
                data.add(arrayList);
            }

            //Long类型
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

            //无特殊长度的类型，如String、file、FileInputStream等类型
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
                	int dependLocal = dataName.indexOf(dependDataName.get(i));//查找依赖项位置

                    for (int j = 0; j < fileCount; j++) {
                    	//System.err.println((String) data.get(dependLocal).get(j));
                    	File file = new File((String) data.get(dependLocal).get(j));//已包含.png
            			FileOutputStream fileOutputStream = new FileOutputStream(file);//从程序输出到文件中
            			file_length = (int) file.length();
            			System.err.println(file_length);//文件长度也是一个依赖项，这个样的封装性价比较低
            			
            			currentLocal = 0;
            			while(currentLocal<file_length-1024) {
            				byte[] bytes=new byte[1024];//1M1M的发
            				in.readFully(bytes);//读输入流1M，用readFully读取的大小确实是bytes的大小
            				fileOutputStream.write(bytes,0,1024);//写文件
            				currentLocal = currentLocal+1024;
            			}
            			byte[] bytes=new byte[file_length-currentLocal];//剩余小于等于1024的部分
            			in.readFully(bytes);//读输入流1M
            			fileOutputStream.write(bytes,0,file_length-currentLocal);//写文件1M
            			currentLocal = file_length;
            			
            			fileOutputStream.close();//关闭文件流，文件写入完成
            			
                		arrayList.add(file);
                    }
                    data.add(arrayList);
                }
                
            }
        }
    }
}
