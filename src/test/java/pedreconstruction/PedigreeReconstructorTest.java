package pedreconstruction;

import com.google.common.io.Resources;
import org.junit.Test;

public class PedigreeReconstructorTest{

    @Test
    public void pedigreeReconstructorEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_100_100/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_100_100/pedigree.ibd").getFile();
        String outPref = "test_outputs/pedigree_100_100/reconstructed.";

        PedigreeReconstructor pedigreeReconstructor = new PedigreeReconstructor(ibdFile, demographicsFile, outPref,
                false, false);
        pedigreeReconstructor.reconstruct();
    }

}