package simulator;

import graph.MyLogger;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;
import prepare.IBDFeaturesWeight;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class WrightFisherSimulator {
    private final int initialPopSize;
    private final int finalPopSize;
    private final int generations;
    private final double monogamyProb;
    private final String outDir;
    private final double popIncreaseRatio;
    private final Random randomGenerator = new Random();
    private final Pedigree ped = new Pedigree();
    private final PrintWriter pedWriter;
    Map<String, Integer> structure = new HashMap<>();
    List<List<Integer>> couples = new ArrayList<>();
    private Genotype[] genotypes;
    private boolean[] genders;
    private Genotype[] nextGenotypes;
    private boolean[] nextGenders;
    private int popSize;
    private int totalIndividuals = 0;
    private final double familySizeControlRate = 0.2;

    private static Namespace parseArgs(String[] argv) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(WrightFisherSimulator.class.getSimpleName())
                .defaultHelp(true)
                .description("Wright-Fisher population simulator (random mating of diploid population)");

        parser.addArgument("outputDir");
        parser.addArgument("-initPopulationSize")
                .type(Integer.class)
                .help("initial population size")
                .setDefault(100);
        parser.addArgument("-finalPopulationSize")
                .type(Integer.class)
                .help("final population size")
                .setDefault(100);
        parser.addArgument("-numOfGenerations")
                .type(Integer.class)
                .help("number of generations to simulate mating for")
                .setDefault(5);
        parser.addArgument("-monogamyRate")
                .type(Double.class)
                .help("probability of a person to stay monogamous")
                .setDefault(1.0);
        return parser.parseArgsOrFail(argv);
    }

    public WrightFisherSimulator(Namespace args) {
        initialPopSize = args.getInt("initPopulationSize");
        finalPopSize = args.getInt("finalPopulationSize");
        generations = args.getInt("numOfGenerations");
        monogamyProb = args.getDouble("monogamyRate");
        outDir = args.getString("outputDir");
        popIncreaseRatio = Math.pow(finalPopSize / (double) initialPopSize, 1.0 / generations);
        System.out.println("popIncreaseRatio=" + popIncreaseRatio);
        MyLogger.important("monogamyProb=" + monogamyProb);
        genotypes = new Genotype[popSize];
        genders = new boolean[popSize];
        popSize = initialPopSize;
        new File(outDir).mkdirs();
        File pedFile = new File(outDir + "/pedigree.ped");
        try {
            pedWriter = new PrintWriter(pedFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] argv) {
        Namespace args = parseArgs(argv);
        WrightFisherSimulator wrightFisherSimulator = new WrightFisherSimulator(args);
        wrightFisherSimulator.run();
    }

    private void run() {

        createFounderGenerationGenotypes();

        for (int geneneration = 1; geneneration < generations; geneneration++) {
            MyLogger.important("generation " + geneneration);
            int nextPopSize = (int) Math.round(popSize * popIncreaseRatio);
            nextGenotypes = new Genotype[nextPopSize];
            nextGenders = new boolean[nextPopSize];

            System.out.println("totalIndividuals=" + totalIndividuals);
            System.out.println("new populationSize=" + nextPopSize);
            //Sample parents for each individual, and recombine to get new genotypes
            for (int i = 0; i < nextPopSize; i++) {
                simulateIndividual(geneneration, i);
            }
            incrementGeneration(nextPopSize);
        }
        pedWriter.close();

        totalIndividuals += popSize;

        writeOutputs();
        MyLogger.debug("");
    }

    private void incrementGeneration(int nextPopSize) {
        System.out.println("Move to next generation");

        for (int i = 0; i < popSize; i++)
            couples.set(i, null);
        for (int i = popSize; i < nextPopSize; i++)
            couples.add(null);

        totalIndividuals += popSize;
        System.out.println("totalIndividuals=" + totalIndividuals);
        popSize = nextPopSize;
        System.out.println("populationSize=" + popSize);
        genotypes = nextGenotypes;
        genders = nextGenders;
    }

    private void simulateIndividual(int generation, int individualIndex) {
        boolean foundMates = false;
        int parent = -1;
        int mate = -1;
        //Find parents to individual i
        while (!foundMates) {
            parent = randomGenerator.nextInt(popSize);
            boolean parentGender = genders[parent];
            List<Integer> mates = couples.get(parent);

            //Already has a mate (or mates)
            if (mates != null) {
                //MyLogger.important("parent " + parent + " has a mate/mates: " + mates);

                //Reduce family size by randomly skipping mated parents
                if (randomGenerator.nextDouble() > familySizeControlRate) {
                    //MyLogger.important("Not ready to have another child yet");
                    continue;
                }

                if (randomGenerator.nextDouble() < monogamyProb) {//Passed monogamy test
                    foundMates = true;
                    mate = mates.get(randomGenerator.nextInt(mates.size())); //sample from previous mates uniformly
                    MyLogger.important("having another child with " + mate);

                } else//(Cheated)
                    MyLogger.important("parent " + parent + " has an out of marraige child");
            }
            if (!foundMates) {
                //If need new mate
                mate = randomGenerator.nextInt(popSize);
                boolean mateGender = genders[mate];
                List<Integer> mateMates = couples.get(mate);
                double agreeProb;
                if (mateMates == null)
                    agreeProb = 1;
                else
                    agreeProb = randomGenerator.nextDouble();

                //Matching gender, and mate agreed to have a child
                if (parentGender != mateGender && agreeProb >= monogamyProb) {
                    foundMates = true;
                    if (mates == null)
                        mates = new ArrayList<>();
                    if (mateMates == null)
                        mateMates = new ArrayList<>();

                    MyLogger.important(parent + " is having first child with " + mate);
                    mates.add(mate);
                    couples.set(parent, mates);
                    mateMates.add(parent);
                    couples.set(mate, mateMates);
                }
            }
        }
        //Create child genotype from parent and mate
        MyLogger.debug("recombine = " + genotypes[parent]);
        MyLogger.debug("recombine = " + genotypes[mate]);

        nextGenotypes[individualIndex] = new Genotype(genotypes[parent].recombine(), genotypes[mate].recombine());
        nextGenders[individualIndex] = randomGenerator.nextBoolean();
        int fatherId;
        int motherId;
        if (genders[parent]) {
            //fatherId=totalIndividuals-(gen*popSize)+parent;
            //motherId=totalIndividuals-(gen*popSize)+mate;
            fatherId = totalIndividuals + parent;
            motherId = totalIndividuals + mate;
        } else {
            //fatherId=totalIndividuals-(gen*popSize)+mate;
            //motherId=totalIndividuals-(gen*popSize)+parent;
            fatherId = totalIndividuals + mate;
            motherId = totalIndividuals + parent;
        }
        //int id = (totalIndividuals-((gen+1)*popSize)+i);
        int id = totalIndividuals + popSize + individualIndex;
        MyLogger.important("Adding vertex " + id + " " + fatherId + " " + motherId);
        MyLogger.debug("genotype = " + nextGenotypes[individualIndex]);

        ped.addVertex(id, fatherId, motherId, false);
        pedWriter.println(id + ":" + fatherId + "-" + motherId + " generation:" + (generations - generation));
        structure.put(id + "\t" + fatherId + "\t" + motherId, (generations - generation));
    }

    private void writeOutputs() {
        String structName = outDir + "/pedigree.structure";
        File ibdIped = new File(outDir + "/pedigree.iped.ibd");
        File demFile = new File(outDir + "/pedigree.demographics");
        File ibdFile = new File(outDir + "/pedigree.ibd");
        try {
            writeIpedIBDFile(genotypes, ibdIped);
            List<PedVertex> lastGen = new ArrayList<>();
            writeDemographics(demFile, lastGen);
            ped.pruneExtinct(lastGen);
            writePedigreeStructure(structName);
            writeIBDFile(ibdFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writePedigreeStructure(String structName) throws IOException {
        for (int gen = 2; gen <= generations; gen++) {
            PrintWriter structWriter = new PrintWriter(new File(structName + (gen - 1)));
            for (String key : structure.keySet()) {
                String[] split = key.split("\t");
                int id = Integer.parseInt(split[0]);
                if (ped.hasVertex(id) && structure.get(key) < gen)
                    structWriter.println(key);
            }
            structWriter.close();
        }
        ped.writeToFile(new File(structName));
    }

    private void writeDemographics(File dem, List<PedVertex> lastGen) throws FileNotFoundException {
        PrintWriter demWriter = new PrintWriter(dem);
        demWriter.println("name\tage\tgender");
        for (int i = 0; i < popSize; i++) {
            if (genders[i])
                demWriter.println(totalIndividuals - popSize + i + "\t" + 20 + "\t" + 0);
            else
                demWriter.println(totalIndividuals - popSize + i + "\t" + 20 + "\t" + 1);
            lastGen.add(ped.getVertex(totalIndividuals - popSize + i));
        }
        demWriter.close();
    }

    private void createFounderGenerationGenotypes() {
        genotypes = new Genotype[popSize];
        genders = new boolean[popSize];
        for (int i = 0; i < popSize; i++) {
            genotypes[i] = new Genotype(i);
            genders[i] = randomGenerator.nextBoolean();
            couples.add(null);
            MyLogger.debug("Added vertex " + i + " " + -1 + " " + -1);
            ped.addVertex(i);
        }
    }


    /**
     * Print the number of IBD segments, and the mean segment length (Mbp)
     * @param ibd       ibd file
     */
    private void writeIBDFile(File ibd)
            throws FileNotFoundException {
        PrintWriter ibdWriter = new PrintWriter(ibd);
        for (int i = 0; i < popSize; i++) {
            for (int j = i + 1; j < popSize; j++) {
                IBDFeaturesWeight ibdW = IBDFeaturesWeight.calcIBDFeatureWeight(genotypes[i], genotypes[j], false, false);
                if (ibdW.getSegmentNum() > 0) {
                    MyLogger.important("IBD : " + (totalIndividuals - popSize + i) + "," + (totalIndividuals - popSize + j) + " =" + ibdW);
                    //MyLogger.important(0 + " geno= " + genotypes[0]);
                    //MyLogger.important(j + " geno= " + genotypes[j]);
                    ibdWriter.println((totalIndividuals - popSize + i) + "\t" + (totalIndividuals - popSize + j) + "\t" + ibdW.getSegmentNum() + "\t" + ibdW.getMeanLength() + "\t20\t20");
                }
            }
        }
        ibdWriter.close();
    }


    private void writeIpedIBDFile(Genotype[] genotypes, File ibdFile) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(ibdFile);
        Haplotype windows = new Haplotype();
        for (int chr = 1; chr <= 22; chr++) {
            for (int pos = 1; pos <= HumanGenome.getChrLength(chr); pos += 1000000) {
                HapRegion window = new HapRegion(new Location(chr, pos), new Location(chr, pos + 1000000), "");
                windows.addRegion(window);
            }
        }

        for (Genotype g : genotypes) {
            printHapoltypeWindows(printWriter, windows, g.getHap1());
            printHapoltypeWindows(printWriter, windows, g.getHap2());

        }
        printWriter.close();
    }

    private void printHapoltypeWindows(PrintWriter printWriter, Haplotype windows, Haplotype hap1) {
        windows.rewind();
        HapRegion window = windows.getCurrRegion();
        HapRegion hapRegion = hap1.getCurrRegion();
        //MyLogger.important("r=" + r);

        while (windows.hasMore()) {

            //skip regions that end before window
            while (hapRegion.getEnd().compareTo(window.getStart()) < 0) {
                hapRegion = hap1.getNextRegion();
                MyLogger.important("skip r=" + hapRegion);

            }

            //transpass all intersecting regions
            List<HapRegion> intersect = new ArrayList<>();
            while (hapRegion.getStart().compareTo(window.getEnd()) < 0) {

                Location start = hapRegion.getStart();
                if (start.compareTo(window.getStart()) < 0)
                    start = window.getStart();

                Location end = hapRegion.getEnd();
                if (end.compareTo(window.getEnd()) > 0)
                    end = window.getEnd();

                HapRegion res = new HapRegion(start, end, hapRegion.getAncestry());
                intersect.add(res);
                if (hapRegion.getEnd().compareTo(window.getEnd()) > 0)
                    break;

                hapRegion = hap1.getNextRegion();
                //MyLogger.important("check next intersecting r=" + r);

            }
            Map<String, Integer> hapLengths = new HashMap<>();
            for (HapRegion in : intersect) {
                String ancestor = in.getAncestry();
                int length = in.getEnd().getPosition() - in.getStart().getPosition();
                if (hapLengths.containsKey(ancestor))
                    hapLengths.put(ancestor, hapLengths.get(ancestor) + length);
                else
                    hapLengths.put(ancestor, length);
                //	MyLogger.important("intersect " + window + " : " + in);
            }
            int maxLength = 0;
            String maxAncestor = "";
            for (String key : hapLengths.keySet()) {
                if (hapLengths.get(key) > maxLength) {
                    maxLength = hapLengths.get(key);
                    maxAncestor = key;
                }
            }
            String[] split = maxAncestor.split("\\.");
            printWriter.print(split[0] + split[1] + " ");
            //MyLogger.important(window.getStart().getChr() + " " + window.getStart().getPosition() + " " + window.getEnd().getPosition() + " " + maxAncestor);

            window = windows.getNextRegion();

        }
        printWriter.print("\n");

    }
}


