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

public class AccountCommand extends BaseCommand implements CommandInterface {

    String COMMAND_ROOT = "account";

    @Override
    public void register() {
        CommandRegistrationEvent.EVENT.register((CommandDispatcher, CommandBuildContext, CommandSelection)->{
            CommandDispatcher.register(
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
        });
    }

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) {
        CommandSourceStack source = commandContext.getSource();
        double amount = DoubleArgumentType.getDouble(commandContext, "amount");
        try{
            ServerPlayer target = EntityArgument.getPlayer(commandContext, "player");
            try{
                String keyWord = commandContext.getInput().split(" ")[2];
                switch (keyWord){
                    case "add"->{
                        PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID(),amount);
                        double targetBalance=PlayerInfo.getPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID());
                        source.sendSystemMessage(
                                Component
                                    .translatable("info.command.wheatmarket.admin_add_balance",String.valueOf(amount),target.getName().getString())
                                    .append(Component.translatable("info.command.wheatmarket.balance",String.valueOf(targetBalance)))
                                    .withColor(CommonColors.SOFT_YELLOW)
                        );
                        target.sendSystemMessage(
                                Component
                                    .translatable("info.command.wheatmarket.receive_from_admin",String.valueOf(amount))
                                    .append(Component.translatable("info.command.wheatmarket.balance",String.valueOf(targetBalance)))
                                    .withColor(CommonColors.SOFT_YELLOW)
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    case "set"->{
                        PlayerInfo.updatePlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID(),amount);
                        source.sendSystemMessage(
                                Component
                                    .translatable("info.command.wheatmarket.admin_set_balance",target.getName().getString(),String.valueOf(amount))
                                    .withColor(CommonColors.SOFT_YELLOW)
                        );
                        target.sendSystemMessage(
                                Component
                                    .translatable("info.command.wheatmarket.set_from_admin",String.valueOf(amount))
                                    .withColor(CommonColors.SOFT_YELLOW)
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    case "remove"->{
                        double targetBalance=PlayerInfo.getPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID());
                        if(amount>targetBalance){
                            source.sendSystemMessage(
                                    Component
                                        .translatable("error.command.wheatmarket.admin_remove_balance",target.getName().getString())
                                        .append(Component.translatable("info.command.wheatmarket.balance",String.valueOf(targetBalance)))
                                        .withColor(CommonColors.SOFT_RED)
                            );
                        }
                        else{
                            PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),target.getUUID(),0-amount);
                            source.sendSystemMessage(
                                    Component
                                            .translatable("info.command.wheatmarket.admin_remove_balance",String.valueOf(amount),target.getName().getString())
                                            .append(Component.translatable("info.command.wheatmarket.balance",String.valueOf(targetBalance-amount)))
                                            .withColor(CommonColors.SOFT_YELLOW)
                            );
                            target.sendSystemMessage(
                                    Component
                                            .translatable("info.command.wheatmarket.remove_from_admin",String.valueOf(amount))
                                            .append(Component.translatable("info.command.wheatmarket.balance",String.valueOf(targetBalance-amount)))
                                            .withColor(CommonColors.SOFT_YELLOW)
                            );
                        }
                        return Command.SINGLE_SUCCESS;
                    }
                }
            } catch(Exception e){
                WheatMarket.LOGGER.error("Account command failed.", e);
                return Command.SINGLE_SUCCESS;
            }
        } catch (CommandSyntaxException e){
            source.sendSystemMessage(Component.translatable("error.command.wheatmarket.player_not_found_or_offline").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
