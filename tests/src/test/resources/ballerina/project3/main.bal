// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import wso2/mi;
import ballerina/io;

@mi:Operation
public function test(xml xmlA, xml xmlB, xml xmlC, xml xmlD) returns xml {
    return xml `<may22>${xmlA}${xmlB}${xmlC}${xmlD}</may22>`;
}
@mi:Operation
public function testXmlReturn(xml x) returns xml {
    return x;
}

// Test function to handle absence of input/return values
@mi:Operation
public function testAbsenceOfValues() {
    // Test with optional parameters
   io:println("Testing with absent value:");
}


// Function to validate boolean operations and logical output
@mi:Operation
public function validateBooleanOperations(boolean inputA, boolean inputB) returns boolean {
    // Test AND operation
    boolean andResult = inputA && inputB;
    io:println("AND operation: " + inputA.toString() + " && " + inputB.toString() + " = " + andResult.toString());
    
    // Test OR operation
    boolean orResult = inputA || inputB;
    io:println("OR operation: " + inputA.toString() + " || " + inputB.toString() + " = " + orResult.toString());
    
    // Test NOT operation
    boolean notA = !inputA;
    boolean notB = !inputB;
    io:println("NOT operation: !" + inputA.toString() + " = " + notA.toString());
    io:println("NOT operation: !" + inputB.toString() + " = " + notB.toString());
    
    // Test XOR operation (exclusive OR)
    boolean xorResult = (inputA || inputB) && !(inputA && inputB);
    io:println("XOR operation: " + inputA.toString() + " XOR " + inputB.toString() + " = " + xorResult.toString());
    
    // Test NAND operation
    boolean nandResult = !(inputA && inputB);
    io:println("NAND operation: !(" + inputA.toString() + " && " + inputB.toString() + ") = " + nandResult.toString());
    
    // Test NOR operation
    boolean norResult = !(inputA || inputB);
    io:println("NOR operation: !(" + inputA.toString() + " || " + inputB.toString() + ") = " + norResult.toString());
    
    // Return combined logical result
    return andResult || orResult;
}

// Function to validate integer operations and arithmetic output
@mi:Operation
public function validateIntegerOperations(int inputA, int inputB) returns int {
    // Test addition
    int addResult = inputA + inputB;
    io:println("Addition: " + inputA.toString() + " + " + inputB.toString() + " = " + addResult.toString());
    
    // Test subtraction
    int subtractResult = inputA - inputB;
    io:println("Subtraction: " + inputA.toString() + " - " + inputB.toString() + " = " + subtractResult.toString());
    
    // Test multiplication
    int multiplyResult = inputA * inputB;
    io:println("Multiplication: " + inputA.toString() + " * " + inputB.toString() + " = " + multiplyResult.toString());
    
    // Test division (with zero check)
    if inputB != 0 {
        int divideResult = inputA / inputB;
        io:println("Division: " + inputA.toString() + " / " + inputB.toString() + " = " + divideResult.toString());
    } else {
        io:println("Division: Cannot divide by zero");
    }
    
    // Test modulo (with zero check)
    if inputB != 0 {
        int moduloResult = inputA % inputB;
        io:println("Modulo: " + inputA.toString() + " % " + inputB.toString() + " = " + moduloResult.toString());
    } else {
        io:println("Modulo: Cannot perform modulo with zero");
    }
    
    // Test absolute value
    int absA = inputA < 0 ? -inputA : inputA;
    int absB = inputB < 0 ? -inputB : inputB;
    io:println("Absolute value: |" + inputA.toString() + "| = " + absA.toString());
    io:println("Absolute value: |" + inputB.toString() + "| = " + absB.toString());
    
    // Test comparison operations
    boolean isGreater = inputA > inputB;
    boolean isLess = inputA < inputB;
    boolean isEqual = inputA == inputB;
    io:println("Comparison: " + inputA.toString() + " > " + inputB.toString() + " = " + isGreater.toString());
    io:println("Comparison: " + inputA.toString() + " < " + inputB.toString() + " = " + isLess.toString());
    io:println("Comparison: " + inputA.toString() + " == " + inputB.toString() + " = " + isEqual.toString());
    
    // Return combined result (sum of all operations)
    int combinedResult = addResult + subtractResult + multiplyResult;
    return combinedResult;
}

