package com.blademaster.p2pLocalTransmission;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
/**
 * ������JScrollPaned����setViewportView�������Panel��壬Ȼ����Panel���Ļ����϶�̬��ӵ�ѡ��������
 * <p>ͨ�� {@link #setRadioButtons(String[])}�����������еİ�ť��
 * <p>ͨ�� {@link #setRadioButtonsKeepStatus(String[])}�������ͬʱ�����ᱣ����һ�ΰ������µ�״̬������ͬ���İ���ʱʹ�䰴�£���
 * <p>ͨ�� {@link #indexSelectedRadioButton()}�ɻ�ȡ��ǰ�����µİ���������������-1ʱ˵���ް��������»��ް������ڡ�
 * 
 * @see #setRadioButtons(String[])
 * @see #setRadioButtonsKeepStatus(String[])
 * @see #indexSelectedRadioButton()
 * 
 * @author blademaster
 *
 */
public class RadioButtonScrollPane extends JScrollPane{
	
	private JPanel jPanel;
	private JRadioButton[] jRadioButtons;
	
	public RadioButtonScrollPane() {
		// TODO �Զ����ɵĹ��캯�����
		
		//�½�Panel��壬���ڷ��ð�ť
		jPanel=new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));//ʹ�ô�ֱ�����Box����
		
		this.setBounds(10, 100, 300, 100);//�Ը����JScrollPane����λ�ã������ⲿ����RadioButtonScrollPane���setBounds����Ҳ˵���ǵ���this��setBounds��������ע��JFrame���ɾ��Բ��֣���Ȼ������Ч
		this.setViewportView(jPanel);//ΪJScrollPane���jPanel���(ע�ⲻҪ�á�Ԫ�����ࡱ��add��������������)
	}
	
	/**
	 * ��String���鴴����ť����ť�ĸ���ΪName�ĳ��ȣ�ÿ�����ú�ˢ��UI���൱�ڵ�һ�ν���set��
	 * @param name
	 */

	public void setRadioButtons(String[] name) {
		jPanel.removeAll();//ɾ������ԭ�����������Ϊ��������add������ӣ���remove�Ļ��ᱣ��ԭ�������
		
		jRadioButtons = new JRadioButton[name.length];// ���ɵ�ѡ����
		ButtonGroup BG = new ButtonGroup();// ButtonGroup�����������ֻ�ǹ���ť�Ļ����ϵ��һ���ࣨ��add������ӵ�ButtonGroup�İ�ť�����⣩��������ӵ�frame
		for (int i = 0; i < name.length; i++) {
			jRadioButtons[i] = new JRadioButton(name[i]);
			
			jPanel.add(jRadioButtons[i]);
			BG.add(jRadioButtons[i]);
		}
		
		jPanel.updateUI();//����panel��UI
	}
	
	/**
	 * ������ͬ���ֵİ�ť�����£����ְ���״̬����������һ��set���û�������Ӱ�죩
	 * @param name
	 */
	
	public void setRadioButtonsKeepStatus(String[] name) {
		jPanel.removeAll();//ɾ������ԭ�����������Ϊ��������add������ӣ���remove�Ļ��ᱣ��ԭ�������
		
		String str = null;
		if(indexSelectedRadioButton()!=-1) {
			str = jRadioButtons[indexSelectedRadioButton()].getText();//��ȡ֮ǰ��ѡ��İ�ť������
		}
		
		jRadioButtons = new JRadioButton[name.length];// ���ɵ�ѡ����
		ButtonGroup BG = new ButtonGroup();// ButtonGroup�����������ֻ�ǹ���ť�Ļ����ϵ��һ���ࣨ��add������ӵ�ButtonGroup�İ�ť�����⣩��������ӵ�frame
		for (int i = 0; i < name.length; i++) {
			jRadioButtons[i] = new JRadioButton(name[i]);
			
			jPanel.add(jRadioButtons[i]);
			BG.add(jRadioButtons[i]);
		}
		
		if (str != null) {
			for (int i = 0; i < name.length; i++) {// �����Ƿ���֮ǰ�����µ�ͬ������
				if (name[i].equals(str)) {
					jRadioButtons[i].setSelected(true);
				}
			}
		}
		
		jPanel.updateUI();//����panel��UI
	}
	
	/**
	 * ���ر����µİ�ť��������δ�ҵ�����RadioButtonʱ����-1
	 * @return
	 */

	public int indexSelectedRadioButton() {
		int index = -1;
		if (jRadioButtons != null) {
			for (int i = 0; i < jRadioButtons.length; i++) {// ��ԭ�������µİ�ť���в���
				if (jRadioButtons[i].isSelected()) {
					index = i;
				}
			}
		}
		return index;
	}
}
