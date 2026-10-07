package blackhole.g3d;

/**
 * Real pinhole perspective camera shared by the in-game renderer and the offline baker.
 *
 * <p>The camera sits at (0, cy, D) above the building (south of its centre) and its image plane is parallel
 * to the ground ("shift lens"). This is a genuine 3D perspective projection, but the ground plane z=0 maps
 * 1:1 onto Mindustry's tile grid, so every model stays perfectly aligned with its footprint while heights
 * lean away from the camera with correct foreshortening. Along any pixel ray, a larger z is nearer to the
 * camera, which gives the baker an exact depth test and the live renderer an exact painter order.
 */
public final class Cam{
    public static final float distRatio = 7f, offsetRatio = 4.9f;
    public final float half, D, cy;

    public Cam(float half){
        this.half = half;
        D = half * distRatio;
        cy = -half * offsetRatio;
    }

    public float scale(float z){
        return D / (D - z);
    }

    public float sx(float x, float z){
        return x * D / (D - z);
    }

    public float sy(float y, float z){
        return cy + (y - cy) * D / (D - z);
    }

    /** Exact perspective back-face test: is the plane with normal n through p facing the camera? */
    public boolean facing(float nx, float ny, float nz, float px, float py, float pz){
        return facingValue(nx, ny, nz, px, py, pz) > 0f;
    }

    /**
     * v7.6: the signed result of the same perspective test. The renderer needs the magnitude, not just the sign:
     * a quad whose value sits within a hair of zero is edge-on, where floating point noise in the transform can
     * flip the sign from frame to frame and make the face blink. Callers keep those faces instead of culling.
     */
    public float facingValue(float nx, float ny, float nz, float px, float py, float pz){
        return -nx * px + ny * (cy - py) + nz * (D - pz);
    }

    /** Highest z that is still inside the footprint at north edge y (keeps sprites inside their tiles). */
    public float maxHeightAt(float y){
        return D * (half - y) / (half - cy);
    }
}
