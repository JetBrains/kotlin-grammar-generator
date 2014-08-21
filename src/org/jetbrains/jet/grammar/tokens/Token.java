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

import org.apache.commons.io.FilenameUtils;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "token")
@XmlAccessorType(XmlAccessType.FIELD)
public class Token {
    @XmlAttribute(name = "file-name")
    protected String fileName;
    @XmlElement(name = "text")
    protected String text;
    @XmlAttribute(name = "line")
    protected int line;
    @XmlElementWrapper(name = "usages")
    @XmlElement(name="declaration")
    protected List<String> usages;

    public void addUsage(String usage) {
        if (usages == null) usages = new ArrayList<String>();
        usages.add(usage);
    }

    public Token(String text, String fileName, int line) {
        this.text = text;
        this.fileName = FilenameUtils.getName(fileName);
        this.line = line;
    }

    public CharSequence getText() {
        return text;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public Token() {
    }
    /*
    @Override
    public String toString() {
        return String.format("<%s>%s</%s>", this.getClass().getName(), text, this.getClass().getName());
//        return getText().toString();
    }
*/
}