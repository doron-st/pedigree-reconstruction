package pedreconstruction;

import misc.MyLogger;
import pedigree.Pedigree;

public class PedAccuracy {

    public void calcAccuracy(Pedigree ip, Pedigree rp) {
        double score = 0;
        double relatedPairs = 0;
        double fp = 0;
        int pairs = 0;
        double bothUnrelated = 0;

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
                } else if (inferredDistance < Integer.MAX_VALUE)//unrelated by real pedigree, and we say related
                    //Take seriously only if both pedigrees are the same level
                    fp++;
                else //both are unrelated
                    bothUnrelated++;
            }
        }
        MyLogger.important("True related pairs=" + relatedPairs);
        MyLogger.important("Inferred related pairs=" + (score + fp));
        MyLogger.important("IPED_score=" + ((score + bothUnrelated) / pairs));
        MyLogger.important("Sensitivity=" + score / relatedPairs);
        MyLogger.important("Specificity=" + (score / (score + fp)));
    }
}
