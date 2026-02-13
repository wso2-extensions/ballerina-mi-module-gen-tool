/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests for TemplateEngine class.
 */
public class TemplateEngineTest {

    @Test
    public void testDefaultConstructor() {
        TemplateEngine engine = new TemplateEngine();
        Assert.assertNotNull(engine);
        Assert.assertNotNull(engine.getHandlebars());
    }

    @Test
    public void testConstructorWithHandlebars() {
        Handlebars handlebars = new Handlebars();
        TemplateEngine engine = new TemplateEngine(handlebars);
        Assert.assertNotNull(engine);
        Assert.assertSame(engine.getHandlebars(), handlebars);
    }

    @Test
    public void testGetHandlebars() {
        Handlebars handlebars = new Handlebars();
        TemplateEngine engine = new TemplateEngine(handlebars);
        Assert.assertSame(engine.getHandlebars(), handlebars);
    }

    @Test
    public void testClearCache() {
        TemplateEngine engine = new TemplateEngine();
        // Should not throw
        engine.clearCache();
    }

    @Test
    public void testHandlebarsCompileInline() throws IOException {
        TemplateEngine engine = new TemplateEngine();
        Handlebars handlebars = engine.getHandlebars();

        // Test that the handlebars instance can compile inline templates
        Template template = handlebars.compileInline("Hello {{name}}!");
        Assert.assertNotNull(template);

        // Apply template
        String result = template.apply(new Object() {
            public String getName() { return "World"; }
        });
        Assert.assertEquals(result, "Hello World!");
    }

    @Test
    public void testHandlebarsWithConditional() throws IOException {
        TemplateEngine engine = new TemplateEngine();
        Handlebars handlebars = engine.getHandlebars();

        Template template = handlebars.compileInline("{{#if enabled}}Yes{{else}}No{{/if}}");

        String enabledResult = template.apply(new Object() {
            public boolean isEnabled() { return true; }
        });
        Assert.assertEquals(enabledResult, "Yes");

        String disabledResult = template.apply(new Object() {
            public boolean isEnabled() { return false; }
        });
        Assert.assertEquals(disabledResult, "No");
    }

    @Test
    public void testHandlebarsWithLoop() throws IOException {
        TemplateEngine engine = new TemplateEngine();
        Handlebars handlebars = engine.getHandlebars();

        Template template = handlebars.compileInline("{{#each items}}{{this}},{{/each}}");

        String result = template.apply(new Object() {
            public String[] getItems() { return new String[]{"a", "b", "c"}; }
        });
        Assert.assertEquals(result, "a,b,c,");
    }
}
