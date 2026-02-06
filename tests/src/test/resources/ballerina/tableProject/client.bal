// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

// Simple record types for testing table UI with record-typed map values and array elements
public type HeaderConfig record {|
    string name;
    string value;
|};

public type Tag record {|
    string category;
    string label;
|};

// Extended record with many fields (>10) to test removal of field count limit
public type ExtendedRecord record {|
    string field1;
    string field2;
    int field3;
    int field4;
    boolean field5;
    string field6;
    string field7;
    decimal field8;
    string field9;
    string field10;
    string field11;
|};

// Connection configuration with optional maps and arrays
public type ConnectionConfig record {|
    string baseUrl;
    map<string> defaultHeaders?;
    string[] allowedOrigins?;
|};

// Client for testing table UI generation for maps and arrays
public isolated client class TableClient {

    private string serviceUrl;

    public isolated function init(ConnectionConfig config) returns error? {
        self.serviceUrl = config.baseUrl;
    }

    // Test 1: Simple string map (should render as 2-column table: key, value)
    # Send request with custom headers
    # + headers - HTTP headers as key-value pairs
    # + return - Success message or error
    remote isolated function sendWithHeaders(map<string> headers) returns string|error {
        return "Headers processed: " + headers.length().toString();
    }

    // Test 2: Integer map (should render as 2-column table with integer validation)
    # Process query parameters
    # + queryParams - Query parameters with integer values
    # + return - Query string or error
    remote isolated function processQueryParams(map<int> queryParams) returns string|error {
        return "Query params: " + queryParams.length().toString();
    }

    // Test 3: Boolean map (should render as 2-column table with boolean dropdown)
    # Configure feature flags
    # + features - Feature flags as key-boolean pairs
    # + return - Configuration result
    remote isolated function configureFeatures(map<boolean> features) returns string|error {
        return "Features configured: " + features.length().toString();
    }

    // Test 4: Decimal map (should render as 2-column table with decimal validation)
    # Set pricing
    # + prices - Product prices as key-decimal pairs
    # + return - Pricing result
    remote isolated function setPricing(map<decimal> prices) returns string|error {
        return "Prices set: " + prices.length().toString();
    }

    // Test 5: String array (should render as single-column table)
    # Process tags
    # + tags - Array of tag strings
    # + return - Processing result
    remote isolated function processTags(string[] tags) returns string|error {
        return "Tags processed: " + tags.length().toString();
    }

    // Test 6: Integer array (should render as single-column table with integer validation)
    # Calculate sum of numbers
    # + numbers - Array of integers
    # + return - Sum of numbers
    remote isolated function calculateSum(int[] numbers) returns int|error {
        int sum = 0;
        foreach int num in numbers {
            sum += num;
        }
        return sum;
    }

    // Test 7: Boolean array (should render as single-column table with boolean dropdown)
    # Check flags
    # + flags - Array of boolean flags
    # + return - Count of true flags
    remote isolated function countTrueFlags(boolean[] flags) returns int|error {
        int count = 0;
        foreach boolean flag in flags {
            if flag {
                count += 1;
            }
        }
        return count;
    }

    // Test 8: Map with record values (should render as multi-column table: key, name, value)
    # Update header configurations
    # + headerConfigs - Header configurations as key-record pairs
    # + return - Update result
    remote isolated function updateHeaderConfigs(map<HeaderConfig> headerConfigs) returns string|error {
        return "Headers updated: " + headerConfigs.length().toString();
    }

    // Test 9: Array of records (should render as multi-column table: category, label)
    # Apply tags
    # + tagList - Array of tag records
    # + return - Application result
    remote isolated function applyTags(Tag[] tagList) returns string|error {
        return "Tags applied: " + tagList.length().toString();
    }

    // Test 10: Optional map (should render as table with required=false)
    # Set metadata
    # + metadata - Optional metadata key-value pairs
    # + return - Metadata result
    remote isolated function setMetadata(map<string> metadata = {}) returns string|error {
        return "Metadata set: " + metadata.length().toString();
    }

    // Test 11: Optional array (should render as table with required=false)
    # Add aliases
    # + aliases - Optional array of alias strings
    # + return - Aliases result
    remote isolated function addAliases(string[] aliases = []) returns string|error {
        return "Aliases added: " + aliases.length().toString();
    }

    // Test 12: Complex map - map<anydata> (should fall back to JSON input)
    # Process complex data
    # + data - Map with any data type values
    # + return - Processing result
    remote isolated function processComplexData(map<anydata> data) returns string|error {
        return "Data processed: " + data.length().toString();
    }

    // Test 13: Nested map (should fall back to JSON input)
    # Process nested configuration
    # + config - Nested map configuration
    # + return - Configuration result
    remote isolated function processNestedConfig(map<map<string>> config) returns string|error {
        return "Config processed: " + config.length().toString();
    }

    // Test 14: Nested array (should fall back to JSON input)
    # Process matrix data
    # + matrix - 2D array of strings
    # + return - Matrix result
    remote isolated function processMatrix(string[][] matrix) returns string|error {
        return "Matrix processed: " + matrix.length().toString();
    }

    // Test 15: Array of arrays with integers (should render as nested table)
    # Calculate sum of nested arrays
    # + numbers - Nested integer arrays
    # + return - Total sum
    remote isolated function calculateNestedSum(int[][] numbers) returns int|error {
        int sum = 0;
        foreach int[] row in numbers {
            foreach int num in row {
                sum += num;
            }
        }
        return sum;
    }

    // Test 16: Union type array (should render as table with type dropdown + value)
    # Process mixed data
    # + items - Array of string or integer values
    # + return - Processing result
    remote isolated function processMixedData((string|int)[] items) returns string|error {
        return "Mixed data processed: " + items.length().toString();
    }

    // Test 17: Large record for array (>10 fields) - should now render as table (no limit)
    # Process extended records
    # + records - Array of extended records
    # + return - Processing result
    remote isolated function processExtendedRecords(ExtendedRecord[] records) returns string|error {
        return "Extended records processed: " + records.length().toString();
    }

    // Test 18: Large record for map (>5 fields) - should now render as table (no limit)
    # Process large map records
    # + data - Map of extended records
    # + return - Processing result
    remote isolated function processLargeMapRecords(map<ExtendedRecord> data) returns string|error {
        return "Large map records processed: " + data.length().toString();
    }
}
