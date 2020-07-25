package evaluation;

import com.google.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PedigreeIBDFitScoreCalculatorTest {

    @Test
    public void calcPedigreeIBDScoreEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.ibd").getFile();
        String inferredPedigreeFile = Resources.getResource("pedigree_start10_end10_gen3/reconstructed.3").getFile();

        PedigreeIBDFitScoreCalculator pedigreeIBDFitScoreCalculator = new PedigreeIBDFitScoreCalculator(inferredPedigreeFile, demographicsFile, ibdFile, 0);
        double score = pedigreeIBDFitScoreCalculator.evaluate();

        assertEquals(score, 1.3, 0.01);
        
    }

}