// Function to validate floating-point math and correct float output
@mi:Operation
public function validateFloatOperations(float inputA, float inputB) returns float {
    // Test addition
    float addResult = inputA + inputB;
    io:println("Float Addition: " + inputA.toString() + " + " + inputB.toString() + " = " + addResult.toString());
    
    // Test subtraction
    float subtractResult = inputA - inputB;
    io:println("Float Subtraction: " + inputA.toString() + " - " + inputB.toString() + " = " + subtractResult.toString());
    
    // Test multiplication
    float multiplyResult = inputA * inputB;
    io:println("Float Multiplication: " + inputA.toString() + " * " + inputB.toString() + " = " + multiplyResult.toString());
    
    // Test division (with zero check)
    if inputB != 0.0 {
        float divideResult = inputA / inputB;
        io:println("Float Division: " + inputA.toString() + " / " + inputB.toString() + " = " + divideResult.toString());
    } else {
        io:println("Float Division: Cannot divide by zero");
    }
    
    // Test modulo (with zero check)
    if inputB != 0.0 {
        float moduloResult = inputA % inputB;
        io:println("Float Modulo: " + inputA.toString() + " % " + inputB.toString() + " = " + moduloResult.toString());
    } else {
        io:println("Float Modulo: Cannot perform modulo with zero");
    }
    
    // Test absolute value
    float absA = inputA < 0.0 ? -inputA : inputA;
    float absB = inputB < 0.0 ? -inputB : inputB;
    io:println("Float Absolute value: |" + inputA.toString() + "| = " + absA.toString());
    io:println("Float Absolute value: |" + inputB.toString() + "| = " + absB.toString());
    
    // Test power operation (inputA squared)
    float squareA = inputA * inputA;
    float squareB = inputB * inputB;
    io:println("Square: " + inputA.toString() + "^2 = " + squareA.toString());
    io:println("Square: " + inputB.toString() + "^2 = " + squareB.toString());
    
    // Test comparison operations
    boolean isGreater = inputA > inputB;
    boolean isLess = inputA < inputB;
    boolean isEqual = inputA == inputB;
    io:println("Float Comparison: " + inputA.toString() + " > " + inputB.toString() + " = " + isGreater.toString());
    io:println("Float Comparison: " + inputA.toString() + " < " + inputB.toString() + " = " + isLess.toString());
    io:println("Float Comparison: " + inputA.toString() + " == " + inputB.toString() + " = " + isEqual.toString());
    
    // Test average calculation
    float averageResult = (inputA + inputB) / 2.0;
    io:println("Average: (" + inputA.toString() + " + " + inputB.toString() + ") / 2 = " + averageResult.toString());
    
    // Return combined result (average of addition and multiplication)
    float combinedResult = (addResult + multiplyResult) / 2.0;
    return combinedResult;
}

// Function to validate high-precision decimal handling and arithmetic
@mi:Operation
public function validateDecimalOperations(decimal inputA, decimal inputB) returns decimal {
    // Test addition with high precision
    decimal addResult = inputA + inputB;
    io:println("Decimal Addition: " + inputA.toString() + " + " + inputB.toString() + " = " + addResult.toString());
    
    // Test subtraction with high precision
    decimal subtractResult = inputA - inputB;
    io:println("Decimal Subtraction: " + inputA.toString() + " - " + inputB.toString() + " = " + subtractResult.toString());
    
    // Test multiplication with high precision
    decimal multiplyResult = inputA * inputB;
    io:println("Decimal Multiplication: " + inputA.toString() + " * " + inputB.toString() + " = " + multiplyResult.toString());
    
    // Test division with high precision (with zero check)
    if inputB != 0d {
        decimal divideResult = inputA / inputB;
        io:println("Decimal Division: " + inputA.toString() + " / " + inputB.toString() + " = " + divideResult.toString());
    } else {
        io:println("Decimal Division: Cannot divide by zero");
    }
    
    // Test modulo with high precision (with zero check)
    if inputB != 0d {
        decimal moduloResult = inputA % inputB;
        io:println("Decimal Modulo: " + inputA.toString() + " % " + inputB.toString() + " = " + moduloResult.toString());
    } else {
        io:println("Decimal Modulo: Cannot perform modulo with zero");
    }
    
    // Test absolute value with high precision
    decimal absA = inputA < 0d ? -inputA : inputA;
    decimal absB = inputB < 0d ? -inputB : inputB;
    io:println("Decimal Absolute value: |" + inputA.toString() + "| = " + absA.toString());
    io:println("Decimal Absolute value: |" + inputB.toString() + "| = " + absB.toString());
    
    // Test power operation (squared) with high precision
    decimal squareA = inputA * inputA;
    decimal squareB = inputB * inputB;
    io:println("Decimal Square: " + inputA.toString() + "^2 = " + squareA.toString());
    io:println("Decimal Square: " + inputB.toString() + "^2 = " + squareB.toString());
    
    // Test comparison operations
    boolean isGreater = inputA > inputB;
    boolean isLess = inputA < inputB;
    boolean isEqual = inputA == inputB;
    io:println("Decimal Comparison: " + inputA.toString() + " > " + inputB.toString() + " = " + isGreater.toString());
    io:println("Decimal Comparison: " + inputA.toString() + " < " + inputB.toString() + " = " + isLess.toString());
    io:println("Decimal Comparison: " + inputA.toString() + " == " + inputB.toString() + " = " + isEqual.toString());
    
    // Test average calculation with high precision
    decimal averageResult = (inputA + inputB) / 2d;
    io:println("Decimal Average: (" + inputA.toString() + " + " + inputB.toString() + ") / 2 = " + averageResult.toString());
    
    // Test precision preservation in complex calculation
    decimal complexResult = (inputA * inputB + inputA - inputB) / 2d;
    io:println("Complex Calculation: (" + inputA.toString() + " * " + inputB.toString() + " + " + inputA.toString() + " - " + inputB.toString() + ") / 2 = " + complexResult.toString());
    
    // Return combined result with high precision (weighted average)
    decimal combinedResult = (addResult * 0.3d + multiplyResult * 0.5d + averageResult * 0.2d);
    return combinedResult;
}

