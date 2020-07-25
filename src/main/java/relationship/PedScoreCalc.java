package relationship;

import graph.Edge;
import graph.Graph;
import jsat.classifiers.DataPoint;
import jsat.linear.Vec;
import misc.VecImpl;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import java.util.List;
import java.util.Map;

public class PedScoreCalc extends PedLikelihoodCalcAbs {

    int numOfIter;
    int thresh;
    double meanLoss;
    double[] lossArr;

    public PedScoreCalc(int numOfIter, int debugThresh, boolean phased) {
        super(1, phased);
        this.numOfIter = numOfIter;
        this.thresh = debugThresh;
        lossArr = new double[numOfIter];
    }

    public double calcLikelihood(Pedigree p, Graph IBDGraph, Map<Integer, Integer> idConversion, int vid1, int vid2) {
        return -1;
    }

    public void calcLoss(Pedigree pedigree, Graph IBDGraph) {
        double totalLoss = 0;
        for (int i = 0; i < numOfIter; i++) {
            int pairNum = 0;
            double loss = 0;
            Map<String, List<DataPoint>> simDataSets = sampleFeaturesFromInheritanceSpace(pedigree, false);

            //RMSE score for IBD features
            for (PedVertex v1 : pedigree.getLiving()) {
                for (PedVertex v2 : pedigree.getLiving()) {
                    if (v1.getId() >= v2.getId()) continue;//Do only one side calculation
                    String pairID = v1.getId() + "." + v2.getId();
                    Vec simFeatures = simDataSets.get(pairID).get(0).getNumericalValues();
                    Vec obsFeatures = new VecImpl(0, 0);
                    Edge e = IBDGraph.getUndirectedEdge(v1.getId(), v2.getId());
                    if (e != null)
                        obsFeatures = e.getWeight().asVector();

                    double pairLoss = Math.pow(obsFeatures.get(0) * obsFeatures.get(1) - simFeatures.get(0) * simFeatures.get(1), 2);
                    //MyLogger.important(v1.getId()+","+v2.getId()+" simFeatures " + simFeatures);
                    //MyLogger.important(v1.getId()+","+v2.getId()+" obsFeatures " + obsFeatures);
                    //MyLogger.important(v1.getId()+","+v2.getId()+" squared loss " + pairLoss);
                    //Squared difference in total IBD length (obs vs. expected)
                    loss += pairLoss;
                    pairNum++;
                }
            }
            double RMSIBDE = Math.sqrt(loss / pairNum);
            totalLoss += RMSIBDE;
            lossArr[i] = RMSIBDE;
        }
        meanLoss = totalLoss / numOfIter;
    }

    public double getMeanLoss() {
        return meanLoss;
    }

    public double getStdLoss() {
        double totalDeviance = 0;
        for (int i = 0; i < numOfIter; i++)
            totalDeviance += Math.pow(lossArr[i] - meanLoss, 2);
        return Math.sqrt(totalDeviance / numOfIter);
    }

    @Override
    public double calcLikelihood(Pedigree p, Graph iBDgraph,
                                 Map<Integer, Integer> idConversion, List<PedVertex> descendants1,
                                 List<PedVertex> descendants2) {
        // TODO Auto-generated method stub
        return 0;
    }
}
