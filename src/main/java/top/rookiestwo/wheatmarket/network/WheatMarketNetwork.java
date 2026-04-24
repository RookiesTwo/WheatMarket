package top.rookiestwo.wheatmarket.network;

import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.c2s.BuyItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.ListItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.ManageItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.RequestMarketListC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

public class WheatMarketNetwork {
    public static final NetworkChannel CHANNEL = NetworkChannel.create(
            ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "main_channel")
    );

    public static void init() {
        WheatMarket.LOGGER.info("WheatMarket Network Initializing...");

        CHANNEL.register(RequestMarketListC2SPacket.class,
                RequestMarketListC2SPacket::encode,
                RequestMarketListC2SPacket::new,
                RequestMarketListC2SPacket::apply);

        CHANNEL.register(BuyItemC2SPacket.class,
                BuyItemC2SPacket::encode,
                BuyItemC2SPacket::new,
                BuyItemC2SPacket::apply);

        CHANNEL.register(ListItemC2SPacket.class,
                ListItemC2SPacket::encode,
                ListItemC2SPacket::new,
                ListItemC2SPacket::apply);

        CHANNEL.register(ManageItemC2SPacket.class,
                ManageItemC2SPacket::encode,
                ManageItemC2SPacket::new,
                ManageItemC2SPacket::apply);

        CHANNEL.register(MarketListS2CPacket.class,
                MarketListS2CPacket::encode,
                MarketListS2CPacket::new,
                MarketListS2CPacket::apply);

        CHANNEL.register(OperationResultS2CPacket.class,
                OperationResultS2CPacket::encode,
                OperationResultS2CPacket::new,
                OperationResultS2CPacket::apply);

        CHANNEL.register(BalanceUpdateS2CPacket.class,
                BalanceUpdateS2CPacket::encode,
                BalanceUpdateS2CPacket::new,
                BalanceUpdateS2CPacket::apply);

        WheatMarket.LOGGER.info("WheatMarket Network Initialized.");
    }
}
