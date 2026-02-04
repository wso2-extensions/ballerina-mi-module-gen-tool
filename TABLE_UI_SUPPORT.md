# Table UI Support for MAP and ARRAY Parameters

This document describes which Ballerina parameter types render as table UI in MI Studio and which fall back to JSON input.

## Supported Types (Render as Table UI)

### Simple Maps
Maps with primitive value types render as 2-column tables (key + value):

- ✅ `map<string>` - String values with text input
- ✅ `map<int>` - Integer values with number validation
- ✅ `map<boolean>` - Boolean values with dropdown (true/false)
- ✅ `map<float>` - Float values with decimal validation
- ✅ `map<decimal>` - Decimal values with decimal validation

**UI Format:** 2 columns - "key" (string) and "value" (typed input)

**Example:**
```ballerina
function setPricing(map<decimal> prices) returns error?
```
Renders as:
| Key | Value |
|-----|-------|
| ... | ...   |

---

### Simple Arrays
Arrays of primitive types render as single-column tables:

- ✅ `string[]` - Text input per row
- ✅ `int[]` - Number input with integer validation
- ✅ `boolean[]` - Dropdown (true/false) per row
- ✅ `float[]` - Number input with decimal validation
- ✅ `decimal[]` - Number input with decimal validation

**UI Format:** 1 column - "value" (typed input)

**Example:**
```ballerina
function processTags(string[] tags) returns error?
```
Renders as:
| Value |
|-------|
| ...   |

---

### Record Arrays (≤10 fields)
Arrays of records with up to 10 fields render as multi-column tables:

- ✅ `Record[]` where Record has 1-10 fields

**UI Format:** One column per record field

**Example:**
```ballerina
type Tag record {
    string category;
    string label;
};

function applyTags(Tag[] tagList) returns error?
```
Renders as:
| Category | Label |
|----------|-------|
| ...      | ...   |

---

### Map of Records (≤5 fields)
Maps with record values where the record has up to 5 fields render as multi-column tables:

- ✅ `map<Record>` where Record has 1-5 fields

**UI Format:** One column for key + one column per record field

**Example:**
```ballerina
type HeaderConfig record {
    string name;
    string value;
};

function updateHeaderConfigs(map<HeaderConfig> headers) returns error?
```
Renders as:
| Key | Name | Value |
|-----|------|-------|
| ... | ...  | ...   |

---

## Unsupported Types (Fall Back to JSON Input)

### Complex Maps
These map types are too complex for table UI:

- ❌ `map<anydata>` - Value type is too generic
- ❌ `map<json>` - Value type is too generic
- ❌ `map<Record>` where Record has >5 fields - Too many columns
- ❌ `map<map<T>>` - Nested maps
- ❌ `map<T[]>` - Map values are arrays
- ❌ `map<T|U>` - Union type values

**Fallback:** JSON input field expecting a complete JSON object

**Example:**
```ballerina
function processComplexData(map<anydata> data) returns error?
```
User provides:
```json
{
  "key1": {"nested": "object"},
  "key2": [1, 2, 3],
  "key3": "simple value"
}
```

---

### Complex Arrays
These array types are too complex for table UI:

- ❌ `Record[]` where Record has >10 fields - Too many columns
- ❌ `string[][]` - Nested/2D arrays
- ❌ `int[][]` - Nested/2D arrays
- ❌ `(T|U)[]` - Union type arrays
- ❌ `json[]` - Generic JSON arrays
- ❌ `anydata[]` - Generic arrays

**Fallback:** JSON input field expecting a JSON array

**Example:**
```ballerina
function processMatrix(int[][] matrix) returns error?
```
User provides:
```json
[
  [1, 2, 3],
  [4, 5, 6],
  [7, 8, 9]
]
```

---

### Other Complex Types
- ❌ `map<record {| ... |}>` - Anonymous records
- ❌ `table<Record>` - Table types
- ❌ Optional complex types: `map<string>?`, `int[]?` - Currently not supported for table UI

---

## Field Count Limits

The table UI has field count limits to ensure usable UI:

| Type | Field Limit | Reason |
|------|-------------|--------|
| `Record[]` | ≤10 fields | Too many columns make table unusable |
| `map<Record>` | ≤5 fields | Additional key column + record fields |

When limits are exceeded, the parameter falls back to JSON input.

---

## Runtime Data Format

### Table to JSON Transformation

MI Studio serializes table data differently than direct JSON. The runtime automatically transforms:

**Simple Maps:**
```
Table format:  [{"key": "k1", "value": "v1"}, {"key": "k2", "value": "v2"}]
Runtime needs: {"k1": "v1", "k2": "v2"}
```

**Simple Arrays:**
```
Table format:  [{"value": "v1"}, {"value": "v2"}]
Runtime needs: ["v1", "v2"]
```

**Record Arrays:**
```
Table format:  [{"field1": "f1", "field2": "f2"}, ...]
Runtime needs: [{"field1": "f1", "field2": "f2"}, ...]  (no change)
```

**map<Record>:**
```
Table format:  [{"key": "k1", "field1": "f1", "field2": "f2"}, ...]
Runtime needs: {"k1": {"field1": "f1", "field2": "f2"}, ...}
```

These transformations happen automatically in `BalExecutor.java`.

---

## Known Issues

### MI Studio Trailing Comma Bug
MI Studio's table UI sometimes serializes arrays with trailing commas, producing invalid JSON:
```json
[["val1", "val2",],]
```

**Workaround:** The runtime automatically cleans up trailing commas before parsing using regex:
```java
json.replaceAll(",\\s*([\\]\\}])", "$1")
```

---

## Best Practices

1. **Use simple types when possible** - They provide the best user experience
2. **Limit record fields** - Keep records under the field limits for table UI
3. **Document complex types** - If using JSON fallback, provide clear examples in descriptions
4. **Test with MI Studio** - Verify table UI renders correctly in MI Studio
5. **Provide defaults** - Consider making complex optional parameters have default values

---

## Testing

A comprehensive test suite is available in:
```
tests/src/test/resources/ballerina/tableProject/
```

This includes test cases for:
- All supported simple maps (string, int, boolean, decimal)
- All supported simple arrays (string, int, boolean, decimal)
- Record arrays with multiple fields
- map<Record> with multiple fields
- Complex types that fall back to JSON
- Nested arrays and maps

Run tests with:
```bash
./gradlew :mi-tests:test --tests io.ballerina.mi.test.ConnectorGenTest.testTableProject
```
