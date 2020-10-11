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

public class ReceiveFilesThread extends Thread {
	private String host;
	private int receiveFile_POST;
	private ServerSocket serverSocket;
	private String ServiceOrClient = null;// �ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���

	private Object synchronizeReceiveFile;

	// ���ؽ�����Ϣ
	// ����
	private int totalFilesLength;// �ļ��ܴ�С
	private int totalFilesCount;// �ļ�����
	private int totalCurrentLocal;// �ܵĴ������
	// �����ļ�
	private int singleFileLength;// ��ǰ�ļ���С
	private int singleFileCount;// ��ǰ�ļ��ǵڼ����ļ�
	private int singleCurrentLocal;// ���ڴ����λ��

	public ReceiveFilesThread(int receiveFile_POST, Object synchronizeReceiveFile) {// ��Ϊ�����
		this.receiveFile_POST = receiveFile_POST;
		this.ServiceOrClient = "Service";
		try {
			this.serverSocket = new ServerSocket(receiveFile_POST);// ֻ���ڳ�ʼ��ʱ�趨һ��
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.synchronizeReceiveFile = synchronizeReceiveFile;
	}

	public ReceiveFilesThread(String host, int receiveFile_POST, Object synchronizeReceiveFile) {// ��Ϊ�ͻ���
		this.host = host;
		this.receiveFile_POST = receiveFile_POST;
		this.ServiceOrClient = "Client";
		this.synchronizeReceiveFile = synchronizeReceiveFile;
	}

	@Override
	public void run() {
		Socket socket = null;
		try {
			if (ServiceOrClient == "Service") {
				System.out.println("�ȴ��ͻ�������");
				socket = serverSocket.accept();// �ڿͻ���δ���ӣ��������ݣ�ʱ�����жϣ�������
			}

			if (ServiceOrClient == "Client") {
				// ��������
				System.out.println("�ȴ�������...");
				socket = new Socket();// �ڷ����δ����ʱ�����жϣ�������
				SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
				socket.connect(socketAddress, 10000);// 10sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
				// ����λ��
				System.out.println("����socket");
			}

			// ��ȡ��������ͨ���������ȡ��Ϣ
			DataInputStream input = new DataInputStream(socket.getInputStream());
			// ����������Ϣ
			inputFile(input);

			synchronized (synchronizeReceiveFile) {
				synchronizeReceiveFile.notify();
			}

			input.close();
			socket.close();
			if (ServiceOrClient == "Service") {// ��Ϊ�����ʱ�������߳�ǰӦ�ر�serverSocket����Ȼ�´ν���serverSocket����ж˿�ռ�õĴ���
				serverSocket.close();
			}
		} catch (SocketTimeoutException s) {// ���շ�����δ����ʱ��ʱ1s���쳣����
			System.out.println("���Ϳͻ���δ���� sendClient isn't established");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void inputFile(DataInputStream input) throws IOException {
		// ���ļ�����
		int fileCount = input.readInt();
		
		totalFilesCount = fileCount;

		// ���ļ���
		ArrayList<String> filesName = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int byteLength = input.readInt();
			byte[] bytes = new byte[byteLength];
			input.readFully(bytes);
			String str = new String(bytes, "UTF-8");
			filesName.add(str);
		}

		// ���ļ���С
		ArrayList<Integer> filesLength = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int singleFileLength = input.readInt();
			filesLength.add(singleFileLength);
			totalFilesLength = totalFilesLength + singleFileLength;
		}

		// ���ļ�����޸�����
		ArrayList<Long> fileLastModified = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			fileLastModified.add(input.readLong());// ����ļ��ĳ���
		}

		// ���ļ�·�������ڻش�ʱ��ԭ��
		ArrayList<String> filesPath = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int byteLength = input.readInt();
			byte[] bytes = new byte[byteLength];
			input.readFully(bytes);
			String str = new String(bytes, "UTF-8");
			filesPath.add(str);
		}
		
		
		// ���ļ�
		for (int i = 0; i < fileCount; i++) {
			singleFileCount = i + 1;
			singleFileLength = filesLength.get(i);// ��i���ļ��Ĵ�С
			System.out.println("len = " + singleFileLength);
			System.out.println("�ļ�����" + filesName.get(i) + "��" + i + "��");
			// Ϊ�ļ������������������ļ�
			File file = new File("p2pLocalTransmission������/" + filesName.get(i));// �Ѱ���.png
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();// �������ļ��У��ǣ�
				// file.createNewFile();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(file);// �ӳ���������ļ���

			singleCurrentLocal = 0;// ÿ��һ���ļ�������һ��
			while (singleCurrentLocal < filesLength.get(i) - 1024) {
				byte[] bytes = new byte[1024];// 1M1M�ķ�
				input.readFully(bytes);// ��������1M����readFully��ȡ�Ĵ�Сȷʵ��bytes�Ĵ�С
				fileOutputStream.write(bytes, 0, 1024);// д�ļ�
				singleCurrentLocal = singleCurrentLocal + 1024;
				totalCurrentLocal = totalCurrentLocal + 1024;
			}
			byte[] bytes = new byte[filesLength.get(i) - singleCurrentLocal];// ʣ��С�ڵ���1024�Ĳ���
			input.readFully(bytes);// ��ʣ��С�ڵ���1M��������
			fileOutputStream.write(bytes, 0, filesLength.get(i) - singleCurrentLocal);// дʣ���ļ�
			singleCurrentLocal = filesLength.get(i);
			totalCurrentLocal = totalCurrentLocal + bytes.length;
			
			fileOutputStream.close();// �ر��ļ������ļ�д�����

			if (fileLastModified.get(i) != 0) {
				file.setLastModified(fileLastModified.get(i));// �޸��ļ�������޸�ʱ��
			}
			System.out.println("ok");
		}

		// ����·����Ϣ
		FileOutputStream fileOutputStream = new FileOutputStream("p2pLocalTransmission·���浵.txt", true);// ���ļ�ĩβд��
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		for (int i = 0; i < fileCount; i++) {
			bufferedWriter.write(filesName.get(i) + "\t" + filesPath.get(i));
			bufferedWriter.newLine();
		}

		bufferedWriter.close();
	}

	/**
	 * ��ȡ���δ������ܵ��ļ���С
	 * @return
	 */
	public int getTotalFilesLength() {
		return totalFilesLength;
	}
	
	/**
	 * ��ȡ���δ������ܵ��ļ�����
	 * @return
	 */
	public int getTotalFilesCount() {
		return totalFilesCount;
	}

	/**
	 * ��ȡ���δ������ܵĴ������
	 * @return
	 */
	public int getTotalCurrentLocal() {
		return totalCurrentLocal;
	}
	
	/**
	 * ��ȡ���ڴ����ĳ���ļ��Ĵ�С
	 * @return
	 */
	public int getSingleFileLength() {
		return singleFileLength;
	}

	/**
	 * ��ȡ���ڴ����ĳ���ļ��ǵڼ����ļ�
	 * @return
	 */
	public int getSingleFileCount() {
		return singleFileCount;
	}

	/**
	 * ��ȡ���ڴ����ĳ���ļ��Ĵ������
	 * @return
	 */
	public int getSingleCurrentLocal() {
		return singleCurrentLocal;
	}
}
