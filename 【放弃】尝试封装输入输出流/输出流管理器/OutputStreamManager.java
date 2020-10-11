package com.blademaster.p2plocaltransmission;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * 一个输出流管理器，接收端使用相应的输入流管理器可以接收特定的输出流格式
 * @author blademaster
 */
public class OutputStreamManager {
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
    private Long file_length;//传输某个文件的长度
    private int currentFileCount;//当前传输到第几个文件

    private ArrayList<ArrayList<Object>> data = new ArrayList<>();//Object可以储存任意类型的数据
    private ArrayList<String> dataName = new ArrayList<>();//数据名（发送该数据的用途）
    private ArrayList<String> dependDataName = new ArrayList<>();//数据依赖关系

    public OutputStreamManager(){
    }

    /**
     * 获取输出流，由输出流输出数据
     * @param out
     */
    public void write(DataOutputStream out) throws IOException {
        CheckOutputStreamManager();

        //发送数据的种数（data的行数）
        out.writeInt(data.size());

        //发送数据类型
        for(int i=0;i<data.size();i++) {
            String string = data.get(i).get(0).getClass().getSimpleName();
            byte[] bytes = string.getBytes();//将String转化为字节类型
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出string的byte长度
            out.write(bytes);//输出string的二进制
        }

        //发送数据名
        for(int i=0;i<data.size();i++) {
            String string = dataName.get(i);
            byte[] bytes = string.getBytes();//将String转化为字节类型
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出string的byte长度
            out.write(bytes);//输出string的二进制
        }

        //发送依赖关系
        for(int i=0;i<data.size();i++) {
            String string = "null";
            if(dependDataName.get(i) != null){
                string = dependDataName.get(i);
            }
            byte[] bytes = string.getBytes();//将String转化为字节类型
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出string的byte长度
            out.write(bytes);//输出string的二进制
        }

        //发送文件的个数（data的列数）
        out.writeInt(data.get(0).size());

        for(int i=0;i<data.size();i++) {
            //Integer类型
            if (data.get(i).get(0).getClass().getSimpleName().equals(Type.Integer)) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    out.writeInt((Integer) data.get(i).get(j));
                }
            }

            //Long类型
            else if (data.get(i).get(0).getClass().getSimpleName().equals(Type.Long)) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    out.writeLong((Long) data.get(i).get(j));
                }
            }

            else if (data.get(i).get(0).getClass().getSimpleName().equals(Type.Boolean)) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    out.writeBoolean((Boolean) data.get(i).get(j));
                }
            }

            else if (data.get(i).get(0).getClass().getSimpleName().equals(Type.Double)) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    out.writeDouble((Double) data.get(i).get(j));
                }
            }

            else if (data.get(i).get(0).getClass().getSimpleName().equals(Type.Float)) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    out.writeFloat((Float) data.get(i).get(j));
                }
            }

            //无特殊长度的类型，如String、file、FileInputStream等类型
            else {
                if (data.get(i).get(0).getClass().getSimpleName().equals(Type.String)) {
                    for (int j = 0; j < data.get(i).size(); j++) {
                        String string = (String) data.get(i).get(j);
                        byte[] bytes = string.getBytes();//将String转化为字节类型
                        int byteLength = bytes.length;
                        out.writeInt(byteLength);//输出string的byte长度
                        out.write(bytes);//输出string的二进制
                    }
                }

                if (data.get(i).get(0).getClass().getSimpleName().equals(Type.File)) {
                    for (int j = 0; j < data.get(i).size(); j++) {
                        File file = (File) data.get(i).get(j);
                        file_length = file.length();//获取文件长度
                        FileInputStream fileInputStream = new FileInputStream(file);
                        out.writeLong(file.length());//文件长度

                        byte[] bytes = new byte[1024];//1k1k的发
                        int n;
                        currentLocal = 0;
                        while ((n = fileInputStream.read(bytes)) != -1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                            try {
                                out.write(bytes, 0, n);//二进制数组写入输出缓冲区，当缓冲区以满时，此处将产生阻塞，直到客户端使用read接收
                            } catch (SocketException s) {
                                if (s.getMessage().equals("Connection reset by peer: socket write error")) {//当对方取消接受时触发
                                    System.err.println("对方主动停止连接");
                                    s.printStackTrace();
                                }
                                break;//跳出while循环
                            }
                            currentLocal = currentLocal + n;//当前传输的数据量
                        }
                        fileInputStream.close();
                        currentFileCount = i+1;
                    }
                }

                if(data.get(i).get(0).getClass().getSimpleName().equals(Type.FileInputStream)) {
                    for (int j = 0; j < data.get(i).size(); j++) {
                        FileInputStream fileInputStream = (FileInputStream) data.get(i).get(j);
                        file_length = (long)fileInputStream.available();
                        out.writeInt(fileInputStream.available());

                        byte[] bytes = new byte[1024];//1k1k的发
                        int n;
                        currentLocal = 0;
                        while ((n = fileInputStream.read(bytes)) != -1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                            try {
                                out.write(bytes, 0, n);//二进制数组写入输出缓冲区，当缓冲区以满时，此处将产生阻塞，直到客户端使用read接收
                            } catch (SocketException s) {
                                if (s.getMessage().equals("Connection reset by peer: socket write error")) {//当对方取消接受时触发
                                    System.err.println("对方主动停止连接");
                                    s.printStackTrace();
                                }
                                break;//跳出while循环
                            }
                            currentLocal = currentLocal + n;//当前传输的数据量
                        }
                        fileInputStream.close();
                        currentFileCount = i+1;
                    }
                }

            }
        }
    }

    /**
     * 检测输入是否有误
     */
    private void CheckOutputStreamManager(){
        if(data.isEmpty()||data.get(0).isEmpty()){
            try {
                throw new Throwable("无数据");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if(data.size() != dataName.size() || dataName.size() != dependDataName.size()){
            try {
                throw new Throwable("输入数据维度不一致");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        for(int i=0;i<data.size();i++){
            if(data.get(0).size() != data.get(i).size()){
                try {
                    throw new Throwable("数据每行的长度不同，不同数据的文件数不同");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            for(int j=1;j<data.get(i).size();j++){
                if(data.get(i).get(0).getClass() != data.get(i).get(j).getClass()){
                    try {
                        throw new Throwable("同一个子ArrayList含有不同类型的数据");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 添加要发送的数据即其名字（发送的目的）
     * {@code dependDataName}是某个数据对之前的数据的依赖关系，如接收文件时需要先根据文件路径创建文件，缺省值为null
     * @param data_single
     * @param dataName_single
     * @param dependDataName_single
     */
    public void addData_single(ArrayList<Object> data_single, String dataName_single, String dependDataName_single){
        this.data.add(data_single);
        this.dataName.add(dataName_single);
        this.dependDataName.add(dependDataName_single);
    }
}
