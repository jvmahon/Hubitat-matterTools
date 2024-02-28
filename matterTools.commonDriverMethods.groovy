/* 
This library standardizes the implementation of common driver functions including:

parse
updated
configure
initialize
refresh
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Methods Common to Matter Drivers",
        name: "commonDriverMethods",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)


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
    Map descMap, decodedDescMap
    List<Map> hubEvents
    try {
        try {
            descMap = matter.parseDescriptionAsMap(description)
        } catch (e) {
            log.error e
            descMap = null 
        }
        decodedDescMap = parseDescriptionAsDecodedMap(description)

        log.debug "Parsed description:<br>${description}<br>toproduce map:<br>${descMap}<br>and decoded Map:<br>${decodedDescMap}"
        if(!descMap) return 
        
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // supplement map with endpointId in integer form
        storeRetrievedData(descMap)

        
        // No events are generated for the following global Element attributes. They do get stored and be retrieved from data stored using storeRetrievedData
        // See Section 7.13 of Matter core specification, Version 1.2
        if ( [0xFE, 0xFFF8, 0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC, 0xFFFD].contains(descMap.attrInt) ) return 
        
        // Following clusters are not  parsed correctly by Hubitat as of Feb. 2024, or  not needed,
        // Note that they are still stored on receipt, so they can be used by the program if needed.
        // 0x001D - Descriptor
        // 0x001E - Binding
        // 0x001F - Access Control
        // 0x002C - Time Format Localization
        // 0x002E - Power Surce Configuration
        // 0x0030 - General Commissioning
        // 0x0031 - Network Commissiioning
        // 0x0033 - General Diagnostics
        // 0x0034 - Software Diagnostics
        // 0x0037 - Ethernet Network Diagnostics
        // 0x0038 - Time Synchronization
        // 0x003C - Administrator Commissioning
        // 0x003E - Node Operational Credentials
        // 0x003F - Group Key Management
        // 0x0040 - Fixed Label
        // 0x0041 - User Label
        // 0x0046 - ICD Management
        /*
        if ( [0x001D, 0x001E, 0x001F, 0x002C, 0x002E, 
              0x0030, 0x0031, 0x0033, 0x0034, 0x0037, 0x0038, 0x003C, 0x003E, 0x003F, 
              0x0040, 0x0041, 0x0046].contains(descMap.clusterInt) ) { 
            return
        }
        */
        
        List<Integer> ignoreTheseClusters = [0x001D, 0x001E, 0x001F, 0x002C, 0x002E, 
              0x0030, 0x0031, 0x0033, 0x0034, 0x0037, 0x0038, 0x003C, 0x003E, 0x003F, 
              0x0040, 0x0041, 0x0046]
        if (descMap.clusterInt in ignoreTheseClusters) return
        
        hubEvents = getHubitatEvents(descMap)
        if (logEnable) log.debug "Events to be sent to main and child components are: " + hubEvents
        // The next several lines distribut the events generated by getHubitatEvents
        
        // Some events, like battery (Power Cluster), can be sent to each child component device regardless of original endpoint. 0x002F -> Power cluster
        List<Integer> sendEverywhereClusters = [0x002F]
        
        if (  descMap.clusterInt  in sendEverywhereClusters ) {  // this is the "send to all regardless of endpoint" group of clusters!
            childDevices.each{it.parse(hubEvents) } // send to each child
            device.parse(hubEvents) // and to the parent (root) device
        } else { // other events just go to the particular endpoint they originated from!
            if( hubEvents?.size()) sendEventsToEndpointByParse(events:hubEvents, ep:(descMap.endpointInt))
        }       
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void updated(){
    if (txtEnable) log.info "Processing Preference changes for ${device.displayName}..."
    if (logEnable) {
		log.info "For device ${device.displayName}: Debug logging enabled for 30 minutes"
		runIn(1800,logsOff)
	}
}

void logsOff(){
    if (txtEnable) "For device ${device.displayName}: Turning off Debug logging."
    device.updateSetting("logEnable", [value:"false",type:"bool"])
}

void clearStoredData() {
    state.clear()
}

// See Matter Core Spec. Section 2.11.2.2 - Interaction Model Limits for the number of subscriptions permitted.
// Guaranteed to support at least 3 Subscriber Interactions, each with at least 3 attribute/event paths.
void resubscribeAll(){
    if (txtEnable) log.info "Sending command to Subscribe to all device attribute reports with a 1 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void initialize(){ 
    log.warn "${device.displaName}: Initialize currrently does nothing, but seems to be called when there is a Matter protocol error.<br> Use Configure instead to clear and seup up subscriptions"
}

void unsubscribeAll(){   
        if (txtEnable) log.info "Sending commands to Unsubscribe from all device attribute reports."

    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) 
}

void configure(){
    unsubscribeAll()
    if (txtEnable) log.debug "Resubscribing in 5 seconds"
    runIn(5, resubscribeAll)
}

void componentInitialize(com.hubitat.app.DeviceWrapper cd) { 
    refreshMatter(ep:getEndpointIdInt(cd)) 
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void refresh() {
    if (txtEnable) log.info "${device.displayName} refreshing all device data"
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}

