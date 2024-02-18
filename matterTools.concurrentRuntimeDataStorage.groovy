/* 
Library to store runtime generate data in a concurrency-safe manner.
This is primarily used for storing cluster and attribute data.
A concurrentHashMap is used to avoid conflicting writes where multiple attributes may be reported from a node and get processed simultaneously
In newer Hubitat drivers, could also use atomicState writes, but this can be faster

Library assumes that descMap also includes the endpointId as an integer (descMap.endpointIdInt). 
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16)))
See matterTools.commonDriverMethods library for example
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Methods Common to Matter Drivers",
        name: "concurrentRuntimeDataStorage",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.0"
)
import java.util.concurrent.* 
import groovy.transform.Field
import groovy.json.JsonBuilder

/*
The globalDataStorage variable is the "root' of the data structure used in this library
Since this is an @Field static variable (shared by all devices that use the driver), the first level of 'get' into this
variable will be a device's unique network ID. The next level is where the current "device" really starts and begins with a map of one or more endpoint IDs as keys.
After tha, for each endpoint, there is another map keyed by cluster IDs and for each cluster ID, a map keyed by attribute and then the value for the attribute.

The resultant structure is a Map as follows
globalDataStorage = [netID#1:[EndpointID#0:[ClusterID#1:[AttributeID#1:value, AttributeID#2:value, etc.], ClusterID#2:[... attribute ID structure ...]], EndpointID#1[repeat sub-structure as per #0]:

globalDataStorage = 
    [  netID#1:
            EndpointID#0:[
                clusterID#1:[attributeID#1:value, AttributeID#2:value, etc],
                clusterID#2:[attributeID#1:value, AttributeID#2:value, etc],               
                (repeat Cluster ID structure as needed for other clusters)
                        ],
            EndpointID#1:[
                clusterID#1:[attributeID#1:value, AttributeID#2:value, etc],
                clusterID#2:[attributeID#1:value, AttributeID#2:value, etc],               
                (repeat Cluster ID structure as needed for other clusters)
                        ],
            (and repeat Endpoint ID structure for other endpoints as needed)
    ],
      netID #2, 3, 4, etc.:
            (repeat as per above)
    ]

But you don't actually create this structure, its done by the storeRetrievedData call, below!
*/
@Field static ConcurrentHashMap globalDataStorage = new ConcurrentHashMap(16, 0.75, 1)

/* Following function is placed  after the driver parse routine's parseDescriptionAsMap to store most receent retrieved attributes so you can use when needed.
Typical usage:
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16)))
        storeRetrievedData(descMap)
*/

void storeRetrievedData(Map descMap){
    globalDataStorage.get(device.getDeviceNetworkId(), new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this Network ID or create a blank of one doesn't exist
        .get(descMap.endpointInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this Endpoint or create a blank of one doesn't exist
            .get(descMap.clusterInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this cluster or create a blank of one doesn't exist
                .put(descMap.attrInt, descMap.value) // And then put the attribute value into the map for this attribute / cluster / endpoint / network ID
}

// Following function retrieves the last-stored data for a particular attribute.
def getStoredAttributeData(Map params = [:] ){
    try { 
        Map inputs = [endpointInt:null, ep:null, clusterInt:null, attrInt:null]  << params
        assert ( inputs.ep.is(null) || inputs.endpointInt.is(null))
        
        if ( (!inputs.ep.is(null)) && (inputs.ep instanceof Integer) ) inputs.endpointInt = inputs.ep // if ep label was used, copy ep to endpointInt! 
        assert inputs.endpointInt instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.attrInt instanceof Integer
        
        return globalDataStorage.get(device.getDeviceNetworkId()) // First, get the data sub-Map for this specific node using deviceNetworkId
            ?.get(inputs.endpointInt)
                ?.get(inputs.clusterInt)
                    ?.get(inputs.attrInt)       
        
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

Map getStoredDeviceData() { return  globalDataStorage.get(device.getDeviceNetworkId()) }

// Returns the device data as JSON. See JsonBuilder class: https://docs.groovy-lang.org/latest/html/gapi/groovy/json/JsonBuilder.html
JsonBuilder getStoredDeviceDataAsJSON(){ return new JsonBuilder( getStoredDeviceData() ) }

/*
A utility function to pretty-print all stored data in json form. Useful for debugging
Endpoint, Cluster, and Attribute Map Keys are in Integer format, so, for example, 
a "10" in as map key means ten, not sixteen.

The values for the attributes themselves, however, are as-received from the Hubitat parse routine.
So "10" in the attribute's value field may be hexidecimal meaning sixteen if Hubitat parsed it into hex form, 
or likewise, could be an Integer meaning ten if Hubitat supplied it as an Integer. 
This is a case where you need to know what Hubitat has done!
*/
void prettyPrintStoredAttributeData(){
    log.info "<br><pre>${ getStoredDeviceDataAsJSON()?.toPrettyString() }"
}