package prepare.graph;

import prepare.misc.MyLogger;

import java.util.HashMap;
import java.util.Map;


/**
 * User: moshe
 */
public class BaseVertex implements Vertex {
    private static final long serialVersionUID = 1659765997375354120L;

    private VertexData data;
    private Map<Integer, Edge> edgeMap = new HashMap<>();

    public BaseVertex(VertexData data) {
        this.data = data;
    }

    public Integer getVertexId() {
        return data.getId();
    }

    public void addEdge(Edge edge) {
        if (edge.getVertex1().getVertexId().equals(this.getVertexId()))
            edgeMap.put(edge.getVertex2().getVertexId(), edge);
        else
            throw new RuntimeException("Cannot add an edge that doesn't include this vertex. edge=" + edge);

        if (edge.getVertex1() == edge.getVertex2())
            throw new RuntimeException("Trying to add a loop edge=" + edge.getVertex1().getVertexId() + "," + edge.getVertex2().getVertexId());

    }

    public VertexData getData() {
        return data;
    }

    public Map<Integer, Edge> getEdgeMap() {
        return edgeMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseVertex vertex = (BaseVertex) o;
        return getVertexId() != null ? getVertexId().equals(vertex.getVertexId()) : vertex.getVertexId() == null;
    }

    @Override
    public int hashCode() {
        return getVertexId() != null ? getVertexId().hashCode() : 0;
    }

    @Override
    public String toString() {
        //return "BaseVertex{" + "data=" + data + "edgeMap.size()=" + edgeMap.size() +'}';
        return data.toString();
    }

    public boolean hasEdgeTo(int vertexID) {
        Map<Integer, Edge> edges = this.getEdgeMap();
        return edges.containsKey(vertexID);
    }

    public Edge getEdgeTo(int vertexID) {
        Map<Integer, Edge> edges = this.getEdgeMap();
        return edges.get(vertexID);
    }

    public void removeEdgeTo(Vertex v) {
        MyLogger.info("removing " + v.getVertexId() + " from " + this.getVertexId() + " edges");
        MyLogger.debug("num of edges from " + this.getVertexId() + " before = " + edgeMap.size());
        this.edgeMap.remove(v.getVertexId());
        MyLogger.debug("num of edges from " + this.getVertexId() + " after = " + edgeMap.size());
    }

}
