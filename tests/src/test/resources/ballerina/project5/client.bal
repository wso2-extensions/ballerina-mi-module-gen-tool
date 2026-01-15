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
import ballerina/openapi;

// Define a record type for User
public type User record {|
    int id;
    string name;
    string email;
|};

// Simple client with resource functions
public client class SimpleClient {
    private final http:Client httpClient;

    function init(string baseUrl) returns error? {
        self.httpClient = check new (baseUrl);
    }

    // Resource function to get all users
    @openapi:ResourceInfo {
        operationId: "getUsers"
    }
    resource function get users() returns User[]|error {
        User[] users = check self.httpClient->get("/users");
        return users;
    }

    // Resource function to get a specific user by ID
    @openapi:ResourceInfo {
        operationId: "Testing123"
    }
    resource function get users/[int userId]() returns User|error {
        User user = check self.httpClient->get(string `/users/${userId}`);
        return user;
    }

    // Resource function to create a new user
    @openapi:ResourceInfo {
        operationId: "createUser"
    }
    resource function post users(User newUser) returns User|error {
        User createdUser = check self.httpClient->post("/users", newUser);
        return createdUser;
    }

    // Resource function to update a user
    @openapi:ResourceInfo {
        operationId: "updateUser"
    }
    resource function put users/[int userId](User updatedUser) returns User|error {
        User user = check self.httpClient->put(string `/users/${userId}`, updatedUser);
        return user;
    }

    // Resource function to delete a user
    @openapi:ResourceInfo {
        operationId: "deleteUser"
    }
    resource function delete users/[int userId]() returns string|error {
        string response = check self.httpClient->delete(string `/users/${userId}`);
        return response;
    }
}


