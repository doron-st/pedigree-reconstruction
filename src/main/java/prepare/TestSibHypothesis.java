package prepare;


import graph.Graph;
import graph.MyLogger;
import graph.Vertex;
import graph.VertexData;
import misc.Person;
import org.junit.Test;
import relationship.SibHypothesisTester;
import simulator.Pedigree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestSibHypothesis {

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

        SibHypothesisTester tester = new SibHypothesisTester(IBDgraph, false, true, true);
        Contraction contraction = new Contraction(ped);
        Graph contractedRelationGraph = contraction.createEdgelessContractedGraph();

        MyLogger.important("Test half-sibs");
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 703, 753);
        MyLogger.important("Test unrelated");
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 714, 774);
        MyLogger.important("Test cousins");
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 641, 649);
        MyLogger.important("Test sibs");
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 785, 809);
        MyLogger.important("Test avuncular");
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 631, 817);
        testSib(ped, IBDgraph, fullPed, tester, contractedRelationGraph, 817, 631);

    }

    private void testSib(Pedigree ped, Graph IBDgraph,
                         Pedigree fullPed, SibHypothesisTester tester,
                         Graph contractedRelationGraph, int s1, int s2) {

        List<Vertex> sibs = new ArrayList<Vertex>();
        sibs.add(contractedRelationGraph.getVertex(s1));
        sibs.add(contractedRelationGraph.getVertex(s2));

        tester.run(ped, contractedRelationGraph, sibs, 1, fullPed);
    }
}
