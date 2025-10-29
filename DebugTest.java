public class DebugTest {
    public static void main(String[] args) {
        // Test what happens with varargs
        testVarargs();
        testVarargsWithNull();
    }
    
    public static void testVarargs(Object... args) {
        System.out.println("Args is null: " + (args == null));
        System.out.println("Args length: " + (args != null ? args.length : "null"));
        System.out.println("Args: " + java.util.Arrays.toString(args));
    }
    
    public static void testVarargsWithNull(Object... args) {
        System.out.println("Explicit null - Args is null: " + (args == null));
        System.out.println("Explicit null - Args length: " + (args != null ? args.length : "null"));
    }
    
    static {
        System.out.println("=== Testing varargs behavior ===");
        System.out.println("--- No args provided ---");
        testVarargs();
        System.out.println("--- Explicit null provided ---");
        testVarargsWithNull((Object[]) null);
    }
}