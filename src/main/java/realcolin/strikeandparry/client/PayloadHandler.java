package realcolin.strikeandparry.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import realcolin.strikeandparry.Constants;
import realcolin.strikeandparry.network.PlayAnimationPayload;

public class PayloadHandler {

    public static void handlePlayAnimation(PlayAnimationPayload payload, IPayloadContext context) {
        if (!Double.isFinite(payload.speed()) || payload.speed() <= 0)
            return;

        // Only the local player's accepted attack controls the input lock.
        // Start the countdown even if its animation cannot be played below.
        if (payload.entityId() == context.player().getId()) {
            if (!context.player().isAlive() || context.player().isSpectator())
                return;

            ClientState.weaponSwitchTicksRemaining = Math.max(1, (int) Math.round(20.0 / payload.speed()));
            ClientState.weaponSwitchLocked = true;

            var mc = Minecraft.getInstance();
            if (mc.gameMode != null)
                mc.gameMode.stopDestroyBlock();
        }

        var entity = context.player().level().getEntity(payload.entityId());

        if (!(entity instanceof Avatar avatar))
            return;

        var layer = PlayerAnimationAccess.getPlayerAnimationLayer(avatar, Constants.PAL_ID);

        if (!(layer instanceof PlayerAnimationController controller))
            return;

        controller.removeModifierIf(modifier -> modifier instanceof SpeedModifier);
        controller.addModifierLast(new SpeedModifier((float)payload.speed()));
        var animID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, payload.attackId());
        controller.triggerAnimation(animID);
    }
}
