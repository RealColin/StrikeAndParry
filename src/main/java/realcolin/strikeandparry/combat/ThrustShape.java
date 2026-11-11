package realcolin.strikeandparry.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ThrustShape(double reach, double radius) implements AttackShape {

    public ThrustShape {
        if (!Double.isFinite(reach) || !Double.isFinite(radius) || radius < 0.0 || reach < radius)
            throw new IllegalArgumentException("Invalid dimensions for ThrustShape");
    }

    @Override
    public List<LivingEntity> getTargets(LivingEntity attacker) {
        return getCandidates(attacker)
                .stream()
                .filter(target -> intersects(attacker, target))
                .toList();
    }

    @Override
    public Vec3 support(LivingEntity attacker, Vec3 direction) {
        var forward = attacker.getLookAngle().normalize();
        var origin = attacker.getBoundingBox().getCenter();

        var length = direction.length();
        var dir = length > 0.0
                ? direction.scale(1.0 / length)
                : forward;

        var centerlineLength = reach - radius;

        var point = direction.dot(forward) >= 0.0
                ? origin.add(forward.scale(centerlineLength))
                : origin;

        return point.add(dir.scale(radius));
    }
}
