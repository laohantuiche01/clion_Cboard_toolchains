import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class CheckPlugin263 {
    public static void main(String[] args) throws Exception {
        Class<?> buildClass = Class.forName("com.intellij.openapi.util.BuildNumber");
        Object build = buildClass.getMethod("fromString", String.class).invoke(null, "CL-263.7307.257");
        Class<?> loader = Class.forName("com.intellij.ide.plugins.PluginDescriptorLoader");
        Object descriptor = loader.getMethod("loadDescriptorFromArtifact", Path.class, buildClass)
            .invoke(null, Path.of(args[0]), build);
        if (descriptor == null) throw new AssertionError("No plugin descriptor loaded");
        Class<?> descriptorInterface = Class.forName("com.intellij.ide.plugins.IdeaPluginDescriptor");
        boolean incompatible = (boolean) Class.forName("com.intellij.ide.plugins.PluginManagerCore")
            .getMethod("isIncompatible", descriptorInterface, buildClass).invoke(null, descriptor, build);
        boolean expected = Boolean.parseBoolean(args[1]);
        if (incompatible != expected) throw new AssertionError("Unexpected compatibility verdict: " + incompatible);
        System.out.println("PASS: actual CLion 263 compatibility checker: incompatible=" + incompatible);
        if (expected) return;
        int checked = 0;
        try (JarFile jar = new JarFile(args[2])) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (!name.endsWith(".class") || !name.startsWith("com/github/waveformplotter/")) continue;
                Class<?> type = Class.forName(name.substring(0, name.length()-6).replace('/', '.'), false,
                    CheckPlugin263.class.getClassLoader());
                type.getDeclaredConstructors();
                type.getDeclaredMethods();
                type.getDeclaredFields();
                checked++;
            }
        }
        System.out.println("PASS: linked " + checked + " plugin classes against installed IDE libraries");
    }
}
