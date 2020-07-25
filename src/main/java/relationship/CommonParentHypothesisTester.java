package relationship;

import graph.*;
import misc.MyLogger;
import pedigree.NucFamily;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;
import pedigree.Person;
import pedreconstruction.Contraction;

import java.util.*;

import static relationship.Relationship.*;

/**
 * Test if two sib cliques share a common parent or not
 */
public class CommonParentHypothesisTester extends RelationHypothesisTester {
    boolean synchronous;

    public CommonParentHypothesisTester(Graph IBDGraph, boolean synchronous, boolean phased) {
        super(IBDGraph, phased);
        this.synchronous = synchronous;
    }

    public Graph run(Pedigree p, Contraction sibContraction, List<NucFamily> nucFamilies) {
        List<VertexData> datas = new ArrayList<>(sibContraction.getSuperVertices());
        Graph halfSibGraph = new Graph(datas);

        //clone pedigree
        Pedigree ped = new Pedigree(p);

        lCalc = new PedLikelihoodCalcInheritancePaths(50, phased);

        for (NucFamily fam1 : nucFamilies) {
            //	MyLogger.important("Testing\n" + fam1);
            if (fam1.getFather().isAlive() && fam1.getMother().isAlive())
                continue;
            for (NucFamily fam2 : nucFamilies) {

                //Already tested other direction, or same family
                if (fam1.getFather().getId() >= fam2.getFather().getId())
                    continue;


                int candParent1;
                int candParent2;
                boolean isFather = true;
                if (!fam1.getFather().isAlive() && !fam2.getFather().isAlive()) {
                    candParent1 = fam1.getFather().getId();
                    candParent2 = fam2.getFather().getId();
                }
                else if (!fam1.getMother().isAlive() && !fam2.getMother().isAlive()) {
                    candParent1 = fam1.getMother().getId();
                    candParent2 = fam2.getMother().getId();
                    isFather = false;
                }
                else
                    continue;

                //If detected living parent, don't compare to its family (will cause a loop)
                //Assumes no incest
                if (fam2.siblings.contains(fam1.getFather()) || fam2.siblings.contains(fam1.getMother())
                        || fam1.siblings.contains(fam2.getFather()) || fam1.siblings.contains(fam2.getMother())
                        || fam1.getMother() == fam2.getMother() || fam1.getFather() == fam2.getFather()) {
                    //MyLogger.important("skip testing common parent for " + fam1.getFather() +","+fam2.getFather());
                    continue;
                }

                //If both nuclear families are contained in one super-vertex (single husband-single wife)
                if (fam1.siblings.size() == 1 && fam2.siblings.size() == 1) {
                    int sib1ID = sibContraction.getWrappingSuperVertex(fam1.siblings.get(0).getId()).getRepresentativeID();
                    int sib2ID = sibContraction.getWrappingSuperVertex(fam2.siblings.get(0).getId()).getRepresentativeID();
                    if (sib1ID == sib2ID)
                        continue;
                }

                //Add parents if necessary, need to remove later
                List<Integer> added1 = addNuclearFamilyToPedigree(fam1, ped);
                List<Integer> added2 = addNuclearFamilyToPedigree(fam2, ped);

                PedVertex f1 = ped.getVertex(candParent1);
                PedVertex f2 = ped.getVertex(candParent2);


                Map<Integer, Integer> enumTable;
                idConversion = new HashMap<>();
                Pedigree relevantPed = ped.extractSubPedigreeNoConversion(f1, f2, idConversion);
                enumTable = idConversion;

                idConversion = enumTable;

                //Remove extra vertices from pedigree
                added1.addAll(added2);
                for (int addedID : added1) {
                    ped.removeVertex(addedID);
                }

                int f1NewID = enumTable.get(f1.getId());
                int f2NewID = enumTable.get(f2.getId());

                List<PedVertex> descendants1 = relevantPed.getDescendants(f1NewID);
                List<PedVertex> descendants2 = relevantPed.getDescendants(f2NewID);

                //In this case no pairs to test the hypothesis on, assume unrealted
                if (descendants1.containsAll(descendants2) || descendants2.containsAll(descendants1))
                    continue;

                double[] likelihoods = new double[4];
                Arrays.fill(likelihoods, Double.NEGATIVE_INFINITY);

                /*
                 * Test relatedness hypothesis
                 */
                likelihoods[0] = calcUnrelatedLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);
                likelihoods[1] = calcSameLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2, isFather);

                if (likelihoods[1] <= likelihoods[0]) //if unrelated
                    continue;

                //MyLogger.important("CP families are related:\n" + fam1 + "\n" + fam2);

                likelihoods[2] = calcSibLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2);

