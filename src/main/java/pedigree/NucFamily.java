package pedigree;

import java.util.List;

public class NucFamily {
    private Person mother;
    private Person father;
    public List<Person> siblings;
    private boolean fatherJoined = false;
    private boolean motherJoined = false;

    public NucFamily(Person mother, Person father, List<Person> siblings) {
        this.mother = mother;
        this.father = father;
        this.siblings = siblings;
    }

    public void setFatherJoined() {
        fatherJoined = true;
    }

    public Person getFather() {
        return father;
    }

    public Person getMother() {
        return mother;
    }

    public void setMotherJoined() {
        motherJoined = true;
    }


    public boolean wasFatherJoined() {
        return fatherJoined;
    }

    public boolean wasMotherJoined() {
        return motherJoined;
    }

    @Override
    public String toString() {
        return "child=" + siblings + ",pop=" + father + ",mom=" + mother;
    }

    public void setFather(Person father) {
        this.father = father;
    }

    public void setMother(Person mother) {
        this.mother = mother;
    }

}
