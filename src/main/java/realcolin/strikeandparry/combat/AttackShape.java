package realcolin.strikeandparry.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import realcolin.strikeandparry.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface AttackShape {
    double EPSILON = 1e-8;
    double EPSILON_SQR = EPSILON * EPSILON;

    default List<LivingEntity> getTargets(LivingEntity attacker) {
        return getCandidates(attacker).stream()
                .filter(target -> hasUnobstructedHit(attacker, target))
                .toList();
    }

    private boolean hasUnobstructedHit(LivingEntity attacker, LivingEntity target) {
        var collision = checkCollision(attacker, target);
        if (!collision.intersects())
            return false;

        if (collision.overlapPoint().isEmpty()) {
            Constants.LOGGER.debug("Skipping hit from entity {} to {}: overlap-point reconstruction failed",
                    attacker.getId(), target.getId());
            return false;
        }

        if (isUnobstructed(attacker, collision.overlapPoint().get()))
            return true;

        // Only blocked hits pay for the fallback: at most eight extra GJK tests and rays.
        var box = target.getBoundingBox();
        var center = box.getCenter();
        for (var octant = 0; octant < 8; octant++) {
            var upperX = (octant & 1) != 0;
            var upperY = (octant & 2) != 0;
            var upperZ = (octant & 4) != 0;
            var subBox = new AABB(
                    upperX ? center.x : box.minX,
                    upperY ? center.y : box.minY,
                    upperZ ? center.z : box.minZ,
                    upperX ? box.maxX : center.x,
                    upperY ? box.maxY : center.y,
                    upperZ ? box.maxZ : center.z
            );
            var subCollision = checkCollision(attacker, subBox);
            if (subCollision.intersects() && subCollision.overlapPoint().isPresent()
                    && isUnobstructed(attacker, subCollision.overlapPoint().get()))
                return true;
        }

        return false;
    }

    double reach();
    Vec3 support(LivingEntity attacker, Vec3 direction);

    default Vec3 getOrigin(LivingEntity attacker) {
        return attacker.getEyePosition()
                .subtract(0.0, attacker.getBbHeight() * 0.15, 0.0);
    }

    default List<LivingEntity> getCandidates(LivingEntity attacker) {
        var searchBox = attacker.getBoundingBox().inflate(reach());

        return attacker.level().getEntitiesOfClass(LivingEntity.class, searchBox, target -> target != attacker);
    }

    default Vec3 supportAABB(LivingEntity target, Vec3 direction) {
        return supportAABB(target.getBoundingBox(), direction);
    }

    default Vec3 supportAABB(AABB box, Vec3 direction) {
        return new Vec3(
                direction.x >= 0.0 ? box.maxX : box.minX,
                direction.y >= 0.0 ? box.maxY : box.minY,
                direction.z >= 0.0 ? box.maxZ : box.minZ
        );
    }

    /**
     * Checks the segment from the attack origin to a validated overlap point.
     * Uses block collision shapes rather than outlines, and ignores fluids.
     * This does not test entity overlap; callers must obtain the point from checkCollision.
     */
    default boolean isUnobstructed(LivingEntity attacker, Vec3 overlapPoint) {
        var origin = getOrigin(attacker);
        var hit = attacker.level().clip(new ClipContext(
                origin,
                overlapPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                attacker
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    default boolean intersects(LivingEntity attacker, LivingEntity target) {
        return checkCollision(attacker, target).intersects();
    }

    /**
     * Runs GJK once and reconstructs an overlap point when it reports an intersection.
     * The intersection flag preserves GJK's decision, even if reconstruction fails.
     * Callers requiring an obstruction check must reject an empty overlapPoint.
     */
    default CollisionResult checkCollision(LivingEntity attacker, LivingEntity target) {
        return checkCollision(attacker, target.getBoundingBox());
    }

    /** Runs the same collision test against a whole hitbox or one of its subdivisions. */
    default CollisionResult checkCollision(LivingEntity attacker, AABB targetBox) {
        final int MAX_ITERATIONS = 32;

        var simplex = new ArrayList<SupportPoint>();

        var origin = getOrigin(attacker);
        var direction = targetBox.getCenter().subtract(origin);

        if (direction.lengthSqr() < EPSILON_SQR)
            direction = new Vec3(1.0, 0.0, 0.0);

        var point = new SupportPoint(
                support(attacker, direction),
                supportAABB(targetBox, direction.scale(-1.0))
        );

        simplex.addFirst(point);
        direction = point.difference().scale(-1.0);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (direction.lengthSqr() < EPSILON_SQR)
                return new CollisionResult(true, reconstructOverlapPoint(simplex));

            point = new SupportPoint(
                    support(attacker, direction),
                    supportAABB(targetBox, direction.scale(-1.0))
            );

            if (point.difference().dot(direction) < -EPSILON)
                return new CollisionResult(false, Optional.empty());

            simplex.addFirst(point);

            if (simplex.size() > 4)
                throw new IllegalStateException("GJK simplex exceeded 4 points");

            if (simplex.size() == 2) {
                var result = handleLine(simplex);

                if (result.containsOrigin())
                    return new CollisionResult(true, reconstructOverlapPoint(simplex));

                direction = result.direction();
            }

            else if (simplex.size() == 3) {
                var result = handleTriangle(simplex);

                if (result.containsOrigin())
                    return new CollisionResult(true, reconstructOverlapPoint(simplex));

                direction = result.direction();
            }

            else if (simplex.size() == 4) {
                var result = handleTetrahedron(simplex);

                if (result.containsOrigin())
                    return new CollisionResult(true, reconstructOverlapPoint(simplex));

                direction = result.direction();
            }
        }

        return new CollisionResult(false, Optional.empty());
    }

    private SimplexResult handleLine(List<SupportPoint> simplex) {
        var pointA = simplex.get(0);
        var a = pointA.difference();
        var pointB = simplex.get(1);
        var b = pointB.difference();

        var ab = b.subtract(a);
        var ao = a.scale(-1.0);

        if (ab.lengthSqr() < EPSILON_SQR) {
            simplex.clear();
            simplex.add(pointA);

            return new SimplexResult(false, ao);
        }

        if (ab.dot(ao) > 0.0) {
            var direction = ab.cross(ao).cross(ab);

            if (direction.lengthSqr() < EPSILON_SQR) {
                double t = ao.dot(ab) / ab.lengthSqr();

                if (t >= 0.0 && t <= 1.0)
                    return new SimplexResult(true, Vec3.ZERO);

                simplex.clear();
                simplex.add(pointB);

                return new SimplexResult(false, b.scale(-1.0));
            }

            return new SimplexResult(false, direction);
        }

        simplex.clear();
        simplex.add(pointA);

        return new SimplexResult(false, ao);
    }

    private SimplexResult handleTriangle(List<SupportPoint> simplex) {
        var pointA = simplex.get(0);
        var a = pointA.difference();
        var pointB = simplex.get(1);
        var b = pointB.difference();
        var pointC = simplex.get(2);
        var c = pointC.difference();

        var ab = b.subtract(a);
        var ac = c.subtract(a);
        var ao = a.scale(-1.0);

        var abc = ab.cross(ac);

        if (abc.lengthSqr() < EPSILON_SQR) {
            if (ac.lengthSqr() > ab.lengthSqr()) {
                simplex.clear();
                simplex.add(pointA);
                simplex.add(pointC);
            } else {
                simplex.clear();
                simplex.add(pointA);
                simplex.add(pointB);
            }

            return handleLine(simplex);
        }

        var outsideAC = abc.cross(ac);

        if (outsideAC.dot(ao) > 0.0) {
            if (ac.dot(ao) > 0.0) {
                simplex.clear();
                simplex.add(pointA);
                simplex.add(pointC);

                return handleLine(simplex);
            }

            simplex.clear();
            simplex.add(pointA);
            simplex.add(pointB);

            return handleLine(simplex);
        }

        var outsideAB = ab.cross(abc);

        if (outsideAB.dot(ao) > 0.0) {
            simplex.clear();
            simplex.add(pointA);
            simplex.add(pointB);

            return handleLine(simplex);
        }

        var side = abc.dot(ao);

        if (Math.abs(side) < EPSILON)
            return new SimplexResult(true, Vec3.ZERO);

        if (side > 0.0) {
            return new SimplexResult(false, abc);
        }

        simplex.clear();
        simplex.add(pointA);
        simplex.add(pointC);
        simplex.add(pointB);

        return new SimplexResult(false, abc.scale(-1.0));
    }

    private SimplexResult handleTetrahedron(List<SupportPoint> simplex) {
        var pointA = simplex.get(0);
        var a = pointA.difference();
        var pointB = simplex.get(1);
        var b = pointB.difference();
        var pointC = simplex.get(2);
        var c = pointC.difference();
        var pointD = simplex.get(3);
        var d = pointD.difference();

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
                simplex.add(pointA);
                simplex.add(pointB);
                simplex.add(pointC);

                return handleTriangle(simplex);
            }

            if (acdAreaSqr >= adbAreaSqr &&
                    acdAreaSqr >= EPSILON_SQR) {

                simplex.clear();
                simplex.add(pointA);
                simplex.add(pointC);
                simplex.add(pointD);

                return handleTriangle(simplex);
            }

            if (adbAreaSqr >= EPSILON_SQR) {
                simplex.clear();
                simplex.add(pointA);
                simplex.add(pointD);
                simplex.add(pointB);

                return handleTriangle(simplex);
            }

            var abLengthSqr = ab.lengthSqr();
            var acLengthSqr = ac.lengthSqr();
            var adLengthSqr = ad.lengthSqr();

            simplex.clear();
            simplex.add(pointA);

            if (abLengthSqr >= acLengthSqr &&
                    abLengthSqr >= adLengthSqr) {

                simplex.add(pointB);

            } else if (acLengthSqr >= adLengthSqr) {

                simplex.add(pointC);

            } else {

                simplex.add(pointD);
            }

            return handleLine(simplex);
        }

        var abc = ab.cross(ac);

        if (abc.dot(ad) > 0.0)
            abc = abc.scale(-1.0);

        if (abc.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(pointA);
            simplex.add(pointB);
            simplex.add(pointC);

            return handleTriangle(simplex);
        }

        var acd = ac.cross(ad);

        if (acd.dot(ab) > 0.0)
            acd = acd.scale(-1.0);

        if (acd.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(pointA);
            simplex.add(pointC);
            simplex.add(pointD);

            return handleTriangle(simplex);
        }

        var adb = ad.cross(ab);

        if (adb.dot(ac) > 0.0)
            adb = adb.scale(-1.0);

        if (adb.dot(ao) > EPSILON) {
            simplex.clear();
            simplex.add(pointA);
            simplex.add(pointD);
            simplex.add(pointB);

            return handleTriangle(simplex);
        }

        return new SimplexResult(true, Vec3.ZERO);
    }

    /**
     * Reconstructs a common point, within 1e-7 blocks, from a completed GJK simplex.
     * An empty result means no numerically valid point could be recovered.
     * Does not modify the simplex or make a new collision decision.
     */
    private static Optional<Vec3> reconstructOverlapPoint(List<SupportPoint> simplex) {
        if (simplex.isEmpty() || simplex.size() > 4)
            return Optional.empty();

        // Try the full simplex first, then all faces, edges, and vertices.
        for (var size = simplex.size(); size >= 1; size--) {
            for (var mask = 1; mask < (1 << simplex.size()); mask++) {
                if (Integer.bitCount(mask) != size)
                    continue;

                var subset = new ArrayList<SupportPoint>(size);
                for (var index = 0; index < simplex.size(); index++) {
                    if ((mask & (1 << index)) != 0)
                        subset.add(simplex.get(index));
                }

                var point = reconstructSimplexPoint(subset);
                if (point.isPresent())
                    return point;
            }
        }

        return Optional.empty();
    }

    private static Optional<Vec3> reconstructSimplexPoint(List<SupportPoint> simplex) {
        var weights = new double[simplex.size()];
        var a = simplex.getFirst().difference();

        if (simplex.size() == 1) {
            weights[0] = 1.0;
        } else {
            // Scale edges to make the degeneracy checks independent of world units.
            var edgeScaleSqr = 0.0;
            for (var index = 1; index < simplex.size(); index++) {
                edgeScaleSqr = Math.max(edgeScaleSqr,
                        simplex.get(index).difference().subtract(a).lengthSqr());
            }

            if (!Double.isFinite(edgeScaleSqr) || edgeScaleSqr <= EPSILON_SQR)
                return Optional.empty();

            var inverseScale = 1.0 / Math.sqrt(edgeScaleSqr);
            var ab = simplex.get(1).difference().subtract(a).scale(inverseScale);
            var ao = a.scale(-inverseScale);

            if (simplex.size() == 2) {
                weights[1] = ao.dot(ab) / ab.lengthSqr();
                weights[0] = 1.0 - weights[1];
            } else {
                var ac = simplex.get(2).difference().subtract(a).scale(inverseScale);
                var normal = ab.cross(ac);

                if (simplex.size() == 3) {
                    var areaSqr = normal.lengthSqr();
                    if (areaSqr <= EPSILON_SQR)
                        return Optional.empty();

                    weights[1] = ao.cross(ac).dot(normal) / areaSqr;
                    weights[2] = ab.cross(ao).dot(normal) / areaSqr;
                    weights[0] = 1.0 - weights[1] - weights[2];
                } else {
                    var ad = simplex.get(3).difference().subtract(a).scale(inverseScale);
                    var volume = normal.dot(ad);
                    if (Math.abs(volume) <= EPSILON)
                        return Optional.empty();

                    weights[1] = ao.dot(ac.cross(ad)) / volume;
                    weights[2] = ab.dot(ao.cross(ad)) / volume;
                    weights[3] = normal.dot(ao) / volume;
                    weights[0] = 1.0 - weights[1] - weights[2] - weights[3];
                }
            }
        }

        return validateOverlapPoint(simplex, weights);
    }

    private static Optional<Vec3> validateOverlapPoint(List<SupportPoint> simplex, double[] weights) {
        var weightSum = 0.0;
        for (var index = 0; index < weights.length; index++) {
            var weight = weights[index];
            if (!Double.isFinite(weight) || weight < -EPSILON || weight > 1.0 + EPSILON)
                return Optional.empty();

            // Only correct rounding-sized negative weights, then renormalize.
            weights[index] = Math.max(0.0, weight);
            weightSum += weights[index];
        }

        if (!Double.isFinite(weightSum) || Math.abs(weightSum - 1.0) > EPSILON)
            return Optional.empty();

        for (var index = 0; index < weights.length; index++)
            weights[index] /= weightSum;

        // Use local offsets to reduce loss of precision far from the world origin.
        var attackOrigin = simplex.getFirst().attackPoint();
        var targetOrigin = simplex.getFirst().targetPoint();
        var attackPoint = attackOrigin;
        var targetPoint = targetOrigin;
        for (var index = 1; index < simplex.size(); index++) {
            var support = simplex.get(index);
            attackPoint = attackPoint.add(support.attackPoint().subtract(attackOrigin).scale(weights[index]));
            targetPoint = targetPoint.add(support.targetPoint().subtract(targetOrigin).scale(weights[index]));
        }

        var distanceTolerance = 1e-7;
        if (!(attackPoint.subtract(targetPoint).lengthSqr() <= distanceTolerance * distanceTolerance))
            return Optional.empty();

        return Optional.of(attackPoint);
    }

    record SupportPoint(Vec3 attackPoint, Vec3 targetPoint, Vec3 difference) {
        public SupportPoint(Vec3 attackPoint, Vec3 targetPoint) {
            this(attackPoint, targetPoint, attackPoint.subtract(targetPoint));
        }
    }

    /** Empty overlapPoint distinguishes failed reconstruction from a usable collision. */
    record CollisionResult(boolean intersects, Optional<Vec3> overlapPoint) {}

    record SimplexResult(boolean containsOrigin, Vec3 direction) {}
}
