package prepare.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenotypeTest {

    @Test
    public void test() {
        Genotype founder = new Genotype(1);
        Haplotype result = founder.recombine();
        assertNotNull(result.getCurrRegion());
        assertEquals(result.getCurrRegion().getStart().getPosition(), 1);
    }
}
