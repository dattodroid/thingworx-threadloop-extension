> [!CAUTION]
> This extension was created for educational purposes and has undergone no testing. It is neither supported nor endorsed by PTC.

# Thread Loop extension

This extension allows a service to be executed in its own thread on each node of a ThingWorx HA cluster.
It can be useful to collect metrics or other information that are node specific at a given interval (bypass the singleton node and event processor).

## Usage:

1. Import the thingworx-threadloop-extension in ThingWorx
2. Create a Thing that extends `ThreadloopTemplate` (or just import Demo/Things_SampleThreadloop.xml)
    1. Override the `Run` service with your code. The minimal implementation should be:
      ```js
      /*
        Your code here.
        You can pass information to the next loop via data/result.
        data.count = 123;
      */
      result = data;
      ```
      * Input parameters:
        * `platformId`: name of the platform node on which the service is running
        * `effectivePause`: effective pause (can be useful to identify long JVM paused)
        * `data`: JSON returned by previous execution of the service (node local - If you want to share information between nodes, use a Thing property).

      2. Grant the **Service Execute** permission on `Run` service to **System** user.
      3. Adjust the execution interval using the `pause` setting on the Configuration page (default is 30sec).