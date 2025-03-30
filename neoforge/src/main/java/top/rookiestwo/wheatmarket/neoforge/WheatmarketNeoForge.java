package top.rookiestwo.wheatmarket.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.rookiestwo.wheatmarket.Wheatmarket;
import net.neoforged.fml.common.Mod;

@Mod(Wheatmarket.MOD_ID)
public final class WheatmarketNeoForge {
    public static final String MOD_ID = Wheatmarket.MOD_ID;


    public WheatmarketNeoForge() {
        // Run our common setup.
        Wheatmarket.init();
    }

}
