package pedreconstruction;

import graph.VertexData;
import pedigree.Person;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Population {
    private final Map<Integer, Person> idToPerson = new HashMap<>();

    public Population(List<VertexData> persons) {
        for (VertexData p : persons) {
            addPerson((Person) p);
        }
    }

    public int getAge(int id) {
        return idToPerson.get(id).getAge();
    }

    public void addPerson(Person p) {
        idToPerson.put(p.getId(), p);
    }

    public Set<Integer> getIDs() {
        return idToPerson.keySet();
    }

    public void setAge(int id, int age) {
        idToPerson.get(id).setAge(age);
    }

    public Person getPerson(int id) {
        return idToPerson.get(id);
    }
}
