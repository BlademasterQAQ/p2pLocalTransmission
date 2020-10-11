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
	private String ServiceOrClient = null;// 由初始化时只有post和既有host又有post区分是服务端还是客户端

	private Object synchronizeReceiveFile;

	// 返回进度信息
	// 总体
	private int totalFilesLength;// 文件总大小
	private int totalFilesCount;// 文件总数
	private int totalCurrentLocal;// 总的传输进度
	// 单个文件
	private int singleFileLength;// 当前文件大小
	private int singleFileCount;// 当前文件是第几个文件
	private int singleCurrentLocal;// 正在传输的位置

	public ReceiveFilesThread(int receiveFile_POST, Object synchronizeReceiveFile) {// 作为服务端
		this.receiveFile_POST = receiveFile_POST;
		this.ServiceOrClient = "Service";
		try {
			this.serverSocket = new ServerSocket(receiveFile_POST);// 只需在初始化时设定一次
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.synchronizeReceiveFile = synchronizeReceiveFile;
	}

	public ReceiveFilesThread(String host, int receiveFile_POST, Object synchronizeReceiveFile) {// 作为客户端
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
				System.out.println("等待客户端连接");
				socket = serverSocket.accept();// 在客户端未连接（发送数据）时产生中断（阻塞）
			}

			if (ServiceOrClient == "Client") {
				// 建立连接
				System.out.println("等待服务器...");
				socket = new Socket();// 在服务端未建立时产生中断（阻塞）
				SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
				socket.connect(socketAddress, 10000);// 10s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
				// 阻塞位置
				System.out.println("建立socket");
			}

			// 获取输入流，通过这个流读取消息
			DataInputStream input = new DataInputStream(socket.getInputStream());
			// 发送文字消息
			inputFile(input);

			synchronized (synchronizeReceiveFile) {
				synchronizeReceiveFile.notify();
			}

			input.close();
			socket.close();
			if (ServiceOrClient == "Service") {// 当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
				serverSocket.close();
			}
		} catch (SocketTimeoutException s) {// 接收服务器未建立时超时1s的异常处理
			System.out.println("发送客户端未建立 sendClient isn't established");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void inputFile(DataInputStream input) throws IOException {
		// 读文件数量
		int fileCount = input.readInt();
		
		totalFilesCount = fileCount;

		// 读文件名
		ArrayList<String> filesName = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int byteLength = input.readInt();
			byte[] bytes = new byte[byteLength];
			input.readFully(bytes);
			String str = new String(bytes, "UTF-8");
			filesName.add(str);
		}

		// 读文件大小
		ArrayList<Integer> filesLength = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int singleFileLength = input.readInt();
			filesLength.add(singleFileLength);
			totalFilesLength = totalFilesLength + singleFileLength;
		}

		// 读文件最后修改日期
		ArrayList<Long> fileLastModified = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			fileLastModified.add(input.readLong());// 输出文件的长度
		}

		// 读文件路径（用于回传时复原）
		ArrayList<String> filesPath = new ArrayList<>(0);
		for (int i = 0; i < fileCount; i++) {
			int byteLength = input.readInt();
			byte[] bytes = new byte[byteLength];
			input.readFully(bytes);
			String str = new String(bytes, "UTF-8");
			filesPath.add(str);
		}
		
		
		// 读文件
		for (int i = 0; i < fileCount; i++) {
			singleFileCount = i + 1;
			singleFileLength = filesLength.get(i);// 第i个文件的大小
			System.out.println("len = " + singleFileLength);
			System.out.println("文件名：" + filesName.get(i) + "第" + i + "个");
			// 为文件创建输出流，输出到文件
			File file = new File("p2pLocalTransmission接收区/" + filesName.get(i));// 已包含.png
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();// 创建父文件夹（们）
				// file.createNewFile();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(file);// 从程序输出到文件中

			singleCurrentLocal = 0;// 每读一个文件都清零一次
			while (singleCurrentLocal < filesLength.get(i) - 1024) {
				byte[] bytes = new byte[1024];// 1M1M的发
				input.readFully(bytes);// 读输入流1M，用readFully读取的大小确实是bytes的大小
				fileOutputStream.write(bytes, 0, 1024);// 写文件
				singleCurrentLocal = singleCurrentLocal + 1024;
				totalCurrentLocal = totalCurrentLocal + 1024;
			}
			byte[] bytes = new byte[filesLength.get(i) - singleCurrentLocal];// 剩余小于等于1024的部分
			input.readFully(bytes);// 读剩余小于等于1M的输入流
			fileOutputStream.write(bytes, 0, filesLength.get(i) - singleCurrentLocal);// 写剩余文件
			singleCurrentLocal = filesLength.get(i);
			totalCurrentLocal = totalCurrentLocal + bytes.length;
			
			fileOutputStream.close();// 关闭文件流，文件写入完成

			if (fileLastModified.get(i) != 0) {
				file.setLastModified(fileLastModified.get(i));// 修改文件的最后修改时间
			}
			System.out.println("ok");
		}

		// 储存路径信息
		FileOutputStream fileOutputStream = new FileOutputStream("p2pLocalTransmission路径存档.txt", true);// 在文件末尾写入
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		for (int i = 0; i < fileCount; i++) {
			bufferedWriter.write(filesName.get(i) + "\t" + filesPath.get(i));
			bufferedWriter.newLine();
		}

		bufferedWriter.close();
	}

	/**
	 * 获取单次传输中总的文件大小
	 * @return
	 */
	public int getTotalFilesLength() {
		return totalFilesLength;
	}
	
	/**
	 * 获取单次传输中总的文件个数
	 * @return
	 */
	public int getTotalFilesCount() {
		return totalFilesCount;
	}

	/**
	 * 获取单次传输中总的传输进度
	 * @return
	 */
	public int getTotalCurrentLocal() {
		return totalCurrentLocal;
	}
	
	/**
	 * 获取正在传输的某个文件的大小
	 * @return
	 */
	public int getSingleFileLength() {
		return singleFileLength;
	}

	/**
	 * 获取正在传输的某个文件是第几个文件
	 * @return
	 */
	public int getSingleFileCount() {
		return singleFileCount;
	}

	/**
	 * 获取正在传输的某个文件的传输进度
	 * @return
	 */
	public int getSingleCurrentLocal() {
		return singleCurrentLocal;
	}
}
