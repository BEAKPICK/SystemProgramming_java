import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Assembler : 
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. 
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. 
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. 
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) 
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) 
 * 
 * 
 * �ۼ����� ���ǻ��� : 
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.
 *  2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)
 * 
 *     
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
	/** instruction ���� ������ ���� */
	InstTable instTable;
	/** �о���� input ������ ������ �� �� �� �����ϴ� ����. */
	ArrayList<String> lineList;
	/** ���α׷��� section���� symbol table�� �����ϴ� ����*/
	ArrayList<SymbolTable> symtabList;
	/** ���α׷��� section���� literal table�� �����ϴ� ����*/
	ArrayList<LiteralTable> literaltabList;
	/** ���α׷��� section���� ���α׷��� �����ϴ� ����*/
	ArrayList<TokenTable> tokenList;
	/** 
	 * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����.   
	 * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
	 */
	ArrayList<String> codeList;
	
	/**
	 * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
	 * 
	 * @param instFile : instruction ���� �ۼ��� ���� �̸�. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		literaltabList = new ArrayList<LiteralTable>();
		tokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
	}

	/** 
	 * ������� ���� ��ƾ
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler(System.getProperty("user.dir")+"/src/inst.data");
		assembler.loadInputFile(System.getProperty("user.dir")+"/src/input.txt");
		assembler.pass1();

		assembler.printSymbolTable(System.getProperty("user.dir")+"/src/symtab_20150323");
		assembler.printLiteralTable(System.getProperty("user.dir")+"/src/literaltab_20150323");
		assembler.pass2();
		assembler.printObjectCode("output_20150323");
	}

	/**
	 * inputFile�� �о�鿩�� lineList�� �����Ѵ�.
	 * @param inputFile : input ���� �̸�.
	 */
	private void loadInputFile(String inputFile) {
		try {
			File file = new File(inputFile);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line = "";
			while((line = bufReader.readLine())!=null){
				lineList.add(line);
			}
			bufReader.close();
			fileReader.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	
	/**
	 * ����� ���ڿ��� �Ľ��ϰ� �ּҸ� ������ִ� ������ �Ѵ�. EQU ��ɾ� ����
	 * �� �Լ��� locCtr�� �ʿ�� ���� �ʴ´�. EQU�� �ܺ����� ���̺��� �������� �ʴ´�. ������Ʈ �ڵ带 �������� �ʱ� �����̴�.
	 * ���� EQU�� operand�� �����ϴ� ��� label�� ��� extdef �Ǿ� �־�� �Ѵ�.
	 * EQU�� label�� �ּҸ� ����ϱ� ������ pass1������ ����.
	 * @param str �Ľ��ϰ��� �ϴ� ���ڿ�
	 * @param symtab Ž���� �ش� ������ �ɺ����̺�
	 */
	private int calculateEQUParser(String str, SymbolTable symtab) {
		int result = 0;
		char mark = 0;
		
		/*������ ó�� ��ȣ�� ����.*/
		if(str.charAt(0)=='-') {
			mark = str.charAt(0);
			str = str.substring(1, str.length());
		}
		else mark = '+';
		
		/*���������� �Ľ��Ѵ�. +�� -�� �ȴ�.*/
		for(int i = 0; i<str.length(); i++) {
			if(str.charAt(i)=='+') {
				int tmpAddr = symtab.search(str.split("+")[0]);
				if(tmpAddr >= 0) {
					if(mark == '+') result += tmpAddr;
					else result -= tmpAddr;
					str = str.split("+")[1];
					mark = '+';
					i = -1;
					continue;
				}
			}
			else if(str.charAt(i)=='-') {
				int tmpAddr = symtab.search(str.split("-")[0]);
				if(tmpAddr >= 0) {
					if(mark == '+') result += tmpAddr;
					else result -= tmpAddr;
					str = str.split("-")[1];
					mark = '-';
					i = -1;
					continue;
				}
			}
		}
		/*�������� ��ȣ�� ����.*/
		int tmpAddr = symtab.search(str);
		if(tmpAddr >= 0) {
			if(mark == '+') result += tmpAddr;
			else result -= tmpAddr;
		}
		return result;
	}
	
	/**
	 * �������� �������� �Ǵ� �� �˸��� �ּұ��̸� �����Ѵ�.
	 * @param �ּұ��̸� ���ϰ��� �ϴ� ���ڿ�
	 */
	private int literalAddrLength(String str) {
		int result = 0;
		for(int i = 0; i < str.length(); i++) {
			if(!Character.isDigit(str.charAt(i))) {
				result += 2;
			}
			else result += 1;
		}
		return result /2;
	}

	/** 
	 * pass1 ������ �����Ѵ�.
	 *   1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����
	 *   2) label�� symbolTable�� ����
	 *   
	 *    ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
	 */
	private void pass1() {
		int locCtr = 0;
		int sectionCount = -1;	//tokenlist, literaltablist, symboltablist�� section index
		int tokentabCount = 0;	//�� section�� tokentable temp index
		for(String inputLine: lineList) {
			
			String[] inputLineArr = inputLine.split("\t");
			
			/*���ο� ������ ���۵� �� �ʱ�ȭ: symboltable�� literaltable�� ���� ������ֱ�*/
			if(inputLineArr.length > 1 && (inputLineArr[1].equals("CSECT")||inputLineArr[1].equals("START")||inputLineArr[1].equals("END"))) {
				
				/*CSECT�� END�϶��� �ش� ������ ���� ���ͷ��� ó���� �����Ѵ�.*/
				if(inputLineArr[1].equals("CSECT")||inputLineArr[1].equals("END")) {
					for(int i = 0; i < tokenList.get(sectionCount).literalTab.literalList.size(); i++) {
						if(tokenList.get(sectionCount).literalTab.locationList.get(i) == 0) {
							tokenList.get(sectionCount).literalTab.locationList.set(i, locCtr);
							tokenList.get(sectionCount).putToken("\t'"+tokenList.get(sectionCount).literalTab.literalList.get(i)+"'", locCtr);
							tokentabCount++;
							locCtr+=literalAddrLength(tokenList.get(sectionCount).literalTab.literalList.get(i));
						}
					}
				}
				
				/*END��� ���ͷ� �۾��� ó���ϰ� �������´�.*/
				if(inputLineArr[1].equals("END")) break;
				
				sectionCount++;
				symtabList.add(new SymbolTable());
				literaltabList.add(new LiteralTable());
				tokenList.add(new TokenTable(symtabList.get(sectionCount), literaltabList.get(sectionCount), instTable));
				tokentabCount=0;
				locCtr = 0;	
			}
			
			tokenList.get(sectionCount).putToken(inputLine, locCtr);
			
			/*symboltable �ۼ�*/
			if(tokenList.get(sectionCount).getToken(tokentabCount).label!=null 
					&& !tokenList.get(sectionCount).getToken(tokentabCount).label.equals("")) {
				/*EQU�� �ִ� �� ���� �� Ȯ���ؾ��Ѵ�.*/
				if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("EQU")
						&& !tokenList.get(sectionCount).getToken(tokentabCount).operand[0].equals("*")) {
					/*���� �ּҸ� �����ϴ� ���� �ƴ� ��� ���ڿ� �Ľ��ؼ� ����� ���ش�.*/
					symtabList.get(sectionCount).putSymbol(
							tokenList.get(sectionCount).getToken(tokentabCount).label,
							calculateEQUParser(tokenList.get(sectionCount).getToken(tokentabCount).operand[0], tokenList.get(sectionCount).symTab));
				}
				else symtabList.get(sectionCount).putSymbol(tokenList.get(sectionCount).getToken(tokentabCount).label, locCtr);
				//symboltabCount++;
			}
			
			/*literaltable �ۼ�*/
			if(tokenList.get(sectionCount).getToken(tokentabCount).operand[0]!=null 
					&& tokenList.get(sectionCount).getToken(tokentabCount).operand[0].contains("=")) {
				literaltabList.get(sectionCount).putLiteral(tokenList.get(sectionCount).getToken(tokentabCount).operand[0].split("'")[1]);
				//literaltabCount++;
			}
			
			/*location counter �˸°� �����ֱ�*/
			Instruction instruction = instTable.searchInstTable(tokenList.get(sectionCount).getToken(tokentabCount).operator);
			if(instruction != null) {
				if(tokenList.get(sectionCount).getToken(tokentabCount).operator.contains("+")) locCtr += 4;
				else if(instruction.type.contains(3)) locCtr += 3;
				else if(instruction.type.contains(2)) locCtr += 2;
				else if(instruction.type.contains(1)) locCtr += 1;
				
			}
			/*instruction�� �˻��� �ȵ� ��� BYTE, WORD, RESB, RESW, LTORG�� �ִ� �� �����Ѵ�.*/
			else if (tokenList.get(sectionCount).getToken(tokentabCount).operator != null){
				if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("BYTE")) locCtr +=1;
				else if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("WORD")) locCtr +=3;
				else if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("RESW"))
					locCtr += Integer.parseInt(tokenList.get(sectionCount).getToken(tokentabCount).operand[0])*3;
				else if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("RESB"))
					locCtr += Integer.parseInt(tokenList.get(sectionCount).getToken(tokentabCount).operand[0]);
				else if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("LTORG")) {
					for(int i = 0; i < tokenList.get(sectionCount).literalTab.literalList.size(); i++) {
						tokenList.get(sectionCount).literalTab.locationList.set(i, locCtr);
						tokenList.get(sectionCount).putToken("\t'"+tokenList.get(sectionCount).literalTab.literalList.get(i)+"'", locCtr);
						tokentabCount++;
						locCtr+=literalAddrLength(tokenList.get(sectionCount).literalTab.literalList.get(i));
					}
				}
			}
			tokentabCount++;
		}
	}
	
	/**
	 * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printSymbolTable(String fileName) {
		try {
			File file = new File(fileName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			if(file.isFile() && file.canWrite()) {
				for(SymbolTable symtab: symtabList) {
					for(int i = 0; i < symtab.symbolList.size(); i++) {
						bw.write(symtab.symbolList.get(i));
						bw.write("\t");
						bw.write(Integer.toHexString(symtab.locationList.get(i)).toUpperCase());
						bw.write("\n");
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �ۼ��� LiteralTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printLiteralTable(String fileName) {
		try {
			File file = new File(fileName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			if(file.isFile() && file.canWrite()) {
				for(LiteralTable literaltab: literaltabList) {
					for(int i = 0; i < literaltab.literalList.size(); i++) {
						bw.write(literaltab.literalList.get(i));
						bw.write("\t");
						bw.write(Integer.toHexString(literaltab.locationList.get(i)).toUpperCase());
						bw.write("\n");
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * pass2 ������ �����Ѵ�.
	 *   1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
	 */
	private void pass2() {
		for(TokenTable tokenTable: tokenList) {
			tokenTable.setNixbpe();
			for(int i = 0; i < tokenTable.tokenList.size(); i++) {
				if(tokenTable.tokenList.get(i).operator!=null){
					tokenTable.makeObjectCode(i);
				}
			}
		}
	}
	
	/**
	 * �ش� ����(���α׷�)�� ��ü ũ�⸦ �����ϴ� �Լ�
	 */
	private int programSize(TokenTable tokenTable) {
		int result = 0;
		int resultIndex = 0;
		for(int i = 0; i < tokenTable.tokenList.size(); i++) {
			if(tokenTable.getToken(i).location > result){
				result = tokenTable.getToken(i).location;
				resultIndex = i;
			}
		}
		/*location �ִ밪�� ã�� �� ���� resultIndex�� ���� �ּҸ� ������ �κ��� üũ�Ѵ�.*/
		result += tokenTable.getToken(resultIndex).byteSize;
		return result;
	}
	

	
	/**
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printObjectCode(String fileName) {
	int startAddr = 0;	//�̰� �������� ���� �� �ִ� ���� �ּ�
	int programStartAddr = 0;	//�̰� ���α׷� �����ּҷ� �����ּ�.
	int sectionCount = 0;	//ù��° E�� ����ϱ� ���ؼ�
		try {
			File file = new File(fileName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			if(file.isFile() && file.canWrite()) {
				for(TokenTable tokenTable: tokenList) {
					startAddr = 0;
					bw.write("\nH" + tokenTable.getToken(0).label+"\t"
								+String.format("%06X", startAddr)+String.format("%06X", programSize(tokenTable)));
					
					for(int i = 1; i < tokenTable.tokenList.size(); i++) {
						/*������ ��ġ�� �������� ���� �� �ֱ� ������ for������ ��� �˻��ϴ� ���ۿ� ����. �̸� ���� �߰����� ������ �Ҵ��ϱ� �ȴ�.*/
						/*EXTDEFó��*/
						if(tokenTable.getToken(i).operator!=null && tokenTable.getToken(i).operator.equals("EXTDEF")) {
							bw.write("\nD");
							for(int t = 0; t < tokenTable.getToken(i).operand.length; t++) {
								if(tokenTable.getToken(i).operand[t]!=null) {
									bw.write(tokenTable.getToken(i).operand[t]);
									/*���⼭�� ������ �����ؾ��ϸ� �������� �ʴ´ٸ� ������ �ִ� ���� �´�.*/
									bw.write(String.format("%06X", tokenTable.symTab.search(tokenTable.getToken(i).operand[t])));
								}
							}
						}
						/*EXTREFó��*/
						if(tokenTable.getToken(i).operator!=null && tokenTable.getToken(i).operator.equals("EXTREF")) {
							bw.write("\nR");
							for(int t = 0; t < tokenTable.getToken(i).operand.length; t++) {
								if(tokenTable.getToken(i).operand[t]!=null) {
									bw.write(tokenTable.getToken(i).operand[t]);
								}
							}
						}
						/*�ڵ�ó��...�������� ������ �� ����Ѵ�.*/
						if(tokenTable.getToken(i).byteSize!=0) {
							bw.write("\nT");
							String tLine = "";
							int tLength = 0;
							while(i<tokenTable.tokenList.size()) {
								tLength+=tokenTable.getToken(i).byteSize;
								if(tLength> 28 || tokenTable.getToken(i).byteSize == 0) {
									if(tokenTable.getToken(i).objectCode!=null)
										tLine += tokenTable.getToken(i).objectCode;
									bw.write(String.format("%06X", startAddr));
									bw.write(String.format("%02X", tLength));
									bw.write(tLine);
									startAddr += tLength;
									tLength = 0;
									break;
								}
								else tLine += tokenTable.getToken(i).objectCode;
								i++;
							}
							/*�����ִ� �ڵ尡 �ִٸ� ���*/
							if(tLength!=0) {
								bw.write(String.format("%06X", startAddr));
								bw.write(String.format("%02X", tLength));
								bw.write(tLine);
								startAddr += tLength;
							}
						}
						else {startAddr = tokenTable.getToken(i).location;}
					}
					/*���� �ܺ����� ���̺��� ���������*/
					for(ExtRef extRef : tokenTable.extRefTab.extRefList) {
						bw.write("\nM"+extRef.pointAddr+String.format("%02X", extRef.size)+extRef.operator+extRef.label);
					}
					bw.write("\nE");
					if(sectionCount==0) bw.write(String.format("%06X", programStartAddr));
					sectionCount++;
				}
			}
			bw.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
}
