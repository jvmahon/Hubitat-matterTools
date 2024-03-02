import groovy.transform.Field
import hubitat.matter.DataType

#include matterTools.matterEnumTypes
#include matterTools.endpointAndChildDeviceTools // Tools for creation of child devices
#include matterTools.identifyClusterMethods0x0003 // Identify methods supporting named parameters, endpoints, and child devices
#include matterTools.OnOffClusterMethods0x0006 // On/Off cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.levelClusterMethods0x0008 // Level cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.ColorClusterMethods0x0300 // Color Cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.createListOfMatterSendEventMaps // Converts the data from parseAsMap to Hubitat event form
// #include matterTools.commonDriverMethods // Main body of the driver, including parse handling and event distribution
#include matterTools.matterHelperUtilities
#include matterTools.concurrentRuntimeDataStorage
#include matterTools.parseDescriptionAsDecodedMap

metadata {
    definition (name: "Matter Universal Driver", namespace: "matterTools", author: "jvm33") {
        capability "Configuration"		
		capability "Initialize"
		capability "Refresh"
        
        command "testLimitedRefresh"
        
        command "identify", [[name: "Identify", type:"NUMBER", description:"Put device into Identify mode"]]
        command "recreateChildDevices" // For debugging purposes
        command "createEveMotionChildDevices" // For debugging purposes
        command "createEveEnergyOutletDevice" // For debugging purposes

        command "unsubscribeAll" // For debugging purposes
        command "resubscribeAll" // / For debugging purposes
        command "createRGBDevice" // For debugging purposes
        command "deleteAllChildDevices" // For debugging purposes
    }
    
    preferences {
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"<b>Enable descriptionText logging</b>", defaultValue:true)
    }
    
    fingerprint endpointId:"00", model:"Smart RGBTW Bulb", manufacturer:"Leedarson", controllerType:"MAT"
}

void testLimitedRefresh(){
    if (txtEnable) log.debug "Refreshing  cluster 0x0006, all attributes, on endpoint #1"
    refreshMatter(ep:1, clusterInt:0x0006)
    
}

@Field static final Map checkChildDevicesOnReboot = [
    name: "checkchildDevicesOnReboot",
    title: "<b>Check and Rebuild Child Devices on Boot</b>",
    type: "bool",
    defaultValue: false,
    options: [],
    description: "Check for presence of all necessary child devices and correct driver types on reboot"
]

// These functions will be replaced by a routine that creates child devices based on endpoint device types.
// Currently, there is a parse error in Hubitat on endpoint type that prevents doing so!
void createRGBDevice(){
    device.updateDataValue("endpointId", "0000")
    addNewChildDevice(endpointType:0x0016, ep:0)
    addNewChildDevice(endpointType:0x0105, ep:1)
}
void createEveMotionChildDevices(){
    addNewChildDevice(endpointType:0x0016, ep:0)
    addNewChildDevice(endpointType:0x0107, ep:1)
    addNewChildDevice(endpointType:0x0106, ep:2)
}

void createEveEnergyOutletDevice(){
    addNewChildDevice(endpointType:0x0016, ep:0)
    addNewChildDevice(endpointType:0x010A, ep:1)
}

