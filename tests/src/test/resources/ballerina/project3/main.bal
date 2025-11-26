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
