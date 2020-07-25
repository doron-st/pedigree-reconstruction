package pedigree;

import misc.MyLogger;
import graph.VertexData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Person implements VertexData, Comparable<Person> {
    private static final long serialVersionUID = -2041304994757775928L;
    // -- Details of the person
    private final int id;
    private int age;
    private final boolean gender; //true==female

    // Members in the person's family - after inferring the reconstruct
    private final Family family = new Family();
    private final Boolean isAlive;
    private final int discoveryGeneration;

    public Person(Integer id, Integer age, Boolean gender, int discoveryGeneration) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.discoveryGeneration = discoveryGeneration;
        isAlive = discoveryGeneration == 0;
    }

    public Integer getId() {
        return id;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Boolean getGender() {
        return gender;
    }

    public Family getFamily() {
        return family;
    }

    public Boolean isAlive() {
        return isAlive;
    }

    public Integer getDiscoveryGeneration() {
        return discoveryGeneration;
    }

    @Override
    public String toString() {
        //return id.toString();
        return "{" +
                "id=" + id
                + ",age=" + age +
                //",gender=" + (gender ? "F" : "M") +
                //",isAlive=" + isAlive +
                ",Gen=" + discoveryGeneration
                + '}';
    }

    /**
     * read person list from demographics file
     *
     * @param demographicsFilename - demographics file
     * @return list of person data
     * @throws IOException - in case of a problem reading the file
     */
    public static List<VertexData> listFromDemograph(String demographicsFilename) throws IOException {
        MyLogger.important("Creating person list from " + demographicsFilename);
        List<VertexData> persons = new ArrayList<>();

        BufferedReader fileReader = new BufferedReader(new FileReader(new File(demographicsFilename)));
        fileReader.readLine();

        String nextLine;
        int i = 0;
        while ((nextLine = fileReader.readLine()) != null) {
            i++;
            StringTokenizer nextLineTokenizer = new StringTokenizer(nextLine, "\t ");

            String personIdStr = nextLineTokenizer.nextToken();
            String personAgeStr = nextLineTokenizer.nextToken();
            String personGenderStr = nextLineTokenizer.nextToken();

            Integer id = Integer.parseInt(personIdStr);
            Integer age = Integer.parseInt(personAgeStr);
            Boolean gender;
            if (personGenderStr.trim().equals("0")) {
                gender = Boolean.FALSE;
            } else if (personGenderStr.trim().equals("1")) {
                gender = Boolean.TRUE;
            } else {
                fileReader.close();
                throw new RuntimeException("Error parsing gender " + personGenderStr);
            }
            Person p = new Person(id, age, gender, 0);
            persons.add(p);
            MyLogger.debug(i + ")Added " + p + " to graph");
        }
        MyLogger.important("Added " + i + " individuals to graph");

        fileReader.close();
        return persons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        return id == person.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(Person o) {
        return Integer.compare(id, o.getId());
    }
}
