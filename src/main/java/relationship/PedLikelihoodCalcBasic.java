package relationship;

import graph.Edge;
import graph.Graph;
import jsat.classifiers.DataPoint;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.linear.Vec;
import misc.VecImpl;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PedLikelihoodCalcBasic extends PedLikelihoodCalcAbs {

    public PedLikelihoodCalcBasic(int numOfSimsIter, boolean phased) {
        super(numOfSimsIter, phased);
    }

    public double calcLikelihood(Pedigree p, Graph IBDGraph) {
        Map<Integer, Integer> idConversion = new HashMap<Integer, Integer>();//override field
        //Create self conversion map
        idConversion.put(-1, -1);
        for (PedVertex v : p.getVertices()) {
            int id = v.getId();
            idConversion.put(id, id);
        }
        return calcLikelihood(p, IBDGraph, idConversion, null, null, false);
    }

    public double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        return calcLikelihood(p, IBDGraph, idConversion, descendants1, descendants2, true);
    }

    private double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, List<PedVertex> descendants1, List<PedVertex> descendants2, boolean useOnlyDirectDescendents) {
        double logLikelihood = 0;

        //MyLogger.important("Calc likelihood for pedigree : " + p);

        Map<String, List<DataPoint>> simDataSets = sampleFeaturesFromInheritanceSpace(p, true, descendants1, descendants2);
        Map<String, MultivariateKDE> kdeMap = new HashMap<String, MultivariateKDE>();


        //Estimate empirical simulated distribution with kernel density functions
        for (PedVertex v1 : descendants1) {
            if (!v1.isAlive()) continue;//compare only living descendents
            for (PedVertex v2 : descendants2) {
                if (!v2.isAlive()) continue;//compare only living descendents

                if (v1.getId() == v2.getId()) continue;//don't self compare
                if (kdeMap.containsKey(v2.getId() + "." + v1.getId())) continue;//Do only one side calculation

                String pairID = v1.getId() + "." + v2.getId();
                kdeMap.put(pairID, estimateDensity(simDataSets.get(pairID)));
            }
        }
        int pairNum = 0;

        /*
         * Calc likelihood of observed data for each pair of individuals
         * Assumes Independence between pairs, (not a valid assumption!!)
         */
        for (PedVertex v1 : descendants1) {
            if (!v1.isAlive()) continue;//compare only living descendents
            for (PedVertex v2 : descendants2) {
                if (!v2.isAlive()) continue;//compare only living descendents

                if (v1.getId() == v2.getId()) continue;//don't self compare
                if (kdeMap.containsKey(v2.getId() + "." + v1.getId())) continue;//Do only one side calculation

                String pairID = v1.getId() + "." + v2.getId();

                //Get observed IBD sharing features
                Vec obsFeatures = new VecImpl(0, 0);
                Edge e = IBDGraph.getUndirectedEdge(idConversion.get(v1.getId()), idConversion.get(v2.getId()));
                if (e != null)
                    obsFeatures = e.getWeight().asVector();


                double pairLogLikelihood = Math.log(kdeMap.get(pairID).pdf(obsFeatures));
                //MyLogger.important(v1 + "," + v2 + " logLikelihood=" + pairLogLikelihood);
                //MyLogger.important("obs=" + obsFeatures + "exp=" + kdeMap.get(pairID).getNearby(obsFeatures));

                if (Double.isInfinite(pairLogLikelihood)) {
                    printExplenationForInf(simDataSets.get(pairID));
                    pairLogLikelihood = -1000; //minimal pair score
                }
                pairNum++;
                logLikelihood += pairLogLikelihood;
            }
        }
        return logLikelihood / pairNum;
    }
}
