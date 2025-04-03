package top.rookiestwo.wheatmarket;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import top.rookiestwo.wheatmarket.blocks.LaptopBlock;

public class WheatMarketRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Wheatmarket.MOD_ID, Registries.SOUND_EVENT);

    public WheatMarketRegistry(){
        Wheatmarket.LOGGER.info("WheatMarket Registering...");
        RegistrySupplier<Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
        ));
        RegistrySupplier<BlockItem> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new Item.Properties()));
        RegistrySupplier<SoundEvent> LAPTOP_OPEN_SOUND = SOUND_EVENTS.register("laptop_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Wheatmarket.MOD_ID, "laptop_open")));
        RegistrySupplier<SoundEvent> LAPTOP_CLOSE_SOUND = SOUND_EVENTS.register("laptop_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Wheatmarket.MOD_ID, "laptop_close")));
        SOUND_EVENTS.register();
        BLOCKS.register();
        ITEMS.register();
    }
}