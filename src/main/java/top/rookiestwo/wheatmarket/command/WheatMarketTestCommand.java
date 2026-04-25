package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.service.EconomyService;
import top.rookiestwo.wheatmarket.service.MarketService;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class WheatMarketTestCommand extends BaseCommand implements CommandInterface {
    private static final String COMMAND_ROOT = "wmtest";
    private static final UUID TEST_PAYER = testUuid("payer");
    private static final UUID TEST_TARGET = testUuid("target");
    private static final UUID TEST_SELLER = testUuid("seller");
    private static final UUID TEST_BUYER = testUuid("buyer");
    private static final UUID TEST_MARKET_ITEM = testUuid("market_item");

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(COMMAND_ROOT)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("data")
                                .executes(this::run)
                        )
                        .then(Commands.literal("listings")
                                .executes(context -> addRandomWheatListings(context, 10))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> addRandomWheatListings(context, IntegerArgumentType.getInteger(context, "count")))
                                )
                        )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        ServerPlayer player = commandContext.getSource().getPlayer();
        if (player == null || WheatMarket.DATABASE == null) {
            return Command.SINGLE_SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.data_start").withColor(CommonColors.SOFT_YELLOW));
        runDataBoundaryTest(player);
        return Command.SINGLE_SUCCESS;
    }

    private int addRandomWheatListings(CommandContext<CommandSourceStack> commandContext, int count) {
        ServerPlayer player = commandContext.getSource().getPlayer();
        if (player == null) {
            return Command.SINGLE_SUCCESS;
        }
        if (WheatMarket.DATABASE == null) {
            player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.database_not_initialized").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.listings_add_start", count).withColor(CommonColors.SOFT_YELLOW));
        MarketService marketService = WheatMarket.DATABASE.getMarketService();
        List<CompletableFuture<ServiceResult<MarketService.ListItemResult>>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            futures.add(marketService.listItem(createRandomWheatListing(player, i)));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) -> player.server.execute(() -> {
            if (throwable != null) {
                WheatMarket.LOGGER.error("Failed to add random wheat listings.", throwable);
                player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.failed", throwable.getMessage()).withColor(CommonColors.SOFT_RED));
                return;
            }

            int added = 0;
            int failed = 0;
            for (CompletableFuture<ServiceResult<MarketService.ListItemResult>> future : futures) {
                ServiceResult<MarketService.ListItemResult> result = future.join();
                if (result.isSuccess()) {
                    added++;
                } else {
                    failed++;
                }
            }
            player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.listings_add_result", added, failed).withColor(failed == 0 ? CommonColors.GREEN : CommonColors.SOFT_YELLOW));
        }));
        return Command.SINGLE_SUCCESS;
    }

    private void runDataBoundaryTest(ServerPlayer player) {
        EconomyService economyService = WheatMarket.DATABASE.getEconomyService();
        MarketService marketService = WheatMarket.DATABASE.getMarketService();
        TestReport report = new TestReport();

        CompletableFuture<Void> test = CompletableFuture.completedFuture(null)
                .thenCompose(ignored -> economyService.setBalance(TEST_PAYER, 100.0)
                        .thenAccept(result -> report.expectSuccess("economy: set payer balance", result)))
                .thenCompose(ignored -> economyService.setBalance(TEST_TARGET, 0.0)
                        .thenAccept(result -> report.expectSuccess("economy: set target balance", result)))
                .thenCompose(ignored -> economyService.transfer(TEST_PAYER, TEST_TARGET, 40.0)
                        .thenAccept(result -> {
                            report.expectSuccess("economy: transfer within balance", result);
                            if (result.isSuccess()) {
                                EconomyService.TransferResult transfer = result.getValue();
                                report.expectEquals("economy: payer balance after transfer", 60.0, transfer.senderBalance());
                                report.expectEquals("economy: target balance after transfer", 40.0, transfer.targetBalance());
                            }
                        }))
                .thenCompose(ignored -> economyService.transfer(TEST_PAYER, TEST_TARGET, 1000.0)
                        .thenAccept(result -> report.expectFailure("economy: reject insufficient transfer", result, "info.command.wheatmarket.not_enough_money")))
                .thenCompose(ignored -> economyService.transfer(TEST_PAYER, TEST_TARGET, Double.NaN)
                        .thenAccept(result -> report.expectFailure("economy: reject NaN transfer", result, "gui.wheatmarket.operation.invalid_amount")))
                .thenCompose(ignored -> economyService.setBalance(TEST_PAYER, Double.POSITIVE_INFINITY)
                        .thenAccept(result -> report.expectFailure("economy: reject infinite balance", result, "gui.wheatmarket.operation.invalid_amount")))
                .thenCompose(ignored -> economyService.getBalance(TEST_PAYER)
                        .thenAccept(result -> report.expectBalance("economy: payer unchanged after failed transfer", result, 60.0)))
                .thenCompose(ignored -> economyService.getBalance(TEST_TARGET)
                        .thenAccept(result -> report.expectBalance("economy: target unchanged after failed transfer", result, 40.0)))
                .thenCompose(ignored -> economyService.setBalance(TEST_SELLER, 0.0)
                        .thenAccept(result -> report.expectSuccess("market: set seller balance", result)))
                .thenCompose(ignored -> economyService.setBalance(TEST_BUYER, 0.0)
                        .thenAccept(result -> report.expectSuccess("market: set buyer balance", result)))
                .thenCompose(ignored -> marketService.delist(TEST_SELLER, true, TEST_MARKET_ITEM)
                        .thenAccept(result -> report.expectSuccessOrMissing("market: cleanup before test", result)))
                .thenCompose(ignored -> marketService.listItem(createTestMarketItem(player, Double.NaN))
                        .thenAccept(result -> report.expectFailure("market: reject NaN price", result, "gui.wheatmarket.operation.invalid_price")))
                .thenCompose(ignored -> marketService.listItem(createTestMarketItem(player))
                        .thenAccept(result -> report.expectSuccess("market: list test item", result)))
                .thenCompose(ignored -> marketService.buyItem(TEST_BUYER, TEST_MARKET_ITEM, 2)
                        .thenAccept(result -> report.expectFailure("market: reject amount above stock", result, "gui.wheatmarket.operation.invalid_amount")))
                .thenCompose(ignored -> marketService.buyItem(TEST_BUYER, TEST_MARKET_ITEM, 1)
                        .thenAccept(result -> report.expectFailure("market: reject insufficient balance", result, "gui.wheatmarket.operation.insufficient_balance")))
                .thenCompose(ignored -> marketService.delist(TEST_SELLER, true, TEST_MARKET_ITEM)
                        .thenAccept(result -> report.expectSuccess("market: cleanup after test", result)));

        test.whenComplete((ignored, throwable) -> player.server.execute(() -> {
            if (throwable != null) {
                WheatMarket.LOGGER.error("WheatMarket data boundary test failed unexpectedly.", throwable);
                report.fail("unexpected exception: " + throwable.getMessage());
            }
            report.sendTo(player);
        }));
    }

    private MarketItem createTestMarketItem(ServerPlayer player) {
        return createTestMarketItem(player, 50.0);
    }

    private MarketItem createTestMarketItem(ServerPlayer player, double price) {
        ItemStack stack = new ItemStack(Items.WHEAT);
        CompoundTag nbt = (CompoundTag) stack.save(player.server.registryAccess());

        MarketItem item = new MarketItem();
        item.setMarketItemID(TEST_MARKET_ITEM);
        item.setItemID(BuiltInRegistries.ITEM.getKey(Items.WHEAT).toString());
        item.setItemNBTCompound(nbt);
        item.setSellerID(TEST_SELLER);
        item.setPrice(price);
        item.setAmount(1);
        item.setListingTime(new Timestamp(System.currentTimeMillis()));
        item.setIfAdmin(false);
        item.setIfSell(true);
        item.setCooldownAmount(0);
        item.setCooldownTimeInMinutes(0);
        item.setTimeToExpire(0);
        item.setLastTradeTime(null);
        return item;
    }

    private MarketItem createRandomWheatListing(ServerPlayer player, int index) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack stack = new ItemStack(Items.WHEAT);
        CompoundTag nbt = (CompoundTag) stack.save(player.server.registryAccess());
        double price = Math.round(random.nextDouble(0.01, 100.0) * 100.0) / 100.0;

        MarketItem item = new MarketItem();
        item.setMarketItemID(UUID.randomUUID());
        item.setItemID(BuiltInRegistries.ITEM.getKey(Items.WHEAT).toString());
        item.setItemNBTCompound(nbt);
        item.setSellerID(player.getUUID());
        item.setPrice(price);
        item.setAmount(1);
        item.setListingTime(new Timestamp(System.currentTimeMillis() - index));
        item.setIfAdmin(false);
        item.setIfSell(random.nextBoolean());
        item.setCooldownAmount(0);
        item.setCooldownTimeInMinutes(0);
        item.setTimeToExpire(0);
        item.setLastTradeTime(null);
        return item;
    }

    private static UUID testUuid(String name) {
        return UUID.nameUUIDFromBytes((WheatMarket.MOD_ID + ":test:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static class TestReport {
        private final List<String> failures = new ArrayList<>();
        private int passed = 0;

        void expectSuccess(String name, ServiceResult<?> result) {
            if (result.isSuccess()) {
                passed++;
            } else {
                fail(name + " expected success, got " + result.getMessageKey());
            }
        }

        void expectSuccessOrMissing(String name, ServiceResult<?> result) {
            if (result.isSuccess() || "gui.wheatmarket.operation.item_not_found".equals(result.getMessageKey())) {
                passed++;
            } else {
                fail(name + " expected success or missing item, got " + result.getMessageKey());
            }
        }

        void expectFailure(String name, ServiceResult<?> result, String expectedKey) {
            if (!result.isSuccess() && expectedKey.equals(result.getMessageKey())) {
                passed++;
            } else if (result.isSuccess()) {
                fail(name + " expected failure " + expectedKey + ", got success");
            } else {
                fail(name + " expected failure " + expectedKey + ", got " + result.getMessageKey());
            }
        }

        void expectBalance(String name, ServiceResult<Double> result, double expected) {
            if (!result.isSuccess()) {
                fail(name + " expected balance " + expected + ", got " + result.getMessageKey());
                return;
            }
            expectEquals(name, expected, result.getValue());
        }

        void expectEquals(String name, double expected, double actual) {
            if (Math.abs(expected - actual) < 0.0001) {
                passed++;
            } else {
                fail(name + " expected " + expected + ", got " + actual);
            }
        }

        void fail(String message) {
            failures.add(message);
        }

        void sendTo(ServerPlayer player) {
            if (failures.isEmpty()) {
                player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.pass", passed).withColor(CommonColors.GREEN));
                return;
            }

            player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.fail", failures.size(), passed).withColor(CommonColors.SOFT_RED));
            for (String failure : failures) {
                player.sendSystemMessage(Component.translatable("info.command.wheatmarket.test.failure_detail", failure).withColor(CommonColors.SOFT_RED));
            }
        }
    }
}
