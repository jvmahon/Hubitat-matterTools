import groovy.transform.Field
import  hubitat.matter.DataType

// #include matterTools.globalDataTools
#include matterTools.endpointAndChildDeviceTools
#include matterTools.identifyCluster0x0003
// #include matterTools.groupCluster0x0004
#include matterTools.OnOffCluster0x0006
#include matterTools.levelCluster0x0008
#include matterTools.ColorCluster0x0300
#include matterTools.IlluminanceCluster0x0400
#include matterTools.occupancySensingCluster0x0406
#include matterTools.commonDriverMethods
#include matterTools.matterHelperUtilities
#include matterTools.concurrentRuntimeDataStorage


metadata {
    definition (name: "Matter Universal Driver", namespace: "matterTools", author: "James Mahon") {
        capability "Actuator"
        capability "Configuration"		
		capability "Initialize"
		capability "Refresh"
        
        command "identify", [[name: "Identify", type:"NUMBER", range:"1..60", description:"Put device into Identify mode"]]
        command "recreateChildDevices"
        command "createEveMotionChildDevices"

        command "unsubscribeAll"
        command "createRGBDevice"
        command "deleteAllChildDevices"
        command "showStoredAttributeData"
    }
    
    preferences {
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"<b>Enable descriptionText logging</b>", defaultValue:true)
        input name: 'advancedOptions', type: 'bool', title: '<b>Advanced Options</b>', description: '<i>These advanced options should be already automatically set in an optimal way for your device...</i>', defaultValue: false
        if (advancedOptions == true) {
            input(name:"refreshEnable", type:"bool", title:"<b>Refresh Status on Hubitat Startup</b>", defaultValue:true)
            input(*:checkChildDevicesOnReboot )
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