// Function to validate receiving and returning string values
@mi:Operation
public function validateStringOperations(string inputA, string inputB) returns string {
    // Test string concatenation
    string concatenated = inputA + inputB;
    io:println("String Concatenation: \"" + inputA + "\" + \"" + inputB + "\" = \"" + concatenated + "\"");
    
    // Test string concatenation with separator
    string joinedWithSpace = inputA + " " + inputB;
    io:println("String Join with space: \"" + inputA + "\" + \" \" + \"" + inputB + "\" = \"" + joinedWithSpace + "\"");
    
    // Test string length
    int lengthA = inputA.length();
    int lengthB = inputB.length();
    io:println("String Length: \"" + inputA + "\".length() = " + lengthA.toString());
    io:println("String Length: \"" + inputB + "\".length() = " + lengthB.toString());
    
    // Test substring operation
    if lengthA > 0 {
        string substringA = inputA.substring(0, lengthA > 3 ? 3 : lengthA);
        io:println("Substring: \"" + inputA + "\".substring(0, 3) = \"" + substringA + "\"");
    }
    
    // Test string comparison
    boolean isEqual = inputA == inputB;
    io:println("String Equality: \"" + inputA + "\" == \"" + inputB + "\" = " + isEqual.toString());
    
    // Test case conversion
    string upperA = inputA.toUpperAscii();
    string lowerB = inputB.toLowerAscii();
    io:println("To Upper: \"" + inputA + "\".toUpperAscii() = \"" + upperA + "\"");
    io:println("To Lower: \"" + inputB + "\".toLowerAscii() = \"" + lowerB + "\"");
    
    // Test string trimming
    string paddedString = "  " + inputA + "  ";
    string trimmedString = paddedString.trim();
    io:println("Trim: \"" + paddedString + "\".trim() = \"" + trimmedString + "\"");
    
    // Test string contains/includes
    boolean containsCheck = joinedWithSpace.includes(inputA);
    io:println("Includes: \"" + joinedWithSpace + "\".includes(\"" + inputA + "\") = " + containsCheck.toString());
    
    // Test indexOf
    int? indexPosition = joinedWithSpace.indexOf(inputB);
    if indexPosition is int {
        io:println("IndexOf: \"" + joinedWithSpace + "\".indexOf(\"" + inputB + "\") = " + indexPosition.toString());
    } else {
        io:println("IndexOf: \"" + inputB + "\" not found in \"" + joinedWithSpace + "\"");
    }
    
    // Test startsWith and endsWith
    boolean startsWithA = joinedWithSpace.startsWith(inputA);
    boolean endsWithB = joinedWithSpace.endsWith(inputB);
    io:println("StartsWith: \"" + joinedWithSpace + "\".startsWith(\"" + inputA + "\") = " + startsWithA.toString());
    io:println("EndsWith: \"" + joinedWithSpace + "\".endsWith(\"" + inputB + "\") = " + endsWithB.toString());
    
    // Return combined result
    string combinedResult = "Result: [" + upperA + " + " + lowerB + "] = " + concatenated;
    return combinedResult;
}

// Record types for validation
public type Person record {|
    string name;
    int age;
    string city;
|};

