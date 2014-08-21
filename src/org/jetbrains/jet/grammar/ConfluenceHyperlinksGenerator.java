/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.grammar;

import com.google.common.base.Supplier;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.jet.grammar.tokens.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfluenceHyperlinksGenerator {

    private static final String GRAMMAR_EXTENSION = "grm";
    private static final List<String> FILE_NAMES_IN_ORDER = Arrays.asList(
            "notation",
            "toplevel",
            "class",
            "class_members",
            "enum",
            "types",
            "control",
            "expressions",
            "when",
            "modifiers",
            "attributes",
            "lexical"
    );

    public static void main(String[] args) throws IOException, TransformerException {

        System.out.println("Checking cmd line arguments.");
        if (args.length < 2)
            throw new IllegalArgumentException("Usage: grammar-parser <path to grm files> <output file name>");

        File grammarDir = new File(args[0]);
        File outputFile = new File(args[1]);

        if (!outputFile.exists()) {
            if (!outputFile.createNewFile()) throw new IOException("Cannot create output file.");
        }

        Set<File> used = new HashSet<File>();
        List<Token> tokens = getJoinedTokensFromAllFiles(grammarDir, used);
        assertAllFilesAreUsed(grammarDir, used);
        System.out.println("Preparing resources.");
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        StreamSource xml = new StreamSource(new StringReader(generate(tokens)));
        StreamSource xsl = new StreamSource(loader.getResourceAsStream("convert.xsl"));

        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
        StreamResult result = new StreamResult(outputFile);

        System.out.println("Processing.");
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(xsl);

        transformer.transform(xml, result);
        result.getOutputStream().close();
        System.out.println("Done.");
    }

    private static List<Token> getJoinedTokensFromAllFiles(File grammarDir, Set<File> used) throws IOException {
        List<Token> allTokens = Lists.newArrayList();
        for (String fileName : FILE_NAMES_IN_ORDER) {
            File file = new File(grammarDir, fileName + "." + GRAMMAR_EXTENSION);
            used.add(file);
            String text = FileUtils.readFileToString(file, "UTF-8");
            StringBuilder textWithMarkedDeclarations = markDeclarations(text);
            List<Token> tokens = tokenize(createLexer(file.getPath(), textWithMarkedDeclarations));
            allTokens.addAll(tokens);
        }
        return allTokens;
    }

    private static _GrammarLexer createLexer(String fileName, StringBuilder output) {
        _GrammarLexer grammarLexer = new _GrammarLexer((Reader) null);
        grammarLexer.reset(output, 0, output.length(), 0);
        grammarLexer.setFileName(fileName);
        return grammarLexer;
    }

    private static void assertAllFilesAreUsed(File grammarDir, Set<File> used) {
        for (File file : grammarDir.listFiles()) {
            if (file.getName().endsWith(GRAMMAR_EXTENSION)) {
                if (!used.contains(file)) {
                    throw new IllegalStateException("Unused grammar file : " + file.getAbsolutePath());
                }
            }
        }
    }

    private static StringBuilder markDeclarations(CharSequence allRules) {
        StringBuilder output = new StringBuilder();

        Pattern symbolReference = Pattern.compile("^\\w+$", Pattern.MULTILINE);
        Matcher matcher = symbolReference.matcher(allRules);
        int copiedUntil = 0;
        while (matcher.find()) {
            int start = matcher.start();
            output.append(allRules.subSequence(copiedUntil, start));

            String group = matcher.group();
            output.append("&").append(group);
            copiedUntil = matcher.end();
        }
        output.append(allRules.subSequence(copiedUntil, allRules.length()));
        return output;
    }

    private static String generate(List<Token> tokens) throws IOException {
        StringWriter result = new StringWriter();

        Set<String> declaredSymbols = new HashSet<String>();
        Set<String> usedSymbols = new HashSet<String>();
        Multimap<String, String>
                usages = Multimaps.newSetMultimap(Maps.<String, Collection<String>>newHashMap(), new Supplier<Set<String>>() {
            @Override
            public Set<String> get() {
                return Sets.newHashSet();
            }
        });

        Declaration lastDeclaration = null;
        for (Token advance : tokens) {
            if (advance instanceof Declaration) {
                Declaration declaration = (Declaration) advance;
                lastDeclaration = declaration;
                declaredSymbols.add(declaration.getName());
            } else if (advance instanceof Identifier) {
                Identifier identifier = (Identifier) advance;
                assert lastDeclaration != null;
                usages.put(identifier.getName(), lastDeclaration.getName());
                usedSymbols.add(identifier.getName());
            }
        }


        try {

            JAXBContext context =
                    JAXBContext.newInstance(Annotation.class, Comment.class, Declaration.class, DocComment.class,
                            Identifier.class, Other.class, StringToken.class, SymbolToken.class, Token.class,
                            WhiteSpace.class, TokenList.class);
            Marshaller m = context.createMarshaller();
            TokenList list = new TokenList(tokens);
            list.updateUsages(usedSymbols, usages);
            m.marshal(list, result);

        } catch (PropertyException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        result.flush();
        result.close();
        return result.toString();
    }

    private static String tokenWithPosition(Token token) {
        return token + " at " + token.getFileName() + ":" + token.getLine();
    }

    private static List<Token> tokenize(_GrammarLexer grammarLexer) throws IOException {
        List<Token> tokens = new ArrayList<Token>();
        while (true) {
            Token advance = grammarLexer.advance();
            if (advance == null) {
                break;
            }
            tokens.add(advance);
        }
        return tokens;
    }

    private static void copyToClipboard(StringBuilder result) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(result.toString()), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {

            }
        });
    }
}
