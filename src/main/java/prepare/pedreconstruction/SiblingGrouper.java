package prepare.pedreconstruction;

import prepare.graph.*;
import prepare.graph.SimpleGraph.SimpleVertex;
import prepare.misc.MyLogger;
import prepare.pedigree.NucFamily;
import prepare.pedigree.Person;
import prepare.relationship.RelationshipProbWeight;
import prepare.pedigree.Pedigree;

import java.util.*;

import static prepare.relationship.Relationship.FULL_SIB;
import static prepare.relationship.Relationship.HALF_SIB;


public class SiblingGrouper {
    Graph graph;
    Pedigree ped;
    int totalSibs = 0;

    public SiblingGrouper(Graph graph) {
        this.graph = graph;
    }

    /**
     * Sibling detection - Clique method
     */
    public void detectSiblings(Pedigree pedigree) {

        List<Vertex> existingVertices = new ArrayList<>(graph.getVertexMap().values());
        SimpleGraph sibGraph = new SimpleGraph();
        ped = pedigree;

        /*
         * Create sibs prepare.graph
         */
        for (Vertex v : existingVertices) {
            sibGraph.createSimpleVertex(v.getVertexId().toString());
        }

        for (Vertex v : existingVertices) {
            for (Edge e : v.getEdgeMap().values()) {
                RelationshipProbWeight weight = (RelationshipProbWeight) e.getWeight();

                if (weight.isMaxProbCategory(FULL_SIB)) {
                    MyLogger.debug(v.getVertexId() + "might be sib with " + e.getVertex2().getVertexId());
                    if (!sibGraph.getNeighbors(sibGraph.getVertex(v.getVertexId().toString())).contains(sibGraph.getVertex(e.getVertex2().getVertexId().toString()))) {
                        sibGraph.createSimpleEdge(v.getVertexId().toString(), e.getVertex2().getVertexId().toString(), weight.getProb(FULL_SIB));
                    }
                }
            }
        }

        //Compute connected components
        Map<SimpleVertex, SimpleGraph> CC = GraphAlgorithms.SCC(sibGraph);

        List<SimpleGraph> list = new ArrayList<>(CC.values());
        //Identify each CC in the Sibs-prepare.graph as siblings, (include thining for large CC's)
        maxCliqueAsSibs(sibGraph, list);
        MyLogger.important("Found " + totalSibs + " sibs");
    }

