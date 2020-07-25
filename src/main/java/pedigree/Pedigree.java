package pedigree;

import misc.MyLogger;
import pedreconstruction.Population;
import common.Genotype;
import common.Haplotype;

import java.io.*;
import java.util.*;


public class Pedigree {
    private final Map<Integer, PedVertex> vertices = new HashMap<>();
    private final Map<PedVertex, Genotype> genotypes = new HashMap<>();
    private int largestID;
    private Population population;

    public Pedigree(Population population) {
        this.population = population;
        for (int id : population.getIDs()) {
            addVertex(id, -1, -1, true);
            if (id > largestID)
                largestID = id;
        }
    }

    //clone constructor
    public Pedigree(Pedigree ped) {
        MyLogger.debug("Cloning pedigree" + ped);
        //Add all vertices
        for (PedVertex v : ped.getVertices())
            addVertex(v.getId(), -1, -1, v.isAlive);

        //set parents
        for (PedVertex v : ped.getVertices()) {
            PedVertex u = getVertex(v.id);
            u.setFather(v.getFatherId());
            u.setMother(v.getMotherId());
        }
        MyLogger.debug("Cloning done");

    }

    public Pedigree() {
    }

    /**
     * For debugging purposes
     */
    public Pedigree extractSubPedigreeNoConversion(PedVertex f1, PedVertex f2, Map<Integer, Integer> idConversion) {
        Map<Integer, Integer> enumartionTable = new HashMap<>();

        List<PedVertex> descendants = new ArrayList<>();

        addDescendantsToList(f1, descendants, false);
        addDescendantsToList(f2, descendants, false);

        List<PedVertex> desAncestry = new ArrayList<>();
        List<Integer> degrees = new ArrayList<>();

        for (PedVertex desc : descendants) {
            addAncestry(desc, 0, desAncestry, degrees);
        }

        //Number the vertices by order of discovery
        for (PedVertex v : desAncestry) {
            enumartionTable.put(v.id, v.id);
        }
        enumartionTable.put(-1, -1);


        //Fill conversion table, that convert enumation back to the real IDs
        idConversion.put(-1, -1);
        for (Integer id : enumartionTable.keySet()) {
            idConversion.put(enumartionTable.get(id), id);
        }

        Pedigree subPed = new Pedigree();

        //Add vertices to pedigree, top->bottom order
        subPed.createPedigreeFromList(desAncestry, enumartionTable);

        return subPed;
    }

    /**
     * Calc likelihood of pedigree
     */

    public void simulateRecombinations() {
        //MyLogger.important("Pedigree::simulateRecombinations:" + this);
        //Create founder genotypes,and reset non-founder genotypes
        for (PedVertex v : getVertices()) {
            if (v.isFounder()) {
                MyLogger.debug(v + " is a founder");
                attachGenotype(v, new Genotype(v.getId()));
            } else {
                MyLogger.debug(v + " is a non-founder, genotype reset before simulation");

                resetGenotype(v.getId());
            }
        }
        //simulate other genotypes
        for (PedVertex v : getVertices()) {
            simulateRecombination(v);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other.getClass() != Pedigree.class) {
            MyLogger.important("Compare Pedigree to not-Pedigree object");
            return false;
        }
        Pedigree o = (Pedigree) other;
        if (getVertices().size() != o.getVertices().size())
            return false;

        for (PedVertex v : getVertices()) {
            if (o.getVertex(v.getId()) == null) {
                MyLogger.info("Missing vertex " + v.getId());
                return false;
            }
            PedVertex oV = o.getVertex(v.getId());
            if (v.isAlive() != oV.isAlive()) {
                MyLogger.info("Different living status, vertex " + v.getId());
                return false;
            }
            if (v.getFatherId() != oV.getFatherId()) {
                MyLogger.info("Different father, vertex " + v.getId() + ": " + v.getFatherId() + "," + oV.getFatherId());
                return false;
            }
            if (v.getMotherId() != oV.getMotherId()) {
                MyLogger.info("Different mother, vertex " + v.getId() + ": " + v.getMotherId() + "," + oV.getMotherId());
                return false;
            }
        }
        return true;
    }

