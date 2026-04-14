package me.duncanruns.speedrunshowdown.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.duncanruns.speedrunshowdown.SSData;
import me.duncanruns.speedrunshowdown.SSDataOwner;
import me.duncanruns.speedrunshowdown.SpeedrunShowdown;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements SSDataOwner {
    @Unique
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;
    @Unique
    private SSData ssd = new SSData();

    public SSData speedrunshowdown$getData() {
        return ssd;
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void onServerLoad(CallbackInfo ci) {
        loadSSD();
    }

    @Unique
    private void loadSSD() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                String contents = new String(Files.readAllBytes(path));
                ssd = GSON.fromJson(contents, SSData.class);
            } catch (IOException e) {
                SpeedrunShowdown.LOGGER.error("Failed to load SSD file!", e);
            }
        }
    }

    @Inject(method = "saveEverything", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z", shift = At.Shift.AFTER))
    private void onServerSave(CallbackInfoReturnable<Boolean> cir) {
        saveSSD();
    }

    @Unique
    private void saveSSD() {
        try {
            String contents = GSON.toJson(ssd);
            Files.write(getPath(), contents.getBytes());
        } catch (IOException e) {
            SpeedrunShowdown.LOGGER.error("Failed to save SSD file!", e);
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void cancelStart(CallbackInfo ci) {
        if ((!ssd.released) && ssd.startTimer != -1) {
            ssd.startTimer = -1;
        }
    }

    @Unique
    private @NonNull Path getPath() {
        return this.storageSource.getLevelPath(LevelResource.ROOT).normalize().resolve("ss.json");
    }
}
