package prepare;

import java.io.*;
import java.util.*;

import misc.NextIDGenerator;
import misc.NucFamily;
import misc.NuclearFamilyCreator;
import misc.Person;
import misc.RelationshipProbWeight;

import relationship.CommonParentHypothesisTester;
import relationship.SibHypothesisTester;
//import prepare.ParentCouplesHypothesisTester;

import simulator.Pedigree;
import graph.Graph;
import graph.MyLogger;
import graph.Vertex;

/**
 * Implemeting reconstruct inference based on posterior relationship probabilities file
 */
public class PedigreeBuilder {

	private Graph graph;

	private int totalParents=0;
	private Set<Person> allPersonsEver = new HashSet<Person>();
	private int generation=1;
	private File outputDir;
	private boolean polygamous;
	private boolean synchronous;
	private boolean phased;

	public PedigreeBuilder(Graph graph, String outputDir,boolean poly,boolean sync, boolean phasedInput) {
		this.graph = graph;
		this.outputDir = new File(outputDir);
		this.polygamous=poly;
		this.synchronous=sync;
		this.phased=phasedInput;
	}

	/**
	 * Main method - using actual pedigree for IBD sharing estimation
	 */
	public void buildGeneration(Pedigree ped,int gen,int startFromGen,Pedigree fullPed,Demographics dem) {

		this.generation=gen;
		Graph IBDgraph=graph;

		NextIDGenerator nextIDGen = new NextIDGenerator(gen);

		SibHypothesisTester sibHypTester = new SibHypothesisTester(IBDgraph,synchronous,polygamous,phased);
		CommonParentHypothesisTester commomParentHypTester = new CommonParentHypothesisTester(IBDgraph,synchronous,phased);
		//ParentCouplesHypothesisTester parentsHypTester = new ParentCouplesHypothesisTester(IBDgraph,);

		MyLogger.important(">>> buildGeneration" + gen);
		Contraction contraction = new Contraction(ped); 

		MyLogger.important("===================Test for full-sib hypothesis (" + gen + " )====================");
		Graph contractedRelationGraph;
		contractedRelationGraph = contraction.createEdgelessContractedGraph();

		sibHypTester.run(ped,contractedRelationGraph,contractedRelationGraph.getVertices(),gen,fullPed);
		//contractedRelationGraph = sibHypTester.debugWithRealRelationships(ped,contraction,fullPed);

	//	if(!synchronous && gen>1){
	//		MyLogger.important("===================Detect deceased parents (" + gen + " )====================");
	//		parentsHypTester.run(ped,contractedRelationGraph,contraction,gen,fullPed,nextIDGen);
	//	}

		SibGraphExpander sge = new SibGraphExpander(contractedRelationGraph,contraction);

		if(polygamous){
			MyLogger.important("===========Remove couple's fullSib double edges if existing(" + gen + ")==========");
			sge.removeRedundantEdges(ped);
		}
		MyLogger.important("===================Expand contracted sibs graph (" + gen + " )====================");
		Graph sibExpendedGraph = sge.run();

		if(gen==1 && !synchronous){
			MyLogger.important("===================Detect living parents (" + gen + " )====================");
			assignLivingParents(contractedRelationGraph);
		}

		//Search for maximal weight independent set of sib cliques
		MyLogger.important("======================Partition Full-Sibs(" + gen + ")=====================");
		SiblingGrouper sibGrouper = new SiblingGrouper(sibExpendedGraph);
		sibGrouper.detectSiblings(gen,ped);

		//Retreive group of siblings
		NuclearFamilyCreator nuclearFamilyCreator = new NuclearFamilyCreator(sibExpendedGraph,gen,nextIDGen);
		List<List<Vertex>> siblingGroups = nuclearFamilyCreator.getSiblingGroups();

		// process siblings groups to create Nuclear Families
		MyLogger.important("===========Create Nuclear Families (" + gen + ")==========");
		List<NucFamily> nucFamilies = nuclearFamilyCreator.createNuclearFamilies(siblingGroups,false);

		if(polygamous){
			//MyLogger.important("===========Re-Contracting unexpended vertices(" + gen + ")==========");
			//contraction.reContractUnexpendedVertices(sibExpendedGraph);
			//contraction.wrapExpendedVertices(sibExpendedGraph);
			//MyLogger.important("Num of edges=" + sibExpendedGraph.getNumOfEdges());

			//sibExpendedGraph.clearAllEdges();
			//MyLogger.important("Num of edges=" + sibExpendedGraph.getNumOfEdges());
			Contraction sibContraction = new Contraction(nucFamilies);

			MyLogger.important("===========Test Nuclear Families for common parent(" + gen + ")==========");
			Graph halfSibGraph = commomParentHypTester.run(ped,sibContraction,nucFamilies,gen,fullPed);
			//commomParentHypTester.debugWithRealRelationships(ped,sibExpendedGraph,contraction,nucFamilies,gen,fullPed);

			HalfSibGraphExpender hsge = new HalfSibGraphExpender(halfSibGraph,ped,sibContraction);
			MyLogger.important("===========Remove couple's double halfSib edges if existing(" + gen + ")==========");
			Graph expendedHalfSibGraph = hsge.run();
			//MyLogger.important("===================Expand contracted half-sib graph (" + gen + " )====================");
			//Graph halfSibExpendedRelationGraph = rge.run("halfSib");
			//MyLogger.important("Num of edges=" + halfSibExpendedRelationGraph.getNumOfEdges());

			MyLogger.important("======================Partition Half-Sibs(" + gen + ")=====================");
			sibGrouper = new SiblingGrouper(expendedHalfSibGraph);
			sibGrouper.uniteCommonParentOfHalfSibs(nucFamilies,gen);

			//MyLogger.important("======================Partition Half-Sibs(" + gen + ")=====================");
			//sibGrouper = new SiblingGrouper(halfSibExpendedGraph);
			//sibGrouper.uniteCommonParentOfHalfSibs(nucFamilies,gen);
		}
		MyLogger.important("======================Update pedigree(" + gen + ")=====================");
		updatePedigreeObj(ped,nucFamilies);

		MyLogger.important("=========================Writing output========================");
		try {ped.writeToFile(outputDir,dem);
		} catch (IOException e) { throw new RuntimeException("Failed opening out file when writing generation " + gen,e);}
	}

