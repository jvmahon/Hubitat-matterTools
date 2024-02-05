/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.6 ("Level Control Cluster")
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
        description: "level Control Cluster 0x0008 Tools",
        name: "levelCluster0x0008",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import groovy.transform.Field
import groovy.transform.CompileStatic
import java.lang.Math
import hubitat.helper.HexUtils

Integer getOnOffTransitionTime(Map params = [:]){
    Map inputs = [ep:null] << params
    assert inputs.ep instanceof Integer
    String storedData = getStoredAttributeData(endpointInt:inputs.ep, clusterInt:0x0008, attrInt:0x0010)
    return (storedData ? Integer.parseInt(storedData, 16) : (null as Integer))
}

// Following functions implement Matter Spec 1.6.7.2 "Move" command.
void componentStartLevelChange(com.hubitat.app.DeviceWrapper cd, direction) { startLevelChange(direction:direction, ep:getEndpointIdInt(cd)) }
void startLevelChange(direction){ startLevelChange(ep:getEndpointIdInt(device), direction:direction)}
void startLevelChange(Map params = [:] ){
    try { 
        Map inputs = [ep:null, direction: null, rate:50] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.rate instanceof Integer // rate corresponds to parameter Rate of command 1.6.7.2. Measured in units, not time (1-254 per second)
        assert ["up", "down"].contains(inputs.direction)

        String MoveMode = (inputs.direction == "up") ? "00" : "01"
        String MoveRate = HexUtils.integerToHexString(inputs.rate, 1) // Units per second in hex. Default rate of 50 means about 5 seconds for 0 -> 100% transition

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, MoveMode)) // MoveMode = 0 = Up, 1 = down
        fields.add(matter.cmdField(DataType.UINT8, 1, MoveRate)) // Move rate in units per second
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8

        String cmd = matter.invoke(inputs.ep, 0x0008, 0x01, fields) // Move Up or Down
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    } 
}

// Following functions implement Matter Spec 1.6.7.4 "Stop" command.
void componentStopLevelChange(com.hubitat.app.DeviceWrapper cd) { stopLevelChange(ep:getEndpointIdInt(cd)) }
void stopLevelChange(direction){ stopLevelChange(ep:getEndpointIdInt(device))}
void stopLevelChange( Map params = [:] ){
    try { 
        Map inputs = [ ep:null ] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8,  0, "00")) // OptionMask, map8
        fields.add(matter.cmdField(DataType.UINT8,  1, "00"))  // OptionsOverride, map8
        String cmd = matter.invoke(inputs.ep, 0x0008, 0x03, fields) // Stop!
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    } 
}
                 
void componentSetLevel(com.hubitat.app.DeviceWrapper cd, cdLevel, cdDuration = null) {
	if (cd.hasCapability("FanControl") ) {
            throw new Exception("FanControl function not yet implemented in matterTools.levelCluster0x0008")
			// need to add a function like ... setSpeed(level:cdLevel, speed:levelToSpeed(cdLevel as Integer), ep:getEndpointIdInt(cd))
		} else { 
			setLevel(ep:getEndpointIdInt(cd), level:(cdLevel as Integer), transitionTime10ths: cdDuration.is(null) ? (null as Integer): ((cdDuration * 10) as Integer)) 
		}
}
void setLevel(inputLevel) {  setLevel(ep: getEndpointIdInt(device), level:(inputLevel as Integer) )} 
void setLevel(inputLevel, durationSeconds) { 
    setLevel(ep: getEndpointIdInt(device), level:(inputLevel as Integer), transitionTime10ths: durationSeconds.is(null) ? (null as Integer) : (durationSeconds * 10 as Integer)) // convert time from seconds to 10ths of a second!
}
void setLevel( Map params = [:] ) {
    try { 
        Map inputs = [ep: null , level: null , transitionTime10ths: null ] << params
        assert inputs.keySet().containsAll(["ep", "level", "transitionTime10ths"]) // check that all function was called with expected inputs.
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.level instanceof Integer
        // Per Matter Spec, if transitionTime is null, use OnOffTransitionTime attribute value.
        if (inputs.transitionTime10ths.is(null)) { 
            inputs.transitionTime10ths = getOnOffTransitionTime(ep: inputs.ep) ?: 0 // get that from previously retrieved data or 0 if that is unavailable. Should be able to use FFFF value, but it doesn't work.
        }
        assert inputs.transitionTime10ths instanceof Integer // TransitionTime is nullable. See Matter cluster spec 0008, Section 1.6.7.1, and core spec section 7.18 (Data Types)
        

        String hexLevel = HexUtils.integerToHexString((Integer) ( Math.round(Math.max(Math.min((Integer) inputs.level, 100), 0) * 2.54)), 1)
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 ) // Time is in 10ths of a second! FFFF is the null value.

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, hexLevel)) // Level
        fields.add(matter.cmdField(DataType.UINT16, 1, byteReverseParameters(hexTransitionTime10ths))) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8
        if (logEnable) log.debug "fields are ${fields}"
        String cmd = matter.invoke(inputs.ep, 0x0008, 0x04, fields) // Move To Level with On/Off
        if (logEnable) log.debug "sending command with transitionTime10ths value ${inputs.transitionTime10ths}: ${cmd}"
          sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch(AssertionError e) {
            log.error "<br><pre> ${getStackTrace(e)}"
            log.error "<pre>${e}"
    } 
}

