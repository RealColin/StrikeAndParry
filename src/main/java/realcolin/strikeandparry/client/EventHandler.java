package realcolin.strikeandparry.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.FirstPersonOffsetModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
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
    static void onClientTick(ClientTickEvent.Post event) {

    }

    @SubscribeEvent
    static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack())
            return;

        var mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        var hitResult = mc.hitResult;
        var stack = mc.player.getMainHandItem();

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK && canMine(stack))
            return;

        event.setSwingHand(false);
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new TryStartAttackPayload());
    }

    private static boolean canMine(ItemStack stack) {
        return !(stack.is(ItemTags.SPEARS) || stack.is(Items.MACE));
    }
}
