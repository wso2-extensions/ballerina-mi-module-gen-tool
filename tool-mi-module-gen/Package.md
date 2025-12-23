# Package Overview

The `mi-module-gen` package provides a Ballerina tool for generating WSO2 Micro Integrator modules from Ballerina code.

## Overview

This tool enables developers to:
- Write transformation and integration logic using Ballerina
- Generate deployable modules for WSO2 Micro Integrator
- Generate MI connectors from Ballerina connectors
- Leverage Ballerina's type safety and expressiveness for MI development

## Quick Start

### Prerequisites

- [Ballerina](https://ballerina.io/downloads/) 2201.13.1 or later
- Java 21 or later
- WSO2 Micro Integrator 4.4.0 or later

### Installation

Pull the tool using the Ballerina CLI:

```bash
$ bal tool pull mi-module-gen
```

### Usage

1. **Create a Ballerina project** with your transformation logic:

```ballerina
import wso2/mi;

@mi:Operation
public function transform(xml input) returns xml {
    // Your transformation logic here
}
```

2. **Generate the MI module**:

```bash
$ bal mi-module-gen -i <path_to_ballerina_project>
```

3. **Deploy** the generated module to WSO2 Micro Integrator.

### Generate MI Connector from Ballerina Connector

You can generate an MI connector from an existing Ballerina connector:

1. **Pull the Ballerina connector** from Ballerina Central:

```bash
$ bal pull ballerinax/<connector_name>
```

2. **Generate the MI connector**:

```bash
$ bal mi-module-gen -i {user.home}/.ballerina/repositories/central.ballerina.io/bala/ballerinax/<connector_name>/<version>/any -t <output_directory>
```

For example, to generate an MI connector from the `ballerinax/github` connector:

```bash
$ bal pull ballerinax/github
$ bal mi-module-gen -i {user.home}/.ballerina/repositories/central.ballerina.io/bala/ballerinax/github/6.0.0/any -t generatedMiConnector
```

## Command Options

| Option | Description |
|--------|-------------|
| `-i, --input` | Path to the Ballerina project or pulled connector |
| `-t, --target` | Output directory for the generated MI connector |
