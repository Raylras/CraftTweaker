package com.blamejared.crafttweaker.impl.tag.registry;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.CraftTweakerRegistry;
import com.blamejared.crafttweaker.api.util.InstantiationUtil;
import com.blamejared.crafttweaker.impl.tag.manager.TagManager;
import com.blamejared.crafttweaker.impl.tag.manager.TagManagerItem;
import com.blamejared.crafttweaker.impl.tag.manager.TagManagerWrapper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public final class CrTTagRegistryData {
    
    public static final CrTTagRegistryData INSTANCE = new CrTTagRegistryData();
    
    /**
     * Classes that implement {@link TagManager} themselves.
     * The recommended way since this ensure that the proper classes are used
     */
    private final Map<String, TagManager<?>> registeredInstances = new HashMap<>();
    
    /**
     * Implementations that were created by CrT on a best-effort basis
     */
    private final Map<String, TagManagerWrapper<?>> syntheticInstances = new HashMap<>();
    
    /**
     * Maps the ZC type -> TagManager implementation, e.g. MCItemDefinition -> {@link TagManagerItem}
     */
    private final Map<Class<?>, TagManager<?>> tagFolderByCrTElementType = new HashMap<>();
    
    private CrTTagRegistryData() {
    
    }
    
    @SuppressWarnings("rawtypes")
    public void addTagImplementationClass(Class<? extends TagManager> cls) {
        
        final TagManager manager = InstantiationUtil.getOrCreateInstance(cls);
        if(manager == null) {
            throw new IllegalArgumentException("TagManagers need to have a public static final instance field or a no-arg constructor");
        }
        
        CraftTweakerAPI.logDebug("Registering Native TagManager with TagFolder '%s'", manager.getTagFolder());
        register(manager);
        
    }
    
    private void register(TagManager<?> tagManager) {
        
        final String tagFolder = tagManager.getTagFolder();
        if(getAllInstances().containsKey(tagFolder)) {
            handleDuplicateTagManager(tagManager, tagFolder);
            return;
        }
        
        if(tagManager instanceof TagManagerWrapper) {
            syntheticInstances.put(tagFolder, (TagManagerWrapper<?>) tagManager);
        } else {
            registeredInstances.put(tagFolder, tagManager);
        }
        tagFolderByCrTElementType.put(tagManager.getElementClass(), tagManager);
    }
    
    private void handleDuplicateTagManager(TagManager<?> tagManager, String tagFolder) {
        
        final String message = "There are two tagManagers registered for tagfolder %s! Classes are '%s' and '%s'.";
        final String nameA = tagManager.getClass().getCanonicalName();
        final String nameB = getAllInstances().get(tagFolder).getClass().getCanonicalName();
        CraftTweakerAPI.logError(message, tagFolder, nameA, nameB);
    }
    
    public void registerForgeTags() {
        
        final RegistryManager registryManager = RegistryManager.ACTIVE;
        for(final ResourceLocation key : ForgeTagHandler.getCustomTagTypeNames()) {
            if(registryManager.getRegistry(key) == null) {
                CraftTweakerAPI.logWarning("Unsupported TagCollection without registry: " + key);
                continue;
            }
            
            final ForgeRegistry<?> registry = registryManager.getRegistry(key);
            String tagFolder = registry.getTagFolder();
            if(tagFolder == null) {
                if(key.getNamespace().equals("minecraft")) {
                    tagFolder = key.getPath();
                } else {
                    CraftTweakerAPI.logWarning("Could not find tagFolder for registry '%s'", key);
                    continue;
                }
            }
            
            if(hasTagManager(tagFolder)) {
                //We already have a custom TagManager for this.
                continue;
            }
            CraftTweakerAPI.logDebug("Creating Wrapper TagManager for type '%s' with tag folder '%s'", key, tagFolder);
            registerTagManagerFromRegistry(key, registry, tagFolder);
        }
    }
    
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerTagManagerFromRegistry(ResourceLocation name, ForgeRegistry<?> registry, String tagFolder) {
        
        final Class<?> registrySuperType = registry.getRegistrySuperType();
        final Optional<String> s = CraftTweakerRegistry.tryGetZenClassNameFor(registrySuperType);
        if(!s.isPresent()) {
            CraftTweakerAPI.logDebug("Could not register tag manager for " + tagFolder);
            return;
        }
        
        register(new TagManagerWrapper(registrySuperType, name, tagFolder));
        
    }
    
    
    public boolean hasTagManager(String location) {
        
        return getAllInstances().containsKey(location);
    }
    
    private Map<String, TagManager<?>> getAllInstances() {
        
        final HashMap<String, TagManager<?>> result = new HashMap<>(registeredInstances);
        result.putAll(syntheticInstances);
        
        return result;
    }
    
    /**
     * {@code TagRegistry.get<Item>()}
     */
    @SuppressWarnings({"unchecked"})
    <T> TagManager<T> getForElementType(Class<T> cls) {
        
        return (TagManager<T>) tagFolderByCrTElementType.get(cls);
    }
    
    /**
     * {@code TagRegistry.get<Item>("minecraft:item")}
     */
    @SuppressWarnings("rawtypes")
    <T> TagManager<T> getForRegistry(ResourceLocation location) {
        
        final ForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(location);
        if(registry == null) {
            throw new IllegalArgumentException("Unknown registry name: " + location);
        }
        
        return getByTagFolder(registry.getTagFolder());
    }
    
    public <T extends TagManager<?>> T getByImplementation(Class<T> cls) {
        //Here it's enough to use registeredInstances, since all syntheticInstances are TagManagerWrapper anyways
        for(TagManager<?> value : registeredInstances.values()) {
            if(cls.isInstance(value)) {
                return cls.cast(value);
            }
        }
        throw new IllegalArgumentException("Unknown tag implementation name: " + cls);
    }
    
    public boolean isSynthetic(String tagFolder) {
        
        return syntheticInstances.containsKey(tagFolder);
    }
    
    public String getElementZCTypeFor(String tagFolder) {
        
        final Map<String, TagManager<?>> allInstances = getAllInstances();
        if(!allInstances.containsKey(tagFolder)) {
            throw new IllegalArgumentException("Could not find registry for name " + tagFolder);
        }
        
        final Class<?> elementClass = allInstances.get(tagFolder).getElementClass();
        final Optional<String> s = CraftTweakerRegistry.tryGetZenClassNameFor(elementClass);
        return s.orElseThrow(() -> new IllegalArgumentException("Cannot find ZC type for name " + tagFolder));
    }
    
    public String getImplementationZCTypeFor(String location) {
        //Is only called for custom impls, so registeredInstances is fine.
        //Also, syntheticInstances would not have a name annotation.
        final TagManager<?> tagManager = registeredInstances.get(location);
        return tagManager.getClass().getAnnotation(ZenCodeType.Name.class).value();
    }
    
    @SuppressWarnings("unchecked")
    public <T> TagManager<T> getByTagFolder(String location) {
        
        for(TagManager<?> value : getAllInstances().values()) {
            if(value.getTagFolder().equals(location)) {
                return (TagManager<T>) value;
            }
        }
        throw new IllegalArgumentException("No TagManager with tag folder " + location + " is registered");
    }
    
    public Collection<TagManager<?>> getAll() {
        
        return getAllInstances().values();
    }
    
}
