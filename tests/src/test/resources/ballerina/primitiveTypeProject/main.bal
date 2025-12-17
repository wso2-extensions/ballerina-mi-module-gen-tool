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

// Client class for primitive data type operations
public client class PrimitiveDataTypeClient {

    private string apiUrl;

    public function init(string apiUrl) returns error? {
        self.apiUrl = apiUrl;
    }

    // String scenarios
    remote function validateString(string input) returns boolean|error {
        return input.length() > 0;
    }

    remote function transformStringToUpperCase(string input) returns string|error {
        return input.toUpperAscii();
    }

    remote function transformStringToLowerCase(string input) returns string|error {
        return input.toLowerAscii();
    }

    remote function concatenateStrings(string first, string second) returns string|error {
        return first + second;
    }

    remote function getStringLength(string input) returns int|error {
        return input.length();
    }

    remote function extractSubstring(string input, int startIndex, int endIndex) returns string|error {
        return input.substring(startIndex, endIndex);
    }

    remote function checkStringContains(string input, string substring) returns boolean|error {
        return input.includes(substring);
    }

    remote function trimString(string input) returns string|error {
        return input.trim();
    }

    // Integer scenarios
    remote function addIntegers(int first, int second) returns int|error {
        return first + second;
    }

    remote function subtractIntegers(int first, int second) returns int|error {
        return first - second;
    }

    remote function multiplyIntegers(int first, int second) returns int|error {
        return first * second;
    }

    remote function divideIntegers(int dividend, int divisor) returns int|error {
        if divisor == 0 {
            return error("Division by zero");
        }
        return dividend / divisor;
    }

    remote function compareIntegers(int first, int second) returns int|error {
        if first > second {
            return 1;
        } else if first < second {
            return -1;
        }
        return 0;
    }

    remote function checkIntegerIsPositive(int value) returns boolean|error {
        return value > 0;
    }

    remote function checkIntegerIsEven(int value) returns boolean|error {
        return value % 2 == 0;
    }

    remote function getIntegerAbsoluteValue(int value) returns int|error {
        if value < 0 {
            return -value;
        }
        return value;
    }

    remote function calculateIntegerPower(int base, int exponent) returns int|error {
        if exponent < 0 {
            return error("Negative exponent not supported");
        }
        int result = 1;
        int i = 0;
        while i < exponent {
            result = result * base;
            i = i + 1;
        }
        return result;
    }

    remote function checkIntegerInRange(int value, int min, int max) returns boolean|error {
        return value >= min && value <= max;
    }

    // Boolean scenarios
    remote function performLogicalAnd(boolean first, boolean second) returns boolean|error {
        return first && second;
    }

    remote function performLogicalOr(boolean first, boolean second) returns boolean|error {
        return first || second;
    }

    remote function performLogicalNot(boolean value) returns boolean|error {
        return !value;
    }

    remote function validateBooleanTrue(boolean value) returns boolean|error {
        return value == true;
    }

    remote function validateBooleanFalse(boolean value) returns boolean|error {
        return value == false;
    }

    remote function toggleBoolean(boolean value) returns boolean|error {
        return !value;
    }

    remote function compareBooleans(boolean first, boolean second) returns boolean|error {
        return first == second;
    }

    // Float scenarios
    remote function addFloats(float first, float second) returns float|error {
        return first + second;
    }

    remote function subtractFloats(float first, float second) returns float|error {
        return first - second;
    }

    remote function multiplyFloats(float first, float second) returns float|error {
        return first * second;
    }

    remote function divideFloats(float dividend, float divisor) returns float|error {
        if divisor == 0.0 {
            return error("Division by zero");
        }
        return dividend / divisor;
    }

    remote function compareFloats(float first, float second) returns int|error {
        if first > second {
            return 1;
        } else if first < second {
            return -1;
        }
        return 0;
    }

    remote function checkFloatIsPositive(float value) returns boolean|error {
        return value > 0.0;
    }

    remote function getFloatAbsoluteValue(float value) returns float|error {
        if value < 0.0 {
            return -value;
        }
        return value;
    }

    remote function roundFloat(float value) returns int|error {
        if value >= 0.0 {
            return <int>(value + 0.5);
        }
        return <int>(value - 0.5);
    }

    remote function checkFloatInRange(float value, float min, float max) returns boolean|error {
        return value >= min && value <= max;
    }

    // Decimal scenarios
    remote function addDecimals(decimal first, decimal second) returns decimal|error {
        return first + second;
    }

    remote function subtractDecimals(decimal first, decimal second) returns decimal|error {
        return first - second;
    }

    remote function multiplyDecimals(decimal first, decimal second) returns decimal|error {
        return first * second;
    }

    remote function divideDecimals(decimal dividend, decimal divisor) returns decimal|error {
        if divisor == 0.0d {
            return error("Division by zero");
        }
        return dividend / divisor;
    }

    remote function compareDecimals(decimal first, decimal second) returns int|error {
        if first > second {
            return 1;
        } else if first < second {
            return -1;
        }
        return 0;
    }

    remote function checkDecimalIsPositive(decimal value) returns boolean|error {
        return value > 0.0d;
    }

    remote function getDecimalAbsoluteValue(decimal value) returns decimal|error {
        if value < 0.0d {
            return -value;
        }
        return value;
    }

    remote function checkDecimalInRange(decimal value, decimal min, decimal max) returns boolean|error {
        return value >= min && value <= max;
    }

    remote function convertDecimalToFloat(decimal value) returns float|error {
        return <float>value;
    }

    remote function convertFloatToDecimal(float value) returns decimal|error {
        return <decimal>value;
    }

    // Signed and Unsigned integer subtypes
    remote function processSigned8(int:Signed8 value) returns int:Signed8|error {
        return value;
    }

    remote function processSigned16(int:Signed16 value) returns int:Signed16|error {
        return value;
    }

    remote function processSigned32(int:Signed32 value) returns int:Signed32|error {
        return value;
    }

    remote function processUnsigned8(int:Unsigned8 value) returns int:Unsigned8|error {
        return value;
    }

    remote function processUnsigned16(int:Unsigned16 value) returns int:Unsigned16|error {
        return value;
    }

    remote function processUnsigned32(int:Unsigned32 value) returns int:Unsigned32|error {
        return value;
    }

    // String Char subtype
    remote function processChar(string:Char value) returns string:Char|error {
        return value;
    }
}
