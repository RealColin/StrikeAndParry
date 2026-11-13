package realcolin.strikeandparry;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import realcolin.strikeandparry.combat.CombatState;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class StrikeandParry {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static final Supplier<AttachmentType<CombatState>> COMBAT_STATE_ATTACHMENT = ATTACHMENT_TYPES.register(
            "combat_state", () -> AttachmentType.builder(CombatState::new).build());

    public StrikeandParry(IEventBus modEventBus, ModContainer modContainer) {

        ATTACHMENT_TYPES.register(modEventBus);
    }
}
