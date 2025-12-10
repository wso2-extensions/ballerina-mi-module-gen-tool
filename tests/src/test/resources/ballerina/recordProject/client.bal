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

public isolated client class RecordClient {

    private string serviceUrl;

    public isolated function init(string serviceUrl = "https://test.sh") returns error? {

        self.serviceUrl = serviceUrl;
    }

    remote isolated function simpleRecordFunction(Person person) returns string {
        return person.first_name + " " + person.last_name + ", " + person.age.toString();
    }

    // Additional remote methods for testing complex records

    // Test A: Nested record
    remote isolated function getUserSummary(User user) returns string {
        return user.name + ", " + user.address.city + ", " + user.age.toString();
    }

    // Test B: Optional fields and arrays
    remote isolated function summarizeOrder(Order order1) returns string {
        int itemsCount = order1.items.length();
        string note = order1.note ?: "no-note";
        return order1.orderId + ":" + itemsCount.toString() + ":" + note;
    }

    // Test C: Nested arrays of records — sum product prices
    remote isolated function computeCatalogTotal(Catalog catalog) returns float {
        float total = 0.0;
        foreach Category c in catalog.categories {
            foreach Product p in c.products {
                total += p.price;
            }
        }
        return total;
    }

    // Test D: Optional nested record present vs absent
    remote isolated function formatProfile(Profile pr) returns string {
        boolean email = false;
        int tagCount = 0;
        Settings? s = pr.settings;
        if s is Settings {
            email = s.emailOptIn;
            tagCount = s.tags.length();
        }
        return pr.id + ":" + email.toString() + ":" + tagCount.toString();
    }

    // Test E: Return person with uppercased name
    remote isolated function getUppercasedPerson(Person person) returns Person {
        return {
            first_name: person.first_name.toUpperAscii(),
            last_name: person.last_name.toUpperAscii(),
            age: person.age
        };
    }
}