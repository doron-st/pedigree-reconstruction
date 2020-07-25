package pedreconstruction;

import com.google.common.io.Resources;
import org.junit.Test;
import pedigree.Pedigree;

import static org.junit.Assert.assertEquals;

public class PedigreeReconstructorTest{

    @Test
    public void pedigreeReconstructorEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.ibd").getFile();
        String outPref = "test_outputs/pedigree_start10_end10_gen3/reconstructed.";

        PedigreeReconstructor pedigreeReconstructor = new PedigreeReconstructor(ibdFile, demographicsFile, outPref,
                false, false, 2);
        Pedigree pedigree = pedigreeReconstructor.reconstruct();
        assertEquals(262, pedigree.getVertices().size(), 10);
        assertEquals(100, pedigree.getLiving().size());
        assertEquals(88, pedigree.getFounders().size(), 10);
    }
}