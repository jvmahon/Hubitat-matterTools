import groovy.transform.Field
import hubitat.matter.DataType

#include matterTools.matterEnumTypes
#include matterTools.endpointAndChildDeviceTools // Tools for creation of child devices
#include matterTools.identifyClusterMethods0x0003 // Identify methods supporting named parameters, endpoints, and child devices
#include matterTools.OnOffClusterMethods0x0006 // On/Off cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.levelClusterMethods0x0008 // Level cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.ColorClusterMethods0x0300 // Color Cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.createListOfMatterSendEventMaps // Converts the data from parseAsMap to Hubitat event form
#include matterTools.commonDriverMethods // Main body of the driver, including parse handling and event distribution
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
        command "prettyPrintStoredAttributeData" // For debugging purposes
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

