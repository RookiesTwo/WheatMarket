package top.rookiestwo.wheatmarket.network;

import net.minecraft.server.level.ServerPlayer;
import top.rookiestwo.wheatmarket.WheatMarket;

public class WheatMarketNetwork {
    public static final LegacyChannel CHANNEL = new LegacyChannel();

    public static void init() {
        WheatMarket.LOGGER.info("WheatMarket legacy network isolated; NeoForge payload wiring is pending LDLib2 UI integration.");
    }

    public static class LegacyChannel {
        public void sendToPlayer(ServerPlayer player, Object packet) {
            WheatMarket.LOGGER.debug("Skipped legacy packet {} for {}", packet.getClass().getSimpleName(), player.getGameProfile().getName());
        }
    }
}
