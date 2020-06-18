# Kinetic Bridgehub Adapter Autopilot

This Rest based bridge adapter was designed to work with Autopilot services "api2" rest api.
___
## Adapter Configurations
Name | Description
------------ | -------------
Token | The application token
___
## Supported structures
* Contacts
___
## Example Qualification Mapping
* id=test@slarty.com
___
## Notes
* To get a single contact provide id=${id}
___
### Developer notes
* All endpoints are conforming to JSON object except Custom Fields which returns
a JSON array.  Care must be taken if Custom Fields is ever an implemented Structure.
* Pagination for contacts requires a token to be appended to the Contacts url.
