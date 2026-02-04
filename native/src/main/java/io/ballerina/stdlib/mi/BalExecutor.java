/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.mi;

import com.google.gson.JsonParser;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.StructureType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;

import io.ballerina.runtime.api.types.ClientType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ResourceMethodType;

import java.util.Arrays;

import io.ballerina.runtime.internal.values.MapValueImpl;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.data.connector.ConnectorResponse;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.template.TemplateContext;
import org.ballerinalang.langlib.value.FromJsonStringWithType;

import java.util.Stack;

import static io.ballerina.stdlib.mi.Constants.*;
import static io.ballerina.stdlib.mi.Constants.ARRAY;

public class BalExecutor {

    private static final String TEMP_RESPONSE_PROPERTY_NAME = "TEMP_BAL_RESPONSE_PROPERTY_";
    protected Log log = LogFactory.getLog(BalExecutor.class);

    public boolean execute(Runtime rt, Object callable, MessageContext context) throws AxisFault, BallerinaExecutionException {
        String paramSize = getPropertyAsString(context, Constants.SIZE);
        int size = 0;
        if (paramSize != null && !paramSize.isEmpty()) {
            try {
                size = Integer.parseInt(paramSize);
            } catch (NumberFormatException e) {
                throw new SynapseException("Invalid value for property '" + Constants.SIZE + "': " + paramSize, e);
            }
        }
        Object[] args = new Object[size];
        setParameters(args, context);
        try {
            Object result;
            if (callable instanceof Module) {
                String functionName = getPropertyAsString(context, Constants.FUNCTION_NAME);
                result = rt.callFunction((Module) callable, functionName, null, args);
            } else if (callable instanceof BObject) {
                // Check if this is a resource function
                String functionType = getPropertyAsString(context, Constants.FUNCTION_TYPE);
                if (Constants.FUNCTION_TYPE_RESOURCE.equals(functionType)) {
                    // For resource functions, use the JVM method name (includes path segments)
                    // and prepend path params to args
                    String jvmMethodName = getPropertyAsString(context, Constants.JVM_METHOD_NAME);
                    if (jvmMethodName != null) {
                        jvmMethodName = jvmMethodName.replace("$$", "$^");
                    }
                    // Fallback to FUNCTION_NAME if jvmMethodName is not available
                    // This handles cases where resource functions don't have path segments
                    if (jvmMethodName == null || jvmMethodName.isEmpty()) {
                        jvmMethodName = getPropertyAsString(context, FUNCTION_NAME);
                    }
                    if (jvmMethodName == null || jvmMethodName.isEmpty()) {
                        throw new SynapseException("Neither jvmMethodName nor paramFunctionName is available for resource function invocation");
                    }
                    Object[] argsWithPathParams = prependPathParams(args, context);

                    // DEBUG: List available methods to find the correct name for rt.callMethod
                    Type callableType = ((BObject) callable).getType();
                    log.info("DEBUG: BObject Type: " + callableType.getClass().getName());

                    if (callableType instanceof ClientType) {
                        ClientType clientType = (ClientType) callableType;
                        log.info("DEBUG: Processing ClientType resources...");
                        for (ResourceMethodType resource : clientType.getResourceMethods()) {
                            log.info("DEBUG: Available Resource: Name='" + resource.getName() +
                                    "', Path=" + Arrays.toString(resource.getResourcePath()) +
                                    ", Accessor=" + resource.getAccessor());
                        }
                        for (MethodType method : clientType.getMethods()) {
                            log.info("DEBUG: Available Method: " + method.getName());
                        }
                    } else {
                        log.info("DEBUG: Not a ClientType. Type: " + callableType.getClass().getName());
                    }


                    // Manual invocation for resources because rt.callMethod doesn't support them well
                    try {
                        // 1. Get Scheduler from Runtime via Reflection
                        java.lang.reflect.Field schedulerField = rt.getClass().getDeclaredField("scheduler");
                        schedulerField.setAccessible(true);
                        Object scheduler = schedulerField.get(rt);

                        // 2. Create Strand
                        Class<?> strandClass = Class.forName("io.ballerina.runtime.internal.scheduling.Strand");
                        Class<?> schedulerClass = Class.forName("io.ballerina.runtime.internal.scheduling.Scheduler");
                        log.info("DEBUG: Inspecting Strand constructors...");
                        for (java.lang.reflect.Constructor<?> c : strandClass.getDeclaredConstructors()) {
                            log.info("DEBUG: Strand Ctor: " + c.toString());
                        }

                        java.lang.reflect.Constructor<?> strandCtor = null;
                        Object[] ctorArgs = null;

                        try {
                            strandCtor = strandClass.getDeclaredConstructor(schedulerClass);
                            ctorArgs = new Object[]{scheduler};
                        } catch (NoSuchMethodException e) {
                            // Fallback: finding first constructor having Scheduler as first parameter
                            for (java.lang.reflect.Constructor<?> c : strandClass.getDeclaredConstructors()) {
                                if (c.getParameterCount() > 0 && c.getParameterTypes()[0].equals(schedulerClass)) {
                                    strandCtor = c;
                                    log.info("DEBUG: Found substitute constructor: " + c);
                                    Class<?>[] paramTypes = c.getParameterTypes();
                                    ctorArgs = new Object[paramTypes.length];
                                    ctorArgs[0] = scheduler;
                                    // Provide proper defaults for each parameter type
                                    for (int i = 1; i < paramTypes.length; i++) {
                                        if (paramTypes[i] == boolean.class) {
                                            ctorArgs[i] = false;
                                        } else if (paramTypes[i] == int.class) {
                                            ctorArgs[i] = 0;
                                        } else if (paramTypes[i] == long.class) {
                                            ctorArgs[i] = 0L;
                                        } else if (paramTypes[i] == double.class) {
                                            ctorArgs[i] = 0.0;
                                        } else if (paramTypes[i] == float.class) {
                                            ctorArgs[i] = 0.0f;
                                        } else if (paramTypes[i] == String.class) {
                                            ctorArgs[i] = "mi-strand";
                                        } else {
                                            ctorArgs[i] = null; // Object types can be null
                                        }
                                    }
                                    break;
                                }
                            }
                        }

                        if (strandCtor == null) {
                            throw new BallerinaExecutionException("Could not find Strand constructor accepting Scheduler", new Exception("Strand constructor missing"));
                        }
                        strandCtor.setAccessible(true);
                        Object strand = strandCtor.newInstance(ctorArgs);

                        // 3. Invoke BObject.call() with logic to handle concurrency
                        // Since `call` is deprecated for removal, we access it via BObject interface if possible or reflection?
                        // Actually, BObject interface still has it. But we need to pass internal Strand (Object here).
                        // BObject.call expects `io.ballerina.runtime.internal.scheduling.Strand`.
                        // Since we can't easily import internal classes in all envs without compilation issues,
                        // we cast to BObject which effectively uses the internal class at runtime.
                        // However, adding explicit import `io.ballerina.runtime.internal.scheduling.Strand` is risky if package format changes.
                        // But `BalExecutor` is in native, so it should be fine.

                        // We need to cast our reflected 'strand' object to the Type expected by call method.
                        // The `call` method expects `io.ballerina.runtime.internal.scheduling.Strand`.
                        // If we add the import, we can do it.

                        // Let's try invoking `call` via reflection to avoid Import issues with internal classes if possible.
                        // Method callMethod = callable.getClass().getMethod("call", strandClass, String.class, Object[].class);
                        // result = callMethod.invoke(callable, strand, jvmMethodName, argsWithPathParams);

                        // BUT, if we can import, it's better. `MapValueImpl` is already imported from internal.
                        // So we CAN import Strand.

                        // RETRY: Using imports at top of file (added via separate step if needed, or I can try here).
                        // I will assume I can't easily add imports mid-file. 
                        // I will use Reflection for EVERYTHING to be safe from import errors.

                        java.lang.reflect.Method callMethod = callable.getClass().getMethod("call", strandClass, String.class, Object[].class);
                        result = callMethod.invoke(callable, strand, jvmMethodName, argsWithPathParams);

                        // 4. Handle Async Result
                        if (result == null) {
                            // Function yielded. We need to wait for the Future inside the strand.
                            // Strand has a field `future`?
                            // No, probably need to check implementation.
                            // Usually `call` returns the value if strict?
                            // Wait, generated code: return $value$ or yields.
                            // If it yields, it returns NULL?
                            // We might need to block on `strand.returnValue`?

                            // Let's try to assume result is returned if we waited?
                            // No, we didn't wait.

                            // Simplest Hack: Loop and wait until strand is 'done'.
                            java.lang.reflect.Method isDoneMsg = strandClass.getMethod("isDone");
                            while (!(boolean) isDoneMsg.invoke(strand)) {
                                Thread.sleep(10); // Spin wait (bad but effective for tool)
                            }

                            // Get result from future
                            java.lang.reflect.Field futureField = strandClass.getDeclaredField("future");
                            futureField.setAccessible(true);
                            Object futureValue = futureField.get(strand);

                            if (futureValue != null) {
                                Class<?> futureClass = futureValue.getClass();
                                java.lang.reflect.Field resultField = futureClass.getDeclaredField("result");
                                resultField.setAccessible(true);
                                result = resultField.get(futureValue);

                                // Check for panic/error
                                java.lang.reflect.Field panicField = futureClass.getDeclaredField("panic");
                                panicField.setAccessible(true);
                                Object panic = panicField.get(futureValue);
                                if (panic != null) {
                                    if (panic instanceof BError) {
                                        throw (BError) panic;
                                    }
                                    if (panic instanceof Throwable) {
                                        throw new BallerinaExecutionException("Panic in Ballerina function: " + ((Throwable) panic).getMessage(), (Throwable) panic);
                                    }
                                    throw new BallerinaExecutionException("Panic in Ballerina function: " + panic, new Exception(String.valueOf(panic)));
                                }
                            }
                        }

                    } catch (Exception e) { // ReflectiveOperationException etc
                        // Detect "No such method" from BObject.call logic if any?
                        if (e.getCause() instanceof BError) {
                            throw (BError) e.getCause();
                        }
                        log.error("Failed to invoke resource manually: " + e.getMessage(), e);
                        throw new BallerinaExecutionException("Resource invocation failed: " + e.getMessage(), e);
                    }

                } else {
                    // For remote/other functions, use the synapse name (paramFunctionName)
                    String functionName = getPropertyAsString(context, Constants.FUNCTION_NAME);
                    log.info("DEBUG: Invoking remote function: " + functionName + " with " + args.length + " args");
                    for (int i = 0; i < args.length; i++) {
                        log.info("DEBUG: Arg[" + i + "] type: " + (args[i] != null ? args[i].getClass().getName() : "null"));
                    }
                    result = rt.callMethod((BObject) callable, functionName, null, args);
                    log.info("DEBUG: Remote function returned: " + (result != null ? result.getClass().getName() : "null"));
                }
            } else {
                throw new SynapseException("Unsupported callable type: " + callable.getClass().getName());
            }
            if (result instanceof BError bError) {
                log.error("Function execution returned BError: " + bError.getMessage());
                throw new BallerinaExecutionException(bError.getMessage(), bError.fillInStackTrace());
            }
            log.info("Function execution successful, processing response...");
            Object processedResult = processResponse(result);
            log.info("Response processed, processed result type: " + (processedResult != null ? processedResult.getClass().getSimpleName() : "null"));

            ConnectorResponse connectorResponse = new DefaultConnectorResponse();
            String resultProperty = getResultProperty(context);
            boolean overwriteBody = isOverwriteBody(context);
            log.info("Setting result - overwriteBody=" + overwriteBody + ", resultProperty=" + resultProperty);

            if (overwriteBody) {
                log.info("Calling PayloadWriter.overwriteBody with processed result");
                PayloadWriter.overwriteBody(context, processedResult);
                log.info("PayloadWriter.overwriteBody completed successfully");
            } else {
                log.info("Setting payload on ConnectorResponse");
                connectorResponse.setPayload(processedResult);
            }
            context.setVariable(resultProperty, connectorResponse);
            log.info("Result variable set successfully, invocation complete");
        } catch (BError bError) {
            log.error("BError caught during execution: " + bError.getMessage(), bError);
            throw new BallerinaExecutionException(bError.getMessage(), bError.fillInStackTrace());
        } catch (Exception e) {
            log.error("Unexpected error during execution: " + e.getMessage(), e);
            throw new SynapseException("Error during Ballerina function execution", e);
        }
        return true;
    }

