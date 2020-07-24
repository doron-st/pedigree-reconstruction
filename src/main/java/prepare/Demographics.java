package prepare;
import graph.VertexData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import misc.Person;

public class Demographics {

	private Map<Integer,Person> idToPerson = new HashMap<Integer,Person>();
	private Map<String,Person> idStrToPerson = new HashMap<String,Person>();
	
	public Demographics(List<VertexData> persons){
		for(VertexData p : persons){
			addPerson((Person) p);
		}
	}
	public int getAge(int id){
		return idToPerson.get(id).getAge();
	}
	public String getIDString(int id){
		return idToPerson.get(id).getIdString();
	}
	public boolean getGender(int id){
		return idToPerson.get(id).getGender();
	}
	public int getDiscoveryGeneration(int id){
		return idToPerson.get(id).getDiscoveryGeneration();
	}
	public void addPerson(Person p){
		idToPerson.put(p.getId(), p);
		idStrToPerson.put(p.getIdString(), p);
	}
	public Set<Integer> getIDs(){
		return idToPerson.keySet();
	}
	public void setAge(int id,int age){
		idToPerson.get(id).setAge(age);
	}
	public Person getPerson(int id){
		return idToPerson.get(id);
	}
	public Person getPerson(String id){
		return idStrToPerson.get(id);
	}
	
}
