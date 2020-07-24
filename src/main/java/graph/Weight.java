package graph;

import java.io.Serializable;

import jsat.classifiers.DataPoint;
import jsat.linear.Vec;

/**
 * Created with IntelliJ IDEA.
 * User: moshe
 * Date: 3/2/13
 * Time: 8:21 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Weight extends Serializable {

	DataPoint asDataPoint();

	Vec asVector();
}
