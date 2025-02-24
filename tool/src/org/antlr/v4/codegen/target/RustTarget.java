/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */

package org.antlr.v4.codegen.target;

import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.codegen.Target;
import org.antlr.v4.codegen.UnicodeEscapes;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarAST;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.misc.STMessage;

import java.util.*;

public class RustTarget extends Target {
	protected static final Map<Character, String> targetCharValueEscape;

	static {
		HashMap<Character, String> map = new HashMap<>();
		addEscapedChar(map, '\0', '0');
		addEscapedChar(map, '\\');
		addEscapedChar(map, '\t', 't');
		addEscapedChar(map, '\n', 'n');
		addEscapedChar(map, '\r', 'r');
		addEscapedChar(map, '\"');
		addEscapedChar(map, '\'');
		targetCharValueEscape = map;
	}

	protected static final HashSet<String> reservedWords = new HashSet<>(Arrays.asList(
		"_", "abstract", "alignof", "as", "become ",
		"box", "break", "const", "continue", "crate",
		"do", "else", "enum", "extern", "false",
		"final", "fn", "for", "if", "impl",
		"in", "let", "loop", "macro", "match",
		"mod", "move", "mut", "offsetof", "override",
		"priv", "proc", "pub", "pure", "ref",
		"return", "Self", "self", "sizeof", "static",
		"struct", "super", "trait", "true", "type",
		"typeof", "unsafe", "unsized", "use", "virtual",
		"where", "while", "yield",

		"rule", "parserRule"
	));

	public RustTarget(CodeGenerator gen) {
		super(gen);
	}

	@Override
	public String getRecognizerFileName(boolean header) {
		Grammar g = getCodeGenerator().g;
		assert g != null;
		String name;
		switch (g.getType()) {
			case ANTLRParser.PARSER:
				name = g.name.endsWith("Parser") ? g.name.substring(0, g.name.length() - 6) : g.name;
				name = name.toLowerCase() + "parser";
				break;
			case ANTLRParser.LEXER:
				name = g.name.endsWith("Lexer") ? g.name.substring(0, g.name.length() - 5) : g.name; // trim off "lexer"
				name = name.toLowerCase() + "lexer";
				break;
			case ANTLRParser.COMBINED:
				name = g.name.toLowerCase() + "parser";
				break;
			default:
				return "INVALID_FILE_NAME";
		}
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		return name + extST.render();
	}

	@Override
	public String getListenerFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name.toLowerCase() + "listener";
		return listenerName + extST.render();
	}

	@Override
	public String getVisitorFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name.toLowerCase() + "visitor";
		return listenerName + extST.render();
	}

	@Override
	public String getBaseListenerFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name + "_baseListener";
		return listenerName + extST.render();
	}

	@Override
	public String getBaseVisitorFileName(boolean header) {
		assert gen.g.name != null;
		ST extST = getTemplates().getInstanceOf("codeFileExtension");
		String listenerName = gen.g.name + "_baseVisitor";
		return listenerName + extST.render();
	}

	@Override
	protected Set<String> getReservedWords() {
		return reservedWords;
	}

	@Override
	protected STGroup loadTemplates() {
		STGroup result = super.loadTemplates();
		result.registerRenderer(String.class, new RustStringRenderer());

		return result;
	}

	protected static class RustStringRenderer extends StringRenderer {
		@Override
		public String toString(Object o, String formatString, Locale locale) {
			if ("java-escape".equals(formatString)) {
				// 5C is the hex code for the \ itself
				return ((String) o).replace("\\u", "\\u{005C}u");
			}

			return super.toString(o, formatString, locale);
		}
	}

	@Override
	public boolean wantsBaseListener() {
		return false;
	}

	@Override
	public boolean wantsBaseVisitor() {
		return false;
	}

	@Override
	public int getInlineTestSetWordSize() {
		return 32;
	}

	@Override
	protected String escapeWord(String word) {
		return "r#" + word;
	}

	@Override
	protected String escapeChar(int v) {
		if (v >= 0 && v <= 0x7f) {
			return String.format("\\x%02X", v);
		}

		// encode surrogates
		if (0xD800 <= v && v <= 0xDFFF) {
			v += 0x3000;
		}

		return String.format("\\u{%X}", v);
	}
}
