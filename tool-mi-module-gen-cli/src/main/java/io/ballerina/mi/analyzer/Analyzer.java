package io.ballerina.mi.analyzer;

import io.ballerina.projects.Package;

public interface Analyzer {
    void analyze(Package compiledPackage);
}
