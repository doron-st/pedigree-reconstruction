package simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class Genotype {

	private Haplotype hap1;
	private Haplotype hap2;
	private Random randomgenerator = new Random();

	//create founder genotype
	public Genotype(int founderId){
		hap1=new Haplotype(founderId + ".1");
		hap2=new Haplotype(founderId + ".2");
	}

	public Genotype(Haplotype fatherHaplotype, Haplotype motherHaplotype) {
		hap1 = fatherHaplotype;
		hap2 = motherHaplotype;
	}

	public Haplotype recombine(){
		List<Location> recombPoints = sampleRecombinationLocs();
		recombPoints.add(new Location(24,1));
		ListIterator<Location> iter = recombPoints.listIterator();
		boolean startAtHap1 =  randomgenerator.nextBoolean();

		Haplotype recombHap = new Haplotype();

		Haplotype currHap = hap2;
		if(startAtHap1)
			currHap= hap1;

		Location currStart = new Location(1,1);
		Location nextRecomb=iter.next();

		while(nextRecomb!=null){
			//increament curr haplotype to current position
			while(currStart.compareTo(currHap.getCurrRegion().getEnd())>0)
				currHap.getNextRegion();

			//MyLogger.debug("currStart=" + currStart + "hapEnd=" + currHap.getCurrRegion().getEnd());

			if(nextRecomb.compareTo(currHap.getCurrRegion().getEnd())<0){
				//MyLogger.debug("recombine region " + currHap.getCurrRegion() + " at " + nextRecomb);
				HapRegion block =  new HapRegion(currStart, nextRecomb, currHap.getCurrRegion().getAncestry());
				//MyLogger.debug("Adding block " + block);
				recombHap.addRegion(block);
				currStart=nextRecomb;
				nextRecomb=iter.next();

				//MyLogger.debug("nextRecomb=" + nextRecomb);

				if(currHap.equals(hap1))
					currHap=hap2;
				else
					currHap=hap1;
			}
			else{

				HapRegion block = new HapRegion(currStart, currHap.getCurrRegion().getEnd(), currHap.getCurrRegion().getAncestry());
				//MyLogger.debug("Adding block " + block);

				recombHap.addRegion(block);
				if(currHap.hasMore())
					currHap.getNextRegion();
				else
					break;
				Location previousStart=currStart;
				currStart=currHap.getCurrRegion().getStart();
				if(currStart.getChr()>previousStart.getChr()){
					startAtHap1 =  randomgenerator.nextBoolean();
					currHap = hap2;
					if(startAtHap1)
						currHap= hap1;
				}
			}	
		}

		recombHap.rewind();
		hap1.rewind();
		hap2.rewind();
		return recombHap;
	}
	
	public Haplotype getHap1(){
		return hap1;
	}
	public Haplotype getHap2(){
		return hap2;
	}

	private List<Location> sampleRecombinationLocs() {
		List<Location> locs = new ArrayList<Location>();
		for(int chr=1;chr<=22;chr++){
			int pos= (int) Math.round(-Math.log(1 - randomgenerator.nextDouble()) * 100000000);

			while(pos < HumenGenome.getChrLength(chr)){
				locs.add(new Location(chr, pos));

				//MyLogger.debug("Adding recomb point " + chr + ":" + pos);
				pos += (int) Math.round(-Math.log(1 - randomgenerator.nextDouble())*100000000);

			}
		}
		return locs;
	}

	public String toString(){
		return "\n" + hap1 + " \n" + hap2; 
	}
}
