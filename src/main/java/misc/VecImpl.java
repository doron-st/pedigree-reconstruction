package misc;

import jsat.linear.Vec;

public class VecImpl extends Vec {

    private static final long serialVersionUID = 1L;
    private final double[] arr;

    public VecImpl(double a, double b) {
        arr = new double[]{a, b};
    }

    public VecImpl(double a) {
        arr = new double[]{a};
    }

    @Override
    public Vec clone() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double get(int arg0) {
        return arr[arg0];
    }

    @Override
    public boolean isSparse() {
        return false;
    }

    @Override
    public int length() {
        return arr.length;
    }

    @Override
    public void set(int arg0, double arg1) {
        arr[arg0] = arg1;

    }

}
