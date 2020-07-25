package prepare.misc;

/**
 * Based on the Combinadic Algorithm explained by James McCaffrey
 * in the MSDN article titled: "Generating the mth Lexicographical
 * Element of a Mathematical Combination"
 * <http://msdn.microsoft.com/en-us/library/aa289166(VS.71).aspx>
 *
 * @author Ahmed Abdelkader
 * Licensed under Creative Commons Attribution 3.0
 * <http://creativecommons.org/licenses/by/3.0/us/>
 */
public class Combinations {
    /**
     * returns the mth lexicographic element of combination C(n,k)
     **/
    public static int[] element(int n, int k, int m) {
        int[] ans = new int[k];

        int a = n;
        int b = k;
        int x = (choose(n, k) - 1) - m;  // x is the "dual" of m

        for (int i = 0; i < k; ++i) {
            a = largestV(a, b, x);          // largest value v, where v < a and vCb < x
            x = x - choose(a, b);
            b = b - 1;
            ans[i] = (n - 1) - a;
        }

        return ans;
    }

    /**
     * returns the largest value v where v < a and Choose(v,b) <= x
     **/
    public static int largestV(int a, int b, int x) {
        int v = a - 1;

        while (choose(v, b) > x)
            --v;

        return v;
    }

    /**
     * returns nCk - watch out for overflows
     **/
    public static int choose(int n, int k) {
        if (n < 0 || k < 0)
            return -1;
        if (n < k)
            return 0;
        if (n == k || k == 0)
            return 1;

        int delta, iMax;

        if (k < n - k) {
            delta = n - k;
            iMax = k;
        } else {
            delta = k;
            iMax = n - k;
        }

        int ans = delta + 1;

        for (int i = 2; i <= iMax; ++i) {
            ans = (ans * (delta + i)) / i;
        }

        return ans;
    }
}