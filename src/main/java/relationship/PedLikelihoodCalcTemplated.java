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

import java.util.ArrayList;
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
public class PedLikelihoodCalcTemplated extends PedLikelihoodCalcAbs {

    private final Map<String, List<Pedigree>> templatesBySize = new HashMap<String, List<Pedigree>>();
    private final Map<Pedigree, Map<String, MultivariateKDE>> kdeMapsByTemplate = new HashMap<Pedigree, Map<String, MultivariateKDE>>();
    private final Map<Pedigree, Map<String, List<DataPoint>>> dataSetMap = new HashMap<Pedigree, Map<String, List<DataPoint>>>();
    private int templateNum = 1;
    private int numOfTemplateUsages = 0;

    public PedLikelihoodCalcTemplated(int numOfSimsIter, boolean phased) {
        super(numOfSimsIter, phased);
    }


    @Override
    public double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, List<PedVertex> descendants1, List<PedVertex> descendants2) {
        Map<String, MultivariateKDE> kdeMap;

        double logLikelihood = 0;

        //Use an existing template
        //(IBDFeature distributions that were calculated previouly for synonymous pedigree)
        if (hasTemplate(p)) {
            MyLogger.info("Using saved template for likelihood calculation, numOfTemplateUsages=" + (++numOfTemplateUsages));
            kdeMap = getKDEMap(p);
        } else {
            MyLogger.debug("Create template " + templateNum + " for Pedigree:" + p);

            //	try {p.writeToFile(new File("D://workspace/pedigree/debug/template" + templateNum));
            //	} catch (IOException e) { throw new RuntimeException("Failed writing template " + templateNum ,e);}

            kdeMap = new HashMap<String, MultivariateKDE>();
            Map<String, List<DataPoint>> simDataSets = sampleFeaturesFromInheritanceSpace(p, true, descendants1, descendants2);

            //Estimate empirical simulated distribution with kernel density functions
            for (PedVertex v1 : descendants1) {
                if (!v1.isAlive()) continue;//compare only living descendents
                for (PedVertex v2 : descendants2) {
                    if (!v2.isAlive()) continue;//compare only living descendents
                    if (v1.getId() == v2.getId()) continue;//don't self compare

                    if (kdeMap.containsKey(v2.getId() + "." + v1.getId())) continue;//Do only one side calculation

                    String pairID = v1.getId() + "." + v2.getId();
                    MultivariateKDE kde = estimateDensity(simDataSets.get(pairID));
                    kdeMap.put(pairID, kde);
                }
            }

            //Add new template
            addTemplate(p, kdeMap);
            dataSetMap.put(p, simDataSets);
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
                //MyLogger.important(pairID + ":obs=" + obsFeatures + "exp=" + kdeMap.get(pairID).getNearby(obsFeatures));
                //MyLogger.important(v1 + "," + v2 + " logLikelihood=" + pairLogLikelihood);

                if (Double.isInfinite(pairLogLikelihood)) {
                    Map<String, List<DataPoint>> dataSet = getDataSet(p);
                    printExplenationForInf(dataSet.get(pairID));
                    pairLogLikelihood = -100;
                }
                pairNum++;
                logLikelihood += pairLogLikelihood;
            }
        }
        return logLikelihood / pairNum;
    }

    /**
     * Assume template is final!
     *
     * @param t      - template
     * @param kdeMap
     */
    private void addTemplate(Pedigree t, Map<String, MultivariateKDE> kdeMap) {
        MyLogger.debug("adding template " + t);

        String tKey = t.getVertices().size() + "," + t.getLiving().size() + "," + t.getFounders().size();
        //Pedigree cloneT = new Pedigree(t);
        if (!templatesBySize.containsKey(tKey)) {
            templatesBySize.put(tKey, new ArrayList<Pedigree>());
            MyLogger.info("First time seeing template with key " + tKey);
        }

        templatesBySize.get(tKey).add(t);
        kdeMapsByTemplate.put(t, kdeMap);
        templateNum++;
    }

    private boolean hasTemplate(Pedigree t) {
        String tKey = t.getVertices().size() + "," + t.getLiving().size() + "," + t.getFounders().size();
        if (templatesBySize.containsKey(tKey))
            return templatesBySize.get(tKey).contains(t);
        else
            return false;
    }

    private Map<String, MultivariateKDE> getKDEMap(Pedigree p) {
        String tKey = p.getVertices().size() + "," + p.getLiving().size() + "," + p.getFounders().size();
        for (Pedigree t : templatesBySize.get(tKey))
            if (p.equals(t))
                return kdeMapsByTemplate.get(t);
        return null;
    }

    private Map<String, List<DataPoint>> getDataSet(Pedigree p) {
        String tKey = p.getVertices().size() + "," + p.getLiving().size() + "," + p.getFounders().size();
        for (Pedigree t : templatesBySize.get(tKey))
            if (p.equals(t))
                return dataSetMap.get(t);
        return null;
    }


    public double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, int vid1, int vid2) {
        double logLikelihood = 0;

        Map<String, MultivariateKDE> kdeMap;

        //Use an existing template
        //(IBDFeature distributions that were calculated previouly for synonymous pedigree)
        if (hasTemplate(p)) {
            MyLogger.info("Using saved template for likelihood calculation, numOfTemplateUsages=" + (++numOfTemplateUsages));
            kdeMap = getKDEMap(p);
        } else {
            MyLogger.debug("Create template " + templateNum + " for Pedigree:" + p);

            //	try {p.writeToFile(new File("D://workspace/pedigree/debug/template" + templateNum));
            //	} catch (IOException e) { throw new RuntimeException("Failed writing template " + templateNum ,e);}

            kdeMap = new HashMap<String, MultivariateKDE>();
            Map<String, List<DataPoint>> simDataSets = sampleFeaturesFromInheritanceSpace(p, true);

            //Estimate empirical simulated distribution with kernel density functions
            for (PedVertex v1 : p.getLiving()) {
                for (PedVertex v2 : p.getLiving()) {

                    if (v1.getId() >= v2.getId()) continue;//Do only one side calculation

                    String pairID = v1.getId() + "." + v2.getId();
                    MultivariateKDE kde = estimateDensity(simDataSets.get(pairID));
                    kdeMap.put(pairID, kde);
                }
            }

            //Add new template
            addTemplate(p, kdeMap);
            dataSetMap.put(p, simDataSets);
        }

        /*
         * Calc likelihood of observed data for each pair of individuals
         * Assumes Independence between pairs, (not a valid assumption!!)
         */
        MyLogger.debug("Calc likelihood for pair " + vid1 + "," + vid2);
        List<PedVertex> descendants = p.getDescendants(vid1);
        List<PedVertex> descendants2 = p.getDescendants(vid2);
        for (PedVertex v : descendants2)
            if (!descendants.contains(v))
                descendants.add(v);

        for (PedVertex v1 : p.getLiving()) {

            if (!descendants.contains(v1)) continue; //consider only desccendants of vid1 || vid2

            for (PedVertex v2 : p.getLiving()) {

                if (!descendants.contains(v2)) continue;//consider only desccendants of vid1 || vid2
                if (v1.getId() >= v2.getId()) continue;//Do only one side calculation, don't self compare

                String pairID = v1.getId() + "." + v2.getId();

                //Get observed IBD sharing features
                Vec obsFeatures = new VecImpl(0, 0);
                Edge e = IBDGraph.getUndirectedEdge(idConversion.get(v1.getId()), idConversion.get(v2.getId()));
                if (e != null)
                    obsFeatures = e.getWeight().asVector();


                double pairLogLikelihood = Math.log(kdeMap.get(pairID).pdf(obsFeatures));
                //MyLogger.important(pairID + ":obs=" + obsFeatures + "exp=" + kdeMap.get(pairID).getNearby(obsFeatures));
                //MyLogger.important(v1 + "," + v2 + " logLikelihood=" + pairLogLikelihood);

                if (Double.isInfinite(pairLogLikelihood)) {
                    Map<String, List<DataPoint>> dataSet = getDataSet(p);
                    printExplenationForInf(dataSet.get(pairID));
                    pairLogLikelihood = -100;
                }

                logLikelihood += pairLogLikelihood;


            }
        }
        return logLikelihood;
    }
}
