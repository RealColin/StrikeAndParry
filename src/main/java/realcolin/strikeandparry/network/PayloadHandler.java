package realcolin.strikeandparry.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PayloadHandler {

    public static void handleTryStartAttack(TryStartAttackPayload payload, final IPayloadContext context) {
        var player = context.player();

    }
}