    public boolean hasVertex(int id) {
        return vertices.containsKey(id);
    }

    public PedVertex getVertex(int id) {
        if (id == -1)
            return null;
        return vertices.get(id);
    }

    /**
     * Get list of vertices sorted by ID
     */
    public List<PedVertex> getVertices() {
        List<PedVertex> l = new ArrayList<>(vertices.values());
        l.sort(Comparator.comparingInt(arg0 -> arg0.id));
        return l;
    }

    public List<PedVertex> getFounders() {
        List<PedVertex> founders = new ArrayList<>();
        for (PedVertex v : getVertices()) {
            if (v.isFounder()) {
                founders.add(v);
            }
        }
        return founders;
    }

    /**
     * Add a dead founder
     */
    public void addVertex(int id) {
        addVertex(id, -1, -1, false);
    }

    /**
     * Add a new vertex (if id exists, do nothing!)
     */
    public void addVertex(int id, int fatherId, int motherId, boolean isAlive) {
        MyLogger.debug("Adding vertex " + id + " " + fatherId + " " + motherId);
        PedVertex v = new PedVertex(id, fatherId, motherId, isAlive);

        if (getVertex(id) != null) {
            MyLogger.info("Pedigree::addVertex: Ignoring existing vertex ID: " + id);
            return;
        }

        vertices.put(id, v);
        if (id > largestID)
            largestID = id;
    }

    public int getNewID() {
        return ++largestID;
    }

    public void resetGenotype(int vid) {
        genotypes.remove(getVertex(vid));
    }

    public void removeVertex(int id) {
        for (PedVertex child : getVertex(id).children) {
            if (child.getFatherId() == id)
                child.overrideFather(-1);
            if (child.getMotherId() == id)
                child.overrideMother(-1);
        }
        vertices.remove(id);
        genotypes.remove(getVertex(id));
    }

    public void attachGenotype(PedVertex v, Genotype g) {
        genotypes.put(v, g);
    }

    public Genotype getGenotype(int vid) {
        return genotypes.get(getVertex(vid));
    }

    public List<PedVertex> getDescendants(int vid) {
        PedVertex v = getVertex(vid);
        if (v == null)
            MyLogger.error("Pedigree:getDescendants:: " + vid + " is not in pedigree");

        List<PedVertex> list = new ArrayList<>();
        addDescendantsToList(v, list, false);
        return list;
    }

    public List<PedVertex> getAllDescendants(int vid) {
        PedVertex v = getVertex(vid);
        if (v == null)
            MyLogger.error("Pedigree:getDescendants:: " + vid + " is not in pedigree");

        List<PedVertex> list = new ArrayList<>();
        addDescendantsToList(v, list, true);
        return list;
    }

    public List<PedVertex> getLiving() {
        List<PedVertex> living = new ArrayList<>();
        for (PedVertex v : getVertices()) {
            if (v.isAlive()) {
                living.add(v);
            }
        }
        return living;
    }

    public Population getPopulation() {
        return population;
    }

    public void calcExpectedFounderAges(int generation) {
        MyLogger.important("Pedigree::Calc expected founders ages");
        for (PedVertex v : getFounders()) {
            handleDemographics(v.getId(), generation);
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (PedVertex v : getVertices())
            result.append(v).append(",");
        return result.toString();
    }

    public void readFromFile(String filename) {
        MyLogger.important("Read pedigree from file " + filename);
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(new File(filename)));
        } catch (IOException e) {
            throw new RuntimeException("Failed openning structure file " + filename, e);
        }

