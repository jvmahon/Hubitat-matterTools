/*
Copyright James Mahon
Distribution requires permission / Not for Commercial distribution
*/

import groovy.transform.Field
import hubitat.matter.DataType

#include matterTools.InovelliEndpointAndChildDeviceTools // Tools for creation of child devices
#include matterTools.identifyClusterMethods0x0003 // Identify methods supporting named parameters, endpoints, and child devices
#include matterTools.OnOffClusterMethods0x0006 // On/Off cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.levelClusterMethods0x0008 // Level cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.ColorClusterMethods0x0300 // Color Cluster methods supporting named parameters, endpoints, and child devices
#include matterTools.genericSwitchMethods0x003B // Process Generic Switch Events
#include matterTools.modeSelectClusterMethods0x0050 // Mode Select Cluster Methods to support Mode Select commands
#include inovelliTools.createListOfMatterSendEventMaps // Converts the data from parseAsMap to Hubitat event form
#include matterTools.matterHelperUtilities
#include matterTools.concurrentRuntimeDataStorage
#include matterTools.parseDescriptionAsDecodedMap

metadata {
    definition (name: "Inovelli White Series Matter Advanced Device Driver", namespace: "matterTools", author: "jvm33") {
		capability "Initialize"
		capability "Refresh"
        capability "Configuration"
        // capability "PushableButton"
        // capability "DoubleTapableButton"
        // capability "HoldableButton"
        // capability "ReleasableButton"
        
        command "identify",     [[name: "Identify", type:"NUMBER", description:"Put device into Identify mode (seconds)."]]
        command "initialize",   [[name: "Initialize",  description:"Called at boot time to confirm child devices exist and to initialize Matter."]]
        command "configure",    [[name: "Configure",   description:"Create child devices and set up Matter subscriptions."]]
        command "refresh",      [[name: "Refresh",     description:"Refresh device data."]]
    }
    
    preferences {
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"<b>Enable descriptionText logging</b>", defaultValue:true)
           input( name: "ModeSelect_1", type:"enum", options:getModeSelectOptions(1),  title: (getModeSelectLabel(1) ) )   
           input( name: "ModeSelect_2", type:"enum", options:getModeSelectOptions(2),  title: (getModeSelectLabel(2) ) )   
           input( name: "ModeSelect_3", type:"enum", options:getModeSelectOptions(3),  title: (getModeSelectLabel(3) ) )   
           input( name: "ModeSelect_4", type:"enum", options:getModeSelectOptions(4),  title: (getModeSelectLabel(4) ) )   
           input( name: "ModeSelect_5", type:"enum", options:getModeSelectOptions(5),  title: (getModeSelectLabel(5) ) )   
           input( name: "ModeSelect_6", type:"enum", options:getModeSelectOptions(6),  title: (getModeSelectLabel(6) ) )    
           input( name: "ModeSelect_7", type:"enum", options:getModeSelectOptions(2),  title: (getModeSelectLabel(7) ) )   
           input( name: "ModeSelect_8", type:"enum", options:getModeSelectOptions(3),  title: (getModeSelectLabel(8) ) )
    }
    
    fingerprint endpointId:"01", inClusters:"0003,0004,0006,0008,001D,0040,0041,0050,122FFC31", outClusters:"", model:"VTM30-SN", manufacturer:"Inovelli", controllerType:"MAT"
    fingerprint endpointId:"01", inClusters:"0003,0004,0005,0006,0008,001D,0040,0050,122FFC31", outClusters:"", model:"VTM31-SN", manufacturer:"Inovelli", controllerType:"MAT"
    fingerprint endpointId:"01", inClusters:"0003,0004,0006,0008,001D,0040,0050,0202,122FFC31", outClusters:"", model:"VTM35-SN", manufacturer:"Inovelli", controllerType:"MAT"
    fingerprint endpointId:"01", inClusters:"0003,0004,0005,0006,0008,001D,0040,0050,122FFC31", outClusters:"", model:"VTM36",    manufacturer:"Inovelli", controllerType:"MAT"
}


