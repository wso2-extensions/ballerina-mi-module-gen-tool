package io.ballerina.stdlib.mi.plugin;

import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.Location;

import java.util.List;

public class MiModuleGenDiagnostic extends Diagnostic {
    @Override
    public Location location() {
        return null;
    }

    @Override
    public DiagnosticInfo diagnosticInfo() {
        return null;
    }

    @Override
    public String message() {
        return "";
    }

    @Override
    public List<DiagnosticProperty<?>> properties() {
        return List.of();
    }
}
