package me.duncanruns.speedrunshowdown;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
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

        final Vec3 holdPos = new Vec3(0, 1000000, 0);
        ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> {
            tickNetherTimer(minecraftServer);
            tickStartTimer(minecraftServer);
            if (rrts != null) {
                ServerLevel world = minecraftServer.getAllLevels().iterator().next();
                world.getGameRules().set(GameRules.RESPAWN_RADIUS, rrts, minecraftServer);
                rrts = null;
            }
            if (released) return;
            for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                if (!player.isAlive()) continue;
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10000, 100, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10000, 100, true, false));
                player.teleport(new TeleportTransition(player.level(), holdPos, Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
            }
        });
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(Commands.literal("release").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                startTimer = 100;
                return 1;
            }));
            commandDispatcher.register(Commands.literal("unrelease").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
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
        server.setWeatherParameters(0, 1, true, false);
        ServerClockManager clockManager = server.clockManager();
        server.getAllLevels().forEach(level -> {
            Holder<WorldClock> clock = level.dimensionTypeRegistration().value().defaultClock().orElseThrow();
            clockManager.setTotalTicks(clock, 0);
        });
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
        ServerLevel world = server.getAllLevels().iterator().next();
        GameRules gameRules = world.getGameRules();
        rrts = gameRules.get(GameRules.RESPAWN_RADIUS);
        boolean oldDIR = gameRules.get(GameRules.IMMEDIATE_RESPAWN);
        gameRules.set(GameRules.RESPAWN_RADIUS, 0, server);
        gameRules.set(GameRules.IMMEDIATE_RESPAWN, true, server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.kill(player.level());
        }
        gameRules.set(GameRules.IMMEDIATE_RESPAWN, oldDIR, server);
    }

    public static boolean canEnterNether() {
        return netherTimer <= 0;
    }

    private static void tickNetherTimer(MinecraftServer server) {
        if (netherTimer < 0) return;
        // If there's more than 5 minutes left, announce every 5 minutes, otherwise announce every minute
        int announcementRate = netherTimer >= FIVE_MINUTES ? FIVE_MINUTES : ONE_MINUTE;
        if (netherTimer > 0 && netherTimer % announcementRate == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether opens in " + netherTimer / 20 / 60 + " minutes."), false);
        } else if (netherTimer == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether is now open!"), false);
        }

        if (netherTimer > 0 && netherTimer <= 100 && netherTimer % 20 == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether opens in " + netherTimer / 20 + " seconds."), false);
        }
        netherTimer--;
    }

    private static void tickStartTimer(MinecraftServer server) {
        if (startTimer < 0) return;
        if (startTimer > 0 && startTimer % 20 == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The game starts in " + startTimer / 20 + " seconds."), false);
        }
        if (startTimer == 0) {
            release(server);
        }
        startTimer--;
    }
}