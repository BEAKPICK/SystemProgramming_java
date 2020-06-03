package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

/*Ah...InstLauncher를 오타낸 듯 하다...그냥 못본 척 하자...*/
public class InstLuncher {
    ResourceManager rMgr;
    private HashMap<String,String> instDic;
    private SicSimulator simul;
    public int targetAddress;
    
    public InstLuncher(ResourceManager resourceManager, SicSimulator simul) {
        this.rMgr = resourceManager;
        this.simul = simul;
        instDic = new HashMap<>();
        loadInst(System.getProperty("user.dir")+"/inst.data");
    }

    /**
     * instruction 정보를 가져온다.
     * @param fileName
     */
    public void loadInst(String fileName) {
    	try {
    		File file = new File(fileName);
        	FileReader fileReader = new FileReader(file);
        	BufferedReader bufReader = new BufferedReader(fileReader);
        	String line = "";
        	while((line= bufReader.readLine())!=null) {
        		String[] instArray = line.split("\t");
        		/*key는 실제코드이며 value는 instruction name이 된다.*/
        		instDic.put(instArray[2], instArray[0]);
        	}
        	bufReader.close();
        	fileReader.close();
    	} catch(FileNotFoundException e) {
    		System.out.println(e);
    	} catch(IOException e) {
    		System.out.println(e);
    	}
    }
    
    /**
     * int형인 레지스터를 charArray로 바꿔주는 역할을 한다. 참고로 레지스터의 길이는 24bit 즉 char[3]이다.
     * rightmostbyte일때는 char[1]로 리턴한다.
     */
    public char[] intTo3CharArray(int input, boolean isRightMostByte) {
    	if(!isRightMostByte) {
    		char[] result = new char[3];
        	int shifter = 0;
        	result[2] = (char)((input & (255 << shifter)) >>> shifter);
        	shifter+=8;
        	result[1] = (char)((input & (255 << shifter)) >>> shifter);
        	shifter+=8;
        	result[0] = (char)((input & (255 << shifter)) >>> shifter);
        	return result;
    	}
    	else {
    		char[] result = new char[1];
    		result[0] = (char)(input & 255);
    		return result;
    	}
    }
    
    /**
     * char[]를 int로 바꿔주는 함수, 이 프로그램에선 메모리를 3개씩만 접근하므로 input은 크기가 무조건 char[3]이하이다.
     * @param input
     * @return
     */
    public int charArrayToInt(char[] input) {
    	int result = 0;
    	int shifter = 0;
    	if(input.length >= 3) {
    		result = (int)input[2];
        	shifter += 8;
    	}
    	if(input.length >= 2) {
    		result += (int)input[1] << shifter;
        	shifter += 8;
    	}
    	result += (int)input[0] << shifter;
    	return result;
    }
    
    /**
     * 메모리 주소를 계산할 때 범위를 넘어가면 0이 될 수 있게 마스킹을 해줘야한다. 4bit씩 3개고 int를 리턴한다.
     */
    public int maskOverFlowedAddress(int target) {
    	target += rMgr.getRegister(8) - simul.sectionStartAddress.peek();
    	return target & 4095;
    }
    
    /**
     * nixbpe를 가지고 있는 3,4형식에만 적용된다. 타겟주소를 계산하고 메모리에 접근해서 가져온 값을 리턴한다.
     * 여기서는 메모리를 항상 3byte씩만을 가져온다. 10000000111100
     */
    public int calAddress(Instruction lt) {
    	/*주소전처리*/
    	int targetAddress = lt.displacementInt;
    	/*p가 1이면 PC register를 더함*/
		if((int)(lt.nixbpe & 2)==2) {
			targetAddress+=rMgr.getRegister(8);
		}
		/*b가 1이면 BASE register를 더함*/
		else if((int)(lt.nixbpe & 4)==4) {
			targetAddress+=rMgr.getRegister(3);
		}
		/*x가 1이면 X register를 더함*/
		else if((int)(lt.nixbpe & 8)==8) {
			targetAddress+=rMgr.getRegister(1);
		}
		
    	/*extension*/
    	if((int)(lt.nixbpe & 1) == 1) {
    		this.targetAddress = targetAddress;
    		return targetAddress;
    	}
    	else {
    		/*simple addressing*/
        	if(!((int)(lt.nixbpe & 32) == 32 ^ (int)(lt.nixbpe & 16) == 16)) {
        		this.targetAddress = targetAddress;
        		return targetAddress;
        	}
        	/*indirect addressing*/
        	else if((int)(lt.nixbpe & 32) == 32 && !((int)(lt.nixbpe & 16) == 16)){
        		this.targetAddress = charArrayToInt(rMgr.getMemory(targetAddress,3));
        		return this.targetAddress;
        	}
        	/*immediate addressing*/
        	else if(!((int)(lt.nixbpe & 32)== 32)  && (int)(lt.nixbpe & 16) == 16) {
        		this.targetAddress = targetAddress;
        		return targetAddress;
        	}
    	}
    	
    	return this.targetAddress;
    }
    
