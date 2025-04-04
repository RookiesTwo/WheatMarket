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
import net.minecraft.world.entity.player.Player;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfo;

public class WheatMarketCommands {

    private final String WheatMarketCommand="wheatmarket";
    private final String PayCommand="pay";

    /*
    * 指令：/account <PlayerName> <add|set|remove> <amount>  管理员指令，调整玩家余额
    * 指令：/pay <PlayerName> <amount> 玩家指令，给xxx付款
    * 指令：/wheatmarket 显示版权信息
    */

    public WheatMarketCommands() {
        registerCommands();
    }

    private void registerCommands(){
        CommandRegistrationEvent.EVENT.register((CommandDispatcher, CommandBuildContext, CommandSelection)->{
            CommandDispatcher.register(Commands.literal(WheatMarketCommand).executes(this::WheatMarketCommand));
        });
        CommandRegistrationEvent.EVENT.register((CommandDispatcher, CommandBuildContext, CommandSelection)->{
            CommandDispatcher.register(
                    Commands.literal(PayCommand)
                    .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0F))
                                    .executes(this::PayCommand)
                            )
                    )
            );
        });
    }

    private int WheatMarketCommand(CommandContext<CommandSourceStack> commandContext){
        commandContext.getSource().sendSuccess(() -> Component.literal("Wheat Market. By Rookies__Two/Stagnater/Florililio_UwU"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int PayCommand(CommandContext<CommandSourceStack> commandContext){
        ServerPlayer sender=commandContext.getSource().getPlayer();
        double amount = DoubleArgumentType.getDouble(commandContext, "amount");

        if(PlayerInfo.getPlayerBalance(WheatMarket.DATABASE.getConnection(),sender.getUUID())<amount){
            sender.sendSystemMessage(Component.translatable("info.command.wheatmarket.not_enough_money").withColor(CommonColors.SOFT_RED));
            return Command.SINGLE_SUCCESS;
        }
        try{
            ServerPlayer player = EntityArgument.getPlayer(commandContext, "target");
            try{
                PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),player.getUUID(),amount);
                PlayerInfo.addPlayerBalance(WheatMarket.DATABASE.getConnection(),sender.getUUID(),0-amount);

                sender.sendSystemMessage(Component.translatable("info.command.wheatmarket.pay_success").withColor(CommonColors.GREEN));
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
