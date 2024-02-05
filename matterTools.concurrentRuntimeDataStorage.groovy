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
		version: "0.5.0"
)
import java.util.concurrent.* 
import groovy.transform.Field
import groovy.json.JsonBuilder

@Field static ConcurrentHashMap globalDataStorage = new ConcurrentHashMap(32, 0.75, 1) // Intended to Store info. that does not change. Default is static

/* Following function is placed  after the driver parse routine's parseDescriptionAsMap to store most receent retrieved attributes so you can use when needed.
Typical usage:
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16)))
        storeRetrievedData(descMap)
*/
void storeRetrievedData(Map descMap){
    globalDataStorage.get(device.getDeviceNetworkId(), new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
        .get(descMap.endpointInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
            .get(descMap.clusterInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
                .put(descMap.attrInt, descMap.value)
}

// A utility function to pretty-print all stored data in json form. Useful for debugging
void showStoredAttributeData(){
    Map storedData = globalDataStorage.get(device.getDeviceNetworkId())
    log.info "<br><pre> ${ new JsonBuilder(storedData)?.toPrettyString()}"
}

// Following function retrieves the last-stored data for a particular attribute.
def getStoredAttributeData(Map params = [:] ){
    try { 
        Map inputs = [endpointInt:null, clusterInt:null, attrInt:null]  << params
        if (inputs.ep && (inputs.ep instanceof Integer) && (inputs.endpointInt.is(null)) ) inputs.endpointInt = inputs.ep // ep is often used in other functions, so accept that too! 
        assert inputs.endpointInt instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.attrInt instanceof Integer
        
        return globalDataStorage.get(device.getDeviceNetworkId()) // First, get the data sub-Map for this specific node using deviceNetworkId
            ?.get(inputs.endpointInt)
                ?.get(inputs.clusterInt)
                    ?.get(inputs.attrInt)       
        
    } catch(AssertionError e) {
        log.error "Assertion error in function getStoredAttributeData: <pre>${e}"
        log.error getStackTrace(e)
        throw(e)
    } catch(e) {
        log.error "Caught error in function getStoredAttributeData: <pre>${e}"
        log.error getStackTrace(e)
        throw(e)
    }
}