    public void uniteCommonParentOfHalfSibs(List<NucFamily> families) {
        //Create prepare.graph of nuclear families with prepare.common parent edges
        SimpleGraph nucGraph = new SimpleGraph();
        for (int i = 0; i < families.size(); i++) {
            nucGraph.createSimpleVertex(i + ":" + families.get(i).siblings.toString());
        }

        for (int i = 0; i < families.size(); i++) {
            NucFamily fam1 = families.get(i);
            for (int j = i + 1; j < families.size(); j++) {
                NucFamily fam2 = families.get(j);

                //Looking at two nuclear families with potential prepare.common parent
                double halfSibProbSum = 0;
                int numPairs = 0;

                //Already have a prepare.common parent
                if (fam1.getFather().equals(fam2.getFather()) || fam1.getMother().equals(fam2.getMother())) {
                    continue;
                }
                //Two parents are alive, assume would be a mistake in half-sib prepare.relationship
                if ((fam1.getFather().isAlive() && fam1.getMother().isAlive())
                        || (fam2.getFather().isAlive() && fam2.getMother().isAlive())) {
                    continue;
                }
                for (Vertex sib1 : graph.verticesFromDatas(fam1.siblings)) {
                    for (Vertex sib2 : graph.verticesFromDatas(fam2.siblings)) {
                        numPairs++;
                        if (sib1.hasEdgeTo(sib2.getVertexId())) {
                            Edge e = sib1.getEdgeTo(sib2.getVertexId());
                            RelationshipProbWeight w = (RelationshipProbWeight) e.getWeight();
                            if (w.isMaxProbCategory(HALF_SIB))
                                halfSibProbSum++;
                        } else if (sib2.hasEdgeTo(sib1.getVertexId())) {
                            Edge e = sib2.getEdgeTo(sib1.getVertexId());
                            RelationshipProbWeight w = (RelationshipProbWeight) e.getWeight();
                            if (w.isMaxProbCategory(HALF_SIB))
                                halfSibProbSum++;
                        }
                    }
                }
                double commonParentScore = halfSibProbSum / numPairs;

                if (commonParentScore == 1) {
                    MyLogger.important("SiblingGroupuer::prepare.common parent score > thresh: " + families.get(i).siblings + " " + families.get(j).siblings);
                    nucGraph.addEdge(nucGraph.getVertex(i + ":" + families.get(i).siblings.toString()), nucGraph.getVertex(j + ":" + families.get(j).siblings.toString()), commonParentScore);
                }
            }
        }

        MyLogger.info(nucGraph.toString());
        //Compute connected components

        List<SimpleGraph> CC = new ArrayList<>(GraphAlgorithms.SCC(nucGraph).values());

        /*
         * Find half-sib cliques in each CC
         */
        for (int i = 0; i < CC.size(); i++) {
            SimpleGraph g = CC.get(i);

            if (g.getVertices().size() == 1)
                continue;


            if (thinLargeCC(15, g, CC))
                continue;

            MyLogger.important("ConnectedComponent " + i);

            for (SimpleVertex vertex : g.getVertices()) {
                String[] nameSplit = vertex.name.split(":");
                int famIdx = Integer.parseInt(nameSplit[0]);
                NucFamily fam = families.get(famIdx);
                MyLogger.important(fam.toString());
            }

            MyLogger.important(g.toString());
            List<SimpleGraph> cliques = new ArrayList<>();
            // Add other sizes of cliques (from largest to smallest)
            cliques.addAll(GraphAlgorithms.findCliques(g, 4));
            cliques.addAll(GraphAlgorithms.findCliques(g, 3));
            cliques.addAll(GraphAlgorithms.findCliques(g, 2));

            List<List<NucFamily>> allCliquesForJoining = new ArrayList<>();

            //Join the largest clique containing each vertex
            for (SimpleGraph clique : cliques) {
                List<SimpleVertex> nodes = clique.getVertices();
                List<NucFamily> toBeJoined = new ArrayList<>();
                boolean alreadyJoined = false;

                //Check if nucFamilies were joined by the edge weights
                for (SimpleVertex node1 : nodes) {
                    SimpleVertex v1 = nucGraph.getVertex(node1.name);

                    for (SimpleVertex node2 : nodes) {
                        if (node1 == node2) {
                            continue;
                        }
                        SimpleVertex v2 = nucGraph.getVertex(node2.name);
                        if (nucGraph.getEdgeWeight(v1, v2) < 0) {
                            alreadyJoined = true;
                        }
                    }
                }
                //Join  clique nodes a a prepare.common parent
                if (!alreadyJoined) {
                    for (SimpleVertex node1 : nodes) {
                        String[] nameSplit = node1.name.split(":");
                        int nucIdx = Integer.parseInt(nameSplit[0]);
                        toBeJoined.add(families.get(nucIdx));
                    }
                    allCliquesForJoining.add(toBeJoined);

                    //Mark clique edges as joined
                    for (SimpleVertex node1 : nodes) {
                        SimpleVertex v1 = nucGraph.getVertex(node1.name);
                        for (SimpleVertex node2 : nodes) {
                            if (node1 == node2) {
                                continue;
                            }
                            SimpleVertex v2 = nucGraph.getVertex(node2.name);
                            nucGraph.setEdgeWeight(v1, v2, -1);
                        }
                    }
                }
            }
            uniteAllCommonParents(allCliquesForJoining);
        }
    }

    /**
     * @param maxSize max CC size
     * @param g             - Connected component prepare.graph
     * @param list          - list of connected components in sib-Graph
     * @return true if CC was to large, in this case need to skip addition of sibs,
     * as the subCC will be added to the end of the list
     */
    private boolean thinLargeCC(int maxSize, SimpleGraph g, List<SimpleGraph> list) {

        if (g.getVertices().size() > maxSize) {
            Map<SimpleVertex, SimpleGraph> subCC = null;
            MyLogger.warn("Sibling CC is too large!!");
            Random rg = new Random();
            int largestCCSize = g.getVertices().size();
            while (largestCCSize > maxSize) {
                MyLogger.important("largestCC=" + largestCCSize);
                for (SimpleVertex v : g.getVertices()) {
                    for (SimpleVertex u : g.getVertices()) {
                        if (g.getNeighbors(v).contains(u)) {
                            if (g.getEdgeWeight(v, u) < rg.nextDouble() + 0.05) {
                                g.removeEdge(v, u);
                                MyLogger.important("Removing edge " + v + "," + u);
                            }
                        }
                    }
                }
                subCC = GraphAlgorithms.SCC(g);
                largestCCSize = 0;
                for (SimpleGraph subg : subCC.values()) {
                    //System.out.println(subg.toString());

                    if (subg.getVertices().size() > largestCCSize)
                        largestCCSize = subg.getVertices().size();
                }
            }
            MyLogger.important("largestCC=" + largestCCSize);


            //Add new subCC to the end of the list
            list.remove(g);
            MyLogger.important("Removed " + g);
            for (SimpleVertex root : subCC.keySet()) {
                list.add(subCC.get(root));
                MyLogger.important("Added subCC: " + subCC.get(root));
            }
            return true;
        }
        return false;
    }

