package realcolin.strikeandparry.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record SweepShape(double reach, double sweepAngle, double thickness, double rollAngle) implements AttackShape{
    public Vec3 support(LivingEntity attacker, Vec3 direction) {
        var origin = getOrigin(attacker);

        // Build the sweep's local coordinate system
        var forward = attacker.getLookAngle().normalize();
        var up = attacker.getUpVector(1.0F).normalize();
        var right = forward.cross(up).normalize();

        var roll = Math.toRadians(rollAngle);
        var cosRoll = Math.cos(roll);
        var sinRoll = Math.sin(roll);

        var sweepAxis = right.scale(cosRoll)
                .add(up.scale(sinRoll))
                .normalize();

        var thicknessAxis = up.scale(cosRoll)
                .subtract(right.scale(sinRoll))
                .normalize();

        // Project the requested direction onto our local axes
        var forwardAmount = direction.dot(forward);
        var sweepAmount = direction.dot(sweepAxis);
        var thicknessAmount = direction.dot(thicknessAxis);

        // Find the direction within the sweep plane that points most toward
        // the requested support direction
        var desiredAngle = Math.atan2(sweepAmount, forwardAmount);
        var halfSweep = Math.toRadians(sweepAngle / 2.0);

        var supportAngle = Math.max(-halfSweep, Math.min(halfSweep, desiredAngle));

        var cosAngle = Math.cos(supportAngle);
        var sinAngle = Math.sin(supportAngle);

        // Determine whether moving outward from the origin actually gets us
        // farther in the requested direction
        var planarAmount =
                forwardAmount * cosAngle +
                        sweepAmount * sinAngle;

        Vec3 planarOffset;

        if (planarAmount > 0.0) {
            planarOffset = forward.scale(cosAngle * reach)
                    .add(sweepAxis.scale(sinAngle * reach));
        } else {
            // The requested direction points away from the entire sector,
            // so the center of the sector is furthest.
            planarOffset = Vec3.ZERO;
        }

        // Pick whichever thickness face points farther in the requested direction
        var thicknessOffset =
                thicknessAmount >= 0.0
                        ? thickness / 2.0
                        : -thickness / 2.0;

        return origin
                .add(planarOffset)
                .add(thicknessAxis.scale(thicknessOffset));
    }
}
