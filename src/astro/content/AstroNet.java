package astro.content;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;

import static astro.content.DetainerType.*;

/**
 * Network glue of the pilot controls. Plain string packets through the vanilla custom-packet API
 * ({@code Call.serverPacketReliable} / {@code Call.clientPacketReliable}), so nothing has to be registered and it works on
 * a dedicated server, on a LAN host and in single player, on the desktop and on Android.
 *
 * <pre>
 * client -> server   astro-cfg   pro,manualMask
 *                    astro-act   ability,aimX,aimY,kind
 * server -> clients  astro-ev    unitId,code,a,b,c,d      (a command that every side has to know: the weapons and arms run on all sides)
 * server -> pilot    astro-st    status for the panel
 * </pre>
 */
public final class AstroNet{
    private AstroNet(){}

    public static final String CFG = "astro-cfg", ACT = "astro-act", EV = "astro-ev", ST = "astro-st";
    public static final int EV_CFG = 0, EV_REQ = 1, EV_ORB = 2, EV_WARP_START = 3, EV_WARP_GO = 4;

    static boolean inited;
    /** the newest status received by the local pilot (client side, read by the panel) */
    public static volatile Status status = new Status();
    /** test hooks */
    public static int eventsSent, actsHandled, cfgHandled;

    public static class Status{
        public int unit = -1;
        public boolean pro, aegis, orb;
        public int manual, held;
        public float energy, maxEnergy = 3000f, charge, warpCd, costEnemy = 1f, costHome = 1f, hp = 1f, shield, wind;
        public final int[] st = new int[AB_N];
        public float time;
        public boolean enemyOk, homeOk;
    }

    public static void init(){
        if(inited) return;
        inited = true;
        if(Vars.netServer != null){
            Vars.netServer.addPacketHandler(CFG, AstroNet::serverCfg);
            Vars.netServer.addPacketHandler(ACT, AstroNet::serverAct);
        }
        if(Vars.netClient != null){
            Vars.netClient.addPacketHandler(EV, AstroNet::clientEvent);
            Vars.netClient.addPacketHandler(ST, AstroNet::clientStatus);
        }
        Events.run(Trigger.update, AstroNet::tick);
    }

    // ---------------------------------------------------------------------------------------------------
    // client -> server
    // ---------------------------------------------------------------------------------------------------

    /** send a packet to the server; in single player / on a host it is handled right here */
    public static void toServer(String type, String payload){
        if(Vars.net != null && Vars.net.client()) Call.serverPacketReliable(type, payload);
        else if(Vars.player != null){
            if(type.equals(CFG)) serverCfg(Vars.player, payload);
            else if(type.equals(ACT)) serverAct(Vars.player, payload);
        }
    }

    static Unit pilotUnit(Player p){
        if(p == null) return null;
        Unit u = p.unit();
        return u != null && !u.dead && u.isAdded() && u.type instanceof DetainerType ? u : null;
    }

    static float[] nums(String s, int n){
        float[] v = new float[n];
        String[] parts = s.split(",");
        for(int i = 0; i < n && i < parts.length; i++){
            try{ v[i] = Float.parseFloat(parts[i].trim()); }catch(Exception ignored){}
        }
        return v;
    }

    /** pro / auto mode and which abilities the pilot keeps for himself */
    public static void serverCfg(Player p, String s){
        Unit u = pilotUnit(p);
        if(u == null) return;
        cfgHandled++;
        float[] v = nums(s, 2);
        boolean pro = v[0] > 0.5f;
        int mask = (int)v[1];
        DetainerType t = (DetainerType)u.type;
        setCfg(t, u, pro, mask);
        event(u, EV_CFG, pro ? 1 : 0, t.logic(u).manual, 0, 0);
    }

    /** the warp always belongs to the pilot; in auto mode nothing else does */
    public static void setCfg(DetainerType t, Unit u, boolean pro, int mask){
        Logic L = t.logic(u);
        int all = (1 << AB_N) - 1;
        L.pro = pro;
        L.manual = pro ? ((mask & all) | (1 << AB_WARP)) : (1 << AB_WARP);
        if(!pro){
            L.orbOn = false;
            for(int a = 0; a < AB_N; a++) L.req[a] = 0f;
        }
    }

