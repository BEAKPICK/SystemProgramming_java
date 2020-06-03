package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

// instruction�� ���� ������ �����ϴ� �޼ҵ带 �����ϴ� Ŭ����

/*Ah...InstLauncher�� ��Ÿ�� �� �ϴ�...�׳� ���� ô ����...*/
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
     * instruction ������ �����´�.
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
        		/*key�� �����ڵ��̸� value�� instruction name�� �ȴ�.*/
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
     * int���� �������͸� charArray�� �ٲ��ִ� ������ �Ѵ�. ����� ���������� ���̴� 24bit �� char[3]�̴�.
     * rightmostbyte�϶��� char[1]�� �����Ѵ�.
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
     * char[]�� int�� �ٲ��ִ� �Լ�, �� ���α׷����� �޸𸮸� 3������ �����ϹǷ� input�� ũ�Ⱑ ������ char[3]�����̴�.
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
     * �޸� �ּҸ� ����� �� ������ �Ѿ�� 0�� �� �� �ְ� ����ŷ�� ������Ѵ�. 4bit�� 3���� int�� �����Ѵ�.
     */
    public int maskOverFlowedAddress(int target) {
    	target += rMgr.getRegister(8) - simul.sectionStartAddress.peek();
    	return target & 4095;
    }
    
    /**
     * nixbpe�� ������ �ִ� 3,4���Ŀ��� ����ȴ�. Ÿ���ּҸ� ����ϰ� �޸𸮿� �����ؼ� ������ ���� �����Ѵ�.
     * ���⼭�� �޸𸮸� �׻� 3byte������ �����´�. 10000000111100
     */
    public int calAddress(Instruction lt) {
    	/*�ּ���ó��*/
    	int targetAddress = lt.displacementInt;
    	/*p�� 1�̸� PC register�� ����*/
		if((int)(lt.nixbpe & 2)==2) {
			targetAddress+=rMgr.getRegister(8);
		}
		/*b�� 1�̸� BASE register�� ����*/
		else if((int)(lt.nixbpe & 4)==4) {
			targetAddress+=rMgr.getRegister(3);
		}
		/*x�� 1�̸� X register�� ����*/
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
     * launch Target�� �����Ű�� �Լ�. �����ϸ� �ش� instruction name�� �����Ѵ�.
     * ���ϰ��� log�� �ϱ� ���� ������ �̿�ȴ�.
     */
    public String launchInst(Instruction launchTarget) {
    	/*operation�� ������ hex 2�ڸ� �ڵ��̴� ���� �׻� 8byte*/
    	String inst = instDic.get(String.format("%02X", (int)(launchTarget.op)));
    	if(inst == null) return "instruction not found";
    	if(inst.equals("STL")) {
    		/*store L register*/
    		rMgr.setMemory(calAddress(launchTarget), intTo3CharArray(rMgr.getRegister(2), false), 6);
    		return "STL";
    	}
    	else if(inst.equals("LDA")) {
    		/*load A register from m*/
    		/*immediate addressing�� �޸� �������� �ʴ´�.*/
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
    			/*immediate addressing�� �޸� �������� �ʴ´�.*/
        		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
        			rMgr.setRegister(8, calAddress(launchTarget));
        		}
        		/*JEQ�� ��� �������� �� ���� �ְ� �������� �� ���� �ִ�. �׸��� �ű⿡ ���� ��������� ���� �ٸ���.*/
        		/*���� ����� ������ ������ ������ �ּ� ù��°�� F�� �ִ� ����� �����ϰ� �̸� �ذ��Ѵ�.*/
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
    		/*immediate addressing�� �޸� �������� �ʴ´�.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(8, calAddress(launchTarget));
    		}
    		else rMgr.setRegister(8, calAddress(launchTarget));
    		return "JSUB";
    	}
    	else if(inst.equals("J")) {
    		/*set PC register from memory*/
    		/*immediate addressing�� �޸� �������� �ʴ´�.*/
    		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
    			rMgr.setRegister(8, calAddress(launchTarget));
    		}
    		/*J�� ��쵵 ���������� �������� �� ���� �ְ� �������� �� ���� �ִ�. �׸��� �ű⿡ ���� ��������� ���� �ٸ���.*/
    		/*���� ����� ������ ������ ������ �ּ� ù��°�� F�� �ִ� ����� �����ϰ� �̸� �ذ��Ѵ�.*/
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
    		/*immediate addressing�� �޸� �������� �ʴ´�.*/
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
    		/*1byte�̱� ������ ������ ���̰� 1*/
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
    			/*immediate addressing�� �޸� �������� �ʴ´�.*/
        		if(!((int)(launchTarget.nixbpe & 32)== 32)  && (int)(launchTarget.nixbpe & 16) == 16) {
        			rMgr.setRegister(8, calAddress(launchTarget));
        		}
    			//rMgr.setRegister(8, charArrayToInt(rMgr.getMemory(calAddress(launchTarget),3)));
        		/*back�ϴ� ��ǥ���� jump ��ɾ�� overflow�� üũ�ϴ� masking�� �ʿ��ϴ�.*/
        		/*������ FE9���� PC�� ���ؼ� ����ŷ�� �Ѵٰ� �ؼ� �����ڵ�ó�� ���ϴ� ���� �ּҰ� �����°� �ƴ�...���� PC���� ������ �����ּҰ��� ������Ѵ�.*/
        		/*instLuncher���� �� ��ó���� sicSimulator�� �߾��� ������ sicSimulator�� instLuncher��  ���ۿ� ���������� ������ ���� ���ϱ⿡ �Ұ����ϰ� 2���� ���յ��� �������ߴ�.*/
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