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


