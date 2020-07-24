package simulator;

import graph.Graph;
import graph.MyLogger;
import graph.VertexData;
import pedigree.Person;
import prepare.Demographics;
import prepare.IBDFeaturesWeight;
import relationship.PedScoreCalc;

import java.io.IOException;
import java.util.List;

public class CalcPedigreeIBDScore {

    public static void main(String[] args) {

        String inferredPedigreeFile = args[0];
        String demographicsFilename = args[1];
        String IBDFile = args[2];
        int debugThresh = Integer.parseInt(args[3]);

        Pedigree inferredPed;
        Graph IBDGraph;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographicsFilename);
            Demographics demographics = new Demographics(persons);
            IBDGraph = new Graph(persons);
            IBDFeaturesWeight.readEdgesWeights(IBDGraph, IBDFile, demographics);        // Adding edges to the graph
            inferredPed = new Pedigree(demographics, false);
        } catch (IOException e) {
            throw new RuntimeException("Error...", e);
        }
        inferredPed.readFromFile(inferredPedigreeFile);

        PedScoreCalc pedScoreCalc = new PedScoreCalc(5, debugThresh, true);

        MyLogger.important("Calc score of inferred pedigree");
        pedScoreCalc.calcLoss(inferredPed, IBDGraph);
        double inferredLoss = pedScoreCalc.getMeanLoss();
        MyLogger.important("Inferred pedigree loss  = " + (float) inferredLoss + " +- " + (float) pedScoreCalc.getStdLoss());
    }
}
