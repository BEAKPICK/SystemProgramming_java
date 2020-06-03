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
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다.
 * 크게 네가지의 가상 자원 공간을 선언하고, 이를 관리할 수 있는 함수들을 제공한다.<br><br>
 * 
 * 1) 입출력을 위한 외부 장치 또는 device<br>
 * 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.<br>
 * 3) 연산을 수행하는데 사용하는 레지스터 공간.<br>
 * 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들. 
 * <br><br>
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면,
 * 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서 차이가 있다.
 */
public class ResourceManager{
	/**
	 * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다.<br>
	 * 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의 파일을 의미한다. <br>
	 * deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을 넣음으로서 이를 관리할 수 있다.
	 * <br><br>
	 * 변형된 형태로 사용하는 것 역시 허용한다.<br>
	 * 예를 들면 key값으로 String대신 Integer를 사용할 수 있다.
	 * 파일 입출력을 위해 사용하는 stream 역시 자유로이 선택, 구현한다.
	 * <br><br>
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	char[] memory = new char[65536]; // String으로 수정해서 사용하여도 무방함.
	int[] register = new int[10];
	/*이 방법은 한번에 여러 디바이스를 다룰 수 없다는 단점이 있다.*/
	//int cursor = 0;
	double register_F;
	int memoryStartAddress = 0;	//이 프로그램에서는 항상 0일테지만 상징적 의미로 메모리 시작주소를 둔다.
	int reserved = 0;	//section의 size를 보고 할당시켜주기위한 변수. 작업이 완료되면 이것이 프로그램 사이즈가 된다.
	String usingDevice;
	
	static final int COMPARISON_EQUAL = 1;
	static final int COMPARISON_GREATER = 2;
	static final int COMPARISON_LESS = 3;
	/*TD후 JEQ에서 무한루프가 도는 현상이 발생하여 TD의 결과를 저장하기 위한 Flag추가*/
	static final int DEVICE_TESTED = 4;
	
	SymbolTable symtabList;
	// 이외에도 필요한 변수 선언해서 사용할 것.
	
	
	public ResourceManager() {
		this.symtabList = new SymbolTable();
		initializeResource();
	}
	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
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
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할.
	 * 프로그램을 종료하거나 연결을 끊을 때 호출한다.
	 */
	public void closeDevice() {
		deviceManager.clear();
	}
	
	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수.
	 * 입출력 stream을 열고 deviceManager를 통해 관리시킨다.
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
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
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param num 가져오는 글자의 개수
	 * @return 가져온 데이터
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
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param data 보내는 데이터
	 * @param num 보내는 글자의 개수
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
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 * @param location 메모리 접근 위치 인덱스
	 * @param num 데이터 개수
	 * @return 가져오는 데이터
	 */
	public char[] getMemory(int location, int num){
		String requested = "";
		for(int i = location; i < location+num; i++) {
			requested += memory[i];
		}
		return requested.toCharArray();
	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다. 
	 * @param locate 접근 위치 인덱스
	 * @param data 저장하려는 데이터
	 * @param num 저장하는 데이터의 개수 (앞의 4bit만 전달하고자 할 때)
	 */
	public void setMemory(int locate, char[] data, int num){
		int t = locate;
		int i = 0;
		
		/*사실 모두 길이가 짝수개인 주소를 메모리에 저장하기 때문에 필요없지만 그냥 혹시 몰라서 해둔 것.*/
		if(num%2==1) {
			for(i = 0; i < data.length; i++) {
				if(i==0) {
					/*첫번째 data의 뒤의 4개와*/
					char crop = (char)((data[i] << 4) >> 4);
					/*시작부분 memory 앞의 4개를 더해준다.*/
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
	 * 외부참조테이블의 주소연산을 하기 위한 함수다. 따라서 데이터는 10진수로 변환된 int형이 온다.
	 * @param locate
	 * @param length
	 * @param data
	 */
	public void calMemory(int locate, int length, int data) {
		//String cropped = "";
		int croppedInt = 0;
		/*스캔할 길이는 4bit단위의 길이로 들어오기 때문에 2로 나눠준 뒤 반올림해준다.*/
		int cntr = (int)(length / 2);
		int shifter = 0;
		/*거꾸로 메모리를 스캔하며 int형으로 주소를 가져온다.*/
		for(int i = locate+cntr; i>=locate; i--) {
			if(i == locate) {
				/*거꾸로 스캔하여 마지막에 길이가 홀수개면 뒤의 4개만 가져온다*/
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
		/*가져온 주소를 data와 연산시키기*/
		croppedInt += data;
		
		/*이제 계산된 int형 주소를 마스킹하면서 char형인 메모리에 하나하나 넣어준다.*/
		shifter = 0;
		for(int i = locate+cntr; i>=locate; i--) {
			if(i == locate) {
				/*거꾸로 스캔하여 마지막에 길이가 홀수개면 뒤의 4개만 설정한다.*/
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
	 * 메모리할당
	 * @param size
	 */
	public void reserve(int size) {
		reserved += size;
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터의 분류번호
	 * @param value 레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value){
		register[regNum] = value;
	}
	
	/**
	 * 레지스터의 rightmostbyte를 가져오는 함수.
	 */
	public char getRightMostByteRegister(int regNum) {
		return (char)(register[regNum] & 255);
	}
	
	/**
	 * 레지스터의 rightmostbyte에 저장해야되는 경우를 처리하는 함수.
	 */
	public void setRightMostByteRegister(int regNum, int value) {
		int temp = register[regNum];
		temp = temp & ~255;
		value = value & 255;
		temp += value;
		register[regNum] = temp;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		return Integer.toString(data).toCharArray();
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 * @param data
	 * @return
	 */
	public int byteToInt(byte[] data){
		int result = ByteBuffer.wrap(data).getInt();
		return result;
	}
}

///*디바이스를 관리하는 객체*/
//class DeviceDriver{
//
//}