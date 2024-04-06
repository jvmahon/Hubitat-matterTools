import hubitat.matter.DataType
import hubitat.helper.HexUtils
import groovy.transform.Field
metadata {
    definition (name: "Inovelli Matter Mode Select  Test Driver", namespace: "matterTools", author: "jvm33") {
        command "refresh", [[name:"Step 3 - Refresh Attributes", description:"A debugging tool. Choose this to Request Matter Mode Select attribute reports."]]
        command "matterUnsubscribe", [[name:"Step 1 - Unsubscribe Device", description:"A debugging tool. Choose this to unsubscribe from Matter attribute reports."]]
        command "modeSelectReportSubscriptions", [[name:"Step 2 - Subscribe Device", description:"A debugging tool. Choose this to subscribe to Matter attribute reports."]]

    }
       preferences {
           input( name: "ModeSelect_1", type:"enum", options:modeSelectMenuMap[1].menuItems,  title:modeSelectMenuMap[1].title, description: modeSelectMenuMap[1].description )   
           input( name: "ModeSelect_2", type:"enum", options:modeSelectMenuMap[2].menuItems,  title:modeSelectMenuMap[2].title, description: modeSelectMenuMap[2].description )   
           input( name: "ModeSelect_3", type:"enum", options:modeSelectMenuMap[3].menuItems,  title:modeSelectMenuMap[3].title, description: modeSelectMenuMap[3].description )   
           input( name: "ModeSelect_4", type:"enum", options:modeSelectMenuMap[4].menuItems,  title:modeSelectMenuMap[4].title, description: modeSelectMenuMap[4].description )   
           input( name: "ModeSelect_5", type:"enum", options:modeSelectMenuMap[5].menuItems,  title:modeSelectMenuMap[5].title, description: modeSelectMenuMap[5].description )   
           input( name: "ModeSelect_6", type:"enum", options:modeSelectMenuMap[6].menuItems,  title:modeSelectMenuMap[6].title, description: modeSelectMenuMap[6].description )   
      }
}

void updated(){
    // Process the mode updates!
    for (int ep = 1; ep<=6; ep++) {
        Integer updatedMode = Integer.parseInt(device.getSetting("ModeSelect_${ep}"), 10)
        if (!updatedMode.is(null)) { changeToMode(ep:ep, mode:updatedMode) }
    }
}

// Another option is to read the menu choices from the device, but this allows addition of decription text!
// Map is by endpoint (1-6), then choices for the ModeSelect for that endpoint.
@Field static modeSelectMenuMap =[    
    1:[title:"<b>Switch or Dimmer Mode</b>", 
       menuItems:[0:"OnOff + Single", 1:"OnOff + Dumb", 2:"OnOff + AUX", 3:"OnOff + Full Wave(default)", 4:"Dimmer + Single", 5:"Dimmer + Dumb", 6:"Dimmer + AUX" ],
       description:"Operate as a On/Off Device or a Dimmer, and set the type of 3-way switch if in 3-way configuration.",
      ],
    2:[title:"<b>Smart Bulb Operation</b>", 
       menuItems:[0:"Normal Mode", 1:"Use Smart Bulb Mode"],
       description:"Normal Mode means device controls power to the load, Smart Bulb Mode means power to load is always on",
      ],
    3:[title:"<b>Set Dimming Edge (advanced)</b>", 
       menuItems:[0:"Leading", 1:"Trailing"],
       description:"Experiment to find the dimming type that works best with your LED bulbs.",
      ],
    4:[title:"<b>Button Tapping Timeout</b>", 
       menuItems:[0:"No Delay (Disable Button Tap Detection)", 3:"300ms", 5:"500ms (default)", 7:"700ms"],
       description:"Maximum time to detect a sequence of button taps",
      ],
    5:[title:"<b>Relay Operation</b>", 
       menuItems:[0:"Relay Enabled (default)", 1:"Relay Disabled (Quiet Operation)"],
       description:"Relay Enabled ensures that even the most fussy LEDs turn off, but adds noise. Disable for quiet operation!",
       ],
    6:[title:"<b>LED Strip Color</b>", 
       menuItems:[ 0:"Red", 1:"Orange", 2:"Lemon", 3:"Lime", 4:"Green", 5:"Teal", 6:"Cyan", 7:"Aqua", 8:"Blue", 9:"Violet",  10:"Magenta", 11:"Pink", 12:"White" ],
       description:"Color of LED strip during dimming operations. A separate RGB control sets color for the alert feature!",
      ],
    ]

