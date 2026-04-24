package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfoTable;


public class BalanceCommand extends BaseCommand implements CommandInterface {

    String COMMAND_ROOT = "balance";

    @Override
    public void register() {
        CommandRegistrationEvent.EVENT.register((CommandDispatcher, CommandBuildContext, CommandSelection)->{
            CommandDispatcher.register(
                    Commands.literal(COMMAND_ROOT)
                            .executes(this::run)
            );
        });
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        ServerPlayer sender = commandContext.getSource().getPlayer();
        double balance = PlayerInfoTable.getPlayerBalance(WheatMarket.DATABASE.getConnection(), sender.getUUID());
        sender.sendSystemMessage(Component.translatable("info.command.wheatmarket.balance", String.valueOf(balance)));
        return Command.SINGLE_SUCCESS;
    }
}
