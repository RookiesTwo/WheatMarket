package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public interface CommandInterface {
    void register(CommandDispatcher<CommandSourceStack> dispatcher);
    int run(CommandContext<CommandSourceStack> commandContext);
}
