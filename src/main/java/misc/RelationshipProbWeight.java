package misc;

import graph.*;
import jsat.classifiers.DataPoint;
import jsat.linear.Vec;

import javax.management.RuntimeErrorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: moshe
 * Date: 3/2/13
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelationshipProbWeight implements Weight {
    private static final long serialVersionUID = -4721584804671645993L;
    // ID1 ID2 p(sib) p(parent) p(child) p(uncle) p(nephew) p(half_sib) p(cousin) p(not-related)
    private Map<String, Double> map;

    public RelationshipProbWeight() {
        map = new HashMap<String, Double>();
        map.put("fullSib", new Double(0));
        map.put("halfSib", new Double(0));
        map.put("child", new Double(0));
        map.put("parent", new Double(0));
        map.put("fullUncle", new Double(0));
        map.put("fullNephew", new Double(0));
        map.put("fullCousin", new Double(0));
        map.put("halfUncle", new Double(0));
        map.put("halfNephew", new Double(0));
        map.put("halfCousin", new Double(0));
        map.put("full2Uncle", new Double(0));
        map.put("full2Nephew", new Double(0));
        map.put("full2Cousin", new Double(0));
        map.put("half2Uncle", new Double(0));
        map.put("half2Nephew", new Double(0));
        map.put("half2Cousin", new Double(0));
        map.put("notRelated", new Double(1));
    }

    public void normalizedWith(Double factor) {
        for (String category : map.keySet()) {
            map.put(category, map.get(category) / factor);
        }
    }

    public Double getSumOfProbs() {
        Double sum = 0.0;
        for (String category : map.keySet()) {
            sum += map.get(category);
        }
        return sum;
    }


    public Double getProb(String categoryName) {
        if (map.containsKey(categoryName))
            return map.get(categoryName);
        else
            throw new RuntimeErrorException(null, "Non existant key: " + categoryName + ", in RelationshipProbWeight::getProb");
    }

    public void setProb(String categoryName, Double value) {
        if (map.containsKey(categoryName)) {
            map.remove(categoryName);
            map.put(categoryName, value);
        } else
            throw new RuntimeErrorException(null, "Non existant key: " + categoryName + ", in RelationshipProbWeight::setProb");
    }

    public void makeDeteministicChoice(String categoryName) {
        String[] keys = Arrays.asList(map.keySet().toArray()).toArray(new String[map.keySet().toArray().length]);
        for (String cat : keys) {
            setProb(cat, 0.0);
        }
        setProb(categoryName, 1.0);
    }

    @Override
    public String toString() {
        //return map.toString();
        return "{sib=" + getProb("fullSib") + " parent=" + getProb("parent") + " child=" + getProb("child") + " notRelated=" + getProb("notRelated") + "}";
    }

    public boolean isMaxProbCategoryWithoutNotRelated(String categoryName) {
        if (map.containsKey(categoryName)) {
            if (getMaxProbCategoryWithoutNotRelated() == categoryName)
                return true;
            else
                return false;
        } else
            throw new RuntimeErrorException(null, "Non existant key: " + categoryName + ", in RelationshipProbWeight::setProb");
    }

    public boolean isMaxProbCategory(String categoryName) {
        if (map.containsKey(categoryName)) {
            if (getMaxProbCategory().equals(categoryName))
                return true;
            else
                return false;
        } else
            throw new RuntimeErrorException(null, "Non existant key: " + categoryName + ", in RelationshipProbWeight::setProb");
    }

    public String getMaxProbCategory() {
        Double max = -1.0;
        String maxCategory = null;
        for (String category : map.keySet()) {
            if (max < map.get(category)) {
                max = map.get(category);
                maxCategory = category;
            }
        }
        return maxCategory;
    }

    public String getMaxProbCategoryWithoutNotRelated() {
        Double max = -1.0;
        String maxCategory = null;
        for (String category : map.keySet()) {
            if (max > map.get(category) && !category.equals("notRelated")) {
                max = map.get(category);
                maxCategory = category;
            }
        }
        return maxCategory;
    }


    public static void readRelationshipProbEdges(Graph graph, String filename) {
        MyLogger.debug("Adding probabilities from " + filename);

        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(new File(filename)));
            //-- First line in the file holds the titles and is redundant
            //fileReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed openning posteriors file " + filename, e);
        }

        String nextLine;
        try {
            while ((nextLine = fileReader.readLine()) != null) {
                StringTokenizer nextLineTokenizer = new StringTokenizer(nextLine, " ");

                String iid1Str = nextLineTokenizer.nextToken();
                String iid2Str = nextLineTokenizer.nextToken();
                String fullSibStr = nextLineTokenizer.nextToken();
                String childParentStr = nextLineTokenizer.nextToken();
                String parentChildStr = nextLineTokenizer.nextToken();
                String uncleStr = nextLineTokenizer.nextToken();
                String nephewStr = nextLineTokenizer.nextToken();
                String halfSibStr = nextLineTokenizer.nextToken();
                String cousinStr = nextLineTokenizer.nextToken();

                String halfUncleStr = nextLineTokenizer.nextToken();
                String halfNephewStr = nextLineTokenizer.nextToken();
                String halfCousinStr = nextLineTokenizer.nextToken();
                String secondUncleStr = nextLineTokenizer.nextToken();
                String secondNephewStr = nextLineTokenizer.nextToken();
                String secondCousinStr = nextLineTokenizer.nextToken();
                String halfSecondUncleStr = nextLineTokenizer.nextToken();
                String halfSecondNephewStr = nextLineTokenizer.nextToken();
                String halfSecondCousinStr = nextLineTokenizer.nextToken();


                String notRelatedStr = nextLineTokenizer.nextToken();

                Integer iid1 = Integer.parseInt(iid1Str);
                Integer iid2 = Integer.parseInt(iid2Str);
                Double fullSibProb = Double.parseDouble(fullSibStr);
                Double childParentProb = Double.parseDouble(childParentStr);
                Double parentChildProb = Double.parseDouble(parentChildStr);
                Double uncleProb = Double.parseDouble(uncleStr);
                Double nephewProb = Double.parseDouble(nephewStr);
                Double halfSibProb = Double.parseDouble(halfSibStr);
                Double cousinProb = Double.parseDouble(cousinStr);
                Double notRelatedProb = Double.parseDouble(notRelatedStr);

                Double halfUncleProb = Double.parseDouble(halfUncleStr);
                Double halfNephewProb = Double.parseDouble(halfNephewStr);
                Double halfCousinProb = Double.parseDouble(halfCousinStr);
                Double secondUncleProb = Double.parseDouble(secondUncleStr);
                Double secondNephewProb = Double.parseDouble(secondNephewStr);
                Double secondCousinProb = Double.parseDouble(secondCousinStr);
                Double halfSecondUncleProb = Double.parseDouble(halfSecondUncleStr);
                Double halfSecondNephewProb = Double.parseDouble(halfSecondNephewStr);
                Double halfSecondCousinProb = Double.parseDouble(halfSecondCousinStr);

                try {
                    Edge edge1 = addEdgeIfNeeded(graph, iid1, iid2);
                    //If edge appears twice, use the first instance
                    if (edge1 != null) {
                        RelationshipProbWeight weight = (RelationshipProbWeight) edge1.getWeight();
                        weight.setProb("fullSib", fullSibProb);
                        weight.setProb("parent", childParentProb);
                        weight.setProb("child", parentChildProb);
                        weight.setProb("fullUncle", uncleProb);
                        weight.setProb("fullNephew", nephewProb);
                        weight.setProb("halfSib", halfSibProb);
                        weight.setProb("fullCousin", cousinProb);

                        weight.setProb("halfUncle", halfUncleProb);
                        weight.setProb("halfNephew", halfNephewProb);
                        weight.setProb("halfCousin", halfCousinProb);
                        weight.setProb("full2Cousin", secondCousinProb);
                        weight.setProb("full2Uncle", secondUncleProb);
                        weight.setProb("full2Nephew", secondNephewProb);
                        weight.setProb("half2Cousin", halfSecondCousinProb);
                        weight.setProb("half2Uncle", halfSecondUncleProb);
                        weight.setProb("half2Nephew", halfSecondNephewProb);

                        weight.setProb("notRelated", notRelatedProb);

                        MyLogger.debug("(" + iid1 + "," + iid2 + ") =" + weight.toString());
                        MyLogger.info("(" + iid1 + "," + iid2 + ") = " + weight.getMaxProbCategory() + " " + weight.getProb(weight.getMaxProbCategory()));
                    }
                } catch (Exception e) {
                    fileReader.close();
                    MyLogger.error(String.format("Error adding edge for %d and %d", iid1, iid2));
                }
            }
            fileReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed processing relationship probabilities file " + filename, e);
        }

    }

    public static RelationshipProbWeight switchWeightsDirection(RelationshipProbWeight weight) {
        RelationshipProbWeight switched = new RelationshipProbWeight();
        switched.setProb("fullSib", weight.getProb("fullSib"));
        switched.setProb("halfSib", weight.getProb("halfSib"));
        switched.setProb("fullCousin", weight.getProb("fullCousin"));
        switched.setProb("halfCousin", weight.getProb("halfCousin"));
        switched.setProb("full2Cousin", weight.getProb("full2Cousin"));
        switched.setProb("half2Cousin", weight.getProb("half2Cousin"));
        switched.setProb("fullUncle", weight.getProb("fullNephew"));
        switched.setProb("halfUncle", weight.getProb("halfNephew"));
        switched.setProb("fullNephew", weight.getProb("fullUncle"));
        switched.setProb("halfNephew", weight.getProb("halfUncle"));
        switched.setProb("full2Uncle", weight.getProb("full2Nephew"));
        switched.setProb("half2Uncle", weight.getProb("half2Nephew"));
        switched.setProb("full2Nephew", weight.getProb("full2Uncle"));
        switched.setProb("half2Nephew", weight.getProb("half2Uncle"));
        switched.setProb("parent", weight.getProb("child"));
        switched.setProb("child", weight.getProb("parent"));
        switched.setProb("notRelated", weight.getProb("notRelated"));
        return switched;
    }

    private static Edge addEdgeIfNeeded(Graph graph, int iid1, int iid2) {
        Edge edge = graph.getEdge(iid1, iid2);
        if (edge == null && iid1 != iid2) {
            RelationshipProbWeight w = new RelationshipProbWeight();
            Vertex v1 = graph.getVertex(iid1);
            Vertex v2 = graph.getVertex(iid2);
            edge = new BaseEdge(v1, v2, w);
            graph.addEdge(edge);
            return edge;
        }
        return null;
    }

    public int compareTo(RelationshipProbWeight backWeight) {
        for (String relation : this.map.keySet()) {
            double myProb = getProb(relation);
            double otherProb = backWeight.getProb(relation);
            if (Math.abs(myProb - otherProb) > 0.0001) {
                return 1;
            }
        }
        return 0;
    }

    public void averageWith(RelationshipProbWeight backWeight) {
        this.setProb("child", (this.getProb("child") + backWeight.getProb("child")) / 2);
        this.setProb("parent", (this.getProb("parent") + backWeight.getProb("parent")) / 2);
        this.setProb("fullSib", (this.getProb("fullSib") + backWeight.getProb("fullSib")) / 2);
        this.setProb("halfSib", (this.getProb("halfSib") + backWeight.getProb("halfSib")) / 2);
        this.setProb("fullUncle", (this.getProb("fullUncle") + backWeight.getProb("fullUncle")) / 2);
        this.setProb("fullNephew", (this.getProb("fullNephew") + backWeight.getProb("fullNephew")) / 2);
        this.setProb("fullCousin", (this.getProb("fullCousin") + backWeight.getProb("fullCousin")) / 2);
        this.setProb("halfUncle", (this.getProb("halfUncle") + backWeight.getProb("halfUncle")) / 2);
        this.setProb("halfNephew", (this.getProb("halfNephew") + backWeight.getProb("halfNephew")) / 2);
        this.setProb("halfCousin", (this.getProb("halfCousin") + backWeight.getProb("halfCousin")) / 2);
        this.setProb("full2Uncle", (this.getProb("full2Uncle") + backWeight.getProb("full2Uncle")) / 2);
        this.setProb("full2Nephew", (this.getProb("full2Nephew") + backWeight.getProb("full2Nephew")) / 2);
        this.setProb("full2Cousin", (this.getProb("full2Cousin") + backWeight.getProb("full2Cousin")) / 2);
        this.setProb("half2Uncle", (this.getProb("half2Uncle") + backWeight.getProb("half2Uncle")) / 2);
        this.setProb("half2Nephew", (this.getProb("half2Nephew") + backWeight.getProb("half2Nephew")) / 2);
        this.setProb("half2Cousin", (this.getProb("half2Cousin") + backWeight.getProb("half2Cousin")) / 2);
        this.setProb("notRelated", (this.getProb("notRelated") + backWeight.getProb("notRelated")) / 2);
    }

    public double getDegreeProb(int i) {
        if (i == 1) {
            return getProb("fullSib") + getProb("halfSib") + getProb("parent") + getProb("child");
        }
        if (i == 2) {
            return getProb("fullCousin") + getProb("halfCousin") + getProb("fullUncle") + getProb("halfUncle")
                    + getProb("fullNephew") + getProb("halfNephew");
        }
        if (i > 2) {
            return getSumOfProbs();
        }
        return 0;
    }

    public double getFullRelationProb() {
        return getProb("fullSib") + getProb("fullCousin") + getProb("fullNephew") + getProb("fullUncle") + getProb("full2Cousin") + getProb("full2Nephew") + getProb("full2Uncle");
    }

    public double getHalfRelationProb() {
        return getProb("halfSib") + getProb("halfCousin") + getProb("halfNephew") + getProb("halfUncle") + getProb("full2Cousin") + getProb("half2Nephew") + getProb("half2Uncle");
    }

    @Override
    public DataPoint asDataPoint() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vec asVector() {
        // TODO Auto-generated method stub
        return null;
    }
}
