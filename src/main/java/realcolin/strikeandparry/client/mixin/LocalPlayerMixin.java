package realcolin.strikeandparry.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import realcolin.strikeandparry.Constants;
import realcolin.strikeandparry.StrikeandParry;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @ModifyExpressionValue(
            method = "modifyInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z")
    )
    private boolean strikeandparry$applyAttackSlowdown(boolean usingItem) {
        // Reuse vanilla's item-use input scaling, including its passenger exception.
        return usingItem || strikeandparry$isAttackMovementSlowed();
    }

    @ModifyExpressionValue(
            method = "modifyInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F")
    )
    private float strikeandparry$attackMovementMultiplier(float multiplier) {
        if (!strikeandparry$isAttackMovementSlowed())
            return multiplier;

        var player = (LocalPlayer) (Object) this;
        var attackMultiplier = Constants.ATTACK_MOVEMENT_SPEED_MULTIPLIER;
        // The default item-use multiplier also exists when no item is being used.
        return player.isUsingItem() ? Math.min(multiplier, attackMultiplier) : attackMultiplier;
    }

    @ModifyReturnValue(method = "isSprintingPossible", at = @At("RETURN"))
    private boolean strikeandparry$preventAttackSprinting(boolean possible) {
        return possible && !strikeandparry$isAttackMovementSlowed();
    }

    @Unique
    private boolean strikeandparry$isAttackMovementSlowed() {
        return ((LocalPlayer) (Object) this).getData(StrikeandParry.ATTACK_MOVEMENT_SLOWED);
    }
}
