package realcolin.strikeandparry.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realcolin.strikeandparry.combat.BlockBreakingReset;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements BlockBreakingReset {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private int destroyProgressStart;
    @Shadow private BlockPos destroyPos;
    @Shadow private int gameTicks;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos delayedDestroyPos;
    @Shadow private int delayedTickStart;
    @Shadow private int lastSentState;

    @Override
    public void strikeandparry$resetBlockBreaking() {
        if (isDestroyingBlock)
            strikeandparry$clearBlockBreaking(destroyPos);

        if (hasDelayedDestroy && (!isDestroyingBlock || !delayedDestroyPos.equals(destroyPos)))
            strikeandparry$clearBlockBreaking(delayedDestroyPos);

        isDestroyingBlock = false;
        hasDelayedDestroy = false;
        lastSentState = -1;
        destroyProgressStart = gameTicks;
        delayedTickStart = gameTicks;
    }

    @Unique
    private void strikeandparry$clearBlockBreaking(BlockPos pos) {
        level.destroyBlockProgress(player.getId(), pos, -1);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
    }

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void strikeandparry$rejectStaleCompletion(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                                    Direction direction, int maxY, int sequence, CallbackInfo ci) {
        // Vanilla checks the old target on STOP even if the mining attempt was already cleared.
        if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                && (!isDestroyingBlock || !pos.equals(destroyPos))) {
            if (player.isWithinBlockInteractionRange(pos, 1.0) && level.hasChunkAt(pos))
                player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            ci.cancel();
        }
    }
}