void handleModeSelectClusterUpdate(decodedDescriptionMap){
    switch(decodedDescriptionMap.attrInt){
        case 0x0003: // Current Mode
            String settingsName = "ModeSelect_${decodedDescriptionMap.endpointInt}"
            String newValue = "${decodedDescriptionMap.decodedValue}" // Hubitat oddity - all device.settings keys are strings, so newValue must be a number in string format, even though the index to menuItems was originally an Intger number
            device.updateSetting(settingsName, [value:newValue, type:"enum"])
            break
        }
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String description) {
    Map decodedDescriptionMap = parseDescriptionAsDecodedMap(description) // Using parser from matterTools.parseDescriptionAsDecodedMap
    log.info "${device.displayName}: Received report: <font color='blue'>${decodedDescriptionMap}"
    switch(decodedDescriptionMap.clusterInt){
        case 0x0050:
            handleModeSelectClusterUpdate(decodedDescriptionMap)
            break
    }
}

void matterUnsubscribe(){
    log.info "${device.displayName}: Clearing Matter device subscriptions"
    if (txtEnable) log.info "${device.displayName}: Unsubscribing from Matter attribute changes."
    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) // unsubscribe
}

void modeSelectReportSubscriptions(){
    log.info "${device.displayName}: Subscribing to reports for Mode Select cluster with a 1 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0x0050","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}


void refresh() {
    log.info "${device.displayName}: Refreshing Mode Select Data on All Endpoints."
    refreshMatter(ep:0xFFFF, clusterInt: 0x0050, attrInt: 0xFFFFFFFF)
}



// ==============================================================================

// Following functions implement Matter Spec 1.8.7 "ChangeToMode" command.
void changeToMode( Map params = [:] ) {
    try { 
        Map inputs = [ep: null , mode: null] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.mode instanceof Integer
                
        String hexMode = HexUtils.integerToHexString((Integer) inputs.mode, 1)

        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, hexMode)) // Mode

        if (logEnable) log.debug "fields are ${fields}"
        String cmd = matter.invoke(inputs.ep, 0x0050, 0x00, fields) // ChangeToMode
        if (logEnable) log.debug "sending changeToMode command: ${cmd}"
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing changeToMode with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

// Matter payloads need hex parameters of greater than 2 characters to be pair-reversed.
// This function takes a list of parameters and pair-reverses those longer than 2 characters.
// Alternatively, it can take a string and pair-revers that.
// Thus, e.g., ["0123", "456789", "10"] becomes "230189674510" and "123456" becomes "563412"
private String byteReverseParameters(String oneString) { byteReverseParameters([] << oneString) }
private String byteReverseParameters(List<String> parameters) {
	StringBuilder rStr = new StringBuilder(64)
	for (hexString in parameters) {
		if (hexString.length() % 2) throw new Exception("In method byteReverseParameters, trying to reverse a hex string that is not an even number of characters in length. Error in Hex String: ${hexString}, All method parameters were ${parameters}.")
		
		for(Integer i = hexString.length() -1 ; i > 0 ; i -= 2) {
			rStr << hexString[i-1..i]
		}	
	}
	return rStr
}

// ==============================================================================
// Performs a refresh on a designated endpoint / cluster / attribute (all specified in Integer)
// Does a wildcard refresh if parameters are not specified (ep=FFFF / cluster=FFFFFFFF/ endpoint=FFFFFFFF is the Matter wildcard designation
void refreshMatter(Map params = [:]) {
    try {
        Map inputs = [ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF] << params
        assert inputs.ep instanceof Integer         // Make sure the type is as expected! 
        assert inputs.clusterInt instanceof Integer || inputs.clusterInt instanceof Long
        assert inputs.attrInt instanceof Integer || inputs.attrInt instanceof Long
        
       // Groovy Slashy String form of a GString  https://docs.groovy-lang.org/latest/html/documentation/#_slashy_string
        String cmd = /he rattrs [{"ep":"${inputs.ep}","cluster":"${inputs.clusterInt}","attr":"${inputs.attrInt}"}]/

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing refreshMatter with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}
// ==============================================================================


// Per Matter Spec Appendix A.6, values greater than 0b11000 are reserved, except for 0b00011000 which is End-of-Container
Boolean isReservedValue(Integer controlOctet){ 
    return  ( ((controlOctet & 0b00011111) >= (0b11000)) && !(controlOctet == 0b00011111))
}

String HexToString(String hexStr){
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int i = 0; i < hexStr.length(); i += 2) {
      baos.write(Integer.parseInt(hexStr.substring(i, i + 2), 16));
    } 
    return baos.toString()
    // return new String(baos.toByteArray() );     
}

// Strings are immutable in groovy, but StringBuilder strings are not. 
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string.
Object getTagValue(StringBuilder valueString, Integer tagControl){
    Object rValue
    switch(tagControl){
        case 0b000: // 0 Octets
            rValue = null; break;
        case 0b001: // Context-specific, 1 octet
            rValue = Integer.parseInt(valueString[0..1] , 16); valueString.delete(0,2); break
        case 0b010: // Common Profile, 2 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..3]; valueString.delete(0,4);  break
        case 0b011: // Common Profile, 4 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..7]; valueString.delete(0,8); break
        case 0b100: // Implicit Profile, 2 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..3]; valueString.delete(0,4); break
        case 0b101: // Implicit Profile, 4 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..7]; valueString.delete(0,8); break
        case 0b110: // Fully-Qualified form, 6 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..11];  valueString.delete(0,12); break
        case 0b111: // Fully-Qualified form, 8 octets. Not really sure how this should be represented. For now, using a string!
            rValue = valueString[0..15];  valueString.delete(0, 16); break
    }
    return rValue
}

// Strings are immutable in groovy, but StringBuilder strings are not. 
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string
Object getElementValue(StringBuilder valueString, Integer elementType){
    Object rValue = null
    try {
		switch(elementType){
		case 0b00000: // Signed Integer, 1-Octet
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error!
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet
			if(rValue & 0x80) rValue = rValue - 256 // Make into a negative if greater than 0x80
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00001: // Signed Integer, 2-Octet
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error!
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets
			if(rValue & 0x8000) rValue = rValue - 65536 // Make into a negative if greater than 0x8000
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00010: // Signed Integer, 4-Octet
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error!
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) as Integer // Parse the next 4 octets. Need to parse as Long then change to Integer or can get a numeric exception on negative numbers (odd!)
			// if(rValue & 0x8000_0000) rValue = rValue - 0xFFFF_FFFF -1 // Make into a negative if greater than 0x8000_0000
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00011: // Signed Integer, 8-Octet
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error!
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) as Long // Parse the next 8 octets then change to long.
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed
			return rValue
			break;

		case 0b00100: // Unsigned Integer, 1-Octet
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error!
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00101: // Unsigned Integer, 2-Octet
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error!
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00110: // Unsigned Integer, 4-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values!
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error!
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) // Parse the next 4 octets
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed
			break;
		case 0b00111: // Unsigned Integer, 8-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values!
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error!
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets as BigInteger.
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed
			break;

		case 0b01000: // Boolean False
			rValue = false; 
			break;
			case 0b01001: // Boolean True
			rValue = true; 
			break;

		case 0b01010: // Floating Point, 4-Octet Value
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error!
			rValue = Float.intBitsToFloat(Integer.parseInt(byteReverseParameters(valueString[0..7]), 16)) // Parse the next 4 octets
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed
			break;
		case 0b01011: // Floating Point, 8-Octet Value
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error!
			rValue = Double.longBitsToDouble(Long.parseLong(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed
			break;

		case 0b01100: // UTF-8 String, 1-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16)
			valueString = valueString.delete(0, 2)
            if (length == 0) { rValue = ""; break }
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                 
		case 0b01101: // UTF-8 String, 2-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16)
			valueString = valueString.delete(0, 4)
            if (length == 0) { rValue = ""; break }
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                             
		case 0b01110: // UTF-8 String, 4-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16)
			valueString = valueString.delete(0, 8)
            if (length == 0) { rValue = ""; break }
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                             
		case 0b01111: // UTF-8 String, 8-octet length
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16)
			valueString = valueString.delete(0, 16)
            if (length == 0) { rValue = ""; break }
			rValue = HexToString(valueString[0..((int)length*2-1)])
			valueString = valueString.delete(0, ((int)length*2))
			break;  
		 
		case 0b10000: // Octet String, 1-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16)
			valueString = valueString.delete(0, 2)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10001: // Octet String, 2-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16)
			valueString = valueString.delete(0, 4)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10010: // Octet String, 4-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16)
			valueString = valueString.delete(0, 8)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10011: // Octet String, 8-octet length
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16)
			valueString = valueString.delete(0, 16)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		 
		case 0b10100: // Null
			rValue = null; 
			break;
		 
		case 0b10101: // Structure
			 rValue = []
			   
			// Now add each sub-element to the structure. Maximum 100 times through the loop!
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container
			    // Recursively process the contents and push into the map rValue
			    rValue << parseToValue(valueString)
			}
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off!
			if(rValue.every{it instanceof Map}){
			     rValue = rValue.collectEntries({ it })
			}
			break;
		case 0b10110: // Array
			rValue = []
			// Now add each sub-element to the Array. Maximum 100 times through the loop!
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container
			    // Recursively process the contents and push into the map rValue
			    rValue << parseToValue(valueString)
			}
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off!
			break;
		case 0b10111: // List
			rValue = []
			// Now add each sub-element to the List. Maximum 100 times through the loop!
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container
			// Recursively process the contents and push into the map rValue
			rValue << parseToValue(valueString)
			}
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off!
			break;
		case 0b00011000: // End of container
			log.error "end-of-container encountered. Should have been caught in the struture, list, or array processing loop. What happened?"
			break;
		case 0b11001: // Reserved
		case 0b11010: // Reserved
		case 0b11011: // Reserved
		case 0b11100: // Reserved
		case 0b11101: // Reserved
		case 0b11110: // Reserved
		case 0b11111: // Reserved
			log.error "Received a Reserved value - Whaaaat?"; break
			rValue= null
			break;
		}   
        return rValue
    } catch(AssertionError e)  {
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}"
        return null
    }catch(e) {
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}"
        return null
    }
}
    
