package relationship;

import simulator.Pedigree;
import simulator.Pedigree.PedVertex;
import graph.Edge;
import graph.Graph;
import graph.MyLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import prepare.IBDFeaturesWeight;

import jsat.classifiers.DataPoint;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.multivariate.MetricKDE;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.linear.distancemetrics.EuclideanDistance;


public abstract class PedLikelihoodCalcAbs {
	private int numOfSimIter;
	private boolean phased;
	
	public PedLikelihoodCalcAbs(int numOfSimIterations, boolean phased){
		numOfSimIter=numOfSimIterations;
		this.phased=phased;
	}
	public abstract double calcLikelihood(Pedigree p, Graph iBDgraph,
			Map<Integer, Integer> idConversion, List<PedVertex> descendants1,List<PedVertex> descendants2);
	/**
	 * @return true iff chance of bing sib/half-sib is not minute
	 */
	public boolean passRelatedFilter(Pedigree p,int vid1, int vid2,Graph IBDGraph,Map<Integer,Integer> idConversion){
		List<PedVertex> descendants1 = p.getDescendants(vid1);
		List<PedVertex> descendants2 = p.getDescendants(vid2);
		IBDFeaturesWeight aggregatedFeatures=new IBDFeaturesWeight(0, 0);
		int numOfPairs=0;

		for(PedVertex v1 : p.getLiving()){
			for(PedVertex v2 : p.getLiving()){

				if(((descendants1.contains(v1) && descendants2.contains(v2)) || //consider only desccendants of vid1 and vid2 respectively
						(descendants1.contains(v2) && descendants2.contains(v1))
						&& v1.getId()<v2.getId())){//Do only one side calculation, don't self compare

					MyLogger.debug("Consider " +idConversion.get(v1.getId()) + ","+ idConversion.get(v2.getId()));
					//Get observed IBD sharing features
					IBDFeaturesWeight obsFeatures=new IBDFeaturesWeight(0,0);
					Edge e = IBDGraph.getUndirectedEdge(idConversion.get(v1.getId()), idConversion.get(v2.getId()));
					if(e!=null) 
						obsFeatures = (IBDFeaturesWeight) e.getWeight();

					//Aggregate IBD features of all descendents
					aggregatedFeatures.setMeanLength(aggregatedFeatures.getMeanLength()+obsFeatures.getMeanLength());
					aggregatedFeatures.setSegmentNum(aggregatedFeatures.getSegmentNum()+obsFeatures.getSegmentNum());
					numOfPairs++;
				}
			}
		}
		aggregatedFeatures.setMeanLength(aggregatedFeatures.getMeanLength()/numOfPairs);
		aggregatedFeatures.setSegmentNum(aggregatedFeatures.getSegmentNum()/numOfPairs);

		if(aggregatedFeatures.getSegmentNum()>10 && aggregatedFeatures.getMeanLength()>1){
			MyLogger.info("Average num of segments="+aggregatedFeatures.getSegmentNum());
			MyLogger.info("Average mean length="+aggregatedFeatures.getMeanLength());
			return true;
		}
		return false;
	}

	/**
	 * Create IBD feature distribution for every pair of living individuals in pedigree
	 * Simulate recombination process N times
	 * @param addNoise 
	 **/	
	Map<String, List<DataPoint>> sampleFeaturesFromInheritanceSpace(Pedigree p, boolean addNoise) {

		Map<String,List<DataPoint>> simDataSets = new HashMap<String,List<DataPoint>>();

		for(int i=1;i<=numOfSimIter;i++){
			MyLogger.debug("Simulate recombinations of pedigree");
			p.simulateRecombinations();

			MyLogger.debug("Calc pairwise IBD sharing");

			//Save sharing features for each pair of living individuals
			for(PedVertex v1 : p.getLiving()){
				for(PedVertex v2 : p.getLiving()){
					MyLogger.debug("calcIBDSharing " + v1 +"," + v2);

					if(v1.getId()>=v2.getId()) continue;//Do only one side calculation

					String pairID = v1.getId()+"."+v2.getId();

					//calculate features for pair
					DataPoint simFeatures = IBDFeaturesWeight.calcIBDFeatureWeight(p.getGenotype(v1.getId()),p.getGenotype(v2.getId()),addNoise,phased).asDataPoint();

					//Initialize datasets
					if(i==1){
						List<DataPoint> pairDataSet = new ArrayList<DataPoint>();
						pairDataSet.add(simFeatures);
						simDataSets.put(pairID,pairDataSet);
					}
					else //Add to existing datasets
						simDataSets.get(pairID).add(simFeatures);

					MyLogger.debug("sampleFeaturesFromInheritanceSpace("+v1+","+v2+")::simW=" + simFeatures.getNumericalValues());
				}
			}
		}
		return simDataSets;
	}
	
