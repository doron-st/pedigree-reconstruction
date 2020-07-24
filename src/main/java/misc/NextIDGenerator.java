package misc;

public class NextIDGenerator {
	static Integer lastID;
	static Integer idJump = 10000;
	
	public NextIDGenerator(int generation){
		lastID=idJump*generation-1;
	}
	public int getNextID(){
		return ++lastID;
	}
}
