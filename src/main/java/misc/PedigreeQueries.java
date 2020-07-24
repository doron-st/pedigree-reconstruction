package misc;

import graph.Edge;
import graph.Graph;
import graph.MyLogger;
import graph.Vertex;

public class PedigreeQueries {	

	/**
	 *
	 * @param v1 - vertex
	 * @param v2 - vertex
	 * @return  true if the max prob of relatedness is not "unrelated"
	 */
	public static Boolean isRelated(Graph graph, Vertex v1,Vertex v2,int degree) {
		boolean isRelated = false;

		RelationshipProbWeight w = getWeight(graph,v1, v2);
		
		if(w==null)
			return false;
		
		isRelated =  !w.isMaxProbCategory("notRelated");

		if(isRelated && degree==2){
			if(w.getDegreeProb(1) + w.getDegreeProb(2) <0.5){
				isRelated=false;
			}
		}
		return isRelated;
	}
	
	
	public static RelationshipProbWeight getWeight(Graph graph,Vertex v1, Vertex v2) {
		Edge e = v1.getEdgeTo(v2.getVertexId());
		if(e!=null)
			return (RelationshipProbWeight) e.getWeight();
		e= v2.getEdgeTo(v1.getVertexId());
		if(e!= null)
			return RelationshipProbWeight.switchWeightsDirection( (RelationshipProbWeight)e.getWeight());
		return  null;
	}

	public static boolean isVertexNephewOfSiblings(Graph graph, Vertex v, NucFamily nucFamily) {
		int maxNeph = 0;

		for(Vertex u : graph.verticesFromDatas(nucFamily.siblings)){
			RelationshipProbWeight w = getWeight(graph,u, v);
			if(w != null && w.isMaxProbCategory("fullNephew")){
				maxNeph++;
			}
		}
		if(maxNeph/nucFamily.siblings.size()>0.5){
			MyLogger.info("For siblings " + nucFamily.siblings + ", Ignoring nephew " + v.getVertexId() + " , (probably, fullSib is not yet detected)");
			return true;
		}
		return false;
	}

	public static boolean isVertexSibOfSiblings(Graph graph ,Vertex v, NucFamily nucFamily) {
		int maxSib = 0;
		if(v.getVertexId()==4){
			v.getVertexId();
		}
		for(Vertex u : graph.verticesFromDatas(nucFamily.siblings)){
			RelationshipProbWeight w = getWeight(graph,u, v);
			if(w!= null && w.isMaxProbCategory("fullSib")){
				maxSib++;
			}
		}
		if(maxSib/nucFamily.siblings.size()>0.5){
			MyLogger.info("For siblings " + nucFamily.siblings + ", Ignoring sibling " + v.getVertexId());
			return true;
		}
		return false;
	}

	public static boolean isVertexChildOfSiblings(Graph graph,Vertex v, NucFamily nucFamily) {
		Person his = (Person) v.getData();
		for(Vertex u : graph.verticesFromDatas(nucFamily.siblings)){
			Person sib = (Person) u.getData();
			if(his.getFamily().father == sib || his.getFamily().mother == sib){
				return true;
			}
		}
		return false;
	}

}
