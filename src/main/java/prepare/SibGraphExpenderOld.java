package prepare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import misc.RelationshipProbWeight;


import graph.BaseEdge;
import graph.Edge;
import graph.Graph;
import graph.MyLogger;
import graph.Vertex;

public class SibGraphExpenderOld {

	private Graph contractedGraph;
	private Contraction contraction;
	private Map<Vertex,Boolean> vacancyMap = new HashMap<Vertex,Boolean>();
	private List<Vertex> eList;//list of expended vertices
	private int expendedNum=0;

	public SibGraphExpenderOld(Graph contractedGraph,Contraction cont){
		this.contractedGraph = contractedGraph;
		contraction = cont;
		eList = new ArrayList<Vertex>();
		int numOfCont=0;
		for(Vertex sv : contractedGraph.getVertices()){
			for(Vertex v : ((SuperVertex) sv.getData()).getInnerVertices()){
				vacancyMap.put(v, true);
			}
			if(((SuperVertex) sv.getData()).getInnerVertices().size()>1)
				numOfCont++;
		}
		MyLogger.important("RelationGraphExpender::num of contracted vertices=" + numOfCont);
	}
	/**
	 * Run Expension algorithm:
	 * Partitions ambigous relationships into father/mother relationships
	 * @return - The expended relationships graph
	 */
	public Graph run(){
		for(Vertex sv : contractedGraph.getVertices()){
			Collection<Edge> edges =  new ArrayList<Edge>(sv.getEdgeMap().values());
			for(Edge se : edges){
				//if(((RelationshipProbWeight) se.getWeight()).isMaxProbCategory(category))
				RelationshipProbWeight w = (RelationshipProbWeight) se.getWeight();
				if(w.isMaxProbCategory("fullSib"))
					expandOnEdge(sv, se);
			}
		}
		MyLogger.important("RelationGraphExpender::expended " +  expendedNum + " vertices");
		//Add all unexpended vertices to list (single children)
		List<Vertex> uList = new ArrayList<Vertex>();//list of unexpended vertices
		for(Vertex sv : contractedGraph.getVertices()){
			for(Vertex v : ((SuperVertex) sv.getData()).getInnerVertices())
				if(!eList.contains(v)){
					eList.add(v);
					MyLogger.important(v + " was not expended!");
					if(!uList.contains(sv))
						uList.add(sv);
				}
		}
		contraction.addUnexpendedList(uList);

		return new Graph(eList);
	}


	private void expandOnEdge(Vertex sv, Edge se) {
		MyLogger.important("Expending sv " + sv + " on edge to" + se.getVertex2());
		SuperVertex sv1 = (SuperVertex) sv.getData();
		SuperVertex sv2 = (SuperVertex) se.getVertex2().getData();

		//If both supervertices have one inner vertex
		if(sv1.getInnerVertices().size()==1 && sv2.getInnerVertices().size()==1){
			MyLogger.important("expanding pseudo super-vertices: " + sv1.getId() + "," + sv2.getId());
			assignSuperEdge(se, sv1.getInnerVertices().get(0), sv2.getInnerVertices().get(0));
			return;
		}
		//If this supervertex has one inner vertex, but the other has more
		else if(sv1.getInnerVertices().size()==1){
			return; //let other vertex do the work
		}

		for(Vertex v : ((SuperVertex) sv.getData()).getInnerVertices()){
			if(vacancyMap.get(v)){
				for(Vertex u : ((SuperVertex) se.getVertex2().getData()).getInnerVertices()){
					//v,u are vacant
					if(vacancyMap.get(u)){
						MyLogger.important("Found vacant pair: " + v +","+u);
						assignSuperEdge(se, v, u);
						return;
					}
					else{
						MyLogger.important(v + " is vacant, but " + u + " is not");
						//	trioTest(v,se);
					}
				}
			}
			else{//v is occupied with a previous edge
				if(trioTest(v,se))
					return;
			}
		}
	}

	private boolean trioTest(Vertex v,Edge se){
		Vertex otherEndSV = se.getVertex2(); 

		//iterate v's edges
		for(Edge e : v.getEdgeMap().values()){
			Vertex w = e.getVertex2();
			Vertex thirdSV = contractedGraph.getVertex(contraction.getWrappingSuperVertex(w).getId());

			//If there is an assigned third edge, assign se to its other end
			for(Vertex u : ((SuperVertex) otherEndSV.getData()).getInnerVertices()){	
				if(w.hasEdgeTo(u.getVertexId())){
					MyLogger.important("Found assigned third edge : " + v + ","+ u + "," + w);
					assignSuperEdge(se, v, u);
					return true;
				}
			}
			//If there is an unassigned third edge
			if(otherEndSV.hasEdgeTo(thirdSV.getVertexId()) && ((RelationshipProbWeight) otherEndSV.getEdgeTo(thirdSV.getVertexId()).getWeight()).isMaxProbCategory("fullSib")){
				MyLogger.important("Found unassigned third edge : " + v + ","+ otherEndSV + "," + thirdSV);
				//if(TRIPPLE_SHARING)
				for(Vertex u : ((SuperVertex) otherEndSV.getData()).getInnerVertices()){
					if(vacancyMap.get(u)){
						assignSuperEdge(se,v,u);
						assignSuperEdge(otherEndSV.getEdgeTo(thirdSV.getVertexId()),u,w);
						return true;
					}
				}
			}
			else
				MyLogger.important("No third edge : " + v + ","+ otherEndSV + "," + thirdSV);
		}
		return false;
	}

	private void assignSuperEdge(Edge se, Vertex v, Vertex u) {
		MyLogger.important("Assigning " + v + ","+u);
		vacancyMap.put(v, false);
		vacancyMap.put(u, false);
		v.addEdge(new BaseEdge(v,u,se.getWeight()));
		u.addEdge(new BaseEdge(u,v,RelationshipProbWeight.switchWeightsDirection((RelationshipProbWeight)se.getWeight())));
		se.getVertex1().removeEdgeTo(se.getVertex2());
		se.getVertex2().removeEdgeTo(se.getVertex1());
		//Add new vertices to eList
		if(!eList.contains(v)){
			for(Vertex inner : contraction.getWrappingSuperVertex(v.getVertexId()).getInnerVertices())
				eList.add(inner);
			expendedNum++;
		}

		if(!eList.contains(u)){
			for(Vertex inner : contraction.getWrappingSuperVertex(u.getVertexId()).getInnerVertices())
				eList.add(inner);
			expendedNum++;
		}
	}
}
