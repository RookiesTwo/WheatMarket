package top.rookiestwo.wheatmarket;



public final class Wheatmarket {
    public static final String MOD_ID = "wheatmarket";
    public static final Wheatmarket INSTANCE = null;
    public static WheatMarketRegistry REGISTRY = null;

    public static void init() {
        System.out.println("WheatMarket Init");
        // Write common init code here.
        REGISTRY=new WheatMarketRegistry();
    }
}