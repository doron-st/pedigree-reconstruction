package simulator;

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

    @Test
    public void WrightFisherSimulatorEndToEnd1000SamplesTest(){
        String outDir = "test_outputs/pedigree_start1000_end1000_gen3";
        Namespace args = WrightFisherSimulator.parseArgs(new String[]{outDir, "-initPopulationSize", "1000", "-finalPopulationSize", "1000"});
        WrightFisherSimulator simulator = new WrightFisherSimulator(args);
        simulator.run();
    }

}