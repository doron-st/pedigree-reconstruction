package simulator;

import simulator.Pedigree.PedVertex;
import graph.MyLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import prepare.IBDFeaturesWeight;

public class WFSimOld {

	public static void main(String [] args){

		if(args.length<4){
			MyLogger.error("Usage: java -jar WrightFisherSimulation.jar popSize generations monogamyProb out");
			System.exit(1);
		}
		int popSize = Integer.valueOf(args[0]);
		int generations = Integer.valueOf(args[1]);
		double monogamyProb = Double.valueOf(args[2]);
		String out = args[3];		

		MyLogger.important("monogamyProb="+monogamyProb);

		Genotype [] genotypes = new Genotype[popSize];
		Genotype [] nextGenotypes = new Genotype[popSize];
		boolean [] genders = new boolean[popSize];
		boolean [] nextGenders = new boolean[popSize];
		Random rg = new Random();
		Map<String,Integer> structure = new HashMap<String,Integer>();
		List<List<Integer>> couples = new ArrayList<List<Integer>>();

		Pedigree ped = new Pedigree();

		int totalIndividuals=popSize*generations;

		//Create founder genotypes
		for(int i=0;i<popSize;i++){
			int id = totalIndividuals-popSize+i;
			genotypes[i] = new Genotype(id);
			genders[i] = rg.nextBoolean();
			couples.add(null);
			MyLogger.debug("Added vertex " + id + " "+ -1 + " " + -1);
			ped.addVertex(id);
		}

		File pedFile = new File(out + "/pedigree.ped");
		PrintWriter pedWriter = null;
		try {
			pedWriter = new PrintWriter(pedFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for(int gen =1;gen<generations;gen++){
			MyLogger.important("generation " + gen);
			//Sample parents for each individual, and recombine to get new genotypes
			for(int i=0;i<popSize;i++){
				boolean foundMates=false;
				int parent=-1;
				int mate=-1;
				//Find parents to individual i
				while(!foundMates){
					parent = rg.nextInt(popSize);
					boolean parentGender = genders[parent];
					List<Integer> mates = couples.get(parent);

					//Already has a mate (or mates)
					if(mates!=null){
						MyLogger.important("parent " + parent + " has a mate/mates: " + mates);

						//Reduce family size by randomely skipping mated parents
						if(rg.nextDouble()>0.2){
							MyLogger.important("Not ready to have another child yet");

							continue;
						}

						if(rg.nextDouble()<monogamyProb){//Passed monogy test  
							foundMates=true;
							mate = mates.get(rg.nextInt(mates.size())); //sample from previous mates uniformly
							MyLogger.important("having another child with " + mate);

						}
						else//(Cheated)
							MyLogger.important("parent " + parent + " has an out of marraige child");
					}
					if(!foundMates){
						//If need new mate
						mate = rg.nextInt(popSize);
						boolean mateGender = genders[mate];
						List<Integer> mateMates=couples.get(mate);
						double agreeProb;
						if(mateMates==null)
							agreeProb=1;
						else
							agreeProb = rg.nextDouble();

						//Matching gender, and mate agreed to have a child
						if(parentGender!=mateGender && agreeProb >= monogamyProb){
							foundMates=true;
							if(mates==null)
								mates = new ArrayList<Integer>();
							if(mateMates==null)
								mateMates = new ArrayList<Integer>();

							MyLogger.important(parent + " is having first child with " + mate);
							mates.add(mate);
							couples.set(parent, mates);
							mateMates.add(parent);
							couples.set(mate, mateMates);
						}
					}
				}
				//Create child genotype from parent and mate
				MyLogger.debug("recombine = " + genotypes[parent]);
				MyLogger.debug("recombine = " + genotypes[mate]);

				nextGenotypes[i] = new Genotype(genotypes[parent].recombine(), genotypes[mate].recombine());
				nextGenders[i] = rg.nextBoolean();
				int fatherId;
				int motherId;
				if(genders[parent]){
					fatherId=totalIndividuals-(gen*popSize)+parent;
					motherId=totalIndividuals-(gen*popSize)+mate;
				}
				else{
					fatherId=totalIndividuals-(gen*popSize)+mate;
					motherId=totalIndividuals-(gen*popSize)+parent;
				}
				int id = (totalIndividuals-((gen+1)*popSize)+i);
				MyLogger.important("Adding vertex " + id+ " "+ fatherId + " " + motherId);
				MyLogger.debug("genotype = " + nextGenotypes[i]);

				ped.addVertex(id, fatherId, motherId, false);
				pedWriter.println(id + ":"+ fatherId + "-" + motherId + " generation:" +(generations-gen));
				structure.put(id +"\t" + fatherId +"\t" + motherId,(generations-gen));
			}


			//increament generation
			genotypes=nextGenotypes;
			nextGenotypes = new Genotype[popSize];
			genders=nextGenders;
			nextGenders=new boolean[popSize];
			for(int i=0;i<popSize;i++)
				couples.set(i, null);
		}
		pedWriter.close();


		String structName = out + "/pedigree.structure";
		File ibdIped = new File(out + "/pedigree.iped.ibd");
		File dem = new File(out + "/pedigree.demographics");
		File ibd = new File(out + "/pedigree.ibd");

		try {
			writeIpedIBDFile(genotypes, ibdIped);

			List<PedVertex> lastGen = new ArrayList<PedVertex>();
			PrintWriter demWriter = new PrintWriter(dem);
			demWriter.println("name\tage\tgender");
			for(int i=0;i<popSize;i++){
				if(genders[i])
					demWriter.println(i + "\t" + 20 + "\t" + 0);
				else
					demWriter.println(i + "\t" + 20 + "\t" + 1);
				lastGen.add(ped.getVertex(i));
			}
			demWriter.close();

			ped.pruneExtinct(lastGen);
			for(int gen=2;gen<=generations;gen++){
				PrintWriter structWriter = new PrintWriter(new File(structName+(gen-1)));
				for(String key : structure.keySet()){
					String[] split = key.split("\t");
					int id = Integer.valueOf(split[0]);
					if(ped.hasVertex(id) && structure.get(key)<gen)
						structWriter.println(key);
				}
				structWriter.close();
			}
			ped.writeToFile(new File(structName));

			writeIBDFile(popSize, genotypes, ibd);


		} catch (IOException e) {
			e.printStackTrace();
		}
		MyLogger.debug("");
	}


	private static void writeIBDFile(int popSize, Genotype[] genotypes, File ibd)
			throws FileNotFoundException {
		PrintWriter ibdWriter = new PrintWriter(ibd);
		for(int i=0;i<popSize;i++){
			for(int j=i+1;j<popSize;j++){
				IBDFeaturesWeight ibdW = IBDFeaturesWeight.calcIBDFeatureWeight(genotypes[i],genotypes[j], false,true);
				if(ibdW.getSegmentNum()>0){
					MyLogger.important("IBD : " + i +"," + j + " =" + ibdW);
					//MyLogger.important(0 + " geno= " + genotypes[0]);
					//MyLogger.important(j + " geno= " + genotypes[j]);
					ibdWriter.println(i + "\t" +j+"\t" +ibdW.getSegmentNum()+"\t"+ibdW.getMeanLength()+"\t20\t20");
				}
			}
		}
		ibdWriter.close();
	}


	private static void writeIpedIBDFile(Genotype[] genotypes,File ibdFile) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(ibdFile);
		Haplotype windows = new Haplotype();
		for(int chr=1;chr<=22;chr++){
			for(int pos=1;pos<=HumenGenome.getChrLength(chr);pos+=1000000){
				HapRegion window = new HapRegion(new Location(chr,pos), new Location(chr,pos+1000000), "");
				windows.addRegion(window);
			}
		}

		for(Genotype g : genotypes){
			printHapoltypeWindows(printWriter, windows, g.getHap1());
			printHapoltypeWindows(printWriter, windows, g.getHap2());

		}
		printWriter.close();
	}

	private static void printHapoltypeWindows(PrintWriter printWriter,
			Haplotype windows, Haplotype hap1) {

		windows.rewind();
		HapRegion window = windows.getCurrRegion();
		HapRegion r = hap1.getCurrRegion();
		//MyLogger.important("r=" + r);

		while(windows.hasMore()){

			//skip regions that end before window
			while(r.getEnd().compareTo(window.getStart())<0){
				r=hap1.getNextRegion();
				MyLogger.important("skip r=" + r);

			}

			//transpass all intersecting regions
			List<HapRegion> intersect = new ArrayList<HapRegion>();
			while(r.getStart().compareTo(window.getEnd())<0){

				Location start = r.getStart();
				if(start.compareTo(window.getStart())<0)
					start=window.getStart();

				Location end = r.getEnd();
				if(end.compareTo(window.getEnd())>0)
					end=window.getEnd();

				HapRegion res = new HapRegion(start,end,r.getAncestry());
				intersect.add(res);
				if(r.getEnd().compareTo(window.getEnd())>0)
					break;

				r=hap1.getNextRegion();
				//MyLogger.important("check next intersecting r=" + r);

			}
			Map <String, Integer> hapLengths = new HashMap<String,Integer>();
			for(HapRegion in : intersect){
				String ancestor = in.getAncestry();
				int length =  in.getEnd().getPosition()-in.getStart().getPosition();
				if(hapLengths.containsKey(ancestor))
					hapLengths.put(ancestor, hapLengths.get(ancestor) + length);
				else
					hapLengths.put(ancestor, length);
				//	MyLogger.important("intersect " + window + " : " + in);
			}
			int maxLength=0;
			String maxAncestor="";
			for(String key : hapLengths.keySet()){
				if(hapLengths.get(key)>maxLength){
					maxLength=hapLengths.get(key);
					maxAncestor=key;
				}
			}
			String [] split = maxAncestor.split("\\.");
			printWriter.print( split[0] + split[1] + " ");
			//MyLogger.important(window.getStart().getChr() + " " + window.getStart().getPosition() + " " + window.getEnd().getPosition() + " " + maxAncestor);

			window=windows.getNextRegion();

		}
		printWriter.print("\n");

	}

}
