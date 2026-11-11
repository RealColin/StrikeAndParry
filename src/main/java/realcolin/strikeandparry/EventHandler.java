package realcolin.strikeandparry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import realcolin.strikeandparry.network.TryStartAttackPayload;
import realcolin.strikeandparry.network.PayloadHandler;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class EventHandler {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TryStartAttackPayload.TYPE, TryStartAttackPayload.STREAM_CODEC, PayloadHandler::handleTryStartAttack);
    }
}
