package prepare.evaluation;

import prepare.graph.Graph;
import prepare.graph.VertexData;
import prepare.misc.MyLogger;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import prepare.pedigree.Pedigree;
import prepare.pedigree.Person;
import prepare.pedreconstruction.IBDFeaturesWeight;
import prepare.common.Population;

import java.io.IOException;
import java.util.List;

public class PedigreeIBDLossCalculator {

    private final String inferredPedigreeFile;
    private final String demographicsFile;
    private final String ibdFile;
    private final int debugThresh;

    public PedigreeIBDLossCalculator(String inferredPedigreeFile, String demographicsFile, String ibdFile, int debugThresh) {
        this.inferredPedigreeFile = inferredPedigreeFile;
        this.demographicsFile = demographicsFile;
        this.ibdFile = ibdFile;
        this.debugThresh = debugThresh;
    }

    private static Namespace parseArgs(String[] argv) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(PedigreeIBDLossCalculator.class.getSimpleName())
                .defaultHelp(true)
                .description("Evaluate reconstructed pedigree fit to IBD input data");

        parser.addArgument("inferredPedigreeFile");
        parser.addArgument("demographicsFile");
        parser.addArgument("ibdFile");
        parser.addArgument("-debugThreshold")
                .setDefault(100)
                .help("debug threshold");
        return parser.parseArgsOrFail(argv);
    }

    public double evaluate() {
        Pedigree inferredPed;
        Graph ibdGraph;
        try {
            List<VertexData> persons = Person.listFromDemographics(demographicsFile);
            Population population = new Population(persons);
            ibdGraph = new Graph(persons);
            IBDFeaturesWeight.readEdgesWeights(ibdGraph, ibdFile, population);        // Adding edges to the prepare.graph
            inferredPed = new Pedigree(population);
            inferredPed.readFromFile(inferredPedigreeFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PedScoreCalc pedScoreCalc = new PedScoreCalc(5, debugThresh, true);
        MyLogger.important("Calc score of inferred pedigree");
        pedScoreCalc.calcLoss(inferredPed, ibdGraph);
        double inferredLoss = pedScoreCalc.getMeanLoss();
        MyLogger.important("Inferred pedigree loss  = " + (float) inferredLoss + " +- " + (float) pedScoreCalc.getStdLoss());
        return inferredLoss;
    }

    public static void main(String[] argv) {
        Namespace args = parseArgs(argv);

        String inferredPedigreeFile = args.getString("inferredPedigreeFile");
        String demographicsFile = args.getString("demographicsFile");
        String ibdFile = args.getString("ibdFile");
        int debugThresh = args.getInt("debugThreshold");

        PedigreeIBDLossCalculator pedigreeIBDLossCalculator = new PedigreeIBDLossCalculator(inferredPedigreeFile, demographicsFile, ibdFile, debugThresh);
        pedigreeIBDLossCalculator.evaluate();
    }
}
