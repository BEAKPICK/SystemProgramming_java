package SP20_simulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;


/**
 * ResourceManager�� ��ǻ���� ���� ���ҽ����� �����ϰ� �����ϴ� Ŭ�����̴�.
 * ũ�� �װ����� ���� �ڿ� ������ �����ϰ�, �̸� ������ �� �ִ� �Լ����� �����Ѵ�.<br><br>
 * 
 * 1) ������� ���� �ܺ� ��ġ �Ǵ� device<br>
 * 2) ���α׷� �ε� �� ������ ���� �޸� ����. ���⼭�� 64KB�� �ִ밪���� ��´�.<br>
 * 3) ������ �����ϴµ� ����ϴ� �������� ����.<br>
 * 4) SYMTAB �� simulator�� ���� �������� ���Ǵ� �����͵��� ���� ������. 
 * <br><br>
 * 2���� simulator������ ����Ǵ� ���α׷��� ���� �޸𸮰����� �ݸ�,
 * 4���� simulator�� ������ ���� �޸� �����̶�� ������ ���̰� �ִ�.
 */
public class ResourceManager{
	/**
	 * ����̽��� ���� ����� ��ġ���� �ǹ� ������ ���⼭�� ���Ϸ� ����̽��� ��ü�Ѵ�.<br>
	 * ��, 'F1'�̶�� ����̽��� 'F1'�̶�� �̸��� ������ �ǹ��Ѵ�. <br>
	 * deviceManager�� ����̽��� �̸��� �Է¹޾��� �� �ش� �̸��� ���� ����� ���� Ŭ������ �����ϴ� ������ �Ѵ�.
	 * ���� ���, 'A1'�̶�� ����̽����� ������ read���� ������ ���, hashMap�� <"A1", scanner(A1)> ���� �������μ� �̸� ������ �� �ִ�.
	 * <br><br>
	 * ������ ���·� ����ϴ� �� ���� ����Ѵ�.<br>
	 * ���� ��� key������ String��� Integer�� ����� �� �ִ�.
	 * ���� ������� ���� ����ϴ� stream ���� �������� ����, �����Ѵ�.
	 * <br><br>
	 * �̰͵� �����ϸ� �˾Ƽ� �����ؼ� ����ص� �������ϴ�.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	char[] memory = new char[65536]; // String���� �����ؼ� ����Ͽ��� ������.
	int[] register = new int[10];
	/*�� ����� �ѹ��� ���� ����̽��� �ٷ� �� ���ٴ� ������ �ִ�.*/
	//int cursor = 0;
	double register_F;
	int memoryStartAddress = 0;	//�� ���α׷������� �׻� 0�������� ��¡�� �ǹ̷� �޸� �����ּҸ� �д�.
	int reserved = 0;	//section�� size�� ���� �Ҵ�����ֱ����� ����. �۾��� �Ϸ�Ǹ� �̰��� ���α׷� ����� �ȴ�.
	String usingDevice;
	
	static final int COMPARISON_EQUAL = 1;
	static final int COMPARISON_GREATER = 2;
	static final int COMPARISON_LESS = 3;
	/*TD�� JEQ���� ���ѷ����� ���� ������ �߻��Ͽ� TD�� ����� �����ϱ� ���� Flag�߰�*/
	static final int DEVICE_TESTED = 4;
	
	SymbolTable symtabList;
	// �̿ܿ��� �ʿ��� ���� �����ؼ� ����� ��.
	
	
	public ResourceManager() {
		this.symtabList = new SymbolTable();
		initializeResource();
	}
	/**
	 * �޸�, �������͵� ���� ���ҽ����� �ʱ�ȭ�Ѵ�.
	 */
	public void initializeResource(){
		for(int i = 0; i < memory.length; i++) {
			memory[i] = 0;
		}
		for(int i = 0; i < register.length; i++) {
			register[i] = 0;
		}
		register_F = 0;
		symtabList.symbolList.clear();
		symtabList.addressList.clear();
		reserved = 0;
		closeDevice();
		usingDevice="";
	}
	
	
	
