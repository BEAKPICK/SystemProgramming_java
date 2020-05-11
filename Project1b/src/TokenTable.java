import java.util.ArrayList;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	LiteralTable literalTab;
	InstTable instTab;
	/*이것은 토큰테이블 내부에서 필요한 테이블이므로 링크가 필요없다.*/
	ExtRefTable extRefTab;
	
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList;
	
	/**
	 * 초기화하면서 symTable과 instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param instTab : instruction 명세가 정의된 instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		this.symTab = symTab;
		this.instTab = instTab;
	}

	/**
	 * 초기화하면서 literalTable과 instTable을 링크시킨다.
	 * @param literalTab : 해당 section과 연결되어있는 literal table
	 * @param instTab : instruction 명세가 정의된 instTable
	 */
	public TokenTable(LiteralTable literalTab, InstTable instTab) {
		this.literalTab = literalTab;
		this.instTab = instTab;
	}
	
	/**
	 * 초기화하면서 symTable과 literalTable 그리고 instTable을 링크시킨다.
	 * 생성자가 왜 따로 되어있는 지 이유를 모르겠다...
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param literalTab : 해당 section과 연결되어있는 literal table
	 * @param instTab : instruction 명세가 정의된 instTable
	 */
	public TokenTable(SymbolTable symTab, LiteralTable literalTab, InstTable instTab) {
		tokenList = new ArrayList<>();
		this.symTab = symTab;
		this.literalTab = literalTab;
		this.instTab = instTab;
		this.extRefTab = new ExtRefTable();
	}
	
	/**
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line));
	}
	
	/**
	 * location도 같이 저장하기 위한 putToken
	 * @param line : 분리되지 않은 일반 문자열
	 * @param location : 해당 토큰의 주소
	 */
	public void putToken(String line, int location) {
		tokenList.add(new Token(line, location));
	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * 레지스터 번호를 리턴한다. 해당사항 없다면 -1을 리턴한다.
	 * @param : register 레지스터 문자
	 */
	public int getRegisterNum(char register) {
		switch (register)
		{
		case'X':
			return 1;
		case'A':
			return 0;
		case'S':
			return 4;
		case'T':
			return 5;
		default:
			return -1;
		}
	}
	
	/**
	 * nixbpe를 설정한다. 3형식 이상만 nixbpe를 가질 수 있다.
	 */
	public void setNixbpe() {
		for(Token token: this.tokenList) {
			/*3형식 이상만 세팅한다.*/
			if(token.operator!=null && this.instTab.searchInstTable(token.operator)!=null 
					&& this.instTab.searchInstTable(token.operator).type.contains(3)) {
				/*operator에 +가 있다면 4형식이므로 e를 설정해준다. 이때  location counter를 사용하지 않는다.*/
				if(token.operator.contains("+")) token.setFlag(TokenTable.eFlag, 1);
				
				/*operand에 #이 있다면 바로 nixbpe의 n,i를 설정해준다.*/
				if(token.operand[0]!=null && token.operand[0].contains("#")) {
					token.setFlag(TokenTable.nFlag, 0);
					token.setFlag(TokenTable.iFlag, 1);
					continue;
				}
				else if(token.operand[0]!=null && token.operand[0].contains("@")) {
					token.setFlag(TokenTable.nFlag, 1);
					token.setFlag(TokenTable.iFlag, 0);
				}
				else{
					token.setFlag(TokenTable.nFlag, 1);
					token.setFlag(TokenTable.iFlag, 1);
				}
				/*control section은 base register를 사용하지 않는다.*/
				if(token.getFlag(TokenTable.eFlag)!=TokenTable.eFlag) token.setFlag(TokenTable.pFlag, 1);
				/*항상 RSUB의 존재를 잊지 말자.*/
				if(token.operand[0]==null) token.setFlag(TokenTable.pFlag, 0);
				/*Loop가 있다면 X도 설정해준다. X의 위치는 무조건 두번째 operand자리에 있다고 가정한다.*/
				if(token.operand[1]!=null && token.operand[1].equals("X")) token.setFlag(TokenTable.xFlag, 1);
			}
		}
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table literal table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	 * @param index
	 */
	public void makeObjectCode(int index){
		int intObjectCode = 0;
		
		/*토큰 테이블에 들어간 리터럴에 ''기호가 있으므로(그렇게 유도함.) 그것을 찾아 오브젝트 코드로 변환을 해준다.*/
		if(this.getToken(index).operator.contains("'")) {
			String trimmedLiteralOperator = this.getToken(index).operator.substring(1,this.getToken(index).operator.length()-1);
			int shift = 0;
			float bytesizeForNum = 0;
			for(int i = trimmedLiteralOperator.length()-1; i>=0; i--) {
				if(!Character.isDigit(trimmedLiteralOperator.charAt(i))){
					intObjectCode += (int)trimmedLiteralOperator.charAt(i) << shift;
					this.getToken(index).byteSize += 1;
					shift += 8;
				}
				else {
					intObjectCode += Character.getNumericValue(trimmedLiteralOperator.charAt(i)) << shift;
					bytesizeForNum += 0.5;
					shift += 4;
				}
			}
			this.getToken(index).nixbpe = 0;
			this.getToken(index).byteSize += (int)Math.ceil(bytesizeForNum); 
			this.getToken(index).objectCode = trimmedHexString(this.getToken(index), intObjectCode);
			return;
		}
		/*operator가 byte일때도 오브젝트 코드는 필요하다...*/
		else if(this.getToken(index).operator.equals("BYTE")) {
			this.getToken(index).byteSize = 1;
			/*BYTE의 operand에 인자가 없다면 그것은 바로 에러 따라서 null검사하지 않는다.*/
			this.getToken(index).objectCode = this.getToken(index).operand[0].split("'")[1];
			this.getToken(index).nixbpe = 0;
			return;
		}
		/*역시 WORD일때도 기본적인 설정이 필요하다...*/
		else if(this.getToken(index).operator.equals("WORD")) {
			this.getToken(index).byteSize = 3;
			this.getToken(index).nixbpe = 0;
			this.getToken(index).objectCode = TokenTable.trimmedHexString(this.getToken(index),this.extRefTab.calculateParser(this.getToken(index), this.symTab));
			return;
		}
		/*그밖에 object code 필요없는 녀석들은 전부 리턴시켜버린다.*/
		else if(this.getToken(index).operator.equals("RESW")
				||this.getToken(index).operator.equals("RESB")
				||this.getToken(index).operator.equals("EQU")) return;
		
		/*리터럴 처리가 아닌 경우 정규 instruction을 체크한다.*/
		/*byte size를 설정한다.*/
		//Instruction tmpInstruction = this.instTab.searchInstTable(this.getToken(index).operator);
		Instruction inst = instTab.searchInstTable(tokenList.get(index).operator);
		if(this.getToken(index).operator.contains("+")) this.getToken(index).byteSize = 4;
		else if(inst!=null) {
			if(inst.type.contains(3))this.getToken(index).byteSize = 3;
			else if(inst.type.contains(2))this.getToken(index).byteSize = 2;
			else if(inst.type.contains(1))this.getToken(index).byteSize = 1;
		}
		//else this.getToken(index).nixbpe = 0;
		
		/*opCode처리, nixbpe처리*/
		if(inst!=null) {
			if(this.getToken(index).byteSize == 4) {
				intObjectCode = Integer.parseInt(inst.opCode, 16) << 24;
				intObjectCode += this.getToken(index).nixbpe << 20;
			}
			else if(this.getToken(index).byteSize == 3){
				intObjectCode = Integer.parseInt(inst.opCode, 16) << 16;
				intObjectCode += this.getToken(index).nixbpe << 12;
			}
			else if(this.getToken(index).byteSize == 2) {
				/*2형식은 nixbpe가 없다.*/
				intObjectCode = Integer.parseInt(inst.opCode, 16) << 8;
				int shift = 4;
				for(int i = 0; i < this.getToken(index).operand.length; i++) {
					if(this.getToken(index).operand[i]!=null) {
						int registerNum = getRegisterNum(this.getToken(index).operand[i].charAt(0));
						if(registerNum >= 0) {
							intObjectCode += registerNum << shift;
							shift -= 4;
						}
					}
				}
			}
			else if(this.getToken(index).byteSize == 1) {
				intObjectCode = Integer.parseInt(inst.opCode, 16);
			}
		}
		/*displacement처리*/
		/*indirect addressing : n=1, i=0은 operand에 무조건 @이 있다.*/
		if(this.getToken(index).getFlag(nFlag) == nFlag && this.getToken(index).getFlag(iFlag) != iFlag) {
			String trimmedOperand = this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length());
			/*바이트 사이즈에 따라 마스킹해야되는 범위가 다르다.*/
			if(this.getToken(index).getFlag(eFlag)==eFlag) {
				/*int형이므로 반드시 연산 후 마스킹 처리를 해줘야한다.*/
				intObjectCode += (this.symTab.search(trimmedOperand) - this.getToken(index+1).location) & 1048575;
			}
			else {
				/*int형이므로 반드시 연산 후 마스킹 처리를 해줘야한다.*/
				intObjectCode += (this.symTab.search(trimmedOperand) - this.getToken(index+1).location) & 4095;
			}
		}
		/*immediate addressing : n=0, i=1은 operand에 무조건 #이 있다.*/
		else if(this.getToken(index).getFlag(nFlag) != nFlag && this.getToken(index).getFlag(iFlag) == iFlag) {
			/*바이트 사이즈에 따라 마스킹해야되는 범위가 다르다.*/
			if(this.getToken(index).getFlag(eFlag)==eFlag) {
				/*int형이므로 반드시 연산 후 마스킹 처리를 해줘야한다.*/
				intObjectCode += Integer.parseInt(this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length())) & 1048575;
			}
			else {
				intObjectCode += Integer.parseInt(this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length())) & 4095;
			}
		}
		/*simple addressing : n=1, i=1*/
		else if(this.getToken(index).getFlag(nFlag) == nFlag && this.getToken(index).getFlag(iFlag) == iFlag){
			int targetAddress = 0;
			/*리터럴 참조를 만났을 때 해당 리터럴의 주소를 리턴하도록 한다.*/
			if(this.getToken(index).operand[0]!=null && this.getToken(index).operand[0].contains("=")) {
				targetAddress = this.literalTab.search(this.getToken(index).operand[0].split("'")[1]);
			}
			/*아닐 경우, 심볼테이블을 찾는다.*/
			//else targetAddress = this.symTab.search(this.getToken(index).operand[0]);
			else targetAddress = this.extRefTab.calculateParser(this.getToken(index), this.symTab);
			
			if(targetAddress >= 0) {
				/*바이트 사이즈에 따라 마스킹해야되는 범위가 다르다.*/
				if(this.getToken(index).getFlag(pFlag)==pFlag) {
					if(this.getToken(index).getFlag(eFlag)==eFlag) {
						/*int형이므로 반드시 연산 후 마스킹 처리를 해줘야한다.*/
						intObjectCode += (targetAddress - this.getToken(index+1).location) & 1048575;
					}
					else {
						intObjectCode += (targetAddress - this.getToken(index+1).location) & 4095;
					}
				}
			}
		}
		this.getToken(index).objectCode = trimmedHexString(this.getToken(index), intObjectCode);
	}
	
	/** 
	 * index번호에 해당하는 object code를 리턴한다.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}
	
	/**
	 * byte size에 맞춰 올바르게 출력할 수 있도록 String을 리턴하는 함수
	 * @param token이 필요
	 */
	public static String trimmedHexString(Token token, int target) {
		if(token.byteSize == 4) return String.format("%08X", target);
		else if (token.byteSize == 3) return String.format("%06X", target);
		else if (token.byteSize == 2) return String.format("%04X", target);
		else return String.format("%02X", target);
	}
}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code 생성 단계에서 사용되는 변수들 
	String objectCode;
	int byteSize;
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line) {
		//initialize 추가
		this.operand = new String[TokenTable.MAX_OPERAND];
		parsing(line);
	}
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 실시하고 location을 저장한다.
	 * @param line
	 * @param location
	 */
	public Token(String line, int location) {
		//initialize 추가
		this.operand = new String[TokenTable.MAX_OPERAND];
		this.location = location;
		parsing(line);
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * (nixbpe도 설정한다.)
	 * @param line 문장단위로 저장된 프로그램 코드.
	 */
	public void parsing(String line) {
		String[] parseArray = line.split("\t");
		if(parseArray[0].equals(".")) return;
		
		this.label = parseArray[0];
		this.operator = parseArray[1];
		
		
		int i = 0;
		
		
		/*type이 3,4여도 operand가 존재하지 않을 수 있고...인간은 같은 실수를 반복한다...*/
		if(parseArray.length >= 3 && !parseArray[2].equals("")) {
			/*이건 operand가 존재할 때 파싱을 하는 것*/
			for(String operands: parseArray[2].split(",")) {
				this.operand[i] = operands;
				i++;
			}
		}
		/*마지막(comment)은 존재하지 않을 수 있다.*/
		if(parseArray.length >= 4 && !parseArray[3].equals("")) this.comment = parseArray[3];
	}
	
	/** 
	 * n,i,x,b,p,e flag를 설정한다. 
	 * 
	 * 사용 예 : setFlag(nFlag, 1); 
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
	 */
	public void setFlag(int flag, int value) {
		if(flag == TokenTable.nFlag) {
			if(value == 0 && getFlag(TokenTable.nFlag) == TokenTable.nFlag) this.nixbpe -= TokenTable.nFlag;
			else this.nixbpe |= (value * TokenTable.nFlag);
		}
		else if(flag == TokenTable.iFlag) {
			if(value == 0 && getFlag(TokenTable.iFlag) == TokenTable.iFlag) this.nixbpe -= TokenTable.iFlag; 
			else this.nixbpe |= (value * TokenTable.iFlag);
		}
		else if(flag == TokenTable.xFlag) {
			if(value == 0 && getFlag(TokenTable.xFlag) == TokenTable.xFlag) this.nixbpe -= TokenTable.xFlag;
			else this.nixbpe |= (value * TokenTable.xFlag);
		}
		else if(flag == TokenTable.bFlag) {
			if(value == 0 && getFlag(TokenTable.bFlag) == TokenTable.bFlag) this.nixbpe -= TokenTable.bFlag; 
			else this.nixbpe |= (value * TokenTable.bFlag);
		}
		else if(flag == TokenTable.pFlag) {
			if(value == 0 && getFlag(TokenTable.pFlag) == TokenTable.pFlag) this.nixbpe -= TokenTable.pFlag; 
			else this.nixbpe |= (value * TokenTable.pFlag);
		}
		else if(flag == TokenTable.eFlag) {
			if(value == 0 && getFlag(TokenTable.eFlag) == TokenTable.eFlag) this.nixbpe -= TokenTable.eFlag; 
			else this.nixbpe |= (value * TokenTable.eFlag);
		}
	}
	
	/**
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 
	 * 
	 * 사용 예 : getFlag(nFlag)
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
	 */
	public int getFlag(int flags) {
		return this.nixbpe & flags;
	}
}
