package top.rookiestwo.wheatmarket.command;

public class WheatMarketCommands {
    public static void registerCommands(){
        AccountCommand accountCommand = new AccountCommand();
        BalanceCommand balanceCommand = new BalanceCommand();
        PayCommand payCommand = new PayCommand();
    }
}
