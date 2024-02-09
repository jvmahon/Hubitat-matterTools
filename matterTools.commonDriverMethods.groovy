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

/*
def getEndpointId(device){
    return device.endpointId
}
*/

// This parser handles Hubitat event messages (not raw strings from the device). This parser operates  like the "parse" routine commonly found in subcomponent drivers. Use parse to distribute Hubitat events instead of sendEvent.
// Accepts a list of events -- i.e., may be more than one, and only sends those events which there is an attribute in the driver.
void parse(List<Map> events) {
    events.findAll{device.hasAttribute(it.name)}?.each {
            if (txtEnable && (device.currentValue(it.name) != it.value)) log.info it.descriptionText
            sendEvent(it)
    }
}

// This parser handles the Matter event message originating from Hubitat.
// It has a simple strucure of trying to call a routine in each profile / cluster supported by this driver model
// The routines are there if the associated library has been included.
void parse(String description) {
    Map descMap
    List<Map> hubEvents
    try {
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // supplement map with endpointId in integer form
        storeRetrievedData(descMap)
        
        // Certain manufacturere specific clusters are not processed and can be ignored
        // 0x130AFC01 is an Eve cluster
        /*
        if ([0x130AFC01].contains(descMap.clusterInt) ) {
            if(txtEnable) log.debug "Device ${device.displayName}: Received manufacturere specific cluster which is not processed: ${descMap}"
            return
        }
        */

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
        // 0x0037 - Ethernet NEtwork Diagnostics
        // 0x0038 - Time Synchronization
        // 0x003C - Administrator Commissioning
        // 0x003E - Node Operational Credentials
        // 0x003F - Group Key Management
        // 0x0040 - Fixed Label
        // 0x0041 - User Label
        // 0x0046 - ICD Management
        if ( [0x001D, 0x001E, 0x001F, 0x002C, 0x002E, 
              0x0030, 0x0031, 0x0033, 0x0034, 0x0037, 0x0038, 0x003C, 0x003E, 0x003F, 
              0x0040, 0x0041, 0x0046].contains(descMap.clusterInt) ) { 
            return
        }
        
        if (logEnable) log.info "-----------------: ${descMap}"
        hubEvents = getHubitatEvents(descMap)
        if (logEnable) log.info "Events are: " + hubEvents
        
        if ( [0x002F].contains(descMap.clusterInt) ) { // Some events can be sent to each child device regardless of original endpoint. 0x002F -> Power cluster
            childDevices.each{it.parse(hubEvents) }
        } else {
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

void unsubscribeAll(){
    String cmd = matter.unsubscribe()
    if (logEnable) log.debug "Sending command to Unsubscribe for all events: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void configure(){
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    if (logEnable) log.debug "Sending command to Subscribe for all events with a 1 second minimum time, refresh at 30 Minute maximum: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void componentInitialize(com.hubitat.app.DeviceWrapper cd) { 
    refreshMatter(ep:getEndpointIdInt(cd)) 
}
void initialize(){
    if (txtEnable) log.info "Refreshing all endpoints, all attributes: "
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void refresh() {
    if (txtEnable) log.info "${device.displayName} refreshing all device data"
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}

