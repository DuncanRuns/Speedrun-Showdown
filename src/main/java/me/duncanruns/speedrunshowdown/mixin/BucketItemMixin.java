package me.duncanruns.speedrunshowdown.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.item.BucketItem;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private Fluid fluid;

    @Inject(method = "placeFluid", at = @At("HEAD"), cancellable = true)
    private void noBucketPlaceIfCloseToOthers(LivingEntity user, World world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (!(this.fluid instanceof LavaFluid)) return;

        Team team = player.getScoreboardTeam();
        boolean isOnTeam = team != null;
        if (world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, e -> {
            if (!(e instanceof ServerPlayerEntity other)) return false;
            if (player == other) return false;
            if (!Objects.requireNonNull(other.getGameMode()).isSurvivalLike()) return false;
            if (isOnTeam) {
                Team otherTeam = other.getScoreboardTeam();
                if (otherTeam == null) return true;
                return !otherTeam.equals(team);
            }
            return true;
        }) != null) {
            cir.setReturnValue(false);
        }
    }
}
