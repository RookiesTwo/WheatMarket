package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfo;

public class PayCommand extends BaseCommand implements CommandInterface {
    private final String COMMAND_ROOT = "pay";

    @Override
    public void register() {
        CommandRegistrationEvent.EVENT.register((CommandDispatcher, CommandBuildContext, CommandSelection)->{
            CommandDispatcher.register(
                    Commands.literal(COMMAND_ROOT)
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                            .executes(this::run)
                                    )
                            )
            );
        });
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext){
        if(commandContext.getSource().getPlayer() == null){
            return Command.SINGLE_SUCCESS;
        }

        ServerPlayer sender=commandContext.getSource().getPlayer();
        double amount = DoubleArgumentType.getDouble(commandContext, "amount");
        double senderBalance = PlayerInfo.getPlayerBalance(WheatMarket.DATABASE.getConnection(),sender.getUUID());

        if(senderBalance<amount){
            sender.sendSystemMessage(Component.translatable("info.command.wheatmarket.not_enough_money").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
        try{
            ServerPlayer target = EntityArgument.getPlayer(commandContext, "player");
            try{
                PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),sender.getUUID(),0-amount);
                PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID(),amount);

                double targetBalance = PlayerInfo.getPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID());
                senderBalance-=amount;

                sender.sendSystemMessage(
                        Component.translatable("info.command.wheatmarket.pay_success",String.valueOf(amount),sender.getName().getString())
                                .append(Component.translatable("info.command.wheatmarket.balance",senderBalance))
                                .withColor(CommonColors.GREEN)
                );
                target.sendSystemMessage(
                        Component.translatable("info.command.wheatmarket.receive_success",String.valueOf(amount),sender.getName().getString())
                                .append(Component.translatable("info.command.wheatmarket.balance",targetBalance))
                                .withColor(CommonColors.GREEN));

                return Command.SINGLE_SUCCESS;
            } catch (Exception e){
                WheatMarket.LOGGER.error("Pay command failed.", e);
                return 0;
            }
        }
        catch(CommandSyntaxException e){
            sender.sendSystemMessage(Component.translatable("error.command.wheatmarket.player_not_found_or_offline").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
    }
}
