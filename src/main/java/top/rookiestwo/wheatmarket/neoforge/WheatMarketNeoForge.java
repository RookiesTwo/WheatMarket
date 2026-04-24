package top.rookiestwo.wheatmarket.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;

@Mod(WheatMarket.MOD_ID)
public final class WheatMarketNeoForge {
    public static final String MOD_ID = WheatMarket.MOD_ID;


    public WheatMarketNeoForge(IEventBus modBus) {
        // Run our common setup.
        WheatMarket.init(modBus);
        modBus.addListener(WheatMarketRegistry::registerClientScreens);
    }

}
