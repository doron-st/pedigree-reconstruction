package graph;

import java.io.Serializable;


/**
 * Edge connect 2 vertices in a graph
 *
 * User: moshe
 */
public interface Edge extends Serializable{
    public Vertex getVertex1();

    public Vertex getVertex2();

    public Weight getWeight();
    
    public void setWeight(Weight w);

	public boolean isWeightHandled();

	public void setWeightHandled(boolean b);

}
