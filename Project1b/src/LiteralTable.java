import java.util.ArrayList;

/**
 * literal�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class LiteralTable {
	ArrayList<String> literalList;
	ArrayList<Integer> locationList;
	// ��Ÿ literal, external ���� �� ó������� �����Ѵ�.
	
	public LiteralTable() {
		this.literalList = new ArrayList<>();
		this.locationList = new ArrayList<>();
	}
	
	/**
	 * ���ο� Literal�� table�� �߰��Ѵ�.
	 * @param literal : ���� �߰��Ǵ� literal�� label
	 * @param location : �ش� literal�� ������ �ּҰ�
	 * ���� : ���� �ߺ��� literal�� putLiteral�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifyLiteral()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putLiteral(String literal, int location) {
		int tempIndex = searchIndex(literal); 
		if(tempIndex>=0) {
			locationList.set(tempIndex, location);
		}
		else {
			literalList.add(literal);
			locationList.add(location);
		}
	}
	
	/**
	 * ó������ location�� �������� ���� ä�� �� ���̴�.
	 * @param literal : ���� �߰��Ǵ� literal�� label
	 */
	public void putLiteral(String literal) {
		if(searchIndex(literal)<0) {
			literalList.add(literal);
			locationList.add(0);
		}
	}
	
	/**
	 * ������ �����ϴ� literal ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param literal : ������ ���ϴ� literal�� label
	 * @param newLocation : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifyLiteral(String literal, int newLocation) {
		for(int i = 0; i < this.literalList.size(); i++) {
			if(literalList.get(i).equals(literal)) locationList.set(i, newLocation);
		}
	}
	
	/**
	 * ���ڷ� ���޵� literal�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param literal : �˻��� ���ϴ� literal�� label
	 * @return literal�� ������ �ִ� �ּҰ�. �ش� literal�� ���� ��� -1 ����
	 */
	public int search(String literal) {
		for(int i = 0; i < this.literalList.size(); i++) {
			if(literalList.get(i).equals(literal)) return locationList.get(i);
		}
		return -1;
	}
	
	/**
	 * ���ͷ��� �ε����� �����´�.
	 * @param literal
	 * @return literal�� �ε�����. ������ -1����.
	 */
	public int searchIndex(String literal) {
		for(int i = 0; i < this.literalList.size(); i++) {
			if(literalList.get(i).equals(literal)) return i;
		}
		return -1;
	}
	
}
