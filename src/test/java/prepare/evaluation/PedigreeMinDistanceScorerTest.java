package prepare.evaluation;

import com.google.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.*;

public class PedigreeMinDistanceScorerTest {

    @Test
    public void  PedigreeMinDistanceScorerEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_start100_end100_gen3/pedigree.demographics").getFile();
        String realPedigree = Resources.getResource("pedigree_start100_end100_gen3/pedigree.structure").getFile();
        String inferredPedigreeFile1 = Resources.getResource("pedigree_start100_end100_gen3/reconstructed.1").getFile();
        String inferredPedigreeFile2 = Resources.getResource("pedigree_start100_end100_gen3/reconstructed.2").getFile();
        String inferredPedigreeFile3 = Resources.getResource("pedigree_start100_end100_gen3/reconstructed.3").getFile();
        String inferredPedigreeFile4 = Resources.getResource("pedigree_start100_end100_gen3/reconstructed.4").getFile();


        PedigreeMinDistanceScorer pedigreeMinDistanceScorer = new PedigreeMinDistanceScorer(inferredPedigreeFile1, realPedigree, demographicsFile);
        PedAccuracy pedAccuracy = pedigreeMinDistanceScorer.run();
        assertEquals(0.035, pedAccuracy.getSensitivity(), 0.01);
        assertEquals(1, pedAccuracy.getSpecificity(), 0.01);

        pedigreeMinDistanceScorer = new PedigreeMinDistanceScorer(inferredPedigreeFile2, realPedigree, demographicsFile);
        pedAccuracy = pedigreeMinDistanceScorer.run();
        assertEquals(0.10, pedAccuracy.getSensitivity(), 0.01);
        assertEquals(1, pedAccuracy.getSpecificity(), 0.01);

        pedigreeMinDistanceScorer = new PedigreeMinDistanceScorer(inferredPedigreeFile3, realPedigree, demographicsFile);
        pedAccuracy = pedigreeMinDistanceScorer.run();
        assertEquals(0.30, pedAccuracy.getSensitivity(), 0.01);
        assertEquals(0.99, pedAccuracy.getSpecificity(), 0.01);

        pedigreeMinDistanceScorer = new PedigreeMinDistanceScorer(inferredPedigreeFile4, realPedigree, demographicsFile);
        pedAccuracy = pedigreeMinDistanceScorer.run();
        assertEquals(0.58, pedAccuracy.getSensitivity(), 0.01);
        assertEquals(0.77, pedAccuracy.getSpecificity(), 0.01);
    }

}