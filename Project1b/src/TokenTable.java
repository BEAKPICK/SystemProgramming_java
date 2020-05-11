import java.util.ArrayList;

/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	LiteralTable literalTab;
	InstTable instTab;
	/*�̰��� ��ū���̺� ���ο��� �ʿ��� ���̺��̹Ƿ� ��ũ�� �ʿ����.*/
	ExtRefTable extRefTab;
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		this.symTab = symTab;
		this.instTab = instTab;
	}

	/**
	 * �ʱ�ȭ�ϸ鼭 literalTable�� instTable�� ��ũ��Ų��.
	 * @param literalTab : �ش� section�� ����Ǿ��ִ� literal table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(LiteralTable literalTab, InstTable instTab) {
		this.literalTab = literalTab;
		this.instTab = instTab;
	}
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� literalTable �׸��� instTable�� ��ũ��Ų��.
	 * �����ڰ� �� ���� �Ǿ��ִ� �� ������ �𸣰ڴ�...
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param literalTab : �ش� section�� ����Ǿ��ִ� literal table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, LiteralTable literalTab, InstTable instTab) {
		tokenList = new ArrayList<>();
		this.symTab = symTab;
		this.literalTab = literalTab;
		this.instTab = instTab;
		this.extRefTab = new ExtRefTable();
	}
	
	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line));
	}
	
	/**
	 * location�� ���� �����ϱ� ���� putToken
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 * @param location : �ش� ��ū�� �ּ�
	 */
	public void putToken(String line, int location) {
		tokenList.add(new Token(line, location));
	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * �������� ��ȣ�� �����Ѵ�. �ش���� ���ٸ� -1�� �����Ѵ�.
	 * @param : register �������� ����
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
	 * nixbpe�� �����Ѵ�. 3���� �̻� nixbpe�� ���� �� �ִ�.
	 */
	public void setNixbpe() {
		for(Token token: this.tokenList) {
			/*3���� �̻� �����Ѵ�.*/
			if(token.operator!=null && this.instTab.searchInstTable(token.operator)!=null 
					&& this.instTab.searchInstTable(token.operator).type.contains(3)) {
				/*operator�� +�� �ִٸ� 4�����̹Ƿ� e�� �������ش�. �̶�  location counter�� ������� �ʴ´�.*/
				if(token.operator.contains("+")) token.setFlag(TokenTable.eFlag, 1);
				
				/*operand�� #�� �ִٸ� �ٷ� nixbpe�� n,i�� �������ش�.*/
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
				/*control section�� base register�� ������� �ʴ´�.*/
				if(token.getFlag(TokenTable.eFlag)!=TokenTable.eFlag) token.setFlag(TokenTable.pFlag, 1);
				/*�׻� RSUB�� ���縦 ���� ����.*/
				if(token.operand[0]==null) token.setFlag(TokenTable.pFlag, 0);
				/*Loop�� �ִٸ� X�� �������ش�. X�� ��ġ�� ������ �ι�° operand�ڸ��� �ִٰ� �����Ѵ�.*/
				if(token.operand[1]!=null && token.operand[1].equals("X")) token.setFlag(TokenTable.xFlag, 1);
			}
		}
	}
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table literal table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){
		int intObjectCode = 0;
		
		/*��ū ���̺� �� ���ͷ��� ''��ȣ�� �����Ƿ�(�׷��� ������.) �װ��� ã�� ������Ʈ �ڵ�� ��ȯ�� ���ش�.*/
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
		/*operator�� byte�϶��� ������Ʈ �ڵ�� �ʿ��ϴ�...*/
		else if(this.getToken(index).operator.equals("BYTE")) {
			this.getToken(index).byteSize = 1;
			/*BYTE�� operand�� ���ڰ� ���ٸ� �װ��� �ٷ� ���� ���� null�˻����� �ʴ´�.*/
			this.getToken(index).objectCode = this.getToken(index).operand[0].split("'")[1];
			this.getToken(index).nixbpe = 0;
			return;
		}
		/*���� WORD�϶��� �⺻���� ������ �ʿ��ϴ�...*/
		else if(this.getToken(index).operator.equals("WORD")) {
			this.getToken(index).byteSize = 3;
			this.getToken(index).nixbpe = 0;
			this.getToken(index).objectCode = TokenTable.trimmedHexString(this.getToken(index),this.extRefTab.calculateParser(this.getToken(index), this.symTab));
			return;
		}
		/*�׹ۿ� object code �ʿ���� �༮���� ���� ���Ͻ��ѹ�����.*/
		else if(this.getToken(index).operator.equals("RESW")
				||this.getToken(index).operator.equals("RESB")
				||this.getToken(index).operator.equals("EQU")) return;
		
		/*���ͷ� ó���� �ƴ� ��� ���� instruction�� üũ�Ѵ�.*/
		/*byte size�� �����Ѵ�.*/
		//Instruction tmpInstruction = this.instTab.searchInstTable(this.getToken(index).operator);
		Instruction inst = instTab.searchInstTable(tokenList.get(index).operator);
		if(this.getToken(index).operator.contains("+")) this.getToken(index).byteSize = 4;
		else if(inst!=null) {
			if(inst.type.contains(3))this.getToken(index).byteSize = 3;
			else if(inst.type.contains(2))this.getToken(index).byteSize = 2;
			else if(inst.type.contains(1))this.getToken(index).byteSize = 1;
		}
		//else this.getToken(index).nixbpe = 0;
		
		/*opCodeó��, nixbpeó��*/
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
				/*2������ nixbpe�� ����.*/
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
		/*displacementó��*/
		/*indirect addressing : n=1, i=0�� operand�� ������ @�� �ִ�.*/
		if(this.getToken(index).getFlag(nFlag) == nFlag && this.getToken(index).getFlag(iFlag) != iFlag) {
			String trimmedOperand = this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length());
			/*����Ʈ ����� ���� ����ŷ�ؾߵǴ� ������ �ٸ���.*/
			if(this.getToken(index).getFlag(eFlag)==eFlag) {
				/*int���̹Ƿ� �ݵ�� ���� �� ����ŷ ó���� ������Ѵ�.*/
				intObjectCode += (this.symTab.search(trimmedOperand) - this.getToken(index+1).location) & 1048575;
			}
			else {
				/*int���̹Ƿ� �ݵ�� ���� �� ����ŷ ó���� ������Ѵ�.*/
				intObjectCode += (this.symTab.search(trimmedOperand) - this.getToken(index+1).location) & 4095;
			}
		}
		/*immediate addressing : n=0, i=1�� operand�� ������ #�� �ִ�.*/
		else if(this.getToken(index).getFlag(nFlag) != nFlag && this.getToken(index).getFlag(iFlag) == iFlag) {
			/*����Ʈ ����� ���� ����ŷ�ؾߵǴ� ������ �ٸ���.*/
			if(this.getToken(index).getFlag(eFlag)==eFlag) {
				/*int���̹Ƿ� �ݵ�� ���� �� ����ŷ ó���� ������Ѵ�.*/
				intObjectCode += Integer.parseInt(this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length())) & 1048575;
			}
			else {
				intObjectCode += Integer.parseInt(this.getToken(index).operand[0].substring(1,this.getToken(index).operand[0].length())) & 4095;
			}
		}
		/*simple addressing : n=1, i=1*/
		else if(this.getToken(index).getFlag(nFlag) == nFlag && this.getToken(index).getFlag(iFlag) == iFlag){
			int targetAddress = 0;
			/*���ͷ� ������ ������ �� �ش� ���ͷ��� �ּҸ� �����ϵ��� �Ѵ�.*/
			if(this.getToken(index).operand[0]!=null && this.getToken(index).operand[0].contains("=")) {
				targetAddress = this.literalTab.search(this.getToken(index).operand[0].split("'")[1]);
			}
			/*�ƴ� ���, �ɺ����̺��� ã�´�.*/
			//else targetAddress = this.symTab.search(this.getToken(index).operand[0]);
			else targetAddress = this.extRefTab.calculateParser(this.getToken(index), this.symTab);
			
			if(targetAddress >= 0) {
				/*����Ʈ ����� ���� ����ŷ�ؾߵǴ� ������ �ٸ���.*/
				if(this.getToken(index).getFlag(pFlag)==pFlag) {
					if(this.getToken(index).getFlag(eFlag)==eFlag) {
						/*int���̹Ƿ� �ݵ�� ���� �� ����ŷ ó���� ������Ѵ�.*/
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
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}
	
	/**
	 * byte size�� ���� �ùٸ��� ����� �� �ֵ��� String�� �����ϴ� �Լ�
	 * @param token�� �ʿ�
	 */
	public static String trimmedHexString(Token token, int target) {
		if(token.byteSize == 4) return String.format("%08X", target);
		else if (token.byteSize == 3) return String.format("%06X", target);
		else if (token.byteSize == 2) return String.format("%04X", target);
		else return String.format("%02X", target);
	}
}

/**
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	String objectCode;
	int byteSize;
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line) {
		//initialize �߰�
		this.operand = new String[TokenTable.MAX_OPERAND];
		parsing(line);
	}
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �ǽ��ϰ� location�� �����Ѵ�.
	 * @param line
	 * @param location
	 */
	public Token(String line, int location) {
		//initialize �߰�
		this.operand = new String[TokenTable.MAX_OPERAND];
		this.location = location;
		parsing(line);
	}
	
	/**
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * (nixbpe�� �����Ѵ�.)
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line) {
		String[] parseArray = line.split("\t");
		if(parseArray[0].equals(".")) return;
		
		this.label = parseArray[0];
		this.operator = parseArray[1];
		
		
		int i = 0;
		
		
		/*type�� 3,4���� operand�� �������� ���� �� �ְ�...�ΰ��� ���� �Ǽ��� �ݺ��Ѵ�...*/
		if(parseArray.length >= 3 && !parseArray[2].equals("")) {
			/*�̰� operand�� ������ �� �Ľ��� �ϴ� ��*/
			for(String operands: parseArray[2].split(",")) {
				this.operand[i] = operands;
				i++;
			}
		}
		/*������(comment)�� �������� ���� �� �ִ�.*/
		if(parseArray.length >= 4 && !parseArray[3].equals("")) this.comment = parseArray[3];
	}
	
	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. 
	 * 
	 * ��� �� : setFlag(nFlag, 1); 
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
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
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� 
	 * 
	 * ��� �� : getFlag(nFlag)
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getFlag(int flags) {
		return this.nixbpe & flags;
	}
}
