package misc;

import graph.Graph;
import graph.Vertex;

import java.util.ArrayList;
import java.util.List;


public class NucFamily {
	private Person mother;
	private Person father;
	public List<Person> siblings = new ArrayList<Person>();
	private boolean fatherJoined=false;
    private boolean motherJoined=false;
    
    public NucFamily(Person mother, Person father, List<Person> siblings, Graph graph) {
        this.mother = mother;
        this.father = father;
        this.siblings = siblings;
    }

    public Integer getNumberOfLivingParents() {
        Integer parentAlive = 0;
        if(mother != null && mother.isAlive()) {
            parentAlive++;
        }
        if(father != null && father.isAlive()) {
            parentAlive++;
        }

        return parentAlive;
    }

    public void setFatherJoined(){
    	fatherJoined=true;
    }
    public Person getFather(){
    	return father;
    }
    public Person getMother(){
    	return mother;
    }
    public void setMotherJoined(){
    	motherJoined=true;
    }
    

    public boolean wasFatherJoined(){
    	return fatherJoined;
    }
    public boolean wasMotherJoined(){
    	return motherJoined;
    }
    
	public static Boolean isVertexInNucFamily(Vertex v, NucFamily nucFamily) {
		if(nucFamily.father==v.getData()){
			return true;
		}
		if(nucFamily.mother==v.getData()){
			return true;
		}
		if(nucFamily.siblings.contains(v)){
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "chld=" + siblings + ",pop="+father+",mom=" + mother;
	}

	public void setFather(Person father) {
		this.father=father;		
	}
	public void setMother(Person mother) {
		this.mother=mother;		
	}
	
}
