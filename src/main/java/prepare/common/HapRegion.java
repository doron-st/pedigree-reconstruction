package prepare.common;

public class HapRegion implements Comparable<HapRegion> {
    private Location start;
    private Location end;
    private String ancestry;

    public HapRegion(Location start, Location end, String ancestry) {
        this.setStart(start);
        this.setEnd(end);
        this.setAncestry(ancestry);
    }


    public Location getStart() {
        return start;
    }

    public void setStart(Location start) {
        this.start = start;
    }

    public Location getEnd() {
        return end;
    }

    public void setEnd(Location end) {
        this.end = end;
    }

    public String getAncestry() {
        return ancestry;
    }

    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    public String toString() {
        return "[" + start + "," + end + "," + ancestry + "]";
    }


    @Override
    public int compareTo(HapRegion other) {
        return getStart().compareTo(other.getStart());
    }
}
