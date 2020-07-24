package prepare;

import graph.Graph;
import graph.MyLogger;
import graph.VertexData;
import pedigree.Person;
import simulator.Pedigree;

import java.io.IOException;
import java.util.List;


public class Prepare {

    public static void main(String[] args) {
        String IBDFile = args[0];
        // demographics file
        String demographFilename = args[1];
        // output dir
        String out = args[2];

        boolean polygamous = false;
        boolean phasedInput = false;
        if (args.length == 4 && args[3].equals("-pol") || args.length == 5 && (args[4].equals("-pol") || args[4].equals("-pol"))) {
            polygamous = true;
            MyLogger.important("Polygamous mode!");
        } else {
            MyLogger.important("Monogamous mode!");

        }
        if (args.length == 4 && args[3].equals("-phased") || args.length == 5 && (args[4].equals("-phased") || args[4].equals("-phased"))) {
            phasedInput = true;
            MyLogger.important("Phased input mode!");
        } else {
            MyLogger.important("Unphased input mode!");

        }


        Pedigree ped;
        Graph IBDgraph;
        Demographics demographics;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographFilename);
            demographics = new Demographics(persons);
            IBDgraph = new Graph(persons);
            MyLogger.info("====================Adding IBD Features edges===============================");
            IBDFeaturesWeight.readEdgesWeights(IBDgraph, IBDFile, demographics);// Adding edges to the graph
            MyLogger.info("Graph is " + IBDgraph);
            ped = new Pedigree(demographics, false);

        } catch (IOException e) {
            throw new RuntimeException("Error...", e);
        }

        int generation = 1;
        int startFromGen = 1;

        Pedigree fullPed = new Pedigree();
        //fullPed.readFromFile("D://workspace/pedigree/500_500/3g/pedigree.structure");
        //fullPed.readFromFile("D://workspace/pedigree/polygamous_160CeuYri_100_150/pedigree.structure");
        //fullPed.readFromFile("D://workspace/pedigree/200_200_5/pedigree.structure");


        if (startFromGen > 1) {
            ped.readFromFile(out + (startFromGen - 1));
            //ped.readFromFile("D://workspace/pedigree/WFSim/pol200_3g_0.8/pedigree.structure1");
            ped.calcExpectedFounderAges((startFromGen - 1));
            generation = (startFromGen);
        }

        boolean synchronous = true;

        for (int gen = generation; gen <= 4; gen++) {
            PedigreeBuilder pedBuilder = new PedigreeBuilder(IBDgraph, out + gen, polygamous, synchronous, phasedInput);
            pedBuilder.buildGeneration(ped, gen, fullPed, demographics);
        }
    }
}
