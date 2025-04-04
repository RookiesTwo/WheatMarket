package top.rookiestwo.wheatmarket;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import top.rookiestwo.wheatmarket.blocks.LaptopBlock;
import top.rookiestwo.wheatmarket.database.WheatMarketDatabase;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfo;

public class WheatMarketRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.SOUND_EVENT);

    public WheatMarketRegistry(){
        WheatMarket.LOGGER.info("WheatMarket Registering...");
        RegistrySupplier<Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
        ));
        RegistrySupplier<BlockItem> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new Item.Properties()));
        RegistrySupplier<SoundEvent> LAPTOP_OPEN_SOUND = SOUND_EVENTS.register("laptop_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_open")));
        RegistrySupplier<SoundEvent> LAPTOP_CLOSE_SOUND = SOUND_EVENTS.register("laptop_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_close")));

        SOUND_EVENTS.register();
        BLOCKS.register();
        ITEMS.register();
    }

    public static void registerEvents(){
        //服务器启动时启动数据库
        LifecycleEvent.SERVER_STARTING.register((server) -> {
            WheatMarket.DATABASE=new WheatMarketDatabase();
        });

        //服务器关闭时关闭数据库
        LifecycleEvent.SERVER_STOPPING.register((server) -> {
            WheatMarket.DATABASE.closeConnection();
        });

        //玩家进入时，若没有账号则创建账号
        PlayerEvent.PLAYER_JOIN.register((player) -> {
            PlayerInfo.ifNotExistsCreateRecord(WheatMarket.DATABASE.getConnection(),player.getUUID());
        });
    }
}