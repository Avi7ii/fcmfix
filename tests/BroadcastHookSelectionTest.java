import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import javax.tools.*;

/** Tests the production selector with reflection fixtures, not Android delivery. */
public class BroadcastHookSelectionTest {
    static String method(String name, int count, int badIndex, String badType, String returnType) {
        var params = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            String type = i == 3 ? "android.content.Intent" : i == 13 ? "int" : i == 14 ? "android.app.BroadcastOptions" : "Object";
            if (i == badIndex) type = badType;
            params.add(type + " p" + i);
        }
        return "public " + returnType + " " + name + "(" + String.join(",", params) + "){ return " + (returnType.equals("int") ? "0" : "false") + "; }";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the path to BroadcastFix.java");
        String source = Files.readString(Path.of(args[0]));
        int start = source.indexOf("    protected void startHookBroadcastIntentLocked()");
        int end = source.indexOf("    protected void createBroadcastIntentLockedHooker", start);
        if (start < 0 || end < 0) throw new AssertionError("Actual method not found");
        String selection = source.substring(start, end).replace("protected void startHook", "public void startHook");
        Path root = Files.createTempDirectory("fcm-hook-selection-test-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("android/os/Build.java", "package android.os; public class Build { public static class VERSION { public static int SDK_INT; } public static class VERSION_CODES { public static final int Q=29,R=30; } }");
        sources.put("android/content/Intent.java", "package android.content; public class Intent {}");
        sources.put("android/app/BroadcastOptions.java", "package android.app; public class BroadcastOptions {}");
        String signatures = "package test; public class Signatures {";
        signatures += method("outer", 25, -1, "", "int");
        signatures += method("valid", 26, -1, "", "int");
        signatures += method("wrongArity", 27, -1, "", "int");
        signatures += method("wrongIntent", 26, 3, "Object", "int");
        signatures += method("wrongAppOp", 26, 13, "Object", "int");
        signatures += method("wrongOptions", 26, 14, "Object", "int");
        signatures += method("wrongReturn", 26, -1, "", "boolean");
        sources.put("test/Signatures.java", signatures + "}");
        sources.put("test/XposedUtils.java", "package test; import java.lang.reflect.*; public class XposedUtils { public static Method outer, inner; public static Method tryFindMethodMostParam(ClassLoader l,String c,String m){return m.equals(\"broadcastIntentLockedTraced\")?inner:outer;} }");
        sources.put("test/Selection.java", "package test; import java.lang.reflect.*; import android.os.Build; import android.content.Intent; public class Selection { ClassLoader classLoader; public Method selected; public int hooks; void printLog(String s){} void createBroadcastIntentLockedHooker(int i,int a,Method m){if(i!=3||a!=13)throw new AssertionError(\"Argument indices changed\"); selected=m; hooks++;}" + selection + "}");
        var files = new ArrayList<String>();
        for (var e : sources.entrySet()) {
            Path path = root.resolve(e.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, e.getValue());
            files.add(path.toString());
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Run with a JDK, not a JRE");
        try (var fm = compiler.getStandardFileManager(null, null, null)) {
            if (!compiler.getTask(null, fm, null, List.of("-d", root.toString()), null,
                    fm.getJavaFileObjectsFromStrings(files)).call()) throw new AssertionError("Fixture compilation failed");
        }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()})) {
            Class<?> sdk = loader.loadClass("android.os.Build$VERSION");
            Class<?> utils = loader.loadClass("test.XposedUtils");
            Class<?> fixtures = loader.loadClass("test.Signatures");
            Class<?> selectionClass = loader.loadClass("test.Selection");
            var methods = new HashMap<String, Method>();
            for (Method m : fixtures.getDeclaredMethods()) methods.put(m.getName(), m);
            utils.getField("outer").set(null, methods.get("outer"));
            Object[][] cases = {
                {36, "valid", "valid"}, {35, "valid", "outer"}, {37, "valid", "valid"},
                {38, "valid", "outer"},
                {34, "valid", "outer"}, {36, null, "outer"},
                {36, "wrongArity", "outer"}, {36, "wrongIntent", "outer"},
                {36, "wrongAppOp", "outer"}, {36, "wrongOptions", "outer"},
                {36, "wrongReturn", "outer"},
                {37, null, "outer"}, {37, "wrongArity", "outer"},
                {37, "wrongIntent", "outer"}, {37, "wrongAppOp", "outer"},
                {37, "wrongOptions", "outer"}, {37, "wrongReturn", "outer"}
            };
            for (Object[] test : cases) {
                sdk.getField("SDK_INT").setInt(null, (int)test[0]);
                utils.getField("inner").set(null, methods.get(test[1]));
                Object instance = selectionClass.getConstructor().newInstance();
                selectionClass.getMethod("startHookBroadcastIntentLocked").invoke(instance);
                Method selected = (Method) selectionClass.getField("selected").get(instance);
                if (selected == null || !selected.getName().equals(test[2]) || selectionClass.getField("hooks").getInt(instance) != 1) {
                    throw new AssertionError(Arrays.toString(test) + " selected " + selected);
                }
                System.out.println("PASS SDK " + test[0] + ", inner=" + test[1] + " -> " + test[2]);
            }
        }
    }
}
