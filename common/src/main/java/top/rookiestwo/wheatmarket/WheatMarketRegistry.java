package top.rookiestwo.wheatmarket;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class WheatMarketRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.BLOCK);
    public WheatMarketRegistry(){
        System.out.println("WheatMarket Registering...");
        RegistrySupplier<Item> LAPTOP = ITEMS.register("laptop", () -> new Item(new Item.Properties()));
        RegistrySupplier<Block> LAPTOP_BLOCK = BLOCKS.register("laptop_block", () -> new Block(Block.Properties.of()));
        ITEMS.register();
    }
}