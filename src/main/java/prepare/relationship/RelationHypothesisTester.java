package prepare.relationship;

import prepare.graph.*;
import prepare.misc.MyLogger;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import prepare.pedigree.Pedigree;
import prepare.pedigree.Pedigree.PedVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class RelationHypothesisTester {
    Graph IBDGraph;
    PedLikelihoodCalcAbs lCalc;
    Map<Integer, Integer> idConversion;
    ExponentialDistribution sibAgeDifDist = new ExponentialDistribution(7);
    NormalDistribution parentAgeDifDist = new NormalDistribution(25, 10);
    int numOfSibs = 0;
    int numOfHalfSibs = 0;
    boolean phased;

    public RelationHypothesisTester(Graph IBDGraph, boolean phased) {
        this.IBDGraph = IBDGraph;
        this.phased = phased;
    }

    double calcSameLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2, boolean isFather) {
        Pedigree p = new Pedigree(ped);//clone
        for (PedVertex f2Child : p.getVertex(f2).getChildren()) {
            p.getVertex(f1).addChild(f2Child);
            if (isFather)
                f2Child.overrideFather(f1);
            else
                f2Child.overrideMother(f1);
        }
        p.removeVertex(f2);

        MyLogger.info("calcSameLikelihood::pedSize=" + p.getVertices().size());

        double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " sameLikelihood=" + l);

        return l;
    }

    double calcUnrelatedLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Pedigree p = new Pedigree(ped);//clone
        MyLogger.info("calcUnrelatedLikelihood::pedSize=" + p.getVertices().size());


        double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " unrelatedLikelihood=" + l);

        return l;
    }

    double calcSibLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Pedigree pedigree = new Pedigree(ped);//clone

        int fatherID;
        int motherID;

        fatherID = pedigree.getNewID();
        motherID = pedigree.getNewID();

        //Add nodes that create sibling hypothesis
        pedigree.addVertex(fatherID);
        pedigree.addVertex(motherID);

        pedigree.getVertex(f1).setFather(fatherID);
        pedigree.getVertex(f1).setMother(motherID);
        pedigree.getVertex(f2).setFather(fatherID);
        pedigree.getVertex(f2).setMother(motherID);

        MyLogger.info("calcSibLikelihood::pedSize=" + pedigree.getVertices().size());

        double l = lCalc.calcLikelihood(pedigree, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " sibLikelihood=" + l);

        return l;
    }

    /**
     * Calc the likelihood for f2 to be the parent of f1
     */
    double[] calcParentLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        MyLogger.info("calcParentLikelihood");

        //Add nodes that create parent hypothesis
        int newMateID = -2;

        List<Integer> possibleMateIDs = new ArrayList<>();
        possibleMateIDs.add(newMateID);

        //Find all possible mates
        for (PedVertex child : ped.getVertex(f2).getChildren()) {
            int possibleMate;
            if (child.getFatherId() == f2)
                possibleMate = child.getMotherId();
            else
                possibleMate = child.getFatherId();
            if (!possibleMateIDs.contains(possibleMate))
                possibleMateIDs.add(possibleMate);
        }
        double bestLikelihood = Double.NEGATIVE_INFINITY;
        int bestMate = -1;

        for (int possibleMate : possibleMateIDs) {
            MyLogger.info("Possible mate = " + possibleMate);
            Pedigree p = new Pedigree(ped);//clone pedigree
            if (possibleMate == newMateID)
                p.addVertex(possibleMate);

            if (ped.getVertex(f2).getChildren().isEmpty()
                    || ped.getVertex(f2).getChildren().get(0).getFatherId() == f2) {
                p.getVertex(f1).setFather(f2);
                p.getVertex(f1).setMother(possibleMate);
            } else {
                p.getVertex(f1).setMother(f2);
                p.getVertex(f1).setFather(possibleMate);
            }
            if (p.getVertex(possibleMate).getFatherId() == f1 || p.getVertex(possibleMate).getMotherId() == f1) {
                MyLogger.important(possibleMate + "Can't be the child of its child: " + f1 + ", skip hypothesis");
                continue;
            }
            MyLogger.info("calcParentLikelihood::pedSize=" + p.getVertices().size());
            double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
            MyLogger.info(f1 + " child of " + f2 + "," + possibleMate + "=" + l);
            if (l > bestLikelihood) {
                bestLikelihood = l;
                bestMate = possibleMate;
            }
        }
        return new double[]{bestLikelihood, bestMate};
    }

    double calcHalfSibLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Pedigree p = new Pedigree(ped);//clone

        int fatherID;
        int f1MotherID;
        int f2MotherID;

        //If one person is not founder set hypothesized parents as his parents
        fatherID = p.getNewID();
        f1MotherID = p.getNewID();
        f2MotherID = p.getNewID();

        //Add nodes that create half-sibling hypothesis
        p.addVertex(fatherID);
        p.addVertex(f1MotherID);
        p.addVertex(f2MotherID);

        p.getVertex(f1).setFather(fatherID);
        p.getVertex(f1).setMother(f1MotherID);
        p.getVertex(f2).setFather(fatherID);
        p.getVertex(f2).setMother(f2MotherID);

        MyLogger.info("calcHalfSibLikelihood::pedSize=" + p.getVertices().size());
        double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " halfSibLikelihood=" + l);

        return l;
    }

    double calcCousinLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Pedigree p = new Pedigree(ped);//clone

        int f1FatherID = p.getNewID();
        int f1MotherID = p.getNewID();
        int f2FatherID = p.getNewID();
        int f2MotherID = p.getNewID();
        int grandFatherID = p.getNewID();
        int grandMotherID = p.getNewID();

        //Add nodes that create cousin hypothesis
        p.addVertex(grandFatherID);
        p.addVertex(grandMotherID);
        p.addVertex(f1FatherID, grandFatherID, grandMotherID, false);
        p.addVertex(f2FatherID, grandFatherID, grandMotherID, false);
        p.addVertex(f1MotherID);
        p.addVertex(f2MotherID);

        p.getVertex(f1).setFather(f1FatherID);
        p.getVertex(f1).setMother(f1MotherID);
        p.getVertex(f2).setFather(f2FatherID);
        p.getVertex(f2).setMother(f2MotherID);

        MyLogger.info("calcCousinLikelihood::pedSize=" + p.getVertices().size());

        double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " cousinLikelihood=" + l);

        return l;
    }

    double calcAvuncularLikelihood(Pedigree ped, int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Pedigree p = new Pedigree(ped);//clone

        int f1FatherID = p.getNewID();
        int f1MotherID = p.getNewID();

        int f2FatherID = p.getNewID();
        int f2MotherID = p.getNewID();

        p.addVertex(f1MotherID);
        p.addVertex(f1FatherID);
        p.addVertex(f2FatherID);
        p.addVertex(f2MotherID);

        p.getVertex(f1).setFather(f1FatherID);
        p.getVertex(f1).setMother(f1MotherID);
        p.getVertex(f2).setFather(f2FatherID);
        p.getVertex(f2).setMother(f2MotherID);

        //set f2 as the uncle of f1
        p.getVertex(f2FatherID).setFather(f1FatherID);
        p.getVertex(f2FatherID).setMother(f1MotherID);

        MyLogger.info("calcAvuncularLikelihood::pedSize=" + p.getVertices().size());

        double l = lCalc.calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2);
        MyLogger.info(f1 + "," + f2 + " avuncularLikelihood=" + l);

        return l;
    }

    public static boolean isMaxFromArray(double candidate, double[] arr) {
        for (double elm : arr) {
            if (elm > candidate)
                return false;
        }
        return true;
    }

    void applyBayesUniPriors(double[] likelihoods) {
        double Lsum = 0;
        int i = 0;
        for (double l : likelihoods) {
            double L = Math.exp(l);
            likelihoods[i] = L;
            Lsum += L;
            i++;
        }
        for (i = 0; i < likelihoods.length; i++) {
            likelihoods[i] /= Lsum;
        }
    }


    /**
     * Debugging Methods
     */
    boolean areSibs(SuperVertex s1, SuperVertex s2, Pedigree p) {
        for (Vertex v1 : s1.getInnerVertices()) {
            for (Vertex v2 : s2.getInnerVertices()) {
                if (areSibs(v1.getVertexId(), v2.getVertexId(), p))
                    return true;
            }
        }
        return false;
    }

    boolean areSibs(int id1, int id2, Pedigree p) {
        if (id1 != id2 && p.getVertex(id1).getFatherId() == p.getVertex(id2).getFatherId() &&
                p.getVertex(id1).getMotherId() == p.getVertex(id2).getMotherId()) {
            MyLogger.info("Found sibs in pedigree " + id1 + " " + id2);
            return true;
        }
        return false;
    }

    boolean areParentChild(SuperVertex s1, SuperVertex s2, Pedigree p) {
        for (Vertex v1 : s1.getInnerVertices()) {
            for (Vertex v2 : s2.getInnerVertices()) {
                if (p.getVertex(v2.getVertexId()).getFatherId() == v1.getVertexId() ||
                        p.getVertex(v2.getVertexId()).getMotherId() == v1.getVertexId())
                    return true;
            }
        }
        return false;
    }
}

