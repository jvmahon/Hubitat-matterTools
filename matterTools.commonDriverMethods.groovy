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

def getEndpointId(device){
    return device.endpointId
}

// This parser handles Hubitat event messages (not raw strings from the device). This parser operates  like the "parse" routine commonly found in subcomponent drivers. Use parse to distribute Hubitat events instead of sendEvent.
// Accepts a list of events -- i.e., may be more than one, and only sends those events which there is an attribute in the driver.
void parse(List<Map> events) {
    events.findAll{device.hasAttribute(it.name)}?.each {
            if (txtEnable && it.descriptionText && (device.currentValue(it.name) != it.value)) log.info it.descriptionText
            sendEvent(it)
    }
}

// This parser handles the Matter event message originating from Hubitat.
// It has a simple strucure of trying to call a routine in each profile / cluster supported by this driver model
// The routines are there if the associated library has been included.
void parse(String description) {
    Map descMap
    List<Map> hubEvents = []

    try {
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // supplement map with endpointId in integer form
        storeRetrievedData(descMap)

		// If the relevant cluster library has been included, then call it!
		switch(descMap.clusterInt) {
            case 0x0000: if (processClusterResponse_0000) { hubEvents += processClusterResponse_0000(descMap) }; break; // Basic
			case 0x0003: if (processClusterResponse_0003) { hubEvents += processClusterResponse_0003(descMap) }; break; // Identify
			case 0x0004: if (processClusterResponse_0004) { hubEvents += processClusterResponse_0004(descMap) }; break; // Groups
			case 0x0005: if (processClusterResponse_0005) { hubEvents += processClusterResponse_0005(descMap) }; break; // Scenes
			case 0x0006: if (processClusterResponse_0006) { hubEvents += processClusterResponse_0006(descMap) }; break; // OnOff
			case 0x0008: if (processClusterResponse_0008) { hubEvents += processClusterResponse_0008(descMap) }; break; // Level
			case 0x002F: if (processClusterResponse_002F) { hubEvents += processClusterResponse_002F(descMap) }; break; // Power Source
			case 0x0050: if (processClusterResponse_0050) { hubEvents += processClusterResponse_0050(descMap) }; break; // Mode Select
            case 0x003B: if (processClusterResponse_003B) { hubEvents += processClusterResponse_003B(descMap) }; break; // Generic Switch Cluster for button taps
			case 0x0101: if (processClusterResponse_0101) { hubEvents += processClusterResponse_0101(descMap) }; break; // Door Lock
            case 0x0102: if (processClusterResponse_0102) { hubEvents += processClusterResponse_0102(descMap) }; break; // Window Covering
			case 0x0300: if (processClusterResponse_0300) { hubEvents += processClusterResponse_0300(descMap) }; break; // Color Control
			case 0x001D: if (processClusterResponse_001D) { hubEvents += processClusterResponse_001D(descMap) }; break; // Descriptor 
			case 0x001E: if (processClusterResponse_001E) { hubEvents += processClusterResponse_001E(descMap) }; break; // Binding Cluster
			case 0x001F: if (processClusterResponse_001F) { hubEvents += processClusterResponse_001F(descMap) }; break; // Root Node, Access Control
			case 0x0028: if (processClusterResponse_0028) { hubEvents += processClusterResponse_0028(descMap) }; break; // Root Node, Basic Information
			case 0x002A: if (processClusterResponse_002A) { hubEvents += processClusterResponse_002A(descMap) }; break; // OTA Software Update Requestor Cluster
			case 0x0030: if (processClusterResponse_0030) { hubEvents += processClusterResponse_0030(descMap) }; break; // Root Node, Generic Commissioning
			case 0x0031: if (processClusterResponse_0031) { hubEvents += processClusterResponse_0031(descMap) }; break; // Root Node, Network Commissiioning   
			case 0x0033: if (processClusterResponse_0033) { hubEvents += processClusterResponse_0033(descMap) }; break; // Root Node, General Diagnostics
			case 0x0035: if (processClusterResponse_0035) { hubEvents += processClusterResponse_0035(descMap) }; break; // Root Node, Thread Network Diagnostics
			case 0x0036: if (processClusterResponse_0036) { hubEvents += processClusterResponse_0036(descMap) }; break; // Root Node, Wi-Fi Network Diagnostics
			case 0x0037: if (processClusterResponse_0037) { hubEvents += processClusterResponse_0037(descMap) }; break; // Root Node, Ethernet Network Diagnostics
			case 0x0038: if (processClusterResponse_0038) { hubEvents += processClusterResponse_0038(descMap) }; break; // Root Node, Time Synchronization
            case 0x003C: if (processClusterResponse_003C) { hubEvents += processClusterResponse_003C(descMap) }; break; // Root Node, Administrator Commissioning   
			case 0x003E: if (processClusterResponse_003E) { hubEvents += processClusterResponse_003E(descMap) }; break; // Root Node, Node Operational Credentials  
			case 0x003F: if (processClusterResponse_003F) { hubEvents += processClusterResponse_003F(descMap) }; break; // Root Node, Group Key Management     
			case 0x0040: if (processClusterResponse_0040) { hubEvents += processClusterResponse_0040(descMap) }; break; // Fixed Label Cluster
			case 0x0041: if (processClusterResponse_0041) { hubEvents += processClusterResponse_0041(descMap) }; break; // User Label Cluster
			case 0x0400: if (processClusterResponse_0400) { hubEvents += processClusterResponse_0400(descMap) }; break; // Illuminance Measurement
			case 0x0406: if (processClusterResponse_0406) { hubEvents += processClusterResponse_0406(descMap) }; break; // Occupancy Sensing
			default:
                log.warn "In commonDriverMethods parse function parsed ${description} to get a Mapped message ${descMap} that was not handled by parse routine"
                childDevices.findAll{ getEndpointIdInt(it) == descMap.endpointInt }.each{ it.parseInChildDriver() }
        }
    } catch (AssertionError e) {
        log.error "<pre>${e}"
        log.error getStackTrace(e)
    } catch(e){
        log.error "<pre>${e}"
        log.error getStackTrace(e)    
    }

    if (hubEvents?.size()) sendEventsToEndpointByParse(events:hubEvents, ep:(descMap.endpointInt))
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
    String cmd = 'he subscribe 0x0001 0x00FF [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    if (logEnable) log.debug "Sending command to Subscribe for all events with a 1 second minimum time: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void initialize(){
    if (txtEnable) log.info "Refreshing all endpoints, all attributes: "
    String cmd = 'he rattrs [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void refreshAdvanced(Map params = [:]) {
    try {
        Map inputs = [ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF, attrIntList:[]] << params
        assert inputs.keySet().containsAll(params.keySet()) // check that function was called with expected input labels.
        assert inputs.ep instanceof Integer         // Make sure the type is as expected! 
        assert inputs.clusterInt instanceof Integer
        assert (inputs.attrInt instanceof Integer) ^ (inputs.attrList instanceof List) // Exclusive Or - accept either a single integer, or a list
        
        String cmd = 'he rattrs [{"ep":"' + inputs.ep  +'","cluster":"' + inputs.clusterInt    + '","attr":"' + inputs.attrInt    + '"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}"
        log.error getStackTrace(e)
		throw(e)
    }  catch(e) {
        log.error "Caught error in function refreshAdvanced: <pre>${e}"
        log.error getStackTrace(e)
        throw(e)
    }
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refresh(ep:getEndpointIdInt(cd)) }
void refresh(Map params = [:]) {
    try { 
        Map inputs = [ep:0xFFFF] << params // By default, refresh all endpoints using wildcard
		if (txtEnable) log.info "Refreshing device ${device.displayName}..."
		if (refresh_0000) refresh_0000(ep:inputs.ep) // Basic
		if (refresh_0003) refresh_0003(ep:inputs.ep) // Identify
		// if (refresh_0004) refresh_0004(ep:inputs.ep) // Groups
		// if (refresh_0005) refresh_0005(ep:inputs.ep) // Scenes
		if (refresh_0006) refresh_0006(ep:inputs.ep) // OnOff
		if (refresh_0008) refresh_0008(ep:inputs.ep) // Level
        // if (refresh_0050) refresh_0050(ep:inputs.ep) // Mode Select
        // if (refresh_003B) refresh_003B(ep:inputs.ep) // Generic Switch Cluster for button taps
        // if (refresh_0101) refresh_0101(ep:inputs.ep) // Door Lock
        // if (refresh_0102) refresh_0102(ep:inputs.ep) // Window Covering
        if (refresh_002F) refresh_002F(ep:inputs.ep) // Power Source
        if (refresh_0300) refresh_0300(ep:inputs.ep) // Color Control
        if (refresh_0400) refresh_0400(ep:inputs.ep) // Illuminance Measurement
        if (refresh_0406) refresh_0406(ep:inputs.ep) // Occupancy Sensing
    } catch (AssertionError e) {
        log.error "<pre>${e}"
        log.error getStackTrace(e)
    } catch(e) {
        log.error "Caught error in function refresh: <pre>${e}"
        log.error getStackTrace(e)
    }
}

