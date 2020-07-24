package simulator;

import graph.MyLogger;
import graph.VertexData;
import pedigree.Person;
import prepare.Demographics;
import prepare.PedAccuracy;

import java.io.IOException;
import java.util.List;

public class ScorePedigreeMinDistance {

    public static void main(String[] args) {

        String inferredPedigreeFile = args[0];
        String demographFilename = args[1];
        String realPedigreeFile = args[2];

        Pedigree realPed;
        Pedigree inferredPed;
        try {
            List<VertexData> persons = Person.listFromDemograph(demographFilename);
            Demographics demographics = new Demographics(persons);
            inferredPed = new Pedigree(demographics, true);
            realPed = new Pedigree(demographics, true);
        } catch (IOException e) {
            throw new RuntimeException("Error...", e);
        }
        inferredPed.readFromFile(inferredPedigreeFile);
        realPed.readFromFile(realPedigreeFile);
        PedAccuracy accCalc = new PedAccuracy();

        MyLogger.important("Calc score of inferred pedigree");
        accCalc.calcAccuracy(inferredPed, realPed);
    }
}