    /** ability,aimX,aimY,kind */
    public static void serverAct(Player p, String s){
        Unit u = pilotUnit(p);
        if(u == null) return;
        Logic L = ((DetainerType)u.type).logic(u);
        if(Time.time - L.lastAct < 2f) return; //flood guard
        L.lastAct = Time.time;
        actsHandled++;
        float[] v = nums(s, 4);
        DetainerCtl.act((DetainerType)u.type, u, (int)v[0], v[1], v[2], (int)v[3]);
    }

    // ---------------------------------------------------------------------------------------------------
    // events: sent by the server, applied on every side (the server applies them itself)
    // ---------------------------------------------------------------------------------------------------

    static void broadcast(Unit u, int code, float a, float b, float c, float d){
        eventsSent++;
        if(Vars.net != null && Vars.net.server()){
            Call.clientPacketReliable(EV, u.id + "," + code + "," + a + "," + b + "," + c + "," + d);
        }
    }

    static void event(Unit u, int code, float a, float b, float c, float d){
        apply(u, code, a, b, c, d);
        broadcast(u, code, a, b, c, d);
    }

    public static void req(Unit u, int ab, Teamc target){
        boolean unit = target instanceof Unit;
        event(u, EV_REQ, ab, unit ? ((Unit)target).id : -1, target == null ? 0 : target.getX(), target == null ? 0 : target.getY());
    }

    public static void orb(Unit u, boolean on){
        event(u, EV_ORB, on ? 1 : 0, 0, 0, 0);
    }

    public static void warpStart(Unit u){
        event(u, EV_WARP_START, 0, 0, 0, 0);
    }

    public static void warpGo(Unit u, float fx, float fy, float tx, float ty){
        //the blast and the positions are carried by the event, the effects are local
        apply(u, EV_WARP_GO, fx, fy, tx, ty);
        broadcast(u, EV_WARP_GO, fx, fy, tx, ty);
    }