    /**
     * launch Target을 실행시키는 함수. 성공하면 해당 instruction name을 리턴한다.
     * 리턴값은 log를 하기 위한 정보로 이용된다.
     */
    public String launchInst(Instruction launchTarget) {
    	/*operation은 무조건 hex 2자리 코드이다 따라서 항상 8byte*/
    	String inst = instDic.get(String.format("%02X", (int)(launchTarget.op)));
    	if(inst == null) return "instruction not found";
    	if(inst.equals("STL")) {
    		/*store L register*/
    		rMgr.setMemory(calAddress(launchTarget), intTo3CharArray(rMgr.getRegister(2), false), 6);
    		return "STL";
    	}
    	else if(inst.equals("LDA")) {
    		/*load A register from m*/
    		/*immediate addressing은 메모리 접근하지 않는다.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(0, calAddress(launchTarget));
    		}
    		else rMgr.setRegister(0, charArrayToInt(rMgr.getMemory(calAddress(launchTarget),3)));
    		return "LDA";
    	}
    	else if(inst.equals("COMP")) {
    		/*compare registers*/
    		if(rMgr.getRegister(0) > calAddress(launchTarget)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_GREATER);
    		}
    		else if(rMgr.getRegister(0) < calAddress(launchTarget)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_LESS);
    		}
    		else if(rMgr.getRegister(0) == calAddress(launchTarget)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_EQUAL);
    		}
    		return "COMP";
    	}
    	else if(inst.equals("JEQ")) {
    		/*equal then store displacement to PC register*/
    		if(rMgr.getRegister(9) == ResourceManager.COMPARISON_EQUAL) {
    			/*immediate addressing은 메모리 접근하지 않는다.*/
        		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
        			rMgr.setRegister(8, calAddress(launchTarget));
        		}
        		/*JEQ의 경우 역행으로 갈 수도 있고 순행으로 갈 수도 있다. 그리고 거기에 따라 대응방법이 각각 다르다.*/
        		/*따라서 순행과 역행을 나누는 기준은 주소 첫번째에 F가 있는 경우라고 제한하고 이를 해결한다.*/
        		else {
        			if(String.format("%X", (launchTarget.displacementInt & (15<<8))>>8).equals("F")) {
        				rMgr.setRegister(8, simul.sectionStartAddress.peek()+maskOverFlowedAddress(launchTarget.displacementInt));
        				//return "JEQ BACKWARD";
        			}
        			else {
        				rMgr.setRegister(8, calAddress(launchTarget));
        				//return "JEQ FORWARD";
        			}
        		}
//        		else rMgr.setRegister(8, simul.sectionStartAddress.peek()+maskOverFlowedAddress(launchTarget.displacementInt));
    		}
    		else return "JEQ PASS";
    		return "JEQ";
    	}
    	else if(inst.equals("JSUB")) {
    		/*L<-PC and PC<-m*/
    		rMgr.setRegister(2, rMgr.getRegister(8));
    		/*immediate addressing은 메모리 접근하지 않는다.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(8, calAddress(launchTarget));
    		}
    		else rMgr.setRegister(8, calAddress(launchTarget));
    		return "JSUB";
    	}
    	else if(inst.equals("J")) {
    		/*set PC register from memory*/
    		/*immediate addressing은 메모리 접근하지 않는다.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(8, calAddress(launchTarget));
    		}
    		/*J의 경우도 마찬가지로 역행으로 갈 수도 있고 순행으로 갈 수도 있다. 그리고 거기에 따라 대응방법이 각각 다르다.*/
    		/*따라서 순행과 역행을 나누는 기준은 주소 첫번째에 F가 있는 경우라고 제한하고 이를 해결한다.*/
    		else{
        		if(String.format("%X", (launchTarget.displacementInt & (15<<8))>>8).equals("F")) {
        			rMgr.setRegister(8, simul.sectionStartAddress.peek()+maskOverFlowedAddress(launchTarget.displacementInt));
        			return "J BACKWARD";
        		}
        		else {
        			rMgr.setRegister(8, calAddress(launchTarget));
        			return "J FOREWARD";
        		}
//    			rMgr.setRegister(8, (calAddress(launchTarget)& 4095));
    		}
    		//dontTouchPCRegister = false;
    		return "J";
    	}
    	else if(inst.equals("STA")) {
    		/*store A register to memory*/
    		rMgr.setMemory(calAddress(launchTarget), intTo3CharArray(rMgr.getRegister(0), false), 6);
    		return "STA";
    	}
    	else if(inst.equals("CLEAR")) {
    		/*set 0 to register*/
    		rMgr.setRegister((int)launchTarget.register1, 0);
    		return "CLEAR";
    	}
    	else if(inst.equals("LDT")) {
    		/*immediate addressing은 메모리 접근하지 않는다.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(5, calAddress(launchTarget));
    		}
    		/*load T register from memory*/
    		rMgr.setRegister(5, charArrayToInt(rMgr.getMemory(calAddress(launchTarget),3)));
    		return "LDT";
    	}
    	else if(inst.equals("TD")) {
    		/*Test device specified by m*/
    		rMgr.testDevice(String.format("%02X", charArrayToInt(rMgr.getMemory(calAddress(launchTarget), 1))));
    		rMgr.setRegister(9, ResourceManager.DEVICE_TESTED);
    		return "TD";
    	}
    	else if(inst.equals("RD")) {
    		/*rightmost byte of A register <- data from device specified by m*/
    		char[] result = rMgr.readDevice(String.format("%02X", charArrayToInt(rMgr.getMemory(calAddress(launchTarget), 1))), 1);
    		/*1byte이기 때문에 무조건 길이가 1*/
    		rMgr.setRightMostByteRegister(0, (int)result[0]);
    		return "RD";
    	}
    	else if(inst.equals("COMPR")) {
    		/*compare between 2 registers*/
    		if(rMgr.getRegister(launchTarget.register1) > rMgr.getRegister(launchTarget.register2)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_GREATER);
    		}
    		else if(rMgr.getRegister(launchTarget.register1) < rMgr.getRegister(launchTarget.register2)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_LESS);
    		}
    		else if(rMgr.getRegister(launchTarget.register1) == rMgr.getRegister(launchTarget.register2)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_EQUAL);
    		}
    		return "COMPR";
    	}
    	else if(inst.equals("STCH")) {
    		/*store right most byte A register to m*/
    		rMgr.setMemory(calAddress(launchTarget), intTo3CharArray(rMgr.getRegister(0), true), 2);
    		return "STCH";
    	}
    	else if(inst.equals("TIXR")) {
    		/*tick X register and compare*/
    		rMgr.setRegister(1, rMgr.getRegister(1)+1);
    		if(rMgr.getRegister(1) > rMgr.getRegister(launchTarget.register1)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_GREATER);
    		}
    		else if(rMgr.getRegister(1) < rMgr.getRegister(launchTarget.register1)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_LESS);
    		}
    		else if(rMgr.getRegister(1) == rMgr.getRegister(launchTarget.register1)) {
    			rMgr.setRegister(9, ResourceManager.COMPARISON_EQUAL);
    		}
    		return "TIXR";
    	}
    	else if(inst.equals("JLT")) {
    		/*if less set PC register from m*/
    		if(rMgr.getRegister(9) == ResourceManager.COMPARISON_LESS) {
    			/*immediate addressing은 메모리 접근하지 않는다.*/
        		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
        			rMgr.setRegister(8, calAddress(launchTarget));
        		}
    			//rMgr.setRegister(8, charArrayToInt(rMgr.getMemory(calAddress(launchTarget),3)));
        		/*back하는 대표적인 jump 명령어로 overflow를 체크하는 masking이 필요하다.*/
        		/*하지만 FE9에서 PC를 더해서 마스킹을 한다고 해서 원본코드처럼 원하는 곳의 주소가 나오는게 아님...따라서 PC에서 섹션의 시작주소값을 빼줘야한다.*/
        		/*instLuncher동작 후 뒷처리를 sicSimulator가 했었기 때문에 sicSimulator는 instLuncher의  동작에 직접적으로 관여를 하지 못하기에 불가피하게 2개의 결합도를 높여야했다.*/
    			rMgr.setRegister(8, simul.sectionStartAddress.peek()+maskOverFlowedAddress(launchTarget.displacementInt));
    			return "JLT LOOP IN";
    		}
    		else return "JLT LOOP OUT";
    	}
    	else if(inst.equals("STX")) {
    		/*store X register value to m*/
    		rMgr.setMemory(calAddress(launchTarget), intTo3CharArray(rMgr.getRegister(1), false), 6);
    		return "STX";
    	}
    	else if(inst.equals("LDCH")) {
    		/*load m to right most byte of A register*/
    		rMgr.setRightMostByteRegister(0, charArrayToInt(rMgr.getMemory(calAddress(launchTarget),1)));
    		return "LDCH";
    	}
    	else if(inst.equals("WD")) {
    		/*device specified by m <- A*/
    		char[] data = new char[1];
    		data[0] = rMgr.getRightMostByteRegister(0);
    		rMgr.writeDevice(String.format("%02X", charArrayToInt(rMgr.getMemory(calAddress(launchTarget), 1))), data, 1);
    		return "WD";
    	}
    	else if(inst.equals("RSUB")) {
    		/*load PC register from L register*/
    		rMgr.setRegister(8, rMgr.getRegister(2));
    		return "RSUB";
    	}
    	return "";
    }
}