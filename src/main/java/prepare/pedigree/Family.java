package prepare.pedigree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: moshe
 * Date: 3/23/13
 * Time: 8:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class Family implements Serializable {
    private static final long serialVersionUID = 1289828664952209624L;
    public Person mother;
    public Person father;
    public List<Person> siblings = new ArrayList<>();
    public Double motherProbability = 0.0;
    public Double fatherProbability = 0.0;


    @Override
    public String toString() {
        return "mother=" + mother + " ,father=" + father;
    }
}
