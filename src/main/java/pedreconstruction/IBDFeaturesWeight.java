package pedreconstruction;

import graph.*;
import jsat.classifiers.DataPoint;
import jsat.linear.Vec;
import misc.MyLogger;
import misc.VecImpl;
import common.Genotype;
import common.HapRegion;
import common.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class IBDFeaturesWeight implements Weight {
    private static final long serialVersionUID = -3336217617441788940L;
    private double segmentNum;
    private double meanLength;

    public IBDFeaturesWeight(double segmentNum, double meanLength) {
        this.setSegmentNum(segmentNum);
        this.setMeanLength(meanLength);
    }

    public double getMeanLength() {
        return meanLength;
    }

    public void setMeanLength(double meanLength) {
        this.meanLength = meanLength;
    }

    public double getSegmentNum() {
        return segmentNum;
    }

    public void setSegmentNum(double segmentNum) {
        this.segmentNum = segmentNum;
    }

    public static void readEdgesWeights(Graph graph, String filename, Population population) {
        MyLogger.important("Adding IBD features from " + filename);

        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(new File(filename)));
            //-- First line in the file holds the titles and is redundant
            //fileReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed openning IBD file " + filename, e);
        }

        String nextLine;
        try {
            while ((nextLine = fileReader.readLine()) != null) {
                StringTokenizer nextLineTokenizer = new StringTokenizer(nextLine, "\t");

                String iid1Str = nextLineTokenizer.nextToken();
                String iid2Str = nextLineTokenizer.nextToken();
                if (iid1Str.equals(iid2Str)) {
                    MyLogger.info(iid1Str + ": skipping self IBD match, suggests inbreeding");
                    continue;
                }
                String segmentNumStr = nextLineTokenizer.nextToken();
                String meanLengthStr = nextLineTokenizer.nextToken();

                if (population.getPerson(iid1Str) == null || population.getPerson(iid2Str) == null)
                    continue;
                Integer iid1 = population.getPerson(iid1Str).getId();
                Integer iid2 = population.getPerson(iid2Str).getId();
                //Integer iid1 = Integer.parseInt(iid1Str);
                //Integer iid2 = Integer.parseInt(iid2Str);
                Double segmentNum = Double.parseDouble(segmentNumStr);
                Double meanLength = Double.parseDouble(meanLengthStr);

                Vertex v1 = graph.getVertex(iid1);
                Vertex v2 = graph.getVertex(iid2);
                IBDFeaturesWeight w = new IBDFeaturesWeight(segmentNum, meanLength);
                MyLogger.debug(v1 + "," + v2 + " Read IBD features " + w);
                if (v1 != null && v2 != null)
                    graph.addEdge(new BaseEdge(v1, v2, w));
            }
            fileReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed processing IBD features file " + filename, e);
        }
    }

    /**
     * Calculate IBD features for a pair of genotypes
     *
     * @param g1
     * @param g2
     * @param addNoise
     * @param phased   - Count phased IBD segments.
     * @return
     */
    public static IBDFeaturesWeight calcIBDFeatureWeight(Genotype g1, Genotype g2, boolean addNoise, boolean phased) {
        List<HapRegion> IBD11 = g1.getHap1().getIBDSegments(g2.getHap1());
        List<HapRegion> IBD12 = g1.getHap1().getIBDSegments(g2.getHap2());
        List<HapRegion> IBD21 = g1.getHap2().getIBDSegments(g2.getHap1());
        List<HapRegion> IBD22 = g1.getHap2().getIBDSegments(g2.getHap2());

        List<HapRegion> IBD = new ArrayList<HapRegion>();
        IBD.addAll(IBD11);
        IBD.addAll(IBD12);
        IBD.addAll(IBD21);
        IBD.addAll(IBD22);

        if (phased)
            IBD = extendIBDSegments(IBD);
        else
            IBD = extendUnphasedIBDSegments(IBD);


        //MyLogger.debug("All IBD : " + IBD);
        double totalLength = 0;
        int segNum = 0;

        for (HapRegion segment : IBD) {
            totalLength += (segment.getEnd().getPosition() - segment.getStart().getPosition()) / 1000000;
            segNum++;
        }

        double meanLength = 0;
        if (segNum > 0)
            meanLength = totalLength / segNum;

        //Add noise if unrelated
        if (addNoise) {
            Random randomgenerator = new Random();
            if (segNum == 0) {
                segNum = (int) (10 * Math.abs(randomgenerator.nextGaussian()));
                if (segNum > 0)
                    meanLength = Math.max(1.0, Math.pow(15 - segNum + randomgenerator.nextGaussian(), 3) / 100 + 10 / segNum * randomgenerator.nextGaussian());
                //MyLogger.important("segNum="+ segNum + " meanLength="+ meanLength);
            }

            //Add parent-child noise
            if (segNum == 22 && meanLength > 130) {
                segNum += (int) Math.abs((randomgenerator.nextGaussian() * 10));
                meanLength = 40 + Math.pow(50 - segNum, 2) / 9 + randomgenerator.nextGaussian();
                //MyLogger.info("segNum="+ segNum + " meanLength="+ meanLength);
            }
        }
        //MyLogger.important("segNum = " + segNum +",meanLength=" + meanLength);
        return new IBDFeaturesWeight(segNum, meanLength);
        //return new IBDFeaturesWeight(segNum/18.0, meanLength/6);
    }

    /**
     * Connect consecutive IBD segments with different founder origin, into one IBD segment
     *
     * @param IBD
     * @return list of extended IBD segments
     */
    private static List<HapRegion> extendIBDSegments(List<HapRegion> IBD) {

        if (IBD.size() < 2) {
            return IBD;
        }
        List<HapRegion> extended = new ArrayList<HapRegion>();
        MyLogger.info("IBD before= " + IBD);
        Collections.sort(IBD);
        MyLogger.info("IBD after= " + IBD);

        Location currStart = IBD.get(0).getStart();
        ListIterator<HapRegion> iter = IBD.listIterator();

        while (iter.hasNext()) {
            HapRegion segment = iter.next();

            Location nextStart;
            if (iter.hasNext()) {
                nextStart = iter.next().getStart();
                iter.previous();
            } else
                nextStart = new Location(100, 1);//after genome end

            if (segment.getEnd().compareTo(nextStart) != 0) {
                extended.add(new HapRegion(currStart, segment.getEnd(), "ext"));
                MyLogger.info("Extended IBD= " + currStart + " , " + segment.getEnd());
                currStart = nextStart;
            }
            //else
            //MyLogger.important("Extend IBD segment until " +  segment.getEnd());
        }
        //MyLogger.debug("After extention: " + extended);
        return extended;
    }

    /**
     * Connect consecutive IBD segments with different founder origin, into one IBD segment
     *
     * @param IBD
     * @return list of extended IBD segments
     */
    private static List<HapRegion> extendUnphasedIBDSegments(List<HapRegion> IBD) {

        if (IBD.size() < 2) {
            return IBD;
        }
        List<HapRegion> extended = new ArrayList<HapRegion>();
        //	MyLogger.important("IBD before= " + IBD);
        Collections.sort(IBD);
        //MyLogger.important("IBD after= " + IBD);

        //MyLogger.debug("Before extention: " + IBD);
        Location currStart = IBD.get(0).getStart();
        ListIterator<HapRegion> iter = IBD.listIterator();

        while (iter.hasNext()) {
            HapRegion segment = iter.next();

            Location nextStart;
            if (iter.hasNext()) {
                nextStart = iter.next().getStart();
                iter.previous();
            } else
                nextStart = new Location(100, 1);//after genome end

            if (segment.getEnd().compareTo(nextStart) < 0) {
                extended.add(new HapRegion(currStart, segment.getEnd(), "ext"));
                //MyLogger.important("Extended IBD= " + currStart + " , "+ segment.getEnd());
                currStart = nextStart;
            }
            //else
            //MyLogger.important("Extend IBD segment until " +  segment.getEnd());
        }
        //MyLogger.debug("After extention: " + extended);
        return extended;
    }

    //public double distanceTo(IBDFeaturesWeight w) {
    //	return Math.pow(meanLength-w.meanLength,2)+ Math.pow(segmentNum-w.segmentNum,2);
    //}
    public String toString() {
        return String.format("NumberOfSegments: %.1f, MeanLength: %.1f", segmentNum, meanLength);
    }

    public DataPoint asDataPoint() {
        return new DataPoint(asVector(), null, null);
    }

    public Vec asVector() {
        //return new VecImpl(segmentNum,meanLength);
        return new VecImpl(meanLength * segmentNum / 30);
    }
}
