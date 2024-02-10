/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.6 ("Level Control Cluster")
Dependencies: Need to import the following
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function
    matterTools.concurrentRuntimeDataStorage // needed to retrieve stored timing data

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
        name: "levelClusterMethods0x0008",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import groovy.transform.Field
import groovy.transform.CompileStatic
import java.lang.Math
import hubitat.helper.HexUtils

// Gets the OnOffTransitionTime parameter from stored data.
// Needs the 
Integer getOnOffTransitionTime(Map params = [:]){
    Map inputs = [ep:null] << params
    assert inputs.ep instanceof Integer
    String storedData = getStoredAttributeData(endpointInt:inputs.ep, clusterInt:0x0008, attrInt:0x0010)
    return (storedData ? Integer.parseInt(storedData, 16) : (null as Integer))
}

// Following functions implement Matter Spec 1.6.7.6 "MoveWithOnOFf and Section 1.6.7.2 "Move" commands" commands.
// Move command 0x01 is used if moveWithOnOff is set to 'false'
// MoveWithOnOff command 0x05 is used if movewithOnOff is set to true (default).
void componentStartLevelChange(com.hubitat.app.DeviceWrapper cd, direction, rate = 50 ) { startLevelChange(ep:getEndpointIdInt(cd), direction:direction, rate:rate) }
void startLevelChange(direction){ startLevelChange(ep:getEndpointIdInt(device), direction:direction)}
void startLevelChange(Map params = [:] ){
    try { 
        Map inputs = [ep:null, direction: null, rate:50, moveWithOnOff:true] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.rate instanceof Integer // rate corresponds to parameter Rate of command 1.6.7.2. Measured in units, not time (1-254 per second)
        assert ["up", "down"].contains(inputs.direction)
        assert inputs.moveWithOnOff instanceof Boolean

        String MoveMode = (inputs.direction == "up") ? "00" : "01"
        String MoveRate = HexUtils.integerToHexString(inputs.rate, 1) // Units per second in hex. Default rate of 50 means about 5 seconds for 0 -> 100% transition

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, MoveMode)) // MoveMode = 00 = Up, 01 = down per Matter spec 1.6.7.2
        fields.add(matter.cmdField(DataType.UINT8, 1, MoveRate)) // Move rate in units per second
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8

        String cmd = matter.invoke(inputs.ep, 0x0008, (inputs.moveWithOnOff ? 0x05 : 0x01), fields) // Move Up or Down
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
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
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
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
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.level instanceof Integer
        
        // Per Matter Spec, if transitionTime is null, use OnOffTransitionTime attribute value.
        // get that from previously retrieved data using getOnOffTransitionTime function or use 0 if that is unavailable. 
        // transitionTime is nullable. See Matter cluster spec 0008, Section 1.6.7.1, and core spec section 7.18 (Data Types),
        // so following the spec, you should be able to juse use a null value (FFFF) and not retrieve the stored value, but that doesn't work.
        if (inputs.transitionTime10ths.is(null)) { 
            inputs.transitionTime10ths = getOnOffTransitionTime(ep: inputs.ep) ?: 0 
        }
        assert inputs.transitionTime10ths instanceof Integer 
        

        String hexLevel = HexUtils.integerToHexString((Integer) ( Math.round(Math.max(Math.min((Integer) inputs.level, 100), 0) * 2.54)), 1)
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 ) // Time is in 10ths of a second! FFFF is the null value.

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, hexLevel)) // Level
        fields.add(matter.cmdField(DataType.UINT16, 1, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]) )) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8
        if (logEnable) log.debug "fields are ${fields}"
        String cmd = matter.invoke(inputs.ep, 0x0008, 0x04, fields) // Move To Level with On/Off
        if (logEnable) log.debug "sending command with transitionTime10ths value ${inputs.transitionTime10ths}: ${cmd}"
          sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

