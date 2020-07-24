package prepare;


import graph.Graph;
import graph.MyLogger;
import graph.VertexData;
import org.junit.Test;
import pedigree.NucFamily;
import pedigree.Person;
import relationship.CommonParentHypothesisTester;
import simulator.Pedigree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCommonParentHypothesis {
    Contraction contraction;

    @Test
    public void test() {

        String IBDFile = "D://workspace/pedigree/polygamous_160CeuYri_100_150/pedigree.haplotypes.phased.IBD.ages";
        // demographics file
        String demographFilename = "D://workspace/pedigree/polygamous_160CeuYri_100_150/pedigree.demographics";

        Pedigree ped;
        Graph IBDgraph;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographFilename);
            Demographics demographics = new Demographics(persons);
            IBDgraph = new Graph(persons);
            MyLogger.info("====================Adding IBD Features edges===============================");
            IBDFeaturesWeight.readEdgesWeights(IBDgraph, IBDFile, demographics);        // Adding edges to the graph
            MyLogger.important("Graph is " + IBDgraph);
            ped = new Pedigree(demographics, false);

        } catch (IOException e) {
            throw new RuntimeException("Error...", e);
        }

        Pedigree fullPed = new Pedigree();
        fullPed.readFromFile("D://workspace/pedigree/polygamous_160CeuYri_100_150/pedigree.structure");

        boolean phased = true;
        CommonParentHypothesisTester tester = new CommonParentHypothesisTester(IBDgraph, false, phased);
        contraction = new Contraction(ped);
        Graph contractedRelationGraph = contraction.createEdgelessContractedGraph();

        MyLogger.important("Test half-sibs");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 703, 753);
        MyLogger.important("Test unrelated");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 714, 774);
        MyLogger.important("Test three sibs");
        List<Person> sib1 = new ArrayList<Person>();
        sib1.add((Person) IBDgraph.getVertex(631).getData());
        sib1.add((Person) IBDgraph.getVertex(662).getData());
        sib1.add((Person) IBDgraph.getVertex(688).getData());
        List<Person> sib2 = new ArrayList<Person>();
        sib2.add((Person) IBDgraph.getVertex(782).getData());
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, sib1, sib2);

        MyLogger.important("Test cousins");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 641, 649);
        MyLogger.important("Test half-cousins");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 782, 631);
        MyLogger.important("Test sibs");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 785, 809);
        MyLogger.important("Test avuncular");
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 631, 817);
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 817, 631);

    }

    private void testCommonParent(Pedigree ped, Graph IBDgraph,
                                  Pedigree fullPed, CommonParentHypothesisTester tester,
                                  Graph contractedRelationGraph, int s1, int s2) {

        List<Person> sib1 = new ArrayList<Person>();
        sib1.add((Person) IBDgraph.getVertex(s1).getData());
        List<Person> sib2 = new ArrayList<Person>();
        sib2.add((Person) IBDgraph.getVertex(s2).getData());
        testCommonParent(ped, IBDgraph, fullPed, tester, contractedRelationGraph, sib1, sib2);
    }

    private void testCommonParent(Pedigree ped, Graph IBDgraph,
                                  Pedigree fullPed, CommonParentHypothesisTester tester,
                                  Graph contractedRelationGraph, List<Person> sib1, List<Person> sib2) {

        List<NucFamily> nucFamilies = new ArrayList<NucFamily>();
        nucFamilies.add(new NucFamily(new Person(1, "1", 50, false, 1), new Person(2, "2", 50, true, 1), sib1));
        nucFamilies.add(new NucFamily(new Person(3, "3", 50, false, 1), new Person(4, "4", 50, true, 1), sib2));
//		tester.run(ped, contractedRelationGraph, contraction, nucFamilies, 1, fullPed);
    }

}
