package top.rookiestwo.wheatmarket;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class WheatMarketRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.ITEM);

    public WheatMarketRegistry(){
        System.out.println("WheatMarket Registering...");
        RegistrySupplier<Item> LAPTOP = ITEMS.register("laptop", () -> new Item(new Item.Properties()));
        ITEMS.register();
    }
}
