package prepare.pedreconstruction;

import prepare.common.Population;
import prepare.graph.Graph;
import prepare.misc.MyLogger;
import prepare.graph.VertexData;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import prepare.pedigree.Person;
import prepare.pedigree.Pedigree;

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

    public Pedigree reconstruct(){
        Pedigree ped;
        Graph IBDgraph;
        Population population;
        try {
            List<VertexData> persons = Person.listFromDemographics(demographicsFilename);
            population = new Population(persons);
            IBDgraph = new Graph(persons);
            MyLogger.info("====================Adding IBD Features edges===============================");
            IBDFeaturesWeight.readEdgesWeights(IBDgraph, ibdFilename, population);// Adding edges to the prepare.graph
            MyLogger.info("Graph is " + IBDgraph);
            ped = new Pedigree(population);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int generation = 1;
        Pedigree fullPed = new Pedigree();

        boolean synchronous = true;

        for (int gen = generation; gen <= generations; gen++) {
            PedigreeBuilder pedBuilder = new PedigreeBuilder(IBDgraph, outPref + gen, polygamous, synchronous, phasedInput);
            pedBuilder.buildGeneration(ped, gen, population);
        }
        return ped;
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
