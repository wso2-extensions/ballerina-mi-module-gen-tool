# Package Overview

[![Build](https://github.com/wso2-extensions/ballerina-mi-module-gen-tool/actions/workflows/build.yml/badge.svg)](https://github.com/wso2-extensions/ballerina-mi-module-gen-tool/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

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

## Building from Source

```bash
$ git clone https://github.com/wso2-extensions/ballerina-mi-module-gen-tool.git
$ cd ballerina-mi-module-gen-tool
$ ./gradlew clean :tool-mi-module-gen:localPublish
```

## Contributing

Contributions are welcome! Please read the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md) before submitting a pull request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/wso2-extensions/ballerina-mi-module-gen-tool/blob/main/LICENSE) file for details.

## Useful Links

- [GitHub Repository](https://github.com/wso2-extensions/ballerina-mi-module-gen-tool)
- [WSO2 Micro Integrator](https://wso2.com/micro-integrator/)
- [Ballerina](https://ballerina.io/)
