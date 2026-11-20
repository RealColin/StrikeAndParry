package realcolin.strikeandparry.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.FirstPersonOffsetModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import realcolin.strikeandparry.Constants;
import realcolin.strikeandparry.network.PlayAnimationPayload;
import realcolin.strikeandparry.network.TryStartAttackPayload;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class EventHandler {
    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (FMLEnvironment.isProduction())
            return;

        Keybinds.init();
        event.registerCategory(Keybinds.COMBAT);
        event.register(Keybinds.DEV_TEST);
    }

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

    /*
        These two are needed to make sure nothing breaks in the CombatState on Player logout or respawn.
     */
    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientAttackState();
    }

    @SubscribeEvent
    static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        resetClientAttackState();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) {
            resetClientAttackState();
            return;
        }

        if (mc.isPaused())
            return;

        /*
            Empty while loops because the consumeClick methods update variables for some reason.
            The purpose of this is to discard input related to switching slots on the hotbar during
            an attack, so that it doesn't visually rubber-band due to the server rejecting the action.
         */
        if (ClientState.weaponSwitchLocked) {
            for (var key : mc.options.keyHotbarSlots)
                while (key.consumeClick())
                    ;

            while (mc.options.keySwapOffhand.consumeClick())
                ;
        }

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

    /*
        Stop the block break animation from playing on the client during an attack.
     */
    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() || !ClientState.weaponSwitchLocked)
            return;

        var action = event.getAction();
        if (action == PlayerInteractEvent.LeftClickBlock.Action.START
                || action == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            event.setCanceled(true);
            var mc = Minecraft.getInstance();
            if (mc.gameMode != null)
                mc.gameMode.stopDestroyBlock();
        }
    }

    // TODO this needs to be cleaned up, reorganized, properly commented, etc
    @SubscribeEvent
    static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isPickBlock() && ClientState.weaponSwitchLocked) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (!event.isAttack())
            return;

        var mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        if (!mc.player.isAlive() || mc.player.isSpectator())
            return;

        var hitResult = mc.hitResult;
        var stack = mc.player.getMainHandItem();

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK && canMine(stack)) {
            if (ClientState.weaponSwitchLocked) {
                event.setSwingHand(false);
                event.setCanceled(true);
                if (mc.gameMode != null)
                    mc.gameMode.stopDestroyBlock();
            }
            return;
        }

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
