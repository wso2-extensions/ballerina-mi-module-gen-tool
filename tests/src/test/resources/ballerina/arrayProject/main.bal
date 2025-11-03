// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
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

// Test array transformations (similar to project2)

public function testStringArray(string[] names) returns string {
    string result = "";
    foreach string name in names {
        result = result + name + ",";
    }
    return result;
}

public function testIntArray(int[] numbers) returns int {
    int sum = 0;
    foreach int num in numbers {
        sum = sum + num;
    }
    return sum;
}

public function testBooleanArray(boolean[] flags) returns boolean {
    foreach boolean flag in flags {
        if !flag {
            return false;
        }
    }
    return true;
}

public function testFloatArray(float[] values) returns float {
    if values.length() == 0 {
        return 0.0;
    }
    float sum = 0.0;
    foreach float val in values {
        sum = sum + val;
    }
    return sum / <float>values.length();
}

public function testReturnArray(int count) returns string[] {
    string[] result = [];
    int i = 0;
    while i < count {
        result.push("item" + i.toString());
        i = i + 1;
    }
    return result;
}

public function testReturnIntArray(int max) returns int[] {
    int[] result = [];
    int i = 1;
    while i <= max {
        result.push(i);
        i = i + 1;
    }
    return result;
}

public function testReturnBooleanArray(int size) returns boolean[] {
    boolean[] result = [];
    int i = 0;
    while i < size {
        result.push(i % 2 == 0);  // Alternating true/false
        i = i + 1;
    }
    return result;
}

public function testReturnFloatArray(int size) returns float[] {
    float[] result = [];
    int i = 0;
    while i < size {
        result.push(<float>i * 1.5);
        i = i + 1;
    }
    return result;
}

public function testEmptyArray(string[] items) returns int {
    return items.length();
}

// Client class for connector testing (remote functions with arrays)
public client class ArrayClient {

    private string apiUrl;

    public function init(string apiUrl) returns error? {
        self.apiUrl = apiUrl;
    }

    remote function processStringArray(string[] names) returns string|error {
        string result = "";
        foreach string name in names {
            result = result + name + ",";
        }
        return result;
    }

    remote function processIntArray(int[] numbers) returns int|error {
        int sum = 0;
        foreach int num in numbers {
            sum = sum + num;
        }
        return sum;
    }

    remote function processBooleanArray(boolean[] flags) returns boolean|error {
        foreach boolean flag in flags {
            if !flag {
                return false;
            }
        }
        return true;
    }

    remote function processFloatArray(float[] values) returns float|error {
        if values.length() == 0 {
            return 0.0;
        }
        float sum = 0.0;
        foreach float val in values {
            sum = sum + val;
        }
        return sum / <float>values.length();
    }

    remote function getArrayLength(string[] items) returns int|error {
        return items.length();
    }

    remote function generateStringArray(int count) returns string[]|error {
        string[] result = [];
        int i = 0;
        while i < count {
            result.push("item" + i.toString());
            i = i + 1;
        }
        return result;
    }

    remote function generateIntArray(int max) returns int[]|error {
        int[] result = [];
        int i = 1;
        while i <= max {
            result.push(i);
            i = i + 1;
        }
        return result;
    }

    remote function generateBooleanArray(int size) returns boolean[]|error {
        boolean[] result = [];
        int i = 0;
        while i < size {
            result.push(i % 2 == 0);
            i = i + 1;
        }
        return result;
    }

    remote function generateFloatArray(int size) returns float[]|error {
        float[] result = [];
        int i = 0;
        while i < size {
            result.push(<float>i * 1.5);
            i = i + 1;
        }
        return result;
    }
}
