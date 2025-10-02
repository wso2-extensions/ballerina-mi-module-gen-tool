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
