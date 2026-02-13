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
import io.ballerina.mi.model.FunctionType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for HandlebarsHelperRegistry class.
 */
public class HandlebarsHelperRegistryTest {

    private Handlebars handlebars;

    @BeforeMethod
    public void setUp() {
        handlebars = new Handlebars();
        HandlebarsHelperRegistry.registerAll(handlebars);
    }

    // ─── Comparison Helpers ───────────────────────────────────────────────────

    @Test
    public void testEqHelper_EqualValues() throws IOException {
        Template template = handlebars.compileInline("{{#eq value1 value2}}equal{{else}}not equal{{/eq}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value1", "test");
        context.put("value2", "test");

        String result = template.apply(context);
        Assert.assertEquals(result, "equal");
    }

    @Test
    public void testEqHelper_NotEqualValues() throws IOException {
        Template template = handlebars.compileInline("{{#eq value1 value2}}equal{{else}}not equal{{/eq}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value1", "test1");
        context.put("value2", "test2");

        String result = template.apply(context);
        Assert.assertEquals(result, "not equal");
    }

    @Test
    public void testEqHelper_BothNull() throws IOException {
        Template template = handlebars.compileInline("{{#eq value1 value2}}equal{{else}}not equal{{/eq}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value1", null);
        context.put("value2", null);

        String result = template.apply(context);
        Assert.assertEquals(result, "equal");
    }

    @Test
    public void testEqHelper_OneNull() throws IOException {
        Template template = handlebars.compileInline("{{#eq value1 value2}}equal{{else}}not equal{{/eq}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value1", null);
        context.put("value2", "test");

        String result = template.apply(context);
        Assert.assertEquals(result, "not equal");
    }

    @Test
    public void testNotHelper_True() throws IOException {
        Template template = handlebars.compileInline("{{not value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", true);

        String result = template.apply(context);
        Assert.assertEquals(result, "false");
    }

    @Test
    public void testNotHelper_False() throws IOException {
        Template template = handlebars.compileInline("{{not value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", false);

        String result = template.apply(context);
        Assert.assertEquals(result, "true");
    }

    @Test
    public void testNotHelper_NonBoolean() throws IOException {
        Template template = handlebars.compileInline("{{not value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "string");

        String result = template.apply(context);
        Assert.assertEquals(result, "true");
    }

    @Test
    public void testCheckFuncTypeHelper() throws IOException {
        Template template = handlebars.compileInline("{{checkFuncType funcType \"RESOURCE\"}}");

        Map<String, Object> context = new HashMap<>();
        context.put("funcType", FunctionType.RESOURCE);

        String result = template.apply(context);
        Assert.assertEquals(result, "true");
    }

    @Test
    public void testCheckFuncTypeHelper_NotMatching() throws IOException {
        Template template = handlebars.compileInline("{{checkFuncType funcType \"RESOURCE\"}}");

        Map<String, Object> context = new HashMap<>();
        context.put("funcType", FunctionType.REMOTE);

        String result = template.apply(context);
        Assert.assertEquals(result, "false");
    }

    // ─── String Helpers ───────────────────────────────────────────────────────

    @Test
    public void testEscapeCharsHelper() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "hello\nworld");

        String result = template.apply(context);
        Assert.assertEquals(result, "hello\\nworld");
    }

    @Test
    public void testEscapeCharsHelper_Quotes() throws IOException {
        // Use triple braces to avoid HTML escaping by Handlebars
        Template template = handlebars.compileInline("{{{escapeChars value}}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "say \"hello\"");

        String result = template.apply(context);
        Assert.assertEquals(result, "say \\\"hello\\\"");
    }

    @Test
    public void testEscapeCharsHelper_QuotedValue() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "\"wrapped\"");

        String result = template.apply(context);
        Assert.assertEquals(result, "wrapped");
    }

    @Test
    public void testEscapeCharsHelper_EmptyParentheses() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "()");

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testEscapeCharsHelper_Null() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", null);

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testEscapeCharsHelper_Backslash() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "path\\to\\file");

        String result = template.apply(context);
        Assert.assertEquals(result, "path\\\\to\\\\file");
    }

    @Test
    public void testEscapeCharsHelper_Tab() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "col1\tcol2");

        String result = template.apply(context);
        Assert.assertEquals(result, "col1\\tcol2");
    }

    @Test
    public void testSanitizeParamNameHelper() throws IOException {
        Template template = handlebars.compileInline("{{sanitizeParamName value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "auth.token.value");

        String result = template.apply(context);
        Assert.assertEquals(result, "auth_token_value");
    }

    @Test
    public void testSanitizeParamNameHelper_WithQuote() throws IOException {
        Template template = handlebars.compileInline("{{sanitizeParamName value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "'paramName");

        String result = template.apply(context);
        Assert.assertEquals(result, "paramName");
    }

    @Test
    public void testSanitizeParamNameHelper_Null() throws IOException {
        Template template = handlebars.compileInline("{{sanitizeParamName value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", null);

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testUppercaseHelper() throws IOException {
        Template template = handlebars.compileInline("{{uppercase value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "hello");

        String result = template.apply(context);
        Assert.assertEquals(result, "HELLO");
    }

    @Test
    public void testUppercaseHelper_Null() throws IOException {
        Template template = handlebars.compileInline("{{uppercase value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", null);

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testCapitalizeHelper() throws IOException {
        Template template = handlebars.compileInline("{{capitalize value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "hello");

        String result = template.apply(context);
        Assert.assertEquals(result, "Hello");
    }

    @Test
    public void testCapitalizeHelper_NonString() throws IOException {
        Template template = handlebars.compileInline("{{capitalize value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", 123);

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testSanitizeModuleNameHelper() throws IOException {
        Template template = handlebars.compileInline("{{sanitizeModuleName value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "ballerina.http.client");

        String result = template.apply(context);
        Assert.assertEquals(result, "ballerina_http_client");
    }

    @Test
    public void testSanitizeModuleNameHelper_Null() throws IOException {
        Template template = handlebars.compileInline("{{sanitizeModuleName value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", null);

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    // ─── Miscellaneous Helpers ────────────────────────────────────────────────

    @Test
    public void testUnwrapOptionalHelper_Present() throws IOException {
        Template template = handlebars.compileInline("{{unwrapOptional value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", Optional.of("test"));

        String result = template.apply(context);
        Assert.assertEquals(result, "test");
    }

    @Test
    public void testUnwrapOptionalHelper_Empty() throws IOException {
        Template template = handlebars.compileInline("{{unwrapOptional value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", Optional.empty());

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testUnwrapOptionalHelper_NonOptional() throws IOException {
        Template template = handlebars.compileInline("{{unwrapOptional value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "not an optional");

        String result = template.apply(context);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testEscapeCharsHelper_CarriageReturn() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "line1\rline2");

        String result = template.apply(context);
        Assert.assertEquals(result, "line1\\rline2");
    }

    @Test
    public void testEscapeCharsHelper_FormFeed() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "page1\fpage2");

        String result = template.apply(context);
        Assert.assertEquals(result, "page1\\fpage2");
    }

    @Test
    public void testEscapeCharsHelper_Backspace() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "text\bmore");

        String result = template.apply(context);
        Assert.assertEquals(result, "text\\bmore");
    }

    @Test
    public void testEscapeCharsHelper_NullChar() throws IOException {
        Template template = handlebars.compileInline("{{escapeChars value}}");

        Map<String, Object> context = new HashMap<>();
        context.put("value", "text\u0000more");

        String result = template.apply(context);
        Assert.assertEquals(result, "text\\u0000more");
    }
}
