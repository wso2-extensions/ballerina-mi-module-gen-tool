/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
package io.ballerina.stdlib.mi.plugin;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.ballerina.stdlib.mi.plugin.connectorModel.attributeModel.Element;

import java.io.IOException;

import static io.ballerina.stdlib.mi.plugin.Utils.readFile;


public class JsonTemplateBuilder {
    private final StringBuilder result;
    private final Handlebars handlebar;

    public JsonTemplateBuilder() {
        this.result = new StringBuilder();
        this.handlebar = new Handlebars();
        handlebar.registerHelper("eq", (context, options) -> context != null &&
                context.equals(options.param(0)));
    }

    public JsonTemplateBuilder addFromTemplate(String templatePath, Element element) throws IOException {
        String content = readFile(templatePath);
        Template template = handlebar.compileInline(content);
        String output = template.apply(element);
        result.append(output);
        return this;
    }

    public JsonTemplateBuilder addSeparator(String separator) {
        result.append(separator);
        return this;
    }

    public JsonTemplateBuilder addConditionalSeparator(boolean condition, String separator) {
        if (condition) result.append(separator);
        return this;
    }

    public String build() {
        return result.toString();
    }
}
