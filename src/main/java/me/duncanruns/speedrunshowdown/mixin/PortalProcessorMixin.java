package me.duncanruns.speedrunshowdown.mixin;

import me.duncanruns.speedrunshowdown.SpeedrunShowdown;
import net.minecraft.world.entity.PortalProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalProcessor.class)
public abstract class PortalProcessorMixin {
    @Inject(method = "processPortalTeleportation", at = @At("RETURN"), cancellable = true)
    private void noPortalTick(CallbackInfoReturnable<Boolean> cir) {
        if (SpeedrunShowdown.canEnterNether()) return;
        cir.setReturnValue(false);
    }
}
