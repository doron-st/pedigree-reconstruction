package graph;


import java.io.Serializable;
import java.util.Map;

/**
 * Implemetation of Vertex in a graph
 *
 * User: moshe
 */
public interface Vertex extends Serializable {

    // Each vertex should have an ID
    public Integer getVertexId();

    // Edges from this vertex to another vertex.
    // map of the other vertex id to the edge itself
    public Map<Integer, Edge> getEdgeMap();

    // Adding an edge from this vertex. The edge should already point to the other vertex
    public void addEdge(Edge edge);

    // The data of this vertex
    public VertexData getData();
    
    public boolean hasEdgeTo(int vertexID);

    public Edge getEdgeTo(int vertexID);

	public void removeEdgeTo(Vertex v);

	public void setData(VertexData data);

	public void clearEdges();

}
