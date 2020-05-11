import java.util.ArrayList;

/**
 * 외부참조 테이블이다. 각 섹션(프로그램)에 할당이 되어있으며 링크없이 TokenTable에서 관리되어진다.
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
	 * 연산식 문자열을 파싱하고 주소를 계산해 리턴하는 역할을 한다.
	 * WORD는 외부참조 테이블을 생성할 수 있다. 오브젝트 코드를 생성하기 때문이다.
	 * 
	 * @param token 파싱하고자 하는 토큰
	 * @param symtab 탐색할 해당 섹션의 심볼테이블
	 */
	public int calculateParser(Token token, SymbolTable symtab) {
		int result = 0;
		char mark = 0;
		if(token.operand[0]==null) return -1;
		String str = token.operand[0];
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
				}
				/*주어진 심볼테이블에서 못 찾았다면 외부참조 테이블에 추가해야한다.*/
				else {
					if(token.operator.equals("WORD"))
						/*word는 operand크기 전체가 대입 대상*/
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
						/*word는 operand크기 전체가 대입 대상*/
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
		/*마지막은 기호가 없다.*/
		int tmpAddr = symtab.search(str);
		if(tmpAddr >= 0) {
			if(mark == '+') result += tmpAddr;
			else result -= tmpAddr;
		}
		else {
			if(token.operator.equals("WORD"))
				/*word는 operand크기 전체가 대입 대상*/
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