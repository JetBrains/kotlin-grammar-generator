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

package org.jetbrains.jet.grammar.tokens;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="declaration")
@XmlAccessorType(XmlAccessType.FIELD)
public class Declaration extends Token {
    @XmlAttribute(name="name")
    private String name;

    public Declaration(String text, String fileName, int line) {
        super(text, fileName, line);
        name = text.substring(1);
    }

    public Declaration() {
    }

    @Override
    public String toString() {
        return "*" + name + "*";
    }

    public String getName() {
        return name;
    }
}
