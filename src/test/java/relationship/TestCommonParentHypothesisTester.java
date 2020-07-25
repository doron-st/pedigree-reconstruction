package relationship;


import com.google.common.io.Resources;
import graph.Graph;
import graph.VertexData;
import misc.MyLogger;
import org.junit.Test;
import pedigree.NucFamily;
import pedigree.Pedigree;
import pedigree.Person;
import pedreconstruction.Contraction;
import pedreconstruction.IBDFeaturesWeight;
import pedreconstruction.Population;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCommonParentHypothesisTester {
    Contraction contraction;

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

        boolean phased = true;
        CommonParentHypothesisTester tester = new CommonParentHypothesisTester(ibdGraph, false, phased);
        contraction = new Contraction(ped);
        Graph contractedRelationGraph = contraction.createEdgelessContractedGraph();

        MyLogger.important("Test sibs");
        testCommonParent(ped, ibdGraph, fullPed, tester, contractedRelationGraph, 414, 464);
    }

    private void testCommonParent(Pedigree ped, Graph ibdGraph,
                                  Pedigree fullPed, CommonParentHypothesisTester tester,
                                  Graph contractedRelationGraph, int s1, int s2) {
        List<Person> sib1 = new ArrayList<>();
        sib1.add((Person) ibdGraph.getVertex(s1).getData());
        List<Person> sib2 = new ArrayList<>();
        sib2.add((Person) ibdGraph.getVertex(s2).getData());
        testCommonParent(ped, ibdGraph, fullPed, tester, contractedRelationGraph, sib1, sib2);
    }

    private void testCommonParent(Pedigree ped, Graph IBDgraph,
                                  Pedigree fullPed, CommonParentHypothesisTester tester,
                                  Graph contractedRelationGraph, List<Person> sib1, List<Person> sib2) {

        List<NucFamily> nucFamilies = new ArrayList<>();
        nucFamilies.add(new NucFamily(new Person(1, "1", 50, false, 1), new Person(2, "2", 50, true, 1), sib1));
        nucFamilies.add(new NucFamily(new Person(3, "3", 50, false, 1), new Person(4, "4", 50, true, 1), sib2));
		tester.run(ped, contraction, nucFamilies);
    }

}
