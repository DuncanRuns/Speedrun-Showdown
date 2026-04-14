package me.duncanruns.speedrunshowdown;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedrunShowdown implements ModInitializer {
    public static final String MOD_ID = "speedrun-showdown";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int ONE_MINUTE = 20 * 60;
    private static final int FIVE_MINUTES = 20 * 60 * 5;
    private static final int FIFTEEN_MINUTES = 20 * 60 * 15;


    private static SSData getSSData(MinecraftServer server) {
        return ((SSDataOwner) server).speedrunshowdown$getData();
    }


    @Override
    public void onInitialize() {
        final Vec3 holdPos = new Vec3(0, 1000000, 0);
        ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> {
            SSData ssd = getSSData(minecraftServer);
            tickNetherTimer(minecraftServer);
            tickStartTimer(minecraftServer);
            if (ssd.rrts != null) {
                ServerLevel world = minecraftServer.getAllLevels().iterator().next();
                world.getGameRules().set(GameRules.RESPAWN_RADIUS, ssd.rrts, minecraftServer);
                ssd.rrts = null;
            }
            if (ssd.released) {
                for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                    if (!player.isAlive()) continue;
                    if (player.getY() < 500000) continue;
                    player.kill(player.level());
                }
            } else {
                for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                    if (!player.isAlive()) continue;
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10000, 100, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10000, 100, true, false));
                    player.teleport(new TeleportTransition(player.level(), holdPos, Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(Commands.literal("release").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                SSData ssd = getSSData(server);
                ssd.startTimer = 100;
                return 1;
            }));
            commandDispatcher.register(Commands.literal("unrelease").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                return unrelease(server) ? 1 : 0;
            }));
        });
    }

    private static boolean release(MinecraftServer server) {
        SSData ssd = getSSData(server);
        if (ssd.released) return false;
        ssd.released = true;
        instaKillAllPlayers(server);
        server.setWeatherParameters(0, 1, true, false);
        ServerClockManager clockManager = server.clockManager();
        server.getAllLevels().forEach(
                level -> level.dimensionTypeRegistration().value().defaultClock().ifPresent(
                        clock -> clockManager.setTotalTicks(clock, 0)
                )
        );
        ssd.netherTimer = FIFTEEN_MINUTES;
        return true;
    }

    private static boolean unrelease(MinecraftServer server) {
        SSData ssd = getSSData(server);
        if (!ssd.released) return false;
        ssd.released = false;
        instaKillAllPlayers(server);
        ssd.netherTimer = -1;
        ssd.startTimer = -1;
        return true;
    }

    private static void instaKillAllPlayers(MinecraftServer server) {
        SSData ssd = getSSData(server);
        ServerLevel world = server.getAllLevels().iterator().next();
        GameRules gameRules = world.getGameRules();
        ssd.rrts = gameRules.get(GameRules.RESPAWN_RADIUS);
        boolean oldDIR = gameRules.get(GameRules.IMMEDIATE_RESPAWN);
        gameRules.set(GameRules.RESPAWN_RADIUS, 0, server);
        gameRules.set(GameRules.IMMEDIATE_RESPAWN, true, server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.kill(player.level());
        }
        gameRules.set(GameRules.IMMEDIATE_RESPAWN, oldDIR, server);
    }

    public static boolean canEnterNether(MinecraftServer server) {
        return getSSData(server).netherTimer <= 0;
    }

    private static void tickNetherTimer(MinecraftServer server) {
        SSData ssd = getSSData(server);
        if (ssd.netherTimer < 0) return;
        // If there's more than 5 minutes left, announce every 5 minutes, otherwise announce every minute
        int announcementRate = ssd.netherTimer >= FIVE_MINUTES ? FIVE_MINUTES : ONE_MINUTE;
        if (ssd.netherTimer > 0 && ssd.netherTimer % announcementRate == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether opens in " + ssd.netherTimer / 20 / 60 + " minutes."), false);
        } else if (ssd.netherTimer == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether is now open!"), false);
        }

        if (ssd.netherTimer > 0 && ssd.netherTimer <= 100 && ssd.netherTimer % 20 == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The Nether opens in " + ssd.netherTimer / 20 + " seconds."), false);
        }
        ssd.netherTimer--;
    }

    private static void tickStartTimer(MinecraftServer server) {
        SSData ssd = getSSData(server);
        if (ssd.startTimer < 0) return;
        if (ssd.startTimer > 0 && ssd.startTimer % 20 == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("The game starts in " + ssd.startTimer / 20 + " seconds."), false);
        }
        if (ssd.startTimer == 0) {
            release(server);
        }
        ssd.startTimer--;
    }
}