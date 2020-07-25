package pedigree;

import graph.Graph;
import graph.MyLogger;
import graph.Vertex;

import javax.management.RuntimeErrorException;
import java.util.*;

/**
 * Processes functions of Nuclear Families
 */
public class NuclearFamilyCreator {

    private final Graph graph;
    private final List<NucFamily> allFamilies;
    private final int generation;
    private final NextIDGenerator nextIDGen;

    public NuclearFamilyCreator(Graph graph, int generation, NextIDGenerator nextIDGen) {
        this.graph = graph;
        this.allFamilies = new ArrayList<>();
        this.generation = generation;
        this.nextIDGen = nextIDGen;
    }


    /**
     * Check consistency of nuclear families,
     * break into two families when needed
     */
    public List<NucFamily> createNuclearFamilies(List<List<Vertex>> siblingGroups, boolean doingHirarchialClustering) {

        List<NucFamily> nucFamilies = new ArrayList<>();
        NucFamiliyCreationStat nucFamiliyCreationStat = new NucFamiliyCreationStat();
        for (List<Vertex> siblingsGroup : siblingGroups) {
            // Its easier working with persons and not with Vertices
            List<Person> siblingsPersons = personsFromVertices(siblingsGroup);

            boolean areAllSiblingFromPreviosGeneration = true;
            for (Person p : siblingsPersons) {
                if (p.getDiscoveryGeneration() != (generation - 1)) {
                    areAllSiblingFromPreviosGeneration = false;
                }
            }
            MyLogger.info("NucFamCreator::SiblingGroup=" + siblingsPersons);
            if (areAllSiblingFromPreviosGeneration || doingHirarchialClustering) {
                createNucFamilyForSiblings(nucFamilies, siblingsPersons, nucFamiliyCreationStat, generation);
            } else {
                addNewSibsToFamily(nucFamilies, siblingsGroup);
                MyLogger.important("Skipping creation of nuclear familiy since some siblings are from previous generation " + siblingsPersons);
            }
        }
        MyLogger.info("Processing nuclear families returned " + nucFamiliyCreationStat);
        return nucFamilies;
    }


    public List<List<Vertex>> getSiblingGroups() {
        List<List<Vertex>> siblingGroups = new ArrayList<>();
        List<List<Person>> lists = getSiblingGroupsAsPersons();
        for (List<Person> persons : lists) {
            try {
                siblingGroups.add(verticesFromPersons(persons));
            } catch (Throwable t) {
                MyLogger.error("Error processing the list of persons " + persons);
                t.printStackTrace();
            }
        }

        writeSibGroupsStats(siblingGroups);

        return siblingGroups;
    }


    private void writeSibGroupsStats(List<List<Vertex>> siblingGroups) {
        Integer[] sizes = {0, 0, 0, 0, 0, 0};
        for (List<Vertex> vertexes : siblingGroups) {
            if (vertexes.size() < sizes.length - 1) {
                sizes[vertexes.size()]++;
            } else {
                sizes[sizes.length - 1]++;
            }
        }

        StringBuilder s = new StringBuilder("Sibling Statistics: ");
        for (int i = 1; i < sizes.length; ++i) {
            s.append("[#sibs:").append(i).append(" #groups ").append(sizes[i]).append("] ");
        }

        MyLogger.important(s.toString());
    }

