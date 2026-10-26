package blackhole.g3d;

/**
 * Keeps articulated limbs (legs, arms) of one model from ever crossing each other.
 *
 * <p>Vanilla leg simulation only clamps the <i>distance</i> of a planted foot from its hip, never its
 * <i>direction</i>: when the body turns while a foot stays planted (turning on the move, back-pedalling,
 * strafing), a foot can drift into the neighbouring leg's quadrant and the two legs visibly cross.
 *
 * <p>Every limb therefore owns an angular cone whose apex is its own hip (rest direction +- halfWidth). The
 * knee always lies in the vertical plane through hip and foot, so the whole ground projection of the limb is a
 * ray segment from the hip inside that cone; the cones of neighbouring hips diverge, so limbs cannot intersect
 * by construction. {@link #crossing} is the independent check used
 * by the regression test (tools/preview/LegTest).
 */
public final class LegGuard{
    private LegGuard(){}

    /**
     * Constrains a foot target in the body frame (x right, y forward) to its sector.
     * @param restDeg rest direction, degrees, measured like atan2(x, y) (0 = forward, 90 = right)
     * @param halfWidth half opening of the sector, degrees (must be < half the angle to the neighbour)
     * @param rMin minimum horizontal distance from the body centre
     * @param rMax maximum horizontal distance from the body centre
     * @param out receives {x, y}
     */
    public static void clamp(float x, float y, float restDeg, float halfWidth, float rMin, float rMax, float[] out){
        float r = (float)Math.sqrt(x * x + y * y);
        float a = (float)Math.toDegrees(Math.atan2(x, y));
        float d = a - restDeg;
        d = d % 360f;
        if(d > 180f) d -= 360f;
        if(d < -180f) d += 360f;
        if(d > halfWidth) d = halfWidth;
        if(d < -halfWidth) d = -halfWidth;
        if(r < rMin) r = rMin;
        if(r > rMax) r = rMax;
        double t = Math.toRadians(restDeg + d);
        out[0] = (float)(Math.sin(t) * r);
        out[1] = (float)(Math.cos(t) * r);
    }

    /** True if segment p1-p2 intersects segment p3-p4 (2D, proper or touching). */
    public static boolean segments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4){
        float d1 = cross(x3, y3, x4, y4, x1, y1), d2 = cross(x3, y3, x4, y4, x2, y2);
        float d3 = cross(x1, y1, x2, y2, x3, y3), d4 = cross(x1, y1, x2, y2, x4, y4);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static float cross(float ax, float ay, float bx, float by, float px, float py){
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    /**
     * Regression check: limbs given as ground-projected polylines (hip, knee, foot) = 6 floats each.
     * Returns the index pair packed as i*100+j of the first crossing, or -1.
     */
    public static int crossing(float[][] limbs){
        for(int i = 0; i < limbs.length; i++){
            for(int j = i + 1; j < limbs.length; j++){
                float[] a = limbs[i], b = limbs[j];
                for(int s = 0; s < 2; s++){
                    for(int t = 0; t < 2; t++){
                        if(segments(a[s * 2], a[s * 2 + 1], a[s * 2 + 2], a[s * 2 + 3], b[t * 2], b[t * 2 + 1], b[t * 2 + 2], b[t * 2 + 3])){
                            return i * 100 + j;
                        }
                    }
                }
            }
        }
        return -1;
    }
}
