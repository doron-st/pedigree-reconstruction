package prepare.evaluation;

import prepare.misc.MyLogger;
import prepare.pedigree.Pedigree;

public class PedAccuracy {
    private double score = 0;
    private double relatedPairs = 0;
    private double fp = 0;
    private int pairs = 0;
    private double bothUnrelated = 0;
    private double inferredRelatedPairs;
    private double ipedScore;
    private double sensitivity;
    private double specificity;

    public void calcAccuracy(Pedigree ip, Pedigree rp) {
        for (Pedigree.PedVertex v1 : ip.getLiving()) {
            for (Pedigree.PedVertex v2 : ip.getLiving()) {

                if (v1.getId() >= v2.getId()) continue;//Do only one side calculation
                pairs++;
                int inferredDistance = v1.distanceTo(v2);
                int realDistance = rp.getVertex(v1.getId()).distanceTo(rp.getVertex(v2.getId()));

                if (realDistance < Integer.MAX_VALUE) {//related
                    relatedPairs++;
                    if (inferredDistance == realDistance) {//same minimal distance
                        score++;
                    }
                } else if (inferredDistance < Integer.MAX_VALUE)//unrelated by real prepare.pedigree, and we say related
                    //Take seriously only if both pedigrees are the same level
                    fp++;
                else //both are unrelated
                    bothUnrelated++;
            }
        }

        inferredRelatedPairs =  (score + fp);
        ipedScore = (score + bothUnrelated) / pairs;
        sensitivity = score / relatedPairs;
        specificity = score / (score + fp);
        MyLogger.important("True related pairs=" + relatedPairs);
        MyLogger.important("Inferred related pairs=" + inferredRelatedPairs);
        MyLogger.important("IPED_score=" + ipedScore);
        MyLogger.important("Sensitivity=" + sensitivity);
        MyLogger.important("Specificity=" + specificity);
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getSpecificity() {
        return specificity;
    }
}
