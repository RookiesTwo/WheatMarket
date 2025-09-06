package top.rookiestwo.wheatmarket;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import top.rookiestwo.wheatmarket.blocks.LaptopBlock;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMainScreen;
import top.rookiestwo.wheatmarket.command.WheatMarketCommands;
import top.rookiestwo.wheatmarket.database.WheatMarketDatabase;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfoTable;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class WheatMarketRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.SOUND_EVENT);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(WheatMarket.MOD_ID, Registries.MENU);

    private static RegistrySupplier<Block> LAPTOP_BLOCK = null;
    public static RegistrySupplier<MenuType<WheatMarketMenu>> WHEAT_MARKET_MENU = null;

    public WheatMarketRegistry(){
        WheatMarket.LOGGER.info("WheatMarket Registering...");

        registerBlocks();
        registerItems();
        registerSounds();
        registerMenus();
        registerEvents();

        WheatMarketCommands.registerCommands();
    }

    private static void registerBlocks(){
        LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL)));
        BLOCKS.register();
    }

    private static void registerItems(){
        RegistrySupplier<BlockItem> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new Item.Properties()));
        ITEMS.register();
    }

    private static void registerSounds(){
        RegistrySupplier<SoundEvent> LAPTOP_OPEN_SOUND = SOUND_EVENTS.register("laptop_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_open")));
        RegistrySupplier<SoundEvent> LAPTOP_CLOSE_SOUND = SOUND_EVENTS.register("laptop_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_close")));
        SOUND_EVENTS.register();
    }

    private static void registerMenus() {
        WHEAT_MARKET_MENU = MENUS.register("wheat_market_menu", () -> new MenuType<>(WheatMarketMenu::new, FeatureFlags.DEFAULT_FLAGS));
        MENUS.register();
    }

    private static void registerClientScreens() {
        WheatMarket.LOGGER.info("WheatMarket Client Registering...");
        MenuRegistry.registerScreenFactory(WHEAT_MARKET_MENU.get(), WheatMarketMainScreen::new);
    }

    public static void registerEvents(){
        //服务器启动时
        LifecycleEvent.SERVER_STARTING.register((server) -> {
            //创建线程池
            WheatMarket.ASYNC =  Executors.newFixedThreadPool(4);

            //初始化数据库
            WheatMarket.DATABASE = new WheatMarketDatabase(Platform.getEnvironment());
        });

        //服务器关闭时
        LifecycleEvent.SERVER_STOPPING.register((server) -> {
            //关闭数据库连接
            WheatMarket.DATABASE.closeConnection();
        });

        //服务器保存时
        LifecycleEvent.SERVER_LEVEL_SAVE.register((server) -> {
            //保存数据库（待完成）
        });

        //玩家进入时
        PlayerEvent.PLAYER_JOIN.register((player) -> {
            //若没有信息则创建信息
            CompletableFuture.runAsync(() -> {
                PlayerInfoTable.ifNotExistsCreateRecord(WheatMarket.DATABASE.getConnection(), player.getUUID());
            }, WheatMarket.ASYNC);
        });

        /*==========Client Only Events=========*/
        if (Platform.getEnvironment() == Env.CLIENT) {
            //客户端启动时
            ClientLifecycleEvent.CLIENT_SETUP.register((client) -> {
                //注册界面
                registerClientScreens();
            });
        }
    }
}