
public client class Client {
    public function init() {
    }

    remote function validFunction(string input) returns string {
        return input;
    }

    remote function skipped1(stream<int> s) {
    }

    remote function skipped2(function() f) {
    }

    remote function skipped3(future<int> f) {
    }
}
