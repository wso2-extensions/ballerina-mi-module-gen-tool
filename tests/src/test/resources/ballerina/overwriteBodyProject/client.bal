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

import ballerina/xmldata;

// Client class for testing all combinations between JSON, Text, and XML
public isolated client class OverwriteBodyClient {

    private string serviceUrl;

    public isolated function init(string serviceUrl = "https://test.api.com") returns error? {
        self.serviceUrl = serviceUrl;
    }

    // ==================== JSON Input ====================

    // JSON request -> JSON response
    remote isolated function jsonToJson(json jsonInput) returns json {
        return jsonInput;
    }

    // JSON request -> XML response
    remote isolated function jsonToXml(json jsonInput) returns xml|error {
        xml? result = check xmldata:fromJson(jsonInput);
        if result is () {
            return error("Failed to convert JSON to XML");
        }
        return result;
    }

    // JSON request -> Text response
    remote isolated function jsonToText(json jsonInput) returns string {
        return jsonInput.toJsonString();
    }

    // ==================== XML Input ====================

    // XML request -> JSON response
    remote isolated function xmlToJson(xml xmlInput) returns json|error {
        json result = check xmldata:toJson(xmlInput);
        return result;
    }

    // XML request -> XML response
    remote isolated function xmlToXml(xml xmlInput) returns xml {
        return xmlInput;
    }

    // XML request -> Text response
    remote isolated function xmlToText(xml xmlInput) returns string {
        return xmlInput.toString();
    }

    // ==================== Text Input ====================

    // Text request -> JSON response
    remote isolated function textToJson(string textInput) returns json|error {
        json result = check textInput.fromJsonString();
        return result;
    }

    // Text request -> XML response
    remote isolated function textToXml(string textInput) returns xml|error {
        xml result = check xml:fromString(textInput);
        return result;
    }

    // Text request -> Text response
    remote isolated function textToText(string textInput) returns string {
        return textInput;
    }
}

