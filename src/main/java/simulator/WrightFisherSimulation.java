package simulator;

import graph.MyLogger;
import pedigree.Pedigree;
import prepare.IBDFeaturesWeight;
import pedigree.Pedigree.PedVertex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class WrightFisherSimulation {

    public static void main(String[] args) {

        if (args.length < 4) {
            MyLogger.error("Usage: java -jar WFSim.jar initPopSize finalPopSize generations monogamyProb out");
            System.exit(1);
        }
        int popSize = Integer.parseInt(args[0]);
        int finalPopSize = Integer.parseInt(args[1]);
        int generations = Integer.parseInt(args[2]);
        double monogamyProb = Double.parseDouble(args[3]);
        String out = args[4];

        double popIncreaseRatio = Math.pow(finalPopSize / (double) popSize, 1.0 / generations);
        System.out.println("popIncreaseRatio=" + popIncreaseRatio);
        MyLogger.important("monogamyProb=" + monogamyProb);

        Genotype[] genotypes = new Genotype[popSize];
        boolean[] genders = new boolean[popSize];

        Random rg = new Random();
        Map<String, Integer> structure = new HashMap<String, Integer>();
        List<List<Integer>> couples = new ArrayList<List<Integer>>();

        Pedigree ped = new Pedigree();

        //Create founder genotypes
        for (int i = 0; i < popSize; i++) {
            genotypes[i] = new Genotype(i);
            genders[i] = rg.nextBoolean();
            couples.add(null);
            MyLogger.debug("Added vertex " + i + " " + -1 + " " + -1);
            ped.addVertex(i);
        }

        int totalIndividuals = 0;


        File pedFile = new File(out + "/pedigree.ped");
        pedFile.mkdirs();
        PrintWriter pedWriter = null;
        try {
            pedWriter = new PrintWriter(pedFile);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        for (int gen = 1; gen < generations; gen++) {
            MyLogger.important("generation " + gen);
            int nextPopSize = (int) Math.round(popSize * popIncreaseRatio);
            Genotype[] nextGenotypes = new Genotype[nextPopSize];
            boolean[] nextGenders = new boolean[nextPopSize];

            System.out.println("totalIndividuals=" + totalIndividuals);
            System.out.println("new populationSize=" + nextPopSize);
            //Sample parents for each individual, and recombine to get new genotypes
            for (int i = 0; i < nextPopSize; i++) {
                boolean foundMates = false;
                int parent = -1;
                int mate = -1;
                //Find parents to individual i
                while (!foundMates) {
                    parent = rg.nextInt(popSize);
                    boolean parentGender = genders[parent];
                    List<Integer> mates = couples.get(parent);

                    //Already has a mate (or mates)
                    if (mates != null) {
                        //MyLogger.important("parent " + parent + " has a mate/mates: " + mates);

                        //Reduce family size by randomly skipping mated parents
                        if (rg.nextDouble() > 0.2) {
                            //MyLogger.important("Not ready to have another child yet");
                            continue;
                        }

                        if (rg.nextDouble() < monogamyProb) {//Passed monogamy test
                            foundMates = true;
                            mate = mates.get(rg.nextInt(mates.size())); //sample from previous mates uniformly
                            MyLogger.important("having another child with " + mate);

                        } else//(Cheated)
                            MyLogger.important("parent " + parent + " has an out of marraige child");
                    }
                    if (!foundMates) {
                        //If need new mate
                        mate = rg.nextInt(popSize);
                        boolean mateGender = genders[mate];
                        List<Integer> mateMates = couples.get(mate);
                        double agreeProb;
                        if (mateMates == null)
                            agreeProb = 1;
                        else
                            agreeProb = rg.nextDouble();

                        //Matching gender, and mate agreed to have a child
                        if (parentGender != mateGender && agreeProb >= monogamyProb) {
                            foundMates = true;
                            if (mates == null)
                                mates = new ArrayList<Integer>();
                            if (mateMates == null)
                                mateMates = new ArrayList<Integer>();

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


                nextGenotypes[i] = new Genotype(genotypes[parent].recombine(), genotypes[mate].recombine());
                nextGenders[i] = rg.nextBoolean();
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
                int id = totalIndividuals + popSize + i;
                MyLogger.important("Adding vertex " + id + " " + fatherId + " " + motherId);
                MyLogger.debug("genotype = " + nextGenotypes[i]);

                ped.addVertex(id, fatherId, motherId, false);
                assert pedWriter != null;
                pedWriter.println(id + ":" + fatherId + "-" + motherId + " generation:" + (generations - gen));
                structure.put(id + "\t" + fatherId + "\t" + motherId, (generations - gen));
            }


            //increment generation
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

            System.out.println("Move to next generation");
        }
        pedWriter.close();

        totalIndividuals += popSize;

        String structName = out + "/pedigree.structure";
        File ibdIped = new File(out + "/pedigree.iped.ibd");
        File dem = new File(out + "/pedigree.demographics");
        File ibd = new File(out + "/pedigree.ibd");

        try {
            writeIpedIBDFile(genotypes, ibdIped);

            List<PedVertex> lastGen = new ArrayList<PedVertex>();
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

            ped.pruneExtinct(lastGen);
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

            writeIBDFile(popSize, genotypes, ibd, totalIndividuals);


        } catch (IOException e) {
            e.printStackTrace();
        }
        MyLogger.debug("");
    }


    /**
     * Print the number of IBD segments, and the mean segment length (Mbp)
     *
     * @param popSize   population size
     * @param genotypes - array of genotype objects
     * @param ibd       ibd file
     */
    private static void writeIBDFile(int popSize, Genotype[] genotypes, File ibd, int totalIndividuals)
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


    private static void writeIpedIBDFile(Genotype[] genotypes, File ibdFile) throws FileNotFoundException {
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

    private static void printHapoltypeWindows(PrintWriter printWriter,
                                              Haplotype windows, Haplotype hap1) {

        windows.rewind();
        HapRegion window = windows.getCurrRegion();
        HapRegion r = hap1.getCurrRegion();
        //MyLogger.important("r=" + r);

        while (windows.hasMore()) {

            //skip regions that end before window
            while (r.getEnd().compareTo(window.getStart()) < 0) {
                r = hap1.getNextRegion();
                MyLogger.important("skip r=" + r);

            }

            //transpass all intersecting regions
            List<HapRegion> intersect = new ArrayList<HapRegion>();
            while (r.getStart().compareTo(window.getEnd()) < 0) {

                Location start = r.getStart();
                if (start.compareTo(window.getStart()) < 0)
                    start = window.getStart();

                Location end = r.getEnd();
                if (end.compareTo(window.getEnd()) > 0)
                    end = window.getEnd();

                HapRegion res = new HapRegion(start, end, r.getAncestry());
                intersect.add(res);
                if (r.getEnd().compareTo(window.getEnd()) > 0)
                    break;

                r = hap1.getNextRegion();
                //MyLogger.important("check next intersecting r=" + r);

            }
            Map<String, Integer> hapLengths = new HashMap<String, Integer>();
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


