package realcolin.strikeandparry.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import realcolin.strikeandparry.StrikeandParry;
import realcolin.strikeandparry.network.PlayAnimationPayload;

public class CombatController {

    public static void startAttack(LivingEntity attacker) {
        if (!attacker.isAlive() || attacker.isSpectator())
            return;

        var state = attacker.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT);
        var attackSpeed = attacker.getAttributeValue(Attributes.ATTACK_SPEED);
        var attackSet = AttackSets.resolve(attacker);

        var started = state.startAttack(attackSpeed, attackSet, attacker);
        if (!started)
            return;

        var animSpeed = 20.0 / state.getAttackDurationTicks();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(attacker, new PlayAnimationPayload(attacker.getId(), state.getAttack().id(), animSpeed));
    }

    public static void tickCombat(LivingEntity entity) {
        var state = entity.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT);

        if (!entity.isAlive() || entity.isSpectator()) {
            state.reset();
            return;
        }

        // Keep these through the damage tick, even if tickAttack finishes and clears the state.
        var weapon = state.getAttackWeapon();
        var weaponSnapshot = state.getAttackWeaponSnapshot();
        var slot = state.getAttackSlot();
        if (state.isAttacking() && !matchesAttackWeapon(entity, weapon, weaponSnapshot, slot)) {
            state.reset();
            return;
        }

        var attack = state.tickAttack();
        if (attack == null)
            return;

        applyAttack(entity, attack, weapon, weaponSnapshot, slot);
    }

    private static boolean matchesAttackWeapon(LivingEntity attacker, ItemStack weapon, ItemStack snapshot, int slot) {
        if (attacker instanceof Player player && player.getInventory().getSelectedSlot() != slot)
            return false;

        var current = attacker.getMainHandItem();
        if (snapshot.isEmpty())
            return current.isEmpty();

        // Identity rejects identical replacements; durability wear from hits is allowed.
        return current == weapon && ItemStack.matchesIgnoringComponents(current, snapshot,
                component -> component == DataComponents.DAMAGE);
    }

    private static void applyAttack(LivingEntity attacker, Attack attack, ItemStack weapon, ItemStack weaponSnapshot, int slot) {
        var targets = attack.shape().getTargets(attacker);

        for (var target : targets) {
            if (!matchesAttackWeapon(attacker, weapon, weaponSnapshot, slot)) {
                attacker.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).reset();
                return;
            }

            applyDamage(attacker, target, attack);
        }
    }

    private static void applyDamage(LivingEntity attacker, Entity target, Attack attack) {
        if (!(attacker.level() instanceof ServerLevel level) || !TargetSelector.canDamage(attacker, target))
            return;

        var damage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * attack.damageMultiplier();

        var damageSource = attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker);

        var stack = attacker.getMainHandItem();
        var hit = target.hurtServer(level, damageSource, (float)damage);

        if (hit && target instanceof LivingEntity livingTarget) {
            stack.hurtEnemy(livingTarget, attacker);
            stack.postHurtEnemy(livingTarget, attacker);
        }
    }
}
