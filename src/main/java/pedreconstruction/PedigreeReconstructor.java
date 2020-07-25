package pedreconstruction;

import graph.Graph;
import misc.MyLogger;
import graph.VertexData;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import pedigree.Person;
import pedigree.Pedigree;

import java.io.IOException;
import java.util.List;


public class PedigreeReconstructor {
    private final String ibdFilename;
    private final String demographicsFilename;
    private final String outPref;
    private final boolean polygamous;
    private final boolean phasedInput;
    private final int generations;

    public PedigreeReconstructor(String ibdFilename, String demographicsFilename, String outPref, boolean polygamous,
                                 boolean phasedInput, int generations) {
        this.ibdFilename = ibdFilename;
        this.demographicsFilename = demographicsFilename;
        this.outPref = outPref;
        this.polygamous = polygamous;
        this.phasedInput = phasedInput;
        this.generations = generations;

        if (polygamous)
            MyLogger.important("Polygamous mode!");
        else
            MyLogger.important("Monogamous mode!");
        if (phasedInput)
            MyLogger.important("Phased input mode!");
        else
            MyLogger.important("Unphased input mode!");
    }

    private static Namespace parseArgs(String[] argv) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(PedigreeReconstructor.class.getSimpleName())
                .defaultHelp(true)
                .description("PREPARE - Pedigree Reconstruction from Extant Population using Partitioning of RElatives");

        parser.addArgument("ibdFile");
        parser.addArgument("demographicsFile");
        parser.addArgument("outDir");
        parser.addArgument("-polygamous")
                .setDefault("false")
                .action(Arguments.storeTrue())
                .help("use polygamous model");
        parser.addArgument("-phased")
                .setDefault("false")
                .action(Arguments.storeTrue())
                .help("phased input");
        parser.addArgument("-generations")
                .setDefault(4)
                .help("number of generations to reconstruct");
        return parser.parseArgsOrFail(argv);
    }

    public void reconstruct(){
        Pedigree ped;
        Graph IBDgraph;
        Population population;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographicsFilename);
            population = new Population(persons);
            IBDgraph = new Graph(persons);
            MyLogger.info("====================Adding IBD Features edges===============================");
            IBDFeaturesWeight.readEdgesWeights(IBDgraph, ibdFilename, population);// Adding edges to the graph
            MyLogger.info("Graph is " + IBDgraph);
            ped = new Pedigree(population);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int generation = 1;
        int startFromGen = 1;

        Pedigree fullPed = new Pedigree();
        //fullPed.readFromFile("D://workspace/pedigree/500_500/3g/pedigree.structure");
        //fullPed.readFromFile("D://workspace/pedigree/polygamous_160CeuYri_100_150/pedigree.structure");
        //fullPed.readFromFile("D://workspace/pedigree/200_200_5/pedigree.structure");


        if (startFromGen > 1) {
            ped.readFromFile(outPref + (startFromGen - 1));
            //ped.readFromFile("D://workspace/pedigree/WFSim/pol200_3g_0.8/pedigree.structure1");
            ped.calcExpectedFounderAges((startFromGen - 1));
            generation = (startFromGen);
        }

        boolean synchronous = true;

        for (int gen = generation; gen <= generation; gen++) {
            PedigreeBuilder pedBuilder = new PedigreeBuilder(IBDgraph, outPref + gen, polygamous, synchronous, phasedInput);
            pedBuilder.buildGeneration(ped, gen, fullPed, population);
        }
    }

    public static void main(String[] argv) {
        Namespace args = parseArgs(argv);
        String demographFilename = args.getString("demographicsFile");
        String ibdFile = args.getString("ibdFile");
        String out = args.getString("outDir");
        boolean polygamous = args.getBoolean("polygamous");
        boolean phasedInput = args.getBoolean("phased");
        int generations = args.get("generations");

        PedigreeReconstructor pedigreeReconstructor = new PedigreeReconstructor(
                ibdFile, demographFilename, out, polygamous, phasedInput, generations);
        pedigreeReconstructor.reconstruct();
    }
}
