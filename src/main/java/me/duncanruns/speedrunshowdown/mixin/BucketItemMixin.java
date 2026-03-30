package me.duncanruns.speedrunshowdown.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void noBucketPlaceIfCloseToOthers(LivingEntity user, Level world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClientSide()) return;
        if (!(user instanceof ServerPlayer player)) return;
        if (!(this.content instanceof LavaFluid)) return;

        PlayerTeam team = player.getTeam();
        boolean isOnTeam = team != null;
        if (world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, e -> {
            if (!(e instanceof ServerPlayer other)) return false;
            if (player == other) return false;
            if (!Objects.requireNonNull(other.gameMode()).isSurvival()) return false;
            if (isOnTeam) {
                PlayerTeam otherTeam = other.getTeam();
                if (otherTeam == null) return true;
                return !otherTeam.equals(team);
            }
            return true;
        }) != null) {
            cir.setReturnValue(false);
        }
    }
}
