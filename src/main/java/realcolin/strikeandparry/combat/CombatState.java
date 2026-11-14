package realcolin.strikeandparry.combat;

import net.minecraft.util.Mth;

public class CombatState {


    // mutable state
    private boolean attacking = false;
    private int attackTick = 0;
    private int attackDurationTicks = 0;
    private int damageTick = 0;
    private boolean damageApplied = false;

    private String comboAttackSetId = null;
    private int comboIndex = 0;
    private Attack activeAttack = null;

    public CombatState() {

    }

    public boolean startAttack(double attackSpeed, AttackSet attackSet) {
        if (attacking) return false;
        if (attackSpeed <= 0.0) return false;

        if (!attackSet.id().equals(comboAttackSetId)) {
            comboAttackSetId = attackSet.id();
            comboIndex = 0;
        }

        this.activeAttack = attackSet.get(comboIndex);

        double windup = activeAttack.windupRatio();

        // figure out how many ticks the attack will last, and at which tick damage is applied
        this.attackDurationTicks = Math.max(1, Mth.floor((20.0 / attackSpeed)));
        this.damageTick = Math.max(1, Mth.floor(this.attackDurationTicks * windup));

        // then, finalize the state
        this.attackTick = 0;
        this.damageApplied = false;
        this.attacking = true;

        return true;
    }

    public Attack tickAttack() {
        if (!attacking)
            return null;

        attackTick++;

        Attack toApply = null;

        if (!damageApplied && attackTick >= damageTick) {
            damageApplied = true;
            toApply = activeAttack;
        }

        // attack has finished, reset state
        if (attackTick >= attackDurationTicks)
            finishAttack();

        return toApply;
    }

    private void finishAttack() {
        attacking = false;
        attackTick = 0;
        attackDurationTicks = 0;
        damageTick = 0;
        damageApplied = false;
        activeAttack = null;
        comboIndex++;
    }

    public Attack getAttack() {
        return this.activeAttack;
    }

    public void reset() {
        this.attacking = false;
        this.attackTick = 0;
        this.attackDurationTicks = 0;
        this.damageTick = 0;
        this.damageApplied = false;
        comboIndex = 0;
        comboAttackSetId = null;
        activeAttack = null;
    }

}
