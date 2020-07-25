package graph;


/**
 * implementation of {@link Edge}
 * <p>
 * User: moshe
 */
public class BaseEdge implements Edge {
    private static final long serialVersionUID = -5577391332683063930L;
    private final Vertex vertex1;
    private final Vertex vertex2;
    private Weight weight;
    private boolean weightHandled = false;

    public BaseEdge(Vertex vertex1, Vertex vertex2, Weight weight) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.weight = weight;
    }

    public Vertex getVertex1() {
        return vertex1;
    }

    public Vertex getVertex2() {
        return vertex2;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

}
