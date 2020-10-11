package com.blademaster.p2pLocalTransmission;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import com.blademaster.p2pLocalTransmission.netManager.HotspotManager;
import com.blademaster.p2pLocalTransmission.netManager.WiFiManager;
import java.awt.SystemColor;
import javax.swing.JProgressBar;

public class Main extends JFrame {
	private int message_POST = 30001;
	private int file_POST = 30002;
	private String host = null;

	private Object synchronizeReceiveMessage = new Object();// ͬ�����ֽ���
	private Object synchronizeSendFile = new Object();// ͬ���ļ�����
	private Object synchronizeReceiveFile = new Object();// ͬ���ļ�����

	private ArrayList<String> globalHostName = new ArrayList<String>();
	private ArrayList<String> globalConnecterIP = new ArrayList<String>();
	private ArrayList<String> globalMyIP = new ArrayList<String>();

	java.util.List<File> files;// ������϶��ļ�ץȡ��File����
	private JTextField textField_information;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();// ��ĳ�ʼ������frame
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Create the frame.
	 */
	public Main() {
		// ********************UI�ؼ�������***********************
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 437, 256);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// �������ڴ洢��ѡ�ؼ��Ķ���
		RadioButtonScrollPane radioButtonScrollPane = new RadioButtonScrollPane();
		radioButtonScrollPane.setBounds(34, 80, 300, 70);
		contentPane.add(radioButtonScrollPane);

		JTextArea textArea_send = new JTextArea();
		textArea_send.setDragEnabled(true);
//		textArea_send.setDropMode(DropMode.INSERT);
//		DropTarget dropTarget = new DropTarget(textArea_send, new DropTargetListener() {
//			
//			@Override
//			public void dropActionChanged(DropTargetDragEvent dtde) {
//				// TODO �Զ����ɵķ������
//				
//			}
//			
//			@Override
//			public void drop(DropTargetDropEvent dtde) {
//				// TODO �Զ����ɵķ������
//				Transferable data = dtde.getTransferable();
//		        if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//		            try {
//						java.util.List<File> list = (java.util.List<File>) data.getTransferData(
//						    DataFlavor.javaFileListFlavor);
//						System.err.println(list.size());
//					} catch (UnsupportedFlavorException | IOException e) {
//						// TODO �Զ����ɵ� catch ��
//						e.printStackTrace();
//					}
//		           
//		        }
//				textArea_send.setText(String.valueOf(dtde.getTransferable()));
//			}
//			
//			@Override
//			public void dragOver(DropTargetDragEvent dtde) {
//				// TODO �Զ����ɵķ������
//				
//			}
//			
//			@Override
//			public void dragExit(DropTargetEvent dte) {
//				// TODO �Զ����ɵķ������
//				
//			}
//			
//			@Override
//			public void dragEnter(DropTargetDragEvent dtde) {
//				// TODO �Զ����ɵķ������
//				
//			}
//		});
//		textArea_send.setDropTarget(dropTarget);
//		dropTarget.
		
		textArea_send.setEditable(false);
		textArea_send.setBounds(34, 15, 292, 44);
		textArea_send.setText("�뽫�ļ��϶����˴�");
		textArea_send.setColumns(10);

		JScrollPane jScrollPane = new JScrollPane(textArea_send);// �൱��new
																	// JScrollPane()��jScrollPane.setViewportView(textArea_send);
		jScrollPane.setBounds(34, 15, 292, 44);
		// jScrollPane.setViewportView(textArea_send);
		contentPane.add(jScrollPane);

