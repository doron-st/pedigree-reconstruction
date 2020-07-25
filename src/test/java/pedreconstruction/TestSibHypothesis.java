package pedreconstruction;

import com.google.common.io.Resources;
import graph.Graph;
import graph.Weight;
import misc.MyLogger;
import graph.Vertex;
import graph.VertexData;
import org.junit.Test;
import pedigree.Person;
import relationship.RelationshipProbWeight;
import relationship.SibHypothesisTester;
import pedigree.Pedigree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static relationship.Relationship.FULL_SIB;

public class TestSibHypothesis {

    @Test
    public void test() {

        String demographicsFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.ibd").getFile();
        String pedigreeFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.structure").getFile();

        Pedigree ped;
        Graph ibdGraph;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographicsFile);
            Population population = new Population(persons);
            ibdGraph = new Graph(persons);
            MyLogger.info("====================Adding IBD Features edges===============================");
            IBDFeaturesWeight.readEdgesWeights(ibdGraph, ibdFile, population);        // Adding edges to the graph
            MyLogger.important("Graph is " + ibdGraph);
            ped = new Pedigree(population);

        } catch (IOException e) {
            throw new RuntimeException("Error...", e);
        }

        Pedigree fullPed = new Pedigree();
        fullPed.readFromFile(pedigreeFile);

        SibHypothesisTester tester = new SibHypothesisTester(ibdGraph, false, true, true);
        Contraction contraction = new Contraction(ped);
        Graph contractedRelationGraph = contraction.createEdgelessContractedGraph();

        // currently sibs are not distinguished well from parent-child relationship
        // this is left for follow-up research and work.
        // method currently works on a single generation so this is not a problem
        MyLogger.important("Test sibs");
        testSib(ped, fullPed, tester, contractedRelationGraph, 414, 464, 0.065);
        testSib(ped, fullPed, tester, contractedRelationGraph, 464, 414, 0.065);

        MyLogger.important("Test unrelated");
        testSib(ped, fullPed, tester, contractedRelationGraph, 414, 415, 0);
    }

    private void testSib(Pedigree ped, Pedigree fullPed, SibHypothesisTester tester,
                         Graph contractedRelationGraph, int s1, int s2, double expectedProbability) {

        List<Vertex> sibs = new ArrayList<>();
        sibs.add(contractedRelationGraph.getVertex(s1));
        sibs.add(contractedRelationGraph.getVertex(s2));
        tester.run(ped, contractedRelationGraph, sibs, 1, fullPed);

        RelationshipProbWeight weight = contractedRelationGraph.getWeight(sibs.get(0), sibs.get(1));
        if(weight == null && expectedProbability == 0)
            return;

        assert weight != null;

        double fullSibProb = weight.getProb(FULL_SIB);
        assertEquals(fullSibProb, expectedProbability, 0.1);
    }
}