    private void createNucFamilyForSiblings(List<NucFamily> nucFamilies, List<Person> siblingsPersons,
                                            NucFamiliyCreationStat nucFamiliyCreationStat, int discoveryGeneration) {

        List<NucFamily> newNucFamilies = new ArrayList<>();
        //
        // Init a map of mother to all her children and the same for fathers
        //
        Map<Person, List<Person>> motherToSiblings = new HashMap<>();
        Map<Person, List<Person>> fatherToSiblings = new HashMap<>();
        Person deadMother = null;
        Person deadFather = null;
        MyLogger.info("Create nuclear family from :" + siblingsPersons.toString());

        for (Person sibling : siblingsPersons) {
            MyLogger.debug("new child in family: " + sibling);
            Person mother = sibling.getFamily().mother;
            Person father = sibling.getFamily().father;

            if (mother == null) {
                if (deadMother == null) {
                    int id = nextIDGen.getNextID();
                    deadMother = new Person(id, String.valueOf(id), 9999, true, discoveryGeneration);
                    //	deadMother.setAlive(false);
                    nucFamiliyCreationStat.numberOfDead++;
                    MyLogger.info("New mother: " + deadMother.getId());

                }
                mother = deadMother;
                // Updating the mother for this sibling
                sibling.getFamily().mother = deadMother;
            } else {
                if (mother != deadMother)
                    nucFamiliyCreationStat.numberOfAlive++;
            }
            if (father == null) {
                if (deadFather == null) {
                    int id = nextIDGen.getNextID();
                    deadFather = new Person(id, String.valueOf(id), 9999, false, discoveryGeneration);
                    //	deadFather.setAlive(false);
                    nucFamiliyCreationStat.numberOfDead++;
                    MyLogger.info("New father: " + deadFather.getId());
                }
                father = deadFather;
                sibling.getFamily().father = deadFather;

            } else {
                if (father != deadFather)
                    nucFamiliyCreationStat.numberOfAlive++;
            }

            // Validations
            if (sibling.equals(sibling.getFamily().father.getFamily().father)) {
                throw new RuntimeException("Sibling " + sibling + " is the parent of its father " + sibling.getFamily().father);
            }
            if (sibling.equals(sibling.getFamily().mother.getFamily().mother)) {
                throw new RuntimeException("Sibling " + sibling + " is the parent of its mother " + sibling.getFamily().mother);
            }

            if (!motherToSiblings.containsKey(mother)) {
                motherToSiblings.put(mother, new ArrayList<>());
            }
            if (!fatherToSiblings.containsKey(father)) {
                fatherToSiblings.put(father, new ArrayList<>());
            }
            motherToSiblings.get(mother).add(sibling);
            fatherToSiblings.get(father).add(sibling);
        }

        if (motherToSiblings.size() == 1 && fatherToSiblings.size() == 1) {
            nucFamiliyCreationStat.normalFamilies++;
            // exactly one parent- great!

            Person mother = motherToSiblings.keySet().iterator().next();
            Person father = fatherToSiblings.keySet().iterator().next();

            NucFamily nucFamily = new NucFamily(mother, father, motherToSiblings.get(mother));

            newNucFamilies.add(nucFamily);

        } else if (motherToSiblings.size() == 1 && fatherToSiblings.size() == 2) {
            MyLogger.important("found one living mother, and two fathers");
            nucFamiliyCreationStat.motherTwoFathers++;
            // exactly one mother and 2 fathers
            Person mother = motherToSiblings.keySet().iterator().next();
            Iterator<Person> fatherIterator = fatherToSiblings.keySet().iterator();
            Person father1 = fatherIterator.next();
            Person father2 = fatherIterator.next();

            NucFamily nucFamily1 = new NucFamily(mother, father1, fatherToSiblings.get(father1));
            newNucFamilies.add(nucFamily1);
            NucFamily nucFamily2 = new NucFamily(mother, father2, fatherToSiblings.get(father2));
            newNucFamilies.add(nucFamily2);
        } else if (motherToSiblings.size() == 2 && fatherToSiblings.size() == 1) {
            MyLogger.important("found one living father, and two mothers");
            nucFamiliyCreationStat.fatherTwoMothers++;
            // exactly one mother and 2 fathers
            Person father = fatherToSiblings.keySet().iterator().next();
            Iterator<Person> motherIterator = motherToSiblings.keySet().iterator();
            Person mother1 = motherIterator.next();
            Person mother2 = motherIterator.next();

            NucFamily nucFamily1 = new NucFamily(mother1, father, motherToSiblings.get(mother1));
            newNucFamilies.add(nucFamily1);
            NucFamily nucFamily2 = new NucFamily(mother2, father, motherToSiblings.get(mother2));
            newNucFamilies.add(nucFamily2);
        } else if (motherToSiblings.size() == 2 && fatherToSiblings.size() == 2) {
            MyLogger.important("found two living mothers, and two fathers");
            nucFamiliyCreationStat.twoFatherTwoMothers++;

            Iterator<Person> fatherIterator = fatherToSiblings.keySet().iterator();
            Person father1 = fatherIterator.next();
            Person father2 = fatherIterator.next();

            Iterator<Person> motherIterator = motherToSiblings.keySet().iterator();
            Person mother1 = motherIterator.next();
            Person mother2 = motherIterator.next();

            // No matching between the groups
            if (!motherToSiblings.get(mother1).equals(fatherToSiblings.get(father1)) ||
                    !motherToSiblings.get(mother2).equals(fatherToSiblings.get(father2))) {
                if (motherToSiblings.get(mother2).equals(fatherToSiblings.get(father1)) &&
                        motherToSiblings.get(mother1).equals(fatherToSiblings.get(father2))) {
                    // switching mother1 and mother 2
                    Person tempMother = mother1;
                    mother1 = mother2;
                    mother2 = tempMother;
                } else {
                    MyLogger.error("father1=" + father1);
                    MyLogger.error("father2=" + father2);
                    MyLogger.error("mother1=" + mother1);
                    MyLogger.error("mother2=" + mother2);
                    //throw new RuntimeException("Multiple parents are mixed up. need to check exact mixup");
                }
            }


            NucFamily nucFamily1 = new NucFamily(mother1, father1, motherToSiblings.get(mother1));
            newNucFamilies.add(nucFamily1);
            NucFamily nucFamily2 = new NucFamily(mother2, father2, motherToSiblings.get(mother2));
            newNucFamilies.add(nucFamily2);
        } else {
            nucFamiliyCreationStat.tooManyParents++;
            MyLogger.error("ERROR - more than 2 mothers or 2 fathers for the following siblings " + siblingsPersons);
        }

        // Updating siblings of each sibling to match the newly added nuclear families
        for (NucFamily newNucFamily : newNucFamilies) {
            MyLogger.important("NucFamilyCreator::create nuclear family: " + newNucFamily);
            for (Vertex mainSib : verticesFromPersons(newNucFamily.siblings)) {
                Person mainSibPerson = ((Person) mainSib.getData());
                mainSibPerson.getFamily().siblings = new ArrayList<>();
                for (Person sib : newNucFamily.siblings) {
                    if (!sib.equals(mainSibPerson)) {
                        mainSibPerson.getFamily().siblings.add(sib);
                    }
                }
            }
        }

        nucFamilies.addAll(newNucFamilies);
        allFamilies.addAll(newNucFamilies);
    }