def processClusterResponse_0008(Map descMap) {
	List<Map> hubEvents = []
    switch(descMap.attrInt) {
		case 0x0000: //currentLevel
			Integer newLevel = Math.round( Integer.parseInt(descMap.value, 16) / 2.54 )
			hubEvents << [name:"level", value: newLevel, units:"%", descriptionText: "${device.displayName} was set to level ${newLevel}%", isStateChange:false]
			break
		case 0x0001: // Remaining Time
			hubEvents << [name:"remainingTime", value: (Integer.parseInt(descMap.value, 16) / 10 ), units:"seconds", descriptionText: "${device.displayName} Remaining TransitionTime is ${(Integer.parseInt(descMap.value, 16) / 10 )} seconds", isStateChange:false]                    
            break
		case 0x0002: // MinLevel
			hubEvents << [name:"minLevel", value: Math.round(Integer.parseInt(descMap.value, 16) / 2.54 ), units:"%", descriptionText: "${device.displayName} Minimum Level Setting is ${Math.round(Integer.parseInt(descMap.value, 16) / 2.54 )} %", isStateChange:false]                    
            break
		case 0x0003: // MaxLevel
			hubEvents << [name:"maxLevel", value: Math.round(Integer.parseInt(descMap.value, 16) / 2.54 ), units:"%", descriptionText: "${device.displayName} Remaining TransitionTime is ${Math.round(Integer.parseInt(descMap.value, 16) / 10 )} seconds", isStateChange:false]                    
            break
		case 0x0004: // CurrentFrequency
		case 0x0005: // MinFrequency
		case 0x0006: // MaxFrequency
			break
		case 0x0010: // OnOffTransitionTime
			hubEvents << [name:"OnOffTransitionTime", value: (Integer.parseInt(descMap.value, 16) / 10 ), units:"seconds", descriptionText: "${device.displayName} On-Off TransitionTime is ${(Integer.parseInt(descMap.value, 16) / 10 )} seconds", isStateChange:false]                    
        break
		case 0x0011: // OnLevel
			hubEvents << [name:"OnLevel", value: (Integer.parseInt(descMap.value, 16) / 2.54 ), units:"%", descriptionText: "${device.displayName} On Level is ${(Integer.parseInt(descMap.value, 16) / 2.54 )} %", isStateChange:false]                    
			break
		case 0x0012: // OnTransitionTime
			hubEvents << [name:"OnTransitionTime", value: (Integer.parseInt(descMap.value, 16) / 10 ), units:"seconds", descriptionText: "${device.displayName} On TransitionTime is ${(Integer.parseInt(descMap.value, 16) / 10 )} seconds", isStateChange:false]                    
			break                    
		case 0x0013: // OffTransitionTime
			hubEvents << [name:"OffTransitionTime", value: (Integer.parseInt(descMap.value, 16) / 10 ), units:"seconds", descriptionText: "${device.displayName} Off TransitionTime is ${(Integer.parseInt(descMap.value, 16) / 10 )} seconds", isStateChange:false]                    
			break                    
		case 0x0014: // DefaultMoveRate
		case 0x000F: // Options
		case 0x4000: // StartUpCurrentLevel
		case 0xFFF8: // GeneratedCommandList
        case 0xFFF9: // AcceptedCommandList
        case 0xFFFA: // EventList
        case 0xFFFB: // AttributeList
        case 0xFFFC: // FeatureMap
        case 0xFFFD: // ClusterRevision
            break
		default:
			break
	}
    return hubEvents
}    

void refresh_0008(Map params = [:]){
    try { 
        Map inputs = [ep: 0xFFFF] << params
        assert inputs.ep instanceof Integer // Default value of FFFF is wildcard meaning all endpoints!
        String hexEP = HexUtils.integerToHexString(inputs.ep, 2) 
        String cmd = 'he rattrs [{"ep":"0x' + hexEP + '","cluster":"0x0008","attr":"0xFFFFFFFF"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }  
}
