package realcolin.strikeandparry;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class Constants {
    public static final String MOD_ID = "strikeandparry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Identifier PAL_ID = Identifier.fromNamespaceAndPath(MOD_ID, "strikeandparry");
}
