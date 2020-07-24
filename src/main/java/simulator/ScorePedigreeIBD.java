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

public class ScorePedigreeIBD {

	public static void main(String[] args){

		String inferredPedigreeFile = args[0];
		String demographFilename = args[1];
		String IBDFile = args[2];
		int debugThresh = 80000;//Integer.valueOf(args[3]);

		Pedigree ped;
		Graph IBDGraph;
		try {
			List<VertexData> persons = Person.listFromDemograph(demographFilename);
			Demographics demographics = new Demographics(persons);
			IBDGraph = new Graph(persons);
			IBDFeaturesWeight.readEdgesWeights(IBDGraph, IBDFile,demographics);		// Adding edges to the graph
			ped = new Pedigree(demographics,false);
		} catch (IOException e) {
			throw new RuntimeException("Error...",e);
		}
		ped.readFromFile(inferredPedigreeFile);
		PedScoreCalc scoreCalc = new PedScoreCalc(5,debugThresh,true);
		
		MyLogger.important("Calc score of inferred pedigree");
		scoreCalc.calcLoss(ped, IBDGraph);
		
		double inferredLoss = scoreCalc.getMeanLoss();
		MyLogger.important("Inferred pedigree loss  = " + (float)inferredLoss+" +- " + (float)scoreCalc.getStdLoss());
	}
}

