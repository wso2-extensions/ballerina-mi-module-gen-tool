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

package io.ballerina.mi.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.ballerina.mi.model.attribute.Element;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.mi.util.Utils.readFile;


public class JsonTemplateBuilder {
    private final StringBuilder result;

    private static final Handlebars handlebar = createHandlebars();
    private static final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    private static Handlebars createHandlebars() {
        Handlebars hb = new Handlebars();
        hb.registerHelper("eq", (context, options) -> context != null &&
                context.equals(options.param(0)));
        return hb;
    }

    public JsonTemplateBuilder() {
        this.result = new StringBuilder();
    }

    public JsonTemplateBuilder addFromTemplate(String templatePath, Element element) throws IOException {
        Template template = templateCache.computeIfAbsent(templatePath, k -> {
            try {
                String content = readFile(k);
                return handlebar.compileInline(content);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        String output = template.apply(element);
        result.append(output);
        return this;
    }

    public JsonTemplateBuilder addSeparator(String separator) {
        // Always treat plain separators the same as conditional ones with condition=true
        // This keeps commas on the same line as the closing brace when templates end with a newline.
        return addConditionalSeparator(true, separator);
    }

    public JsonTemplateBuilder addConditionalSeparator(boolean condition, String separator) {
        if (condition) {
            // If the last character is a newline, remove it, add the separator
            // This ensures commas appear on the same line as the closing brace: },{
            // The next template will start with {, so we get },{ on the same line
            int length = result.length();
            if (length > 0 && result.charAt(length - 1) == '\n') {
                result.deleteCharAt(length - 1);
                result.append(separator);
                // Don't add newline back - let the next template provide it
            } else {
                result.append(separator);
            }
        }
        return this;
    }

    public String build() {
        return result.toString();
    }

    public static void clearCache() {
        templateCache.clear();
    }
}
