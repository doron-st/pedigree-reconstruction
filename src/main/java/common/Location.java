package common;

public class Location implements Comparable<Location> {
    private int chr;
    private int position;

    public Location(int chr, int position) {
        this.setChr(chr);
        this.setPosition(position);
    }

    public int getChr() {
        return chr;
    }

    public void setChr(int chr) {
        this.chr = chr;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int compareTo(Location other) {
        if (this.chr < other.chr)
            return -1;
        else if (this.chr > other.chr)
            return 1;
        else return Integer.compare(this.position, other.position);
    }

    public String toString() {
        return "(" + chr + ":" + position + ")";
    }

}
