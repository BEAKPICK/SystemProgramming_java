package SP20_simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;


/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라
 * ResourceManager에 접근하여 작업을 수행한다.  
 * 
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것.<br>
 *  2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 * 
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {
	ResourceManager rMgr;
	int nextAddress = 0;
	InstLuncher ilchr;
	public boolean termination = false;
	private Instruction launchTarget;
	/*점프 후 첫 시작 주소는 해당 섹션의 시작주소다.*/
	public Stack<Integer> sectionStartAddress;
	public String log;
	
	/*실행할 메모리의 주소와 길이를 저장할 공간을 만든다.*/
	ArrayList<Integer[]> instructions;
	int counter = 0;	//instructions의 인덱스값, (=현재 실행되고 있는 인덱스값)

	public SicSimulator(ResourceManager resourceManager) {
		// 필요하다면 초기화 과정 추가
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
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행.
	 * 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다. 
	 */
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등*/
		/*메모리 초기화와 레지스터 초기화는 ResourceManager생성자로 해결됨.*/
	}
	
	public void load() {
		nextAddress = instructions.get(counter+1)[0];
		rMgr.setRegister(8, nextAddress);
		sectionStartAddress.push(instructions.get(0)[0]);
	}
	
	/**
	 * 시뮬레이션을 하기 위해 필요한 메모리접근 정보를 추가하는 함수.
	 */
	public void addSimulationUnit(int location, int length) {
		this.instructions.add(new Integer[] {location, length});
	}
	
	/**
	 * UI에 instructions정보를 hex code로 출력해주는 함수.(처음에 한번 실행됨.)
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
	 * instruction정보를 통해 읽은 메모리를 int형으로 변환해주는 함수.
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
	 * 1개의 instruction이 수행된 모습을 보인다. 
	 */
	public void oneStep() {
		if(counter >= instructions.size() ) return;
		
		if(counter == 6) {
			System.out.println("");
		}
		
		int currentCode = instructionToInt(counter);
		char masked = 0;
		/*4형식*/
		if(instructions.get(counter)[1] == 4) {
			launchTarget.type = 4;
			int shifter = 0;
			char[] tPlace = new char[3];
			char nixbpe = 0;
			
			/*displacement처리*/
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
			
			/*nixbpe처리*/
			masked = (char)((currentCode & (240 << shifter))>>>shifter);
			masked = (char)(masked >> 4);
			nixbpe = masked;
			shifter+=8;
			/*바로 더할 수 있게 마지막 shift를 4번 뺀다.*/
			masked = (char)((currentCode & (3 << shifter))>>>shifter-4);
			nixbpe += masked;
			launchTarget.nixbpe = nixbpe;
			
			/*op*/
			masked = (char)((currentCode & (252 << shifter))>>>shifter);
			launchTarget.op = masked;
		}
		/*3형식*/
		else if(instructions.get(counter)[1] == 3) {
			launchTarget.type = 3;
			int shifter = 0;
			char[] tPlace = new char[2];
			char nixbpe = 0;
			
			/*displacement처리*/
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			tPlace[1] = masked;
			shifter+=8;
			masked = (char)((currentCode & (255 << shifter))>>>shifter);
			masked = (char)((masked & (15 << shifter))>>>shifter);
			tPlace[0] = masked;
			launchTarget.displacement = tPlace;
			launchTarget.displacementInt = currentCode & 4095;
			
			/*nixbpe처리*/
			masked = (char)((currentCode & (240 << shifter))>>>shifter);
			masked = (char)(masked >> 4);
			nixbpe = masked;
			shifter+=8;
			/*바로 더할 수 있게 마지막 shift를 4번 뺀다.*/
			masked = (char)((currentCode & (3 << shifter))>>>shifter-4);
			nixbpe += masked;
			launchTarget.nixbpe = nixbpe;
			
			/*op*/
			masked = (char)((currentCode & (252 << shifter))>>>shifter);
			launchTarget.op = masked;
		}
		/*2형식*/
		else if(instructions.get(counter)[1] == 2) {
			launchTarget.type = 2;
			/*레지스터 처리*/
			int shifter = 0;
			masked = (char)((currentCode & (15 << shifter)) >>> shifter);
			launchTarget.register2 = masked;
			shifter+=4;
			masked = (char)((currentCode & (15 << shifter)) >>> shifter);
			launchTarget.register1 = masked;
			shifter+=4;
			
			/*op처리*/
			masked = (char)((currentCode & (255 << shifter)) >>> shifter);
			launchTarget.op = masked;
		}
		/*1형식*/
		else if(instructions.get(counter)[1] == 1) {
			launchTarget.type = 1;
			launchTarget.op = (char)currentCode;
		}
		String instLog = ilchr.launchInst(launchTarget); 
		addLog(instLog);
		
		/*Jump와 관련된 명령어를 수행한 경우 counter를 맞춰준다.*/
		if(instLog.equals("JSUB") || instLog.equals("J BACKWARD") || instLog.equals("RSUB") 
				|| instLog.equals("JLT LOOP IN") || instLog.equals("JEQ")) {
			syncCounter();
			/*점프한 곳을 넘어가지 않고 실행을 해야하므로 counter를 건드리지 않는다.*/
			if(instLog.equals("JSUB")) {
				int pcRegister = rMgr.getRegister(8);
				for(int i = 0; i < instructions.size(); i++) {
					if(instructions.get(i)[0] == pcRegister) sectionStartAddress.push(instructions.get(i)[0]);
				}
			}
			/*RSUB도 PC register를 저장했던 곳으로 돌아가는 것이기 때문에 역시 counter를 건드리지 않는다.*/
			else if(instLog.equals("RSUB")) {
				if(sectionStartAddress.size()>=2) sectionStartAddress.pop();
			}
		}
		else counter+=1;
		/*
		 * 0대신 endRecord를 쓰면 좋을 것 같지만 endRecord가 loader에 있는 상태라 서로 연결시켜줘야된다.
		 * 확실하지도 않은 상황에서 불필요한 연결은 지양한다.
		 */
		/*if(counter+1<instructions.size())*/
		nextAddress = instructions.get(counter)[0]+instructions.get(counter)[1];
		rMgr.setRegister(8, nextAddress);
		
		/*종료*/
		if(instLog.equals("J BACKWARD")||instLog.equals("J FOREWARD")||instLog.equals("J")) {
			if(ilchr.targetAddress == instructions.get(0)[0]) {
				termination = true;
				addLog("\nTermination");
				rMgr.setRegister(8, ilchr.targetAddress);
			}
		}
	}
	
	/**
	 * Jump와 관련된 명령어를 수행했을 경우 PC register와 시뮬레이터의 접근정보 counter를 맞춰주기 위한 함수.
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
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep() {
		while(!termination) {
			oneStep();
		}
	}
	
	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
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
	/*Appendix에는 4형식은 address라는 명칭을 사용하고 있지만 여기서는 3,4형식 모두 displacement라고 명명한다.*/
	char[] displacement;
	int displacementInt;
	char register1;
	char register2;
}
