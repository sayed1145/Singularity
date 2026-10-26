package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.2 unit rig: the prism monolith.
 *
 * <p>Deliberately not a walker and not a tank hull. The silhouette is one slab - a hexagonal armour deck with
 * a cut-back prow - riding three lift pods (two front, one rear), with a single lance turret on the spine and
 * two small air pods on the shoulders. No tracks, no legs, no floating parts: every piece is bolted to the
 * body or to the turret bone it belongs to, and the pods sit under the deck inside its own outline.
 */
public final class CommandUnits{
    private CommandUnits(){}

    static final Color prism = Color.valueOf("b69cff");

    public static Rig prismMonolith(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1.4f, 6.2f);
        int bar = r.bone(tur, 0, 2.0f, 1.3f);
        int podL = r.bone(body, -5.6f, 2.4f, 5.4f);
        int podR = r.bone(body, 5.6f, 2.4f, 5.4f);

        //main slab: wide at the back, cut back at the prow, sloped top deck
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -7.4f, -11.0f, 2.3f, 7.4f, -11.0f, 2.3f, 4.6f, 12.4f, 2.3f, -4.6f, 12.4f, 2.3f,
            -6.3f, -9.8f, 5.6f, 6.3f, -9.8f, 5.6f, 3.6f, 10.6f, 5.2f, -3.6f, 10.6f, 5.2f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //prow wedge and the two side strakes
        m = r.part(body);
        hull2(m, s).taper(0, 9.0f, 5.25f, 3.5f, 3.4f, 6.5f, 2.2f, 1.8f, 0, -0.8f, false);
        r.on(hullI);
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            trim(m, s).box(sd * 6.4f - 0.45f, -8.6f, 2.6f, sd * 6.4f + 0.45f, 8.4f, 4.6f);
            r.on(hullI).detail();
        }

        //three lift pods under the deck: two forward, one aft, each a ring with a glowing throat
        float[][] pods = {{-5.2f, 6.0f}, {5.2f, 6.0f}, {0f, -8.0f}};
        for(float[] p : pods){
            m = r.part(body);
            dark(m, s).at(p[0], p[1], 0).ring(10, 1.45f, 2.9f, 0.4f, 2.5f);
            r.shadow(1);
            int pod = r.lastIndex();
            m = r.part(body);
            steel(m, s).at(p[0], p[1], 0).ring(10, 1.5f, 2.1f, 2.4f, 2.8f);
            r.on(pod).detail();
            m = r.part(body);
            glow(m, s).at(p[0], p[1], 0).lathe(10, 0, 0.001f, 0.3f, 1.35f, 0.9f, 1.35f, 1.7f, 0.001f, 2.2f);
            r.on(pod).detail();
        }

        //team strip on the rear deck, and a plain vent block behind the turret
        m = r.part(body);
        team(m).box(-2.4f, -9.4f, 5.61f, 2.4f, -7.6f, 5.66f);
        r.on(hullI).detail();
        m = r.part(body);
        grate(m, s).box(-3.0f, -7.0f, 5.6f, 3.0f, -4.4f, 6.5f);
        r.on(hullI).detail();

        //turret drum on the spine
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).lathe(12, 15f, 0.001f, -0.6f, 4.4f, -0.6f, 4.4f, 1.6f, 3.2f, 2.6f, 0.001f, 2.6f);
        r.on(hullI);
        int drum = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 0).cbevel(0f, -2.2f, 2.0f, 1.8f, 1.2f, 0.75f, 0.15f);
        r.on(drum).detail();

        //the lance: a long square rail with three collars and an emitter mouth
        m = r.part(bar);
        steel(m, s).at(0, 1.4f, 0f).rot(0, -90).cyl(8, 1.15f, 0f, 9.6f);
        r.on(drum);
        int barI = r.lastIndex();
        for(int i = 0; i < 3; i++){
            m = r.part(bar);
            trim(m, s).at(0, 3.4f + i * 2.9f, 0f).rot(0, -90).lathe(8, 0, 0.001f, 0f, 1.55f, 0f, 1.55f, 0.8f, 1.15f, 1.0f, 0.001f, 1.0f);
            r.on(barI).detail();
        }
        m = r.part(bar);
        dark(m, s).at(0, 10.4f, 0f).rot(0, -90).lathe(10, 0, 0.001f, 0f, 1.5f, 0f, 1.5f, 1.3f, 0.95f, 1.5f, 0.001f, 1.5f);
        r.on(barI).detail();
        m = r.part(bar);
        glow(m, s).at(0, 11.9f, 0f).rot(0, -90).lathe(8, 0, 0.001f, 0f, 0.62f, 0f, 0.001f, 0.75f);
        r.on(barI).detail();
        t.weapon(0, tur, bar, 0, 12.2f, 1.3f);

        //two shoulder pods for air cover, one per side, each on its own yaw bone
        int[] podBones = {podL, podR};
        for(int i = 0; i < 2; i++){
            int b = podBones[i];
            m = r.part(b);
            hull2(m, s).at(0, 0, 0).cbevel(0, 0, 0, 1.5f, 1.7f, 1.0f, 0.22f);
            r.on(hullI);
            int shell = r.lastIndex();
            m = r.part(b);
            steel(m, s).at(0, 1.1f, 0.2f).rot(0, -90).cyl(8, 0.42f, 0f, 2.6f);
            r.on(shell).detail();
            m = r.part(b);
            glow(m, s).at(0, 3.7f, 0.2f).rot(0, -90).lathe(6, 0, 0.001f, 0f, 0.3f, 0f, 0.001f, 0.4f);
            r.on(shell).detail();
            //mounts 1 and 2 are the mirrored pod pair
            t.weapon(i + 1, b, b, 0, 3.9f, 0.2f);
        }

        r.half = 12.6f;
        r.height = 9.0f;
        return r.finish();
    }
}
