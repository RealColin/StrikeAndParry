package realcolin.strikeandparry.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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

        var started = state.startAttack(attackSpeed, attackSet);
        if (!started)
            return;

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(attacker, new PlayAnimationPayload(attacker.getId(), state.getAttack().id(), attackSpeed));
    }

    public static void tickCombat(LivingEntity entity) {
        var state = entity.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT);

        var attack = state.tickAttack();
        if (attack == null)
            return;

        applyAttack(entity, attack);
    }

    public static void applyAttack(LivingEntity attacker, Attack attack) {
        var targets = attack.shape().getTargets(attacker);

        for (var target : targets) {
            applyDamage(attacker, target, attack);
        }
    }

    private static void applyDamage(LivingEntity attacker, Entity target, Attack attack) {
        if (!(attacker.level() instanceof ServerLevel level) || !TargetSelector.canDamage(attacker, target))
            return;

        var damage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * attack.damageMultiplier();

        var damageSource = attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker);

        var hit = target.hurtServer(level, damageSource, (float)damage);

        if (hit && target instanceof LivingEntity livingTarget) {
            var stack = attacker.getMainHandItem();
            stack.hurtEnemy(livingTarget, attacker);
            stack.postHurtEnemy(livingTarget, attacker);
        }
    }
}
