package blackhole;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.type.*;

/**
 * v8.0-beta unit control overhaul.
 *
 * <p>Every unit in this mod was shipping with the vanilla default command list, which for most of them was
 * nothing but "move" (plus "enter payload" where the unit fits in a block). A unit that could mine could not
 * be told to mine, a unit that could build could not be told to rebuild or to assist, and nothing mined on
 * its own after it rolled out of the factory.
 *
 * <p>This pass walks every unit the mod owns and derives the command list from what the unit can actually
 * do - mine tier, build speed, payload capacity, healing weapons. It only <b>adds</b> commands and picks a
 * sensible default; no stat, weapon or AI of any unit is changed, so nothing is weakened.
 */
public final class UnitCapabilities{
    /** units whose default command is the work command, not "move" - they start working on their own */
    public static int autoWorkers;
    public static int touched;

    private UnitCapabilities(){}

    public static void apply(){
        touched = 0;
        autoWorkers = 0;
        for(UnitType u : Vars.content.units()){
            if(u == null || !u.name.startsWith("blackhole-")) continue;
            apply(u);
        }
        Log.info("[blackhole] v8.0 指挥能力：已为 @ 个单位补全指令，其中 @ 个出厂即自动作业。", touched, autoWorkers);
    }

    public static void apply(UnitType u){
        boolean canMine = u.mineTier > 0 && (u.mineFloor || u.mineWalls);
        boolean canBuild = u.buildSpeed > 0f;
        boolean canCarry = u.payloadCapacity > 0f;
        boolean heals = healer(u);
        boolean fighter = fighter(u);

        Seq<UnitCommand> cmds = new Seq<>();
        add(cmds, UnitCommand.moveCommand);
        if(canMine) add(cmds, UnitCommand.mineCommand);
        if(canBuild){
            add(cmds, UnitCommand.rebuildCommand);
            add(cmds, UnitCommand.assistCommand);
        }
        if(heals) add(cmds, UnitCommand.repairCommand);
        if(canCarry){
            add(cmds, UnitCommand.enterPayloadCommand);
            add(cmds, UnitCommand.loadUnitsCommand);
            add(cmds, UnitCommand.loadBlocksCommand);
            add(cmds, UnitCommand.unloadPayloadCommand);
            add(cmds, UnitCommand.loopPayloadCommand);
        }else if(u.allowedInPayloads){
            //a unit that fits inside a carrier can still be told to climb in
            add(cmds, UnitCommand.enterPayloadCommand);
        }

        //keep anything the unit already declared - this pass never removes a command
        for(UnitCommand old : u.commands){
            add(cmds, old);
        }
        if(cmds.size <= 1 && u.commands.size <= 1) return;

        u.commands = cmds;
        u.allowChangeCommands = true;
        touched++;

        //a support unit should not sit still waiting for an order: it leaves the factory already working
        UnitCommand def = null;
        //a dedicated miner (fast drill head) works on its own even if it carries a gun for self defence;
        //a line fighter that happens to mine slowly keeps "move" so it does not wander off a defence order
        boolean dedicatedMiner = canMine && (!fighter || u.mineSpeed >= 8f);
        if(!fighter || dedicatedMiner){
            if(dedicatedMiner && cmds.contains(UnitCommand.mineCommand)){
                def = UnitCommand.mineCommand;
            }else if(canBuild){
                def = UnitCommand.rebuildCommand;
            }else if(heals){
                def = UnitCommand.repairCommand;
            }
        }
        if(def != null){
            u.defaultCommand = def;
            autoWorkers++;
        }
    }

    private static void add(Seq<UnitCommand> out, UnitCommand c){
        if(c != null && !out.contains(c)) out.add(c);
    }

    /** true if one of the unit's weapons shoots something that heals - only then is "repair" useful */
    private static boolean healer(UnitType u){
        if(u.canHeal) return true;
        for(Weapon w : u.weapons){
            if(w.bullet != null && (w.bullet.healPercent > 0f || w.bullet.healAmount > 0f)) return true;
        }
        return false;
    }

    /** true if the unit has a weapon that actually damages an enemy - fighters keep "move" as their default */
    private static boolean fighter(UnitType u){
        for(Weapon w : u.weapons){
            if(w.bullet == null) continue;
            if((w.bullet.damage > 0f || w.bullet.splashDamage > 0f) && (u.targetAir || u.targetGround)) return true;
        }
        return false;
    }
}
