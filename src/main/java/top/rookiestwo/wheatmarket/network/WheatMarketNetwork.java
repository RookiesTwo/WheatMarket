package top.rookiestwo.wheatmarket.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.c2s.*;
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

public class WheatMarketNetwork {
    private static final String NETWORK_VERSION = "1";

    public static void init(IEventBus modBus) {
        modBus.addListener(WheatMarketNetwork::registerPayloads);
        WheatMarket.LOGGER.info("WheatMarket Network Initializing...");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        registrar.playToServer(RequestMarketListC2SPacket.TYPE, RequestMarketListC2SPacket.STREAM_CODEC, RequestMarketListC2SPacket::handle);
        registrar.playToServer(BuyItemC2SPacket.TYPE, BuyItemC2SPacket.STREAM_CODEC, BuyItemC2SPacket::handle);
        registrar.playToServer(ListItemC2SPacket.TYPE, ListItemC2SPacket.STREAM_CODEC, ListItemC2SPacket::handle);
        registrar.playToServer(ManageItemC2SPacket.TYPE, ManageItemC2SPacket.STREAM_CODEC, ManageItemC2SPacket::handle);
        registrar.playToServer(SetItemSelectionModeC2SPacket.TYPE, SetItemSelectionModeC2SPacket.STREAM_CODEC, SetItemSelectionModeC2SPacket::handle);
        registrar.playToServer(BeginStockEditC2SPacket.TYPE, BeginStockEditC2SPacket.STREAM_CODEC, BeginStockEditC2SPacket::handle);
        registrar.playToServer(FinalizeStockEditC2SPacket.TYPE, FinalizeStockEditC2SPacket.STREAM_CODEC, FinalizeStockEditC2SPacket::handle);

        registrar.playToClient(MarketListS2CPacket.TYPE, MarketListS2CPacket.STREAM_CODEC, MarketListS2CPacket::handle);
        registrar.playToClient(OperationResultS2CPacket.TYPE, OperationResultS2CPacket.STREAM_CODEC, OperationResultS2CPacket::handle);
        registrar.playToClient(BalanceUpdateS2CPacket.TYPE, BalanceUpdateS2CPacket.STREAM_CODEC, BalanceUpdateS2CPacket::handle);

        WheatMarket.LOGGER.info("WheatMarket Network Initialized.");
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