	/**
	 * Create IBD feature distribution for every pair of living individuals in pedigree
	 * Simulate recombination process N times
	 * @param addNoise 
	 **/	
	Map<String, List<DataPoint>> sampleFeaturesFromInheritanceSpace(Pedigree p, boolean addNoise,List<PedVertex> descendants1, List<PedVertex> descendants2) {

		Map<String,List<DataPoint>> simDataSets = new HashMap<String,List<DataPoint>>();

		for(int i=1;i<=numOfSimIter;i++){
			MyLogger.debug("Simulate recombinations of pedigree");
			p.simulateRecombinations();

			MyLogger.debug("Calc pairwise IBD sharing");

			//Save sharing features for each pair of living individuals
			for(PedVertex v1 : descendants1){
				if(!v1.isAlive()) continue;//compare only living descendents
				for(PedVertex v2 : descendants2){
					if(!v2.isAlive()) continue;//compare only living descendents

					if(v1.getId()==v2.getId()) continue;//don't self compare
					if(simDataSets.containsKey(v2.getId()+"."+v1.getId())) continue;//do only one side calculation
					
					String pairID = v1.getId()+"."+v2.getId();
					//MyLogger.important("calc Features for " + pairID);
					//calculate features for pair
					DataPoint simFeatures = IBDFeaturesWeight.calcIBDFeatureWeight(p.getGenotype(v1.getId()),p.getGenotype(v2.getId()),addNoise,phased).asDataPoint();

					//Initialize datasets
					if(i==1){
						List<DataPoint> pairDataSet = new ArrayList<DataPoint>();
						pairDataSet.add(simFeatures);
						simDataSets.put(pairID,pairDataSet);
					}
					else //Add to existing datasets
						simDataSets.get(pairID).add(simFeatures);

					MyLogger.debug("sampleFeaturesFromInheritanceSpace("+v1+","+v2+")::simW=" + simFeatures.getNumericalValues());
				}
			}
		}
		Map<String,List<DataPoint>> bothDirectionsSimDataSets = new HashMap<String,List<DataPoint>>();

		for(String key : simDataSets.keySet()){
			String[] split = key.split("\\.");
			String reverseKey = split[1] + "." + split[0];
			bothDirectionsSimDataSets.put(key, simDataSets.get(key));
			bothDirectionsSimDataSets.put(reverseKey, simDataSets.get(key));
		}
		return bothDirectionsSimDataSets;
	}
	
	MultivariateKDE estimateDensity(List<DataPoint> dataSet) {
		EuclideanDistance ed = new EuclideanDistance();
		MetricKDE kde = new MetricKDE(GaussKF.getInstance(),ed);
		if(dataSet==null){
			MyLogger.error("PedLikelihoodCalcAbs::estimateDensity::Null dataSet");
			throw new RuntimeErrorException(new Error("Null dataSet"));
		}
		kde.setUsingDataList(dataSet);
		//MyLogger.important("BW = " + kde.getBandwith());
		
		kde.setBandwith(8);
		return kde;
	}
	
	void printExplenationForInf(List<DataPoint> l ) {
		MyLogger.debug("dataSet for pair");
		MyLogger.debug("list size=" + l.size());
		for(int i=0;i<numOfSimIter ;i++){
			DataPoint point = l.get(i);
			MyLogger.debug(point.getNumericalValues().toString());
		}
	}

}

