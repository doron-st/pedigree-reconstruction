package relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import prepare.SuperVertex;


import simulator.Pedigree;
import simulator.Pedigree.PedVertex;
import graph.Edge;
import graph.Graph;
import graph.MyLogger;
import graph.Vertex;

public abstract class RelationHypothesisTester {
	Graph IBDgraph;
	PedLikelihoodCalcAbs lCalc;
	Map<Integer,Integer> idConversion;
	ExponentialDistribution sibAgeDifDist = new ExponentialDistribution(7);
	NormalDistribution parentAgeDifDist = new NormalDistribution(25, 10);
	NormalDistribution avuncularAgeDifDist = new NormalDistribution(20, 20);
	int numOfSibs=0;
	int numOfHalfSibs=0;
	int mistakes=0;
	boolean phased;
	
	public RelationHypothesisTester(Graph IBDGraph, boolean phased){
		this.IBDgraph=IBDGraph;
		this.phased=phased;
	}

	double calcSameLikelihood(Pedigree ped,int f1, int f2, List<PedVertex> descendants1, List<PedVertex> descendants2, boolean isFather){
		Pedigree p = new Pedigree(ped);//clone
		for(PedVertex f2Child : p.getVertex(f2).getChildren()){
			p.getVertex(f1).addChild(f2Child);
			if(isFather)
				f2Child.overrideFather(f1);
			else
				f2Child.overrideMother(f1);
		}
		p.removeVertex(f2);

		MyLogger.info("calcSameLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" sameLikelihood="+l);

		return  l;
	}

	double calcUnrelatedLikelihood(Pedigree ped,int f1, int f2, List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone
		MyLogger.info("calcUnrelatedLikelihood::pedSize=" + p.getVertices().size());


		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" unrelatedLikelihood="+l);

		return  l;
	}

