package common;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Haplotype {
    private final List<HapRegion> linkageGroups;
    private ListIterator<HapRegion> iterator;
    private HapRegion currRegion;

    //create empty haplotype
    public Haplotype() {
        linkageGroups = new ArrayList<>();
        iterator = linkageGroups.listIterator();
    }

    //create founder haplotype
    public Haplotype(String id) {
        linkageGroups = new ArrayList<>();
        for (int chr = 1; chr <= 22; chr++) {
            HapRegion chrRegion = new HapRegion(new Location(chr, 1), new Location(chr, HumanGenome.getChrLength(chr)), id);
            linkageGroups.add(chrRegion);
        }
        iterator = linkageGroups.listIterator();
        currRegion = iterator.next();
    }

    public List<HapRegion> getIBDSegments(Haplotype other) {
        rewind();
        other.rewind();
        //MyLogger.debug("myHap=" + this.toString());
        //MyLogger.debug("otHap=" + other.toString());
        List<HapRegion> IBD = new ArrayList<>();
        HapRegion otherRegion = other.getCurrRegion();
        //MyLogger.debug("otherRegion=" + otherRegion);

        for (HapRegion currRegion : linkageGroups) {
            //MyLogger.debug("currRegion=" + currRegion);
            while (currRegion.getStart().compareTo(otherRegion.getEnd()) > 0) {
                otherRegion = other.getNextRegion();
                //MyLogger.debug("otherRegion=" + otherRegion);
            }

            while (currRegion.getEnd().compareTo(otherRegion.getStart()) > 0) {

                Location start = currRegion.getStart();
                if (start.compareTo(otherRegion.getStart()) < 0)
                    start = otherRegion.getStart();

                Location end = currRegion.getEnd();
                if (end.compareTo(otherRegion.getEnd()) > 0)
                    end = otherRegion.getEnd();

                if (currRegion.getAncestry().equals(otherRegion.getAncestry())) {
                    //MyLogger.important("Add IBD region " + start + ","+ end + " " + currRegion.getAncestry());
                    IBD.add(new HapRegion(start, end, currRegion.getAncestry()));
                }  //MyLogger.debug("Not IBD region " + start + ","+ end);

                if (currRegion.getEnd().compareTo(otherRegion.getEnd()) > 0) {
                    otherRegion = other.getNextRegion();
                    //MyLogger.debug("otherRegion=" + otherRegion);
                } else break;
            }
        }
        rewind();
        other.rewind();
        //MyLogger.debug("IBD=" + IBD.toString());
        return IBD;
    }

    public HapRegion getNextRegion() {
        currRegion = iterator.next();
        return currRegion;
    }

    public HapRegion getCurrRegion() {
        return currRegion;
    }

    public void addRegion(HapRegion r) {
        linkageGroups.add(r);
    }

    public boolean hasMore() {
        return iterator.hasNext();
    }

    public void rewind() {
        iterator = linkageGroups.listIterator();
        currRegion = iterator.next();
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (HapRegion region : linkageGroups) {
            result.append(region).append(", ");
        }
        return result.toString();
    }
}
