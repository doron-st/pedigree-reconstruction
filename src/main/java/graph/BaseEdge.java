package graph;


/**
 * implementation of {@link Edge}
 *
 * User: moshe
 */
public class BaseEdge implements Edge {
	private static final long serialVersionUID = -5577391332683063930L;
	private Vertex vertex1 = null;
    private Vertex vertex2 = null;
    private Weight weight = null;
    private boolean weightHandled=false;

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

	@Override
	public void setWeight(Weight weight) {
		this.weight = weight;
	}

	@Override
	public boolean isWeightHandled() {
		return weightHandled;
	}
	@Override
	public void setWeightHandled(boolean state) {
		weightHandled=state;
	}

}
