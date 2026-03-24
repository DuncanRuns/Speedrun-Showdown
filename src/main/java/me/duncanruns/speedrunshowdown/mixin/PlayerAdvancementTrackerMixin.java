package me.duncanruns.speedrunshowdown.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

/**
 * Prevents spectators from causing advancement messages, which could spoil the existence of structures.
 */
@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @WrapOperation(method = "method_53637", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void noAdvancementMessages(PlayerManager instance, Text message, boolean overlay, Operation<Void> original) {
        if (Objects.requireNonNull(this.owner.getGameMode()).isSurvivalLike())
            original.call(instance, message, overlay);
    }
}
