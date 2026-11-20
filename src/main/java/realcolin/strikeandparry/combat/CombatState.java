package realcolin.strikeandparry.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CombatState {


    // mutable state
    private boolean attacking = false;
    private int attackTick = 0;
    private int attackDurationTicks = 0;
    private int damageTick = 0;
    private boolean damageApplied = false;

    private String attackSetId = null;
    private int attackIndex = 0;
    private Attack activeAttack = null;
    private ItemStack attackWeapon = ItemStack.EMPTY;
    private ItemStack attackWeaponSnapshot = ItemStack.EMPTY;
    private int attackSlot = -1;
    private int ticksUntilIndexReset = 0;

    public CombatState() {

    }

    public boolean startAttack(double attackSpeed, AttackSet attackSet, LivingEntity attacker) {
        if (attacking) return false;
        if (attackSpeed <= 0.0) return false;

        if (!attackSet.id().equals(attackSetId)) {
            attackSetId = attackSet.id();
            attackIndex = 0;
        }

        this.activeAttack = attackSet.get(attackIndex);
        this.attackWeapon = attacker.getMainHandItem();
        this.attackWeaponSnapshot = attackWeapon.copy();
        this.attackSlot = attacker instanceof Player player ? player.getInventory().getSelectedSlot() : -1;

        double windup = activeAttack.windupRatio();

        // figure out how many ticks the attack will last, and at which tick damage is applied
        this.attackDurationTicks = Math.max(1, Mth.floor((20.0 / attackSpeed)));
        this.damageTick = Math.max(1, Mth.floor(this.attackDurationTicks * windup));

        // then, finalize the state
        this.attackTick = 0;
        this.damageApplied = false;
        this.attacking = true;
        ticksUntilIndexReset = 40;

        return true;
    }

    public Attack tickAttack() {
        if (!attacking) {
            if (ticksUntilIndexReset > 0) {
                ticksUntilIndexReset--;

                if (ticksUntilIndexReset == 0)
                    attackIndex = 0;
            }

            return null;
        }

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

    /** Suppresses pending damage without ending the attack or shortening its recovery. */
    public void interruptDamage() {
        if (attacking)
            damageApplied = true;
    }

    private void finishAttack() {
        attacking = false;
        attackTick = 0;
        attackDurationTicks = 0;
        damageTick = 0;
        damageApplied = false;
        activeAttack = null;
        attackWeapon = ItemStack.EMPTY;
        attackWeaponSnapshot = ItemStack.EMPTY;
        attackSlot = -1;
        attackIndex++;
    }

    public Attack getAttack() {
        return this.activeAttack;
    }

    public int getAttackDurationTicks() {
        return this.attackDurationTicks;
    }

    public boolean isAttacking() {
        return attacking;
    }

    ItemStack getAttackWeapon() {
        return attackWeapon;
    }

    ItemStack getAttackWeaponSnapshot() {
        return attackWeaponSnapshot;
    }

    int getAttackSlot() {
        return attackSlot;
    }

    public void reset() {
        this.attacking = false;
        this.attackTick = 0;
        this.attackDurationTicks = 0;
        this.damageTick = 0;
        this.damageApplied = false;
        attackIndex = 0;
        attackSetId = null;
        activeAttack = null;
        attackWeapon = ItemStack.EMPTY;
        attackWeaponSnapshot = ItemStack.EMPTY;
        attackSlot = -1;
        ticksUntilIndexReset = 0;
    }

}
