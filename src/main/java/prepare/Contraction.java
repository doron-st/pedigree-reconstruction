package prepare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import misc.NucFamily;
import misc.Person;


import simulator.Pedigree;
import simulator.Pedigree.PedVertex;
import graph.BaseVertex;
import graph.Graph;
import graph.MyLogger;
import graph.Vertex;
import graph.VertexData;

public class Contraction {

	Map<Integer,SuperVertex> contractionMap = new HashMap<Integer,SuperVertex>();
	List<SuperVertex> contractedNodes = new ArrayList<SuperVertex>();
	List<Vertex> unexpendedNodes = new ArrayList<Vertex>();
	/**
	 * Create a contraction from the founder pedigree vertices
	 * @param p
	 */
	public Contraction(Pedigree p) {

		for(PedVertex v1 : p.getFounders()){

			List<PedVertex> descendants1 = p.getDescendants(v1.getId());
			List<PedVertex> livingDescendants1 = p.getDescendants(v1.getId());
			for (PedVertex pv : descendants1){
				if(!pv.isAlive())
					livingDescendants1.remove(pv);
			}
			//descendants1.remove(v1);
			//System.out.println(v1 + " " + livingDescendants1);

			for(PedVertex v2 : p.getFounders()){
				if(v2.isAlive())
					continue;

				List<PedVertex> descendants2 = p.getDescendants(v2.getId());
				List<PedVertex> livingDescendants2 = p.getDescendants(v2.getId());
				for (PedVertex pv : descendants2){
					if(!pv.isAlive())
						livingDescendants2.remove(pv);
				}
				//descendants2.remove(v2);

				int vid1 = v1.getId();
				int vid2 = v2.getId();

				if(vid1>=vid2) continue;//Do only one side calculation, don't self compare


				if(livingDescendants1.containsAll(livingDescendants2) && livingDescendants2.containsAll(livingDescendants1)){
					MyLogger.debug("Contracting " + v1 + "," + v2);
					Person p1 = p.getDemographics().getPerson(vid1);
					Person p2 = p.getDemographics().getPerson(vid2);

					
					//if both are yet to be contracted
					if(contractionMap.get(vid1)== null && contractionMap.get(vid1)== null){
						MyLogger.debug("contract " + vid1 + " & " + vid2);
						SuperVertex contractedNode = new SuperVertex(new BaseVertex(p1));
						contractedNode.addVertex(new BaseVertex(p2));
						contractedNodes.add(contractedNode);
						contractionMap.put(v1.getId(), contractedNode);
						contractionMap.put(v2.getId(), contractedNode);
					}
					//If vid1 is already contracted and vid2 is not in the same super vertex
					else if(contractionMap.get(vid1)!= null && contractionMap.get(vid1)!=contractionMap.get(vid2)){
						MyLogger.debug("Adding " + vid2 + " to " + contractionMap.get(vid1));
						//add p2 to the super-vertex of vid1
						contractionMap.get(vid1).addVertex(new BaseVertex(p2));
						//set this super-vertex as the contraction of vid2
						contractionMap.put(vid2, contractionMap.get(vid1));
					}
					else if(contractionMap.get(vid2)!= null && contractionMap.get(vid1)!=contractionMap.get(vid2)){
						MyLogger.debug("Adding " + vid1 + " to " + contractionMap.get(vid2));
						contractionMap.get(vid2).addVertex(new BaseVertex(p1));
						contractionMap.put(vid1, contractionMap.get(vid2));
					}
				}
			}
			//If no one to contract with, create singleton super vertex
			if(contractionMap.get(v1.getId())==null){
				Person p1 = p.getDemographics().getPerson(v1.getId());
				SuperVertex sv = new SuperVertex(new BaseVertex(p1));
				contractedNodes.add(sv);
				contractionMap.put(v1.getId(),sv);
				MyLogger.debug("Contracting " + v1);

			}
			MyLogger.info("SuperVertex of " + v1 + " is" + contractionMap.get(v1.getId()));
		}
	}

	//Trivial contraction
	public Contraction(Pedigree ped, boolean trivial) {
		for(PedVertex f : ped.getFounders()){
			Person p = ped.getDemographics().getPerson(f.getId());
			SuperVertex contractedNode = new SuperVertex(new BaseVertex(p));
			contractedNodes.add(contractedNode);
			contractionMap.put(f.getId(), contractedNode);
		}
	}

	/**create sibling contraction**/
	public Contraction(List<NucFamily> nucFamilies) {
		for(NucFamily fam : nucFamilies){
			SuperVertex sv = new SuperVertex(new BaseVertex(fam.siblings.get(0)));
			for(Person sib : fam.siblings)
				if(sib!=fam.siblings.get(0))
					sv.addVertex(new BaseVertex(sib));
			contractedNodes.add(sv);
			for(Person sib : fam.siblings)
				contractionMap.put(sib.getId(), sv);
		}
	}

	public List<SuperVertex> getSuperVertices(){
		return contractedNodes;
	}

	/**
	 * Applies the vertex contruction on g
	 * @param g
	 */
	public Graph createEdgelessContractedGraph(){
		List<VertexData> dataList = new ArrayList<VertexData>(contractedNodes);
		return new Graph(dataList);
	}

	public SuperVertex getWrappingSuperVertex(Vertex v) {
		return contractionMap.get(v.getVertexId());
	}
	public SuperVertex getWrappingSuperVertex(int id) {
		return contractionMap.get(id);
	}

	public void wrapExpendedVertices(Graph graph) {
		for(Vertex v : graph.getVertices()){
			if(!v.getData().getClass().equals(SuperVertex.class)){
				//MyLogger.important("Wrapping " +v);
				v.setData(new SuperVertex(v));
				contractionMap.put(v.getVertexId(), (SuperVertex)v.getData());
			}
			//else
			//	MyLogger.important("Already super-vertex:  " +v);
		}
	}

	public void reContractUnexpendedVertices(Graph graph) {
		//Add unexpended vertices back to the graph
		for(Vertex sv : unexpendedNodes){
			//MyLogger.important("Re-Contracting " + sv);
			for(Vertex v : ((SuperVertex) sv.getData()).getInnerVertices()){
				graph.removeVertex(v);
			}
			graph.addVertex(sv);			
		}
	}

	public void addUnexpendedList(List<Vertex> uList) {
		unexpendedNodes=uList;

	}

	public void expendList(List<Integer> toExpand,Graph graph) {
		for(int id : toExpand){
			SuperVertex sv = getWrappingSuperVertex(id);
			int svID = sv.getId();
			Vertex contGraphVertex = graph.getVertex(svID);
			MyLogger.important("Expending on id " + id + " sv="+sv);

			Vertex v=null;
			for(Vertex inner : sv.getInnerVertices()){
				if(inner.getVertexId()==id)
					v=inner;
			}
			sv.removeVertex(v);
			graph.setVertexID(contGraphVertex,sv.getId());

			//remove empty super-vertex
			if(sv.getInnerVertices().size()==0){
				MyLogger.important("remove empty vertex : " + contGraphVertex);
				graph.removeVertex(contGraphVertex);
			}
			
			//SuperVertex wrap = new SuperVertex(v);
			//graph.addVertex(new BaseVertex(wrap));
			//contractedNodes.add(wrap);
			//contractionMap.put(id, wrap);
		}
	}
}