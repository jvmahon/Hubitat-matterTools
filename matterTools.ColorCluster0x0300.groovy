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
        name: "ColorCluster0x0300",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

import groovy.transform.Field
import java.lang.Math
import hubitat.helper.HexUtils

void componentSetColor(com.hubitat.app.DeviceWrapper cd, Map colormap) { setColor(*:colormap, ep:getEndpointIdInt(cd)) }
void setColor(Map params = [:]){ // UI passes a Map so trying to set defaults here doesn't work.
    log.debug params
    try {
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue: null, saturation: null, level: null] << params
        assert inputs.keySet().containsAll(params.keySet()) // checks that all user-specified parameters use permitted labels.
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
        
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }    
}

void componentSetHue(com.hubitat.app.DeviceWrapper cd, hue) { setHue(hue:hue, ep:getEndpointIdInt(cd)) }
void setHue(hue){ setHue(ep:getEndpointIdInt(device), hue:hue as Integer) }
// Following functions implement Matter Spec 3.2.11.4 "MoveToSaturation" command 0x02
void setHue( Map params = [:] ){
    try {
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue:null] << params
        assert inputs.keySet().containsAll(["ep", "hue", "transitionTime10ths"]) // checks that all user-specified parameters use permitted labels.
        assert inputs.ep instanceof Integer 
	    assert (inputs.hue >= 0) && (inputs.hue <= 100) // Hubitat setss hue in percent
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
        
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
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
        assert inputs.keySet().containsAll(["ep", "saturation", "transitionTime10ths"]) // checks that all user-specified parameters use permitted labels.
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
       
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
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
        assert inputs.keySet().containsAll(["ep", "colortemperature", "transitionTime10ths"] )  // check that function was called with required inputs.
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
       
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
            return
    } 
}    


List<Map> processClusterResponse_0300(Map descMap){
    List<Map> hubEvents = []
	Map event
	switch(descMap.attrInt) {
		case 0x0000: //current Hue
			Integer newHue = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
			hubEvents << [name:"hue", value: newHue, units:"%", descriptionText: "${device.displayName} was set to hue ${newHue}%", isStateChange:false]
            String colorName = convertHueToGenericColorName(newHue)
            hubEvents << [name:"colorName", value: colorName, descriptionText: "Color Name set to ${colorName}", isStateChange:false]
			break
		case 0x0001: // Current Saturation
			Integer newValue = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
			hubEvents << [name:"saturation", value: newValue, units:"%", descriptionText: "${device.displayName} was set to Saturation ${newValue}%", isStateChange:false]
			break
		case 0x0002: // remaining Time
			hubEvents << [name:"remainingColorTransitionTime", value: (Integer.parseInt(descMap.value, 16) / 10), descriptionText: "${device.displayName} Remaining Color Transition Time", isStateChange:false]
			break
        case 0x0003: // CurrentX
        case 0x0004: // CurrentY
            break
		case 0x0007: // Color Temperature in Mireds
			Integer kelvinValue = 1000000 / Integer.parseInt(descMap.value, 16)
			hubEvents << [name:"colorTemperature", value: kelvinValue, descriptionText: "${device.displayName} was set to new color temperature: ${kelvinValue} Kelvin", unit: "°K", isStateChange:false]
            String colorName = convertTemperatureToGenericColorName(kelvinValue)
            hubEvents << [name:"colorName", value: colorName, descriptionText: "Color Name set to ${colorName}", isStateChange:false]
        break
		case 0x0008: // Color Mode
			switch (Integer.parseInt(descMap.value, 16)) {
				case 0x00: // Hue and  Saturation Mode
					hubEvents << [name:"colorMode", value: "RGB", descriptionText: "${device.displayName} was set to color mode RGB", isStateChange:false]
					break
				case 0x01: // CurrentX and CurrentY  Mode
					hubEvents << [name:"colorMode", value: "CurrentXY", descriptionText: "${device.displayName} was set to color mode CurrentXY", isStateChange:false]
					log.error "${device.displayName} was set to color mode CurrentXY which is not supported"
					break
				case 0x02: // Color Temperature in Mireds
					hubEvents << [name:"colorMode", value: "CT", descriptionText: "${device.displayName} was set to color temperature mode", isStateChange:false]
					break
			}
			break
        case 0x000F: // options
            break;
        case 0x0010: // NumberOfPrimaries
             break;
        case 0x4000: // EnhancedCurrentHue
        case 0x4001: // EnhancedColorMode
        case 0x4002: // ColorLoopActive
        case 0x4003: // ColorLoopDirection
        case 0x4004: // ColorLoopTime
        case 0x4005: // ColorLoopStartEnhancedHue
        case 0x4006: // ColorLoopStoredEnhancedHue
                break
		case 0x400A: // ColorCapabilities
			Integer capabilitiesInt = Integer.parseInt(descMap.value, 16)
	
			Map capabilities = ["HS":false, "Enhanced Hue":false, "Color Loop":false, "Cie xyY": false, "Color Temp":false]
				if (capabilitiesInt & 0b00000001) capabilities << ["HS":true]
				if (capabilitiesInt & 0b00000010) capabilities << ["Enhanced Hue":true]
				if (capabilitiesInt & 0b00000100) capabilities << ["Color Loop":true]
				if (capabilitiesInt & 0b00001000) capabilities << ["Cie xyY":true]
				if (capabilitiesInt & 0b00010000) capabilities << ["Color Temp":true]
			hubEvents << [name:"colorCapabilities", value: capabilities, descriptionText: "${device.displayName} color Capabilities", isStateChange:false]
			break
        case 0x400B: // ColorTempPhysicalMinMireds.  There is an inverse relationship between Mireds and Kelvin, so Min Mireds corresponds to Max Kelvin.
        		Integer kelvinValue = 1000000 / Integer.parseInt(descMap.value, 16)
        		hubEvents << [name:"colorTemperatureMax", value: kelvinValue, descriptionText: "Minimum Possible Color temperature for ${device.displayName} is: ${kelvinValue} Kelvin", unit: "°K", isStateChange:false]
                break
        case 0x400C: // ColorTempPhysicalMaxMireds. There is an inverse relationship between Mireds and Kelvin, so Max Mireds corresponds to Min Kelvin.
        		Integer kelvinValue = 1000000 / Integer.parseInt(descMap.value, 16)
                hubEvents << [name:"colorTemperatureMin", value: kelvinValue, descriptionText: "Maximum Possible Color temperature for ${device.displayName} is: ${kelvinValue} Kelvin", unit: "°K", isStateChange:false]

                break
        case 0x400D: // CoupleColorTempToLevelMinMireds
        case 0x4010: // StartUpColorTemperatureMireds
        
        // Global Attributes
        case 0xFFF8: // GeneratedCommandList
        case 0xFFF9: // AcceptedCommandList
        case 0xFFFA: // EventList
        case 0xFFFB: // AttributeList
        case 0xFFFC: // FeatureMap
        case 0xFFFD: // ClusterRevision
            break
		default:
			log.error "${device.displayName} has unprocessed color mode attribute. See ${descMap}"
		
	}
    return hubEvents
}

void refresh_0300( Map params = [:] ){
    try { 
        Map inputs = [ep: 0xFFFF] << params
        assert inputs.ep instanceof Integer // Default value of FFFF is wildcard meaning all endpoints!
        String hexEP = HexUtils.integerToHexString(inputs.ep, 2) 
        String cmd = 'he rattrs [{"ep":"0x' + hexEP + '","cluster":"0x0300","attr":"0xFFFFFFFF"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }  
}