        Map<Integer, int[]> verMap = new HashMap<>();
        String nextLine;
        try {
            while ((nextLine = fileReader.readLine()) != null) {
                StringTokenizer nextLineTokenizer = new StringTokenizer(nextLine, "\t");

                int childId = Integer.parseInt(nextLineTokenizer.nextToken());
                int fatherID = Integer.parseInt(nextLineTokenizer.nextToken());
                int motherID = Integer.parseInt(nextLineTokenizer.nextToken());

                //Living vertices were added first to the pedigree in construction
                int isAlive = 0;
                if(hasVertex(childId) && getVertex(childId).isAlive()) {
                    isAlive = 1;
                }

                //Add founders, (if not founder will be overriden)
                if (!verMap.containsKey(fatherID) && fatherID != -1) {
                    verMap.put(fatherID, new int[]{fatherID, -1, -1, 0});
                    MyLogger.debug("Added father to map " + fatherID);
                }
                if (!verMap.containsKey(motherID) && motherID != -1) {
                    verMap.put(motherID, new int[]{motherID, -1, -1, 0});
                    MyLogger.debug("Added mother to map " + motherID);

                }
                int[] child = {childId, fatherID, motherID, isAlive};
                verMap.put(childId, child);
                MyLogger.debug("Added child to map " + childId + " " + Arrays.toString(child));
            }
            fileReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed processing IBD features file " + filename, e);
        }

        //Delete all vertices, as alive information was extracted
        //Start from an empty pedigree
        vertices.clear();

        //Add founders
        for (int[] currPerson : verMap.values()) {
            if (currPerson[1] == -1) {
                addVertex(currPerson[0]);
                MyLogger.debug("added foudner: " + getVertex(currPerson[0]));
            }
        }

