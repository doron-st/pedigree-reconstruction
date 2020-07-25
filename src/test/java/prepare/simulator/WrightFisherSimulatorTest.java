package prepare.simulator;

import net.sourceforge.argparse4j.inf.Namespace;
import org.junit.Test;

public class WrightFisherSimulatorTest {

    @Test
    public void WrightFisherSimulatorEndToEnd100SamplesTest(){
        String outDir = "test_outputs/pedigree_start100_end100_gen3";
        Namespace args = WrightFisherSimulator.parseArgs(new String[]{outDir});
        WrightFisherSimulator simulator = new WrightFisherSimulator(args);
        simulator.run();
    }
}