package me.duncanruns.speedrunshowdown.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public class BedBlockMixin {
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", ordinal = 0), cancellable = true)
    private void noBedBoom(CallbackInfoReturnable<ActionResult> cir) {
        cir.setReturnValue(ActionResult.PASS);
    }
}