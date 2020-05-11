import java.util.ArrayList;

/**
 * �ܺ����� ���̺��̴�. �� ����(���α׷�)�� �Ҵ��� �Ǿ������� ��ũ���� TokenTable���� �����Ǿ�����.
 * @author User
 *
 */
public class ExtRefTable {
	ArrayList<ExtRef> extRefList;
	
	public ExtRefTable() {
		this.extRefList = new ArrayList<>();
	}
	
	public ExtRef getExtRef(int index) {
		return extRefList.get(index);
	}
	
	/**
	 * ����� ���ڿ��� �Ľ��ϰ� �ּҸ� ����� �����ϴ� ������ �Ѵ�.
	 * WORD�� �ܺ����� ���̺��� ������ �� �ִ�. ������Ʈ �ڵ带 �����ϱ� �����̴�.
	 * 
	 * @param token �Ľ��ϰ��� �ϴ� ��ū
	 * @param symtab Ž���� �ش� ������ �ɺ����̺�
	 */
	public int calculateParser(Token token, SymbolTable symtab) {
		int result = 0;
		char mark = 0;
		if(token.operand[0]==null) return -1;
		String str = token.operand[0];
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
				}
				/*�־��� �ɺ����̺��� �� ã�Ҵٸ� �ܺ����� ���̺� �߰��ؾ��Ѵ�.*/
				else {
					if(token.operator.equals("WORD"))
						/*word�� operandũ�� ��ü�� ���� ���*/
						extRefList.add(new ExtRef(String.format("%06X",token.location), (token.byteSize*2), mark, str.split("+")[0]));
					else 
						extRefList.add(new ExtRef(String.format("%06X",token.location+1), (token.byteSize*2) -3, mark, str.split("+")[0]));
				}
				str = str.split("+")[1];
				mark = '+';
				i = -1;
				continue;
			}
			else if(str.charAt(i)=='-') {
				int tmpAddr = symtab.search(str.split("-")[0]);
				if(tmpAddr >= 0) {
					if(mark == '+') result += tmpAddr;
					else result -= tmpAddr;
				}
				else {
					if(token.operator.equals("WORD"))
						/*word�� operandũ�� ��ü�� ���� ���*/
						this.extRefList.add(new ExtRef(String.format("%06X",token.location), (token.byteSize*2), mark, str.split("-")[0]));
					else 
						this.extRefList.add(new ExtRef(String.format("%06X",token.location+1), (token.byteSize*2) -3, mark, str.split("-")[0]));
				}
				str = str.split("-")[1];
				mark = '-';
				i = -1;
				continue;
			}
		}
		/*�������� ��ȣ�� ����.*/
		int tmpAddr = symtab.search(str);
		if(tmpAddr >= 0) {
			if(mark == '+') result += tmpAddr;
			else result -= tmpAddr;
		}
		else {
			if(token.operator.equals("WORD"))
				/*word�� operandũ�� ��ü�� ���� ���*/
				extRefList.add(new ExtRef(String.format("%06X",token.location), (token.byteSize*2), mark, str));
			else 
				extRefList.add(new ExtRef(String.format("%06X",token.location+1), (token.byteSize*2) -3, mark, str));
		}
		return result;
	}
}

class ExtRef{
	String pointAddr;
	int size;
	char operator;
	String label;
	
	public ExtRef(String pointAddr, int size, char operator, String label) {
		this.pointAddr = pointAddr;
		this.size = size;
		this.operator = operator;
		this.label = label;
	}
}