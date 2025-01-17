package com.blamejared.crafttweaker.impl.commands.custom;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.mojang.brigadier.ParseResults;
import net.minecraft.command.CommandSource;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Map;
import java.util.stream.Collectors;

@ZenRegister
@ZenCodeType.Name("crafttweaker.api.commands.custom.MCParseResults")
@Document("vanilla/api/commands/custom/MCParseResults")
public class MCParseResults {
    
    private final ParseResults<CommandSource> internal;
    
    public MCParseResults(ParseResults<CommandSource> internal) {
        
        this.internal = internal;
    }
    
    public ParseResults<CommandSource> getInternal() {
        
        return internal;
    }
    
    @ZenCodeType.Method
    public MCCommandContextBuilder getContext() {
        
        return new MCCommandContextBuilder(getInternal().getContext());
    }
    
    @ZenCodeType.Method
    public MCImmutableStringReader getReader() {
        
        return new MCImmutableStringReader(getInternal().getReader());
    }
    
    @ZenCodeType.Method
    public Map<MCCommandNode, Exception> getExceptions() {
        
        return getInternal().getExceptions()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> MCCommandNode.convert(e.getKey()), Map.Entry::getValue));
    }
    
    @ZenCodeType.Method
    @Override
    public boolean equals(final Object o) {
        
        return o instanceof MCParseResults && getInternal().equals(((MCParseResults) o).getInternal());
    }
    
    @ZenCodeType.Operator(ZenCodeType.OperatorType.EQUALS)
    public boolean opEquals(final Object o) {
        
        return equals(o);
    }
    
    @ZenCodeType.Method
    public int hashCode() {
        
        return getInternal().hashCode();
    }
    
    @ZenCodeType.Method
    @Override
    public String toString() {
        
        return getInternal().toString();
    }
    
    @ZenCodeType.Caster(implicit = true)
    public String asString() {
        
        return toString();
    }
    
}
