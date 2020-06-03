package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다. 
 * <br><br>
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다.<br>
 * - program code를 메모리에 적재시키기<br>
 * - 주어진 공간만큼 메모리에 빈 공간 할당하기<br>
 * - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;
	private SicSimulator simulator;
	
	public String programName;
	public String endRecord;
	public int startAddress = 0;
	private boolean init = true;
	
	public File objectCode;
	
	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
	}
	
	public void resetSicLoader() {
		programName = "";
		endRecord = "";
		startAddress = 0;
		init = true;
		objectCode = null;
	}

	/**
	 * 메모리 접근 정보를 줄 시뮬레이터를 연결하는 함수.
	 */
	public void connectSimulator(SicSimulator simul) {
		this.simulator = simul;
	}
	
	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	
	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다.
	 * load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode){
		try {
			this.objectCode = objectCode;
			List<Ext> extList = new ArrayList<>(); 
			FileReader fileReader = new FileReader(objectCode);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line = "";
			while((line = bufReader.readLine())!=null){
				/*Header 처리*/
				if(line.charAt(0) == 'H') {
					extList.add(new Ext());
					extList.get(extList.size()-1).startAddress = rMgr.reserved;
					String[] splited = line.split("\t");
					String[] splitedAddress = splited[1].split(" ");
					/*지금 현재 프로그램 시작주소는 오브젝트 코드에 기인하지만 startAddress로 설정해야할 것이다.하지만 이 프로그램에서 시작주소는 항상 0이기에 오브젝트 코드의 시작주소를 따르도록한다..*/
					rMgr.symtabList.putSymbol(splited[0].substring(1), extList.get(extList.size()-1).startAddress+Integer.parseInt(splitedAddress[0], 16));
					rMgr.reserve(Integer.parseInt(splitedAddress[1], 16));
					/*첫 헤드에 기록된 정보만을 가져온다.*/
					if(init == true) {
						programName = splited[0].substring(1);
						init = false;
					}
				}
				/*EXTDEF 처리*/
				else if(line.charAt(0) == 'D') {
					line = line.substring(1);
					String[] splited = line.split(" ");
					for(int i = 0; i < splited.length; i+=2) {
						rMgr.symtabList.putSymbol(splited[i], Integer.parseInt(splited[i+1], 16));
					}
				}
				/*코드처리*/
				else if(line.charAt(0) == 'T') {
					line = line.substring(1);
					String[] splited = line.split(" ");
					//
					int startAddress = Integer.parseInt(splited[0], 16) + extList.get(extList.size()-1).startAddress;
					for(int i = 2; i < splited.length; i++) {
						
						char[] data = twoCharToByte(splited[i].toCharArray());
						rMgr.setMemory(startAddress, data, data.length*2);
						
						/*시뮬레이터에 메모리 접근정보를 준다.*/
						if(simulator!=null) simulator.addSimulationUnit(startAddress, data.length);
						
						startAddress+=(int)(splited[i].length()/2);
					}
					//rMgr.setMemory(startAddress, twoCharToByte(actualCode.toCharArray()), Integer.parseInt(splited[1], 16)*2);
				}
				/*외부참조테이블처리*/
				else if(line.charAt(0) == 'M') {
					line = line.substring(1);
					
					if(line.contains("+")) {
						String[] splited = line.split("\\+");
						String[] splited2 = splited[0].split(" ");
						/*외부참조테이블의 section은 extList의 index로 구분한다.*/
						extList.get(extList.size()-1).putSymbol("+"+splited[1], Integer.parseInt(splited2[1], 16), Integer.parseInt(splited2[0], 16));
					}
					else if(line.contains("-")) {
						String[] splited = line.split("-");
						String[] splited2 = splited[0].split(" ");
						extList.get(extList.size()-1).putSymbol("-"+splited[1], Integer.parseInt(splited2[1], 16), Integer.parseInt(splited2[0], 16));
					}
				}
				else if(line.charAt(0) == 'E') {
					if(line.length()!=1) {
						endRecord = line.substring(1);
					}
				}
			}
			bufReader.close();
			fileReader.close();
			
			/*주소를 편집해준다.*/
			for(int i = 0; i < extList.size(); i++) {
				for(int n = 0; n < extList.get(i).symbolList.size(); n++) {
					/*외부참조테이블의 해당 섹션의 시작주소에 해당 외부참조의 주소를 더한다.*/
					int location = extList.get(i).startAddress + extList.get(i).addressList.get(n);
					if(location == 4171) {
						System.out.println("");
					}
					if(extList.get(i).symbolList.get(n).charAt(0) == '+') {
						rMgr.calMemory(location, extList.get(i).addressLength.get(n),
								rMgr.symtabList.search(extList.get(i).symbolList.get(n).substring(1)));
					}
					else if (extList.get(i).symbolList.get(n).charAt(0) == '-') {
						rMgr.calMemory(location, extList.get(i).addressLength.get(n),
								0-rMgr.symtabList.search(extList.get(i).symbolList.get(n).substring(1)));
					}
				}
			}
		} catch(IOException e) {
		}
	}
	
	/**
	 * 16진수 String은 각 숫자가 4bit이기 때문에 8bit인 메모리형식에 맞게 변환해준다.
	 * @param target
	 * @return
	 */
	private char[] twoCharToByte(char[] target) {
		String result = "";
		for(int i = 0; i < target.length; i+=2) {
			char unit = (char)(Character.digit(target[i],16) << 4);
			unit += Character.digit(target[i+1],16);
			result += unit;
		}
		return result.toCharArray();
	}
}

class Ext extends SymbolTable{
	List<Integer> addressLength;
	int startAddress = 0;

	public Ext() {
		super();
		this.addressLength = new ArrayList<>();
	}
	public void putSymbol(String symbol, int length, int address) {
		symbolList.add(symbol);
		addressList.add(address);
		addressLength.add(length);
	}
}
