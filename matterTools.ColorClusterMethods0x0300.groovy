/* 
Reference: Matter Application Cluster Specification Version 1.2, Section 3.2
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
        description: "Color Cluster 0x0300 Tools",
        name: "ColorClusterMethods0x0300",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

import groovy.transform.Field
import java.lang.Math
import hubitat.helper.HexUtils

void componentSetColor(com.hubitat.app.DeviceWrapper cd, Map colormap) { setColor(*:colormap, ep:getEndpointIdInt(cd)) }
void setColor(Map params = [:]){ // UI passes a Map so trying to set defaults here doesn't work.
    try {
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue: null, saturation: null, level: null] << params
        assert inputs.ep instanceof Integer
	    assert (inputs.saturation instanceof Integer) && (inputs.saturation >= 0) && (inputs.saturation <= 100)
	    assert (inputs.hue instanceof Integer) // Hue is degrees around a circule - don't need to range check, but mod with 100 before use!
                                                        
        assert inputs.transitionTime10ths instanceof Integer
                                                                                      
        inputs.hue = inputs.hue %100
        Integer targetHue = Math.round(inputs.hue * 2.54) // Hue is a color wheel so values > 100 are not an error, but should be 'modulus'-ed to 0-99
 	    Integer targetSat = Math.round(Math.max(Math.min((Integer) inputs.saturation, 100), 0) * 2.54)
    
        String hexHue = HexUtils.integerToHexString(targetHue, 1) // 1 Byte
 	    String hexSat = HexUtils.integerToHexString(targetSat, 1) // 1 Byte
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 )

        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT8,   0, hexHue)) // Hue uint8 0-254
            fields.add(matter.cmdField(DataType.UINT8,   1, hexSat)) // Saturation uint8 0-254
            fields.add(matter.cmdField(DataType.UINT16,  2, byteReverseParameters(hexTransitionTime10ths))) // TransitionTime in 0.1 Seconds, uint16 0-65534, byte swapped
            fields.add(matter.cmdField(DataType.UINT8, 3, "00")) // OptionMask, map8
            fields.add(matter.cmdField(DataType.UINT8, 4, "00"))  // OptionsOverride, map8
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x0006, fields)
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))  

        if (inputs.level) setLevel(*:inputs)

    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

void componentSetHue(com.hubitat.app.DeviceWrapper cd, hue) { setHue(hue:hue, ep:getEndpointIdInt(cd)) }
void setHue(hue){ setHue(ep:getEndpointIdInt(device), hue:hue as Integer) }
// Following functions implement Matter Spec 3.2.11.4 "MoveToSaturation" command 0x02
void setHue( Map params = [:] ){
    try {
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue:null] << params
        assert inputs.ep instanceof Integer 
	    assert (inputs.hue >= 0) && (inputs.hue <= 100) // Hubitat sets hue in percent
        assert inputs.transitionTime10ths instanceof Integer
    
        Integer targetHue = Math.round(Math.max(Math.min((Integer) inputs.hue, 100), 0) * 2.54)
    
        String hexHue = HexUtils.integerToHexString(targetHue, 1) // 1 Byte
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 )

        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT8,   0, hexHue)) // Hue uint8 0-254
            fields.add(matter.cmdField(DataType.UINT8,   1, "00")) // Direction 00 = Shortest
            fields.add(matter.cmdField(DataType.UINT16,  2, byteReverseParameters(hexTransitionTime10ths))) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped
            fields.add(matter.cmdField(DataType.UINT8, 3, "00")) // OptionMask, map8
            fields.add(matter.cmdField(DataType.UINT8, 4, "00"))  // OptionsOverride, map8
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x00, fields) // Move To Hue Command is 0x00. Matter Spec. Section 3.2.11.4
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))  
    
        if (inputs.level) setLevel(*:inputs)    
        
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 
}

void componentSetSaturation(com.hubitat.app.DeviceWrapper cd, saturation) {
    setSaturation(saturation:saturation, ep:getEndpointIdInt(cd))
}
void setSaturation(saturation){ setSaturation (ep:getEndpointIdInt(device), saturation:saturation as Integer) }
// Following functions implement Matter Spec 3.2.11.7 "MoveToSaturation" command 0x03,
void setSaturation( Map params = [:] ){
    try {
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, saturation:null ] << params
        assert inputs.ep instanceof Integer
	    assert (inputs.saturation >= 0) && (inputs.saturation <= 100) // hubitat specifies saturation in percent
        assert (inputs.transitionTime10ths instanceof Integer)

 	    Integer targetSat = Math.round(Math.max(Math.min((Integer) inputs.saturation, 100), 0) * 2.54)
    
 	    String hexSat = HexUtils.integerToHexString(targetSat, 1) // 1 Byte
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 )

        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT8,   0, hexSat)) // Saturation uint8 0-254
            fields.add(matter.cmdField(DataType.UINT8,   1, "00")) // Direction 00 = Shortest
            fields.add(matter.cmdField(DataType.UINT16,  2, byteReverseParameters(hexTransitionTime10ths))) // TransitionTime uint16 0-65534, byte swapped
            fields.add(matter.cmdField(DataType.UINT8, 3, "00")) // OptionMask, map8
            fields.add(matter.cmdField(DataType.UINT8, 4, "00"))  // OptionsOverride, map8
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x03, fields) // Move To Saturation Command is 0x03. Matter Spec. Section 3.2.11.4
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))  
    
        if (inputs.level) setLevel(*:inputs)    
       
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 		
}


// Following functions are to be called from web UI and child component device.
void componentSetColorTemperature(cd, colortemperature, level = null, transitionTime = null) { 
        setColorTemperature( ep:getEndpointIdInt(cd), colortemperature: colortemperature as Integer,  level: level as Integer, transitionTime10ths: ((transitionTime ?: 0) * 10) as Integer )
        }
void setColorTemperature(colortemperature, level = null, transitionTime = null) { 
        setColorTemperature( ep:getEndpointIdInt(device), colortemperature: colortemperature as Integer,  level: level as Integer, transitionTime10ths: ((transitionTime ?: 0) * 10) as Integer )
        }
// Following functions implement Matter Spec 3.2.11 "MoveToColorTemperature" command, Section 3.2.11.14
void setColorTemperature( Map params = [:] ){
    try {
        Map inputs = [ep: null, colortemperature:null, level:null, transitionTime10ths: null] << params
        assert inputs.ep instanceof Integer
        assert (inputs.colortemperature instanceof Integer) && (inputs.colortemperature > 15) // 15.3 is the minimum supported by Matter based on ColorTemperatureMireds Contrsint 0xFEFF. Section 3.2.11.14.
        assert inputs.level instanceof Integer || inputs.level.is(null)
        assert inputs.transitionTime10ths instanceof Integer || inputs.transitionTime10ths.is(null)  
    
        Integer targetMireds = (1000000 / inputs.colortemperature) // Convert input in Kelvin to Mireds
    
 	    String hexMireds = HexUtils.integerToHexString(targetMireds, 2) //
        String hexTransitionTime10ths = HexUtils.integerToHexString( (inputs.transitionTime10ths ?: 0), 2 )

        List<Map<String, String>> fields = []
            fields.add(matter.cmdField(DataType.UINT16, 0, byteReverseParameters(hexMireds))) // ColorTemperatureMireds
            fields.add(matter.cmdField(DataType.UINT16, 1, byteReverseParameters(hexTransitionTime10ths))) // TransitionTime uint16 0-65534, byte swapped
            fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8
            fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x0A, fields) // Move To Color Temperature Command is 0x0A. Matter Spec. Section 3.2.11.14
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    
        if (inputs.level) setLevel(*:inputs)    
       
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}    


