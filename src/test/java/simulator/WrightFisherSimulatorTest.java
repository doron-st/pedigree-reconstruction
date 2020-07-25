package simulator;

import net.sourceforge.argparse4j.inf.Namespace;
import org.junit.Test;

public class WrightFisherSimulatorTest {

    @Test
    public void WrightFisherSimulatorEndToEndTest(){
        String outDir = "test_outputs/pedigree_start10_end10_gen3";
        Namespace args = WrightFisherSimulator.parseArgs(new String[]{outDir});
        WrightFisherSimulator simulator = new WrightFisherSimulator(args);
        simulator.run();
    }

}