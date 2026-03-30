import java.lang.reflect.*;

public class Check {
    public static void main(String[] args) throws Exception {
        Class<?> c = Class.forName("com.linecorp.bot.messaging.model.PushMessageRequest");
        System.out.println("=== Constructors ===");
        for (Constructor<?> cons : c.getConstructors()) {
            System.out.println(cons);
        }

        System.out.println("=== Methods ===");
        for (Method m : c.getMethods()) {
            System.out.println(m);
        }
    }
}
