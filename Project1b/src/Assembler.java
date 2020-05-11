import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Assembler : 
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. 
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. 
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. 
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) 
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) 
 * 
 * 
 * 작성중의 유의사항 : 
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)
 * 
 *     
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	/** instruction 명세를 저장한 공간 */
	InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 literal table을 저장하는 공간*/
	ArrayList<LiteralTable> literaltabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<TokenTable> tokenList;
	/** 
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간.   
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	ArrayList<String> codeList;
	
	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 * 
	 * @param instFile : instruction 명세를 작성한 파일 이름. 
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
	 * 어셈블러의 메인 루틴
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
	 * inputFile을 읽어들여서 lineList에 저장한다.
	 * @param inputFile : input 파일 이름.
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
	 * 연산식 문자열을 파싱하고 주소를 계산해주는 역할을 한다. EQU 명령어 전용
	 * 이 함수는 locCtr를 필요로 하지 않는다. EQU는 외부참조 테이블을 생성하지 않는다. 오브젝트 코드를 생성하지 않기 때문이다.
	 * 따라서 EQU의 operand에 존재하는 모든 label은 모두 extdef 되어 있어야 한다.
	 * EQU는 label의 주소만 계산하기 때문에 pass1에서만 동작.
	 * @param str 파싱하고자 하는 문자열
	 * @param symtab 탐색할 해당 섹션의 심볼테이블
	 */
	private int calculateEQUParser(String str, SymbolTable symtab) {
		int result = 0;
		char mark = 0;
		
		/*인자의 처음 기호를 본다.*/
		if(str.charAt(0)=='-') {
			mark = str.charAt(0);
			str = str.substring(1, str.length());
		}
		else mark = '+';
		
		/*본격적으로 파싱한다. +와 -만 된다.*/
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
		/*마지막은 기호가 없다.*/
		int tmpAddr = symtab.search(str);
		if(tmpAddr >= 0) {
			if(mark == '+') result += tmpAddr;
			else result -= tmpAddr;
		}
		return result;
	}
	
	/**
	 * 문자인지 숫자인지 판단 후 알맞은 주소길이를 리턴한다.
	 * @param 주소길이를 구하고자 하는 문자열
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
	 * pass1 과정을 수행한다.
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성
	 *   2) label을 symbolTable에 정리
	 *   
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private void pass1() {
		int locCtr = 0;
		int sectionCount = -1;	//tokenlist, literaltablist, symboltablist의 section index
		int tokentabCount = 0;	//각 section의 tokentable temp index
		for(String inputLine: lineList) {
			
			String[] inputLineArr = inputLine.split("\t");
			
			/*새로운 섹션이 시작될 때 초기화: symboltable과 literaltable을 새로 만들어주기*/
			if(inputLineArr.length > 1 && (inputLineArr[1].equals("CSECT")||inputLineArr[1].equals("START")||inputLineArr[1].equals("END"))) {
				
				/*CSECT나 END일때는 해당 섹션의 남은 리터럴의 처리를 진행한다.*/
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
				
				/*END라면 리터럴 작업만 처리하고 빠져나온다.*/
				if(inputLineArr[1].equals("END")) break;
				
				sectionCount++;
				symtabList.add(new SymbolTable());
				literaltabList.add(new LiteralTable());
				tokenList.add(new TokenTable(symtabList.get(sectionCount), literaltabList.get(sectionCount), instTable));
				tokentabCount=0;
				locCtr = 0;	
			}
			
			tokenList.get(sectionCount).putToken(inputLine, locCtr);
			
			/*symboltable 작성*/
			if(tokenList.get(sectionCount).getToken(tokentabCount).label!=null 
					&& !tokenList.get(sectionCount).getToken(tokentabCount).label.equals("")) {
				/*EQU가 있는 지 없는 지 확인해야한다.*/
				if(tokenList.get(sectionCount).getToken(tokentabCount).operator.equals("EQU")
						&& !tokenList.get(sectionCount).getToken(tokentabCount).operand[0].equals("*")) {
					/*현재 주소를 저장하는 것이 아닌 경우 문자열 파싱해서 계산을 해준다.*/
					symtabList.get(sectionCount).putSymbol(
							tokenList.get(sectionCount).getToken(tokentabCount).label,
							calculateEQUParser(tokenList.get(sectionCount).getToken(tokentabCount).operand[0], tokenList.get(sectionCount).symTab));
				}
				else symtabList.get(sectionCount).putSymbol(tokenList.get(sectionCount).getToken(tokentabCount).label, locCtr);
				//symboltabCount++;
			}
			
			/*literaltable 작성*/
			if(tokenList.get(sectionCount).getToken(tokentabCount).operand[0]!=null 
					&& tokenList.get(sectionCount).getToken(tokentabCount).operand[0].contains("=")) {
				literaltabList.get(sectionCount).putLiteral(tokenList.get(sectionCount).getToken(tokentabCount).operand[0].split("'")[1]);
				//literaltabCount++;
			}
			
			/*location counter 알맞게 더해주기*/
			Instruction instruction = instTable.searchInstTable(tokenList.get(sectionCount).getToken(tokentabCount).operator);
			if(instruction != null) {
				if(tokenList.get(sectionCount).getToken(tokentabCount).operator.contains("+")) locCtr += 4;
				else if(instruction.type.contains(3)) locCtr += 3;
				else if(instruction.type.contains(2)) locCtr += 2;
				else if(instruction.type.contains(1)) locCtr += 1;
				
			}
			/*instruction이 검색이 안될 경우 BYTE, WORD, RESB, RESW, LTORG가 있는 지 봐야한다.*/
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
	 * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
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
	 * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
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
	 * pass2 과정을 수행한다.
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
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
	 * 해당 섹션(프로그램)의 전체 크기를 리턴하는 함수
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
		/*location 최대값을 찾은 뒤 이제 resultIndex를 통해 주소를 더해줄 부분을 체크한다.*/
		result += tokenTable.getToken(resultIndex).byteSize;
		return result;
	}
	

	
	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) {
	int startAddr = 0;	//이건 언제든지 변할 수 있는 동적 주소
	int programStartAddr = 0;	//이건 프로그램 시작주소로 정적주소.
	int sectionCount = 0;	//첫번째 E를 출력하기 위해서
		try {
			File file = new File(fileName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			if(file.isFile() && file.canWrite()) {
				for(TokenTable tokenTable: tokenList) {
					startAddr = 0;
					bw.write("\nH" + tokenTable.getToken(0).label+"\t"
								+String.format("%06X", startAddr)+String.format("%06X", programSize(tokenTable)));
					
					for(int i = 1; i < tokenTable.tokenList.size(); i++) {
						/*정해진 위치에 존재하지 않을 수 있기 때문에 for문에서 계속 검색하는 수밖에 없다. 이를 위해 추가적인 변수를 할당하기 싫다.*/
						/*EXTDEF처리*/
						if(tokenTable.getToken(i).operator!=null && tokenTable.getToken(i).operator.equals("EXTDEF")) {
							bw.write("\nD");
							for(int t = 0; t < tokenTable.getToken(i).operand.length; t++) {
								if(tokenTable.getToken(i).operand[t]!=null) {
									bw.write(tokenTable.getToken(i).operand[t]);
									/*여기서는 무조건 존재해야하며 존재하지 않는다면 에러가 있는 것이 맞다.*/
									bw.write(String.format("%06X", tokenTable.symTab.search(tokenTable.getToken(i).operand[t])));
								}
							}
						}
						/*EXTREF처리*/
						if(tokenTable.getToken(i).operator!=null && tokenTable.getToken(i).operator.equals("EXTREF")) {
							bw.write("\nR");
							for(int t = 0; t < tokenTable.getToken(i).operand.length; t++) {
								if(tokenTable.getToken(i).operand[t]!=null) {
									bw.write(tokenTable.getToken(i).operand[t]);
								}
							}
						}
						/*코드처리...시작점을 잡으면 쭉 출력한다.*/
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
							/*남아있는 코드가 있다면 출력*/
							if(tLength!=0) {
								bw.write(String.format("%06X", startAddr));
								bw.write(String.format("%02X", tLength));
								bw.write(tLine);
								startAddr += tLength;
							}
						}
						else {startAddr = tokenTable.getToken(i).location;}
					}
					/*이제 외부참조 테이블을 출력해주자*/
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
