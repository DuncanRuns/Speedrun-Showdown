package me.duncanruns.speedrunshowdown.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.duncanruns.speedrunshowdown.SpeedrunShowdown;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PortalProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalProcessor.class)
public abstract class PortalProcessorMixin {
    @Inject(method = "processPortalTeleportation", at = @At("RETURN"), cancellable = true)
    private void noPortalTick(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) ServerLevel serverLevel) {
        if (SpeedrunShowdown.canEnterNether(serverLevel.getServer())) return;
        cir.setReturnValue(false);
    }
}
