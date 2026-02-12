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

package io.ballerina.mi.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.ballerina.mi.model.ModelElement;
import io.ballerina.mi.util.Utils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching Handlebars template engines.
 * <p>
 * This class centralizes template engine creation, helper registration, and
 * compiled template caching. It implements the Factory pattern to decouple
 * template creation from the artifact generators that use them.
 * </p>
 * <p>
 * Thread-safe: Uses synchronized initialization and concurrent caches.
 * </p>
 */
public class TemplateEngine {

    private final Handlebars handlebars;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Creates a TemplateEngine with the given Handlebars instance.
     *
     * @param handlebars The configured Handlebars engine
     */
    public TemplateEngine(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    /**
     * Creates a TemplateEngine with a plain Handlebars instance (no custom helpers).
     */
    public TemplateEngine() {
        this(new Handlebars());
    }

    /**
     * Returns the underlying Handlebars instance for helper registration.
     *
     * @return The Handlebars instance
     */
    public Handlebars getHandlebars() {
        return handlebars;
    }

    /**
     * Gets or compiles a template from the given path, using the cache.
     *
     * @param templateFilePath Path to the template file (relative to classpath)
     * @return The compiled Template
     * @throws IOException If the template file cannot be read or compiled
     */
    public Template getTemplate(String templateFilePath) throws IOException {
        Template cached = templateCache.get(templateFilePath);
        if (cached != null) return cached;
        String content = Utils.readFile(templateFilePath);
        Template template = handlebars.compileInline(content);
        templateCache.put(templateFilePath, template);
        return template;
    }

    /**
     * Renders a template with the given model element and writes the output to a file.
     *
     * @param templateFilePath Path to the template file
     * @param outputFilePath   Path to the output file (without extension)
     * @param element          The model element to apply to the template
     * @param extension        File extension (e.g., "xml", "json")
     * @throws IOException If an I/O error occurs
     */
    public void renderToFile(String templateFilePath, String outputFilePath, ModelElement element,
                             String extension) throws IOException {
        Template template = getTemplate(templateFilePath);
        String output = template.apply(element);
        String outputFileName = String.format("%s.%s", outputFilePath, extension);
        Utils.writeFile(outputFileName, output);
    }

    /**
     * Clears all cached templates and releases memory.
     */
    public void clearCache() {
        templateCache.clear();
    }
}
