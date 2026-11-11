package realcolin.strikeandparry.combat;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

public final class AttackSets {

    public static AttackSet resolve(LivingEntity entity) {
        if (entity.getMainHandItem().is(ItemTags.SWORDS)) {
            return SWORD;
        } else if (entity.getMainHandItem().is(ItemTags.AXES)) {
            return AXE;
        } else if (entity.getMainHandItem().is(ItemTags.SPEARS)) {
            return SPEAR;
        } else if (entity.getMainHandItem().is(Items.MACE)) {
            return MACE;
        }

        return PUNCH;
    }

    // TODO finish impl
    public static final AttackSet SWORD = new AttackSet(
            "sword",
            List.of(
                    new Attack("horizontal_sweep_1", 0.5, 1.0, null),
                    new Attack("horizontal_sweep_2", 0.5, 1.0, null),
                    new Attack("thrust_1", 0.5, 1.0, null)
            )
    );

    // TODO finish impl
    public static final AttackSet AXE = new AttackSet(
            "axe",
            List.of(
                    new Attack("horizontal_sweep_1", 0.5,  1.0, null),
                    new Attack("horizontal_sweep_2", 0.5, 1.0, null)
            )
    );

    // TODO finish impl
    public static final AttackSet SPEAR = new AttackSet(
            "spear",
            List.of(
                    new Attack("thrust_1", 0.5, 1.0, null)
            )
    );

    // TODO finish impl
    public static final AttackSet MACE = new AttackSet(
            "mace",
            List.of(
                    new Attack("horizontal_sweep_1", 0.5, 1.0, null),
                    new Attack("horizontal_sweep_2", 0.5, 1.0, null),
                    new Attack("vertical_sweep_1", 0.5, 1.0, null)
            )
    );

    // TODO finish impl
    public static final AttackSet PUNCH = new AttackSet(
            "punch",
            List.of(
                    new Attack("punch", 0.5, 1.0, null)
            )
    );
}
