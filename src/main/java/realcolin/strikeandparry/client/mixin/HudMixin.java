package realcolin.strikeandparry.client.mixin;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Redirect(
            method = {"extractCrosshair", "extractItemHotbar"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
    )
    private Object strikeandparry$hideAttackIndicator(OptionInstance<?> option) {
        // These methods only read the attack-indicator option; leave its stored value untouched.
        return AttackIndicatorStatus.OFF;
    }
}
