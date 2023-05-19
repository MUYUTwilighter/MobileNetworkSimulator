package cool.muyucloud.mnsimulator.util;

import cool.muyucloud.mnsimulator.MNSimulator;

import java.util.Random;

public class Pos {
    private static final Random RANDOM = MNSimulator.RANDOM;
    private int x, y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int distanceTo(Pos other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public Pos copy() {
        return new Pos(this.x, this.y);
    }

    public static Pos createRandom(Pos from, Pos to) {
        int x = RANDOM.nextInt(from.x, to.x);
        int y = RANDOM.nextInt(from.y, to.y);
        return new Pos(x, y);
    }
}
