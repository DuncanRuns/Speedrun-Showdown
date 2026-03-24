package me.duncanruns.speedrunshowdown.mixin;

import me.duncanruns.speedrunshowdown.SpeedrunShowdown;
import net.minecraft.world.dimension.PortalManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalManager.class)
public abstract class PortalManagerMixin {
    @Inject(method = "tick", at = @At("RETURN"), cancellable = true)
    private void noPortalTick(CallbackInfoReturnable<Boolean> cir) {
        if (SpeedrunShowdown.canEnterNether()) return;
        cir.setReturnValue(false);
    }
}
