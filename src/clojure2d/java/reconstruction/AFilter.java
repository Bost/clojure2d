package clojure2d.java.reconstruction;

import net.jafama.FastMath;

public abstract class AFilter {
    public double radius, iradius, iradius16;
    public double[] filterTable = new double[16*16];

    protected void init() {
        double r = radius/16.0;
        int off=0;

        for (int x=0; x<16; x++) {
            for (int y=0; y<16; y++) {
                double px = (0.5 + x) * r;
                double py = (0.5 + y) * r;

                filterTable[off] = evaluate(px, py);
                off++;
            }
        }
    }

    public AFilter(double radius) {
        this.radius = FastMath.abs(radius);
        iradius = 1.0 / radius;
        iradius16 = iradius * 16.0;
    }

    public abstract double evaluate(double x, double y);
    public abstract String getName();
}
