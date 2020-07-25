package graph;


import java.io.Serializable;
import java.util.Map;

/**
 * Implementation of Vertex in a graph
 * <p>
 * User: moshe
 */
public interface Vertex extends Serializable {

    // Each vertex should have an ID
    Integer getVertexId();

    // Edges from this vertex to another vertex.
    // map of the other vertex id to the edge itself
    Map<Integer, Edge> getEdgeMap();

    // Adding an edge from this vertex. The edge should already point to the other vertex
    void addEdge(Edge edge);

    // The data of this vertex
    VertexData getData();

    boolean hasEdgeTo(int vertexID);

    Edge getEdgeTo(int vertexID);

    void removeEdgeTo(Vertex v);

}
