package SP20_simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;


/**
 * �ùķ����ͷμ��� �۾��� ����Ѵ�. VisualSimulator���� ������� ��û�� ������ �̿� ����
 * ResourceManager�� �����Ͽ� �۾��� �����Ѵ�.  
 * 
 * �ۼ����� ���ǻ��� : <br>
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� ������ ��.<br>
 *  2) �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.<br>
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.<br>
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)<br>
 * 
 * <br><br>
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class SicSimulator {
	ResourceManager rMgr;
	int nextAddress = 0;
	InstLuncher ilchr;
	public boolean termination = false;
	private Instruction launchTarget;
	/*���� �� ù ���� �ּҴ� �ش� ������ �����ּҴ�.*/
	public Stack<Integer> sectionStartAddress;
	public String log;
	
	/*������ �޸��� �ּҿ� ���̸� ������ ������ �����.*/
	ArrayList<Integer[]> instructions;
	int counter = 0;	//instructions�� �ε�����, (=���� ����ǰ� �ִ� �ε�����)

	public SicSimulator(ResourceManager resourceManager) {
		// �ʿ��ϴٸ� �ʱ�ȭ ���� �߰�
		this.rMgr = resourceManager;
		instructions = new ArrayList<>();
		this.ilchr = new InstLuncher(resourceManager, this);
		this.launchTarget = new Instruction();
		sectionStartAddress = new Stack<>();
	}
	
	public void resetSicSimulator() {
		instructions.clear();
		counter = 0;
		log = "";
		nextAddress = 0;
		termination = false;
	}
	
	/**
	 * ��������, �޸� �ʱ�ȭ �� ���α׷� load�� ���õ� �۾� ����.
	 * ��, object code�� �޸� ���� �� �ؼ��� SicLoader���� �����ϵ��� �Ѵ�. 
	 */
	public void load(File program) {
		/* �޸� �ʱ�ȭ, �������� �ʱ�ȭ ��*/
		/*�޸� �ʱ�ȭ�� �������� �ʱ�ȭ�� ResourceManager�����ڷ� �ذ��.*/
	}
	
	public void load() {
		nextAddress = instructions.get(counter+1)[0];
		rMgr.setRegister(8, nextAddress);
		sectionStartAddress.push(instructions.get(0)[0]);
	}
	
	/**
	 * �ùķ��̼��� �ϱ� ���� �ʿ��� �޸����� ������ �߰��ϴ� �Լ�.
	 */
	public void addSimulationUnit(int location, int length) {
		this.instructions.add(new Integer[] {location, length});
	}
	
	/**
	 * UI�� instructions������ hex code�� ������ִ� �Լ�.(ó���� �ѹ� �����.)
	 */
	public ArrayList<String> instructionsToHex(){
		ArrayList<String> result = new ArrayList<>();
		int shifter = 0;
		for(Integer[] inst : instructions) {
			char[] temp = rMgr.getMemory(inst[0], inst[1]);
			int tempInt = 0;
			for(int i = temp.length-1; i>=0; i--) {
				tempInt += ((int)temp[i]) << shifter;
				shifter += 8;
			}
			result.add(String.format("%0"+(inst[1]*2)+"X", tempInt));
			shifter = 0;
		}
		return result;
	}
	
	/**
	 * instruction������ ���� ���� �޸𸮸� int������ ��ȯ���ִ� �Լ�.
	 */
	public int instructionToInt(int counter) {
		char[] temp = rMgr.getMemory(instructions.get(counter)[0], instructions.get(counter)[1]);
		int result = 0;
		int shifter = 0;
		for(int i = temp.length-1; i>=0; i--) {
			result += ((int)temp[i]) << shifter;
			shifter += 8;
		}
		return result;
	}
	
	/**
	 * 1���� instruction�� ����� ����� ���δ�. 
	 */
	public void oneStep() {
		if(counter >= instructions.size() ) return;
		
		if(counter == 6) {
			System.out.println("");
		}
		
		int currentCode = instructionToInt(counter);
		char masked = 0;
		/*4����*/
		if(instructions.get(counter)[1] == 4) {
			launchTarget.type = 4;
			int shifter = 0;
			char[] tPlace = new char[3];
			char nixbpe = 0;
			
			/*displacementó��*/
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			tPlace[2] = masked;
			shifter+=8;
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			tPlace[1] = masked;
			shifter+=8;
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			masked = (char)((masked & (15 << shifter))>>>shifter);
			tPlace[0] = masked;
			launchTarget.displacement = tPlace;
			launchTarget.displacementInt = currentCode & 1048575;
			
			/*nixbpeó��*/
			masked = (char)((currentCode & (240 << shifter))>>>shifter);
			masked = (char)(masked >> 4);
			nixbpe = masked;
			shifter+=8;
			/*�ٷ� ���� �� �ְ� ������ shift�� 4�� ����.*/
			masked = (char)((currentCode & (3 << shifter))>>>shifter-4);
			nixbpe += masked;
			launchTarget.nixbpe = nixbpe;
			
			/*op*/
			masked = (char)((currentCode & (252 << shifter))>>>shifter);
			launchTarget.op = masked;
		}
		/*3����*/
		else if(instructions.get(counter)[1] == 3) {
			launchTarget.type = 3;
			int shifter = 0;
			char[] tPlace = new char[2];
			char nixbpe = 0;
			
			/*displacementó��*/
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			tPlace[1] = masked;
			shifter+=8;
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			masked = (char)((masked & (15 << shifter))>>>shifter);
			tPlace[0] = masked;
			launchTarget.displacement = tPlace;
			launchTarget.displacementInt = currentCode & 4095;
			
			/*nixbpeó��*/
			masked = (char)((currentCode & (240 << shifter))>>>shifter);
			masked = (char)(masked >> 4);
			nixbpe = masked;
			shifter+=8;
			/*�ٷ� ���� �� �ְ� ������ shift�� 4�� ����.*/
			masked = (char)((currentCode & (3 << shifter))>>>shifter-4);
			nixbpe += masked;
			launchTarget.nixbpe = nixbpe;
			
			/*op*/
			masked = (char)((currentCode & (252 << shifter))>>>shifter);
			launchTarget.op = masked;
		}
		/*2����*/
		else if(instructions.get(counter)[1] == 2) {
			launchTarget.type = 2;
			/*�������� ó��*/
			int shifter = 0;
			masked = (char)((currentCode & (15 << shifter)) >>> shifter);
			launchTarget.register2 = masked;
			shifter+=4;
			masked = (char)((currentCode & (15 << shifter)) >>> shifter);
			launchTarget.register1 = masked;
			shifter+=4;
			
			/*opó��*/
			masked = (char)((currentCode & (255 << shifter)) >>> shifter);
			launchTarget.op = masked;
		}
		/*1����*/
		else if(instructions.get(counter)[1] == 1) {
			launchTarget.type = 1;
			launchTarget.op = (char)currentCode;
		}
		String instLog = ilchr.launchInst(launchTarget); 
		addLog(instLog);
		
		/*Jump�� ���õ� ��ɾ ������ ��� counter�� �����ش�.*/
		if(instLog.equals("JSUB") || instLog.equals("J BACKWARD") || instLog.equals("RSUB") 
				|| instLog.equals("JLT LOOP IN") || instLog.equals("JEQ")) {
			syncCounter();
			/*������ ���� �Ѿ�� �ʰ� ������ �ؾ��ϹǷ� counter�� �ǵ帮�� �ʴ´�.*/
			if(instLog.equals("JSUB")) {
				int pcRegister = rMgr.getRegister(8);
				for(int i = 0; i < instructions.size(); i++) {
					if(instructions.get(i)[0] == pcRegister) sectionStartAddress.push(instructions.get(i)[0]);
				}
			}
			/*RSUB�� PC register�� �����ߴ� ������ ���ư��� ���̱� ������ ���� counter�� �ǵ帮�� �ʴ´�.*/
			else if(instLog.equals("RSUB")) {
				if(sectionStartAddress.size()>=2) sectionStartAddress.pop();
			}
		}
		else counter+=1;
		/*
		 * 0��� endRecord�� ���� ���� �� ������ endRecord�� loader�� �ִ� ���¶� ���� ���������ߵȴ�.
		 * Ȯ�������� ���� ��Ȳ���� ���ʿ��� ������ �����Ѵ�.
		 */
		/*if(counter+1<instructions.size())*/
		nextAddress = instructions.get(counter)[0]+instructions.get(counter)[1];
		rMgr.setRegister(8, nextAddress);
		
		/*����*/
		if(instLog.equals("J BACKWARD")||instLog.equals("J FOREWARD")||instLog.equals("J")) {
			if(ilchr.targetAddress == instructions.get(0)[0]) {
				termination = true;
				addLog("\nTermination");
				rMgr.setRegister(8, ilchr.targetAddress);
			}
		}
	}
	
	/**
	 * Jump�� ���õ� ��ɾ �������� ��� PC register�� �ùķ������� �������� counter�� �����ֱ� ���� �Լ�.
	 */
	public void syncCounter() {
		int pcRegister = rMgr.getRegister(8);
		for(int i = 0; i < instructions.size(); i++) {
			if(instructions.get(i)[0] == pcRegister) {
				counter = i;
			}
		}
	}
	
	/**
	 * ���� ��� instruction�� ����� ����� ���δ�.
	 */
	public void allStep() {
		while(!termination) {
			oneStep();
		}
	}
	
	/**
	 * �� �ܰ踦 ������ �� ���� ���õ� ����� ���⵵�� �Ѵ�.
	 */
	public void addLog(String log) {
		if(this.log != null) this.log += "\n"+log;
		else this.log = log;
	}
}

class Instruction{
	int type;
	char op;
	char nixbpe;
	/*Appendix���� 4������ address��� ��Ī�� ����ϰ� ������ ���⼭�� 3,4���� ��� displacement��� ����Ѵ�.*/
	char[] displacement;
	int displacementInt;
	char register1;
	char register2;
}
