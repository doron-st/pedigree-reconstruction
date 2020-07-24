package relationship;

import graph.Edge;
import graph.Graph;
import graph.MyLogger;
import jsat.classifiers.DataPoint;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.linear.Vec;
import misc.VecImpl;
import simulator.Pedigree;
import simulator.Pedigree.PedVertex;

import javax.management.RuntimeErrorException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a unique set of pedigrees that explain all seen pedigrees for simulation
 * For each template, has a saved mapping of the
 * simulated feature distributions between each pair of living individuals.
 * These ditributions can be reused, to save time
 *
 * @author Doron Shem-Tov
 */
public class PedLikelihoodCalcInheritancePaths extends PedLikelihoodCalcAbs {

    private final Map<String, MultivariateKDE> kdeMap = new HashMap<String, MultivariateKDE>();

    public PedLikelihoodCalcInheritancePaths(int numOfSimsIter, boolean phased) {
        super(numOfSimsIter, phased);
    }

    @Override
    public double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        double logLikelihood = 0;
        boolean isFirstMissing = true;
        Map<String, List<DataPoint>> simDataSets = null;
        int pairNum = 0;
        //Sample features from inheritance space for all pairs, if needed.
        //(IBDFeature distributions that were calculated previouly for synonymous pedigree)
        for (PedVertex v1 : descendants1) {
            if (!v1.isAlive()) continue;//compare only living descendents
            for (PedVertex v2 : descendants2) {
                if (!v2.isAlive()) continue;//compare only living descendents
                if (v1.getId() == v2.getId()) continue;//don't self compare

                List<int[]> commonAncestorsDepth = p.getCommonAncestorDepths(p.getVertex(v1.getId()), p.getVertex(v2.getId()));
                String pairID = v1.getId() + "." + v2.getId();

                String key = "";
                for (int[] depths : commonAncestorsDepth)
                    key += "[" + depths[0] + "," + depths[1] + "]";

                if (!kdeMap.containsKey(key) && isFirstMissing) {
                    simDataSets = sampleFeaturesFromInheritanceSpace(p, true, descendants1, descendants2);
                    isFirstMissing = false;
                    List<DataPoint> pairDataSet = simDataSets.get(pairID);
                    if (pairDataSet == null) {
                        MyLogger.error("PedLikelihoodCalcIP::calcLikelihood::Null dataSet " + pairID + "," + key);
                        MyLogger.error(simDataSets.keySet().toString());
                        throw new RuntimeErrorException(new Error("Null dataSet"));
                    }
                    //MyLogger.important(pairDataSet.get(0).getNumericalValues().toString());
                    kdeMap.put(key, estimateDensity(pairDataSet));

                }
                //We already sampled features from the Inheritance space
                if (!kdeMap.containsKey(key) && !isFirstMissing) {
                    List<DataPoint> pairDataSet = simDataSets.get(pairID);
                    if (pairDataSet == null) {
                        MyLogger.error("PedLikelihoodCalcIP::calcLikelihood::Null dataSet " + pairID + "," + key);
                        throw new RuntimeErrorException(new Error("Null dataSet"));
                    }
                    kdeMap.put(key, estimateDensity(pairDataSet));
                }

                //Should have key in map by now
                MultivariateKDE kde = kdeMap.get(key);

                Vec obsFeatures = new VecImpl(0, 0);
                //Vec obsFeatures = new VecImpl(0);
                Edge e = IBDGraph.getUndirectedEdge(idConversion.get(v1.getId()), idConversion.get(v2.getId()));
                if (e != null)
                    obsFeatures = e.getWeight().asVector();

                double pairLogLikelihood = Math.log(kde.pdf(obsFeatures));
                //MyLogger.important(pairID + ":obs=" + obsFeatures + "exp=" + kdeMap.get(pairID).getNearby(obsFeatures));
                //MyLogger.important(v1 + "," + v2 + " logLikelihood=" + pairLogLikelihood);

                if (Double.isInfinite(pairLogLikelihood)) {
                    //printExplenationForInf(kde);
                    pairLogLikelihood = -100;
                }
                pairNum++;
                logLikelihood += pairLogLikelihood;
            }

        }
        return logLikelihood / pairNum;
    }
}
