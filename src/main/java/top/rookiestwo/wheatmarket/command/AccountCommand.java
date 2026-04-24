package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import top.rookiestwo.wheatmarket.WheatMarket;

public class AccountCommand extends BaseCommand implements CommandInterface {

    String COMMAND_ROOT = "account";

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands
                        .literal(COMMAND_ROOT)
                        .requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                                .executes(this::run)
                                        )
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                                .executes(this::run)
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                                .executes(this::run)
                                        )
                                )
                        )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        CommandSourceStack source = commandContext.getSource();
        double amount = DoubleArgumentType.getDouble(commandContext, "amount");
        if (WheatMarket.DATABASE == null) {
            return Command.SINGLE_SUCCESS;
        }
        try {
            ServerPlayer target = EntityArgument.getPlayer(commandContext, "player");
            try {
                String keyWord = commandContext.getInput().split(" ")[2];
                switch (keyWord) {
                    case "add" -> {
                        WheatMarket.DATABASE.getEconomyService().addBalance(target.getUUID(), amount).thenAccept(result ->
                                source.getServer().execute(() -> {
                                    if (!result.isSuccess()) {
                                        source.sendSystemMessage(Component.translatable(result.getMessageKey(), (Object[]) result.getMessageArgs()).withColor(CommonColors.SOFT_RED));
                                        return;
                                    }
                                    double targetBalance = result.getValue();
                                    source.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.admin_add_balance", formatMoney(amount), target.getName().getString())
                                                    .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(targetBalance)))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                    target.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.receive_from_admin", formatMoney(amount))
                                                    .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(targetBalance)))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                })
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    case "set" -> {
                        WheatMarket.DATABASE.getEconomyService().setBalance(target.getUUID(), amount).thenAccept(result ->
                                source.getServer().execute(() -> {
                                    if (!result.isSuccess()) {
                                        source.sendSystemMessage(Component.translatable(result.getMessageKey(), (Object[]) result.getMessageArgs()).withColor(CommonColors.SOFT_RED));
                                        return;
                                    }
                                    source.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.admin_set_balance", target.getName().getString(), formatMoney(amount))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                    target.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.set_from_admin", formatMoney(amount))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                })
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    case "remove" -> {
                        WheatMarket.DATABASE.getEconomyService().removeBalance(target.getUUID(), amount).thenAccept(result ->
                                source.getServer().execute(() -> {
                                    if (!result.isSuccess()) {
                                        source.sendSystemMessage(
                                                Component
                                                        .translatable(result.getMessageKey(), target.getName().getString())
                                                        .withColor(CommonColors.SOFT_RED)
                                        );
                                        return;
                                    }
                                    double targetBalance = result.getValue();
                                    source.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.admin_remove_balance", formatMoney(amount), target.getName().getString())
                                                    .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(targetBalance)))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                    target.sendSystemMessage(
                                            Component
                                                    .translatable("info.command.wheatmarket.remove_from_admin", formatMoney(amount))
                                                    .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(targetBalance)))
                                                    .withColor(CommonColors.SOFT_YELLOW)
                                    );
                                })
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                }
            } catch (Exception e) {
                WheatMarket.LOGGER.error("Account command failed.", e);
                return Command.SINGLE_SUCCESS;
            }
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(Component.translatable("error.command.wheatmarket.player_not_found_or_offline").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
