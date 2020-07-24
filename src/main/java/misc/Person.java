package misc;

import graph.MyLogger;
import graph.VertexData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * 
 */
public class Person implements  VertexData, Comparable<Person> {
	private static final long serialVersionUID = -2041304994757775928L;
	// -- Details of the person
    private Integer id;
    private String idStr;
    private Integer age;
    private Boolean gender; //true==female

    // Members in the person's family - after inferring the reconstruct
    private Family family = new Family();
    private Boolean isAlive;
    private int discoveryGeneration=-1;

    /**
     * 
     * @param id
     * @param personIdStr 
     * @param age
     * @param gender
     * @param discoveryGeneration
     */
    public Person(Integer id, String personIdStr, Integer age, Boolean gender,int discoveryGeneration) {
        this.id = id;
        this.idStr=personIdStr;
        this.age = age;
        this.gender = gender;
        this.discoveryGeneration = discoveryGeneration;
        if(discoveryGeneration==0)
        	isAlive=true;
        else
        	isAlive=false;        	
    }

    public Integer getId() {
        return id;
    }
    public String getIdString() {
        return idStr;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(int age){
    	this.age=age;
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
        return	"{" +
                "id=" + id
                +",age=" + age +
                //",gender=" + (gender ? "F" : "M") +
                //",isAlive=" + isAlive +
                ",Gen=" + discoveryGeneration 
                +'}';
    }

    /**
     * read person list from demographics file
     *
     * @param demographFilename - demographies file
     * @return map of the person id to the person data
     *
     * @throws IOException - in case of a problem reading the file
     */
    public static List<VertexData> listFromDemograph(String demographFilename) throws IOException {
        MyLogger.important("Creating person list from " + demographFilename);
        List<VertexData> persons = new ArrayList<VertexData>();

        boolean demographicsHasHeader=true;
        BufferedReader fileReader = new BufferedReader(new FileReader(new File(demographFilename)));
        if(demographicsHasHeader)
            fileReader.readLine();

        String nextLine;
        int i=0;
        while((nextLine = fileReader.readLine()) != null) {
        	i++;
            StringTokenizer nextLineTokenizer = new StringTokenizer(nextLine,"\t ");

            String personIdStr = nextLineTokenizer.nextToken();
            String personAgeStr = nextLineTokenizer.nextToken();
            String personGenderStr = nextLineTokenizer.nextToken();
            
            Integer id = i;
            Integer age = Integer.parseInt(personAgeStr);
            Boolean gender = null;
            if(personGenderStr.trim().equals("0")){
                gender = Boolean.FALSE;
            } else if(personGenderStr.trim().equals("1")){
                gender = Boolean.TRUE;
            } else {
            	fileReader.close();
                throw new RuntimeException("Error parsing gender " + personGenderStr);
            }
            Person p = new Person(id,personIdStr,age,gender,0);
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

        if (id != null ? !id.equals(person.id) : person.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public int compareTo(Person o) {
        return this.id.compareTo(o.getId());
    }
}
