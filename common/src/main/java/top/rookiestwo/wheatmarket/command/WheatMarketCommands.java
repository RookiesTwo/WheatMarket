package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class WheatMarketCommands {

    private final String WheatMarketCommand="wheatmarket";

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
        //PayCommand
    }

    private int WheatMarketCommand(CommandContext<CommandSourceStack> commandContext){
        commandContext.getSource().sendSuccess(() -> Component.literal("Wheat Market. By Rookies__Two/Stagnater/Florililio_UwU").withColor(CommonColors.SOFT_YELLOW), false);
        return Command.SINGLE_SUCCESS;
    }


}
