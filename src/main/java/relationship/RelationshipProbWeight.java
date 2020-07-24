package relationship;

import graph.Weight;
import jsat.classifiers.DataPoint;
import jsat.linear.Vec;

import javax.management.RuntimeErrorException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, Double> map;

    public RelationshipProbWeight() {
        map = new HashMap<String, Double>();
        map.put("fullSib", 0.0);
        map.put("halfSib", 0.0);
        map.put("child", 0.0);
        map.put("parent", 0.0);
        map.put("fullUncle", 0.0);
        map.put("fullNephew", 0.0);
        map.put("fullCousin", 0.0);
        map.put("halfUncle", 0.0);
        map.put("halfNephew", 0.0);
        map.put("halfCousin", 0.0);
        map.put("full2Uncle", 0.0);
        map.put("full2Nephew", 0.0);
        map.put("full2Cousin", 0.0);
        map.put("half2Uncle", 0.0);
        map.put("half2Nephew", 0.0);
        map.put("half2Cousin", 0.0);
        map.put("notRelated", 0.0);
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

    public boolean isMaxProbCategory(String categoryName) {
        if (map.containsKey(categoryName)) {
            return getMaxProbCategory().equals(categoryName);
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

    public DataPoint asDataPoint() {
        // TODO Auto-generated method stub
        return null;
    }

    public Vec asVector() {
        // TODO Auto-generated method stub
        return null;
    }
}
