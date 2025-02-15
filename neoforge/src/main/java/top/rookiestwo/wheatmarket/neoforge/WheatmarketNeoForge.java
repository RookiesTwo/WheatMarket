package top.rookiestwo.wheatmarket.neoforge;

import top.rookiestwo.wheatmarket.Wheatmarket;
import net.neoforged.fml.common.Mod;

@Mod(Wheatmarket.MOD_ID)
public final class WheatmarketNeoForge {
    public WheatmarketNeoForge() {
        // Run our common setup.
        Wheatmarket.init();
    }
}
