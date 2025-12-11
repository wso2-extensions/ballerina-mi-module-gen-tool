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

// Record type definitions
public type Person record {|
    string first_name;
    string last_name;
    int age;
|};

public type Address record {|
    string city;
    string street;
|};

public type User record {|
    string name;
    Address address;
    int age;
|};

public type Item record {|
    string itemId;
    int quantity;
|};

public type Order record {|
    string orderId;
    Item[] items;
    string? note;
|};

public type Product record {|
    string productId;
    float price;
|};

public type Category record {|
    string categoryName;
    Product[] products;
|};

public type Catalog record {|
    Category[] categories;
|};

public type Settings record {|
    boolean emailOptIn;
    string[] tags;
|};

public type Profile record {|
    string id;
    Settings? settings;
|};

// Client with map-based inputs
public isolated client class MapClient {

    private string serviceUrl;

    public isolated function init(string serviceUrl = "https://test.sh") returns error? {
        self.serviceUrl = serviceUrl;
    }

    // Test function: Simple record with basic fields
    // Converts person map to record and returns formatted name and age
    remote isolated function simpleRecordFunction(map<anydata> personMap) returns string|error {
        Person person = check personMap.cloneWithType();
        return person.first_name + " " + person.last_name + ", " + person.age.toString();
    }

    // Test function: Nested record handling
    // Processes user with nested address record and returns summary
    remote isolated function getUserSummary(map<anydata> userMap) returns string|error {
        User user = check userMap.cloneWithType();
        return user.name + ", " + user.address.city + ", " + user.age.toString();
    }

    // Test function: Optional fields and arrays
    // Handles order with item array and optional note field
    remote isolated function summarizeOrder(map<anydata> orderMap) returns string|error {
        Order order1 = check orderMap.cloneWithType();
        int itemsCount = order1.items.length();
        string note = order1.note ?: "no-note";
        return order1.orderId + ":" + itemsCount.toString() + ":" + note;
    }

    // Test function: Nested arrays of records
    // Computes total price from nested categories and products
    remote isolated function computeCatalogTotal(map<anydata> catalogMap) returns float|error {
        Catalog catalog = check catalogMap.cloneWithType();
        float total = 0.0;
        foreach Category c in catalog.categories {
            foreach Product p in c.products {
                total += p.price;
            }
        }
        return total;
    }

    // Test function: Optional nested record
    // Handles profile with optional settings and returns formatted string
    remote isolated function formatProfile(map<anydata> profileMap) returns string|error {
        Profile pr = check profileMap.cloneWithType();
        boolean email = false;
        int tagCount = 0;
        Settings? s = pr.settings;
        if s is Settings {
            email = s.emailOptIn;
            tagCount = s.tags.length();
        }
        return pr.id + ":" + email.toString() + ":" + tagCount.toString();
    }

    // Test function: Record transformation
    // Converts person names to uppercase and returns modified record
    remote isolated function getUppercasedPerson(map<anydata> personMap) returns Person|error {
        Person person = check personMap.cloneWithType();
        return {
            first_name: person.first_name.toUpperAscii(),
            last_name: person.last_name.toUpperAscii(),
            age: person.age
        };
    }
}
