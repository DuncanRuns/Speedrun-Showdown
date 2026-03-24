package me.duncanruns.speedrunshowdown;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class SpeedrunShowdown implements ModInitializer {
    public static final String MOD_ID = "speedrun-showdown";
    private static final Path releaseFile = FabricLoader.getInstance().getGameDir().resolve("ss-release");

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean released = false;

    private static Integer rrts = null;

    private static int netherTimer = -1;

    private static final int ONE_MINUTE = 20 * 60;
    private static final int FIVE_MINUTES = 20 * 60 * 5;
    private static final int FIFTEEN_MINUTES = 20 * 60 * 15;

    private static int startTimer = -1;


    @Override
    public void onInitialize() {
        if (Files.exists(releaseFile)) released = true;

        final Vec3d holdPos = new Vec3d(0, 1000000, 0);
        ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> {
            tickNetherTimer(minecraftServer);
            tickStartTimer(minecraftServer);
            if (rrts != null) {
                ServerWorld world = minecraftServer.getWorlds().iterator().next();
                world.getGameRules().setValue(GameRules.RESPAWN_RADIUS, rrts, minecraftServer);
                rrts = null;
            }
            if (released) return;
            for (ServerPlayerEntity player : minecraftServer.getPlayerManager().getPlayerList()) {
                if (!player.isAlive()) continue;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10000, 100, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10000, 100, true, false));
                player.teleportTo(new TeleportTarget(player.getEntityWorld(), holdPos, Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
            }
        });
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(CommandManager.literal("release").requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                startTimer = 100;
                return 1;
            }));
            commandDispatcher.register(CommandManager.literal("unrelease").requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                return unrelease(server) ? 1 : 0;
            }));
        });
    }

    private static boolean release(MinecraftServer server) {
        if (released) return false;
        released = true;
        instaKillAllPlayers(server);
        try {
            Files.createFile(releaseFile);
        } catch (Exception e) {
            LOGGER.error("Failed to create release file!", e);
        }
        server.getOverworld().setWeather(0, 1, true, false);
        server.getWorlds().forEach(world -> world.setTimeOfDay(0));
        netherTimer = FIFTEEN_MINUTES;
        return true;
    }

    private static boolean unrelease(MinecraftServer server) {
        if (!released) return false;
        released = false;
        instaKillAllPlayers(server);
        try {
            Files.delete(releaseFile);
        } catch (Exception e) {
            LOGGER.error("Failed to delete release file!", e);
        }
        netherTimer = -1;
        startTimer = -1;
        return true;
    }

    private static void instaKillAllPlayers(MinecraftServer server) {
        ServerWorld world = server.getWorlds().iterator().next();
        GameRules gameRules = world.getGameRules();
        rrts = gameRules.getValue(GameRules.RESPAWN_RADIUS);
        boolean oldDIR = gameRules.getValue(GameRules.DO_IMMEDIATE_RESPAWN);
        gameRules.setValue(GameRules.RESPAWN_RADIUS, 0, server);
        gameRules.setValue(GameRules.DO_IMMEDIATE_RESPAWN, true, server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.kill(player.getEntityWorld());
        }
        gameRules.setValue(GameRules.DO_IMMEDIATE_RESPAWN, oldDIR, server);
    }

    public static boolean canEnterNether() {
        return netherTimer <= 0;
    }

    private static void tickNetherTimer(MinecraftServer server) {
        if (netherTimer < 0) return;
        // If there's more than 5 minutes left, announce every 5 minutes, otherwise announce every minute
        int announcementRate = netherTimer >= FIVE_MINUTES ? FIVE_MINUTES : ONE_MINUTE;
        if (netherTimer > 0 && netherTimer % announcementRate == 0) {
            server.getPlayerManager().broadcast(Text.literal("The Nether opens in " + netherTimer / 20 / 60 + " minutes."), false);
        } else if (netherTimer == 0) {
            server.getPlayerManager().broadcast(Text.literal("The Nether is now open!"), false);
        }

        if(netherTimer > 0 && netherTimer <= 100 && netherTimer % 20 == 0) {
            server.getPlayerManager().broadcast(Text.literal("The Nether opens in " + netherTimer / 20 + " seconds."), false);
        }
        netherTimer--;
    }

    private static void tickStartTimer(MinecraftServer server) {
        if (startTimer < 0) return;
        if (startTimer > 0 && startTimer % 20 == 0) {
            server.getPlayerManager().broadcast(Text.literal("The game starts in " + startTimer / 20 + " seconds."), false);
        }
        if (startTimer == 0) {
            release(server);
        }
        startTimer--;
    }
}