    private void maxCliqueAsSibs(SimpleGraph sibGraph, List<SimpleGraph> list) {
        /*
         * Find sib cliques in each CC
         */
        for (int i = 0; i < list.size(); i++) {
            SimpleGraph g = list.get(i);
            MyLogger.important("ConnectedComponent " + i);
            MyLogger.important(g.getVertices().toString());
            //MyLogger.important(g.toString());


            int ccSize = g.getVertices().size();
            int maxCliqueSize = 7;
            if (ccSize == 1)
                continue;

            if (thinLargeCC(15, g, list))
                continue;

            //find max clique
            List<SimpleGraph> cliques = new ArrayList<>();
            // Add different sizes of cliques
            for (int size = maxCliqueSize; size >= 2; size--) {
                cliques.addAll(GraphAlgorithms.findCliques(g, size));
                if (!cliques.isEmpty()) {
                    double maxWeight = 0;
                    SimpleGraph maxClique = null;
                    for (SimpleGraph clique : cliques) {
                        double weight = sibGraph.calcSumOfEdgesWeight(clique.getVertices());
                        if (weight > maxWeight) {
                            maxWeight = weight;
                            maxClique = clique;
                        }
                    }

                    MyLogger.important("Siblings clique was found : " + maxClique.getVertices());
                    //Add clique as siblings (historically create a prepare.graph of cliques with a single vertex)
                    SimpleGraph cliqueGraph = new SimpleGraph();
                    cliqueGraph.createSimpleVertex(maxClique.getVertices().toString(), sibGraph.calcSumOfEdgesWeight(maxClique.getVertices()));
                    updateSibsInGraph(graph, GraphAlgorithms.findCliques(cliqueGraph, 1).get(0));
                    //remove clique from sibs prepare.graph
                    for (SimpleVertex v : maxClique.getVertices()) {
                        g.removeVertex(v);
                    }
                    //remove mate parallel edges
                    for (SimpleVertex v : maxClique.getVertices()) {
                        for (SimpleVertex u : maxClique.getVertices()) {
                            List<Integer> vMates = ped.getMates(Integer.parseInt(v.name));
                            List<Integer> uMates = ped.getMates(Integer.parseInt(u.name));
                            for (Integer vMateID : vMates)
                                for (Integer uMateID : uMates) {
                                    SimpleVertex vMate = g.getVertex(vMateID.toString());
                                    SimpleVertex uMate = g.getVertex(vMateID.toString());

                                    if (vMate != null && uMate != null && g.getNeighbors(vMate).contains(uMate)) {
                                        g.removeEdge(vMate, uMate);
                                        MyLogger.important("Already found sib edge between " + v + " and " + u);
                                        MyLogger.important("Remove mate " + vMateID + " edge to " + uMateID);
                                    }
                                }
                        }
                    }
                    cliques = new ArrayList<>();
                    size++;//try finding other cliques with teh same size
                }
            }
        }
    }

