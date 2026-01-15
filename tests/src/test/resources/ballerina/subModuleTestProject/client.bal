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

# Root-level client for the test connector
public isolated client class Client {

    private final string serviceUrl;
    private final string? apiKey;

    # Initialize the root-level client
    #
    # + config - Connection configuration
    # + return - Error on failure
    public isolated function init(ConnectionConfig config = {}) returns error? {
        self.serviceUrl = config.serviceUrl ?: "http://example.com";
        self.apiKey = config.apiKey;
    }

    # Sample operation in root client
    #
    # + message - Message to send
    # + return - Response or error
    public isolated function sendMessage(string message) returns string|error {
        return "Root client: " + message;
    }
}

# Connection configuration for the root client
public type ConnectionConfig record {|
    # Service URL
    string serviceUrl?;
    # API key for authentication
    string apiKey?;
|};