// This parser handles the Matter event message originating from Hubitat.
// valueString is the string description.value originally passed to the driver's parse(description) method from Hubitat
Object parseToValue(StringBuilder valueString) {
    if(valueString?.length() < 2) return null
    Integer controlOctet = Integer.parseInt(valueString[0..1], 16)
    assert !(isReservedValue(controlOctet)) // Should never get a reserved value!
    Integer elementType = controlOctet & 0b00011111
    Integer tagControl  = (controlOctet & 0b11100000) >> 5
    valueString.delete(0,2) // Delete the control octet since its been convereted to tagControl and ElementType
    Object tag = getTagValue(valueString, tagControl)
    Object element = getElementValue(valueString, elementType)
    return (tag.is(null)) ? (element) : [(tag):(element)]
}

Map parseRattrDescription(description){
    assert (description[0..8] == "read attr") 
    return description.substring( description.indexOf("-") +1).split(",")
                        .collectEntries{ entry -> def pair = entry.split(":");  
                            [(pair.first().trim()):(pair.last().trim())] 
                        }  
}

Map parseDescriptionAsDecodedMap(description){
    try {
        Map rattrKeyValues = parseRattrDescription(description)
        Map rValue = [:]
        rValue.put( ("clusterInt"),  Integer.parseInt(rattrKeyValues.cluster, 16) )
        rValue.put( ("attrInt"),     Integer.parseInt(rattrKeyValues.attrId, 16) )
        rValue.put( ("endpointInt"), Integer.parseInt(rattrKeyValues.endpoint, 16) )
    
        StringBuilder parseRattrString = new StringBuilder(rattrKeyValues.value)
        Object decodedValue = parseToValue(parseRattrString)
        rValue.put("decodedValue", decodedValue) 
        return rValue
    } catch(AssertionError e)  {
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}"
    } catch(e) {
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}"
    }
}