    private Object processResponse(Object result) {
        if (result == null) {
            log.info("Processing response: null result");
            return null;
        }

        String resultType = result.getClass().getSimpleName();
        log.info("Processing response of type: " + resultType);

        if (result instanceof BXml) {
            log.info("Converting BXml to OMElement");
            return BXmlConverter.toOMElement((BXml) result);
        } else if (result instanceof BDecimal) {
            String value = ((BDecimal) result).value().toString();
            log.info("Converting BDecimal to String: " + value);
            return value;
        } else if (result instanceof BString) {
            String value = ((BString) result).getValue();
            log.info("Converting BString to String: " + value);
            return value;
        } else if (result instanceof BArray) {
            // Convert BArray to JSON format for MI consumption
            log.info("Converting BArray to JsonElement");
            return JsonParser.parseString(TypeConverter.arrayToJsonString((BArray) result));
        } else if (result instanceof BMap) {
            log.info("Converting BMap to JsonElement");
            return JsonParser.parseString(((MapValueImpl<?, ?>) result).getJSONString());
        } else if (result instanceof Long || result instanceof Integer) {
            // Convert int to JsonElement for proper payload writing
            String value = result.toString();
            log.info("Converting int/Long to JsonElement: " + value);
            return JsonParser.parseString(value);
        } else if (result instanceof Boolean) {
            // Convert boolean to JsonElement for proper payload writing
            String value = result.toString();
            log.info("Converting Boolean to JsonElement: " + value);
            return JsonParser.parseString(value);
        } else if (result instanceof Double || result instanceof Float) {
            // Convert float to JsonElement for proper payload writing
            String value = result.toString();
            log.info("Converting float/Double to JsonElement: " + value);
            return JsonParser.parseString(value);
        }

        log.warn("Unhandled result type: " + resultType + ", returning as-is");
        return result;
    }

