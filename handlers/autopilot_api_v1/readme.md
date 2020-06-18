## Autopilot API V1
This handler is used to interact with the Autopilot api

### Parameters
[Error Handling]
  Select between returning an error message, or raising an exception.
[Method]
  HTTP Method to use for the Autopilot API call being made.
  Options are:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
[Path]
  The relative API path, to the `https://api2.autopilothq.com/v1`, that will be called.
  This value should begin with a forward slash `/`.
[Body]
  The body content (JSON) that will be sent for POST, PUT, and PATCH requests.

### Results
[Response Body]
  The returned value from the Rest Call (JSON format)
