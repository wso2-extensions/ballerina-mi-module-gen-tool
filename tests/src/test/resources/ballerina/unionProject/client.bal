// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

public isolated client class UnionClient {

    private string serviceUrl;

    public isolated function init(string serviceUrl = "https://test.sh") returns error? {
        self.serviceUrl = serviceUrl;
    }

    // Test A: Process string|int union - return type description
    remote isolated function processStringOrInt(string|int value) returns string {
        if value is string {
            return "string:" + value;
        } else {
            return "int:" + value.toString();
        }
    }

    // Test B: Process string|int|float union
    remote isolated function processStringOrIntOrFloat(string|int|float value) returns string {
        if value is string {
            return "string:" + value;
        } else if value is int {
            return "int:" + value.toString();
        } else {
            return "float:" + value.toString();
        }
    }

    // Test C: Process nullable string (string?)
    remote isolated function processNullableString(string? value) returns string {
        if value is () {
            return "null";
        }
        return "value:" + value;
    }

    // Test D: Return union type string|int
    remote isolated function returnStringOrInt(boolean returnString) returns string|int {
        if returnString {
            return "hello";
        }
        return 42;
    }

    // Test E: Process int|boolean union
    remote isolated function processIntOrBoolean(int|boolean value) returns string {
        if value is int {
            return "int:" + value.toString();
        } else {
            return "boolean:" + value.toString();
        }
    }

    // Test F: Process number union (int|float|decimal)
    remote isolated function processNumberType(int|float|decimal value) returns string {
        if value is int {
            return "int:" + value.toString();
        } else if value is float {
            return "float:" + value.toString();
        } else {
            return "decimal:" + value.toString();
        }
    }

    // Test G: Return union of record types SuccessResponse|ErrorResponse
    remote isolated function getResponse(boolean success) returns SuccessResponse|ErrorResponse {
        if success {
            return {status: "OK", message: "Operation successful"};
        }
        return {errorCode: "ERR001", errorMessage: "Operation failed"};
    }

    // Test H: Process union of record types Person|Company
    remote isolated function processEntity(Person|Company entity) returns string {
        if entity is Person {
            return "Person:" + entity.name + ":" + entity.age.toString();
        } else if entity is Company {
            return "Company:" + entity.companyName + ":" + entity.employeeCount.toString();
        }
        return "Unknown";
    }

    // Test J: Return nullable record (record?)
    remote isolated function findPerson(string name) returns Person? {
        if name == "Alice" {
            return {name: "Alice", age: 30};
        }
        return ();
    }

    // Test K: Process union with nil (string|int|())
    remote isolated function processWithNil(string|int|() value) returns string {
        if value is () {
            return "nil";
        } else if value is string {
            return "string:" + value;
        } else {
            return "int:" + value.toString();
        }
    }
}

