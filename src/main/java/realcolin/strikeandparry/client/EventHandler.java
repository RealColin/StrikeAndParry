package realcolin.strikeandparry.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.FirstPersonOffsetModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import realcolin.strikeandparry.Constants;
import realcolin.strikeandparry.network.PlayAnimationPayload;
import realcolin.strikeandparry.network.TryStartAttackPayload;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class EventHandler {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                Constants.PAL_ID,
                1500,
                player -> {
                    var controller = new PlayerAnimationController(
                            player, (_, _, _) -> PlayState.STOP
                    );

                    controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                    controller.setFirstPersonFollowsCamera(true);

                    controller.setFirstPersonConfiguration(
                            new FirstPersonConfiguration().setShowLeftArm(true).setShowRightArm(true)
                    );

                    controller.addModifier(new FirstPersonOffsetModifier(), 0);

                    return controller;
                }));
    }

    @SubscribeEvent
    static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(PlayAnimationPayload.TYPE, PayloadHandler::handlePlayAnimation);
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientAttackState();
    }

    @SubscribeEvent
    static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        resetClientAttackState();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) {
            resetClientAttackState();
            return;
        }

        if (mc.isPaused())
            return;

        if (ClientState.weaponSwitchLocked) {
            for (var key : mc.options.keyHotbarSlots) {
                while (key.consumeClick()) {
                    // Discard queued slot changes before vanilla handles them.
                }
            }

            while (mc.options.keySwapOffhand.consumeClick()) {
                // Discard queued hand swaps rather than delaying them until unlock.
            }
        }

        // One tick for a pending request; the full duration once the attack is accepted.
        if (ClientState.weaponSwitchTicksRemaining > 0) {
            ClientState.weaponSwitchTicksRemaining--;

            if (ClientState.weaponSwitchTicksRemaining == 0)
                ClientState.weaponSwitchLocked = false;
        }
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ClientState.weaponSwitchLocked)
            event.setCanceled(true);
    }

    @SubscribeEvent
    static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
//        if (event.isPickBlock() && ClientState.weaponSwitchLocked) {
//            event.setSwingHand(false);
//            event.setCanceled(true);
//            return;
//        }

        if (!event.isAttack())
            return;

        var mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        if (!mc.player.isAlive() || mc.player.isSpectator())
            return;

        var hitResult = mc.hitResult;
        var stack = mc.player.getMainHandItem();

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK && canMine(stack))
            return;

        event.setSwingHand(false);
        event.setCanceled(true);
        if (mc.player.getAttributeValue(Attributes.ATTACK_SPEED) <= 0.0)
            return;

        // Match vanilla's ordering: synchronize the selected weapon before attacking.
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
        if (!ClientState.weaponSwitchLocked) {
            ClientState.weaponSwitchLocked = true;
            ClientState.weaponSwitchTicksRemaining = 1;
        }
        ClientPacketDistributor.sendToServer(new TryStartAttackPayload());
    }

    private static void resetClientAttackState() {
        ClientState.weaponSwitchLocked = false;
        ClientState.weaponSwitchTicksRemaining = 0;
    }

    private static boolean canMine(ItemStack stack) {
        return !(stack.is(ItemTags.SPEARS) || stack.is(Items.MACE));
    }
}
