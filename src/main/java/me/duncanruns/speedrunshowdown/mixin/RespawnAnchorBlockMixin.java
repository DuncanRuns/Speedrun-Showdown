package me.duncanruns.speedrunshowdown.mixin;

import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RespawnAnchorBlock;explode(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;)V"), cancellable = true)
    private void noBedBoom(CallbackInfoReturnable<ActionResult> cir) {
        cir.setReturnValue(ActionResult.PASS);
    }
}