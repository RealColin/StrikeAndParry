package realcolin.strikeandparry.client.mixin;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @Inject(method = "shouldPlaySwapAnimation", at = @At("HEAD"), cancellable = true)
    private void strikeandparry$skipWeaponSwapAnimation(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (strikeandparry$isWeapon(stack))
            cir.setReturnValue(false);
    }

    @Inject(method = "swapAnimationScale", at = @At("HEAD"), cancellable = true)
    private void strikeandparry$removeWeaponSwapMovement(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        if (strikeandparry$isWeapon(stack))
            cir.setReturnValue(0.0F);
    }

    @Unique
    private static boolean strikeandparry$isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SPEARS)
                || stack.is(Items.MACE);
    }
}
