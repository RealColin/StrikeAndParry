package realcolin.strikeandparry.mixin;

import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realcolin.strikeandparry.StrikeandParry;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleContainerClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;suppressRemoteUpdates()V"
            ),
            cancellable = true
    )
    private void strikeandparry$blockInventoryClicks(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        // The packet is on the server thread and validated; menu changes have not started.
        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSetCreativeModeSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void strikeandparry$blockCreativeInventoryChanges(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        // Creative slot edits and drops bypass the normal container-click handler.
        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(
            method = "handlePlaceRecipe",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void strikeandparry$blockRecipePlacement(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerButtonClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void strikeandparry$blockContainerButtons(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void strikeandparry$blockItemDrops(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        var action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)
            return;

        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "tryPickItem", at = @At("HEAD"), cancellable = true)
    private void strikeandparry$blockPickItem(ItemStack itemStack, CallbackInfo ci) {
        // Both block and entity pick requests reach this helper on the server thread.
        if (player.getData(StrikeandParry.COMBAT_STATE_ATTACHMENT).isAttacking()) {
            player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
            player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
