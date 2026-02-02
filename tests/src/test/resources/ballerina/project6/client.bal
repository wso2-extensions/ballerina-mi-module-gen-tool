// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com).
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

import ballerina/http;

// Configuration for the connector
public type ConnectionConfig record {|
    string baseUrl;
    string apiKey?;
|};

// Sample data record
public type Item record {|
    string id;
    string name;
    string description;
|};

// Connector client with resource functions
public isolated client class ApiClient {
    private final http:Client httpClient;
    private final string apiKey;

    public isolated function init(ConnectionConfig config) returns error? {
        self.httpClient = check new (config.baseUrl);
        self.apiKey = config.apiKey ?: "";
    }

    # Retrieve all items from the inventory
    isolated resource function get items() returns Item[]|error {
        json response = check self.httpClient->get("/items");
        Item[] items = check response.cloneWithType();
        return items;
    }

    # Get a specific item by its unique identifier
    isolated resource function get items/[string itemId]() returns Item|error {
        string path = string `/items/${itemId}`;
        json response = check self.httpClient->get(path);
        Item item = check response.cloneWithType();
        return item;
    }

    # Search for items matching the given query
    isolated resource function get items/search(string query, int 'limit = 10) returns Item[]|error {
        string path = string `/items/search?query=${query}&limit=${'limit}`;
        json response = check self.httpClient->get(path);
        Item[] items = check response.cloneWithType();
        return items;
    }

    # Create a new item in the inventory
    isolated resource function post items(Item item) returns Item|error {
        json response = check self.httpClient->post("/items", item);
        Item createdItem = check response.cloneWithType();
        return createdItem;
    }

    # Update an existing item by its ID
    isolated resource function put items/[string itemId](Item item) returns Item|error {
        string path = string `/items/${itemId}`;
        json response = check self.httpClient->put(path, item);
        Item updatedItem = check response.cloneWithType();
        return updatedItem;
    }

    # Delete an item from the inventory
    isolated resource function delete items/[string itemId]() returns http:Response|error {
        string path = string `/items/${itemId}`;
        http:Response response = check self.httpClient->delete(path);
        return response;
    }
}
