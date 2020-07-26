package prepare.evaluation;

import prepare.common.Population;
import prepare.graph.VertexData;
import prepare.misc.MyLogger;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import prepare.pedigree.Pedigree;
import prepare.pedigree.Person;

import java.io.IOException;
import java.util.List;

public class PedigreeMinDistanceScorer {
    Pedigree realPed;
    Pedigree inferredPed;

    public PedigreeMinDistanceScorer(String inferredPedigreeFile, String realPedigreeFile, String demographicsFile) {
        try {
            List<VertexData> persons = Person.listFromDemographics(demographicsFile);
            Population population = new Population(persons);
            inferredPed = new Pedigree(population);
            realPed = new Pedigree(population);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        inferredPed.readFromFile(inferredPedigreeFile);
        realPed.readFromFile(realPedigreeFile);
    }

    public PedAccuracy run() {
        PedAccuracy accCalc = new PedAccuracy();
        MyLogger.important("Calc score of inferred pedigree");
        accCalc.calcAccuracy(inferredPed, realPed);
        return accCalc;
    }

    private static Namespace parseArgs(String[] argv) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(PedigreeMinDistanceScorer.class.getSimpleName())
                .defaultHelp(true)
                .description("Calculate distance between two pedigrees (usually inferred, actual)");
        parser.addArgument("inferredPedigreeFile");
        parser.addArgument("realPedigreeFile");
        parser.addArgument("demographicsFile");
        return parser.parseArgsOrFail(argv);
    }

    public static void main(String[] argv) {
        Namespace args = parseArgs(argv);
        String inferredPedigreeFile = args.getString("inferredPedigreeFile");
        String demographicsFile = args.getString("demographicsFile");
        String realPedigreeFile = args.getString("realPedigreeFile");
        PedigreeMinDistanceScorer pedigreeMinDistanceScorer = new PedigreeMinDistanceScorer(inferredPedigreeFile, realPedigreeFile, demographicsFile);
        pedigreeMinDistanceScorer.run();
    }
}

