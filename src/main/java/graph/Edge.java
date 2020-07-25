package graph;

import java.io.Serializable;


/**
 * Edge connect 2 vertices in a graph
 * <p>
 * User: moshe
 */
public interface Edge extends Serializable {
    Vertex getVertex1();

    Vertex getVertex2();

    Weight getWeight();

    void setWeight(Weight w);

}
