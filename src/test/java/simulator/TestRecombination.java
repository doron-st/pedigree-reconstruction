package simulator;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRecombination {

	@Test
	public void test() {
		Genotype founder = new Genotype(1);
		Haplotype result = founder.recombine();
		assertNotNull(result.getCurrRegion());
		assertEquals(result.getCurrRegion().getStart().getPosition(),1);
	}
}