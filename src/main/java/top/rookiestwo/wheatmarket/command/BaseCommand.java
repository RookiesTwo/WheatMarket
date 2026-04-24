package top.rookiestwo.wheatmarket.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.Locale;

public abstract class BaseCommand implements CommandInterface {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    }

    public int run(CommandContext<CommandSourceStack> commandContext) {
        return 0;
    }

    protected static String formatMoney(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