// This "parse" method handles Hubitat SendEvent type messages (not the description raw strings originating from the device). 
// It would be preferable if this had a different name so as to not cause confusion with the "parse" method for the description Strings from devices, but
// Hubitat's convntion has been to include a parse() routine with this function in Generic Component drivers (child device drivers) so for compatibility with 
// existing Generic Component drivers, the name is used. This parse method accepts a list of one or more SendEvent-type Maps and determines how those Hubitat sendEvent Maps should be handled.
// The List of SendEvent Maps may include event Maps that are not needed by a particular driver (as determined based on the attributes of the driver)
// and those "extra" Maps are filtered out and discarded. This allows a more generic "event Map" producting method (e.g., matterTools.createListOfMatterSendEventMaps) to produce a 
// collection of potential SendEvent maps which a driver then filters to remove those that are not needed.
void parse(List<Map> sendEventTypeOfEvents) {
    sendEventTypeOfEvents.each {
        if (device.hasAttribute (it.name)) {
            if (txtEnable) {
                if(device.currentValue(it.name) == it.value) {
                    log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") )+" (unchanged)" // Log if txtEnable and the value is the same
                } else {
                    log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") ) // Log if txtEnable and the value is the same
                }
            }
            if (updateLocalStateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
            } else {
                sendEvent(it)
            }
        }
    }
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String description) {
    try {
        Map decodedDescMap = parseDescriptionAsDecodedMap(description) // Using parser from matterTools.parseDescriptionAsDecodedMap
        
        // Following code stores received cluster values in case you want to use them elsewhere
        // But many clusters just aren't needed elsewhere!
        List<Integer> ignoreTheseClusters = [0x001F, // Access Control
                                             0x0029, // OTA Provider Cluster
                                             0x002A, // OTA Software Update Requestor
                                             0x002B, // Localization
                                             0x002C, // Time Format
                                             0x002D, // Unit Localization
                                             0x002E, // Power Source Configuration - but Power Source Cluster, 0x002F is processed!
                                             0x0030, // General Commissioning
                                             0x0031, // Network Commissioning
                                             0x0032, // Diagnostics Log
                                             0x0033, // General Diagnostics. Has some interesting stuff here, like the IP addresses. Consider using later!
                                             0x0034, // Software Diagnostics
                                             0x0035, // Thread Diagnostics. Events have been implemented, but this produces a lot of activity
                                             0x0036, // WiFi Diagnostics. Events have been implemented, but this produces a lot of activity
                                             0x0037, // Ethernet Diagnostics
                                             0x0038, // Time Sync Cluster
                                             0x003C, // Administrative Commissioning
                                             0x003E, // Node Operational Credentials
                                             0x003F, // Group Key Management
                                            ]
        
        List<Integer> ignoreTheseAttributes = [
                                             0xFFF8,// GeneratedCommandList
                                             0xFFF9, // AceptedCommandList
                                             0xFFFA, // EventList
                                             0xFFFB, // Attribute List             
                                             0xFFFD, // ClusterRevision
                                             0xFE, // Fabric Index
                                            ]   
        if (logEnable) log.debug "${device.displayName}: In parse, Matter attribute report string:<br><font color = 'green'>${description}<br><font color = 'black'>was decoded as: <font color='blue'>${decodedDescMap}"
        if ((decodedDescMap.clusterInt in ignoreTheseClusters) || (decodedDescMap.attrInt in ignoreTheseAttributes)) { return }
        
        storeRetrievedData(decodedDescMap)
        
        List<Map> hubEvents = getHubitatEvents(decodedDescMap)
        if (hubEvents.is(null)) { 
            if (decodedDescMap.attrInt in [0xFFFC]) return // FeatureMap is stored, but a Hubitat SendEvent event is not distributed
            if (logEnable) { log.warn "${device.displayName}: No events produced for map: <font color='blue'>${decodedDescMap}" }
            return
        }
        
        if (logEnable) log.debug "${device.displayName}: Events generated: <font color='blue'>${hubEvents}"
        // The next several lines distribut the events generated by getHubitatEvents
        // Some events, like battery (Power Cluster), can be sent to each child component device regardless of original endpoint. 0x002F -> Power cluster
        List<Integer> sendEverywhereClusters = [0x002F]
        
        if (  decodedDescMap.clusterInt  in sendEverywhereClusters ) {  // this is the "send to all regardless of endpoint" group of clusters!
            childDevices.each{it.parse(hubEvents) } // send to each child
            device.parse(hubEvents) // and to the parent (root) device
        } else { // other events just go to the particular endpoint they originated from!
            sendEventsToEndpointByParse(events:hubEvents, ep:(decodedDescMap.endpointInt))
        }       
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void updated(){
    log.info "${device.displayName}: Processing Preference changes..."
    if (logEnable) {
		log.info "${device.displayName}: Debug logging enabled for 30 minutes"
		runIn(1800,logsOff)
	}
}

void logsOff(){
    if (txtEnable) "${device.displayName}: Turning off Debug logging."
    device.updateSetting("logEnable", [value:"false",type:"bool"])
}

void configure(){
    log.info "${device.displayName}: Clearing Matter device subscriptions, and then resubscribing in 5 seconds"
    unsubscribeAll()
    runIn(5, resubscribeAll) // There seems to be a bug in Hubitat or some devices where a subscription too soon after unsubscribe won't "take". So delay the resubscribe.
}

// See Matter Core Spec. Section 2.11.2.2 - Interaction Model Limits for the number of subscriptions permitted.
// Guaranteed to support at least 3 Subscriber Interactions, each with at least 3 attribute/event paths.
void resubscribeAll(){
    if (txtEnable) log.info "${device.displayName}: Subscribing to device attribute reports with a 1 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}
void unsubscribeAll(){
    if (txtEnable) log.info "${device.displayName}: Unsubscribing from Matter attribute changes."
    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) // unsubscribe

}

void componentInitialize(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void initialize(){ 
    log.warn "${device.displayName}: Initialize currently does nothing! There seems to be a bug in Hubitat that calls this randomly due to some error."
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void refresh() {
    if (txtEnable) log.info "${device.displayName}: Refreshing all device data."
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}