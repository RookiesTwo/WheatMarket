package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public interface CommandInterface {
    void register();
    int run(CommandContext<CommandSourceStack> commandContext);
}