// This "parse" method handles Hubitat SendEvent type messages (not the description raw strings originating from the device). 
// It would be preferable if this had a different name so as to not cause confusion with the "parse" method for the description Strings from devices, but
// Hubitat's convntion has been to include a parse() routine with this function in Generic Component drivers (child device drivers) so for compatibility with 
// existing Generic Component drivers, the name is used. This parse method accepts a list of one or more SendEvent-type Maps and determines how those Hubitat sendEvent Maps should be handled.
// The List of SendEvent Maps may include event Maps that are not needed by a particular driver (as determined based on the attributes of the driver)
// and those "extra" Maps are filtered out and discarded. This allows a more generic "event Map" producting method (e.g., matterTools.createListOfMatterSendEventMaps) to produce a 
// collection of potential SendEvent maps which a driver then filters to remove those that are not needed.
void parse(List<Map> sendEventTypeOfEvents) {
    try {
		List updateLocalStateOnlyAttributes = ["Binding", "UserLabelList", "FixedLabelList"]
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
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String nodeReportRawDescriptionString) {
    log.debug "Received string to parse: ${nodeReportRawDescriptionString}"
    try {
        Map decodedNodeReportMap = parseDescriptionAsDecodedMap(nodeReportRawDescriptionString) // Using parser from matterTools.parseDescriptionAsDecodedMap
        log.debug "Main code body decoded a report map ${decodedNodeReportMap}"
        if ( decodedNodeReportMap.clusterInt  == 0x0050 ) {
             handleModeSelectClusterUpdate(decodedNodeReportMap)
             return
        }
        List<Map>  hubEvents
        if (decodedNodeReportMap.containsKey("attrInt")){
            hubEvents = getHubitatEventsFromAttributeReport(decodedNodeReportMap)
            if (hubEvents.is(null)) {  return }
            sendEventsToEndpointByParse(events:hubEvents, ep:(decodedNodeReportMap.endpointInt))
        }

        if (decodedNodeReportMap.containsKey("evtInt") && (decodedNodeReportMap.clusterInt = 0x003B)){
            Map InovelliEndpointToButtonMap = [3:1,  4:2,, 5:3]
            hubEvents = getHubitatEventsFromGenericSwitchEventReport(decodedNodeReportMap, InovelliEndpointToButtonMap)
            hubEvents.each {sendEvent(it)}
        }

    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${nodeReportRawDescriptionString}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void updated(){
    log.info "${device.displayName}: Processing Preference changes..."
    if (logEnable) {
		log.info "${device.displayName}: Debug logging enabled for 30 minutes"
		runIn(1800,logsOff)
	}

    for (ep in 1..6) {
        String modeSettingValue = device.getSetting("ModeSelect_${ep}")
        if (! modeSettingValue.is( null )) {
            Integer updatedMode = Integer.parseInt(modeSettingValue, 10)
            if (updatedMode == getModeSelectCurrentMode(ep)) continue
            if (txtEnable) log.info "${device.displayName}: Setting preference ${getModeSelectLabel(ep)} to value ${getModeSelectOptions(ep)?.get(updatedMode)}"
            changeToMode(ep:ep, mode:updatedMode)
        }
    }
}

void logsOff(){
    if (txtEnable) "${device.displayName}: Turning off Debug logging."
    device.updateSetting("logEnable", [value:"false",type:"bool"])
}

void configure(){
    if (txtenable) log.info  "${device.displayName}: Initializing device setup"
    if (logEnable) log.debug "${device.displayName}: Checking for child devices and subscribing to Matter event reports"

    checkAndCreateChildDevices()
    
    String cmd = 'he cleanSubscribe 0x01 0x0040 [ {"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    
    if (logEnable) log.debug "${device.displayName}: Subscribing to device attribute reports with a 1 second minimum report delay, refresh every 0x0040 = 64 seconds using command ${cmd}."
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void componentInitialize(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void initialize(){ 
    if (logEnable) log.debug "${device.displayName}: Initialize called!"
    configure()
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void refresh() {
    if (txtEnable) log.info "${device.displayName}: Refreshing all device data."
    refreshMatter(ep:0xFFFF, clusterInt: 0x0040, attrInt: 0x0000) // get the labels first just in case child devices are to be created!
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}
