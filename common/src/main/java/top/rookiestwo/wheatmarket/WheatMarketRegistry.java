package top.rookiestwo.wheatmarket;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class WheatMarketRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.BLOCK);
    public WheatMarketRegistry(){
        System.out.println("WheatMarket Registering...");
        RegistrySupplier<Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new Block(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
        ));
        RegistrySupplier<BlockItem> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new Item.Properties()));
        BLOCKS.register();
        ITEMS.register();
    }
}