package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public interface CommandInterface {
    public void register();
    public int run(CommandContext<CommandSourceStack> commandContext);
}