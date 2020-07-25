package graph;

import misc.MyLogger;
import relationship.RelationshipProbWeight;

import java.io.Serializable;
import java.util.*;


/**
 * Graph implementation
 * <p>
 * User: moshe
 */
public class Graph implements Serializable {
    private static final long serialVersionUID = -2913752016824176357L;
    private final Map<Integer, Vertex> vertexMap = new HashMap<Integer, Vertex>();

    public Graph(List<VertexData> verticesData) {
        for (VertexData d : verticesData) {
            BaseVertex vertex = new BaseVertex(d);
            this.addVertex(vertex);
            MyLogger.debug("Vertex " + d + " was added to graph");
        }
    }

    /**
     * Assemble a graph from vertices, use existing vertices, no cloning
     *
     * @param vertices
     */
    public Graph(Collection<Vertex> vertices) {
        for (Vertex v : vertices) {
            this.addVertex(v);
            MyLogger.info("Vertex " + v + " was added to graph");
        }
    }

    public void addVertex(Vertex v) {
        vertexMap.put(v.getVertexId(), v);
    }

    public Vertex getVertex(Integer vertexId) {
        return vertexMap.get(vertexId);
    }

    public void addEdge(Edge e) {
        Vertex v1 = e.getVertex1();
        Vertex v2 = e.getVertex2();
        if (!vertexMap.containsKey(v1.getVertexId())) {
            throw new RuntimeException("Vertex doesn't exists in vertices map " + v1);
        }
        if (!vertexMap.containsKey(v2.getVertexId())) {
            throw new RuntimeException("Vertex doesn't exists in vertices map " + v2);
        }

        v1.addEdge(e);
        MyLogger.debug("Graph::Add edge from " + v1.getVertexId() + " to " + e.getVertex2().getVertexId());
//        v2.addEdge(edge);
    }

    public Edge getEdge(Integer vid1, Integer vid2) {
        Vertex v1 = vertexMap.get(vid1);
        Vertex v2 = vertexMap.get(vid2);
        if (v1 == null || v2 == null) {
            return null;
        }
        return getEdge(v1, v2);
    }

    public Edge getUndirectedEdge(Integer vid1, Integer vid2) {
        Vertex v1 = vertexMap.get(vid1);
        Vertex v2 = vertexMap.get(vid2);
        if (v1 == null || v2 == null) {
            return null;
        }
        Edge e = getEdge(v1, v2);
        if (e == null) {
            e = getEdge(v2, v1);
            MyLogger.debug("no edge between" + vid1 + " and " + vid2);
        }
        return e;
    }

    public Edge getEdge(Vertex v1, Vertex v2) {
        for (Edge e : v1.getEdgeMap().values()) {
            if (e.getVertex2().getVertexId().equals(v2.getVertexId())) {
                return e;
            }
        }
        return null;
    }


    public List<Vertex> verticesFromDatas(List<? extends VertexData> vertexDatas) {
        List<Vertex> vertexes = new ArrayList<Vertex>();
        for (VertexData vertexData : vertexDatas) {

            Vertex vertex = getVertex(vertexData.getId());
            if (vertex == null) {
                throw new RuntimeException("Couldn't find a matching vertex to person " + vertexData);
            }
            vertexes.add(vertex);
        }
        return vertexes;
    }

    public List<? extends VertexData> vertexDataFromVertices(List<Vertex> vertices) {
        List<VertexData> vertexDatas = new ArrayList<VertexData>();
        for (Vertex vertex : vertices) {
            vertexDatas.add(vertex.getData());
        }
        return vertexDatas;
    }

    public void removeVertex(Vertex v) {
        List<Vertex> existingVertices = new ArrayList<Vertex>(getVertexMap().values());
        for (Vertex u : existingVertices) {
            Edge e;
            if ((e = getEdge(u, v)) != null)
                u.getEdgeMap().values().remove(e);
            if ((e = getEdge(v, u)) != null)
                v.getEdgeMap().values().remove(e);

        }
        vertexMap.remove(v.getVertexId());
    }

    public Map<Integer, Vertex> getVertexMap() {
        return vertexMap;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "vertexMap.size=" + vertexMap.size() +
                " vertexMap=" + vertexMap.values() +
                '}';
    }

    /**
     * @param v1 - vertex
     * @param v2 - vertex
     * @return true if the max prob of relatedness is not "unrelated"
     */
	/*public Boolean isRelated(Vertex v1,Vertex v2,int degree) {
		reconstruct.Edge e1 =  this.getEdge(v1,v2);
		reconstruct.Edge e2 =  this.getEdge(v2,v1);
		boolean isRelated = false;

		if(e1==null && e2==null)
			return false;
		if(e1!=null)
			isRelated =  !((RelationshipProbWeight)e1.getWeight()).isMaxProbCategory(NOT_RELATED);
		if(e2!=null)
			isRelated =  !((RelationshipProbWeight)e2.getWeight()).isMaxProbCategory(NOT_RELATED);
		
		if(isRelated && degree==2){
			RelationshipProbWeight w =null;
			if(e1!=null){
				w = (RelationshipProbWeight) e1.getWeight();
			}
			else{
				w = (RelationshipProbWeight) e2.getWeight();
			}
			if(w.getDegreeProb(1) + w.getDegreeProb(2) <0.5){
				isRelated=false;
			}
		}
		return isRelated;
	}
	*/
    public RelationshipProbWeight getWeight(Vertex v1, Vertex v2) {
        Edge e = v1.getEdgeTo(v2.getVertexId());
        if (e != null)
            return (RelationshipProbWeight) e.getWeight();
        e = v2.getEdgeTo(v1.getVertexId());
        if (e != null)
            return RelationshipProbWeight.switchWeightsDirection((RelationshipProbWeight) e.getWeight());
        return null;
    }

    public List<Vertex> getVertices() {
        Iterator<Vertex> iter = vertexMap.values().iterator();
        List<Vertex> vertices = new ArrayList<Vertex>();
        while (iter.hasNext())
            vertices.add(iter.next());
        return vertices;
    }

    public boolean hasVertex(int id) {
        return vertexMap.keySet().contains(id);
    }

    public int getNumOfEdges() {
        int numOfEdges = 0;
        for (Vertex v : getVertices()) {
            numOfEdges += v.getEdgeMap().size();
        }
        return numOfEdges;
    }

    public List<VertexData> getVertexDataList() {
        List<VertexData> l = new ArrayList<VertexData>();
        for (Vertex v : getVertices()) {
            l.add(v.getData());
        }
        return l;
    }

    public void clearAllEdges() {
        for (Vertex v : getVertices()) {
            v.clearEdges();
        }
    }

    public void setVertexID(Vertex v, Integer id) {
        vertexMap.put(id, v);
    }
}
