library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Create Hubitat Events from Matter Generic Switch Reports",
        name: "genericSwitchMethods0x003B",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import java.util.concurrent.* 
import groovy.transform.Field

/*
When first subscribing to events, the Matter node will "dump" a prior event history. Its important not to convert this first set of node events into Hubitat Events that would then trigger automation rules.
To address this, the library stores the last-received event and does a simple check when a new event arrives: 
(a) when each new event arrives, check globalEventDataStorage to see if there was a prior event of the same type;
(b) if not, i.e., prior event is a null, then this is the initial "dump" - just store it but don't generate a Hubitat event
(c) if a prior event of the same type was stored in globalEventDataStorage, this is a "real" event, not a history "dump", so process and convert to a Hubitat Event.

This storage is handled similar to how the concurrentRuntimeDataStorage library stores attribute data.

The globalEventDataStorage variable is the "root' of the data structure used in this library
Since this is an @Field static variable (shared by all devices that use the driver), the first level of 'get' into this
variable will be a device's unique network ID. The next level is where the current "device" really starts and begins with a map of one or more endpoint IDs as keys.
After that, for each endpoint, there is another map keyed by cluster IDs and for each cluster ID, a map keyed by event and then the value for the event.

The resultant structure is a Map as follows

globalEventDataStorage = 
    [  netID#1:
            EndpointID#0:[
                clusterID#1:[eventID#1:value, eventID#2:value, etc],
                clusterID#2:[eventID#1:value, eventID#2:value, etc],               
                (repeat Cluster ID structure as needed for other clusters)
                        ],
            EndpointID#1:[
                clusterID#1:[eventID#1:value, eventID#2:value, etc],
                clusterID#2:[eventID#1:value, eventID#2:value, etc],               
                (repeat Cluster ID structure as needed for other clusters)
                        ],
            (and repeat Endpoint ID structure for other endpoints as needed)
    ],
      netID #2, 3, 4, etc.:
            (repeat as per above)
    ]

But you don't actually create this structure, its done by the storeEventData call, below!
*/
@Field static ConcurrentHashMap globalEventDataStorage = new ConcurrentHashMap(16, 0.75, 1)

void storeEventData(Map decodedNodeReportMap){
    def valueToStore
    if (decodedNodeReportMap.containsKey("decodedValue")) { // This is for use with my custom parser that produced fully decoded values
        valueToStore = decodedNodeReportMap.decodedValue
    } else if (decodedNodeReportMap.containsKey("value")) {
        valueToStore = decodedNodeReportMap.value
    } 
    if (valueToStore.is(null)) return // Java ConcurrentHashMaps can't store null values!
    globalDataStorage.get(device.getDeviceNetworkId(), new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this Network ID or create a blank of one doesn't exist
        .get(decodedNodeReportMap.endpointInt, new ConcurrentHashMap<String,ConcurrentHashMap>(4, 0.75, 1)) // Get Map for this Endpoint or create a blank of one doesn't exist
            .get(decodedNodeReportMap.clusterInt, new ConcurrentHashMap<String,ConcurrentHashMap>(4, 0.75, 1)) // Get Map for this cluster or create a blank of one doesn't exist
                .put(decodedNodeReportMap.evtInt, valueToStore) // And then put the event value into the map for this event / cluster / endpoint / network ID
}

// Following function retrieves the last-stored data for a particular event.
def getStoredEventData(Map inputs = [:] ){
    // the inputs map will generally be a decodedNodeReportMap produced by the parseDescriptionAsDecodedMap library
    try { 
        assert inputs.endpointInt instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.evtInt instanceof Integer
        
        if (device.is(null)) return // can be Null if called from Metadata
        return globalDataStorage.get(device.getDeviceNetworkId()) // First, get the data sub-Map for this specific node using deviceNetworkId
            ?.get(inputs.endpointInt)
                ?.get(inputs.clusterInt)
                    ?.get(inputs.evtInt)       
        
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing getStoredEventData with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

Map getStoredDeviceEventData() { return  globalDataStorage.get(device.getDeviceNetworkId()) }

void clearStoredDeviceEventData() { globalDataStorage.get(device.getDeviceNetworkId())?.clear() }

/* ******* Done with Section of Library that stores Event Data ******** */

List getHubitatEventsFromGenericSwitchEventReport(Map decodedNodeReportMap, Map endpointToButtonMap) {
    List<Map> rEvents = []
    try {
        assert decodedNodeReportMap.clusterInt == 0x003B
        
        // if this is the first time the event type was received, then this is the data "dump" that follows the subscription
        // just store that and return
        if (getStoredEventData(decodedNodeReportMap).is(null) ) {
            log.debug "First time event was received. Storing it and returning. Event data is ${decodedNodeReportMap}"
            storeEventData(decodedNodeReportMap)
            return
        } else { 
            storeEventData(decodedNodeReportMap)
        }
        
        Map rValue = [:]
        Integer button = endpointToButtonMap.get(decodedNodeReportMap.endpointInt)
        
 
        switch (decodedNodeReportMap.evtInt) {
            // Details of these event values are set out in Section 1.12.6- 1.12.8 of the Matter Application Cluster Specification Version 1.2
            case 0x00: // SwitchLatched
                log.warn "Received a SwitchLatched Event with information: ${decodedNodeReportMap}. SwitchLatched is currently not supported."
                break
            case 0x01: // InitialPress. 
                // This event is only relevent if the Matter FeatureMap flag for the cluster has the MSM bit off meaning the device only supports single-press. 
                // Current devices that support button presses all support 2 or more presses, so use event 0x06 instead. 
                // Note that the InitialPress event occurs on each button tap. So a triple-tap would include 3 InitialPresses (that can all be ignored).
                if (logEnable) log.debug "Received a InitialPress Event with information: ${decodedNodeReportMap} for position ${decodedNodeReportMap.decodedValue.get(0)}"
                break
            case 0x02: // LongPress
                rValue = [name:"held", value: button, isStateChange:true]
                break
            case 0x03: // ShortRelease. Analogous to InitialPress, this occurs after each buttontap and can be safely ignored.
                if (logEnable) log.debug "Received a ShortRelease Event with information: ${decodedNodeReportMap}"
                break
            case 0x04: // LongRelease
                rValue = [name:"released", value: button, isStateChange:true]
                break
            case 0x05: // MultiPressOngoing.
                // This event can be ignored. It occurs after each button press with the MSM FeatureMap bit is set, but the real event of interest is event 0x06. 
                if (logEnable) log.debug "Received a MultiPressOngoing Event with information: ${decodedNodeReportMap}"
                break
            case 0x06: // MultiPressComplete
                Integer numberOfPresses = decodedNodeReportMap.decodedValue.get(1)
                switch(numberOfPresses) {
                    case 1:
                        rValue = [name:"pushed", value: button, isStateChange:true]
                         break
                    case 2:
                        rValue = [name:"doubleTapped", value: button, isStateChange:true]
                         break
                    default:
                        if (logEnable) log.warn "Received ${numberOfPresses} presses on button ${button}. Hubitat only supports up to 2 presses."
                }
                break
       
        }
        if (rValue.containsKey("name")) {
            // As a future additon, add the descriptionText key and include text with the endpoint label (up, down, config, etc.)
            // rValue << [descriptionText: "Button event for button ${button}, with label ${}"] 
            rEvents.add(rValue)
        }
        return rEvents
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing getHubitatEventsFromGenericSwitchEventReport inputs ${decodedNodeReportMap}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

