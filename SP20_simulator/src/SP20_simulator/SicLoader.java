package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SicLoader�� ���α׷��� �ؼ��ؼ� �޸𸮿� �ø��� ������ �����Ѵ�. �� �������� linker�� ���� ���� �����Ѵ�. 
 * <br><br>
 * SicLoader�� �����ϴ� ���� ���� ��� ������ ����.<br>
 * - program code�� �޸𸮿� �����Ű��<br>
 * - �־��� ������ŭ �޸𸮿� �� ���� �Ҵ��ϱ�<br>
 * - �������� �߻��ϴ� symbol, ���α׷� �����ּ�, control section �� ������ ���� ���� ���� �� ����
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
		// �ʿ��ϴٸ� �ʱ�ȭ
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
	 * �޸� ���� ������ �� �ùķ����͸� �����ϴ� �Լ�.
	 */
	public void connectSimulator(SicSimulator simul) {
		this.simulator = simul;
	}
	
	/**
	 * Loader�� ���α׷��� ������ �޸𸮸� �����Ų��.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	
	/**
	 * object code�� �о load������ �����Ѵ�. load�� �����ʹ� resourceManager�� �����ϴ� �޸𸮿� �ö󰡵��� �Ѵ�.
	 * load�������� ������� symbol table �� �ڷᱸ�� ���� resourceManager�� �����Ѵ�.
	 * @param objectCode �о���� ����
	 */
	public void load(File objectCode){
		try {
			this.objectCode = objectCode;
			List<Ext> extList = new ArrayList<>(); 
			FileReader fileReader = new FileReader(objectCode);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line = "";
			while((line = bufReader.readLine())!=null){
				/*Header ó��*/
				if(line.charAt(0) == 'H') {
					extList.add(new Ext());
					extList.get(extList.size()-1).startAddress = rMgr.reserved;
					String[] splited = line.split("\t");
					String[] splitedAddress = splited[1].split(" ");
					/*���� ���� ���α׷� �����ּҴ� ������Ʈ �ڵ忡 ���������� startAddress�� �����ؾ��� ���̴�.������ �� ���α׷����� �����ּҴ� �׻� 0�̱⿡ ������Ʈ �ڵ��� �����ּҸ� ���������Ѵ�..*/
					rMgr.symtabList.putSymbol(splited[0].substring(1), extList.get(extList.size()-1).startAddress+Integer.parseInt(splitedAddress[0], 16));
					rMgr.reserve(Integer.parseInt(splitedAddress[1], 16));
					/*ù ��忡 ��ϵ� �������� �����´�.*/
					if(init == true) {
						programName = splited[0].substring(1);
						init = false;
					}
				}
				/*EXTDEF ó��*/
				else if(line.charAt(0) == 'D') {
					line = line.substring(1);
					String[] splited = line.split(" ");
					for(int i = 0; i < splited.length; i+=2) {
						rMgr.symtabList.putSymbol(splited[i], Integer.parseInt(splited[i+1], 16));
					}
				}
				/*�ڵ�ó��*/
				else if(line.charAt(0) == 'T') {
					line = line.substring(1);
					String[] splited = line.split(" ");
					//
					int startAddress = Integer.parseInt(splited[0], 16) + extList.get(extList.size()-1).startAddress;
					for(int i = 2; i < splited.length; i++) {
						
						char[] data = twoCharToByte(splited[i].toCharArray());
						rMgr.setMemory(startAddress, data, data.length*2);
						
						/*�ùķ����Ϳ� �޸� ���������� �ش�.*/
						if(simulator!=null) simulator.addSimulationUnit(startAddress, data.length);
						
						startAddress+=(int)(splited[i].length()/2);
					}
					//rMgr.setMemory(startAddress, twoCharToByte(actualCode.toCharArray()), Integer.parseInt(splited[1], 16)*2);
				}
				/*�ܺ��������̺�ó��*/
				else if(line.charAt(0) == 'M') {
					line = line.substring(1);
					
					if(line.contains("+")) {
						String[] splited = line.split("\\+");
						String[] splited2 = splited[0].split(" ");
						/*�ܺ��������̺��� section�� extList�� index�� �����Ѵ�.*/
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
			
			/*�ּҸ� �������ش�.*/
			for(int i = 0; i < extList.size(); i++) {
				for(int n = 0; n < extList.get(i).symbolList.size(); n++) {
					/*�ܺ��������̺��� �ش� ������ �����ּҿ� �ش� �ܺ������� �ּҸ� ���Ѵ�.*/
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
	 * 16���� String�� �� ���ڰ� 4bit�̱� ������ 8bit�� �޸����Ŀ� �°� ��ȯ���ش�.
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
