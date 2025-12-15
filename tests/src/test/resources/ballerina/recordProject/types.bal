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

public type Person record {
    string first_name;
    string last_name;
    int age;
};

// Additional complex record types used by tests
public type Address record {
    string street;
    string city;
};

public type User record {
    string name;
    Address address;
    int age;
};

public type Item record {
    string id;
    string description?;
};

public type Order record {
    string orderId;
    Item[] items;
    string note?;
};

public type Product record {
    string id;
    float price;
};

public type Category record {
    string name;
    Product[] products;
};

public type Catalog record {
    string name;
    Category[] categories;
};

public type Settings record {
    boolean emailOptIn;
    string[] tags;
};

public type Profile record {
    string id;
    Settings settings?;
};
