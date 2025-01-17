package com.blamejared.crafttweaker.impl.data;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.api.data.IData;
import com.blamejared.crafttweaker.api.data.INumberData;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.openzen.zencode.java.ZenCodeType;

/**
 * Careful with BoolData: While it works for specifying boolean attributes in JSON syntax,
 * using it in Tags will instead use a {@link ByteData} object. Reason for this is that
 * Minecraft does not have Boolean NBT values.
 *
 * @docParam this true
 */
@ZenCodeType.Name("crafttweaker.api.data.BoolData")
@ZenRegister
@Document("vanilla/api/data/BoolData")
public class BoolData implements IData {
    
    private final boolean internal;
    
    @ZenCodeType.Constructor
    public BoolData(boolean internal) {
        
        this.internal = internal;
    }
    
    /**
     * Converts this BoolData to a {@link ByteData} object.
     * This will be used when this Data is converted to NBT
     */
    @ZenCodeType.Caster
    @ZenCodeType.Method
    public ByteData getByteData() {
        
        return new ByteData(ByteNBT.valueOf(getInternalValue()));
    }
    
    @Override
    public IData copy() {
        
        return new BoolData(getInternalValue());
    }
    
    @Override
    public IData copyInternal() {
        
        return new BoolData(getInternalValue());
    }
    
    @Override
    public INBT getInternal() {
        
        return getByteData().getInternal();
    }
    
    // There is no such thing as a BooleanNBT, it uses the byte nbt, so override this if you need to I guess
    public boolean getInternalValue() {
        
        return internal;
    }
    
    @Override
    public String asString() {
        
        return getInternalValue() + " as bool";
    }
    
    @Override
    public ITextComponent asFormattedComponent(String indentation, int indentDepth) {
        
        ITextComponent as = new StringTextComponent(" as ").mergeStyle(IData.SYNTAX_HIGHLIGHTING_AS);
        ITextComponent type = new StringTextComponent("bool").mergeStyle(IData.SYNTAX_HIGHLIGHTING_TYPE);
        return new StringTextComponent(toJsonString()).append(as).append(type)
                .mergeStyle(IData.SYNTAX_HIGHLIGHTING_NUMBER);
    }
    
    @Override
    public INumberData asNumber() {
        
        return new IntData(getInternalValue() ? 1 : 0);
    }
    
    @Override
    public String toJsonString() {
        
        return Boolean.toString(getInternalValue());
    }
    
    @Override
    public boolean equals(Object o) {
    
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        
        BoolData boolData = (BoolData) o;
        
        return internal == boolData.internal;
    }
    
    @Override
    public int hashCode() {
        
        return (internal ? 1 : 0);
    }
    
}
