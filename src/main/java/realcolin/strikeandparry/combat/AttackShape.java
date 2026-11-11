package realcolin.strikeandparry.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public interface AttackShape {
    double EPSILON = 1e-8;
    double EPSILON_SQR = EPSILON * EPSILON;

    List<LivingEntity> getTargets(LivingEntity attacker);
    double reach();
    Vec3 support(LivingEntity attacker, Vec3 direction);

    default List<LivingEntity> getCandidates(LivingEntity attacker) {
        var searchBox = attacker.getBoundingBox().inflate(reach());

        return attacker.level().getEntitiesOfClass(LivingEntity.class, searchBox, target -> target != attacker);
    }

    default Vec3 supportAABB(LivingEntity target, Vec3 direction) {
        var box = target.getBoundingBox();

        return new Vec3(
                direction.x >= 0.0 ? box.maxX : box.minX,
                direction.y >= 0.0 ? box.maxY : box.minY,
                direction.z >= 0.0 ? box.maxZ : box.minZ
        );
    }

    default boolean intersects(LivingEntity attacker, LivingEntity target) {
        final int MAX_ITERATIONS = 32;

        var simplex = new ArrayList<Vec3>();

        var origin = attacker.getBoundingBox().getCenter();
        var direction = target.getBoundingBox().getCenter().subtract(origin);

        if (direction.lengthSqr() < EPSILON_SQR)
            direction = new Vec3(1.0, 0.0, 0.0);

        var point = support(attacker, direction).subtract(supportAABB(target, direction.scale(-1.0)));

        simplex.addFirst(point);
        direction = point.scale(-1.0);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (direction.lengthSqr() < EPSILON_SQR)
                return true;

            point = support(attacker, direction)
                    .subtract(
                            supportAABB(target, direction.scale(-1.0))
                    );

            if (point.dot(direction) < -EPSILON)
                return false;

            simplex.addFirst(point);

            if (simplex.size() > 4)
                throw new IllegalStateException("GJK simplex exceeded 4 points");

            if (simplex.size() == 2) {
                var result = handleLine(simplex);

                if (result.containsOrigin())
                    return true;

                direction = result.direction();
            }

            else if (simplex.size() == 3) {
                var result = handleTriangle(simplex);

                if (result.containsOrigin())
                    return true;

                direction = result.direction();
            }

            else if (simplex.size() == 4) {
                var result = handleTetrahedron(simplex);

                if (result.containsOrigin())
                    return true;

                direction = result.direction();
            }
        }

        return false;
    }

    private SimplexResult handleLine(List<Vec3> simplex) {
        var a = simplex.get(0);
        var b = simplex.get(1);

        var ab = b.subtract(a);
        var ao = a.scale(-1.0);

        if (ab.lengthSqr() < EPSILON_SQR) {
            simplex.clear();
            simplex.add(a);

            return new SimplexResult(false, ao);
        }

        if (ab.dot(ao) > 0.0) {
            var direction = ab.cross(ao).cross(ab);

            if (direction.lengthSqr() < EPSILON_SQR) {
                double t = ao.dot(ab) / ab.lengthSqr();

                if (t >= 0.0 && t <= 1.0)
                    return new SimplexResult(true, Vec3.ZERO);

                simplex.clear();
                simplex.add(b);

                return new SimplexResult(false, b.scale(-1.0));
            }

            return new SimplexResult(false, direction);
        }

        simplex.clear();
        simplex.add(a);

        return new SimplexResult(false, ao);
    }

    private SimplexResult handleTriangle(List<Vec3> simplex) {
        var a = simplex.get(0);
        var b = simplex.get(1);
        var c = simplex.get(2);

        var ab = b.subtract(a);
        var ac = c.subtract(a);
        var ao = a.scale(-1.0);

        var abc = ab.cross(ac);

        if (abc.lengthSqr() < EPSILON_SQR) {
            if (ac.lengthSqr() > ab.lengthSqr()) {
                simplex.clear();
                simplex.add(a);
                simplex.add(c);
            } else {
                simplex.clear();
                simplex.add(a);
                simplex.add(b);
            }

            return handleLine(simplex);
        }

        var outsideAC = abc.cross(ac);

        if (outsideAC.dot(ao) > 0.0) {
            if (ac.dot(ao) > 0.0) {
                simplex.clear();
                simplex.add(a);
                simplex.add(c);

                return handleLine(simplex);
            }

            simplex.clear();
            simplex.add(a);
            simplex.add(b);

            return handleLine(simplex);
        }

        var outsideAB = ab.cross(abc);

        if (outsideAB.dot(ao) > 0.0) {
            simplex.clear();
            simplex.add(a);
            simplex.add(b);

            return handleLine(simplex);
        }

        var side = abc.dot(ao);

        if (Math.abs(side) < EPSILON)
            return new SimplexResult(true, Vec3.ZERO);

        if (side > 0.0) {
            return new SimplexResult(false, abc);
        }

        simplex.clear();
        simplex.add(a);
        simplex.add(c);
        simplex.add(b);

        return new SimplexResult(false, abc.scale(-1.0));
    }

    private SimplexResult handleTetrahedron(List<Vec3> simplex) {
        var a = simplex.get(0);
        var b = simplex.get(1);
        var c = simplex.get(2);
        var d = simplex.get(3);

        var ab = b.subtract(a);
        var ac = c.subtract(a);
        var ad = d.subtract(a);
        var ao = a.scale(-1.0);

        var volume = ab.dot(ac.cross(ad));

        if (Math.abs(volume) < EPSILON) {

            var abcAreaSqr = ab.cross(ac).lengthSqr();
            var acdAreaSqr = ac.cross(ad).lengthSqr();
            var adbAreaSqr = ad.cross(ab).lengthSqr();

            if (abcAreaSqr >= acdAreaSqr &&
                    abcAreaSqr >= adbAreaSqr &&
                    abcAreaSqr >= EPSILON_SQR) {

                simplex.clear();
                simplex.add(a);
                simplex.add(b);
                simplex.add(c);

                return handleTriangle(simplex);
            }

            if (acdAreaSqr >= adbAreaSqr &&
                    acdAreaSqr >= EPSILON_SQR) {

                simplex.clear();
                simplex.add(a);
                simplex.add(c);
                simplex.add(d);

                return handleTriangle(simplex);
            }

            if (adbAreaSqr >= EPSILON_SQR) {
                simplex.clear();
                simplex.add(a);
                simplex.add(d);
                simplex.add(b);

                return handleTriangle(simplex);
            }

            var abLengthSqr = ab.lengthSqr();
            var acLengthSqr = ac.lengthSqr();
            var adLengthSqr = ad.lengthSqr();

            simplex.clear();
            simplex.add(a);

            if (abLengthSqr >= acLengthSqr &&
                    abLengthSqr >= adLengthSqr) {

                simplex.add(b);

            } else if (acLengthSqr >= adLengthSqr) {

                simplex.add(c);

            } else {

                simplex.add(d);
            }

            return handleLine(simplex);
        }

        var abc = ab.cross(ac);

        if (abc.dot(ad) > 0.0)
            abc = abc.scale(-1.0);

        if (abc.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(a);
            simplex.add(b);
            simplex.add(c);

            return handleTriangle(simplex);
        }

        var acd = ac.cross(ad);

        if (acd.dot(ab) > 0.0)
            acd = acd.scale(-1.0);

        if (acd.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(a);
            simplex.add(c);
            simplex.add(d);

            return handleTriangle(simplex);
        }

        var adb = ad.cross(ab);

        if (adb.dot(ac) > 0.0)
            adb = adb.scale(-1.0);

        if (adb.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(a);
            simplex.add(d);
            simplex.add(b);

            return handleTriangle(simplex);
        }

        return new SimplexResult(true, Vec3.ZERO);
    }

    record SimplexResult(boolean containsOrigin, Vec3 direction) {}
}