    /**
     * update the Family.siblings field for a specific Connected component
     *
     * @param bestIC - A prepare.graph representing all of the sib-cliques in a Connected Component
     *               Each node in the prepare.graph is named: [[sib1, sib2].[sib1, sib2, sib3]...]
     */
    private void updateSibsInGraph(Graph graph, SimpleGraph bestIC) {
        int numOfSibsAdded = 0;
        for (SimpleVertex sibsInCC : bestIC.getVertices()) {
            String[] sibGroups = sibsInCC.name.split("]");

            for (String group : sibGroups) {
                String[] sibNames = group.split(",");
                numOfSibsAdded += sibNames.length * (sibNames.length - 1) / 2;
                totalSibs += numOfSibsAdded;
                for (String name1 : sibNames) {
                    name1 = name1.replace("[", "");
                    name1 = name1.replace(" ", "");
                    for (String name2 : sibNames) {
                        name2 = name2.replace("]", "");
                        name2 = name2.replace("[", "");
                        name2 = name2.replace(" ", "");


                        if (!name1.equals(name2)) {//don't add person as a sibling to himself

                            //System.out.println("name1=" + Integer.parseInt((name1)) + " name2=" + Integer.parseInt(name2));
                            Person p1 = (Person) graph.getVertex(Integer.parseInt(name1)).getData();
                            Person p2 = (Person) graph.getVertex(Integer.parseInt(name2)).getData();
                            MyLogger.debug("p1=" + p1.getId() + " sibs= " + p1.getFamily().siblings);
                            MyLogger.debug("p2=" + p2.getId() + " sibs= " + p2.getFamily().siblings);

                            if (!p1.getFamily().siblings.contains(p2)) {
                                p1.getFamily().siblings.add(p2);
                                p2.getFamily().siblings.add(p1);
                                //MyLogger.info("Adding sibling pair: " + p1.getId() + "," + p2.getId());
                            }
                            //merge clicque huristic
                            MyLogger.debug("p1 " + p1.getId() + " sibs= " + p1.getFamily().siblings);
                            MyLogger.debug("p2 " + p2.getId() + " sibs= " + p2.getFamily().siblings);

                            //create a merged list from the 2 sibling lists
                            List<Person> allSibs = p1.getFamily().siblings;
                            for (Person sib : p1.getFamily().siblings) {
                                if (!allSibs.contains(sib))
                                    allSibs.add(sib);
                            }

                            for (Person sib1 : allSibs) {
                                for (Person sib2 : allSibs) {
                                    if (!sib1.getFamily().siblings.contains(sib2) && sib1 != sib2) {
                                        sib1.getFamily().siblings.add(sib2);
                                    }
                                }
                            }
                            for (Person sib : allSibs)
                                MyLogger.debug(sib.getId() + " sibs= " + sib.getFamily().siblings);

                        }
                    }
                }
            }
        }
        MyLogger.important("Added " + numOfSibsAdded + " Sibling pairs");
    }

    private void uniteAllCommonParents(List<List<NucFamily>> familyCliques) {
        if (familyCliques.size() == 0)
            return;

        MyLogger.debug("uniteAllCommonParents");
        MyLogger.important("num of cliques = " + familyCliques.size());

        //Resolve gender if one parent is alive
        MyLogger.important("join nuclear families with one living parent");

        List<Integer> toRemove = new ArrayList<>();
        int index = 0;
        for (List<NucFamily> clique : familyCliques) {
            boolean fatherAlive = false;
            boolean motherAlive = false;

            for (NucFamily fam : clique) {
                if (fam.getFather().isAlive()) {
                    fatherAlive = true;
                    MyLogger.debug("father is alive: " + fam);
                }
                if (fam.getMother().isAlive()) {
                    motherAlive = true;
                    MyLogger.debug("mother is alive: " + fam);
                }
            }
            if (motherAlive && fatherAlive) {
                MyLogger.error("uniteAllCommonParents::Both parents are alive");
                return;
            } else if (motherAlive) {
                uniteFather(clique);
                toRemove.add(index);
            } else if (fatherAlive) {
                uniteMother(clique);
                toRemove.add(index);
            }
            index++;
        }
        MyLogger.debug("toRemove= " + toRemove.toString());
        //Remove joined cliques
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            int cliqueIdx = toRemove.get(i);
            familyCliques.remove(cliqueIdx);
        }

        boolean moreResolvableFamilies = true;
        while (moreResolvableFamilies) {
            moreResolvableFamilies = resolveAlreadyJoined(familyCliques);
        }

