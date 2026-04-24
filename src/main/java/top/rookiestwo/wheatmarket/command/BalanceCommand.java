package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.rookiestwo.wheatmarket.WheatMarket;


public class BalanceCommand extends BaseCommand implements CommandInterface {

    String COMMAND_ROOT = "balance";

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(COMMAND_ROOT)
                        .executes(this::run)
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        ServerPlayer sender = commandContext.getSource().getPlayer();
        if (sender == null || WheatMarket.DATABASE == null) {
            return Command.SINGLE_SUCCESS;
        }

        WheatMarket.DATABASE.getEconomyService().getBalance(sender.getUUID()).thenAccept(result ->
                sender.server.execute(() -> {
                    if (result.isSuccess()) {
                        sender.sendSystemMessage(Component.translatable("info.command.wheatmarket.balance", formatMoney(result.getValue())));
                    } else {
                        sender.sendSystemMessage(Component.translatable(result.getMessageKey(), (Object[]) result.getMessageArgs()));
                    }
                })
        );
        return Command.SINGLE_SUCCESS;
    }
}
