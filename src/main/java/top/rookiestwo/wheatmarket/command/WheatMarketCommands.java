package top.rookiestwo.wheatmarket.command;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class WheatMarketCommands {
    public static void registerCommands(){
        NeoForge.EVENT_BUS.addListener(WheatMarketCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        new AccountCommand().register(event.getDispatcher());
        new BalanceCommand().register(event.getDispatcher());
        new PayCommand().register(event.getDispatcher());
    }
}