        while (familyCliques.size() > 0) {
            startGenderResolutionSeed(familyCliques);
            moreResolvableFamilies = true;
            while (moreResolvableFamilies) {
                moreResolvableFamilies = resolveAlreadyJoined(familyCliques);
            }
        }
    }


    private void startGenderResolutionSeed(List<List<NucFamily>> familyCliques) {
        int index = 0;
        List<Integer> toRemove = new ArrayList<>();
        //Just guess gender
        MyLogger.important("start gender resolution seed - guess father");
        for (List<NucFamily> clique : familyCliques) {
            boolean fatherJoined = false;
            boolean motherJoined = false;
            boolean fatherAlive = false;
            boolean motherAlive = false;

            for (NucFamily fam : clique) {
                if (fam.getFather().isAlive()) {
                    fatherAlive = true;
                }
                if (fam.getMother().isAlive()) {
                    motherAlive = true;
                }
                if (fam.wasFatherJoined()) {
                    fatherJoined = true;
                }
                if (fam.wasMotherJoined()) {
                    motherJoined = true;
                }
            }

            if (motherAlive || fatherAlive) {
                index++;
                continue;
            }

            if (motherJoined || fatherJoined) {
                index++;
            }
            else {
                uniteFather(clique);
                toRemove.add(index);
                break;
            }
        }

        MyLogger.debug("toRemove= " + toRemove.toString());
        //Remove joined cliques
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            MyLogger.debug("remove= " + toRemove.get(i));
            int cliqueIdx = toRemove.get(i);
            familyCliques.remove(cliqueIdx);
        }
    }


    private boolean resolveAlreadyJoined(List<List<NucFamily>> familyCliques) {
        List<Integer> toRemove = new ArrayList<>();
        int index = 0;
        boolean resolved = false;
        MyLogger.important("resolveAlready joined: Num of cliques = " + familyCliques.size());
        //Resolve gender if one parent was already joined
        for (List<NucFamily> clique : familyCliques) {
            boolean fatherJoined = false;
            boolean motherJoined = false;
            boolean fatherAlive = false;
            boolean motherAlive = false;

            for (NucFamily fam : clique) {
                MyLogger.debug(fam.getMother().toString());
                MyLogger.debug(fam.getFather().toString());

                if (fam.getFather().isAlive()) {
                    fatherAlive = true;
                }
                if (fam.getMother().isAlive()) {
                    motherAlive = true;
                }
                if (fam.wasFatherJoined()) {
                    fatherJoined = true;
                }
                if (fam.wasMotherJoined()) {
                    motherJoined = true;
                }
            }
            //skip,because clique was joined in the above code
            if (motherAlive || fatherAlive) {
                continue;
            } else if (motherJoined && fatherJoined) {
                MyLogger.error("Both parents are already joined:" + clique.toString());
                toRemove.add(index);
                continue;
            } else if (motherJoined) {
                uniteFather(clique);
                toRemove.add(index);
                resolved = true;
            } else if (fatherJoined) {
                uniteMother(clique);
                toRemove.add(index);
                resolved = true;
            }
            index++;
        }
        //Remove joined cliques
        MyLogger.debug("toRemove= " + toRemove.toString());
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            int cliqueIdx = toRemove.get(i);
            familyCliques.remove(cliqueIdx);
        }
        return resolved;
    }

    private void uniteFather(List<NucFamily> clique) {
        //Unite prepare.common parent
        MyLogger.important("join father of " + clique.toString());

        //unite on the lowest generation up, since can already have a nuclear family created
        int commonFatherGen = 100;
        Person commonFather = null;
        for (NucFamily fam : clique) {
            int gen = fam.getFather().getDiscoveryGeneration();
            if (gen < commonFatherGen) {
                commonFather = fam.getFather();
                commonFatherGen = gen;
            }
            if (fam.wasFatherJoined()) {
                MyLogger.warn("father was already joined, cancell joining!!");
                return;
            }
            if (fam.getFather() != commonFather && fam.getFather().getFamily().father != null) {
                MyLogger.error("Trying to join a father from previous generation, father,cancell joining!");
                return;
            }
        }

        for (NucFamily fam : clique) {
            //if(fam.getFather()!=commonFather){
            //	prepare.graph.removeVertex(prepare.graph.getVertex(fam.getFather().getId()));
            //}
            if (!fam.getFather().equals(commonFather)) {
                for (Vertex sib : graph.verticesFromDatas(fam.siblings)) {
                    MyLogger.important("Set father " + sib + " " + commonFather);
                    ((Person) sib.getData()).getFamily().father = commonFather;
                }
            }
            fam.setFather(commonFather);
            fam.setFatherJoined();
        }
    }

    private void uniteMother(List<NucFamily> clique) {
        //Unite prepare.common parent
        MyLogger.important("join mother of " + clique.toString());
        int commonMotherGen = 100;
        Person commonMother = null;
        for (NucFamily fam : clique) {
            int gen = fam.getMother().getDiscoveryGeneration();
            if (gen < commonMotherGen) {
                commonMother = fam.getMother();
                commonMotherGen = gen;
            }
            if (fam.wasMotherJoined()) {
                MyLogger.warn("mother was already joined, cancell joining!!");
                return;
            }
            if (fam.getMother() != commonMother && fam.getMother().getFamily().mother != null) {
                MyLogger.error("Trying to join a mother from previous generation, mother,cancell joining!");
                return;
            }
        }

        for (NucFamily fam : clique) {
            //if(fam.getMother()!=commonMother){
            //	prepare.graph.removeVertex(prepare.graph.getVertex(fam.getMother().getId()));
            //}
            if (!fam.getMother().equals(commonMother)) {
                for (Vertex sib : graph.verticesFromDatas(fam.siblings)) {
                    MyLogger.important("Set mother " + sib + " " + commonMother);
                    ((Person) sib.getData()).getFamily().mother = commonMother;
                }
            }
            fam.setMother(commonMother);
            fam.setMotherJoined();
        }
    }
}