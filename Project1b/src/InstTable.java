import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * ��� instruction�� ������ �����ϴ� Ŭ����. instruction data���� �����Ѵ�
 * ���� instruction ���� ����, ���� ��� ����� �����ϴ� �Լ�, ���� ������ �����ϴ� �Լ� ���� ���� �Ѵ�.
 */
public class InstTable {
	/** 
	 * inst.data ������ �ҷ��� �����ϴ� ����.
	 *  ��ɾ��� �̸��� ��������� �ش��ϴ� Instruction�� �������� ������ �� �ִ�.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * Ŭ���� �ʱ�ȭ. �Ľ��� ���ÿ� ó���Ѵ�.
	 * @param instFile : instuction�� ���� ���� ����� ���� �̸�
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * �Է¹��� �̸��� ������ ���� �ش� ������ �Ľ��Ͽ� instMap�� �����Ѵ�.
	 */
	public void openFile(String fileName) {
		try {
			File file = new File(fileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line = "";
			while((line = bufReader.readLine())!=null){
				String[] instArray = line.split("\t");
				if(instArray[0].contains("+")) instArray[0] = instArray[0].split("+")[1];
				instMap.put(instArray[0], new Instruction(line));
			}
			bufReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	//get, set, search ���� �Լ��� ���� ����
	
	/**
	 * search instTable
	 */
	public Instruction searchInstTable(String instruction) {
		if(instruction!=null && instruction.contains("+")) instruction = instruction.substring(1,instruction.length());
		for(Map.Entry<String, Instruction> entry: instMap.entrySet()) {
			if(entry.getKey().equals(instruction)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
/**
 * ��ɾ� �ϳ��ϳ��� ��ü���� ������ InstructionŬ������ ����.
 * instruction�� ���õ� �������� �����ϰ� �������� ������ �����Ѵ�.
 */
class Instruction {
	/* 
	 * ������ inst.data ���Ͽ� �°� �����ϴ� ������ �����Ѵ�.
	 *  
	 * ex)
	 * String instruction;
	 * int opcode;
	 * int numberOfOperand;
	 * String comment;
	 */
	
	//String instName;
	ArrayList<Integer> type;
	String opCode;
	ArrayList<Integer> operandNum;
	
	
	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	int format;
	
	/**
	 * Ŭ������ �����ϸ鼭 �Ϲݹ��ڿ��� ��� ������ �°� �Ľ��Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public Instruction(String line) {
		this.type = new ArrayList<>();
		this.operandNum = new ArrayList<>();
		parsing(line);
	}
	
	/**
	 * �Ϲ� ���ڿ��� �Ľ��Ͽ� instruction ������ �ľ��ϰ� �����Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public void parsing(String line) {
		String[] array = line.split("\t");
		//this.instName = array[0];
		
		String[] typeArray = array[1].split(",");
		for(String ta:typeArray) {
			this.type.add(Integer.parseInt(ta));
		}
		
		this.opCode = array[2];
		
		String[] opnumArray = array[3].split(",");
		for(String oa:opnumArray) {
			this.operandNum.add(Integer.parseInt(oa));
		}
	}
	
		
	//�� �� �Լ� ���� ����
}
