package blackhole;

import mindustry.entities.abilities.*;

/**
 * v8.1: named wrappers for the two vanilla abilities this mod configures inline.
 *
 * <p>An anonymous subclass has an empty simple class name, and {@code Ability.localized()} builds its bundle
 * key from exactly that name - so the stat panel showed a blank line where the ability should be. These two
 * subclasses exist only to carry a name.
 */
public final class AureliaAbilities{
    private AureliaAbilities(){}

    /** repair field with a proper display name */
    public static class MendField extends RepairFieldAbility{
        public MendField(float amount, float reload, float range){
            super(amount, reload, range);
        }

        @Override
        public String localized(){
            return arc.Core.bundle.get("ability.mendfield");
        }
    }

    /** passive regeneration with a proper display name */
    public static class Regrowth extends RegenAbility{
        @Override
        public String localized(){
            return arc.Core.bundle.get("ability.regrowth");
        }
    }
}
