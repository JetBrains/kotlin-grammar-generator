package org.jetbrains.jet.grammar.tokens;

import com.google.common.collect.Multimap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by egor.malyshev on 14.08.2014.
 */
@XmlRootElement(name = "tokens")
@XmlAccessorType(XmlAccessType.FIELD)
public class TokenList {
    @XmlElement(name = "token")
    private List<Token> tokens;

    public TokenList() {
    }

    public TokenList(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void updateUsages(Set<String> usedSymbols, Multimap<String, String> usages) {

        for (Token token : tokens) {
            if (token instanceof Declaration) {

                Declaration declaration = (Declaration) token;

                Collection<String> myUsages = usages.get(declaration.getName());

                if (!myUsages.isEmpty()) {

                    for (String usage : myUsages) {
                        token.addUsage(usage);

                    }
                }
            }
        }
    }
}
