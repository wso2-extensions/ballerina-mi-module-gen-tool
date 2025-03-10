# Ballerina Tool for WSO2 Micro Integrator Module Generator

## Overview

The `mi-module-gen` tools allows generation of modules for WSO2 Micro Integrator from Ballerina code.

**Version Compatibility**:

**Tool version**|**`wso2/mi` Connector version**|**Ballerina Version**|**Java version**|**WSO2 MI version**|
:-----:|:-----:|:-----:|:-----:|:-----:
0.2| 0.2| 2201.10.3| 17| 4.2.0, 4.3.0
0.3| 0.3| 2201.11.0| 21| 4.4.0


## Steps to Create Module for WSO2 MI from Ballerina

### Pull `mi-module-gen` Tool

First, you need to pull the `mi-module-gen` tool which is used to create the module.

```bash
$ bal tool pull mi-module-gen
```

### Write Ballerina Transformation

### Import the `wso2/mi` module

Create a new Ballerina project or use an existing one and write your transformation logic. Import the module `wso2/mi` in your Ballerina program.

```ballerina
import wso2/mi;
```

Write Ballerina Transformation. For example,

```ballerina
import wso2/mi;

@mi:Operation
public function GPA(xml rawMarks, xml credits) returns xml {
   // Your logic to calculate the GPA
}
```

Ballerina function that contains `@mi:Operation` annotation maps with a component in Ballerina module.

### Generate the module

Finally, use the `bal mi-module-gen` command to generate the Module for the WSO2 Micro Integrator.

```bash
$ bal mi-module-gen -i <path_to_ballerina_project>
```

Above command generates the connector zip in the same location.

## Local Build

1. Clone the repository [ballerina-mi-module-gen-tool](https://github.com/wso2-extensions/ballerina-mi-module-gen-tool.git)

2. Build the tool and publish locally:

   ```bash
   $ ./gradlew clean :tool-mi-module-gen:localPublish
   ```

### Run tests

   ```bash
   $ ./gradlew test
   ```

## Contribute to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All the contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

 People who are joining from along the way, could you please add the pickup locations to the sheet:
