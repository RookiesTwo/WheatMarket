package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public abstract class BaseCommand implements CommandInterface {
    public BaseCommand() {this.register();}

    public void register() {}

    public int run(CommandContext<CommandSourceStack> commandContext) {return 0;}
}
