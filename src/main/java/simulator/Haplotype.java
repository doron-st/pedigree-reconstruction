package simulator;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Haplotype {
	private List<HapRegion> linkageGroups;
	private ListIterator<HapRegion> iter;
	private HapRegion currRegion;

	//create empty haplotype
	public Haplotype(){
		linkageGroups = new ArrayList<HapRegion>();
		iter = linkageGroups.listIterator();
	}

	//create founder haplotype
	public Haplotype(String id) {
		linkageGroups = new ArrayList<HapRegion>();
		for(int chr=1;chr<=22;chr++){
			HapRegion chrRegion = new HapRegion(new Location(chr,1), new Location(chr,HumenGenome.getChrLength(chr)),id);
			linkageGroups.add(chrRegion);
		}
		iter = linkageGroups.listIterator();
		currRegion=iter.next();
	}

	public List<HapRegion> getIBDSegments(Haplotype other) {
		rewind();
		other.rewind();
		//MyLogger.debug("myHap=" + this.toString());
		//MyLogger.debug("otHap=" + other.toString());
		List<HapRegion> IBD = new ArrayList<HapRegion>();
		HapRegion otherRegion = other.getCurrRegion();
		//MyLogger.debug("otherRegion=" + otherRegion);

		for(HapRegion currRegion : linkageGroups){
			//MyLogger.debug("currRegion=" + currRegion);
			while(currRegion.getStart().compareTo(otherRegion.getEnd())>0){
				otherRegion=other.getNextRegion();
				//MyLogger.debug("otherRegion=" + otherRegion);
			}

			while(currRegion.getEnd().compareTo(otherRegion.getStart())>0){

				Location start = currRegion.getStart();
				if(start.compareTo(otherRegion.getStart())<0)
					start=otherRegion.getStart();

				Location end = currRegion.getEnd();
				if(end.compareTo(otherRegion.getEnd())>0)
					end=otherRegion.getEnd();

				if(currRegion.getAncestry()==otherRegion.getAncestry()){
					//MyLogger.important("Add IBD region " + start + ","+ end + " " + currRegion.getAncestry());
					IBD.add(new HapRegion(start,end,currRegion.getAncestry()));
				}
				else{
					//MyLogger.debug("Not IBD region " + start + ","+ end);
				}
				if(currRegion.getEnd().compareTo(otherRegion.getEnd())>0){
					otherRegion=other.getNextRegion();
					//MyLogger.debug("otherRegion=" + otherRegion);
				}	
				else break;
			}
		}
		rewind();
		other.rewind();
		//MyLogger.debug("IBD=" + IBD.toString());
		return IBD;
	}

	public HapRegion getNextRegion(){
		currRegion = iter.next();
		return currRegion;
	}
	public HapRegion getCurrRegion(){
		return currRegion;
	}
	public void addRegion(HapRegion r){
		linkageGroups.add(r);
	}
	public boolean hasMore(){
		return iter.hasNext();
	}
	public void rewind(){
		iter = linkageGroups.listIterator();
		currRegion=iter.next();
	}
	public void decreament(){
		iter.previous();
	}
	public String toString(){
		String result="";
		for(HapRegion region : linkageGroups){
			result += region + ", "; 
		}
		return result;
	}	
}
