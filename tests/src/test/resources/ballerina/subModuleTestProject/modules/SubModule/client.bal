# Sub-module client that should NOT be exposed to MI
public isolated client class Client {

    private final string endpoint;
    private final string? token;

    # Initialize the sub-module client
    #
    # + config - Connection configuration for sub-module
    # + return - Error on failure
    public isolated function init(SubModuleConfig config = {}) returns error? {
        self.endpoint = config.endpoint ?: "http://sub.example.com";
        self.token = config.token;
    }

    # Sample operation in sub-module client
    #
    # + data - Data to process
    # + return - Response or error
    public isolated function processData(string data) returns string|error {
        return "Sub-module client: " + data;
    }
}

# Configuration for the sub-module client
public type SubModuleConfig record {|
    # Endpoint URL
    string endpoint?;
    # Authentication token
    string token?;
|};
