package com.kineticdata.bridgehub.adapter.autopilot;

import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutopilotTest extends BridgeAdapterTestBase{
    
    private static final Logger LOGGER = 
        LoggerFactory.getLogger(AutopilotTest.class);
        
    @Override
    public Class getAdapterClass() {
        return AutopilotAdapter.class;
    }
    
    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config.yml";
    }
 
    /*
        Test count method
    */
    @Override
    @Test
    public void test_emptyCount() throws Exception {
        assertTrue(true);
    }
    
    /*
        Test retrieve method
    */
    @Override
    @Test
    public void test_emptyRetrieve() throws Exception {
        assertTrue(true);
    }
    
    /*
        Test search method
    */
    @Override
    @Test
    public void test_emptySearch() throws Exception {
        assertTrue(true);
    }
    
    @Test
    public void test_count() throws Exception{
        BridgeError error = null;

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(new ArrayList<>());
        request.setQuery("");
        request.setParameters(new HashMap());
        
        Count count = null;
        try {
            count = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        // Test Adhoc count query
        assertNull(error);
        assertTrue(count.getValue() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/contacts");
        
        Count adhocCount = null;
        try {
            adhocCount = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(count.getValue().equals(adhocCount.getValue()));
    }
    
    @Test
    public void test_retrieve() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("Email");
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("contact_id=<%=parameter[\"Id\"]%>");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "dmcclure@us.ibm.com");
        }});        
        
        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(record.getRecord().size() > 0);
    }
    
    @Test
    public void test_retrieve_nested_search() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("Email");
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts > List");
        request.setFields(fields);
        request.setQuery("list_id=<%=parameter[\"Id\"]%>");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "contactlist_05068200-83ef-455c-a0f3-e61a4292a465");
        }});        
        
        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(record.getRecord().size() > 0);
    }
    
    @Test
    public void test_adhoc_retrieve() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("Email");
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("contact_id=<%=parameter[\"Id\"]%>");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Id", "dmcclure@us.ibm.com");
        }});
        
        
        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(record.getRecord().size() > 0);

        // Test Adhoc query
        request.setStructure("Adhoc");
        request.setQuery("/contact/<%=parameter[\"Id\"]%>");
        
        
        Record adhocRecord = null;
        try {
            adhocRecord = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertEquals(record.getRecord(),adhocRecord.getRecord());
    }
    
    @Test
    public void test_search() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("Email");
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("");
        request.setParameters(new HashMap<String, String>());
                
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/contacts?accessor=<%=parameter[\"Accessor\"]%>");
        
        request.setParameters(new HashMap<String, String>() {{ 
            put("Accessor", "contacts");
        }});
                
        RecordList adhocRecords = null;
        try {
            adhocRecords = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(adhocRecords.getRecords().size() == records.getRecords().size());
    }
    
    @Test
    public void test_search_json_path() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("Email");
        fields.add("custom_fields");
        fields.add("$.custom_fields[*].kind");
        fields.add("$.lists");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("");
        
        request.setParameters(new HashMap<String, String>());
        
                
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
    
    @Test
    public void test_search_sort() throws Exception{
        BridgeError error = null;
        
       // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("FirstName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("");
        request.setParameters(new HashMap<String, String>());
        
        request.setMetadata(new HashMap<String, String>() {{
            put("order", "<%=field[\"FirstName\"]%>:DESC");
        }});
        
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
            LOGGER.error("Error: ", e);
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
   
    @Test
    public void test_search_sort_multiple() throws Exception{
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);
        request.setQuery("");
        
        Map <String, String> metadata = new HashMap<>();
        metadata.put("order", ""
            + "<%=field[\"FirstName\"]%>:DESC,<%=field[\"LastName\"]%>:DESC");       
        request.setMetadata(metadata);
        
        RecordList records = null;
        try {
            records = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
            LOGGER.error("Error: ", e);
        }
        
        assertNull(error);
        assertTrue(records.getRecords().size() > 0);
    }
}