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

// Test Gmail-style resource functions with multiple GET methods having same accessor
// but different paths. This tests the JVM method name generation.

public type ConnectionConfig record {|
    string baseUrl;
    string apiKey?;
|};

public type Draft record {|
    string id;
    string subject;
    string body;
|};

public type Message record {|
    string id;
    string threadId;
    string snippet;
|};

public type Label record {|
    string id;
    string name;
    string 'type;
|};

// Gmail-style connector client with multiple resource functions
public isolated client class GmailMockClient {
    private final http:Client httpClient;

    public isolated function init(ConnectionConfig config) returns error? {
        self.httpClient = check new (config.baseUrl);
    }

    // GET users/[userId]/drafts - List drafts
    isolated resource function get users/[string userId]/drafts() returns Draft[]|error {
        string path = string `/users/${userId}/drafts`;
        json response = check self.httpClient->get(path);
        return response.cloneWithType();
    }

    // GET users/[userId]/drafts/[id] - Get specific draft
    isolated resource function get users/[string userId]/drafts/[string id]() returns Draft|error {
        string path = string `/users/${userId}/drafts/${id}`;
        json response = check self.httpClient->get(path);
        return response.cloneWithType();
    }

    // GET users/[userId]/messages - List messages
    isolated resource function get users/[string userId]/messages() returns Message[]|error {
        string path = string `/users/${userId}/messages`;
        json response = check self.httpClient->get(path);
        return response.cloneWithType();
    }

    // GET users/[userId]/messages/[id] - Get specific message
    isolated resource function get users/[string userId]/messages/[string id]() returns Message|error {
        string path = string `/users/${userId}/messages/${id}`;
        json response = check self.httpClient->get(path);
        return response.cloneWithType();
    }

    // GET users/[userId]/labels - List labels
    isolated resource function get users/[string userId]/labels() returns Label[]|error {
        string path = string `/users/${userId}/labels`;
        json response = check self.httpClient->get(path);
        return response.cloneWithType();
    }

    // POST users/[userId]/drafts - Create draft
    isolated resource function post users/[string userId]/drafts(Draft draft) returns Draft|error {
        string path = string `/users/${userId}/drafts`;
        json response = check self.httpClient->post(path, draft);
        return response.cloneWithType();
    }

    // DELETE users/[userId]/drafts/[id] - Delete draft
    isolated resource function delete users/[string userId]/drafts/[string id]() returns error? {
        string path = string `/users/${userId}/drafts/${id}`;
        http:Response _ = check self.httpClient->delete(path);
    }

    // POST users/[userId]/messages/send - Send message
    isolated resource function post users/[string userId]/messages/send(Message message) returns Message|error {
        string path = string `/users/${userId}/messages/send`;
        json response = check self.httpClient->post(path, message);
        return response.cloneWithType();
    }

    // POST users/[userId]/messages/[id]/trash - Trash message
    isolated resource function post users/[string userId]/messages/[string id]/trash() returns Message|error {
        string path = string `/users/${userId}/messages/${id}/trash`;
        json response = check self.httpClient->post(path, {});
        return response.cloneWithType();
    }

    // POST users/[userId]/messages/'import - Import message (escaped keyword)
    isolated resource function post users/[string userId]/messages/'import(Message message) returns Message|error {
        string path = string `/users/${userId}/messages/import`;
        json response = check self.httpClient->post(path, message);
        return response.cloneWithType();
    }
}
