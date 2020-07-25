package prepare.relationship;

import prepare.graph.Weight;
import jsat.classifiers.DataPoint;
import jsat.linear.Vec;

import javax.management.RuntimeErrorException;
import java.util.HashMap;
import java.util.Map;

import static prepare.relationship.Relationship.*;

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
    private final Map<Relationship, Double> map;

    public RelationshipProbWeight() {
        map = new HashMap<>();
        for(Relationship relationship : Relationship.values())
            map.put(relationship, 0.0);
    }


    public Double getProb(Relationship categoryName) {
        if (map.containsKey(categoryName))
            return map.get(categoryName);
        else
            throw new RuntimeErrorException(null, "Non existent key: " + categoryName + ", in RelationshipProbWeight::getProb");
    }

    public void setProb(Relationship relationship, Double value) {
        if (map.containsKey(relationship)) {
            map.remove(relationship);
            map.put(relationship, value);
        } else
            throw new RuntimeErrorException(null, "Non existent key: " + relationship + ", in RelationshipProbWeight::setProb");
    }

    public void makeDeterministicChoice(Relationship selected) {
        for (Relationship cat : map.keySet()) {
            setProb(cat, 0.0);
        }
        setProb(selected, 1.0);
    }

    @Override
    public String toString() {
        //return map.toString();
        return "{sib=" + getProb(Relationship.FULL_SIB) + " parent=" + getProb(PARENT) + " child=" + getProb(Relationship.CHILD) + " notRelated=" + getProb(Relationship.NOT_RELATED) + "}";
    }

    public boolean isMaxProbCategory(Relationship categoryName) {
        if (map.containsKey(categoryName)) {
            return getMaxProbCategory().equals(categoryName);
        } else
            throw new RuntimeErrorException(null, "Non existant key: " + categoryName + ", in RelationshipProbWeight::setProb");
    }

    public Relationship getMaxProbCategory() {
        Double max = -1.0;
        Relationship maxCategory = null;
        for (Relationship category : map.keySet()) {
            if (max < map.get(category)) {
                max = map.get(category);
                maxCategory = category;
            }
        }
        return maxCategory;
    }


    public static RelationshipProbWeight switchWeightsDirection(RelationshipProbWeight weight) {
        RelationshipProbWeight switched = new RelationshipProbWeight();
        switched.setProb(FULL_SIB, weight.getProb(FULL_SIB));
        switched.setProb(HALF_SIB, weight.getProb(HALF_SIB));
        switched.setProb(FULL_COUSIN, weight.getProb(FULL_COUSIN));
        switched.setProb(HALF_COUSIN, weight.getProb(HALF_COUSIN));
        switched.setProb(FULL_2_COUSIN, weight.getProb(FULL_2_COUSIN));
        switched.setProb(HALF_2_COUSIN, weight.getProb(HALF_2_COUSIN));
        switched.setProb(FULL_UNCLE, weight.getProb(FULL_NEPHEW));
        switched.setProb(HALF_UNCLE, weight.getProb(HALF_NEPHEW));
        switched.setProb(FULL_NEPHEW, weight.getProb(FULL_UNCLE));
        switched.setProb(HALF_NEPHEW, weight.getProb(HALF_UNCLE));
        switched.setProb(FULL_2_UNCLE, weight.getProb(FULL_2_NEPHEW));
        switched.setProb(HALF_2_UNCLE, weight.getProb(HALF_2_NEPHEW));
        switched.setProb(FULL_2_NEPHEW, weight.getProb(FULL_2_UNCLE));
        switched.setProb(HALF_2_NEPHEW, weight.getProb(HALF_2_UNCLE));
        switched.setProb(PARENT, weight.getProb(CHILD));
        switched.setProb(CHILD, weight.getProb(PARENT));
        switched.setProb(NOT_RELATED, weight.getProb(NOT_RELATED));
        return switched;
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
