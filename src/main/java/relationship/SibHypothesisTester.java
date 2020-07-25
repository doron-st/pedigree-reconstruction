package relationship;

import graph.*;
import misc.MyLogger;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static relationship.Relationship.*;


public class SibHypothesisTester extends RelationHypothesisTester {
    private final boolean synchronous;

    public SibHypothesisTester(Graph IBDGraph, boolean synchronous, boolean phased) {
        super(IBDGraph, phased);
        this.synchronous = synchronous;
    }

    /**
     * Test all contracted founder nodes of pedigree for having one of the following relationships:
     * {fullsib,halfsib,parentChild,childParent,other}
     * Add edge between nodes with fullsib as the maximum likelihood relationship
     */
    public void run(Pedigree ped, Graph contractedRelationGraph, List<Vertex> candidates, int gen) {

        lCalc = new PedLikelihoodCalcInheritancePaths(50, phased);

        for (Vertex sv1 : candidates) {
            SuperVertex s1 = (SuperVertex) sv1.getData();
            for (Vertex sv2 : candidates) {

                SuperVertex s2 = (SuperVertex) sv2.getData();

                PedVertex f1 = ped.getVertex(s1.getRepresentativeID());
                PedVertex f2 = ped.getVertex(s2.getRepresentativeID());


                //Already tested other direction
                if (f1.getId() >= f2.getId())
                    continue;

                idConversion = new HashMap<>();
                Map<Integer, Integer> enumTable;

                //MyLogger.important("Testing " + f1 + " " + f2);

                //Pedigree relevantPed = ped.extractSubPedigree(f1,f2,idConversion,enumTable);
                idConversion = new HashMap<>();//override field
                Pedigree relevantPed = ped.extractSubPedigreeNoConversion(f1, f2, idConversion);
                enumTable = idConversion;

                if (f1.getChildren().contains(f2.getChildren()))
                    MyLogger.important("detecting if couple are sibs");

                int f1NewID = enumTable.get(f1.getId());
                int f2NewID = enumTable.get(f2.getId());

                List<PedVertex> descendants1 = relevantPed.getDescendants(f1NewID);
                List<PedVertex> descendants2 = relevantPed.getDescendants(f2NewID);

                double[] likelihoods = new double[9];
                Arrays.fill(likelihoods, Double.NEGATIVE_INFINITY);

                RelationshipProbWeight w = new RelationshipProbWeight();
                /*
                 * Test relatedness hypothesis
                 */
                likelihoods[0] = calcUnrelatedLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                likelihoods[1] = calcSibLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);

//				printPairWiseIBD(s1, s2, ped);

                if (likelihoods[1] <= likelihoods[0]) { //if unrelated
                    continue;
                }
                //	if(polygamous)
                likelihoods[3] = calcHalfSibLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);

                int[] additionalResults = new int[2];

                if (!synchronous) {
                    likelihoods[2] = testPossibleParanthoodHypothesis(relevantPed, f1NewID, f2NewID, descendants1, descendants2, additionalResults);
                    double uncleL = calcAvuncularLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                    double nephewL = calcAvuncularLikelihood(relevantPed, f2NewID, f1NewID, descendants1, descendants2);
                    likelihoods[4] = Math.max(uncleL, nephewL);
                }
                likelihoods[5] = calcCousinLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                //If no category matches, assume "unrelated"
                if (isMaxFromArray(-1000, likelihoods)) {
                    MyLogger.warn("No category matches " + f1 + "," + f2);
                    continue;
                }

                applyBayesUniPriors(likelihoods);

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
                numOfHalfSibs++;

                if (gen == 1) {
                    if (ped.getPopulation().getAge(f1.getId()) <= ped.getPopulation().getAge(f2.getId()))
                        w.setProb(PARENT, parentChildLikelihood);
                    else
                        w.setProb(CHILD, parentChildLikelihood);
                } else {
                    w.setProb(CHILD, parentChildLikelihood);
                }

                w.setProb(FULL_SIB, sibLikelihood);
                w.setProb(HALF_SIB, halfSibLikelihood);
                w.setProb(FULL_UNCLE, avuncularLikelihood);
                w.setProb(HALF_UNCLE, halfAvuncularLikelihood);
                w.setProb(FULL_COUSIN, cousLikelihood);
                w.setProb(HALF_COUSIN, halfCousinLikelihood);
                w.setProb(NOT_RELATED, unrelatedLikelihood);

                Edge edge = new BaseEdge(contractedRelationGraph.getVertex(f1.getId()), contractedRelationGraph.getVertex(f2.getId()), w);
                Edge backEdge = new BaseEdge(contractedRelationGraph.getVertex(f2.getId()), contractedRelationGraph.getVertex(f1.getId()), RelationshipProbWeight.switchWeightsDirection(w));
                contractedRelationGraph.addEdge(edge);
                contractedRelationGraph.addEdge(backEdge);
            }
        }
    }


    /**
     * Test hypothesis f1 parent of f2 (with all possible mates)
     * and f2 parent of f1 (with all possible mates)
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
}