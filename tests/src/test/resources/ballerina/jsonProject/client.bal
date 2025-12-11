// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
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

public isolated client class JsonClient {

    private string serviceUrl;

    public isolated function init(string serviceUrl = "https://test.sh") returns error? {
        self.serviceUrl = serviceUrl;
    }

    // Test A: Simple JSON input and return as string
    remote isolated function getJsonAsString(json data) returns string {
        return data.toString();
    }

    // Test B: Extract a field from JSON
    remote isolated function extractField(json data, string fieldName) returns json {
        json|error result = data.fieldName;
        if result is error {
            return null;
        }
        return result;
    }

    // Test C: Return JSON object
    remote isolated function createJsonObject(string name, int age) returns json {
        return {
            "name": name,
            "age": age,
            "active": true
        };
    }

    // Test D: Merge two JSON objects
    remote isolated function mergeJson(json obj1, json obj2) returns json {
        map<json> result = {};
        if obj1 is map<json> {
            foreach var [key, value] in obj1.entries() {
                result[key] = value;
            }
        }
        if obj2 is map<json> {
            foreach var [key, value] in obj2.entries() {
                result[key] = value;
            }
        }
        return result;
    }

    // Test E: Get JSON array length
    remote isolated function getArrayLength(json arr) returns int {
        if arr is json[] {
            return arr.length();
        }
        return 0;
    }

    // Test F: Transform JSON - uppercase all string values
    remote isolated function transformJson(json data) returns json {
        if data is string {
            return data.toUpperAscii();
        }
        if data is map<json> {
            map<json> result = {};
            foreach var [key, value] in data.entries() {
                if value is string {
                    result[key] = value.toUpperAscii();
                } else {
                    result[key] = value;
                }
            }
            return result;
        }
        return data;
    }

    // Test G: Get nested JSON value
    remote isolated function getNestedValue(json data) returns string {
        json|error nested = data.person;
        if nested is map<json> {
            json|error name = nested.name;
            if name is string {
                return name;
            }
        }
        return "not found";
    }

    // Test H: Sum values from JSON array
    remote isolated function sumJsonArray(json arr) returns int {
        int sum = 0;
        if arr is json[] {
            foreach var item in arr {
                if item is int {
                    sum += item;
                }
            }
        }
        return sum;
    }

    // Test I: Check if JSON contains a key
    remote isolated function hasKey(json data, string key) returns boolean {
        if data is map<json> {
            return data.hasKey(key);
        }
        return false;
    }

    // Test J: Get JSON array element at index
    remote isolated function getArrayElement(json arr, int index) returns json {
        if arr is json[] {
            if index >= 0 && index < arr.length() {
                return arr[index];
            }
        }
        return null;
    }

    // Test K: Create nested JSON object
    remote isolated function createNestedJson(string name, string city, string country) returns json {
        return {
            "person": {
                "name": name,
                "address": {
                    "city": city,
                    "country": country
                }
            }
        };
    }

    // Test L: Filter JSON array - return only objects with specific field value
    remote isolated function filterJsonArray(json arr, string status) returns json {
        json[] result = [];
        if arr is json[] {
            foreach var item in arr {
                if item is map<json> {
                    json|error statusVal = item.status;
                    if statusVal is string && statusVal == status {
                        result.push(item);
                    }
                }
            }
        }
        return result;
    }

    // Test M: Count keys in JSON object
    remote isolated function countKeys(json data) returns int {
        if data is map<json> {
            return data.keys().length();
        }
        return 0;
    }
}

