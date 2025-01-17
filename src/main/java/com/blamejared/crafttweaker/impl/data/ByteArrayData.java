package com.blamejared.crafttweaker.impl.data;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.api.data.ICollectionData;
import com.blamejared.crafttweaker.api.data.IData;
import com.blamejared.crafttweaker.api.data.INumberData;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * @docParam this [4, 1, 2]
 */
@ZenCodeType.Name("crafttweaker.api.data.ByteArrayData")
@ZenRegister
@Document("vanilla/api/data/ByteArrayData")
public class ByteArrayData implements ICollectionData {
    
    private final ByteArrayNBT internal;
    
    public ByteArrayData(ByteArrayNBT internal) {
        
        this.internal = internal;
    }
    
    @ZenCodeType.Constructor
    public ByteArrayData(byte[] internal) {
        
        this.internal = new ByteArrayNBT(internal);
    }
    
    @Override
    public IData copy() {
        
        return new ByteArrayData(getInternal());
    }
    
    @Override
    public IData copyInternal() {
        
        return new ByteArrayData((ByteArrayNBT) getInternal().copy());
    }
    
    @Override
    public ByteArrayNBT getInternal() {
        
        return internal;
    }
    
    @Override
    public IData setAt(int index, IData value) {
        
        if(value instanceof NumberNBT) {
            return new ByteData(getInternal().set(index, ByteNBT.valueOf(((INumberData) value).getByte())));
        } else {
            return null;
        }
    }
    
    
    @Override
    public void add(int index, IData value) {
        
        if(value instanceof INumberData) {
            getInternal().add(index, ByteNBT.valueOf(((INumberData) value).getByte()));
        }
    }
    
    @Override
    public void add(IData value) {
        
        if(value instanceof INumberData) {
            getInternal().add(ByteNBT.valueOf(((INumberData) value).getByte()));
        }
    }
    
    @Override
    public IData remove(int index) {
        
        return new ByteData(getInternal().remove(index));
    }
    
    @Override
    public IData getAt(int index) {
        
        return new ByteData(getInternal().get(index));
    }
    
    @Override
    public int size() {
        
        return getInternal().size();
    }
    
    @Override
    public boolean isEmpty() {
        
        return getInternal().isEmpty();
    }
    
    @Override
    public void clear() {
        
        getInternal().clear();
    }
    
    @Override
    public String asString() {
        
        StringBuilder result = new StringBuilder();
        result.append("[");
        boolean first = true;
        for(ByteNBT nbt : getInternal()) {
            if(first) {
                first = false;
            } else {
                result.append(", ");
            }
            
            result.append(nbt.getByte());
        }
        result.append("]");
        return result.toString();
    }
    
    @Override
    public ITextComponent asFormattedComponent(String indentation, int indentDepth) {
        
        ITextComponent as = new StringTextComponent(" as ").mergeStyle(IData.SYNTAX_HIGHLIGHTING_AS);
        ITextComponent baseType = new StringTextComponent("byte").mergeStyle(IData.SYNTAX_HIGHLIGHTING_TYPE);
        ITextComponent type = new StringTextComponent("byte[]").mergeStyle(IData.SYNTAX_HIGHLIGHTING_TYPE);
        IFormattableTextComponent component = new StringTextComponent("[");
        
        for(int i = 0; i < size(); ++i) {
            IFormattableTextComponent child = new StringTextComponent(getAt(i).toJsonString()).mergeStyle(IData.SYNTAX_HIGHLIGHTING_NUMBER);
            component.appendString(i == 0 ? "" : " ").append(child);
            if(i != size() - 1) {
                component.appendString(",");
            }
        }
        
        component.appendString("]").append(as).append(type);
        return component;
    }
    
    @Override
    public List<IData> asList() {
        
        final byte[] byteArray = getInternal().getByteArray();
        final List<IData> out = new ArrayList<>(byteArray.length);
        for(byte b : byteArray) {
            out.add(new ByteData(b));
        }
        return out;
    }
    
    @Override
    public boolean equals(Object o) {
    
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        
        ByteArrayData that = (ByteArrayData) o;
        
        return internal.equals(that.internal);
    }
    
    @Override
    public int hashCode() {
        
        return internal.hashCode();
    }
    
}
