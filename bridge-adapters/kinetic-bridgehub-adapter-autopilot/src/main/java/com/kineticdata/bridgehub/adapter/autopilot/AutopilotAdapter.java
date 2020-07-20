package com.kineticdata.bridgehub.adapter.autopilot;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutopilotAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    public AutopilotAdapter () {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user
        this.parser = new AutopilotQualificationParser();
    }
    
    /*----------------------------------------------------------------------------------------------
     * STRUCTURES
     *      AdapterMapping( Structure Name, Path Function)
     *--------------------------------------------------------------------------------------------*/
    public static Map<String,AdapterMapping> MAPPINGS 
        = new HashMap<String,AdapterMapping>() {{
        put("Contacts", new AdapterMapping("Contacts", "contacts",
            AutopilotAdapter::pathContacts));
        put("Lists", new AdapterMapping("Lists", "lists",
            AutopilotAdapter::pathLists));
        put("Adhoc", new AdapterMapping("Adhoc", "_noOp_",
            AutopilotAdapter::pathAdhoc));
    }};
    
    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Autopilot Bridge";

    /** Defines the LOGGER */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AutopilotAdapter.class);
    
    /** Adapter version constant. */
    public static String VERSION = "";
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load(AutopilotAdapter.class.getResourceAsStream("/"+AutopilotAdapter.class.getName()+".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.warn("Unable to load "+AutopilotAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_TOKEN = "Token";
    }

    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.PROPERTY_TOKEN)
            .setIsRequired(true).setIsSensitive(true)
    );

    // Local variables to store the property values in
    private final AutopilotQualificationParser parser;
    private AutopilotApiHelper apiHelper;
    
    // Constants
    private static final String API_PATH = "https://api2.autopilothq.com/v1";

    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        String token = properties.getValue(Properties.PROPERTY_TOKEN);
        
        apiHelper = new AutopilotApiHelper(API_PATH, token);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
       return VERSION;
    }

    @Override
    public void setProperties(Map<String,String> parameters) {
        // This should always be the same unless there are special circumstances
        // for changing it
        properties.setValues(parameters);
    }

    @Override
    public ConfigurablePropertyMap getProperties() {
        // This should always be the same unless there are special circumstances
        // for changing it
        return properties;
    }

    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/
    @Override
    public Count count(BridgeRequest request) throws BridgeError {        
        // Log the access
        LOGGER.trace("Counting records");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());

        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, 
            parameterMap));
        
        Long count;
        if (responseObject.containsKey("total_contacts")) {
            count = (Long)responseObject.get("total_contacts");
        } else {
            count = Long.valueOf(getResponseData(responseObject).size());
        }
        
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Retrieving Record");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());
        LOGGER.trace("  Fields: " + request.getFieldString());
        
        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, 
            parameterMap));

        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }

        Record record = new Record();
        if (responseArray.size() == 1) {
            // Reassign object to single result 
            JSONObject object = (JSONObject)responseArray.get(0);
                
            List<String> fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), object);
            record = buildRecord(fields, object);
        } else if (responseArray.size() == 0) {
            LOGGER.debug("No results found for query: {}", request.getQuery());
        } else {
            throw new BridgeError ("Retrieve must return a single result."
                + " Multiple results found.");
        }
        
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Searching Records");
        LOGGER.trace("  Structure: " + request.getStructure());
        LOGGER.trace("  Query: " + request.getQuery());
        LOGGER.trace("  Fields: " + request.getFieldString());

        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));

        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        Map<String, String> metadata = request.getMetadata() != null ?
                request.getMetadata() : new HashMap<>();

        LinkedHashMap<String,String> sortOrderItems = null; 
        // adapter side sorting requires an order be set by request
        if (request.getMetadata("order") != null) {
            sortOrderItems = getSortOrderItems(
                BridgeUtils.parseOrder(request.getMetadata("order")));
        }
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        // Convert the parameters map into a map of NameValuePairs for use in
        // building the request url
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, 
            parameterMap));
        
        // Bookmark is autopilot pagination strategy
        if (responseObject.containsKey("bookmark")) {
            metadata.put("bookmark", (String)responseObject.get("bookmark"));
        }
        

        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }

        // Create a List of records that will be used to make a RecordList object
        List<Record> recordList = new ArrayList<>();
        List<String> fields = request.getFields() == null ? new ArrayList() : 
            request.getFields();        
        if(responseArray != null && responseArray.isEmpty() != true){
            fields = getFields(fields, (JSONObject)responseArray.get(0));

            // Iterate through the responce objects and make a new Record for each.
            for (Object o : responseArray) {
                JSONObject obj = (JSONObject)o;
                Record record = buildRecord(fields, obj);
                
                // Add the created record to the list of records
                recordList.add(record);
            }
        }
        
        if (request.getMetadata("order") != null) {
            // Sort the result set
            AutopilotComparator comparator = 
                new AutopilotComparator(sortOrderItems);
            Collections.sort(recordList, comparator);
        }
        
        return new RecordList(fields, recordList, metadata);
    }

    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
        protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        // if no fields were provided then all fields will be returned. 
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        
        return fields;
    }
    
    /**
     * Build a Record.  If no fields are provided all fields will be returned.
     * 
     * @param fields
     * @param jsonobj
     * @return Record
     */
    protected Record buildRecord (List<String> fields, JSONObject jsonobj) {
        JSONObject obj = new JSONObject();
        DocumentContext jsonContext = JsonPath.parse(jsonobj); 
        
        fields.stream().forEach(field -> {
            // either use JsonPath or just add the field value.  We're assuming
            // all JsonPath usages will begin with $[ or $.. 
            if (field.startsWith("$.") || field.startsWith("$[")) {
                try {
                    obj.put(field, jsonContext.read(field));
                } catch (JsonPathException e) {
                    // if field is a valid path but object is missing the property
                    // return null for field.  This is consistent with existing 
                    // adapter behavior.
                    if (e.getMessage().startsWith("Missing property")) {
                        obj.put(field, null);
                        LOGGER.debug(String.format("%s was not found, returning"
                            + " null value", field), e);
                    } else {   
                        throw new JsonPathException(String.format("There was an issue"
                            + " reading %s", field), e);
                    }
                }
            } else {
                obj.put(field, jsonobj.get(field));
            }
        });
        
        Record record = new Record(obj, fields);
        return record;
    }
    
    /**
     * Get accessor value. If structure is Adhoc remove accessor from parameters.
     * 
     * @param mapping
     * @param parameters
     * @return 
     */
    private String getAccessor(AdapterMapping mapping, Map<String, String> parameters) {
        String accessor;
        
        if (mapping.getStructure().equals("Adhoc")) {
            accessor = parameters.get("accessor");
            parameters.remove("accessor");
        } else {
            accessor = mapping.getAccessor();
        }
        
        return accessor;
    }
    
    protected JSONArray getResponseData(Object responseData) {
        JSONArray responseArray = new JSONArray();
        
        if (responseData instanceof JSONArray) {
            responseArray = (JSONArray)responseData;
        }
        else if (responseData instanceof JSONObject) {
            // It's an object
            responseArray.add((JSONObject)responseData);
        }
        
        return responseArray;
    }
    
    /**
     * This helper is required due to the Adhoc structure having a different 
     * qualification mapping pattern. 
     * 
     * @param request
     * @param mapping
     * @return
     * @throws BridgeError 
     */
    protected Map<String, String> getParameters(String query,  
        AdapterMapping mapping) throws BridgeError {
        
        Map<String, String> parameters = new HashMap<>();
        if (mapping.getStructure() == "Adhoc") {
            // Adhoc qualifications are two segments. ie path?queryParameters
            String [] segments = query.split("[?]",2);

            // getParameters only needs the queryParameters segment
            if (segments.length > 1) {
                parameters = parser.getParameters(segments[1]);
            }
            // Pass the path along to the functional operator
            parameters.put("adapterPath", segments[0]);
        } else {
            parameters = parser.getParameters(query);
        }
        
        return parameters;
    }
        
    /**
     * This method checks that the structure on the request matches on in the 
     * Mapping internal class.  Mappings map directly to the adapters supported 
     * Structures.  
     * 
     * @param structure
     * @return Mapping
     * @throws BridgeError 
     */
    protected AdapterMapping getMapping (String structure) throws BridgeError{
        AdapterMapping mapping = MAPPINGS.get(structure);
        if (mapping == null) {
            throw new BridgeError("Invalid Structure: '" 
                + structure + "' is not a valid structure");
        }
        return mapping;
    }
    
    /**
     * Convert parameters from a String to a NameValuePair for use with building
     * the URL parameters
     * 
     * @param parameters
     * @return Map<String, NameValuePair>
     */
    protected Map<String, NameValuePair> buildNameValuePairMap(
        Map<String, String> parameters) {
        
        Map<String, NameValuePair> parameterMap = new HashMap<>();

        parameters.forEach((key, value) -> {
            parameterMap.put(key, new BasicNameValuePair(key, value));
        });
        
        return parameterMap;
    }
    
    /**
     * Build URL to be used when making request to the source system.
     * 
     * @param path
     * @param parameters
     * @return String
     */
    protected String getUrl(String path, Map<String, NameValuePair> parameters) {
        
        return String.format("%s?%s", path, 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
    }
 
    /**
     * Take the sort order from metadata and add it to parameters for use with
     * request.
     * 
     * @param order
     * @return
     * @throws BridgeError 
     */
    protected String addSort(String order) throws BridgeError {
        
        LinkedHashMap<String,String> sortOrderItems = getSortOrderItems(
                BridgeUtils.parseOrder(order));
        String sortOrderString = sortOrderItems.entrySet().stream().map(entry -> {
            return entry.getKey() + " " + entry.getValue().toLowerCase();
        }).collect(Collectors.joining(","));
                    
        LOGGER.trace("Adding $orderby parameter because form has order "
            + "feilds \"{}\" defined", sortOrderString);
        return sortOrderString;
    }
    
    /**
     * Create a set of keys to remove from object prior to creating Record.
     * 
     * @param fields
     * @param obj
     * @return 
     */
    protected Set<Object> buildKeySet(List<String> fields, JSONObject obj) {
        if(fields.isEmpty()){
            fields.addAll(obj.keySet());
        }
            
        // If specific fields were specified then we remove all of the 
        // nonspecified properties from the object.
        Set<Object> removeKeySet = new HashSet<>();
        for(Object key: obj.keySet()){
            if(!fields.contains(key)){
                LOGGER.trace("Remove Key: "+key);
                removeKeySet.add(key);
            }
        }
        return removeKeySet;
    }
    
    /**
     * Ensure that the sort order list is linked so that order can not be changed.
     * 
     * @param uncastSortOrderItems
     * @return
     * @throws IllegalArgumentException 
     */
    private LinkedHashMap<String, String> 
        getSortOrderItems (Map<String, String> uncastSortOrderItems)
        throws IllegalArgumentException{
        
        /* results of parseOrder does not allow for a structure that 
         * guarantees order.  Casting is required to preserver order.
         */
        if (!(uncastSortOrderItems instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("Sort Order Items was invalid.");
        }
        
        return (LinkedHashMap)uncastSortOrderItems;
    }
    
    /**************************** Path Definitions ****************************/
    /**
     * Build the path for the Contacts structure.
     * 
     * @param structureList
     * @param parameters
     * @return 
     * @throws com.kineticdata.bridgehub.adapter.BridgeError 
     */
    protected static String pathContacts(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path= new String();
        if (structureList.size() == 1) {
            if (parameters.containsKey("contact_id")) {
                path = String.format("%s/%s", "/contact", parameters.get("contact_id"));
                parameters.remove("contact_id");
            } else {
                path = "/contacts";
                                
                if (parameters.containsKey("bookmark")) {
                    path = String.format("%s/%s", path, parameters.get("bookmark"));
                    parameters.remove("bookmark");
                }
            }
        } else if (structureList.contains("List")) {
            if (!parameters.containsKey("list_id")) {
                throw new BridgeError(String.format("The %s structure requires "
                + "list_id parameter.", String.join(" > ", structureList)));
            }
            path = String.format("/list/%s/contacts", parameters.get("list_id"));
            parameters.remove("list_id");
        }

        return path;
    }
    
    /**
     * Build the path for the Contacts structure.
     * 
     * @param structureList
     * @param parameters
     * @return 
     * @throws com.kineticdata.bridgehub.adapter.BridgeError 
     */
    protected static String pathLists(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path = "/lists";

        return path;
    }
    
    /**
     * Build path for Adhoc structure.
     * 
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError 
     */
    protected static String pathAdhoc(List<String> structureList, 
        Map<String, String> parameters) throws BridgeError {
        
        String path = parameters.get("adapterPath"); 
        parameters.remove("adapterPath");
        return path;
    }

    /**
     * Checks if a parameter exists in the parameters Map.
     * 
     * @param param
     * @param parameters
     * @param structureList
     * @throws BridgeError 
     */
    protected static void checkRequiredParamForStruct(String param,
        Map<String, String> parameters, List<String> structureList)
        throws BridgeError{
        
        if (!parameters.containsKey(param)) {
            String structure = String.join(" > ", structureList);
            throw new BridgeError(String.format("The %s structure requires %s "
                + "parameter.", structure, param));
        }
    }
}