public type Employee record {|
    string employeeId;
    string department;
    decimal salary;
    boolean isActive;
|};

public type CombinedData record {|
    Person person;
    Employee employee;
    string processedBy;
    int timestamp;
|};

// Function to validate record type input and output

public function validateRecordOperations(Person personInput, Employee employeeInput) returns CombinedData {
    io:println("=== Record Type Validation ===");
    
    // Access and print person record fields
    string personName = personInput.name;
    int personAge = personInput.age;
    string personCity = personInput.city;
    io:println("Person: name=" + personName + ", age=" + personAge.toString() + ", city=" + personCity);
    
    // Access and print employee record fields
    string empId = employeeInput.employeeId;
    string empDept = employeeInput.department;
    decimal empSalary = employeeInput.salary;
    boolean empActive = employeeInput.isActive;
    io:println("Employee: id=" + empId + ", dept=" + empDept + ", salary=" + empSalary.toString() + ", active=" + empActive.toString());
    
    // Modify and create new record
    Person modifiedPerson = {
        name: personName.toUpperAscii(),
        age: personAge + 1,
        city: personCity
    };
    io:println("Modified Person: name=" + modifiedPerson.name + ", age=" + modifiedPerson.age.toString());
    
    // Create combined result record
    CombinedData resultRecord = {
        person: modifiedPerson,
        employee: employeeInput,
        processedBy: "validateRecordOperations",
        timestamp: 1234567890
    };
    
    io:println("Combined record created successfully");
    return resultRecord;
}

// Function to validate JSON type input and output
@mi:Operation
public function validateJsonOperations(json inputJsonA, json inputJsonB) returns json {
    io:println("=== JSON Type Validation ===");
    
    // Convert JSON to string for display
    string jsonAString = inputJsonA.toJsonString();
    string jsonBString = inputJsonB.toJsonString();
    io:println("Input JSON A: " + jsonAString);
    io:println("Input JSON B: " + jsonBString);
    
    // Create a new JSON object combining inputs
    json combinedJson = {
        "inputA": inputJsonA,
        "inputB": inputJsonB,
        "processed": true,
        "timestamp": 1234567890,
        "metadata": {
            "operation": "validateJsonOperations",
            "version": "1.0"
        }
    };
    
    // Convert combined JSON to string
    string combinedJsonString = combinedJson.toJsonString();
    io:println("Combined JSON: " + combinedJsonString);
    
    // Test JSON array creation
    json jsonArray = [inputJsonA, inputJsonB, "additional", 123, true];
    string arrayString = jsonArray.toJsonString();
    io:println("JSON Array: " + arrayString);
    
    // Return combined JSON
    return combinedJson;
}

// Function to validate map<anydata> type input and output

public function validateMapOperations(map<anydata> inputMapA, map<anydata> inputMapB) returns map<anydata> {
    io:println("=== Map<anydata> Type Validation ===");
    
    // Get and display map keys
    string[] keysA = inputMapA.keys();
    string[] keysB = inputMapB.keys();
    io:println("Map A keys: " + keysA.toString());
    io:println("Map B keys: " + keysB.toString());
    
    // Get map length
    int lengthA = inputMapA.length();
    int lengthB = inputMapB.length();
    io:println("Map A length: " + lengthA.toString());
    io:println("Map B length: " + lengthB.toString());
    
    // Access map values
    foreach string keyItem in keysA {
        anydata valueItem = inputMapA.get(keyItem);
        io:println("Map A[" + keyItem + "] = " + valueItem.toString());
    }
    
    // Check if map has specific key
    if keysA.length() > 0 {
        string firstKey = keysA[0];
        boolean hasKey = inputMapA.hasKey(firstKey);
        io:println("Map A has key '" + firstKey + "': " + hasKey.toString());
    }
    
    // Create a new combined map
    map<anydata> combinedMap = {
        "mapAData": inputMapA,
        "mapBData": inputMapB,
        "totalKeys": lengthA + lengthB,
        "processed": true,
        "operation": "validateMapOperations"
    };
    
    // Add individual entries from both maps
    foreach string keyItem in keysA {
        anydata valueItem = inputMapA.get(keyItem);
        combinedMap["a_" + keyItem] = valueItem;
    }
    
    foreach string keyItem in keysB {
        anydata valueItem = inputMapB.get(keyItem);
        combinedMap["b_" + keyItem] = valueItem;
    }
    
    // Display combined map info
    int combinedLength = combinedMap.length();
    io:println("Combined map length: " + combinedLength.toString());
    io:println("Combined map keys: " + combinedMap.keys().toString());
    
    return combinedMap;
}
