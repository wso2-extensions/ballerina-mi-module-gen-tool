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

// Test case to verify qualified names prevent conflicts when nested records have duplicate field names

public type ServerConfig record {
    string host;
    int port;
    string protocol;
};

public type DatabaseConfig record {
    string host;  // Intentional conflict with ServerConfig.host
    int port;     // Intentional conflict with ServerConfig.port
    string database;
};

public type ConnectionConfig record {
    ServerConfig server;
    DatabaseConfig database;
};

public isolated client class TestClient {

    public isolated function init(ConnectionConfig config) returns error? {
        // Initialize client with connection config
    }

    remote isolated function testOperation() returns string {
        return "test";
    }
}