    private void addNewSibsToFamily(List<NucFamily> nucFamilies, List<Vertex> siblings) {
        NucFamily fam = findFamily(siblings);

        for (Vertex v : siblings) {
            Person s = (Person) v.getData();
            if (s.getFamily().father == null || s.getFamily().mother == null) {
                s.getFamily().father = fam.getFather();
                s.getFamily().mother = fam.getMother();
                fam.siblings.add((Person) v.getData());
            }
        }

        nucFamilies.add(fam);
    }

    private NucFamily findFamily(List<Vertex> siblings) {
        for (NucFamily fam : allFamilies) {
            if (siblings.containsAll(fam.siblings)) {
                return fam;
            }
        }
        throw new RuntimeErrorException(new Error("Could not find nuclear family contained in siblings"));
    }


    //Returns unsorted list of sibling groups
    private List<List<Person>> getSiblingGroupsAsPersons() {
        List<List<Person>> siblingGroups = new ArrayList<>();
        Integer i = 0;
        for (Vertex vertex : graph.getVertexMap().values()) {
            ++i;
            MyLogger.debug(String.format("Processing vertex %d as part for getting sibling groups. Vertex is %s", i, vertex));
            Person person = (Person) vertex.getData();
            List<Person> siblings = person.getFamily().siblings;
            // This is just a sanity check
            verifySiblings(siblings);

            List<Person> siblingsWithPerson = new ArrayList<>(siblings);
            siblingsWithPerson.add(person);

            if (isSiblingsAlreadyInList(siblingsWithPerson, siblingGroups)) {
                MyLogger.debug("Sibling list already in list" + siblingsWithPerson);
            } else {
                MyLogger.debug("Adding new sibling list to siblings groups " + siblingsWithPerson);
                siblingGroups.add(siblingsWithPerson);
            }
        }

        return siblingGroups;
    }

    @SuppressWarnings("unchecked")
    private List<Person> personsFromVertices(List<Vertex> vertices) {
        return (List<Person>) graph.vertexDataFromVertices(vertices);
    }

    private List<Vertex> verticesFromPersons(List<Person> persons) {
        return graph.verticesFromDatas(persons);
    }

    private Boolean isSiblingsAlreadyInList(List<Person> siblings, List<List<Person>> siblingGroups) {
        Set<Person> set1 = new HashSet<>(siblings);
        for (List<Person> siblingGroup : siblingGroups) {
            Set<Person> set2 = new HashSet<>(siblingGroup);
            if (set1.equals(set2)) {
                return true;
            }
        }
        return false;
    }


    /**
     * a simple method for verifying that all the siblings in a list have the same siblings in their "familiy"
     *
     * @param siblings list of persons who are siblings
     */
    private void verifySiblings(List<Person> siblings) {
        int i = 0;

        while (i < siblings.size() - 1) {
            int firstPersonIndex = i;
            int secondPersonIndex = firstPersonIndex + 1;
            Person p1 = siblings.get(firstPersonIndex);
            Person p2 = siblings.get(secondPersonIndex);
            List<Person> sibsWithPerson1 = new ArrayList<>(p1.getFamily().siblings);
            sibsWithPerson1.add(p1);
            Collections.sort(sibsWithPerson1);
            List<Person> sibsWithPerson2 = new ArrayList<>(p2.getFamily().siblings);
            sibsWithPerson2.add(p2);
            Collections.sort(sibsWithPerson2);

            if (!sibsWithPerson1.equals(sibsWithPerson2)) {
                throw new RuntimeException("Sibling lists don't match");
            }
            ++i;
        }
    }


    public static class NucFamiliyCreationStat {
        Integer numberOfDead = 0;
        Integer numberOfAlive = 0;
        Integer normalFamilies = 0;
        Integer motherTwoFathers = 0;
        Integer fatherTwoMothers = 0;
        Integer twoFatherTwoMothers = 0;
        Integer tooManyParents = 0;

        @Override
        public String toString() {
            return "NucFamiliyCreationStat{" +
                    "numberOfDead=" + numberOfDead +
                    ", numberOfAlive=" + numberOfAlive +
                    ", normalFamilies=" + normalFamilies +
                    ", motherTwoFathers=" + motherTwoFathers +
                    ", fatherTwoMothers=" + fatherTwoMothers +
                    ", twoFatherTwoMothers=" + twoFatherTwoMothers +
                    ", tooManyParents=" + tooManyParents +
                    '}';
        }
    }
}