	/**
	 * deviceManager�� �����ϰ� �ִ� ���� ����� stream���� ���� �����Ű�� ����.
	 * ���α׷��� �����ϰų� ������ ���� �� ȣ���Ѵ�.
	 */
	public void closeDevice() {
		deviceManager.clear();
	}
	
	/**
	 * ����̽��� ����� �� �ִ� ��Ȳ���� üũ. TD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * ����� stream�� ���� deviceManager�� ���� ������Ų��.
	 * @param devName Ȯ���ϰ��� �ϴ� ����̽��� ��ȣ,�Ǵ� �̸�
	 */
	public void testDevice(String devName) {
		if(!deviceManager.containsKey(devName)) {
			File file = new File(devName);
			usingDevice = devName;
			deviceManager.put(devName, file);
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			cursor = 0;
		}
	}

	/**
	 * ����̽��κ��� ���ϴ� ������ŭ�� ���ڸ� �о���δ�. RD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param num �������� ������ ����
	 * @return ������ ������
	 */
	public char[] readDevice(String devName, int num){
		try {
			if(!deviceManager.containsKey(devName)) {
				File file = new File(devName);
				deviceManager.put(devName, file);
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			usingDevice = devName;
			FileReader fileReader = new FileReader((File)deviceManager.get(devName));
			BufferedReader buffReader = new BufferedReader(fileReader);
			String strResult = buffReader.readLine();
			char[] result = new char[1];
			if(strResult == null) result[0] = 0;
			else{
				result[0] = strResult.toCharArray()[0];
				FileWriter fileWriter = new FileWriter((File)deviceManager.get(devName));
				BufferedWriter buffWriter = new BufferedWriter(fileWriter);
				if(strResult!="")buffWriter.write(strResult.substring(1));
				else buffWriter = new BufferedWriter(fileWriter);
				buffWriter.close();
				fileWriter.close();
			}
			buffReader.close();
			fileReader.close();
			return result;
		} catch(IOException e) {
			System.out.println(e);
		}
		char[] result = new char[1];
		result[0] = 0;
		return result;
	}

	/**
	 * ����̽��� ���ϴ� ���� ��ŭ�� ���ڸ� ����Ѵ�. WD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param data ������ ������
	 * @param num ������ ������ ����
	 */
	public void writeDevice(String devName, char[] data, int num){
		try {
			if(!deviceManager.containsKey(devName)) {
				File file = new File(devName);
				deviceManager.put(devName, file);
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			usingDevice = devName;
			FileWriter fileWriter = new FileWriter((File)deviceManager.get(devName), true);
			BufferedWriter buffWriter = new BufferedWriter(fileWriter);
			buffWriter.write(String.valueOf(data));
			buffWriter.close();
			fileWriter.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	
	/**
	 * �޸��� Ư�� ��ġ���� ���ϴ� ������ŭ�� ���ڸ� �����´�.
	 * @param location �޸� ���� ��ġ �ε���
	 * @param num ������ ����
	 * @return �������� ������
	 */
	public char[] getMemory(int location, int num){
		String requested = "";
		for(int i = location; i < location+num; i++) {
			requested += memory[i];
		}
		return requested.toCharArray();
	}

	/**
	 * �޸��� Ư�� ��ġ�� ���ϴ� ������ŭ�� �����͸� �����Ѵ�. 
	 * @param locate ���� ��ġ �ε���
	 * @param data �����Ϸ��� ������
	 * @param num �����ϴ� �������� ���� (���� 4bit�� �����ϰ��� �� ��)
	 */
	public void setMemory(int locate, char[] data, int num){
		int t = locate;
		int i = 0;
		
		/*��� ��� ���̰� ¦������ �ּҸ� �޸𸮿� �����ϱ� ������ �ʿ������ �׳� Ȥ�� ���� �ص� ��.*/
		if(num%2==1) {
			for(i = 0; i < data.length; i++) {
				if(i==0) {
					/*ù��° data�� ���� 4����*/
					char crop = (char)((data[i] << 4) >> 4);
					/*���ۺκ� memory ���� 4���� �����ش�.*/
					crop += (char)((memory[t] >> 4) << 4);
					memory[t] = crop;
					t++;
				}
				else{
					memory[t] = data[i];
					t++;
				}
			}
		}
		else {
			for(i = 0; i < data.length; i++) {
				memory[t] = data[i];
				t++;
			}
		}
	}
	
	/**
	 * �ܺ��������̺��� �ּҿ����� �ϱ� ���� �Լ���. ���� �����ʹ� 10������ ��ȯ�� int���� �´�.
	 * @param locate
	 * @param length
	 * @param data
	 */
	public void calMemory(int locate, int length, int data) {
		//String cropped = "";
		int croppedInt = 0;
		/*��ĵ�� ���̴� 4bit������ ���̷� ������ ������ 2�� ������ �� �ݿø����ش�.*/
		int cntr = (int)(length / 2);
		int shifter = 0;
		/*�Ųٷ� �޸𸮸� ��ĵ�ϸ� int������ �ּҸ� �����´�.*/
		for(int i = locate+cntr; i>=locate; i--) {
			if(i == locate) {
				/*�Ųٷ� ��ĵ�Ͽ� �������� ���̰� Ȧ������ ���� 4���� �����´�*/
				if(length%2==1) {
					croppedInt += ((int)memory[i] & 15) << cntr;
				}
				else croppedInt += ((int)memory[i]) << cntr;
				break;
			}
			int temp = (int)memory[i];
			temp = temp << shifter;
			croppedInt += temp;
			shifter+=8;
		}
		/*������ �ּҸ� data�� �����Ű��*/
		croppedInt += data;
		
		/*���� ���� int�� �ּҸ� ����ŷ�ϸ鼭 char���� �޸𸮿� �ϳ��ϳ� �־��ش�.*/
		shifter = 0;
		for(int i = locate+cntr; i>=locate; i--) {
			if(i == locate) {
				/*�Ųٷ� ��ĵ�Ͽ� �������� ���̰� Ȧ������ ���� 4���� �����Ѵ�.*/
				if(length%2==1) {
					char masked=(char)(croppedInt & (15 << shifter));
					char maskedMemory = (char)(memory[i] & (15 << 4));
					maskedMemory += masked;
					memory[i] = maskedMemory;
					break;
				}
			}
			char masked = (char)((croppedInt & (255 << shifter))>>shifter);
			memory[i] = masked;
			shifter+=8;
		}
	}
	
	/**
	 * �޸��Ҵ�
	 * @param size
	 */
	public void reserve(int size) {
		reserved += size;
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ͱ� ���� ��� �ִ� ���� �����Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum �������� �з���ȣ
	 * @return �������Ͱ� ������ ��
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ϳ� ���ο� ���� �Է��Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum ���������� �з���ȣ
	 * @param value �������Ϳ� ����ִ� ��
	 */
	public void setRegister(int regNum, int value){
		register[regNum] = value;
	}
	
	/**
	 * ���������� rightmostbyte�� �������� �Լ�.
	 */
	public char getRightMostByteRegister(int regNum) {
		return (char)(register[regNum] & 255);
	}
	
	/**
	 * ���������� rightmostbyte�� �����ؾߵǴ� ��츦 ó���ϴ� �Լ�.
	 */
	public void setRightMostByteRegister(int regNum, int value) {
		int temp = register[regNum];
		temp = temp & ~255;
		value = value & 255;
		temp += value;
		register[regNum] = temp;
	}

	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. int���� char[]���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		return Integer.toString(data).toCharArray();
	}

	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. char[]���� int���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public int byteToInt(byte[] data){
		int result = ByteBuffer.wrap(data).getInt();
		return result;
	}
}

///*����̽��� �����ϴ� ��ü*/
//class DeviceDriver{
//
//}