		textArea_send.setTransferHandler(new TransferHandler() {// �����ļ��϶���ȡ·��
			@Override
			public boolean importData(JComponent comp, Transferable t) {
				// TODO �Զ����ɵķ������
				try {
					files = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);// ֻ��List���Ϳ��Գɹ�ǿ��ת���ļ���

					String string = "";
					for (File f : files) {
						string = string + f.getPath() + "\n";
					}
					textArea_send.setText(string);

					return true;
				} catch (Exception e) {

				}
				return false;
			}

			@Override
			public boolean canImport(JComponent comp, DataFlavor[] flavors) {
				for (int i = 0; i < flavors.length; i++) {
					if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
						return true;
					}
				}
				return false;
			}
		});

		JButton send_button = new JButton("����");
		send_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO �Զ����ɵķ������
				if (radioButtonScrollPane.indexSelectedRadioButton() != -1) {
					System.out.println(globalConnecterIP.size());
					host = globalConnecterIP.get(radioButtonScrollPane.indexSelectedRadioButton());

					SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);
					sendMessageThread.setMessage("�������ļ�");
					sendMessageThread.start();

					// ���������ļ��߳�
					SendFilesThread sendFilesThread = new SendFilesThread(host, file_POST, synchronizeSendFile);
					ArrayList<String> filesPath = new ArrayList<>();
					for (File f : files) {
						filesPath.add(f.getPath());
					}
					// filesPath.add(textArea_send.getText());
					sendFilesThread.setFilePath(filesPath);
					sendFilesThread.start();

					new Thread(new Runnable() {// ���ļ����ڷ���ʱ
						@Override
						public void run() {
							while (sendFilesThread.getCurrentLocal() == 0
									|| sendFilesThread.getFile_length() > sendFilesThread.getCurrentLocal()) {
								textField_information.setText("���ڷ��ͣ�" + sendFilesThread.getCurrentLocal() + "/"
										+ sendFilesThread.getFile_length());
							}
							textField_information.setText("");
						}
					}).start();
				}
			}
		});
		send_button.setBounds(336, 27, 80, 23);
		contentPane.add(send_button);

		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setBackground(SystemColor.control);
		textArea.setText("\u4F20\u8F93\u72B6\u6001\uFF1A");
		textArea.setBounds(7, 185, 60, 24);
		contentPane.add(textArea);

		JTextArea txtip = new JTextArea();
		txtip.setText("����״̬��   �Է�ip                  ����ip                �Է�����");
		txtip.setColumns(10);
		txtip.setBounds(7, 60, 305, 23);
		txtip.setBackground(null);
		txtip.setEditable(false);

		contentPane.add(txtip);
		
		textField_information = new JTextField();
		textField_information.setEditable(false);
		textField_information.setBounds(311, 187, 80, 21);
		contentPane.add(textField_information);
		textField_information.setColumns(10);
		
		JPanel panel = new JPanel();
		panel.setBounds(71, 155, 209, 62);
		contentPane.add(panel);
		panel.setLayout(null);
		
		JProgressBar progressBar_single = new JProgressBar();
		progressBar_single.setBounds(0, 21, 209, 17);
		panel.add(progressBar_single);
		
		JProgressBar progressBar_total = new JProgressBar();
		progressBar_total.setBounds(0, 45, 209, 17);
		panel.add(progressBar_total);
		
		JTextArea textArea_filesStatus = new JTextArea();
		textArea_filesStatus.setBounds(169, 0, 46, 23);
		panel.add(textArea_filesStatus);
		textArea_filesStatus.setBackground(SystemColor.control);
		textArea_filesStatus.setText("0/0");
		textArea_filesStatus.setEditable(false);
		
		JTextArea textArea_remainTime = new JTextArea();
		textArea_remainTime.setBackground(SystemColor.control);
		textArea_remainTime.setBounds(296, 160, 101, 24);
		contentPane.add(textArea_remainTime);

		// *******************�̴߳�����*********************
		// ��ȡhost�߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					ArrayList<String> hostName = new ArrayList<String>();
					ArrayList<String> connecterIP = new ArrayList<String>();
					ArrayList<String> myIP = new ArrayList<String>();

					HotspotManager hotspotManager = new HotspotManager();
					if (hotspotManager.getConnecterIP().size() > 0) {// ������ipʱ
						hostName = hotspotManager.getConnecterName();
						connecterIP = hotspotManager.getConnecterIP();
						myIP = hotspotManager.getMyIP();
						// host=hotspotManager.getConnecterIP().get(0);
					}

					WiFiManager wiFiManager = new WiFiManager();
					if (wiFiManager.isWiFiConnected()) {
						hostName.add(wiFiManager.getConnectingWiFiSSID());
						connecterIP.add(wiFiManager.getConnecterIP());
						myIP.add(wiFiManager.getMyIP());
						// host=wiFiManager.getConnecterIP();
					}

					// ���ݻ�õ���Ϣ�ػ�RadioButtonScrollPane
					String[] radioButtonName = new String[hostName.size()];
					for (int i = 0; i < hostName.size(); i++) {
						radioButtonName[i] = connecterIP.get(i) + "   " + myIP.get(i) + "   " + hostName.get(i);
					}

					// ������״̬�����ı䣬�޸�ȫ�ֵ�����
					if (!globalHostName.equals(hostName) || !globalConnecterIP.equals(connecterIP)
							|| !globalMyIP.equals(myIP)) {
						globalHostName = hostName;
						globalConnecterIP = connecterIP;
						globalMyIP = myIP;
					}

					radioButtonScrollPane.setRadioButtonsKeepStatus(radioButtonName);
					// System.out.println(host);

					// �߳�˯��
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO �Զ����ɵ� catch ��
						e.printStackTrace();
					}
				}
			}
		}).start();

		ReceiveMessageLoopThread receiveMessageThread = new ReceiveMessageLoopThread(message_POST,
				synchronizeReceiveMessage);
		receiveMessageThread.start();

		// **************************ͬ���߳���**************************

		// ����ͬ��������Ϣ�̵߳��̣߳������߳̽��յ���Ϣ��֪ͨ���߳�ִ����Ӧ�Ĳ�����
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO �Զ����ɵķ������

				while (true) {// ע������synchronized�������ܳ�����ͬһ���߳��ڣ���Ȼ���ǻụ�����������ܶ�������
					synchronized (synchronizeReceiveMessage) {// ����������Ϣͬ����
						try {
							synchronizeReceiveMessage.wait();// ����

							// �����ļ�����
							String message = receiveMessageThread.getMessage();
							host = receiveMessageThread.getConnecterIP();// ���ڻظ����ɹ������ļ���

							// �����ļ�����
							if (message.equals("�������ļ�")) {// ���յ��Է�"�������ļ�"ʱ��׼�������ļ�

								textField_information.setText("��ʼ�����ļ�");
								// �ȴ�����
								ReceiveFilesThread receiveFilesThread = new ReceiveFilesThread(file_POST,
										synchronizeReceiveFile);
								receiveFilesThread.start();

								new Thread(new Runnable() {// ���ļ����ڽ���ʱ
									@Override
									public void run() {
										int timeout = 5000;//��ʱ5��
										while(--timeout != 0) {
											if(receiveFilesThread.getTotalCurrentLocal() != 0 && receiveFilesThread.getTotalFilesLength() != 0)
												break;
											else {
												try {
													Thread.sleep(1);
												} catch (InterruptedException e) {
													// TODO �Զ����ɵ� catch ��
													e.printStackTrace();
												}
											}
										}
										if(timeout == 0) {
											textField_information.setText("�����ļ���ʱ");
											return;//�˳�
										}
										Calendar start_calendar = Calendar.getInstance();
										//��ʼ�����ļ�
										while (receiveFilesThread.getTotalCurrentLocal() != receiveFilesThread.getTotalFilesLength()) {
											textArea_filesStatus.setText(receiveFilesThread.getSingleFileCount() + "/" + receiveFilesThread.getTotalFilesCount());//���ڷ��͵��ļ����ļ�����
											//�����ļ�����
											progressBar_single.setStringPainted(true);
											progressBar_single.setMaximum(receiveFilesThread.getSingleFileLength());
											progressBar_single.setValue(receiveFilesThread.getSingleCurrentLocal());
											//�������
											progressBar_total.setStringPainted(true);
											progressBar_total.setMaximum(receiveFilesThread.getTotalFilesLength());
											progressBar_total.setValue(receiveFilesThread.getTotalCurrentLocal());
											//ʣ��ʱ��
											textArea_remainTime.setText("ʣ��ʱ�䣺" + remainTimeCalculate(start_calendar, receiveFilesThread.getTotalCurrentLocal(), receiveFilesThread.getTotalFilesLength()));
										}
										//�������
										//progressBar_single.setString("�ɹ�����");
										//textField_information.setText("");
									}
								}).start();

							}
							// �����ļ�����
							if (message.equals("�ɹ������ļ�")) {// ���յ��Է�"�ɹ������ļ�"ʱ����ʾ�ɹ�����
								textField_information.setText("�ɹ������ļ�");
							}

						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��0
							e.printStackTrace();
						}
					}
				}
			}
		}).start();

		// ����ͬ�������ļ��̵߳��߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					synchronized (synchronizeSendFile) {// �����ļ�ͬ����
						try {
							synchronizeSendFile.wait();
							// ��ʼ�����ļ���
							textField_information.setText("��ʼ�����ļ�");
						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��
							e.printStackTrace();
						}
					}
				}
			}
		}).start();

		// ����ͬ�������ļ��̵߳��߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					synchronized (synchronizeReceiveFile) {// �����ļ�ͬ����
						try {
							synchronizeReceiveFile.wait();
							// ���յ��ļ���
							textField_information.setText("�ɹ������ļ�");
							SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);// hostӦ��ǰ��Ľ����̻߳�ȡ��Ϊ��������Ϣ��������ip��
							sendMessageThread.setMessage("�ɹ������ļ�");
							sendMessageThread.start();
						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}
	private String remainTimeCalculate(Calendar start_calendar, int totalCurrentLocal, int totalFilesLength) {
		Calendar calendar = Calendar.getInstance();
		long costTimeInMill = calendar.getTimeInMillis() - start_calendar.getTimeInMillis();//�Ѿ����ĵ�ʱ��
		long remainTimeInMill = (totalFilesLength - totalCurrentLocal) * costTimeInMill / totalCurrentLocal;
		calendar.setTimeInMillis(remainTimeInMill);
		String remainMin = Integer.valueOf(calendar.get(Calendar.MINUTE)).toString();
		String remainSec = Integer.valueOf(calendar.get(Calendar.SECOND)).toString();
		return "0".repeat(2 - remainMin.length()) + remainMin + ":" + "0".repeat(2 - remainSec.length()) + remainSec;
	}
}
