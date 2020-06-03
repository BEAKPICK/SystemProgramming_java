package SP20_simulator;

import java.awt.BorderLayout;


import java.awt.Dimension;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다.<br>
 * 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트 하는 역할을 수행한다.<br>
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator {
	ResourceManager resourceManager = new ResourceManager();
	SicLoader sicLoader = new SicLoader(resourceManager);
	SicSimulator sicSimulator = new SicSimulator(resourceManager);
	
	private boolean init = true;
	
	JTextField programName;
	JTextField opStartAddress;
	JTextField programSize;
	
	JTextField[] registers;
	JTextField endRecord;
	JTextField startAddressInMemory;
	JTextField targetAddress;
	
	JList<String> instructions;
	DefaultListModel<String> instructionListModel;
	JTextField usingDevice;
	JTextArea log;
	
	private void createWindow() {    
	      JFrame frame = new JFrame("SIC/XE Simulator");
	      
	      /*file open ui*/
	      JButton open = new JButton("open");
	      JTextField tf1 = new JTextField(20);
	      open.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent e) {
	    		  loadFile(tf1.getText());
	    		  update();
	    	  }
	      });
	      JLabel label1 = new JLabel("FileName : ");
	      
	      JPanel panel1 = new JPanel();
	      panel1.setLayout(new GridBagLayout());
	      panel1.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
	      panel1.add(label1);
	      panel1.add(tf1);
	      panel1.add(open);
	      
	      /*H (Header Record)*/
	      JLabel label2 = new JLabel("Program name : ");
	      JLabel label3 = new JLabel("Start Address of Object Program : ");
	      JLabel label4 = new JLabel("Length of Program : ");
	      
	      JTextField tf2 = new JTextField(5);
	      programName = tf2;
	      JTextField tf3 = new JTextField(5);
	      opStartAddress = tf3;
	      JTextField tf4 = new JTextField(5);
	      programSize = tf4;
	      tf2.setEditable(false);
	      tf3.setEditable(false);
	      tf4.setEditable(false);
	      
	      JPanel panel2 = new JPanel();
	      panel2.setLayout(new GridLayout(3,2));
	      panel2.setBorder(BorderFactory.createEtchedBorder());
	      panel2.add(label2);
	      panel2.add(tf2);
	      panel2.add(label3);
	      panel2.add(tf3);
	      panel2.add(label4);
	      panel2.add(tf4);
	      
	      /*E (End Record)*/
	      JLabel label5 = new JLabel("<html>Address of First instruction<br> in Object Program (End Record): <html>");
	      JTextField tf5 = new JTextField(10);
	      endRecord = tf5;
	      tf5.setEditable(false);
	      
	      JPanel panel3 = new JPanel();
	      panel3.setBorder(BorderFactory.createEtchedBorder());
	      panel3.setLayout(new GridLayout(1,2));
	      panel3.add(label5);
	      panel3.add(tf5);
	      
	      /*Start Address in Memory*/
	      JLabel label6 = new JLabel("Start Address in Memory : ");
	      JTextField tf6 = new JTextField(5);
	      startAddressInMemory = tf6;
	      tf6.setEditable(false);
	      
	      JPanel panel4 = new JPanel();
	      panel4.setBorder(BorderFactory.createEtchedBorder());
	      panel4.setLayout(new GridLayout(1,2));
	      panel4.add(label6);
	      panel4.add(tf6);
	      
	      /*Register*/
	      JLabel label7 = new JLabel("A(#0)");
	      JLabel label8 = new JLabel("X(#1)");
	      JLabel label9 = new JLabel("L(#2)");
	      JLabel label10 = new JLabel("B(#3)");
	      JLabel label11 = new JLabel("S(#4)");
	      JLabel label12 = new JLabel("T(#5)");
	      JLabel label13 = new JLabel("F(#6)");
	      JLabel label14 = new JLabel("PC(#8)");
	      JLabel label15 = new JLabel("SW(#9)");
	      
	      JLabel label16 = new JLabel("DEC");
	      JLabel label17 = new JLabel("HEX");
	      JLabel blank = new JLabel("");
	      
	      JTextField[] tfArr = new JTextField[16];
	      registers = tfArr;
	      for(int i = 0; i<tfArr.length; i++) {
	    	  tfArr[i] = new JTextField(5);
	    	  tfArr[i].setEditable(false);
	      }
	      
	      JPanel panel5 = new JPanel();
	      panel5.setBorder(BorderFactory.createEtchedBorder());
	      panel5.setLayout(new GridBagLayout());
	      GridBagConstraints c =new GridBagConstraints();
	      c.fill = GridBagConstraints.HORIZONTAL;
	      
	      c.gridwidth =1;
	      c.gridx = 0;
	      c.weightx = 0.8;
	      panel5.add(blank, c);
	      panel5.add(label7, c);
	      panel5.add(label8, c);
	      panel5.add(label9, c);
	      panel5.add(label10, c);
	      panel5.add(label11, c);
	      panel5.add(label12, c);
	      panel5.add(label13, c);
	      panel5.add(label14, c);
	      panel5.add(label15, c);
	      
	      c.gridx = 1;
	      panel5.add(label16, c);
	      panel5.add(tfArr[0], c);
	      panel5.add(tfArr[2], c);
	      panel5.add(tfArr[4], c);
	      panel5.add(tfArr[6], c);
	      panel5.add(tfArr[8], c);
	      panel5.add(tfArr[10], c);
	      c.gridwidth = 2;
	      panel5.add(tfArr[12], c);
	      c.gridwidth = 1;
	      panel5.add(tfArr[13], c);
	      c.gridwidth = 2;
	      panel5.add(tfArr[15], c);
	      c.gridwidth = 1;
	      
	      c.gridx = 2;
	      c.gridy = 0;
	      panel5.add(label17, c);
	      c.gridy++;
	      panel5.add(tfArr[1], c);
	      c.gridy++;
	      panel5.add(tfArr[3], c);
	      c.gridy++;
	      panel5.add(tfArr[5], c);
	      c.gridy++;
	      panel5.add(tfArr[7], c);
	      c.gridy++;
	      panel5.add(tfArr[9], c);
	      c.gridy++;
	      panel5.add(tfArr[11], c);
	      c.gridy+=2;
	      panel5.add(tfArr[14], c);

	      /*Target Address*/
	      JLabel label18 = new JLabel("Target Address : ");
	      JTextField tf7 = new JTextField(5);
	      targetAddress = tf7;
	      tf7.setEditable(false);
	      
	      JPanel panel6 = new JPanel();
	      panel6.setBorder(BorderFactory.createEtchedBorder());
	      panel6.setLayout(new GridLayout(1,2));
	      panel6.add(label18);
	      panel6.add(tf7);
	      
	      /*instructions*/
	      JPanel panel9 = new JPanel();
	      panel9.setBorder(BorderFactory.createEtchedBorder());
	      panel9.setLayout(new GridLayout(1,2));
	      
	      /**/
	      JLabel label19 = new JLabel("Instructions : ");
	      DefaultListModel<String> listModel = new DefaultListModel<String>();
	      instructionListModel = listModel;
	      JList<String> codeList = new JList<String>(listModel);
	      instructions = codeList;
	      codeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	      codeList.setLayoutOrientation(JList.VERTICAL);
	      JScrollPane scroll1 = new JScrollPane();
	      scroll1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	      scroll1.setViewportView(codeList);
	      
	      JPanel panel7 = new JPanel();
	      panel7.setLayout(new BoxLayout(panel7, BoxLayout.Y_AXIS));
	      panel7.add(label19);
	      panel7.add(scroll1);
	      
	      /**/
	      JLabel label20 = new JLabel("Using Device");
	      JTextField tf8 = new JTextField(5);
	      usingDevice = tf8;
	      tf8.setEditable(false);
	      JButton btn1Step = new JButton("Run(1step)");
	      btn1Step.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent e) {
	    		  oneStep();
	    	  }
	      });
	      JButton btnAll = new JButton("Run(All)");
	      btnAll.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent e) {
	    		  allStep();
	    	  }
	      });
	      JButton btnClose = new JButton("Close");
	      btnClose.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent e) {
	    		  close();
	    	  }
	      });
	      JPanel panel8 = new JPanel();
	      panel8.setLayout(new BoxLayout(panel8, BoxLayout.Y_AXIS));
	      panel8.add(label20);
	      panel8.add(tf8);
	      panel8.add(btn1Step);
	      panel8.add(btnAll);
	      panel8.add(btnClose);
	      
	      panel9.add(panel7);
	      panel9.add(panel8);
	      
	      /*log*/
	      JLabel label21 = new JLabel("Log(Instuction)");
	      JTextArea ta2 = new JTextArea(10,1);
	      log = ta2;
	      JScrollPane scroll2 = new JScrollPane(ta2);
	      scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	      ta2.setEditable(false);
	      scroll2.setViewportView(ta2);
	      
	      JPanel panel11 = new JPanel();
	      panel11.setBorder(BorderFactory.createEtchedBorder());
	      panel11.setLayout(new BoxLayout(panel11, BoxLayout.Y_AXIS));
	      panel11.add(label21);
	      panel11.add(scroll2);
	      
	      /*assemble and arrange UI*/
	      frame.add(panel1, BorderLayout.PAGE_START);
	      
	      JPanel mid = new JPanel();
	      GridLayout gl = new GridLayout(1,2);
	      mid.setLayout(gl);
	      
	      FlowLayout fl = new FlowLayout();
	      
	      JPanel midLeft = new JPanel();
	      midLeft.setLayout(new BoxLayout(midLeft, BoxLayout.Y_AXIS));
	      
	      JPanel midRight = new JPanel();
	      midRight.setLayout(new BoxLayout(midRight, BoxLayout.Y_AXIS));
	      
	      midLeft.add(panel2, fl);
	      midLeft.add(panel5, fl);
	      midRight.add(panel3, fl);
	      midRight.add(panel4, fl);
	      midRight.add(panel6, fl);
	      midRight.add(panel9, fl);

	      mid.add(midLeft, BorderLayout.CENTER);
	      mid.add(midRight, BorderLayout.CENTER);
	      
	      frame.add(mid, BorderLayout.CENTER);
	      
	      frame.add(panel11, BorderLayout.PAGE_END);
	      
	      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	      frame.setPreferredSize(new Dimension(820, 700));
	      frame.pack();
	      frame.setVisible(true);
	      
	   }
	/**
	 * 파일을 로드한다.
	 * @param path
	 */
	public void loadFile(String fileName) {
		File file = new File(fileName);
		load(file);
	}
	
	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	public void load(File program){
		//...
		sicLoader.load(program);
		sicSimulator.load();
		update();
	};

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep(){
		sicSimulator.oneStep();
		update();
	};

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep(){
		if(sicLoader.objectCode == null) return;
		sicSimulator.allStep();
		update();
	};
	
	/**
	 * 파일을 닫고 리소스들을 초기화한다.
	 */
	public void close() {
		resourceManager.initializeResource();
		sicSimulator.resetSicSimulator();
		sicLoader.resetSicLoader();
		instructionListModel.clear();
		programName.setText("");
		programSize.setText("");
		opStartAddress.setText("");
		endRecord.setText("");
		startAddressInMemory.setText("");
		log.setText(sicSimulator.log);
		targetAddress.setText("");
		usingDevice.setText("");
		
		for(int i = 0; i < registers.length; i++) registers[i].setText("");
		init = true;
	}
	
	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	public void update(){
		if(sicLoader.objectCode == null) return;
		if(init) {
			for(String str : sicSimulator.instructionsToHex()) {
				instructionListModel.addElement(str);
			}
			programName.setText(sicLoader.programName);
			programSize.setText(String.format("%X", resourceManager.reserved));
			opStartAddress.setText(Integer.toString(sicLoader.startAddress));
			endRecord.setText(sicLoader.endRecord);
			startAddressInMemory.setText(String.format("%X", resourceManager.memoryStartAddress));
			init = false;
		}
		instructions.setSelectedIndex(sicSimulator.counter);
		targetAddress.setText(String.format("%X",sicSimulator.ilchr.targetAddress));
		
		/*resourceManager의 register는 10개인데 왜 예시화면에 register는 9개일까? 7번 레지스터는 왜 없을까? 하...*/
		registers[0].setText(Integer.toString(resourceManager.register[0]));
		registers[1].setText(String.format("%06X", resourceManager.register[0]));
		registers[2].setText(Integer.toString(resourceManager.register[1]));
		registers[3].setText(String.format("%06X", resourceManager.register[1]));
		registers[4].setText(Integer.toString(resourceManager.register[2]));
		registers[5].setText(String.format("%06X", resourceManager.register[2]));
		registers[6].setText(Integer.toString(resourceManager.register[3]));
		registers[7].setText(String.format("%06X", resourceManager.register[3]));
		registers[8].setText(Integer.toString(resourceManager.register[4]));
		registers[9].setText(String.format("%06X", resourceManager.register[4]));
		registers[10].setText(Integer.toString(resourceManager.register[5]));
		registers[11].setText(String.format("%06X", resourceManager.register[5]));
		registers[12].setText(String.format("%06X", resourceManager.register[6]));
		registers[13].setText(Integer.toString(resourceManager.register[8]));
		registers[14].setText(String.format("%06X", resourceManager.register[8]));
		registers[15].setText(String.format("%06X", resourceManager.register[9]));
		
		usingDevice.setText(resourceManager.usingDevice);
		log.setText(sicSimulator.log);
		if(sicSimulator.termination) sicLoader.objectCode = null;
	};
	

	public static void main(String[] args) {
		VisualSimulator vs = new VisualSimulator();
		/*시뮬레이터를 로더에 연결시켜준다.*/
		vs.sicLoader.connectSimulator(vs.sicSimulator);
		vs.createWindow();
	}
}
