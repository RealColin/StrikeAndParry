package realcolin.strikeandparry.combat;

import java.util.List;

public record AttackSet(String id, List<Attack> attacks) {
    public AttackSet {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id must be defined for AttackSet");
        if (attacks == null || attacks.isEmpty())
            throw new IllegalArgumentException("attacks must be defined for AttackSet");
        attacks = List.copyOf(attacks);
    }

    public Attack get(int index) {
        return attacks.get(index % attacks.size());
    }

    public int size() {
        return attacks().size();
    }
}