    static void clientEvent(String s){
        if(Vars.net != null && Vars.net.server()) return; //a host already applied it
        String[] p = s.split(",");
        if(p.length < 6) return;
        try{
            Unit u = Groups.unit.getByID(Integer.parseInt(p[0].trim()));
            if(u == null) return;
            apply(u, Integer.parseInt(p[1].trim()), Float.parseFloat(p[2]), Float.parseFloat(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        }catch(Exception ignored){}
    }

    static void apply(Unit u, int code, float a, float b, float c, float d){
        if(u == null || !(u.type instanceof DetainerType t)) return;
        Logic L = t.logic(u);
        switch(code){
            case EV_CFG -> { L.pro = a > 0.5f; L.manual = (int)b; }
            case EV_REQ -> {
                Teamc tg = null;
                if(b >= 0f) tg = Groups.unit.getByID((int)b);
                else if(a != AB_DRAIN && a != AB_RELEASE){
                    var bd = Vars.world.buildWorld(c, d);
                    if(bd != null && bd.team != u.team) tg = bd;
                }
                DetainerCtl.applyReq(t, u, L, (int)a, tg);
            }
            case EV_ORB -> DetainerCtl.applyOrb(L, a > 0.5f);
            case EV_WARP_START -> {
                L.warpWind = WARP_WIND;
                if(!Vars.headless) AstroFx.warpCharge.at(u.x, u.y, u.rotation, AstroFx.gold, u);
            }
            case EV_WARP_GO -> {
                L.warpWind = 0f;
                L.arrive = 1f;
                L.depart = 1f;
                L.wFromX = a; L.wFromY = b; L.wToX = c; L.wToY = d;
                if(Vars.net != null && Vars.net.client()){
                    //remote clients: jump at once instead of gliding there
                    u.set(c, d);
                    if(!u.isLocal()) u.snapInterpolation();
                }
                if(!Vars.headless){
                    AstroFx.warpOut.at(a, b, u.rotation, AstroFx.gold);
                    AstroFx.warpStreak.at(a, b, Angles.angle(a, b, c, d), AstroFx.gold, new arc.math.geom.Vec2(c, d));
                    AstroFx.warpIn.at(c, d, u.rotation, AstroFx.gold);
                }
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // status: the server tells every pilot what his Detainer can do right now
    // ---------------------------------------------------------------------------------------------------

    static float nextStatus;
    static final int[] tmpStates = new int[AB_N];

    static void tick(){
        if(Vars.state == null || !Vars.state.isGame()) return;
        if(Vars.net != null && Vars.net.client()) return;
        if(Time.time < nextStatus) return;
        nextStatus = Time.time + 6f;
        for(Player p : Groups.player){
            Unit u = pilotUnit(p);
            if(u == null) continue;
            String msg = statusOf((DetainerType)u.type, u);
            if(p.con != null) Call.clientPacketUnreliable(p.con, ST, msg);
            else clientStatus(msg);
        }
    }

    public static String statusOf(DetainerType t, Unit u){
        Logic L = t.logic(u);
        DetainerCtl.states(t, u, L, tmpStates);
        StringBuilder sb = new StringBuilder();
        sb.append(u.id).append('|').append(L.pro ? 1 : 0).append('|').append(L.manual).append('|').append((int)L.energy).append('|')
          .append((int)t.maxEnergy).append('|').append((int)(L.warpCharge * 1000f)).append('|').append((int)L.warpCd).append('|');
        //cost of the two kinds that depend on the world
        float ce = 1f, ch = 1f;
        boolean eo = false, ho = false;
        if(Vars.world != null && Vars.world.width() > 0){
            float[] a = new float[2];
            DetainerCtl.aimOf(u, a);
            if(DetainerCtl.warpDest(t, u, WARP_ENEMY, a[0], a[1])){ eo = true; ce = DetainerCtl.warpCost(u.dst(DetainerCtl.wd[0], DetainerCtl.wd[1])); }
            if(DetainerCtl.warpDest(t, u, WARP_HOME, 0, 0)){ ho = true; ch = DetainerCtl.warpCost(u.dst(DetainerCtl.wd[0], DetainerCtl.wd[1])); }
        }
        sb.append((int)(ce * 1000f)).append('|').append((int)(ch * 1000f)).append('|').append(eo ? 1 : 0).append(ho ? 1 : 0).append('|');
        for(int i = 0; i < AB_N; i++) sb.append((char)('0' + tmpStates[i]));
        sb.append('|').append(L.aegisOn ? 1 : 0).append('|').append(L.orbOn ? 1 : 0).append('|').append(L.heldCount()).append('|')
          .append((int)(u.health / u.maxHealth * 1000f)).append('|').append((int)u.shield).append('|').append((int)L.warpWind);
        return sb.toString();
    }

    static void clientStatus(String s){
        try{
            String[] p = s.split("\\|");
            Status st = new Status();
            st.unit = Integer.parseInt(p[0]);
            st.pro = p[1].equals("1");
            st.manual = Integer.parseInt(p[2]);
            st.energy = Integer.parseInt(p[3]);
            st.maxEnergy = Integer.parseInt(p[4]);
            st.charge = Integer.parseInt(p[5]) / 1000f;
            st.warpCd = Integer.parseInt(p[6]);
            st.costEnemy = Integer.parseInt(p[7]) / 1000f;
            st.costHome = Integer.parseInt(p[8]) / 1000f;
            st.enemyOk = p[9].charAt(0) == '1';
            st.homeOk = p[9].charAt(1) == '1';
            for(int i = 0; i < AB_N && i < p[10].length(); i++) st.st[i] = p[10].charAt(i) - '0';
            st.aegis = p[11].equals("1");
            st.orb = p[12].equals("1");
            st.held = Integer.parseInt(p[13]);
            st.hp = Integer.parseInt(p[14]) / 1000f;
            st.shield = Integer.parseInt(p[15]);
            st.wind = Integer.parseInt(p[16]);
            st.time = Time.time;
            status = st;
        }catch(Exception ignored){}
    }
}
