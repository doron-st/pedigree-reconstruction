package relationship;

import graph.Edge;
import graph.Graph;
import misc.MyLogger;
import jsat.classifiers.DataPoint;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.linear.Vec;
import misc.VecImpl;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import javax.management.RuntimeErrorException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a unique set of pedigrees that explain all seen pedigrees for simulation
 * For each template, has a saved mapping of the
 * simulated feature distributions between each pair of living individuals.
 * These distributions can be reused, to save time
 *
 */
public class PedLikelihoodCalcInheritancePaths extends PedLikelihoodCalcAbs {

    private final Map<String, MultivariateKDE> kdeMap = new HashMap<>();

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
        //(IBDFeature distributions that were calculated previously for synonymous pedigree)
        for (PedVertex v1 : descendants1) {
            if (!v1.isAlive()) continue;//compare only living descendants
            for (PedVertex v2 : descendants2) {
                if (!v2.isAlive()) continue;//compare only living descendants
                if (v1.getId() == v2.getId()) continue;//don't self compare

                List<int[]> commonAncestorsDepth = p.getCommonAncestorDepths(p.getVertex(v1.getId()), p.getVertex(v2.getId()));
                String pairID = v1.getId() + "." + v2.getId();

                StringBuilder key = new StringBuilder();
                for (int[] depths : commonAncestorsDepth)
                    key.append("[").append(depths[0]).append(",").append(depths[1]).append("]");

                if (!kdeMap.containsKey(key.toString()) && isFirstMissing) {
                    simDataSets = sampleFeaturesFromInheritanceSpace(p, true, descendants1, descendants2);
                    isFirstMissing = false;
                    List<DataPoint> pairDataSet = simDataSets.get(pairID);
                    if (pairDataSet == null) {
                        MyLogger.error("PedLikelihoodCalcIP::calcLikelihood::Null dataSet " + pairID + "," + key);
                        MyLogger.error(simDataSets.keySet().toString());
                        throw new RuntimeErrorException(new Error("Null dataSet"));
                    }
                    //MyLogger.important(pairDataSet.get(0).getNumericalValues().toString());
                    kdeMap.put(key.toString(), estimateDensity(pairDataSet));

                }
                //We already sampled features from the Inheritance space
                if (!kdeMap.containsKey(key.toString()) && !isFirstMissing) {
                    List<DataPoint> pairDataSet = simDataSets.get(pairID);
                    if (pairDataSet == null) {
                        MyLogger.error("PedLikelihoodCalcIP::calcLikelihood::Null dataSet " + pairID + "," + key);
                        throw new RuntimeErrorException(new Error("Null dataSet"));
                    }
                    kdeMap.put(key.toString(), estimateDensity(pairDataSet));
                }

                //Should have key in map by now
                MultivariateKDE kde = kdeMap.get(key.toString());

                Vec obsFeatures = new VecImpl(0, 0);
                //Vec obsFeatures = new VecImpl(0);
                Edge e = IBDGraph.getUndirectedEdge(idConversion.get(v1.getId()), idConversion.get(v2.getId()));
                if (e != null)
                    obsFeatures = e.getWeight().asVector();

                double pairLogLikelihood = Math.log(kde.pdf(obsFeatures));

                if (Double.isInfinite(pairLogLikelihood)) {
                    pairLogLikelihood = -100;
                }
                pairNum++;
                logLikelihood += pairLogLikelihood;
            }

        }
        return logLikelihood / pairNum;
    }
}
