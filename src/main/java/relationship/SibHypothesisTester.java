package relationship;

import graph.*;
import prepare.Contraction;
import simulator.Pedigree;
import simulator.Pedigree.PedVertex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SibHypothesisTester extends RelationHypothesisTester {

    int parentMistakes = 1;
    int numOfParents = 1;
    boolean synchronous;
    boolean polygamous;

    public SibHypothesisTester(Graph IBDGraph, boolean synchronous, boolean polygamous, boolean phased) {
        super(IBDGraph, phased);
        this.synchronous = synchronous;
        this.polygamous = polygamous;
    }

    /**
     * Test all contracted founder nodes of pedigree for having one of the following relationships:
     * {fullsib,halfsib,parentChild,childParent,other}
     *
     * @return - The contracted relationship graph
     */
    public void run(Pedigree ped, Graph contractedRelationGraph, List<Vertex> candidates, int gen, Pedigree fullPed) {

        lCalc = new PedLikelihoodCalcInheritancePaths(50, phased);
        //lCalc = new PedLikelihoodCalcBasic(50);

        for (Vertex sv1 : candidates) {
            SuperVertex s1 = (SuperVertex) sv1.getData();
//			MyLogger.important("Find potential siblings to " + s1);
            for (Vertex sv2 : candidates) {

                SuperVertex s2 = (SuperVertex) sv2.getData();

                PedVertex f1 = ped.getVertex(s1.getRepresentativeID());
                PedVertex f2 = ped.getVertex(s2.getRepresentativeID());


                //Already tested other direction
                if (f1.getId() >= f2.getId())
                    continue;

                idConversion = new HashMap<Integer, Integer>();
                Map<Integer, Integer> enumTable = new HashMap<Integer, Integer>();

                //MyLogger.important("Testing " + f1 + " " + f2);

                //Pedigree relevantPed = ped.extractSubPedigree(f1,f2,idConversion,enumTable);
                /**TODO to stop DEBUGGING comment the next two lines, and uncomment line above*/
                idConversion = new HashMap<Integer, Integer>();//override field
                Pedigree relevantPed = ped.extractSubPedigreeNoConversion(f1, f2, idConversion);
                enumTable = idConversion;

                if (f1.getChildren().contains(f2.getChildren()))
                    MyLogger.important("detecting if couple are sibs");

                int f1NewID = enumTable.get(f1.getId());
                int f2NewID = enumTable.get(f2.getId());

                List<PedVertex> descendants1 = relevantPed.getDescendants(f1NewID);
                List<PedVertex> descendants2 = relevantPed.getDescendants(f2NewID);

                double[] likelihoods = new double[9];
                for (int i = 0; i < likelihoods.length; i++)
                    likelihoods[i] = Double.NEGATIVE_INFINITY;

                RelationshipProbWeight w = new RelationshipProbWeight();
                /*
                 * Test relatedness hypothesis
                 */
                likelihoods[0] = calcUnrelatedLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                likelihoods[1] = calcSibLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);

//				printPairWiseIBD(s1, s2, ped);

                if (likelihoods[1] <= likelihoods[0]) { //if unrelated
                    //System.out.println(f1.getId() +" " + f2.getId() + " "  + likelihoods[0] + " " + likelihoods[1]);
					
					/*if(areParentChild(s1, s2, fullPed) || areParentChild(s2, s1, fullPed)){
						MyLogger.warn(parentMistakes++ +")Missed parent-child");
						//MyLogger.warn("IBD features=" + IBDgraph.getUndirectedEdge(s1.getId(), s2.getId()).getWeight());
						MyLogger.warn("unrelated="+likelihoods[0]+",sibs="+likelihoods[1]);
					}*/

                    continue;
                }
                //	if(polygamous)
                likelihoods[3] = calcHalfSibLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);

                int[] additionalResults = new int[2];
                int mateID = additionalResults[1];
                boolean f1Parent = additionalResults[0] == 1;

                if (!synchronous) {
                    likelihoods[2] = testPossibleParanthoodHypothesis(relevantPed, f1NewID, f2NewID, descendants1, descendants2, additionalResults);
                    double uncleL = calcAvuncularLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                    double nephewL = calcAvuncularLikelihood(relevantPed, f2NewID, f1NewID, descendants1, descendants2);
                    if (uncleL > nephewL)
                        likelihoods[4] = uncleL;
                    else
                        likelihoods[4] = nephewL;
                }
                likelihoods[5] = calcCousinLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                //likelihoods[8]=calcDoubleCousinLikelihood(relevantPed,f1NewID,f2NewID,descendants1,descendants2);
                //If no category matches, assume "unrelated"
                if (isMaxFromArray(-1000, likelihoods)) {
                    MyLogger.warn("No category matches " + f1 + "," + f2);
                    continue;
                }

                applyBayesUniPriors(likelihoods);

                //weighProbabilitiesWithAgeDiff(likelihoods,ped,f1.getId(),f2.getId());

                double unrelatedLikelihood = likelihoods[0];
                double sibLikelihood = likelihoods[1];
                double parentChildLikelihood = likelihoods[2];
                double halfSibLikelihood = likelihoods[3];
                double avuncularLikelihood = likelihoods[4];
                double cousLikelihood = likelihoods[5];
                double halfCousinLikelihood = likelihoods[6];
                double halfAvuncularLikelihood = likelihoods[7];
                //double doubleCousinLikelihood = likelihoods[8];
                if (isMaxFromArray(sibLikelihood, likelihoods)) {
                    numOfSibs++;
                    MyLogger.important("found potential sibs " + s1 + "," + s2);
                }
                if (isMaxFromArray(halfSibLikelihood, likelihoods)) ;
                numOfHalfSibs++;


                //System.out.println("LIK: " + f1.getId() + " " + f2.getId() + " " + likelihoods[0] + " " + likelihoods[1] + " " + likelihoods[2] + " " + likelihoods[3] + " " + likelihoods[4] + " " + likelihoods[5] + " " + likelihoods[6] + " " + likelihoods[7]);
                //	printSibDebugInfo(fullPed, numOfSibs,ped,s1, s2,likelihoods, sibLikelihood,false);//full-sibs
                //printSibDebugInfo(fullPed, numOfHalfSibs, s1, s2,likelihoods, halfSibLikelihood,true);//half-sibs

                if (mateID == -2 && ped.getDemographics().getAge(f1.getId()) > ped.getDemographics().getAge(f2.getId()))
                    f1Parent = true;
                //printParentInfo(parentChildLikelihood,likelihoods,s1, s2,fullPed,ped,f1Parent,mateID);

                //if(isMaxFromArray(parentChildLikelihood,likelihoods)){
                //	MyLogger.important("parents info: " + f1+","+f2 +"," + f1Parent + "," + mateID);
                //}

                //printCousinDebugInfo(fullPed, s1, s2, likelihoods,cousLikelihood);


                if (isMaxFromArray(avuncularLikelihood, likelihoods)) ;

                if (gen == 1) {
                    if (ped.getDemographics().getAge(f1.getId()) <= ped.getDemographics().getAge(f2.getId()))
                        w.setProb("parent", parentChildLikelihood);
                    else
                        w.setProb("child", parentChildLikelihood);
                } else {
                    if (f1Parent)
                        w.setProb("parent", parentChildLikelihood);
                    else
                        w.setProb("child", parentChildLikelihood);
                }

                w.setProb("fullSib", sibLikelihood);
                w.setProb("halfSib", halfSibLikelihood);
                w.setProb("fullUncle", avuncularLikelihood);
                w.setProb("halfUncle", halfAvuncularLikelihood);
                w.setProb("fullCousin", cousLikelihood);
                w.setProb("halfCousin", halfCousinLikelihood);
                w.setProb("notRelated", unrelatedLikelihood);

                Edge edge = new BaseEdge(contractedRelationGraph.getVertex(f1.getId()), contractedRelationGraph.getVertex(f2.getId()), w);
                Edge backEdge = new BaseEdge(contractedRelationGraph.getVertex(f2.getId()), contractedRelationGraph.getVertex(f1.getId()), RelationshipProbWeight.switchWeightsDirection(w));
                contractedRelationGraph.addEdge(edge);
                contractedRelationGraph.addEdge(backEdge);
            }
        }
        return;
    }


    public Graph debugWithRealRelationships(Pedigree ped, Contraction cont, Pedigree fullPed) {

        //Create Graph with no edges, with super-vertices for parents with equal descendents
        Graph contractedRelationGraph = cont.createEdgelessContractedGraph();

        for (SuperVertex s1 : cont.getSuperVertices()) {
            MyLogger.important("Find potential siblings to " + s1);
            for (SuperVertex s2 : cont.getSuperVertices()) {

                //Already tested other direction
                if (s1.getId() >= s2.getId())
                    continue;

                RelationshipProbWeight w = new RelationshipProbWeight();

                if (areSibs(s1, s2, fullPed)) {
                    w.setProb("fullSib", 1.0);
                    w.setProb("notRelated", 0.0);
                    MyLogger.important(s1 + " and " + s2 + " are sibs");

                }
                if (areParentChild(s1, s2, fullPed)) {
                    w.setProb("child", 1.0);
                    w.setProb("notRelated", 0.0);
                    MyLogger.important(s1 + " and " + s2 + " are child-parent");
                }

                Edge edge = new BaseEdge(contractedRelationGraph.getVertex(s1.getId()), contractedRelationGraph.getVertex(s2.getId()), w);
                Edge backEdge = new BaseEdge(contractedRelationGraph.getVertex(s2.getId()), contractedRelationGraph.getVertex(s1.getId()), RelationshipProbWeight.switchWeightsDirection(w));
                contractedRelationGraph.addEdge(edge);
                contractedRelationGraph.addEdge(backEdge);
            }
        }
        return contractedRelationGraph;
    }

    /**
     * Test hypothesis f1 parent of f2 (with all possible mates)
     * and f2 parent of f1 (with all possible mates)
     *
     * @param relevantPed
     * @param f1NewID
     * @param f2NewID
     * @param additionalResultsArr
     * @return
     */
    private double testPossibleParanthoodHypothesis(Pedigree relevantPed,
                                                    int f1NewID, int f2NewID, List<PedVertex> descendants1, List<PedVertex> descendants2, int[] additionalResultsArr) {
        int mateID = -2;
        int f1Parent = 0;
        double likelihood;
        double[] parentF2Res = calcParentLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
        double[] parentF1Res = calcParentLikelihood(relevantPed, f2NewID, f1NewID, descendants1, descendants2);
        if (parentF2Res[0] > parentF1Res[0]) {
            likelihood = parentF2Res[0];
            if (parentF2Res[1] != -2) {
                mateID = (int) parentF2Res[1];
                f1Parent = 1;
            }
        } else {
            likelihood = parentF1Res[0];
            if (parentF1Res[1] != -2)
                mateID = (int) parentF1Res[1];
        }
        additionalResultsArr[0] = f1Parent;
        additionalResultsArr[1] = mateID;
        return likelihood;
    }

	/*
	private void weighProbabilitiesWithAgeDiff(double[] likelihoods,Pedigree ped, int id1,int id2) {
		double sibLikelihood = likelihoods[1];
		double parentChildLikelihood = likelihoods[2];
		double halfSibLikelihood = likelihoods[3];
		double avuncularLikelihood = likelihoods[4];

		MyLogger.info("before: sibL="+sibLikelihood + " " + "parentL=" + parentChildLikelihood);
		//Weight by age difference
		double ageDiff = Math.abs(ped.getDemographics().getAge(id1)-ped.getDemographics().getAge(id2));
		double probSum = sibLikelihood+parentChildLikelihood+halfSibLikelihood+avuncularLikelihood;
		double sibAgeDiffProb= sibAgeDifDist.density(ageDiff);
		double parentAgeDiffProb= parentAgeDifDist.density(ageDiff);
		double avuncularAgeDiffProb= avuncularAgeDifDist.density(ageDiff);

		double combinedProb = sibAgeDiffProb*sibLikelihood+parentAgeDiffProb*parentChildLikelihood+sibAgeDiffProb*halfSibLikelihood+avuncularAgeDiffProb*avuncularLikelihood;
		//fix poserior probabilities, by ageDiff weight
		likelihoods[1]=((sibAgeDiffProb*sibLikelihood)/combinedProb)*probSum;
		likelihoods[2]=((parentAgeDiffProb*parentChildLikelihood)/combinedProb)*probSum;
		likelihoods[3]=((sibAgeDiffProb*halfSibLikelihood)/combinedProb)*probSum;
		likelihoods[4]=((avuncularAgeDiffProb*avuncularLikelihood)/combinedProb)*probSum;

		MyLogger.info("after: sibL="+likelihoods[1] + " " + "parentL=" + likelihoods[2]);		
	}
*/
	/*private void printParentInfo(double parentChildLikelihood,double[] likelihoods,SuperVertex s1, SuperVertex s2,
			Pedigree fullPed,Pedigree ped,boolean f1Parent,int mateID){

		if(isMaxFromArray(parentChildLikelihood,likelihoods)){
			numOfParents++;
			if(f1Parent)
				MyLogger.important((numOfParents) +") Found potential parent-child! " + s1 +","+s2 + " L="+parentChildLikelihood);
			else 
				MyLogger.important((numOfParents) +") Found potential child-parent! " + s1 +","+s2 + " L="+parentChildLikelihood);

			MyLogger.important("Mate=" + mateID);
			if(!(areParentChild(s1, s2, fullPed) || areParentChild(s2, s1, fullPed))){
				MyLogger.warn(parentMistakes +") potential parent-child are false!");
				MyLogger.warn("nr="+likelihoods[0]+",sb="+likelihoods[1]+",av="+likelihoods[3]+",cs="+likelihoods[4]);
				printPairWiseIBD(s1,s2,ped);
			}	 
		}
		else if(areParentChild(s1, s2, fullPed) || areParentChild(s2, s1, fullPed)){
			MyLogger.warn(parentMistakes +")Missed parent-child");
			MyLogger.warn("nr="+likelihoods[0]+",sb="+likelihoods[1]+",av="+likelihoods[3]+",cs="+likelihoods[4]);
			printPairWiseIBD(s1,s2,ped);		}
	}
	*/
}