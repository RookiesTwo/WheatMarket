package top.rookiestwo.wheatmarket;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.rookiestwo.wheatmarket.blocks.LaptopBlock;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMainScreen;
import top.rookiestwo.wheatmarket.command.WheatMarketCommands;
import top.rookiestwo.wheatmarket.database.WheatMarketDatabase;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, WheatMarket.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, WheatMarket.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, WheatMarket.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WheatMarket.MOD_ID);
    public static DeferredHolder<MenuType<?>, MenuType<WheatMarketMenu>> WHEAT_MARKET_MENU = null;
    public static DeferredHolder<SoundEvent, SoundEvent> PAPER_FLIPPING = null;
    public static DeferredHolder<SoundEvent, SoundEvent> PENCIL_WRITING = null;
    public static DeferredHolder<SoundEvent, SoundEvent> ORDER_STAMP = null;
    private static DeferredHolder<Block, Block> LAPTOP_BLOCK = null;

    public WheatMarketRegistry(IEventBus modBus) {
        WheatMarket.LOGGER.info("WheatMarket Registering...");

        registerBlocks();
        registerItems();
        registerSounds();
        registerMenus();
        registerDeferredRegisters(modBus);
        registerEvents();

        WheatMarketCommands.registerCommands();
    }

    private static void registerBlocks() {
        LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL)));
    }

    private static void registerItems() {
        ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new Item.Properties()));
    }

    private static void registerSounds() {
        PAPER_FLIPPING = SOUND_EVENTS.register("paper_flipping", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "paper_flipping")));
        PENCIL_WRITING = SOUND_EVENTS.register("pencil_writing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "pencil_writing")));
        ORDER_STAMP = SOUND_EVENTS.register("order_stamp", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "order_stamp")));
        SOUND_EVENTS.register("laptop_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_open")));
        SOUND_EVENTS.register("laptop_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "laptop_close")));
    }

    private static void registerMenus() {
        WHEAT_MARKET_MENU = MENUS.register("wheat_market_menu", () -> new MenuType<>(WheatMarketMenu::new, FeatureFlags.DEFAULT_FLAGS));
    }

    private static void registerDeferredRegisters(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUND_EVENTS.register(modBus);
        MENUS.register(modBus);
    }

    public static void registerClientScreens(RegisterMenuScreensEvent event) {
        WheatMarket.LOGGER.info("WheatMarket Client Registering...");
        event.register(WHEAT_MARKET_MENU.get(), WheatMarketRegistry::createMainScreen);
    }

    private static WheatMarketMainScreen createMainScreen(WheatMarketMenu menu, Inventory inventory, Component title) {
        return new WheatMarketMainScreen(menu, inventory, title);
    }

    private static final int CLEANUP_INTERVAL_TICKS = 20 * 60 * 5;
    private static int cleanupTickCounter;

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(WheatMarketRegistry::onServerStarting);
        NeoForge.EVENT_BUS.addListener(WheatMarketRegistry::onServerStopping);
        NeoForge.EVENT_BUS.addListener(WheatMarketRegistry::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(WheatMarketRegistry::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (++cleanupTickCounter < CLEANUP_INTERVAL_TICKS) {
            return;
        }
        cleanupTickCounter = 0;
        if (WheatMarket.DATABASE != null) {
            WheatMarket.DATABASE.getMarketService().cleanupExpiredOrders();
        }
    }

    private static void onServerStarting(ServerStartingEvent event) {
        WheatMarket.DATABASE = new WheatMarketDatabase(FMLEnvironment.dist);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        if (WheatMarket.DATABASE != null) {
            WheatMarket.DATABASE.closeConnection();
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (WheatMarket.DATABASE == null) {
            return;
        }

        WheatMarket.DATABASE.getEconomyService()
                .ensurePlayerRecord(event.getEntity().getUUID());
    }
}
