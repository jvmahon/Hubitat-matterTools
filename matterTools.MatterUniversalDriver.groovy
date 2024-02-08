import groovy.transform.Field
import  hubitat.matter.DataType

#include matterTools.endpointAndChildDeviceTools // Tools for creation of child devices
#include matterTools.identifyClusterMethods0x0003 // Identify methods supporting named parameters, endpoints, and child devices
#include matterTools.OnOffClusterMethods0x0006 // On/Off cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.levelClusterMethods0x0008 // Level cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.ColorClusterMethods0x0300 // Color Cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.createMatterEvents // Converts the data from parseAsMap to Hubitat event form
#include matterTools.commonDriverMethods // Main body of the driver, including parse handling and event distribution
#include matterTools.matterHelperUtilities
#include matterTools.concurrentRuntimeDataStorage

metadata {
    definition (name: "Matter Universal Driver", namespace: "matterTools", author: "James Mahon") {
        capability "Configuration"		
		capability "Initialize"
		capability "Refresh"
        
        command "identify", [[name: "Identify", type:"NUMBER", range:"1..60", description:"Put device into Identify mode"]]
        command "recreateChildDevices" // For debugging purposes
        command "createEveMotionChildDevices" // For debugging purposes

        command "unsubscribeAll" // For debugging purposes
        command "createRGBDevice" // For debugging purposes
        command "deleteAllChildDevices" // For debugging purposes
        command "showStoredAttributeData" // For debugging purposes
    }
    
    preferences {
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"<b>Enable descriptionText logging</b>", defaultValue:true)
        input name: 'advancedOptions', type: 'bool', title: '<b>Advanced Options</b>', description: '<i>These advanced options should be already automatically set in an optimal way for your device...</i>', defaultValue: false
        if (advancedOptions == true) {
            // input(*:checkChildDevicesOnReboot )
        }
    }
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
    addNewChildDevice(endpointType:0x0016, ep:0)
    addNewChildDevice(endpointType:0x0105, ep:1)
}
void createEveMotionChildDevices(){
    addNewChildDevice(endpointType:0x0016, ep:0)
    addNewChildDevice(endpointType:0x0107, ep:1)
    addNewChildDevice(endpointType:0x0106, ep:2)
}

void update(){
    
}