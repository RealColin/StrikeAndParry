package realcolin.strikeandparry.combat;

public record Attack(
        String id,
        double windupRatio,
        double damageMultiplier,
        AttackShape shape
) {
}
