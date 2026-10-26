package blackhole.g3d;

/** One light model for baked textures and live polygons, so both halves of every model match exactly. */
public final class Light{
    public static final float lx, ly, lz, hx, hy, hz;
    public static final float ambient = 0.50f, diffuse = 0.54f, specular = 0.20f;

    static{
        float x = -0.50f, y = -0.20f, z = 0.84f;
        float l = (float)Math.sqrt(x * x + y * y + z * z);
        lx = x / l; ly = y / l; lz = z / l;
        //view direction of the camera (south, above), used for a Blinn half vector
        float vx = 0f, vy = -0.447f, vz = 0.894f;
        float ax = lx + vx, ay = ly + vy, az = lz + vz;
        float al = (float)Math.sqrt(ax * ax + ay * ay + az * az);
        hx = ax / al; hy = ay / al; hz = az / al;
    }

    private Light(){}

    /** Hemisphere ambient for a unit normal: faces looking at the sky get the full term, walls a little less. */
    public static float hemi(float nz){
        return ambient * (0.80f + 0.20f * nz);
    }

    /** Diffuse multiplier for a unit normal (hemisphere ambient + Lambert). */
    public static float diffuse(float nx, float ny, float nz){
        float d = nx * lx + ny * ly + nz * lz;
        return hemi(nz) + diffuse * (d > 0f ? d : 0f);
    }

    /**
     * Soft highlight shoulder shared by the baker and the live renderer: linear up to 0.8, then a smooth roll-off
     * that keeps gradation in bright (white hull, specular) areas instead of clipping them flat.
     */
    public static float tone(float c){
        if(c <= 0.8f) return c;
        float t = 1f + (c - 0.8f) * 5f;
        return 0.8f + 0.2f * (1f - 1f / (t * t));
    }

    /** Additive specular term; metal = 0..1 extra shininess. */
    public static float spec(float nx, float ny, float nz, float metal){
        float d = nx * hx + ny * hy + nz * hz;
        if(d <= 0f) return 0f;
        float d2 = d * d, d4 = d2 * d2;
        return (specular + metal * 0.25f) * d4 * d4 * (0.5f + metal * 0.5f);
    }
}
