package prepare.pedreconstruction;

import prepare.common.Population;
import prepare.graph.Graph;
import prepare.graph.SuperVertex;
import prepare.graph.Vertex;
import prepare.misc.MyLogger;
import prepare.pedigree.*;
import prepare.relationship.CommonParentHypothesisTester;
import prepare.relationship.RelationshipProbWeight;
import prepare.relationship.SibHypothesisTester;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static prepare.relationship.Relationship.CHILD;
import static prepare.relationship.Relationship.PARENT;

/**
 * Implementing reconstruct inference based on posterior prepare.relationship probabilities file
 */
public class PedigreeBuilder {
    private final Graph graph;
    private int totalParents = 0;
    private int generation = 1;
    private final File outputDir;
    private final boolean polygamous;
    private final boolean synchronous;
    private final boolean phased;

    public PedigreeBuilder(Graph graph, String outputDir, boolean poly, boolean sync, boolean phasedInput) {
        this.graph = graph;
        this.outputDir = new File(outputDir);
        this.polygamous = poly;
        this.synchronous = sync;
        this.phased = phasedInput;
    }

    /**
     * Main method - using actual pedigree for IBD sharing estimation
     */
    public void buildGeneration(Pedigree ped, int gen, Population dem) {

        this.generation = gen;
        Graph IBDGraph = graph;

        NextIDGenerator nextIDGen = new NextIDGenerator(gen);

        SibHypothesisTester sibHypTester = new SibHypothesisTester(IBDGraph, synchronous, phased);
        CommonParentHypothesisTester commonParentHypothesisTester = new CommonParentHypothesisTester(IBDGraph, synchronous, phased);

        MyLogger.important(">>> buildGeneration" + gen);
        Contraction contraction = new Contraction(ped);

        MyLogger.important("===================Test for full-sib hypothesis (" + gen + " )====================");
        Graph contractedRelationGraph;
        contractedRelationGraph = contraction.createEdgelessContractedGraph();

        sibHypTester.run(ped, contractedRelationGraph, contractedRelationGraph.getVertices(), gen);
        SibGraphExpander sge = new SibGraphExpander(contractedRelationGraph, contraction);

        if (polygamous) {
            MyLogger.important("===========Remove couple's fullSib double edges if existing(" + gen + ")==========");
            sge.removeRedundantEdges(ped);
        }
        MyLogger.important("===================Expand contracted sibs prepare.graph (" + gen + " )====================");
        Graph sibExpendedGraph = sge.run();

        if (gen == 1 && !synchronous) {
            MyLogger.important("===================Detect living parents (" + gen + " )====================");
            assignLivingParents(contractedRelationGraph);
        }

        //Search for maximal weight independent set of sib cliques
        MyLogger.important("======================Partition Full-Sibs(" + gen + ")=====================");
        SiblingGrouper sibGrouper = new SiblingGrouper(sibExpendedGraph);
        sibGrouper.detectSiblings(ped);

        // Retrieve group of siblings
        NuclearFamilyCreator nuclearFamilyCreator = new NuclearFamilyCreator(sibExpendedGraph, gen, nextIDGen);
        List<List<Vertex>> siblingGroups = nuclearFamilyCreator.getSiblingGroups();

        // process siblings groups to create Nuclear Families
        MyLogger.important("===========Create Nuclear Families (" + gen + ")==========");
        List<NucFamily> nucFamilies = nuclearFamilyCreator.createNuclearFamilies(siblingGroups, false);

        if (polygamous) {
            Contraction sibContraction = new Contraction(nucFamilies);

            MyLogger.important("===========Test Nuclear Families for prepare.common parent(" + gen + ")==========");
            Graph halfSibGraph = commonParentHypothesisTester.run(ped, sibContraction, nucFamilies);
            HalfSibGraphExpander hsge = new HalfSibGraphExpander(halfSibGraph, ped, sibContraction);
            MyLogger.important("===========Remove couple's double halfSib edges if existing(" + gen + ")==========");
            Graph expendedHalfSibGraph = hsge.run();

            MyLogger.important("======================Partition Half-Sibs(" + gen + ")=====================");
            sibGrouper = new SiblingGrouper(expendedHalfSibGraph);
            sibGrouper.uniteCommonParentOfHalfSibs(nucFamilies);
        }
        MyLogger.important("======================Update pedigree(" + gen + ")=====================");
        updatePedigreeObj(ped, nucFamilies);

        MyLogger.important("=========================Writing output========================");
        try {
            ped.writeToFile(outputDir, dem);
        } catch (IOException e) {
            throw new RuntimeException("Failed opening out file when writing generation " + gen, e);
        }
    }

