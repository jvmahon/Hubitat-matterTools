/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.5 "On/Off Cluster"
Dependencies: Need to import the following
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function
    matterTools.matterHelperUtilities         // needed for byteReverseParameters

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
    
void componentOn(com.hubitat.app.DeviceWrapper cd){ on( ep:getEndpointIdInt(cd)) }
void on( Map params = [:] ){
    try { 
        Map inputs = [ ep:getEndpointIdInt(device)] << params
        assert inputs.ep instanceof Integer // Use Integer, not Hex!
        
       if (useOnOffTimer  && supportsOffTimer()  ) { // useOnOffTime is to be set by a preference in the driver.
           Integer timer = settings.get("offTime") as Integer
           onWithTimedOff(*:inputs, onTime10ths: (timer * 10))
       } else {
            sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x01), hubitat.device.Protocol.MATTER))  
       }
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }    
}

//onWithTimedOff implements Matter 1.2 Cluster Spec Section 1.5.7.6, OnWithTimedOff command
void onWithTimedOff( Map params = [:] ){ 
    try { 
        Map inputs = [ ep: getEndpointIdInt(device), onTime10ths: 10, offWaitTime10ths:0] << params
        assert inputs.ep instanceof Integer // Use Integer, not Hex! 
        assert inputs.onTime10ths instanceof Integer
        assert inputs.offWaitTime10ths instanceof Integer

        String hexOnTime10ths = byteReverseParameters( HexUtils.integerToHexString(inputs.onTime10ths) )
        String hexOffWaitTime10ths = byteReverseParameters( HexUtils.integerToHexString(inputs.offWaitTime10ths) )
        
        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT8,  0, "00")) // OnOffControlBitmap
            fields.add(matter.cmdField(DataType.UINT16, 1, hexOnTime10ths)) // OnTime, byte swapped
            fields.add(matter.cmdField(DataType.UINT16, 2, hexOffWaitTime10ths)) // OffWaitTime - guarded wait time, byte swapped
    
        String cmd = matter.invoke(inputs.ep, 0x0006, 0x42, fields)
        if (logEnable) log.debug "${device.displayName}: Turning on with timer using parameters ${inputs}"
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }   
}

void componentOff(com.hubitat.app.DeviceWrapper cd){ off(ep:getEndpointIdInt(cd)) }
void off( Map params = [:] ){
    try { 
        Map inputs = [ ep:getEndpointIdInt(device) ] << params
        assert inputs.ep instanceof Integer  // Use Integer, not Hex! 
        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x00), hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }
}

void refresh_0006(Map params = [:]){
    try { 
        Map inputs = [ep:0xFFFF] << params
        assert inputs.ep instanceof Integer // Default value of FFFF is wildcard meaning all endpoints!
        String hexEP = HexUtils.integerToHexString(inputs.ep, 2) 
        String cmd = 'he rattrs [{"ep":"0x' + hexEP + '","cluster":"0x0006","attr":"0xFFFFFFFF"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }    
}
