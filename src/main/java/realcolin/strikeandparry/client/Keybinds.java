package realcolin.strikeandparry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import realcolin.strikeandparry.Constants;

public final class Keybinds {
    public static final KeyMapping.Category COMBAT =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "combat"));

    public static KeyMapping DEV_TEST;
    public static KeyMapping FEINT;

    public static void init() {
        DEV_TEST = new KeyMapping(
                "key.strikeandparry.dev_test",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                COMBAT
        );
        FEINT = new KeyMapping(
                "key.strikeandparry.feint",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                COMBAT
        );
    }

}