    /**
     * Find best scoring mother and father, from list of potential parents
     */
    private void assignLivingParents(Graph graph) {

        List<Vertex> existingVertices = new ArrayList<>(graph.getVertexMap().values());
        //traverse all prepare.graph vertices
        for (Vertex v : existingVertices) {

            Person currPerson = (Person) ((SuperVertex) v.getData()).getInnerVertices().get(0).getData();

            //Find curr vertex parents.
            for (prepare.graph.Edge e : v.getEdgeMap().values()) {
                RelationshipProbWeight weight = (RelationshipProbWeight) e.getWeight();
                Vertex u = e.getVertex2();
                Person otherPerson = (Person) ((SuperVertex) u.getData()).getInnerVertices().get(0).getData();

                if (weight.isMaxProbCategory(PARENT)) {
                    if (otherPerson.getGender() && currPerson.getFamily().motherProbability < weight.getProb(PARENT)) {
                        currPerson.getFamily().mother = otherPerson;
                        currPerson.getFamily().motherProbability = weight.getProb(PARENT);
                        MyLogger.important(otherPerson.toString() + " is the mother of " + currPerson.toString());
                        totalParents++;
                    }

                    if (!otherPerson.getGender() && currPerson.getFamily().fatherProbability < weight.getProb(PARENT)) {
                        currPerson.getFamily().father = otherPerson;
                        currPerson.getFamily().fatherProbability = weight.getProb(PARENT);
                        MyLogger.important(otherPerson.toString() + " is the father of " + currPerson.toString());
                        totalParents++;
                    }
                }
                if (weight.isMaxProbCategory(CHILD)) {
                    if (currPerson.getGender() && otherPerson.getFamily().motherProbability < weight.getProb(CHILD)) {
                        otherPerson.getFamily().mother = currPerson;
                        otherPerson.getFamily().motherProbability = weight.getProb(CHILD);
                        MyLogger.important(currPerson.toString() + " is the mother of " + otherPerson.toString());
                        totalParents++;
                    }
                    if (!currPerson.getGender() && otherPerson.getFamily().fatherProbability < weight.getProb(CHILD)) {
                        otherPerson.getFamily().father = currPerson;
                        otherPerson.getFamily().fatherProbability = weight.getProb(CHILD);
                        MyLogger.important(currPerson.toString() + " is the father of " + otherPerson.toString());
                        totalParents++;
                    }
                }
            }

            /*
             * Mark chosen edges, so that they would not be selected again.
             */
            for (prepare.graph.Edge e : v.getEdgeMap().values()) {
                Person otherPerson = (Person) ((SuperVertex) e.getVertex2().getData()).getInnerVertices().get(0).getData();
                if (otherPerson == currPerson.getFamily().father || otherPerson == currPerson.getFamily().mother) {
                    RelationshipProbWeight weight = (RelationshipProbWeight) e.getWeight();
                    MyLogger.debug(otherPerson.getId() + " " + otherPerson.getAge() + " is a parent of " + currPerson.getId() + " " + currPerson.getAge());
                    weight.makeDeterministicChoice(PARENT);
                }
                if (otherPerson.getFamily().father == currPerson || otherPerson.getFamily().mother == currPerson) {
                    RelationshipProbWeight weight = (RelationshipProbWeight) e.getWeight();
                    //System.out.println("before deterministic choice: " + weight);
                    MyLogger.debug(currPerson.getId() + " is a parent of " + otherPerson.getId());
                    weight.makeDeterministicChoice(CHILD);
                }
            }
        }
        MyLogger.important("Found " + totalParents + " parents");
    }


    private void updatePedigreeObj(Pedigree ped, List<NucFamily> nuclearFamilies) {
        MyLogger.important("PedigreeBuilder::updatePedigreeObj");
        for (NucFamily fam : nuclearFamilies) {
            int fatherID = fam.getFather().getId();
            int motherID = fam.getMother().getId();
            ped.addVertex(fatherID);
            ped.addVertex(motherID);
            for (Person child : fam.siblings) {
                int id = child.getId();
                if (!ped.hasVertex(id))
                    ped.addVertex(id, fatherID, motherID, child.isAlive());
                else {
                    ped.getVertex(id).setFather(fatherID);
                    ped.getVertex(id).setMother(motherID);
                }
            }
        }
        ped.calcExpectedFounderAges(this.generation);
    }
}