    private void setParameters(Object[] args, MessageContext context) {
        log.info("Setting parameters for function, args count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            Object param = getParameter(context, "param" + i, "paramType" + i, i);
            args[i] = param;
            String paramType = param != null ? param.getClass().getSimpleName() : "null";
            log.info("Parameter[" + i + "] set - type: " + paramType);
            if (param instanceof BArray) {
                BArray array = (BArray) param;
                log.info("Parameter[" + i + "] is BArray with size: " + array.size());
                if (array.size() > 0) {
                    Object firstElem = array.get(0);
                    log.info("Parameter[" + i + "] first element type: " + (firstElem != null ? firstElem.getClass().getSimpleName() : "null"));
                }
            }
        }
    }

    /**
     * Get a property value as a String from the MessageContext.
     * Returns null if the property doesn't exist.
     */
    private String getPropertyAsString(MessageContext context, String propertyName) {
        Object value = context.getProperty(propertyName);
        return value != null ? value.toString() : null;
    }

    /**
     * Prepend path parameter values to the args array for resource function invocation.
     * Path params are passed as the first arguments to resource methods in Ballerina.
     *
     * @param args    The original function arguments
     * @param context The message context containing path param values
     * @return A new array with path params prepended to the original args
     */
    private Object[] prependPathParams(Object[] args, MessageContext context) {
        String pathParamSizeStr = getPropertyAsString(context, Constants.PATH_PARAM_SIZE);
        int pathParamSize = 0;
        if (pathParamSizeStr != null) {
            try {
                pathParamSize = Integer.parseInt(pathParamSizeStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid PATH_PARAM_SIZE value '" + pathParamSizeStr +
                        "'. Defaulting path parameter size to 0.", e);
            }
        }

        if (pathParamSize == 0) {
            return args;
        }

        Object[] pathParams = new Object[pathParamSize];
        for (int i = 0; i < pathParamSize; i++) {
            String pathParamName = getPropertyAsString(context, "pathParam" + i);
            String pathParamType = getPropertyAsString(context, "pathParamType" + i);

            // Get the path param value from template parameters
            Object pathParamValue = lookupTemplateParameter(context, pathParamName);

            if (pathParamValue != null) {
                // Convert path param to appropriate type
                pathParams[i] = convertPathParam(pathParamValue.toString(), pathParamType);
            } else {
                log.warn("Path parameter '" + pathParamName + "' not found in context");
                pathParams[i] = null;
            }
        }

        // Create new array with path params prepended
        Object[] combined = new Object[pathParamSize + args.length];
        System.arraycopy(pathParams, 0, combined, 0, pathParamSize);
        System.arraycopy(args, 0, combined, pathParamSize, args.length);

        return combined;
    }

    /**
     * Convert a path parameter value to the appropriate Ballerina type.
     *
     * @param value The string value of the path parameter
     * @param type  The expected type (string, int, etc.)
     * @return The converted value
     */
    private Object convertPathParam(String value, String type) {
        if (type == null) {
            type = Constants.STRING; // Default to string
        }
        try {
            return switch (type) {
                case Constants.INT -> Long.parseLong(value);
                case Constants.FLOAT -> Double.parseDouble(value);
                case Constants.BOOLEAN -> Boolean.parseBoolean(value);
                case Constants.DECIMAL -> ValueCreator.createDecimalValue(value);
                default -> StringUtils.fromString(value); // STRING and others
            };
        } catch (NumberFormatException e) {
            throw new SynapseException(
                    "Invalid value '" + value + "' for path parameter type '" + type + "'", e);
        }
    }

    public Object getParameter(MessageContext context, String value, String type, int index) {
        log.info("getParameter called - value: " + value + ", type: " + type + ", index: " + index);

        String paramName = getPropertyAsString(context, value);
        if (paramName == null) {
            log.error("Parameter definition property '" + value + "' not found in context. Check if the generated XML artifacts are correct.");
            throw new SynapseException("Parameter definition property '" + value + "' is missing");
        }
        log.info("Parameter name: " + paramName);

        Object param = lookupTemplateParameter(context, paramName);
        log.info("Parameter value from context: " + (param != null ? "present" : "null"));

        String paramType;
        if (value.matches("param\\d+Union.*")) {
            paramType = type;
        } else {
            paramType = getPropertyAsString(context, type);
            if (paramType == null) {
                // If paramType property is missing, maybe default or error?
                // For safety, log and fail, or default to string if param exists?
                // But paramType is critical for conversion.
                log.warn("Parameter type property '" + type + "' not found in context. Defaulting to STRING.");
                paramType = Constants.STRING;
            }
        }
        log.info("Parameter type: " + paramType);
        if (param == null) {
            // For UNION and RECORD types, we pass null to the handler methods as they might 
            // reconstruction logic (e.g. from flattened fields)
            if (UNION.equals(paramType)) {
                return getUnionParameter(paramName, context, index);
            } else if (RECORD.equals(paramType)) {
                return createRecordValue(null, paramName, context, index);
            }

            // For other types, return null if the value is missing.
            // This assumes the Ballerina parameter is optional/nullable.
            // If it is required, the function invocation will fail later with a type error.
            return null;
        }

        try {
            Object result = switch (paramType) {
                case BOOLEAN -> Boolean.parseBoolean((String) param);
                case INT -> Long.parseLong((String) param);
                case STRING -> StringUtils.fromString((String) param);
                case FLOAT -> Double.parseDouble((String) param);
                case DECIMAL -> ValueCreator.createDecimalValue((String) param);
                case JSON -> getJsonParameter(param);
                case XML -> getBXmlParameter(context, value);
                case RECORD -> createRecordValue((String) param, paramName, context, index);
                case ARRAY -> getArrayParameter((String) param, context, value);
                case MAP -> getMapParameter(param, context, value);
                case UNION -> getUnionParameter(paramName, context, index);
                default -> null;
            };
            log.info("getParameter returning - type: " + (result != null ? result.getClass().getSimpleName() : "null"));
            return result;
        } catch (Exception e) {
            log.error("Error in getParameter for " + paramName + " (type: " + paramType + "): " + e.getMessage(), e);
            throw new SynapseException("Failed to process parameter " + paramName, e);
        }
    }

