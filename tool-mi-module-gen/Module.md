# Module Overview

The `mi-module-gen` Ballerina tool allows generation of modules for WSO2 Micro Integrator from Ballerina code. This tool enables developers to write transformation logic in Ballerina and generate deployable modules for WSO2 MI.

## Features

- Generate WSO2 Micro Integrator modules from Ballerina code
- Generate MI connectors from Ballerina connectors
- Support for `@mi:Operation` annotation to define MI components
- Seamless integration with Ballerina build tools

## Usage

### Pull the Tool

```bash
$ bal tool pull mi-module-gen
```

### Write Ballerina Transformation

Import the `wso2/mi` module and write your transformation logic:

```ballerina
import wso2/mi;

@mi:Operation
public function GPA(xml rawMarks, xml credits) returns xml {
   // Your logic to calculate the GPA
}
```

### Generate the Module

Use the `bal mi-module-gen` command to generate the module:

```bash
$ bal mi-module-gen -i <path_to_ballerina_project>
```

### Generate MI Connector from Ballerina Connector

You can also generate an MI connector from an existing Ballerina connector.

1. **Pull the Ballerina connector** from Ballerina Central:

```bash
$ bal pull ballerinax/<connector_name>
```

2. **Generate the MI connector** using the pulled connector path:

```bash
$ bal mi-module-gen -i {user.home}/.ballerina/repositories/central.ballerina.io/bala/ballerinax/<connector_name>/<version>/any -t <output_directory>
```

For example, to generate an MI connector from the `ballerinax/github` connector:

```bash
$ bal pull ballerinax/github
$ bal mi-module-gen -i {user.home}/.ballerina/repositories/central.ballerina.io/bala/ballerinax/github/6.0.0/any -t generatedMiConnector
```

## Version Compatibility

|   Tool Version    | Ballerina Version | Java Version | WSO2 MI Version |
|:-----------------:|:-----------------:|:------------:|:---------------:|
|       0.4.0       |     2201.12.x     |      21      |    >= 4.4.0     |
|     \>= 0.4.1     |     2201.13.x     |      21      |    >= 4.4.0     |

## Related Links

- [WSO2 Micro Integrator Documentation](https://mi.docs.wso2.com/)
- [Ballerina Documentation](https://ballerina.io/learn/)
