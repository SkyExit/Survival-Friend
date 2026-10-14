package de.laurinhummel.survivalfriend;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDirFactory;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class TestServerFixtures {
    private TestServerFixtures() { }

    /** Modern Bukkit constants are populated by server registries rather than enums. */
    public static void registries(MockedStatic<Bukkit> bukkit) throws Exception {
        if (Attribute.class.isEnum()) return;
        Map<Class<?>, Object> registries = new HashMap<>();
        bukkit.when(() -> Bukkit.getRegistry(any(Class.class))).thenAnswer(call -> {
            Class<?> type = call.getArgument(0);
            return registries.computeIfAbsent(type, ignored -> {
                Map<Object, Object> entries = new HashMap<>();
                return Proxy.newProxyInstance(Registry.class.getClassLoader(), new Class<?>[]{Registry.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "get", "getOrThrow" -> registryEntry(entries, type, args[0]);
                            case "iterator" -> entries.values().iterator();
                            case "stream" -> entries.values().stream();
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "TestRegistry<" + type.getSimpleName() + ">";
                            default -> null;
                        });
            });
        });
        Class.forName(Registry.class.getName(), true, Registry.class.getClassLoader());
    }

    private static Object registryEntry(Class<?> type) {
        if (type.getName().equals("org.bukkit.block.BlockType") || type.getName().equals("org.bukkit.inventory.ItemType")) {
            try { return mock(Class.forName(type.getName() + "$Typed")); }
            catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
        }
        return mock(type);
    }

    private static Object registryEntry(Map<Object, Object> entries, Class<?> type, Object key) {
        Object value = entries.get(key);
        if (value != null) return value;
        value = registryEntry(type);
        Object initializedDuringClassLoading = entries.putIfAbsent(key, value);
        return initializedDuringClassLoading == null ? value : initializedDuringClassLoading;
    }

    public static void legacyItemFactory(ItemFactory factory) throws Exception {
        try {
            Method update = ItemFactory.class.getMethod("updateMaterial", ItemMeta.class, Material.class);
            when((Material) update.invoke(factory, any(ItemMeta.class), any(Material.class)))
                    .thenAnswer(call -> call.getArgument(1));
        } catch (NoSuchMethodException ignored) { }
    }

    public static final class WorkspaceTempDir implements TempDirFactory {
        @Override
        public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws Exception {
            Path root = Path.of("target", "test-tmp").toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path temporary = Files.createTempDirectory(root, "rules-").toRealPath();
            if (!temporary.startsWith(root.toRealPath())) throw new IllegalStateException("Test path escaped target");
            return temporary;
        }
    }
}
