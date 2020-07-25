package relationship;

import graph.Graph;
import graph.MyLogger;
import jsat.classifiers.DataPoint;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.multivariate.MetricKDE;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.linear.distancemetrics.EuclideanDistance;
import prepare.IBDFeaturesWeight;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import javax.management.RuntimeErrorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class PedLikelihoodCalcAbs {
    private final int numOfSimIter;
    private final boolean phased;

    public PedLikelihoodCalcAbs(int numOfSimIterations, boolean phased) {
        numOfSimIter = numOfSimIterations;
        this.phased = phased;
    }

    public abstract double calcLikelihood(Pedigree p, Graph iBDgraph,
                                          Map<Integer, Integer> idConversion, List<PedVertex> descendants1, List<PedVertex> descendants2);

    /**
     * Create IBD feature distribution for every pair of living individuals in pedigree
     * Simulate recombination process N times
     **/
    Map<String, List<DataPoint>> sampleFeaturesFromInheritanceSpace(Pedigree p, boolean addNoise) {

        Map<String, List<DataPoint>> simDataSets = new HashMap<>();

        for (int i = 1; i <= numOfSimIter; i++) {
            MyLogger.debug("Simulate recombinations of pedigree");
            p.simulateRecombinations();

            MyLogger.debug("Calc pairwise IBD sharing");

            //Save sharing features for each pair of living individuals
            for (PedVertex v1 : p.getLiving()) {
                for (PedVertex v2 : p.getLiving()) {
                    MyLogger.debug("calcIBDSharing " + v1 + "," + v2);

                    if (v1.getId() >= v2.getId()) continue;//Do only one side calculation

                    String pairID = v1.getId() + "." + v2.getId();

                    //calculate features for pair
                    DataPoint simFeatures = IBDFeaturesWeight.calcIBDFeatureWeight(p.getGenotype(v1.getId()), p.getGenotype(v2.getId()), addNoise, phased).asDataPoint();

                    //Initialize datasets
                    if (i == 1) {
                        List<DataPoint> pairDataSet = new ArrayList<>();
                        pairDataSet.add(simFeatures);
                        simDataSets.put(pairID, pairDataSet);
                    } else //Add to existing datasets
                        simDataSets.get(pairID).add(simFeatures);

                    MyLogger.debug("sampleFeaturesFromInheritanceSpace(" + v1 + "," + v2 + ")::simW=" + simFeatures.getNumericalValues());
                }
            }
        }
        return simDataSets;
    }

    /**
     * Create IBD feature distribution for every pair of living individuals in pedigree
     * Simulate recombination process N times
     **/
    Map<String, List<DataPoint>> sampleFeaturesFromInheritanceSpace(Pedigree p, boolean addNoise, List<PedVertex> descendants1, List<PedVertex> descendants2) {

        Map<String, List<DataPoint>> simDataSets = new HashMap<>();

        for (int i = 1; i <= numOfSimIter; i++) {
            MyLogger.debug("Simulate recombinations of pedigree");
            p.simulateRecombinations();

            MyLogger.debug("Calc pairwise IBD sharing");

            //Save sharing features for each pair of living individuals
            for (PedVertex v1 : descendants1) {
                if (!v1.isAlive()) continue;//compare only living descendents
                for (PedVertex v2 : descendants2) {
                    if (!v2.isAlive()) continue;//compare only living descendents

                    if (v1.getId() == v2.getId()) continue;//don't self compare
                    if (simDataSets.containsKey(v2.getId() + "." + v1.getId())) continue;//do only one side calculation

                    String pairID = v1.getId() + "." + v2.getId();
                    //MyLogger.important("calc Features for " + pairID);
                    //calculate features for pair
                    DataPoint simFeatures = IBDFeaturesWeight.calcIBDFeatureWeight(p.getGenotype(v1.getId()), p.getGenotype(v2.getId()), addNoise, phased).asDataPoint();

                    //Initialize datasets
                    if (i == 1) {
                        List<DataPoint> pairDataSet = new ArrayList<>();
                        pairDataSet.add(simFeatures);
                        simDataSets.put(pairID, pairDataSet);
                    } else //Add to existing datasets
                        simDataSets.get(pairID).add(simFeatures);

                    MyLogger.debug("sampleFeaturesFromInheritanceSpace(" + v1 + "," + v2 + ")::simW=" + simFeatures.getNumericalValues());
                }
            }
        }
        Map<String, List<DataPoint>> bothDirectionsSimDataSets = new HashMap<>();

        for (String key : simDataSets.keySet()) {
            String[] split = key.split("\\.");
            String reverseKey = split[1] + "." + split[0];
            bothDirectionsSimDataSets.put(key, simDataSets.get(key));
            bothDirectionsSimDataSets.put(reverseKey, simDataSets.get(key));
        }
        return bothDirectionsSimDataSets;
    }

    MultivariateKDE estimateDensity(List<DataPoint> dataSet) {
        EuclideanDistance ed = new EuclideanDistance();
        MetricKDE kde = new MetricKDE(GaussKF.getInstance(), ed);
        if (dataSet == null) {
            MyLogger.error("PedLikelihoodCalcAbs::estimateDensity::Null dataSet");
            throw new RuntimeErrorException(new Error("Null dataSet"));
        }
        kde.setUsingDataList(dataSet);
        //MyLogger.important("BW = " + kde.getBandwith());

        kde.setBandwith(8);
        return kde;
    }

    void printExplenationForInf(List<DataPoint> l) {
        MyLogger.debug("dataSet for pair");
        MyLogger.debug("list size=" + l.size());
        for (int i = 0; i < numOfSimIter; i++) {
            DataPoint point = l.get(i);
            MyLogger.debug(point.getNumericalValues().toString());
        }
    }

}