	double calcSibLikelihood(Pedigree ped, int f1, int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int fatherID;
		int motherID;

		fatherID = p.getNewID();
		motherID = p.getNewID();

		//Add nodes that create sibling hypothesis
		p.addVertex(fatherID);
		p.addVertex(motherID);

		p.getVertex(f1).setFather(fatherID);
		p.getVertex(f1).setMother(motherID);
		p.getVertex(f2).setFather(fatherID);
		p.getVertex(f2).setMother(motherID);

		MyLogger.info("calcSibLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" sibLikelihood="+l);

		return l;
	}
	/**
	 * Calc the likelihood for f2 to be the parent of f1
	 */
	double[] calcParentLikelihood(Pedigree ped, int f1, int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		MyLogger.info("calcParentLikelihood");

		//Add nodes that create parent hypothesis
		int newMateID = -2;

		List<Integer> possibleMateIDs = new ArrayList<Integer>();
		possibleMateIDs.add(newMateID);

		//Find all possible mates
		for(PedVertex child : ped.getVertex(f2).getChildren()){
			int possibleMate=0;
			if(child.getFatherId()==f2)
				possibleMate = child.getMotherId();
			else
				possibleMate = child.getFatherId();
			if(!possibleMateIDs.contains(possibleMate))
				possibleMateIDs.add(possibleMate);
		}
		double bestLikelihood = Double.NEGATIVE_INFINITY;
		int bestMate=-1;

		for(int possibleMate : possibleMateIDs){
			MyLogger.info("Possible mate = " + possibleMate);
			Pedigree p = new Pedigree(ped);//clone pedigree
			if(possibleMate==newMateID)
				p.addVertex(possibleMate);

			if(ped.getVertex(f2).getChildren().isEmpty()
					|| ped.getVertex(f2).getChildren().get(0).getFatherId()==f2){
				p.getVertex(f1).setFather(f2);
				p.getVertex(f1).setMother(possibleMate);
			}
			else{
				p.getVertex(f1).setMother(f2);
				p.getVertex(f1).setFather(possibleMate);
			}
			if(p.getVertex(possibleMate).getFatherId()==f1 || p.getVertex(possibleMate).getMotherId()==f1){
				MyLogger.important(possibleMate + "Can't be the child of its child: " + f1 + ", skip hypothesis");
				continue;
			}
			MyLogger.info("calcParentLikelihood::pedSize=" + p.getVertices().size());
			double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
			MyLogger.info(f1 +" child of "+f2+","+possibleMate+"="+l);
			if(l>bestLikelihood){
				bestLikelihood=l;
				bestMate=possibleMate;
			}
		}
		return new double[] {bestLikelihood,bestMate};
	}

	double calcHalfSibLikelihood(Pedigree ped,int f1, int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int fatherID;
		int f1MotherID=-1;
		int f2MotherID=-1;

		//If one person is not founder set hypothesized parents as his parents
		fatherID = p.getNewID();
		f1MotherID = p.getNewID();
		f2MotherID = p.getNewID();

		//Add nodes that create half-sibling hypothesis
		p.addVertex(fatherID);
		p.addVertex(f1MotherID);
		p.addVertex(f2MotherID);

		p.getVertex(f1).setFather(fatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(fatherID);
		p.getVertex(f2).setMother(f2MotherID);

		MyLogger.info("calcHalfSibLikelihood::pedSize=" + p.getVertices().size());
		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" halfSibLikelihood="+l);

		return l;
	}

	double calcCousinLikelihood(Pedigree ped,int f1, int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int f1FatherID = p.getNewID();
		int f1MotherID = p.getNewID();
		int f2FatherID = p.getNewID();
		int f2MotherID = p.getNewID();
		int grandFatherID = p.getNewID();
		int grandMotherID = p.getNewID();

		//Add nodes that create cousin hypothesis
		p.addVertex(grandFatherID);
		p.addVertex(grandMotherID);
		p.addVertex(f1FatherID,grandFatherID,grandMotherID,false);
		p.addVertex(f2FatherID,grandFatherID,grandMotherID,false);
		p.addVertex(f1MotherID);
		p.addVertex(f2MotherID);

		p.getVertex(f1).setFather(f1FatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(f2FatherID);
		p.getVertex(f2).setMother(f2MotherID);

		MyLogger.info("calcCousinLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" cousLikelihood="+l);

		return l;
	}
	
	double calcDoubleCousinLikelihood(Pedigree ped,int f1, int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int f1FatherID = p.getNewID();
		int f1MotherID = p.getNewID();
		int f2FatherID = p.getNewID();
		int f2MotherID = p.getNewID();
		int fgrandFatherID = p.getNewID();
		int fgrandMotherID = p.getNewID();
		int mgrandFatherID = p.getNewID();
		int mgrandMotherID = p.getNewID();

		//Add nodes that create cousin hypothesis
		p.addVertex(fgrandFatherID);
		p.addVertex(fgrandMotherID);
		p.addVertex(mgrandFatherID);
		p.addVertex(mgrandMotherID);
		
		p.addVertex(f1FatherID,fgrandFatherID,fgrandMotherID,false);
		p.addVertex(f2FatherID,fgrandFatherID,fgrandMotherID,false);
		p.addVertex(f1MotherID,mgrandFatherID,mgrandMotherID,false);
		p.addVertex(f2MotherID,mgrandFatherID,mgrandMotherID,false);
		
		p.getVertex(f1).setFather(f1FatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(f2FatherID);
		p.getVertex(f2).setMother(f2MotherID);

		MyLogger.info("calcDoubleCousinLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" doubleCousLikelihood="+l);

		return l;
	}

	double calcHalfCousinLikelihood(Pedigree ped,int f1,int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int f1FatherID = p.getNewID();
		int f1MotherID = p.getNewID();
		int f2FatherID = p.getNewID();
		int f2MotherID = p.getNewID();
		int grandFatherID = p.getNewID();
		int f1GrandMotherID = p.getNewID();
		int f2GrandMotherID = p.getNewID();

		//Add nodes that create cousin hypothesis
		p.addVertex(grandFatherID);
		p.addVertex(f1GrandMotherID);
		p.addVertex(f2GrandMotherID);
		p.addVertex(f1FatherID,grandFatherID,f1GrandMotherID,false);
		p.addVertex(f1MotherID);
		p.addVertex(f2FatherID,grandFatherID,f2GrandMotherID,false);
		p.addVertex(f2MotherID);

		p.getVertex(f1).setFather(f1FatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(f2FatherID);
		p.getVertex(f2).setMother(f2MotherID);

		MyLogger.info("calcHalfCousinLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" halfCousLikelihood="+l);

		return l;
	}

	double calcAvuncularLikelihood(Pedigree ped,int f1,int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone

		int	f1FatherID = p.getNewID();
		int	f1MotherID = p.getNewID();

		int	f2FatherID = p.getNewID();
		int	f2MotherID = p.getNewID();

		p.addVertex(f1MotherID);
		p.addVertex(f1FatherID);
		p.addVertex(f2FatherID);
		p.addVertex(f2MotherID);

		p.getVertex(f1).setFather(f1FatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(f2FatherID);
		p.getVertex(f2).setMother(f2MotherID);

		//set f2 as the uncle of f1
		p.getVertex(f2FatherID).setFather(f1FatherID);
		p.getVertex(f2FatherID).setMother(f1MotherID);

		MyLogger.info("calcAvuncularLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" avuncularLikelihood="+l);

		return l;
	}

	public static boolean isMaxFromArray(double cand,double[] arr) {
		for(double elm : arr){
			if(elm>cand)
				return false;
		}
		return true;
	}

	double calcHalfAvuncularLikelihood(Pedigree ped,int f1,int f2,List<PedVertex> descendants1,List<PedVertex> descendants2){
		Pedigree p = new Pedigree(ped);//clone
		MyLogger.info("calcHalfAvuncularLikelihood");

		int	f1FatherID = p.getNewID();
		int	f1MotherID = p.getNewID();
		int	f2FatherID = p.getNewID();
		int	f2MotherID = p.getNewID();
		int otherGrandMotherID = p.getNewID();

		p.addVertex(f1MotherID);
		p.addVertex(f1FatherID);	
		p.addVertex(f2FatherID);
		p.addVertex(f2MotherID);
		p.addVertex(otherGrandMotherID);

		p.getVertex(f1).setFather(f1FatherID);
		p.getVertex(f1).setMother(f1MotherID);
		p.getVertex(f2).setFather(f2FatherID);
		p.getVertex(f2).setMother(f2MotherID);

		//f1 is the uncle of f2
		p.getVertex(f2FatherID).setFather(f1FatherID);
		p.getVertex(f2FatherID).setMother(otherGrandMotherID);

		MyLogger.info("calcHalfAvuncularLikelihood::pedSize=" + p.getVertices().size());

		double l =  lCalc.calcLikelihood(p,IBDgraph,idConversion,descendants1,descendants2);
		MyLogger.info(f1 +","+f2+" halfAvuncularLikelihood="+l);

		return l;
	}

	void applyBayesUniPriors(double[] likelihoods) {
		double Lsum = 0;
		int i=0;
		for(double l : likelihoods){
			double L = Math.exp(l);
			likelihoods[i]=L;
			Lsum+=L;
			i++;
		}
		for(i=0; i<likelihoods.length;i++){
			likelihoods[i]/=Lsum;
		}
	}


	/**
	 * Debugging Methods
	 */
	boolean areSibs(SuperVertex s1, SuperVertex s2,Pedigree p) {
		for(Vertex v1 : s1.getInnerVertices()){
			for(Vertex v2 : s2.getInnerVertices()){
				if(areSibs(v1.getVertexId(),v2.getVertexId(),p))	
					return true;
			}
		}
		return false;
	}

	boolean areHalfSibs(SuperVertex s1, SuperVertex s2,Pedigree p) {
		for(Vertex v1 : s1.getInnerVertices()){
			for(Vertex v2 : s2.getInnerVertices()){
				if(areHalfSibs(v1.getVertexId(),v2.getVertexId(),p))	
					return true;
			}
		}
		return false;
	}

	boolean areSibs(int id1, int id2,Pedigree p) {
		if(id1!=id2 && p.getVertex(id1).getFatherId()==p.getVertex(id2).getFatherId() && 
				p.getVertex(id1).getMotherId()==p.getVertex(id2).getMotherId()){
			MyLogger.info("Found sibs in pedigree " + id1 + " " + id2);
			return true;
		}
		return false;
	}

	boolean areHalfSibs(int id1, int id2,Pedigree p) {
		if(id1!=id2 && (p.getVertex(id1).getFatherId()==p.getVertex(id2).getFatherId() || 
				p.getVertex(id1).getMotherId()==p.getVertex(id2).getMotherId()) && !areSibs(id1,id2,p)){
			MyLogger.info("Found half-sibs in pedigree " + id1 + " " + id2);
			return true;
		}
		return false;
	}

	boolean areParentChild(SuperVertex s1, SuperVertex s2,Pedigree p) {
		for(Vertex v1 : s1.getInnerVertices()){
			for(Vertex v2 : s2.getInnerVertices()){
				if(p.getVertex(v2.getVertexId()).getFatherId()==v1.getVertexId() ||  
						p.getVertex(v2.getVertexId()).getMotherId()==v1.getVertexId())
					return true;
			}
		}
		return false;
	}
	boolean areCousins(SuperVertex s1, SuperVertex s2,Pedigree p) {
		for(Vertex v1 : s1.getInnerVertices()){
			for(Vertex v2 : s2.getInnerVertices()){
				if(areSibs(p.getVertex(v1.getVertexId()).getFatherId(),p.getVertex(v2.getVertexId()).getFatherId(),p) || 
						areSibs(p.getVertex(v1.getVertexId()).getMotherId(),p.getVertex(v2.getVertexId()).getMotherId(),p) ||
						areSibs(p.getVertex(v1.getVertexId()).getFatherId(),p.getVertex(v2.getVertexId()).getMotherId(),p) ||
						areSibs(p.getVertex(v1.getVertexId()).getMotherId(),p.getVertex(v2.getVertexId()).getFatherId(),p)){
					MyLogger.important("Found cousins in pedigree " + v1 + " " + v2);
					return true;
				}
			}
		}
		return false;
	}

	void printSibDebugInfo(Pedigree fullPed, int numOfSibs,Pedigree ped,
			SuperVertex s1, SuperVertex s2, double[] likelihoods,
			double sibLikelihood,boolean half) {
		String desc;
		if(half){
			desc = "half-sibs";
		}
		else{
			desc = "sibs";
		}

		if(isMaxFromArray(sibLikelihood,likelihoods)){
			MyLogger.important(numOfSibs + ") Found potential " + desc +"! " + s1 +","+s2 + " L="+sibLikelihood);
			MyLogger.important("nr="+likelihoods[0]+",pc="+likelihoods[2]+",sib="+likelihoods[1]+",av="+likelihoods[4]+",cs="+likelihoods[5]+",hs="+likelihoods[3]+",dc="+likelihoods[8]);
			printPairWiseIBD(s1, s2, ped);

			if((half && !areHalfSibs(s1, s2, fullPed)) || (!half && !areSibs(s1, s2, fullPed))){
				MyLogger.warn((mistakes++) + ")Potential "+desc+" are false! " + s1 + "," + s2);
			}
		}
		else if((half && areHalfSibs(s1, s2, fullPed)) || (!half && areSibs(s1, s2, fullPed))){
			MyLogger.warn((mistakes++) + ") " + desc+" are not max category! " + s1 + "," + s2);
			printPairWiseIBD(s1,s2,ped);
			MyLogger.warn("nr="+likelihoods[0]+",pc="+likelihoods[2]+",sib="+likelihoods[1]+",av="+likelihoods[4]+",cs="+likelihoods[5]+",hs="+likelihoods[3]);
		}

	}


	void printCousinDebugInfo(Pedigree fullPed, SuperVertex s1,Pedigree ped,
			SuperVertex s2, double[] likelihoods, double cousLikelihood) {
		if(isMaxFromArray(cousLikelihood,likelihoods)){
			MyLogger.important("Found potential cousins! " + s1 +","+s2 + " L="+cousLikelihood);
			printPairWiseIBD(s1,s2,ped);
			if(!areCousins(s1, s2, fullPed)){
				MyLogger.warn("Potential cousins are false! " + s1 + "," + s2);
				printPairWiseIBD(s1,s2,ped);
				MyLogger.warn("nr="+likelihoods[0]+",pc="+likelihoods[2]+",sib="+likelihoods[1]+",av="+likelihoods[4]+",cs="+likelihoods[5]);
			}
		}
		else if(areCousins(s1, s2, fullPed)){
			MyLogger.warn("Cousins are not max category! " + s1 + "," + s2);
			printPairWiseIBD(s1,s2,ped);
			MyLogger.warn("nr="+likelihoods[0]+",pc="+likelihoods[2]+",sib="+likelihoods[1]+",av="+likelihoods[4]+",cs="+likelihoods[5]);
		}
	}

	public void printPairWiseIBD(SuperVertex s1,SuperVertex s2,Pedigree ped){
		Vertex v1 = s1.getInnerVertices().get(0);
		Vertex v2 = s2.getInnerVertices().get(0);
		MyLogger.important("IBD for descendents of " + s1 + "," + s2);
		for(PedVertex des1 : ped.getAllDescendants(v1.getVertexId())){
			for(PedVertex des2 : ped.getAllDescendants(v2.getVertexId())){
				Edge e = IBDgraph.getUndirectedEdge(des1.getId(), des2.getId());
				if(des1.isAlive() && des2.isAlive() && des1!=des2)
					if(e!=null)
						MyLogger.important(des1.getId() +"," + des2.getId() + " IBD features=" + IBDgraph.getUndirectedEdge(des1.getId(), des2.getId()).getWeight());
					else
						MyLogger.important(des1.getId() +"," + des2.getId() + " IBD features=0,0");
			}
		}
	}
}

