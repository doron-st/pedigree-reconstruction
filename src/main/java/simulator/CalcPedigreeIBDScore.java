package simulator;

import java.io.IOException;
import java.util.List;

import misc.Person;

import prepare.Demographics;
import prepare.IBDFeaturesWeight;

import relationship.PedScoreCalc;
import graph.Graph;
import graph.MyLogger;
import graph.VertexData;

public class CalcPedigreeIBDScore {

	public static void main(String[] args){

		String inferredPedigreeFile = args[0];
		String demographFilename = args[1];
		String IBDFile = args[2];
		int debugThresh = Integer.valueOf(args[3]);

		Pedigree inferredPed;
		Graph IBDGraph;
		try {
			List<VertexData> persons = Person.listFromDemograph(demographFilename);
			Demographics demographics = new Demographics(persons);
			IBDGraph = new Graph(persons);
			IBDFeaturesWeight.readEdgesWeights(IBDGraph, IBDFile,demographics);		// Adding edges to the graph
			inferredPed = new Pedigree(demographics,false);
		} catch (IOException e) {
			throw new RuntimeException("Error...",e);
		}
		inferredPed.readFromFile(inferredPedigreeFile);

		PedScoreCalc lcalc = new PedScoreCalc(5,debugThresh,true);

		MyLogger.important("Calc score of inferred pedigree");
		lcalc.calcLoss(inferredPed,IBDGraph);
		double inferredLoss = lcalc.getMeanLoss();
		MyLogger.important("Inferred pedigree loss  = " + (float)inferredLoss+" +- " + (float)lcalc.getStdLoss());
	}
}