                if (synchronous)
                    likelihoods[3] = calcParentLikelihood(relevantPed, f1NewID, f2NewID, descendants1, descendants2)[0];

                applyBayesUniPriors(likelihoods);

                //If no category matches, assume "unrelated"
                if (isMaxFromArray(0, likelihoods)) {
                    likelihoods[0] = 1;
                    likelihoods[1] = 0;
                    likelihoods[2] = 0;
                    likelihoods[3] = 0;
                    MyLogger.warn("No category matches " + fam1 + "," + fam2);
                }

                int ageSum = 0;
                for (Person child : fam1.siblings)
                    ageSum += child.getAge();
                int age1 = ageSum / fam1.siblings.size();

                ageSum = 0;
                for (Person child : fam2.siblings)
                    ageSum += child.getAge();
                int age2 = ageSum / fam2.siblings.size();

                if (!synchronous)
                    weighProbabilitiesWithAgeDiff(likelihoods, Math.abs(age1 - age2));

                if (isMaxFromArray(likelihoods[1], likelihoods))
                    MyLogger.important("Found potential common-parent " + fam1.siblings + "," + fam2.siblings);

                Vertex v1 = halfSibGraph.getVertex(fam1.siblings.get(0).getId());
                Vertex v2 = halfSibGraph.getVertex(fam2.siblings.get(0).getId());

                if (isMaxFromArray(likelihoods[1], likelihoods)) {
                    numOfHalfSibs++;
                }

                RelationshipProbWeight w = new RelationshipProbWeight();
                w.setProb(NOT_RELATED, likelihoods[0]);
                w.setProb(HALF_SIB, likelihoods[1]);
                w.setProb(FULL_COUSIN, likelihoods[2]);
                Edge edge = new BaseEdge(v1, v2, w);
                Edge backEdge = new BaseEdge(v2, v1, RelationshipProbWeight.switchWeightsDirection(w));
                halfSibGraph.addEdge(edge);
                halfSibGraph.addEdge(backEdge);
            }
        }
        return halfSibGraph;
    }


    private List<Integer> addNuclearFamilyToPedigree(NucFamily fam, Pedigree ped) {
        int fatherID = fam.getFather().getId();
        int motherID = fam.getMother().getId();
        List<Integer> added = new ArrayList<>();
        if (!ped.hasVertex(fatherID)) {
            ped.addVertex(fatherID);
            added.add(fatherID);
        }
        if (!ped.hasVertex(motherID)) {
            ped.addVertex(motherID);
            added.add(motherID);
        }

        for (Person p : fam.siblings) {
            ped.getVertex(p.getId()).setFather(fatherID);
            ped.getVertex(p.getId()).setMother(motherID);
        }
        return added;
    }

    private void weighProbabilitiesWithAgeDiff(double[] likelihoods, int ageDiff) {
        double samePersonLikelihood = likelihoods[1];
        double sibLikelihood = likelihoods[2];
        double parentChildLikelihood = likelihoods[3];

        //Weight by age difference
        double probSum = sibLikelihood + samePersonLikelihood + parentChildLikelihood;
        if (probSum == 0)
            return;

        double sibAgeDiffProb = sibAgeDifDist.density(ageDiff);
        double parentAgeDiffProb = parentAgeDifDist.density(ageDiff);

        double combinedProb = sibAgeDiffProb * sibLikelihood + parentAgeDiffProb * parentChildLikelihood + sibAgeDiffProb * samePersonLikelihood;
        //fix posterior probabilities, by ageDiff weight
        likelihoods[1] = ((sibAgeDiffProb * samePersonLikelihood) / combinedProb) * probSum;
        likelihoods[2] = ((sibAgeDiffProb * sibLikelihood) / combinedProb) * probSum;
        likelihoods[3] = ((parentAgeDiffProb * parentChildLikelihood) / combinedProb) * probSum;
    }
}