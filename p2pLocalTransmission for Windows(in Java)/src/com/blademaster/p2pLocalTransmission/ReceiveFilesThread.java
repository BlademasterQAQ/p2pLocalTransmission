package com.blademaster.p2pLocalTransmission;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class ReceiveFilesThread extends Thread{
	private String host;
    private int receiveFile_POST;
    private ServerSocket serverSocket;
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    private int file_length;//�ļ�����
    private int currentLocal;//���ڴ����λ��
	
    private Object synchronizeReceiveFile;

    
    public ReceiveFilesThread(int receiveFile_POST,Object synchronizeReceiveFile){//��Ϊ�����
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(receiveFile_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeReceiveFile=synchronizeReceiveFile;
    }

    public ReceiveFilesThread(String host, int receiveFile_POST,Object synchronizeReceiveFile){//��Ϊ�ͻ���
        this.host=host;
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Client";
        this.synchronizeReceiveFile=synchronizeReceiveFile;
    }
    
    @Override
    public void run() {
        Socket socket = null;
        try {
            if (ServiceOrClient == "Service") {
                System.out.println("�ȴ��ͻ�������");
                socket = serverSocket.accept();//�ڿͻ���δ���ӣ��������ݣ�ʱ�����жϣ�������
            }

            if (ServiceOrClient == "Client") {
                //��������
                System.out.println("�ȴ�������...");
                socket = new Socket();//�ڷ����δ����ʱ�����жϣ�������
                SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
                socket.connect(socketAddress, 10000);// 10sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
                //����λ��
                System.out.println("����socket");
            }

            //��ȡ��������ͨ���������ȡ��Ϣ
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //����������Ϣ
            inputFile(input);
            
            synchronized (synchronizeReceiveFile) {
            	synchronizeReceiveFile.notify();
			}
            
            input.close();
            socket.close();
            if(ServiceOrClient == "Service") {//��Ϊ�����ʱ�������߳�ǰӦ�ر�serverSocket����Ȼ�´ν���serverSocket����ж˿�ռ�õĴ���
            	serverSocket.close();
            }
        }catch (SocketTimeoutException s){//���շ�����δ����ʱ��ʱ1s���쳣����
            System.out.println("���Ϳͻ���δ���� sendClient isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void inputFile(DataInputStream input) throws IOException {
    	//���ļ�����
    	int fileCount = input.readInt();
    	
		//���ļ���
    	ArrayList<String> filesName = new ArrayList<>(0);
    	for(int i=0;i<fileCount;i++) {
    		int byteLength = input.readInt();
    		byte[] bytes = new byte[byteLength];
    		input.readFully(bytes);
    		String str = new String(bytes, "UTF-8");  
    		filesName.add(str);
    	}
    	
    	//���ļ���С
    	ArrayList<Integer> filesLength = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
        	filesLength.add(input.readInt());
        }
        
        //���ļ�����޸�����
        ArrayList<Long> fileLastModified = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
        	fileLastModified.add(input.readLong());//����ļ��ĳ���
        }
        
        //���ļ�·�������ڻش�ʱ��ԭ��
    	ArrayList<String> filesPath = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
        	int byteLength = input.readInt();
    		byte[] bytes = new byte[byteLength];
    		input.readFully(bytes);
    		String str = new String(bytes, "UTF-8");  
    		filesPath.add(str);
        }
        
		//���ļ�
    	for(int i=0;i<fileCount;i++) {
    		file_length = filesLength.get(i);//��i���ļ��Ĵ�С
			System.out.println("len = " + file_length);
			System.out.println("�ļ�����" + filesName.get(i)+"��"+i+"��");
			//Ϊ�ļ������������������ļ�
			File file = new File("p2pLocalTransmission������/"+filesName.get(i));//�Ѱ���.png
			if(!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();//�������ļ��У��ǣ�
				//file.createNewFile();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(file);//�ӳ���������ļ���
			
			currentLocal = 0;
			while(currentLocal<filesLength.get(i)-1024) {
				byte[] bytes=new byte[1024];//1M1M�ķ�
				input.readFully(bytes);//��������1M����readFully��ȡ�Ĵ�Сȷʵ��bytes�Ĵ�С
				fileOutputStream.write(bytes,0,1024);//д�ļ�
				currentLocal = currentLocal+1024;
			}
			byte[] bytes=new byte[filesLength.get(i)-currentLocal];//ʣ��С�ڵ���1024�Ĳ���
			input.readFully(bytes);//��������1M
			fileOutputStream.write(bytes,0,filesLength.get(i)-currentLocal);//дʣ���ļ�
			currentLocal = filesLength.get(i);
			
			fileOutputStream.close();//�ر��ļ������ļ�д�����
			
			if(fileLastModified.get(i) != 0) {
				file.setLastModified(fileLastModified.get(i));//�޸��ļ�������޸�ʱ��
			}
			System.out.println("ok");
    	}
    	
    	//����·����Ϣ
    	FileOutputStream fileOutputStream = new FileOutputStream("p2pLocalTransmission·���浵.txt", true);//���ļ�ĩβд��
    	OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
    	BufferedWriter bufferedWriter =new BufferedWriter(outputStreamWriter);
    	
    	for(int i=0;i<fileCount;i++) {
    		bufferedWriter.write(filesName.get(i)+"\t"+filesPath.get(i));
    		bufferedWriter.newLine();
    	}
    	
    	bufferedWriter.close();
	}

    /**
     * ��ȡ��ǰ�ļ��ĳ���
     * @return
     */
    public int getFile_length() {
        return file_length;
    }

    /**
     * ��ȡ���ڴ����λ��
     * @return
     */

    public int getCurrentLocal() {
        return currentLocal;
    }    
}
