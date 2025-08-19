package io.ballerina.stdlib.mi;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.Connection;
import org.wso2.integration.connector.core.connection.ConnectionConfig;

public class BalConnectorConnection implements Connection {
    private final BObject balConnectorObj;

    public BalConnectorConnection(Module module, String objectTypeName, BObject clientObj) {
        this.balConnectorObj = clientObj;
    }

    @Override
    public void connect(ConnectionConfig connectionConfig) throws ConnectException {
        //TODO
    }

    @Override
    public void close() throws ConnectException {
        //TODO
    }

    public BObject getBalConnectorObj() {
        return balConnectorObj;
    }
}
