package prepare;

import graph.BaseVertex;
import graph.MyLogger;
import graph.Vertex;
import graph.VertexData;

import java.util.ArrayList;
import java.util.List;

public class SuperVertex implements VertexData{
	
	private static final long serialVersionUID = 1278390448375992012L;
	int id;
	List<Vertex> vertices;

	/**
	 * Create a supervertex with v as an inner vertex
	 * Forget the edges of v (copy v's data, but not edges)
	 * @param v - first inner vertex 
	 */
	public SuperVertex(Vertex v){
		vertices = new ArrayList<Vertex>();
		vertices.add(new BaseVertex(v.getData()));
		id=v.getVertexId();
	}
	public void addVertex(Vertex v){
		vertices.add(v);
	}
	public String toString(){
		return vertices.toString();
	}
	@Override
	public Integer getId() {
		return id;
	}
	public int getRepresentativeID(){
		return getId();
	}
	public List<Vertex> getInnerVertices() {
		return vertices;
	}
	public void removeVertex(Vertex v) {
		int vid=v.getVertexId();
		vertices.remove(v);
		if(vid==id){
			if(!vertices.isEmpty()){
				id=vertices.get(0).getVertexId();
				MyLogger.warn("id="+id);
			}
			else{
				id=-1;
				MyLogger.warn("id=-1");
			}
		}
	}
}
