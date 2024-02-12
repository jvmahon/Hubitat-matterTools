/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.5 "On/Off Cluster"
Dependencies: Need to import the following
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function

Library also assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt). 
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16)))
See matterTools.commonDriverMethods library for example
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "On Off Cluster 0x0006 Tools",
        name: "OnOffClusterMethods0x0006",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

import hubitat.helper.HexUtils

Boolean supportsOffTimer(){
    if (logEnable) log.debug "supportsOffTime function not fully implemented in matterTools.OnOffCluster0x0006. Defaults to 'true' as this is Mandatory in Lighting device types"
    return true
}
 

// off implements Matter 1.2 Cluster Spec Section 1.5.7.1, Off command
void componentOff(com.hubitat.app.DeviceWrapper cd){ off(ep:getEndpointIdInt(cd)) }
void off( Map params = [:] ){
    try { 
        Map inputs = [ ep:getEndpointIdInt(device) ] << params
        assert inputs.ep instanceof Integer  // Use Integer, not Hex! 
        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x00), hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

// on implements Matter 1.2 Cluster Spec Section 1.5.7.2, On command
void componentOn(com.hubitat.app.DeviceWrapper cd){ on( ep:getEndpointIdInt(cd)) }
void on( Map params = [:] ){
    try { 
        Map inputs = [ ep:getEndpointIdInt(device)] << params
        assert inputs.ep instanceof Integer // Use Integer, not Hex!

        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x01), hubitat.device.Protocol.MATTER))  

    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

// toggleOnOff implements Matter 1.2 Cluster Spec Section 1.5.7.3, Toggle command
void componentToggleOnOff(com.hubitat.app.DeviceWrapper cd){ toggleOnOff( ep:getEndpointIdInt(cd)) }
void toggleOnOff( Map params = [:] ){
    try { 
        Map inputs = [ ep:getEndpointIdInt(device)] << params
        assert inputs.ep instanceof Integer // Use Integer, not Hex!
        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x02), hubitat.device.Protocol.MATTER))  
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

//onWithTimedOff implements Matter 1.2 Cluster Spec Section 1.5.7.6, OnWithTimedOff command
void componentOnWithTimedOff(com.hubitat.app.DeviceWrapper cd, childOnTime10ths, childOffWaitTime10ths) {
    onWithTimedOff(ep:getEndpointIdInt(cd), onTime10ths:childOnTime10ths, offWaitTime10ths:childOffWaitTime10ths)
}
void onWithTimedOff( Map params = [:] ){ 
    try { 
        Map inputs = [ ep: getEndpointIdInt(device), onTime10ths: 10, offWaitTime10ths:0] << params
        assert inputs.ep instanceof Integer // Use Integer, not Hex! 
        assert inputs.onTime10ths instanceof Integer
        assert inputs.offWaitTime10ths instanceof Integer // Doesn't seem to do anything!

        String hexOnTime10ths =       HexUtils.integerToHexString(inputs.onTime10ths, 2) 
        String hexOffWaitTime10ths =  HexUtils.integerToHexString(inputs.offWaitTime10ths, 2) 

        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT8,  0, "00")) // OnOffControlBitmap
            fields.add(matter.cmdField(DataType.UINT16, 1, (hexOnTime10ths[2..3] + hexOnTime10ths[0..1]) )) // OnTime, byte swapped
            fields.add(matter.cmdField(DataType.UINT16, 2, (hexOffWaitTime10ths[2..3] + hexOffWaitTime10ths[0..1]) )) // OffWaitTime - guarded wait time, byte swapped
    
        String cmd = matter.invoke(inputs.ep, 0x0006, 0x42, fields)
        if (logEnable) log.debug "${device.displayName}: Turning on with timer using parameters ${inputs}"
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}
