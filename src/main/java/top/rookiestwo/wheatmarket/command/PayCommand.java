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
import top.rookiestwo.wheatmarket.service.EconomyService;

public class PayCommand extends BaseCommand implements CommandInterface {
    private final String COMMAND_ROOT = "pay";

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(COMMAND_ROOT)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                        .executes(this::run)
                                )
                        )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        if (commandContext.getSource().getPlayer() == null) {
            return Command.SINGLE_SUCCESS;
        }

        if (WheatMarket.DATABASE == null) {
            return Command.SINGLE_SUCCESS;
        }

        ServerPlayer sender = commandContext.getSource().getPlayer();
        double amount = DoubleArgumentType.getDouble(commandContext, "amount");
        try {
            ServerPlayer target = EntityArgument.getPlayer(commandContext, "player");
            WheatMarket.DATABASE.getEconomyService().transfer(sender.getUUID(), target.getUUID(), amount).thenAccept(result ->
                    sender.server.execute(() -> {
                        if (!result.isSuccess()) {
                            sender.sendSystemMessage(Component.translatable(result.getMessageKey(), (Object[]) result.getMessageArgs()).withColor(CommonColors.SOFT_RED));
                            return;
                        }

                        EconomyService.TransferResult transfer = result.getValue();
                        sender.sendSystemMessage(
                                Component.translatable("info.command.wheatmarket.pay_success", formatMoney(amount), target.getName().getString())
                                        .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(transfer.senderBalance())))
                                        .withColor(CommonColors.GREEN)
                        );
                        target.sendSystemMessage(
                                Component.translatable("info.command.wheatmarket.receive_success", formatMoney(amount), sender.getName().getString())
                                        .append(Component.translatable("info.command.wheatmarket.balance", formatMoney(transfer.targetBalance())))
                                        .withColor(CommonColors.GREEN));
                    })
            );
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            sender.sendSystemMessage(Component.translatable("error.command.wheatmarket.player_not_found_or_offline").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
    }
}