	/**
	 * Find best scoring mother and father, from list of potential parents
	 * 
	 */
	private void assignLivingParents(Graph graph) {

		List<Vertex> existingVertices = new ArrayList<Vertex>(graph.getVertexMap().values());
		//transpass all graph verteces
		for(Vertex v : existingVertices) {

			Person currPerson = (Person) ((SuperVertex) v.getData()).getInnerVertices().get(0).getData();

			//Find curr vertex parents.
			for (graph.Edge e : v.getEdgeMap().values()) {
				RelationshipProbWeight weight = (RelationshipProbWeight)e.getWeight();
				Vertex u = e.getVertex2();
				Person otherPerson=(Person) ((SuperVertex) u.getData()).getInnerVertices().get(0).getData();;

				if(weight.isMaxProbCategory("parent")){
					if(otherPerson.getGender() && currPerson.getFamily().motherProbability<weight.getProb("parent")){
						currPerson.getFamily().mother=otherPerson;
						currPerson.getFamily().motherProbability=weight.getProb("parent");
						MyLogger.important(otherPerson.toString() + " is the mother of " + currPerson.toString());
						totalParents++;
					}

					if(!otherPerson.getGender() && currPerson.getFamily().fatherProbability<weight.getProb("parent")){
						currPerson.getFamily().father=otherPerson;
						currPerson.getFamily().fatherProbability=weight.getProb("parent");
						MyLogger.important(otherPerson.toString() + " is the father of " + currPerson.toString());
						totalParents++;
					}
				}
				if(weight.isMaxProbCategory("child")){
					if(currPerson.getGender() && otherPerson.getFamily().motherProbability<weight.getProb("child")){
						otherPerson.getFamily().mother=currPerson;
						otherPerson.getFamily().motherProbability=weight.getProb("child");
						MyLogger.important(currPerson.toString() + " is the mother of " + otherPerson.toString());
						totalParents++;
					} 
					if(!currPerson.getGender() && otherPerson.getFamily().fatherProbability<weight.getProb("child")){
						otherPerson.getFamily().father=currPerson;
						otherPerson.getFamily().fatherProbability=weight.getProb("child");
						MyLogger.important(currPerson.toString() + " is the father of " + otherPerson.toString());
						totalParents++;
					}
				}
			}

			/*
			 * Mark chosen edges, so that they would not be selected again.
			 */
			for (graph.Edge e : v.getEdgeMap().values()) {
				Person otherPerson=(Person) ((SuperVertex)e.getVertex2().getData()).getInnerVertices().get(0).getData();
				if(otherPerson==currPerson.getFamily().father || otherPerson==currPerson.getFamily().mother){
					RelationshipProbWeight weight = (RelationshipProbWeight)e.getWeight();
					MyLogger.debug(otherPerson.getId() + " " + otherPerson.getAge() + " is a parent of " + currPerson.getId() + " " + currPerson.getAge());
					//System.out.println("before deterministic chocie: " + weight);
					weight.makeDeteministicChoice("parent");
				}
				if(otherPerson.getFamily().father==currPerson || otherPerson.getFamily().mother==currPerson){
					RelationshipProbWeight weight = (RelationshipProbWeight)e.getWeight();
					//System.out.println("before deterministic choice: " + weight);
					MyLogger.debug(currPerson.getId() + " is a parent of " + otherPerson.getId());
					weight.makeDeteministicChoice("child");
				}
			}
		}
		MyLogger.important("Found " + totalParents + " parents");
	}


	private void updatePedigreeObj(Pedigree ped,List<NucFamily> nuclearFamilies) {
		MyLogger.important("PedigreeBuilder::updatePedigreeObj");
		for(NucFamily fam : nuclearFamilies) {
			int fatherID=fam.getFather().getId();
			int motherID=fam.getMother().getId();
			ped.addVertex(fatherID);
			ped.addVertex(motherID);
			for(Person child : fam.siblings){
				int id = child.getId();
				if(!ped.hasVertex(id))
					ped.addVertex(id,fatherID,motherID,child.isAlive());
				else{
					ped.getVertex(id).setFather(fatherID);
					ped.getVertex(id).setMother(motherID);
				}
			}
		}
		ped.calcExpectedFounderAges(this.generation);
	}


	public void writeStructureFile(File structFile) throws IOException {
		PrintWriter printWriter = new PrintWriter(structFile);
		//printWriter.println(String.format("name\tfather\tmother"));
		for(Person person : allPersonsEver) {
			if(person.getFamily().mother != null){
				printWriter.println(String.format("%d\t%d\t%d",person.getId(),person.getFamily().father.getId(),person.getFamily().mother.getId()));
				printWriter.flush();
			}
		}
		printWriter.close();
	}
}
