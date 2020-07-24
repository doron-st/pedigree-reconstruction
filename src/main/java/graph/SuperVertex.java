package graph;

import java.util.ArrayList;
import java.util.List;

public class SuperVertex implements VertexData {

    private static final long serialVersionUID = 1278390448375992012L;
    int id;
    List<Vertex> vertices;

    /**
     * Create a super-vertex with v as an inner vertex
     * Forget the edges of v (copy v's data, but not edges)
     *
     * @param v - first inner vertex
     */
    public SuperVertex(Vertex v) {
        vertices = new ArrayList<Vertex>();
        vertices.add(new BaseVertex(v.getData()));
        id = v.getVertexId();
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }

    public String toString() {
        return vertices.toString();
    }

    public Integer getId() {
        return id;
    }

    public int getRepresentativeID() {
        return getId();
    }

    public List<Vertex> getInnerVertices() {
        return vertices;
    }

}
