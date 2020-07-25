package evaluation;

import graph.MyLogger;
import graph.VertexData;
import pedigree.Person;
import prepare.Population;
import prepare.PedAccuracy;
import pedigree.Pedigree;

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
            Population population = new Population(persons);
            inferredPed = new Pedigree(population, true);
            realPed = new Pedigree(population, true);
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
