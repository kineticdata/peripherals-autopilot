# Kinetic Bridgehub Adapter Autopilot

This Rest based bridge adapter was designed to work with Autopilot services "api2" rest api.
___
## Adapter Configurations
Name | Description
------------ | -------------
Token | The application token
___
## Supported structures
Structure | Parameter * required
------------ | -------------
Contacts  | contact_id
Contacts > List | * list_id
Lists | 
Adhoc | * accessor
___
## Example Qualification Mapping
* contact_id=test@slarty.com
___
## Notes
* [JsonPath](https://github.com/json-path/JsonPath#path-examples) can be used to access nested values. The root of the path is values.
* To get a single contact provide contact_id=${id}
___
### Developer notes
* All endpoints are conforming to JSON object except Custom Fields which returns
a JSON array.  Care must be taken if Custom Fields is ever an implemented Structure.
* Pagination for contacts requires a token to be appended to the Contacts url.
