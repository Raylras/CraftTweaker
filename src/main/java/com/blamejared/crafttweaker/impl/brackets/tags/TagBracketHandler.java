package com.blamejared.crafttweaker.impl.brackets.tags;

import com.blamejared.crafttweaker.impl.brackets.util.ParseUtil;
import com.blamejared.crafttweaker.impl_native.util.ExpandResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.lexer.ZSTokenType;
import org.openzen.zenscript.parser.BracketExpressionParser;
import org.openzen.zenscript.parser.expression.ParsedCallArguments;
import org.openzen.zenscript.parser.expression.ParsedExpression;
import org.openzen.zenscript.parser.expression.ParsedExpressionCall;
import org.openzen.zenscript.parser.expression.ParsedExpressionMember;
import org.openzen.zenscript.parser.expression.ParsedExpressionString;
import org.openzen.zenscript.parser.expression.ParsedNewExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagBracketHandler implements BracketExpressionParser {
    
    private final TagManagerBracketHandler tagManagerBracketHandler;
    
    public TagBracketHandler(TagManagerBracketHandler tagManagerBracketHandler) {
        
        this.tagManagerBracketHandler = tagManagerBracketHandler;
    }
    
    @Override
    public ParsedExpression parse(CodePosition position, ZSTokenParser tokens) throws ParseException {
        
        if(tokens.optional(ZSTokenType.T_GREATER) != null) {
            throw new ParseException(position, "Invalid Bracket handler, expected tagFolder here");
        }
        final String tagFolder = tokens.next().getContent();
        tagManagerBracketHandler.confirmTagFolderExists(tagFolder, position);
        
        tokens.required(ZSTokenType.T_COLON, "Expected ':', followed by Tag Name");
        
        final String tagName = ParseUtil.readContent(tokens);
        final ResourceLocation resourceLocation = ResourceLocation.tryCreate(tagName);
        if(resourceLocation == null) {
            throw new ParseException(position, "Invalid Tag Name '" + tagName + "', must be a valid resource location");
        }
        
        return createCall(position, tagFolder, resourceLocation);
        
    }
    
    private ParsedExpression createCall(CodePosition position, String tagFolder, ResourceLocation location) throws ParseException {
        
        final ParsedExpression tagManager = tagManagerBracketHandler.getParsedExpression(position, tagFolder);
        final ParsedExpressionMember getTag = new ParsedExpressionMember(position, tagManager, "getTag", null);
        final ParsedNewExpression newExpression = createResourceLocationArgument(position, location);
        
        final ParsedCallArguments arguments = new ParsedCallArguments(null, Collections.singletonList(newExpression));
        return new ParsedExpressionCall(position, getTag, arguments);
    }
    
    private ParsedNewExpression createResourceLocationArgument(CodePosition position, ResourceLocation location) {
        
        final List<ParsedExpression> arguments = new ArrayList<>(2);
        arguments.add(new ParsedExpressionString(position, location.getNamespace(), false));
        arguments.add(new ParsedExpressionString(position, location.getPath(), false));
        final ParsedCallArguments newCallArguments = new ParsedCallArguments(null, arguments);
        return new ParsedNewExpression(position, ParseUtil.readParsedType(ExpandResourceLocation.ZC_CLASS_NAME, position), newCallArguments);
    }
    
}
