package evaluation;

import com.google.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PedigreeIBDFitScoreCalculatorTest {

    @Test
    public void calcPedigreeIBDScoreEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.ibd").getFile();
        String inferredPedigreeFile1 = Resources.getResource("pedigree_start10_end10_gen3/reconstructed.1").getFile();
        String inferredPedigreeFile2 = Resources.getResource("pedigree_start10_end10_gen3/reconstructed.2").getFile();
        String inferredPedigreeFile3 = Resources.getResource("pedigree_start10_end10_gen3/reconstructed.3").getFile();

        double[] expectedScores = new double[]{11.3, 9.3, 7.66};
        int i = 0;
        for(String file : new String[]{inferredPedigreeFile1, inferredPedigreeFile2, inferredPedigreeFile3}) {
            PedigreeIBDFitScoreCalculator pedigreeIBDFitScoreCalculator = new PedigreeIBDFitScoreCalculator(file, demographicsFile, ibdFile, 0);
            double score = pedigreeIBDFitScoreCalculator.evaluate();
            assertEquals(score, expectedScores[i], 0.1);
            i++;
        }


    }

}