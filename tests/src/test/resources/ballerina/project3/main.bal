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