        while (vertices.size() < verMap.size()) {
            MyLogger.debug(vertices.size() + "<" + verMap.size());
            for (int[] currPerson : verMap.values()) {
                //If from next generation
                if (!hasVertex(currPerson[0]) && hasVertex(currPerson[1]) && hasVertex(currPerson[2])) {
                    addVertex(currPerson[0], currPerson[1], currPerson[2], currPerson[3] == 1);
                    MyLogger.debug("added " + getVertex(currPerson[0]));
                } else if (!hasVertex(currPerson[0]))
                    MyLogger.debug("problematic adding vertex : " + currPerson[0]);
            }
        }
    }

    /**
     * Write pedigree using vertex ids
     */
    public void writeToFile(File file) throws IOException {
        MyLogger.important("Writing pedigree to file: " + file);
        PrintWriter printWriter = new PrintWriter(file);
        //printWriter.println(String.format("name\tfather\tmother"));
        for (PedVertex v : getVertices()) {
            if (!v.isFounder()) {
                printWriter.println(String.format("%d\t%d\t%d", v.getId(), v.getFatherId(), v.getMotherId()));
                printWriter.flush();
            }
        }
        printWriter.close();
    }

    /**
     * Write pedigree using demographic IDs if they exist (Real populations)
     */
    public void writeToFile(File file, Population dem) throws IOException {
        MyLogger.important("Writing pedigree to file: " + file);
        file.getAbsoluteFile().getParentFile().mkdirs();
        PrintWriter printWriter = new PrintWriter(file);
        for (PedVertex v : getVertices()) {
            if (!v.isFounder() && dem.getPerson(v.getId()) != null) {
                printWriter.println(String.format("%d\t%d\t%d", v.getId(), v.getFatherId(), v.getMotherId()));
                printWriter.flush();
            }
        }
        printWriter.close();
    }


    /**
     * Recursively recombine parents untill having available genotypes for meiosis
     */
    private void simulateRecombination(PedVertex v) {
        if (getGenotype(v) != null) {
            //MyLogger.debug("Already has a genotype" + getGenotype(v));
            return;
        }

        if (getGenotype(v.getFather()) == null)
            simulateRecombination(v.getFather());
        if (getGenotype(v.getMother()) == null)
            simulateRecombination(v.getMother());

        //MyLogger.debug("Recombining " + v + " father="+v.getFatherId() + " mother=" + v.getMotherId());
        Haplotype fatherHaplotype = getGenotype(v.getFather()).recombine();
        Haplotype motherHaplotype = getGenotype(v.getMother()).recombine();
        //MyLogger.debug("father Hap=" + fatherHaplotype);
        //MyLogger.debug("mother Hap=" + motherHaplotype);
        Genotype g = new Genotype(fatherHaplotype, motherHaplotype);

        attachGenotype(v, g);
    }

    private Genotype getGenotype(PedVertex v) {
        return genotypes.get(v);
    }

    private void addDescendantsToList(PedVertex v, List<PedVertex> descendants, boolean addAll) {
        if (descendants.contains(v))
            return;

        //MyLogger.debug("addDescendantsToList::Adding " + v);
        descendants.add(v);

        if (v.children.isEmpty() || (v.isAlive() && !addAll))
            return;

        Collections.sort(v.children);

        for (PedVertex child : v.children) {
            if (!descendants.contains(child)) {
                addDescendantsToList(child, descendants, addAll);
            }
        }
    }

    private void addAncestry(PedVertex v, int vDeg, List<PedVertex> ancestors, List<Integer> degrees) {
        if (v == null)
            return;

        if (!ancestors.contains(v)) {
            //for(PedVertex p : ancestors)
            //	if(p.getId()==v.getId())
            //		MyLogger.error("Found same id in different vertex!");

            ancestors.add(v);
            degrees.add(vDeg);
            //MyLogger.important("Pedigree::addAncestry::adding " + v + " deg="+ vDeg);
            addAncestry(v.getFather(), vDeg + 1, ancestors, degrees);
            addAncestry(v.getMother(), vDeg + 1, ancestors, degrees);
        }
    }

    private int handleDemographics(int id, int generation) {
        //	MyLogger.important("handle demographics: " + id);

        if (population.getPerson(id) != null)
            return population.getAge(id);

        int sumAge = 0;
        boolean isMale = true;
        PedVertex v = getVertex(id);
        for (PedVertex child : v.getChildren()) {
            //	MyLogger.important("summing child age" + child);
            sumAge += handleDemographics(child.getId(), generation - 1);
            if (child.getMotherId() == id)
                isMale = false;
        }
        int avgAge = sumAge / v.getChildren().size();
        if (population.getPerson(id) == null) {
            Person p = new Person(id, avgAge + 20, isMale, generation);
            population.addPerson(p);
            //	MyLogger.important("Added person" + p);
        } else {
            population.setAge(v.getId(), avgAge + 20);
            MyLogger.info("Updated age of person" + population.getPerson(v.getId()));
        }
        return avgAge + 20;
    }

    private void createPedigreeFromList(List<PedVertex> list, Map<Integer, Integer> idConversion) {
        List<PedVertex> topLayer = new ArrayList<>();

        for (PedVertex v : list) {
            if (v.isFounder()) {
                topLayer.add(v);
                //	MyLogger.debug("createPedigreeFromList::Adding top layer node: " + v);
                addVertex(idConversion.get(v.getId()), idConversion.get(v.getFatherId()), idConversion.get(v.getMotherId()), v.isAlive());
            }
        }

        //int i =1;
        while (getVertices().size() < list.size()) {
            //if(i>2)
            //	MyLogger.important(getVertices().size() + "<" + list.size());
            //i++;
            for (PedVertex v : list) {
                //if relevant and in next layer and was not added before
                if (!hasVertex(idConversion.get(v.getId())) &&
                        hasVertex(idConversion.get(v.getFatherId())) && hasVertex(idConversion.get(v.getMotherId()))) {
                    MyLogger.debug("createPedigreeFromList::Adding next layer node: " + v);
                    addVertex(idConversion.get(v.getId()), idConversion.get(v.getFatherId()), idConversion.get(v.getMotherId()), v.isAlive());
                }
            }
        }
    }

    public class PedVertex implements Comparable<PedVertex> {
        private final int id;
        private PedVertex father = null;
        private PedVertex mother = null;
        private final List<PedVertex> children = new ArrayList<>();
        private final boolean isAlive;

        public PedVertex(int id) {
            this(id, -1, -1, true);
        }

        public PedVertex(int id, int fatherID, int motherID, boolean isAlive) {
            this.id = id;
            this.isAlive = isAlive;
            setFather(fatherID);
            setMother(motherID);
        }


        public int getId() {
            return id;
        }

        public int getFatherId() {
            if (father == null)
                return -1;
            else
                return father.getId();
        }

        public int getMotherId() {
            if (mother == null)
                return -1;
            else
                return mother.getId();
        }

        public PedVertex getFather() {
            return father;
        }

        public void setFather(int fatherID) {
            if (father != null && father.getId() != fatherID)
                MyLogger.warn("Overriding existing father " + father + " with " + fatherID + " for child " + this);
            father = getVertex(fatherID);

            if (fatherID != -1)
                father.addChild(this);
        }

        public PedVertex getMother() {
            return mother;
        }

        public void setMother(int motherID) {
            if (mother != null && mother.getId() != motherID)
                MyLogger.warn("Overriding existing mother " + mother + " with " + motherID + " for child " + this);
            mother = getVertex(motherID);

            if (motherID != -1)
                mother.addChild(this);
        }

        public void addChild(PedVertex v) {
            this.children.add(v);
        }

        public List<PedVertex> getChildren() {
            return children;
        }

        //public void setChildren(List<PedVertex> children) {
        //	this.children = children;
        //}
        public boolean isFounder() {
            return father == null && mother == null;
        }

        public boolean isAlive() {
            return isAlive;
        }

        public String toString() {
            //	return id + "";
            return String.format("id: %d, father: %d, mother: %d, isAlive: %s, isFounder: %s",
                    id, getFatherId(), getMotherId(), isAlive(), isFounder());
        }

        public int compareTo(PedVertex other) {
            return Integer.compare(getChildren().size(), other.getChildren().size());
        }

        public void overrideFather(int fatherID) {
            father = getVertex(fatherID);
        }

        public void overrideMother(int motherID) {
            mother = getVertex(motherID);
        }

        public int distanceTo(PedVertex v2) {
            List<PedVertex> ancestors1 = new ArrayList<>();
            List<PedVertex> ancestors2 = new ArrayList<>();
            List<Integer> degrees1 = new ArrayList<>();
            List<Integer> degrees2 = new ArrayList<>();

            addAncestry(this, 0, ancestors1, degrees1);
            addAncestry(v2, 0, ancestors2, degrees2);

            final List<PedVertex> unsortedAns1 = new ArrayList<>(ancestors1);
            final List<PedVertex> unsortedAns2 = new ArrayList<>(ancestors2);
            final List<Integer> unsortedDegrees1 = new ArrayList<>(degrees1);
            final List<Integer> unsortedDegrees2 = new ArrayList<>(degrees2);

            Collections.sort(degrees1);
            Collections.sort(degrees2);
            ancestors1.sort((v1, v21) -> {
                int ind1 = 0;
                int ind2 = 0;
                for (PedVertex v : unsortedAns1) {
                    if (v.equals(v1))
                        break;
                    ind1++;
                }
                for (PedVertex v : unsortedAns1) {
                    if (v.equals(v21))
                        break;
                    ind2++;
                }
                return unsortedDegrees1.get(ind1).compareTo(unsortedDegrees1.get(ind2));
            });

            ancestors2.sort((v1, v212) -> {
                int ind1 = 0;
                int ind2 = 0;
                for (PedVertex v : unsortedAns2) {
                    if (v.equals(v1))
                        break;
                    ind1++;
                }
                for (PedVertex v : unsortedAns2) {
                    if (v.equals(v212))
                        break;
                    ind2++;
                }
                return unsortedDegrees2.get(ind1).compareTo(unsortedDegrees2.get(ind2));

            });


            int ind1 = -1;

            for (PedVertex ancestor1 : ancestors1) {
                ind1++;
                int ind2 = 0;
                for (PedVertex ancestor2 : ancestors2) {
                    if (ancestor1.equals(ancestor2))
                        return degrees1.get(ind1) + degrees2.get(ind2);
                    ind2++;
                }
            }
            return Integer.MAX_VALUE;
        }

    }

    public List<int[]> getCommonAncestorDepths(PedVertex v1, PedVertex v2) {
        List<PedVertex> ancestors1 = new ArrayList<>();
        List<PedVertex> ancestors2 = new ArrayList<>();
        List<Integer> degrees1 = new ArrayList<>();
        List<Integer> degrees2 = new ArrayList<>();

        //MyLogger.important("getCommonAncestorDepth: " + v1 + "," + v2);
        addAncestry(v1, 0, ancestors1, degrees1);
        addAncestry(v2, 0, ancestors2, degrees2);

        List<int[]> depthList = new ArrayList<>();
        while (true) {
            List<PedVertex> commonAncAncestors = addCommonAncestorDepth(ancestors1, ancestors2, degrees1, degrees2, depthList);
            if (commonAncAncestors == null)
                return depthList;
            //remove ancestors of common ancestors, as they all are common ancestors
            for (PedVertex greatAncestor : commonAncAncestors) {
                //MyLogger.important("Removing great ancestor from ancestor lists: " + greatAncestor);
                int toRemoveIndex1 = ancestors1.indexOf(greatAncestor);
                int toRemoveIndex2 = ancestors2.indexOf(greatAncestor);
                if (ancestors1.contains(greatAncestor)) {
                    ancestors1.remove(greatAncestor);
                    degrees1.remove(toRemoveIndex1);
                }
                if (ancestors2.contains(greatAncestor)) {
                    ancestors2.remove(greatAncestor);
                    degrees2.remove(toRemoveIndex2);
                }
            }
        }
    }

    private List<PedVertex> addCommonAncestorDepth(List<PedVertex> ancestors1, List<PedVertex> ancestors2, List<Integer> degrees1,
                                                   List<Integer> degrees2, List<int[]> depthList) {
        List<PedVertex> commonAncAncestors = new ArrayList<>();
        int ind1 = 0;
        for (PedVertex ancestor1 : ancestors1) {
            int ind2 = 0;
            for (PedVertex ancestor2 : ancestors2) {

                if (ancestor1.equals(ancestor2)) {
                    //MyLogger.important("addCommonAncestorDepth:common ancestor" + ancestor1);
                    //MyLogger.important("addCommonAncestorDepth:ind1-ind2=" + ind1 + "-"+ind2);
                    //MyLogger.important("ancestors1: " + ancestors1);
                    //MyLogger.important("ancestors2: " + ancestors2);
                    //MyLogger.important("degrees1: " + degrees1);
                    //MyLogger.important("degrees2: " + degrees2);
                    int[] depths = new int[2];
                    depths[0] = degrees1.get(ind1);
                    depths[1] = degrees2.get(ind2);
                    //MyLogger.important("depth="+depths[0] +","+depths[1]);
                    depthList.add(depths);
                    List<Integer> degrees = new ArrayList<>();

                    addAncestry(ancestors1.get(ind1), degrees1.get(ind1), commonAncAncestors, degrees);
                    return commonAncAncestors;
                }
                ind2++;
            }
            ind1++;
        }
        return null;
    }

    public void pruneExtinct(List<PedVertex> lastGen) {
        List<Integer> degrees = new ArrayList<>();
        List<PedVertex> ancestors = new ArrayList<>();

        for (PedVertex v : lastGen) {
            addAncestry(v, 0, ancestors, degrees);
        }
        for (PedVertex v : getVertices()) {
            if (!ancestors.contains(v)) {
                MyLogger.important("remove " + v);
                removeVertex(v.getId());
            }
        }
    }

    public List<Integer> getMates(int id) {
        PedVertex me = getVertex(id);
        List<PedVertex> children = me.getChildren();
        List<Integer> mates = new ArrayList<>();
        for (PedVertex child : children) {
            if (child.getFatherId() == id && !mates.contains(child.getMotherId()))
                mates.add(child.getMotherId());
            else if (child.getMotherId() == id && !mates.contains(child.getFatherId()))
                mates.add(child.getFatherId());
        }
        return mates;
    }
}