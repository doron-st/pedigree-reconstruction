package pedreconstruction;

import com.google.common.io.Resources;
import org.junit.Test;

public class PedigreeReconstructorTest{

    @Test
    public void pedigreeReconstructorEndToEndTest(){
        String demographicsFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.demographics").getFile();
        String ibdFile = Resources.getResource("pedigree_start10_end10_gen3/pedigree.ibd").getFile();
        String outPref = "test_outputs/pedigree_start10_end10_gen3/reconstructed.";

        PedigreeReconstructor pedigreeReconstructor = new PedigreeReconstructor(ibdFile, demographicsFile, outPref,
                false, false, 3);
        pedigreeReconstructor.reconstruct();
    }

}