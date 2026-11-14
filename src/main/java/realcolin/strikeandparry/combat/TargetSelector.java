package realcolin.strikeandparry.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;

public class TargetSelector {

    /** Shared client/server eligibility; does not check reach, obstruction, or aimed-only rules. */
    public static boolean canTarget(LivingEntity attacker, Entity target) {
        if (attacker == target || attacker.level() != target.level()
                || !attacker.isAlive() || attacker.isSpectator()
                || !target.isAlive() || target.isSpectator()
                || !target.isAttackable() || !target.isPickable())
            return false;

        // Explicitly support damageable combat targets, not dropped items or projectiles.
        return target instanceof LivingEntity
                || target instanceof VehicleEntity
                || target instanceof BlockAttachedEntity
                || target instanceof EndCrystal;
    }

    /**
     * Server-only permission check. The eventual hurtServer call must still enforce
     * damage-source restrictions, invulnerability, and applicable damage-event hooks.
     */
    public static boolean canDamage(LivingEntity attacker, Entity target) {
        if (!(attacker.level() instanceof ServerLevel level) || !canTarget(attacker, target))
            return false;

        if (!level.getWorldBorder().isWithinBounds(target.blockPosition()))
            return false;

        if (attacker instanceof Player player) {
            if (target instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer))
                return false;

            // Mirrors the permission checks normally performed by skipAttackInteraction,
            // without calling that method: it can actually break attached decorations.
            if (target instanceof BlockAttachedEntity attached) {
                if (!level.mayInteract(player, attached.getPos()))
                    return false;
            } else if (target instanceof ArmorStand && !level.mayInteract(player, target.blockPosition())) {
                return false;
            }

            if ((target instanceof Cushion || target instanceof ArmorStand) && !player.mayBuild())
                return false;
        }

        return true;
    }
}
