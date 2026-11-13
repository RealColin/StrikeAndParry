package realcolin.strikeandparry;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import realcolin.strikeandparry.combat.CombatController;
import realcolin.strikeandparry.network.PlayAnimationPayload;
import realcolin.strikeandparry.network.TryStartAttackPayload;
import realcolin.strikeandparry.network.PayloadHandler;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class EventHandler {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TryStartAttackPayload.TYPE, TryStartAttackPayload.STREAM_CODEC, PayloadHandler::handleTryStartAttack);
        registrar.playToClient(PlayAnimationPayload.TYPE, PlayAnimationPayload.STREAM_CODEC);
    }

    @SubscribeEvent
    public static void tickEntities(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity))
            return;

        if (livingEntity.level().isClientSide())
            return;

        CombatController.tickCombat(livingEntity);
    }
}