    private Object getUnionParameter(String paramName, MessageContext context, int index) {
        Object paramType = lookupTemplateParameter(context, paramName + "DataType");
        if (paramType instanceof String typeStr) {
            String unionParamName = "param" + index + "Union" + org.apache.commons.lang3.StringUtils.capitalize(typeStr);
            return getParameter(context, unionParamName, typeStr, -1);
        }
        return null;
    }

    private Object createRecordValue(String jsonString, String paramName, MessageContext context, int paramIndex) {
        // Check if this is a flattened record from init function
        // Null jsonString indicates the record needs to be reconstructed from flattened fields
        if (jsonString == null) {
            // This is a flattened record from init function
            String recordParamName = paramName; // e.g., "config"

            // For init functions, find the connection type from the context properties
            String connectionType = findConnectionTypeForParam(context, recordParamName);
            if (connectionType == null) {
                throw new SynapseException("Cannot create record value: jsonString is null and connectionType not found. " +
                        "Parameter '" + paramName + "' at index " + paramIndex + " may be missing required value.");
            }

            // Reconstruct the record from flattened fields
            Object reconstructedBMap = reconstructRecordFromFields(recordParamName, context, connectionType);

            // Now convert the BMap to the typed record
            // For init/config, use connectionType prefix for property name
            String recordNamePropertyKey = connectionType + "_param" + paramIndex + "_recordName";
            Object recordNameObj = context.getProperty(recordNamePropertyKey);

            if (recordNameObj == null) {
                throw new SynapseException("Record name not found for parameter at index " + paramIndex +
                        ". Ensure '" + recordNamePropertyKey + "' property is set in the synapse template.");
            }
            String recordName = recordNameObj.toString();

            BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
            Type recType = recValue.getType();

            // Convert the reconstructed BMap to JSON and then to typed record
            if (reconstructedBMap instanceof BMap) {
                String jsonStr = ((io.ballerina.runtime.internal.values.MapValueImpl<?, ?>) reconstructedBMap).getJSONString();
                BString jsonBString = StringUtils.fromString(jsonStr);
                return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
            }

            throw new SynapseException("Failed to reconstruct record from flattened fields for parameter '" + paramName + "'");
        }

        // Original logic for regular JSON record values
        log.info("DEBUG: Processing regular JSON record value (not flattened)");
        if (jsonString.startsWith("'") && jsonString.endsWith("'")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);
            log.info("DEBUG: Stripped surrounding quotes from JSON");
        }
        log.info("DEBUG: Cleaned JSON string: " + jsonString);

        // Try to get the record name for typed conversion
        Object recordNameObj = context.getProperty("param" + paramIndex + "_recordName");
        log.info("DEBUG: Looking for recordName at key 'param" + paramIndex + "_recordName' -> " + recordNameObj);

        if (recordNameObj != null) {
            String recordName = recordNameObj.toString();
            // Try typed conversion first (works when Strand is available, e.g., in tests)
            try {
                // If Strand is available, this is the best path
                BString jsonBString = StringUtils.fromString(jsonString);
                BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
                Type recType = recValue.getType();
                Object result = FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
                log.info("=== DEBUG: createRecordValue END (typed record via FromJsonStringWithType) ===");
                return result;
            } catch (Exception e) {
                // If typed conversion fails (e.g., Strand null in MI runtime), fall back to manual deep conversion
                log.warn("DEBUG: FromJsonStringWithType failed (likely due to null Strand): " + e.getMessage());
                log.info("DEBUG: Attempting manual deep conversion for: " + recordName);

                try {
                    // 1. Parse JSON to generic BMap/BArray
                    Object parseResult = JsonUtils.parse(jsonString);

                    // 2. Create the target empty record to get Type info
                    BMap<BString, Object> emptyRecord = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
                    Type targetType = emptyRecord.getType();

                    // 3. Perform manual deep conversion
                    Object convertedResult = convertValueToType(parseResult, targetType);
                    log.info("=== DEBUG: createRecordValue END (manual deep conversion) ===");
                    return convertedResult;
                } catch (Exception deepEx) {
                    log.error("DEBUG: Manual deep conversion failed: " + deepEx.getMessage(), deepEx);
                    // Fall through to generic fallback
                }
            }
        }

