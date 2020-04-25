package com.blamejared.crafttweaker.api;

import com.blamejared.crafttweaker.CraftTweaker;
import com.blamejared.crafttweaker.api.annotations.*;
import com.blamejared.crafttweaker.api.brackets.CommandStringDisplayable;
import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import com.blamejared.crafttweaker.api.zencode.IPreprocessor;
import com.blamejared.crafttweaker.impl.brackets.RecipeTypeBracketHandler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CraftTweakerRegistry {
    
    private static final Type TYPE_ZEN_REGISTER = Type.getType(ZenRegister.class);
    private static final Type TYPE_PRE_PROCESSOR = Type.getType(Preprocessor.class);
    
    
    private static final List<Class> ZEN_CLASSES = new ArrayList<>();
    private static final List<Class> ZEN_GLOBALS = new ArrayList<>();
    private static final Map<String, List<Method>> BRACKET_RESOLVERS = new HashMap<>();
    private static final Map<String, Supplier<Collection<String>>> BRACKET_DUMPERS = new HashMap<>();
    private static final Map<String, Class> ZEN_CLASS_MAP = new HashMap<>();
    private static final List<IPreprocessor> PREPROCESSORS = new ArrayList<>();
    private static final Map<String, List<Class>> EXPANSIONS = new HashMap<>();
    
    /**
     * Find all classes that have a {@link ZenRegister} annotation and registers them to the class list for loading.
     */
    public static void findClasses() {
        ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> TYPE_ZEN_REGISTER.equals(a.getAnnotationType())).collect(Collectors.toList()).forEach(CraftTweakerRegistry::addClass);
        sortClasses();
        ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> TYPE_PRE_PROCESSOR.equals(a.getAnnotationType())).forEach(data -> {
            Type type = data.getClassType();
            try {
                Class<?> clazz = Class.forName(type.getClassName(), false, CraftTweaker.class.getClassLoader());
                boolean valid = false;
                for(Class<?> intFace : clazz.getInterfaces()) {
                    if(intFace == IPreprocessor.class) {
                        valid = true;
                        break;
                    }
                }
                if(!valid) {
                    CraftTweakerAPI.logWarning("Preprocessor: \"%s\" does not implement IPreprocessor!", type.getClassName());
                    return;
                }
                IPreprocessor preprocessor = null;
                for(Constructor<?> constructor : clazz.getConstructors()) {
                    if(constructor.getParameterCount() != 0) {
                        continue;
                    }
                    
                    try {
                        preprocessor = (IPreprocessor) constructor.newInstance();
                    } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        CraftTweakerAPI.logThrowing("Preprocessor: \"%s\" can not be instantiated! Make sure it has a public empty constructor", e);
                        e.printStackTrace();
                    }
                }
                if(preprocessor != null) {
                    PREPROCESSORS.add(preprocessor);
                } else {
                    CraftTweakerAPI.logWarning("Can not register Preprocessor: \"%s\"!", type.getClassName());
                }
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Adds a class to {@link CraftTweakerRegistry#ZEN_CLASSES} if it's required mod deps are found.
     *
     * @param data Scan data for a given class.
     */
    private static void addClass(ModFileScanData.AnnotationData data) {

        if(data.getAnnotationData().containsKey("modDeps")) {
            List<String> modOnly = (List<String>) data.getAnnotationData().get("modDeps");
            for(String mod : modOnly) {
                if(mod != null && !mod.isEmpty() && !ModList.get().isLoaded(mod)) {
                    return;
                }
            }
        }
        CraftTweaker.LOG.info("Found ZenRegister: {}", data.getClassType().getClassName());
        try {
            final Class<?> clazz = Class.forName(data.getClassType().getClassName(), false, CraftTweaker.class.getClassLoader());
            ZEN_CLASSES.add(clazz);

            if(!clazz.isInterface() && IRecipeManager.class.isAssignableFrom(clazz)) {
                RecipeTypeBracketHandler.registerRecipeManager(clazz);
            }
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sorts {@link CraftTweakerRegistry#ZEN_CLASSES} into their respective List/Maps.
     */
    private static void sortClasses() {
        for(Class zenClass : ZEN_CLASSES) {
            if(zenClass.isAnnotationPresent(ZenCodeType.Name.class)) {
                ZenCodeType.Name name = (ZenCodeType.Name) zenClass.getAnnotation(ZenCodeType.Name.class);
                ZEN_CLASS_MAP.put(name.value(), zenClass);
            }
            
            if(zenClass.isAnnotationPresent(ZenCodeType.Expansion.class)) {
                EXPANSIONS.computeIfAbsent(((ZenCodeType.Expansion) zenClass.getAnnotation(ZenCodeType.Expansion.class)).value(), s -> new ArrayList<>()).add(zenClass);
            }
            
            if(hasGlobal(zenClass)) {
                ZEN_GLOBALS.add(zenClass);
            }
            
            for(Method method : zenClass.getDeclaredMethods()) {
                handleBracketResolver(method);
                handleBracketDumper(method);
            }
        }
    }
    
    private static void handleBracketResolver(Method method) {
        if(!method.isAnnotationPresent(BracketResolver.class)) {
            return;
        }
        if(!Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as a BracketResolver, but it is not public and static.", method.toString());
            return;
        }
        Class<?>[] parameters = method.getParameterTypes();
        if(parameters.length == 1 && parameters[0].equals(String.class)) {
            Class<?> cls = method.getDeclaringClass();
            final ZenCodeType.Name annotation = cls.getAnnotation(ZenCodeType.Name.class);
            final String name = annotation == null ? cls.getCanonicalName() : annotation.value();
            BRACKET_RESOLVERS.computeIfAbsent(name.split("[.]", 2)[0], s -> new ArrayList<>())
                    .add(method);
        } else {
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as a BracketResolver, but it does not have a String as it's only parameter.", method.toString());
        }

        if(!CommandStringDisplayable.class.isAssignableFrom(method.getReturnType())){
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as a BracketResolver, so it should return something that implements %s.", method.toString(), CommandStringDisplayable.class.getSimpleName());
        }
    }
    
    private static void handleBracketDumper(Method method) {
        if(!method.isAnnotationPresent(BracketDumper.class)) {
            return;
        }
        if(!Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as a BracketDumper, but it is not public and static.", method.toString());
            return;
        }
        
        if(method.getParameterCount() != 0) {
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as BracketDumper but does not have 0 parameters.", method.toString());
            return;
        }
        
        if(!Collection.class.isAssignableFrom(method.getReturnType()) || ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0] != String.class) {
            CraftTweakerAPI.logWarning("Method \"%s\" is marked as BracketDumper but does not have 'Collection<String>' as return type.", method.toGenericString());
            return;
        }
    
        final String value = method.getAnnotation(BracketDumper.class).value();
    
        BRACKET_DUMPERS.merge(value, () -> {
            try {
                //noinspection unchecked
                return (Collection<String>) method.invoke(null);
            } catch(InvocationTargetException | IllegalAccessException ignored) {
                return null;
            }
        }, (dumpFun1, dumpFun2) -> () -> {
            final Collection<String> strings1 = dumpFun1.get();
            final Collection<String> strings2 = dumpFun2.get();
            final Collection<String> result = new HashSet<>(strings1.size() + strings2.size());
            result.addAll(strings1);
            result.addAll(strings2);
            return result;
        });
    }
    
    
    /**
     * Used to determine if the class has a field / method
     *
     * @param zenClass class to scan
     *
     * @return true if this class has a global annotation on a method.
     */
    private static boolean hasGlobal(Class zenClass) {
        for(Method method : zenClass.getDeclaredMethods()) {
            if(method.isAnnotationPresent(ZenCodeGlobals.Global.class) && Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                return true;
            }
        }
        
        for(Field field : zenClass.getDeclaredFields()) {
            if(field.isAnnotationPresent(ZenCodeGlobals.Global.class) && Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Gets an ImmutableMap of {@link CraftTweakerRegistry#ZEN_CLASS_MAP}.
     * <p>
     * Classes should not be added manually to this map. See {@link ZenRegister}
     *
     * @return Map of String -> Class for ZenName -> Java class
     */
    public static Map<String, Class> getZenClassMap() {
        return ImmutableMap.copyOf(ZEN_CLASS_MAP);
    }
    
    /**
     * Gets all classes with a given package root.
     * <p>
     * Providing "crafttweaker" as the name can return:
     * <p>
     * {@code "crafttweaker.sub.package.Class}
     * and
     * {@code "crafttweaker.sub.other.package.Class}
     *
     * @param name Name of the Zen Package.
     *
     * @return list of classes in the Zen Package.
     */
    public static List<Class> getClassesInPackage(String name) {
        return ZEN_CLASS_MAP.keySet().stream().filter(key -> key.startsWith(name)).map(ZEN_CLASS_MAP::get).collect(Collectors.toList());
    }
    
    /**
     * Gets all the top level Zen Packages.
     *
     * @return Set of top level packages
     */
    public static Set<String> getRootPackages() {
        return ZEN_CLASS_MAP.keySet().stream().map(s -> s.split("\\.")[0]).collect(Collectors.toSet());
    }
    
    /**
     * Gets the ZenGlobals list.
     *
     * @return ImmutableList of the ZenGlobals
     */
    public static List<Class> getZenGlobals() {
        return ImmutableList.copyOf(ZEN_GLOBALS);
    }
    
    /**
     * Gets the Bracket Resolver list.
     *
     * @return ImmutableList of the Bracket Resolvers
     */
    public static List<Method> getBracketResolvers(String rootPackage) {
        return ImmutableList.copyOf(BRACKET_RESOLVERS.getOrDefault(rootPackage, Collections.emptyList()));
    }
    
    /**
     * Gets the Bracket dumper Map
     * @return ImmutableMap of the Bracket dumpers
     */
    public static Map<String, Supplier<Collection<String>>> getBracketDumpers() {
        return ImmutableMap.copyOf(BRACKET_DUMPERS);
    }
    
    public static List<IPreprocessor> getPreprocessors() {
        return PREPROCESSORS;
    }
    
    public static Map<String, List<Class>> getExpansions() {
        return EXPANSIONS;
    }
}