        // Final Fallback: Use JsonUtils.parse for generic JSON (no type checking)
        try {
            Object parseResult = JsonUtils.parse(jsonString);
            log.info("DEBUG: JsonUtils.parse result type: " + (parseResult != null ? parseResult.getClass().getName() : "null"));

            if (parseResult instanceof BError) {
                BError error = (BError) parseResult;
                log.error("DEBUG: JsonUtils.parse returned an error: " + error.getMessage());
                throw new SynapseException("Failed to parse JSON for record: " + error.getMessage());
            }

            log.info("=== DEBUG: createRecordValue END (generic JSON) ===");
            return parseResult;
        } catch (Exception e) {
            log.error("DEBUG: Exception in createRecordValue: " + e.getMessage(), e);
            throw new SynapseException("Failed to create record value: " + e.getMessage(), e);
        }
    }

    /**
     * Deep converts a generic value (from JsonUtils.parse) to a strictly typed value
     * based on the target Type. This avoids using FromJsonStringWithType which requires a Strand.
     */
    private Object convertValueToType(Object sourceValue, Type targetType) {
        if (sourceValue == null) {
            return null;
        }

        // Handle Record conversion
        if (targetType.getTag() == TypeTags.RECORD_TYPE_TAG && sourceValue instanceof BMap) {
            return createTypedRecordFromGeneric((BMap<BString, Object>) sourceValue, (StructureType) targetType);
        }

        // Handle Array conversion
        if (targetType.getTag() == TypeTags.ARRAY_TAG && sourceValue instanceof BArray) {
            return createTypedArrayFromGeneric((BArray) sourceValue, (ArrayType) targetType);
        }

        // Handle Union types (simplified approach - check member types)
        // Note: For now we return sourceValue as-is for unions, primitive types, etc.
        // as they are usually compatible or handled by Ballerina's dynamic typing.
        return sourceValue;
    }

    private BMap<BString, Object> createTypedRecordFromGeneric(BMap<BString, Object> genericMap, StructureType targetType) {
        // Create the typed record
        BMap<BString, Object> typedRecord = ValueCreator.createRecordValue(targetType.getPackage(), targetType.getName());

        // Migrate fields
        for (Field field : targetType.getFields().values()) {
            String fieldName = field.getFieldName();
            BString bFieldName = StringUtils.fromString(fieldName);

            if (genericMap.containsKey(bFieldName)) {
                Object genericValue = genericMap.get(bFieldName);
                Object convertedValue = convertValueToType(genericValue, field.getFieldType());
                typedRecord.put(bFieldName, convertedValue);
            }
        }
        return typedRecord;
    }

    private BArray createTypedArrayFromGeneric(BArray genericArray, ArrayType targetType) {
        // NOTE: Creating a strongly typed BArray from scratch is difficult without specific ValueCreator APIs for each type.
        // However, we can convert the elements *inside* the array if possible.
        // Since generic BArray (json[]) can hold any value, replacing generic Maps with Typed Records
        // inside it might be sufficient for Ballerina to accept it, or at least for field access to work.

        long size = genericArray.size();
        for (long i = 0; i < size; i++) {
            Object value = genericArray.get(i);
            Object converted = convertValueToType(value, targetType.getElementType());

            // Update array element if conversion happened and value changed
            if (value != converted) {
                // Determine implicit type of array to invoke correct add/put method?
                // BArray interface has add() for various types. genericArray is likely generic (RefValues).
                // Safe to use add(i, ref) for object/record types.
                try {
                    genericArray.add(i, converted);
                } catch (Exception e) {
                    log.warn("Failed to update array element at index " + i + ": " + e.getMessage());
                }
            }
        }
        return genericArray;
    }

    /**
     * Finds the connection type prefix for a given record parameter name.
     * Gets the connectionType directly from the function stack template parameters.
     *
     * @param context         The message context
     * @param recordParamName The record parameter name (e.g., "config")
     * @return The connection type prefix or null if not found
     */
    private String findConnectionTypeForParam(MessageContext context, String recordParamName) {
        // Get connectionType directly from the function stack
        Object connectionType = lookupTemplateParameter(context, "connectionType");
        if (connectionType != null) {
            return connectionType.toString();
        }
        return null;
    }

    /**
     * Reconstructs a record from flattened fields stored in the context properties.
     * Used for init function parameters where record fields are flattened in the XML.
     *
     * @param recordParamName The name of the record parameter (e.g., "config")
     * @param context         The message context containing property values
     * @param connectionType  The connection type prefix (e.g., "GOOGLEAPIS_GMAIL_CLIENT")
     * @return A JSON object representing the reconstructed record
     */
    private Object reconstructRecordFromFields(String recordParamName, MessageContext context, String connectionType) {
        // Build a JSON object from the flattened fields
        // Fields are stored as {connectionType}_{recordParamName}_param{index} = "fieldPath"
        // e.g., GOOGLEAPIS_GMAIL_CLIENT_config_param0 = "http1Settings.keepAlive"

        log.info("=== DEBUG: reconstructRecordFromFields START ===");
        log.info("DEBUG: recordParamName=" + recordParamName + ", connectionType=" + connectionType);

        com.google.gson.JsonObject recordJson = new com.google.gson.JsonObject();

        // First pass: collect all union field paths and their selected types
        java.util.Map<String, String> unionFieldSelectedTypes = new java.util.HashMap<>();
        int tempIndex = 0;
        while (true) {
            String fieldNameKey = connectionType + "_" + recordParamName + "_param" + tempIndex;
            String fieldTypeKey = connectionType + "_" + recordParamName + "_paramType" + tempIndex;
            Object fieldNameObj = context.getProperty(fieldNameKey);
            Object fieldTypeObj = context.getProperty(fieldTypeKey);

            if (fieldNameObj == null || fieldTypeObj == null) {
                break;
            }

            String fieldPath = fieldNameObj.toString();
            String fieldType = fieldTypeObj.toString();

            if (UNION.equals(fieldType)) {
                String dataTypeKey = connectionType + "_" + recordParamName + "_dataType" + tempIndex;
                Object dataTypeParamNameObj = context.getProperty(dataTypeKey);
                if (dataTypeParamNameObj != null) {
                    String dataTypeParamName = dataTypeParamNameObj.toString();
                    Object selectedTypeObj = lookupTemplateParameter(context, dataTypeParamName);
                    if (selectedTypeObj != null) {
                        unionFieldSelectedTypes.put(fieldPath, selectedTypeObj.toString());
                        log.info("DEBUG: Union field '" + fieldPath + "' has selected type: " + selectedTypeObj);
                    }
                }
            }
            tempIndex++;
        }

        // Second pass: process all fields
        int fieldIndex = 0;
        while (true) {
            String fieldNameKey = connectionType + "_" + recordParamName + "_param" + fieldIndex;
            String fieldTypeKey = connectionType + "_" + recordParamName + "_paramType" + fieldIndex;
            String unionMemberKey = connectionType + "_" + recordParamName + "_unionMember" + fieldIndex;

            Object fieldNameObj = context.getProperty(fieldNameKey);
            Object fieldTypeObj = context.getProperty(fieldTypeKey);
            Object unionMemberObj = context.getProperty(unionMemberKey);

            log.info("DEBUG: Checking fieldIndex=" + fieldIndex + ", fieldNameKey=" + fieldNameKey + " -> " + fieldNameObj);
            log.info("DEBUG: Checking fieldIndex=" + fieldIndex + ", fieldTypeKey=" + fieldTypeKey + " -> " + fieldTypeObj);

            if (fieldNameObj == null || fieldTypeObj == null) {
                log.info("DEBUG: No more fields found at index " + fieldIndex);
                break; // No more fields
            }

            String fieldPath = fieldNameObj.toString();
            String fieldType = fieldTypeObj.toString();
            String unionMemberType = unionMemberObj != null ? unionMemberObj.toString() : null;

            // Handle union type field itself
            if (UNION.equals(fieldType)) {
                // If this union field itself belongs to a parent union member, check if it should be included
                if (unionMemberType != null) {
                    String parentUnionPath = findParentUnionPath(fieldPath, unionFieldSelectedTypes.keySet());
                    if (parentUnionPath != null) {
                        String selectedType = unionFieldSelectedTypes.get(parentUnionPath);
                        if (selectedType != null && !selectedType.equals(unionMemberType)) {
                            // This union field belongs to a different union member, skip it and its nested fields
                            log.info("DEBUG: Skipping union field '" + fieldPath + "' (belongs to " + unionMemberType +
                                    ", but selected type is " + selectedType + ")");
                            fieldIndex++;
                            continue;
                        }
                    }
                }

                // Check if this is a "Leaf Union" (value provided directly, e.g. string|string[])
                // Use sanitized path for lookup as done for regular fields
                String sanitizedFieldPath = fieldPath.replace(".", "_");
                Object unionValue = lookupTemplateParameter(context, sanitizedFieldPath);

                if (unionValue != null) {
                    log.info("DEBUG: Found direct value for union field '" + fieldPath + "': " + unionValue);
                    setNestedField(recordJson, fieldPath, unionValue, fieldType);
                    fieldIndex++;
                    continue;
                }

                log.info("DEBUG: Processing union field at path=" + fieldPath + " (nested fields will provide values)");
                fieldIndex++;
                continue;
            }

            // If this field belongs to a union member, check if it matches the selected type
            if (unionMemberType != null) {
                // Find the parent union field path for this field
                String parentUnionPath = findParentUnionPath(fieldPath, unionFieldSelectedTypes.keySet());
                if (parentUnionPath != null) {
                    String selectedType = unionFieldSelectedTypes.get(parentUnionPath);
                    if (selectedType != null && !selectedType.equals(unionMemberType)) {
                        // This field belongs to a different union member, skip it
                        log.info("DEBUG: Skipping field '" + fieldPath + "' (belongs to " + unionMemberType +
                                ", but selected type is " + selectedType + ")");
                        fieldIndex++;
                        continue;
                    }
                }
            }

            // Get the actual field value from the template parameters
            // Convert dots to underscores because Synapse parameter names use underscores
            // (e.g., field path "auth.token" maps to parameter name "auth_token")
            String sanitizedFieldPath = fieldPath.replace(".", "_");
            Object fieldValue = lookupTemplateParameter(context, sanitizedFieldPath);

            log.info("DEBUG: Field[" + fieldIndex + "]: path=" + fieldPath + ", sanitized=" + sanitizedFieldPath + ", type=" + fieldType + ", value=" + fieldValue);

            if (fieldValue != null) {
                // Set the nested field value in the JSON object using the original dot-notation path
                setNestedField(recordJson, fieldPath, fieldValue, fieldType);
                log.info("DEBUG: Set field '" + fieldPath + "' = " + fieldValue);
            } else {
                log.warn("DEBUG: Field '" + fieldPath + "' has NULL value - NOT SETTING");
            }

            fieldIndex++;
        }

        String recordJsonString = recordJson.toString();
        log.info("DEBUG: Final reconstructed JSON: " + recordJsonString);
        log.info("=== DEBUG: reconstructRecordFromFields END ===");

        // Convert JSON object to BMap
        return JsonUtils.parse(recordJsonString);
    }

    /**
     * Finds the parent union field path for a given field path.
     * For example, if fieldPath is "auth.token" and unionPaths contains "auth",
     * this returns "auth".
     *
     * @param fieldPath  The full field path (e.g., "auth.token")
     * @param unionPaths Set of known union field paths
     * @return The parent union path, or null if not found
     */
    private String findParentUnionPath(String fieldPath, java.util.Set<String> unionPaths) {
        for (String unionPath : unionPaths) {
            if (fieldPath.startsWith(unionPath + ".")) {
                return unionPath;
            }
        }
        return null;
    }

    /**
     * Sets a nested field value in a JSON object using dot notation path.
     * For example, "http1Settings.proxy.host" creates nested objects and sets the value.
     *
     * @param jsonObject The root JSON object
     * @param fieldPath  The dot-notation path (e.g., "http1Settings.proxy.host")
     * @param value      The value to set
     * @param fieldType  The type of the field
     */
    private void setNestedField(com.google.gson.JsonObject jsonObject, String fieldPath, Object value, String fieldType) {
        String[] parts = fieldPath.split("\\.");

        // Navigate/create nested objects up to the second-to-last part
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!jsonObject.has(part)) {
                jsonObject.add(part, new com.google.gson.JsonObject());
            }
            jsonObject = jsonObject.getAsJsonObject(part);
        }

        // Set the final field value with appropriate type
        String finalField = parts[parts.length - 1];
        String valueStr = value.toString();

        switch (fieldType) {
            case BOOLEAN:
                jsonObject.addProperty(finalField, Boolean.parseBoolean(valueStr));
                break;
            case INT:
                jsonObject.addProperty(finalField, Long.parseLong(valueStr));
                break;
            case FLOAT:
                jsonObject.addProperty(finalField, Double.parseDouble(valueStr));
                break;
            case DECIMAL:
                // Handle decimal as a numeric value
                jsonObject.addProperty(finalField, new java.math.BigDecimal(valueStr));
                break;
            case JSON:
            case RECORD:
            case UNION:
            case ARRAY:
                // Parse JSON string and add as JsonElement for complex types
                try {
                    com.google.gson.JsonElement jsonElement = JsonParser.parseString(valueStr);
                    jsonObject.add(finalField, jsonElement);
                } catch (com.google.gson.JsonSyntaxException e) {
                    // If parsing fails, fall back to string
                    log.warn("Failed to parse JSON value for field '" + fieldPath + "', treating as string: " + e.getMessage());
                    jsonObject.addProperty(finalField, valueStr);
                }
                break;
            default:
                // String and other types
                jsonObject.addProperty(finalField, valueStr);
                break;
        }
    }

    private BXml getBXmlParameter(MessageContext context, String parameterName) {
        OMElement omElement = getOMElement(context, parameterName);
        if (omElement == null) {
            return null;
        }
        return OMElementConverter.toBXml(omElement);
    }

    private OMElement getOMElement(MessageContext ctx, String value) {
        String param = ctx.getProperty(value).toString();
        Object paramValue = lookupTemplateParameter(ctx, param);
        if (paramValue != null) {
            if (paramValue instanceof OMElement) {
                return (OMElement) paramValue;
            } else {
                try {
                    return AXIOMUtil.stringToOM((String) lookupTemplateParameter(ctx, param));
                } catch (Exception ignored) {
                }
            }
        }
        log.error("Error in getting the OMElement");
        return null;
    }

    public static Object lookupTemplateParameter(MessageContext ctx, String paramName) {
        Stack funcStack = (Stack) ctx.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        if (funcStack == null || funcStack.isEmpty()) {
            // Fallback for testing or when function stack is not available
            // Read directly from context properties
            return ctx.getProperty(paramName);
        }
        TemplateContext currentFuncHolder = (TemplateContext) funcStack.peek();
        Object value = currentFuncHolder.getParameterValue(paramName);

        // Debug: Log available parameters (only once per template invocation)
        if (value == null && paramName.contains(".")) {
            // Log all available parameter names for debugging
            java.util.Map<String, Object> params = currentFuncHolder.getMappedValues();
            if (params != null && !params.isEmpty()) {
                LogFactory.getLog(BalExecutor.class).info("DEBUG lookupTemplateParameter: Looking for '" + paramName +
                        "' - Available params: " + params.keySet());
            }
        }

        return value;
    }

    private Object getJsonParameter(Object param) {
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            return JsonUtils.parse(strParam);
        } else {
            return JsonUtils.parse(param.toString());
        }
    }

    private BMap getMapParameter(Object param, MessageContext context, String valueKey) {
        String jsonString;
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            jsonString = strParam;
        } else {
            jsonString = param.toString();
        }

        // Clean up invalid JSON (trailing commas from MI Studio table UI)
        log.info("Map parameter JSON string (original): " + jsonString);
        String cleanedJson = cleanupJsonString(jsonString);
        if (!cleanedJson.equals(jsonString)) {
            log.info("JSON cleaned up (removed trailing commas): " + cleanedJson);
        }

        Object parsed = JsonUtils.parse(cleanedJson);

        // If parsed is a BMap, return it directly
        if (parsed instanceof BMap) {
            log.info("Map parameter received in direct JSON object format");
            return (BMap) parsed;
        }

        // If parsed is a BArray, check if it's table format: [{"key": "k1", "value": "v1"}, ...]
        if (parsed instanceof BArray) {
            BArray array = (BArray) parsed;
            log.info("Map parameter received as array, checking for table format. Array size: " + array.size());

            if (array.size() == 0) {
                log.info("Empty array received, returning empty map");
                return ValueCreator.createMapValue();
            }

            // Check if first element is a map with "key" field (table format - object array)
            Object firstElement = array.get(0);
            if (firstElement instanceof BMap) {
                BMap firstRow = (BMap) firstElement;
                if (firstRow.containsKey(StringUtils.fromString("key"))) {
                    log.info("Table format detected (has 'key' field). Transforming table array to map...");
                    // Transform table format to map: [{"key": k, "value": v}] -> {k: v}
                    return transformTableToMap(array);
                }
            }

            // Check if first element is an array (2D array format from MI Studio)
            if (firstElement instanceof BArray) {
                log.info("2D array format detected from MI Studio. Transforming to map...");

                // Extract parameter index from valueKey (e.g., "param0" -> 0)
                int paramIndex = -1;
                String indexStr = valueKey.replaceAll("\\D+", "");
                if (!indexStr.isEmpty()) {
                    try {
                        paramIndex = Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        log.warn("Could not extract parameter index from valueKey: " + valueKey);
                    }
                }

                // Get field names from context if this is map<Record>
                String[] fieldNames = null;
                if (paramIndex >= 0) {
                    Object fieldNamesObj = context.getProperty("mapRecordFields" + paramIndex);
                    if (fieldNamesObj != null) {
                        String fieldNamesStr = fieldNamesObj.toString();
                        fieldNames = fieldNamesStr.split(",");
                        log.info("Map record field names: " + fieldNamesStr);
                    }
                }

                // Transform 2D array format: [["k1", "v1"], ["k2", "v2"]] -> {k1: v1, k2: v2}
                return transform2DArrayToMap(array, fieldNames);
            }

            log.error("Unexpected format - first element type: " + firstElement.getClass().getSimpleName());
            throw new SynapseException("Map parameter array is not in table format (expected objects with 'key' field or 2D array)");
        }

        throw new SynapseException("Map parameter must be a JSON object or table array");
    }

    /**
     * Transform table array format to map.
     * Input: [{"key": "k1", "value": "v1"}, {"key": "k2", "value": "v2"}]
     * Output: {"k1": "v1", "k2": "v2"}
     *
     * For map<Record>, the structure is:
     * Input: [{"key": "k1", "field1": "f1", "field2": "f2"}, ...]
     * Output: {"k1": {"field1": "f1", "field2": "f2"}, ...}
     */
    private BMap transformTableToMap(BArray tableArray) {
        log.info("Starting table to map transformation. Input array size: " + tableArray.size());
        BMap resultMap = ValueCreator.createMapValue();
        BString keyFieldName = StringUtils.fromString("key");

        for (int i = 0; i < tableArray.size(); i++) {
            Object element = tableArray.get(i);
            if (element instanceof BMap) {
                BMap row = (BMap) element;

                // Extract the key
                Object keyObj = row.get(keyFieldName);
                if (keyObj == null) {
                    throw new SynapseException("Table row missing 'key' field at index " + i);
                }

                // Convert key to string
                String keyStr;
                if (keyObj instanceof BString) {
                    keyStr = ((BString) keyObj).getValue();
                } else {
                    keyStr = keyObj.toString();
                }
                BString key = StringUtils.fromString(keyStr);

                // Check if this is a simple map (has "value" field) or map<Record> (has other fields)
                if (row.size() == 2 && row.containsKey(StringUtils.fromString("value"))) {
                    // Simple map: extract value
                    Object value = row.get(StringUtils.fromString("value"));
                    log.info("Row " + i + ": Simple map entry - key='" + keyStr + "', value='" + value + "'");
                    resultMap.put(key, value);
                } else {
                    // map<Record>: create record object without the key field
                    log.info("Row " + i + ": map<Record> entry - key='" + keyStr + "', fields=" + (row.size() - 1));
                    BMap recordValue = ValueCreator.createMapValue();
                    for (Object rowKey : row.getKeys()) {
                        BString rowKeyStr = (BString) rowKey;
                        if (!rowKeyStr.getValue().equals("key")) {
                            recordValue.put(rowKeyStr, row.get(rowKeyStr));
                        }
                    }
                    resultMap.put(key, recordValue);
                }
            }
        }

        log.info("Table to map transformation complete. Result map size: " + resultMap.size());
        return resultMap;
    }

    /**
     * Transform 2D array format (from MI Studio) to map.
     * MI Studio sometimes serializes table data as 2D arrays instead of array of objects.
     *
     * For simple maps:
     * Input: [["k1", "v1"], ["k2", "v2"]]
     * Output: {"k1": "v1", "k2": "v2"}
     *
     * For map<Record>:
     * Input: [["k1", "f1", "f2"], ["k2", "f3", "f4"]] with fieldNames=["name", "value"]
     * Output: {"k1": {"name": "f1", "value": "f2"}, "k2": {"name": "f3", "value": "f4"}}
     *
     * @param array2D The 2D array from MI Studio
     * @param fieldNames Optional field names for map<Record>, null for simple maps
     */
    private BMap transform2DArrayToMap(BArray array2D, String[] fieldNames) {
        log.info("Starting 2D array to map transformation. Input array size: " + array2D.size());
        if (fieldNames != null) {
            log.info("Using field names: " + String.join(", ", fieldNames));
        }
        BMap resultMap = ValueCreator.createMapValue();

        for (int i = 0; i < array2D.size(); i++) {
            Object element = array2D.get(i);
            if (element instanceof BArray) {
                BArray row = (BArray) element;

                if (row.size() == 0) {
                    log.warn("Row " + i + " is empty, skipping");
                    continue;
                }

                // First element is always the key
                Object keyObj = row.get(0);
                String keyStr;
                if (keyObj instanceof BString) {
                    keyStr = ((BString) keyObj).getValue();
                } else {
                    keyStr = keyObj.toString();
                }
                BString key = StringUtils.fromString(keyStr);

                if (row.size() == 2) {
                    // Simple map: [key, value]
                    Object value = row.get(1);
                    log.info("Row " + i + ": Simple map entry - key='" + keyStr + "', value='" + value + "'");
                    resultMap.put(key, value);
                } else if (row.size() > 2) {
                    // map<Record>: [key, field1, field2, ...]
                    BMap recordValue = ValueCreator.createMapValue();
                    log.info("Row " + i + ": map<Record> entry - key='" + keyStr + "', fields=" + (row.size() - 1));

                    // Use provided field names or fall back to generic names
                    for (int j = 1; j < row.size(); j++) {
                        String fieldName;
                        if (fieldNames != null && (j - 1) < fieldNames.length) {
                            fieldName = fieldNames[j - 1];
                        } else {
                            fieldName = "field" + (j - 1);
                        }
                        Object fieldValue = row.get(j);
                        recordValue.put(StringUtils.fromString(fieldName), fieldValue);
                        log.info("  Field '" + fieldName + "' = " + fieldValue);
                    }
                    resultMap.put(key, recordValue);
                } else {
                    log.warn("Row " + i + " has only key, no value. Skipping.");
                }
            } else {
                log.warn("Row " + i + " is not an array, skipping. Type: " + element.getClass().getSimpleName());
            }
        }

        log.info("2D array to map transformation complete. Result map size: " + resultMap.size());
        return resultMap;
    }

    /**
     * Get array parameter from context and convert to Ballerina array.
     * Handles both direct JSON array format and table format.
     * Delegates to TypeConverter for the actual conversion logic.
     *
     * @param jsonArrayString JSON array string
     * @param context         Message context
     * @param valueKey        The property key like "param0" used to extract the parameter
     */
    private Object getArrayParameter(String jsonArrayString, MessageContext context, String valueKey) {
        // Extract parameter index from valueKey (e.g., "param0" -> 0)
        int paramIndex = -1;
        String indexStr = valueKey.replaceAll("\\D+", "");
        if (!indexStr.isEmpty()) {
            try {
                paramIndex = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                log.error("Invalid parameter index in valueKey: " + valueKey, e);
                return null;
            }
        } else {
            log.error("No digits found in valueKey: " + valueKey);
            return null;
        }

        // Get the array element type from context
        String elementType = context.getProperty("arrayElementType" + paramIndex).toString();
        log.info("Array parameter received. Element type: " + elementType);
        log.info("Array parameter JSON string (original): " + jsonArrayString);

        // Clean up invalid JSON (trailing commas from MI Studio table UI)
        String cleanedJson = cleanupJsonString(jsonArrayString);
        if (!cleanedJson.equals(jsonArrayString)) {
            log.info("JSON cleaned up (removed trailing commas): " + cleanedJson);
        }

        // For record arrays, parse and return directly (table format is already correct)
        if ("record".equals(elementType)) {
            log.info("Record array detected - using direct format (no transformation needed)");
            try {
                Object parsed = JsonUtils.parse(cleanedJson);
                log.info("Record array parsed successfully, type: " + (parsed != null ? parsed.getClass().getSimpleName() : "null"));
                if (parsed instanceof BArray) {
                    BArray array = (BArray) parsed;
                    log.info("Parsed record array size: " + array.size());
                }
                return parsed;
            } catch (Exception e) {
                log.error("Failed to parse record array JSON: " + e.getMessage(), e);
                throw new SynapseException("Failed to parse record array: " + e.getMessage(), e);
            }
        }

        // For simple type arrays, check if it's in table format and transform if needed
        Object parsed = JsonUtils.parse(cleanedJson);
        if (parsed instanceof BArray) {
            BArray array = (BArray) parsed;
            log.info("Array size: " + array.size());
            if (array.size() > 0) {
                Object firstElement = array.get(0);
                // If first element is a map with "value" field, it's table format
                if (firstElement instanceof BMap) {
                    BMap firstRow = (BMap) firstElement;
                    if (firstRow.containsKey(StringUtils.fromString("value")) && firstRow.size() == 1) {
                        log.info("Table format detected for simple array (has 'value' field). Transforming...");
                        // Transform table format to simple array: [{"value": "v1"}] -> ["v1"]
                        String transformedJson = transformTableArrayToSimpleArray(array);
                        log.info("Transformed JSON: " + transformedJson);
                        return TypeConverter.convertToArray(transformedJson, elementType);
                    } else {
                        log.info("Array contains objects but not in table format (field count: " + firstRow.size() + ")");
                    }
                } else {
                    log.info("Array already in direct format (elements are primitives)");
                }
            }
        }

        // Use shared TypeConverter for conversion (direct format)
        log.info("Using direct format conversion");
        try {
            Object result = TypeConverter.convertToArray(cleanedJson, elementType);
            log.info("TypeConverter.convertToArray completed successfully");
            return result;
        } catch (Exception e) {
            log.error("TypeConverter.convertToArray failed: " + e.getMessage(), e);
            throw new SynapseException("Failed to convert array: " + e.getMessage(), e);
        }
    }

    /**
     * Clean up JSON string to fix common issues from MI Studio table UI serialization.
     * - Removes trailing commas before closing brackets/braces: ,] or ,}
     *
     * @param json Original JSON string
     * @return Cleaned JSON string
     */
    private String cleanupJsonString(String json) {
        if (json == null) {
            return null;
        }
        // Remove trailing commas before ] or }
        // Pattern: comma followed by optional whitespace, then ] or }
        return json.replaceAll(",\\s*([\\]\\}])", "$1");
    }

    /**
     * Transform table array format to simple array for primitive types.
     * Input: [{"value": "v1"}, {"value": "v2"}, {"value": "v3"}]
     * Output: ["v1", "v2", "v3"]
     *
     * @param tableArray BArray in table format
     * @return JSON string of simple array
     */
    private String transformTableArrayToSimpleArray(BArray tableArray) {
        log.info("Starting table array to simple array transformation. Input size: " + tableArray.size());
        BString valueFieldName = StringUtils.fromString("value");
        StringBuilder jsonBuilder = new StringBuilder("[");

        for (int i = 0; i < tableArray.size(); i++) {
            if (i > 0) {
                jsonBuilder.append(",");
            }

            Object element = tableArray.get(i);
            if (element instanceof BMap) {
                BMap row = (BMap) element;
                Object value = row.get(valueFieldName);

                if (value == null) {
                    throw new SynapseException("Table row missing 'value' field at index " + i);
                }

                // Serialize the value
                String valueType = value.getClass().getSimpleName();
                if (value instanceof BString) {
                    // Escape and quote strings
                    String strValue = ((BString) value).getValue();
                    log.info("Row " + i + ": String value = '" + strValue + "'");
                    jsonBuilder.append("\"").append(strValue.replace("\"", "\\\"")).append("\"");
                } else if (value instanceof Boolean || value instanceof Number) {
                    // Primitives don't need quotes
                    log.info("Row " + i + ": " + valueType + " value = " + value);
                    jsonBuilder.append(value.toString());
                } else {
                    // For other types, use toString
                    log.info("Row " + i + ": " + valueType + " value (toString) = '" + value.toString() + "'");
                    jsonBuilder.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                }
            }
        }

        jsonBuilder.append("]");
        String result = jsonBuilder.toString();
        log.info("Table array transformation complete. Result: " + result);
        return result;
    }

    private static String getResultProperty(MessageContext context) {
        return lookupTemplateParameter(context, Constants.RESPONSE_VARIABLE).toString();
    }

    private static boolean isOverwriteBody(MessageContext context) {
        return Boolean.parseBoolean((String) lookupTemplateParameter(context, Constants.OVERWRITE_BODY));
    }
}
