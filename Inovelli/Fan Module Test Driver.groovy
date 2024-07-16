/*
Copyright James V. Mahon
Distribution requires permission / Not for Commercial distribution
*/



metadata {
    definition (name: "Inovelli Fan Module Test Driver", namespace: "matterTools", author: "jvm33") {
        capability "FanControl"
        capability "Initialize"
        capability "Refresh"
        
        attribute "level", "number"
        
        command "matterUnsubscribe", [[name:"Unsubscribe Device", description:"A debugging tool. Choose this to unsubscribe from Matter attribute reports."]]
    }
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String description) {
    Map decodedDescriptionMap = parseDescriptionAsDecodedMap(description) // Using parser from matterTools.parseDescriptionAsDecodedMap
    log.info "${device.displayName}: Received report: <font color='blue'>${decodedDescriptionMap}"
    if (decodedDescriptionMap.clusterInt == 0x0202) {
        Map rValue = [:]
        switch (decodedDescriptionMap.attrInt){
            case 0x0000:
                Map speeds =  [0:"off", 1:"low", 2:"medium", 3:"high", 4:"on", 5:"auto", 6:"smart"]
                rValue = [name:"speed" , value: speeds.get(decodedDescriptionMap.decodedValue)]
                break
            case 0x0003:
                rValue = [name:"level" , value:decodedDescriptionMap.decodedValue]
                break
        }
        sendEvent(rValue)
    }
}

void matterUnsubscribe(){
    log.info "${device.displayName}: Clearing Matter device subscriptions"
    if (txtEnable) log.info "${device.displayName}: Unsubscribing from Matter attribute changes."
    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) // unsubscribe
}

void initialize(){
    log.info "${device.displayName}: Subscribing to reports for Fan cluster with a 0 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0000 0x0700 [{"ep":"0xFFFF","cluster":"0x0202","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}


void refresh() {
    log.info "${device.displayName}: Refreshing Mode Select Data on All Endpoints."
    refreshMatter(ep:0xFFFF, clusterInt: 0x0050, attrInt: 0xFFFFFFFF)
}

void setSpeed(){
    log.error "Speed setting not implemented"
    
}

void cycleSpeed() {
    log.error "cycle speed setting not implemented"
}

// ~~~~~ start include (18) matterTools.parseDescriptionAsDecodedMap ~~~~~
library ( // library marker matterTools.parseDescriptionAsDecodedMap, line 1
        base: "driver", // library marker matterTools.parseDescriptionAsDecodedMap, line 2
        author: "jvm33", // library marker matterTools.parseDescriptionAsDecodedMap, line 3
        category: "matter", // library marker matterTools.parseDescriptionAsDecodedMap, line 4
        description: "Methods Common to Matter Drivers", // library marker matterTools.parseDescriptionAsDecodedMap, line 5
        name: "parseDescriptionAsDecodedMap", // library marker matterTools.parseDescriptionAsDecodedMap, line 6
        namespace: "matterTools", // library marker matterTools.parseDescriptionAsDecodedMap, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.parseDescriptionAsDecodedMap, line 8
		version: "0.0.1" // library marker matterTools.parseDescriptionAsDecodedMap, line 9
) // library marker matterTools.parseDescriptionAsDecodedMap, line 10

// Per Matter Spec Appendix A.6, values greater than 0b11000 are reserved, except for 0b00011000 which is End-of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 12
Boolean isReservedValue(Integer controlOctet){  // library marker matterTools.parseDescriptionAsDecodedMap, line 13
    return  ( ((controlOctet & 0b00011111) >= (0b11000)) && !(controlOctet == 0b00011111)) // library marker matterTools.parseDescriptionAsDecodedMap, line 14
} // library marker matterTools.parseDescriptionAsDecodedMap, line 15

String HexToString(String hexStr){ // library marker matterTools.parseDescriptionAsDecodedMap, line 17
    ByteArrayOutputStream baos = new ByteArrayOutputStream(); // library marker matterTools.parseDescriptionAsDecodedMap, line 18
    for (int i = 0; i < hexStr.length(); i += 2) { // library marker matterTools.parseDescriptionAsDecodedMap, line 19
      baos.write(Integer.parseInt(hexStr.substring(i, i + 2), 16)); // library marker matterTools.parseDescriptionAsDecodedMap, line 20
    }  // library marker matterTools.parseDescriptionAsDecodedMap, line 21
    return baos.toString() // library marker matterTools.parseDescriptionAsDecodedMap, line 22
    // return new String(baos.toByteArray() );      // library marker matterTools.parseDescriptionAsDecodedMap, line 23
} // library marker matterTools.parseDescriptionAsDecodedMap, line 24

// Strings are immutable in groovy, but StringBuilder strings are not.  // library marker matterTools.parseDescriptionAsDecodedMap, line 26
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string. // library marker matterTools.parseDescriptionAsDecodedMap, line 27
Object getTagValue(StringBuilder valueString, Integer tagControl){ // library marker matterTools.parseDescriptionAsDecodedMap, line 28
    Object rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 29
    switch(tagControl){ // library marker matterTools.parseDescriptionAsDecodedMap, line 30
        case 0b000: // 0 Octets // library marker matterTools.parseDescriptionAsDecodedMap, line 31
            rValue = null; break; // library marker matterTools.parseDescriptionAsDecodedMap, line 32
        case 0b001: // Context-specific, 1 octet // library marker matterTools.parseDescriptionAsDecodedMap, line 33
            rValue = Integer.parseInt(valueString[0..1] , 16); valueString.delete(0,2); break // library marker matterTools.parseDescriptionAsDecodedMap, line 34
        case 0b010: // Common Profile, 2 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 35
            rValue = valueString[0..3]; valueString.delete(0,4);  break // library marker matterTools.parseDescriptionAsDecodedMap, line 36
        case 0b011: // Common Profile, 4 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 37
            rValue = valueString[0..7]; valueString.delete(0,8); break // library marker matterTools.parseDescriptionAsDecodedMap, line 38
        case 0b100: // Implicit Profile, 2 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 39
            rValue = valueString[0..3]; valueString.delete(0,4); break // library marker matterTools.parseDescriptionAsDecodedMap, line 40
        case 0b101: // Implicit Profile, 4 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 41
            rValue = valueString[0..7]; valueString.delete(0,8); break // library marker matterTools.parseDescriptionAsDecodedMap, line 42
        case 0b110: // Fully-Qualified form, 6 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 43
            rValue = valueString[0..11];  valueString.delete(0,12); break // library marker matterTools.parseDescriptionAsDecodedMap, line 44
        case 0b111: // Fully-Qualified form, 8 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 45
            rValue = valueString[0..15];  valueString.delete(0, 16); break // library marker matterTools.parseDescriptionAsDecodedMap, line 46
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 47
    return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 48
} // library marker matterTools.parseDescriptionAsDecodedMap, line 49

// Strings are immutable in groovy, but StringBuilder strings are not.  // library marker matterTools.parseDescriptionAsDecodedMap, line 51
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string // library marker matterTools.parseDescriptionAsDecodedMap, line 52
Object getElementValue(StringBuilder valueString, Integer elementType){ // library marker matterTools.parseDescriptionAsDecodedMap, line 53
    Object rValue = null // library marker matterTools.parseDescriptionAsDecodedMap, line 54
    try { // library marker matterTools.parseDescriptionAsDecodedMap, line 55
		switch(elementType){ // library marker matterTools.parseDescriptionAsDecodedMap, line 56
		case 0b00000: // Signed Integer, 1-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 57
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 58
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet // library marker matterTools.parseDescriptionAsDecodedMap, line 59
			if(rValue & 0x80) rValue = rValue - 256 // Make into a negative if greater than 0x80 // library marker matterTools.parseDescriptionAsDecodedMap, line 60
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 61
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 62
		case 0b00001: // Signed Integer, 2-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 63
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 64
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 65
			if(rValue & 0x8000) rValue = rValue - 65536 // Make into a negative if greater than 0x8000 // library marker matterTools.parseDescriptionAsDecodedMap, line 66
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 67
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 68
		case 0b00010: // Signed Integer, 4-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 69
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 70
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) as Integer // Parse the next 4 octets. Need to parse as Long then change to Integer or can get a numeric exception on negative numbers (odd!) // library marker matterTools.parseDescriptionAsDecodedMap, line 71
			// if(rValue & 0x8000_0000) rValue = rValue - 0xFFFF_FFFF -1 // Make into a negative if greater than 0x8000_0000 // library marker matterTools.parseDescriptionAsDecodedMap, line 72
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 73
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 74
		case 0b00011: // Signed Integer, 8-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 75
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 76
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) as Long // Parse the next 8 octets then change to long. // library marker matterTools.parseDescriptionAsDecodedMap, line 77
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 78
			return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 79
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 80

		case 0b00100: // Unsigned Integer, 1-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 82
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 83
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet // library marker matterTools.parseDescriptionAsDecodedMap, line 84
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 85
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 86
		case 0b00101: // Unsigned Integer, 2-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 87
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 88
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 89
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 90
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 91
		case 0b00110: // Unsigned Integer, 4-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values! // library marker matterTools.parseDescriptionAsDecodedMap, line 92
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 93
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) // Parse the next 4 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 94
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 95
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 96
		case 0b00111: // Unsigned Integer, 8-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values! // library marker matterTools.parseDescriptionAsDecodedMap, line 97
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 98
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets as BigInteger. // library marker matterTools.parseDescriptionAsDecodedMap, line 99
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 100
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 101

		case 0b01000: // Boolean False // library marker matterTools.parseDescriptionAsDecodedMap, line 103
			rValue = false;  // library marker matterTools.parseDescriptionAsDecodedMap, line 104
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 105
			case 0b01001: // Boolean True // library marker matterTools.parseDescriptionAsDecodedMap, line 106
			rValue = true;  // library marker matterTools.parseDescriptionAsDecodedMap, line 107
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 108

		case 0b01010: // Floating Point, 4-Octet Value // library marker matterTools.parseDescriptionAsDecodedMap, line 110
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 111
			rValue = Float.intBitsToFloat(Integer.parseInt(byteReverseParameters(valueString[0..7]), 16)) // Parse the next 4 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 112
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 113
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 114
		case 0b01011: // Floating Point, 8-Octet Value // library marker matterTools.parseDescriptionAsDecodedMap, line 115
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 116
			rValue = Double.longBitsToDouble(Long.parseLong(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 117
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 118
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 119

		case 0b01100: // UTF-8 String, 1-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 121
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 122
			valueString = valueString.delete(0, 2) // library marker matterTools.parseDescriptionAsDecodedMap, line 123
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 124
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 125
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 126
			break;                  // library marker matterTools.parseDescriptionAsDecodedMap, line 127
		case 0b01101: // UTF-8 String, 2-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 128
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 129
			valueString = valueString.delete(0, 4) // library marker matterTools.parseDescriptionAsDecodedMap, line 130
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 131
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 132
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 133
			break;                              // library marker matterTools.parseDescriptionAsDecodedMap, line 134
		case 0b01110: // UTF-8 String, 4-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 135
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 136
			valueString = valueString.delete(0, 8) // library marker matterTools.parseDescriptionAsDecodedMap, line 137
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 138
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 139
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 140
			break;                              // library marker matterTools.parseDescriptionAsDecodedMap, line 141
		case 0b01111: // UTF-8 String, 8-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 142
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 143
			valueString = valueString.delete(0, 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 144
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 145
			rValue = HexToString(valueString[0..((int)length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 146
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 147
			break;   // library marker matterTools.parseDescriptionAsDecodedMap, line 148

		case 0b10000: // Octet String, 1-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 150
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 151
			valueString = valueString.delete(0, 2) // library marker matterTools.parseDescriptionAsDecodedMap, line 152
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 153
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 154
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 155
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 156
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 157
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 158
		case 0b10001: // Octet String, 2-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 159
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 160
			valueString = valueString.delete(0, 4) // library marker matterTools.parseDescriptionAsDecodedMap, line 161
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 162
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 163
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 164
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 165
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 166
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 167
		case 0b10010: // Octet String, 4-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 168
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 169
			valueString = valueString.delete(0, 8) // library marker matterTools.parseDescriptionAsDecodedMap, line 170
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 171
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 172
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 173
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 174
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 175
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 176
		case 0b10011: // Octet String, 8-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 177
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 178
			valueString = valueString.delete(0, 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 179
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 180
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 181
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 182
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 183
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 184
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 185

		case 0b10100: // Null // library marker matterTools.parseDescriptionAsDecodedMap, line 187
			rValue = null;  // library marker matterTools.parseDescriptionAsDecodedMap, line 188
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 189

		case 0b10101: // Structure // library marker matterTools.parseDescriptionAsDecodedMap, line 191
			 rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 192

			// Now add each sub-element to the structure. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 194
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 195
			    // Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 196
			    rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 197
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 198
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 199
			if(rValue.every{it instanceof Map}){ // library marker matterTools.parseDescriptionAsDecodedMap, line 200
			     rValue = rValue.collectEntries({ it }) // library marker matterTools.parseDescriptionAsDecodedMap, line 201
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 202
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 203
		case 0b10110: // Array // library marker matterTools.parseDescriptionAsDecodedMap, line 204
			rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 205
			// Now add each sub-element to the Array. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 206
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 207
			    // Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 208
			    rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 209
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 210
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 211
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 212
		case 0b10111: // List // library marker matterTools.parseDescriptionAsDecodedMap, line 213
			rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 214
			// Now add each sub-element to the List. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 215
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 216
			// Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 217
			rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 218
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 219
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 220
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 221
		case 0b00011000: // End of container // library marker matterTools.parseDescriptionAsDecodedMap, line 222
			log.error "end-of-container encountered. Should have been caught in the struture, list, or array processing loop. What happened?" // library marker matterTools.parseDescriptionAsDecodedMap, line 223
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 224
		case 0b11001: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 225
		case 0b11010: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 226
		case 0b11011: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 227
		case 0b11100: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 228
		case 0b11101: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 229
		case 0b11110: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 230
		case 0b11111: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 231
			log.error "Received a Reserved value - Whaaaat?"; break // library marker matterTools.parseDescriptionAsDecodedMap, line 232
			rValue= null // library marker matterTools.parseDescriptionAsDecodedMap, line 233
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 234
		}    // library marker matterTools.parseDescriptionAsDecodedMap, line 235
        return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 236
    } catch(AssertionError e)  { // library marker matterTools.parseDescriptionAsDecodedMap, line 237
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 238
        return null // library marker matterTools.parseDescriptionAsDecodedMap, line 239
    }catch(e) { // library marker matterTools.parseDescriptionAsDecodedMap, line 240
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 241
        return null // library marker matterTools.parseDescriptionAsDecodedMap, line 242
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 243
} // library marker matterTools.parseDescriptionAsDecodedMap, line 244

// This parser handles the Matter event message originating from Hubitat. // library marker matterTools.parseDescriptionAsDecodedMap, line 246
// valueString is the string description.value originally passed to the driver's parse(description) method from Hubitat // library marker matterTools.parseDescriptionAsDecodedMap, line 247
Object parseToValue(StringBuilder valueString) { // library marker matterTools.parseDescriptionAsDecodedMap, line 248
    if(valueString?.length() < 2) return null // library marker matterTools.parseDescriptionAsDecodedMap, line 249
    Integer controlOctet = Integer.parseInt(valueString[0..1], 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 250
    assert !(isReservedValue(controlOctet)) // Should never get a reserved value! // library marker matterTools.parseDescriptionAsDecodedMap, line 251
    Integer elementType = controlOctet & 0b00011111 // library marker matterTools.parseDescriptionAsDecodedMap, line 252
    Integer tagControl  = (controlOctet & 0b11100000) >> 5 // library marker matterTools.parseDescriptionAsDecodedMap, line 253
    valueString.delete(0,2) // Delete the control octet since its been convereted to tagControl and ElementType // library marker matterTools.parseDescriptionAsDecodedMap, line 254
    Object tag = getTagValue(valueString, tagControl) // library marker matterTools.parseDescriptionAsDecodedMap, line 255
    Object element = getElementValue(valueString, elementType) // library marker matterTools.parseDescriptionAsDecodedMap, line 256
    return (tag.is(null)) ? (element) : [(tag):(element)] // library marker matterTools.parseDescriptionAsDecodedMap, line 257
} // library marker matterTools.parseDescriptionAsDecodedMap, line 258

Map parseRattrDescription(description){ // library marker matterTools.parseDescriptionAsDecodedMap, line 260
    assert (description[0..8] == "read attr")  // library marker matterTools.parseDescriptionAsDecodedMap, line 261
    return description.substring( description.indexOf("-") +1).split(",") // library marker matterTools.parseDescriptionAsDecodedMap, line 262
                        .collectEntries{ entry -> def pair = entry.split(":");   // library marker matterTools.parseDescriptionAsDecodedMap, line 263
                            [(pair.first().trim()):(pair.last().trim())]  // library marker matterTools.parseDescriptionAsDecodedMap, line 264
                        }   // library marker matterTools.parseDescriptionAsDecodedMap, line 265
} // library marker matterTools.parseDescriptionAsDecodedMap, line 266

Map parseDescriptionAsDecodedMap(description){ // library marker matterTools.parseDescriptionAsDecodedMap, line 268
    try { // library marker matterTools.parseDescriptionAsDecodedMap, line 269
        Map rattrKeyValues = parseRattrDescription(description) // library marker matterTools.parseDescriptionAsDecodedMap, line 270
        Map rValue = [:] // library marker matterTools.parseDescriptionAsDecodedMap, line 271
        rValue.put( ("clusterInt"),  Integer.parseInt(rattrKeyValues.cluster, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 272
        rValue.put( ("attrInt"),     Integer.parseInt(rattrKeyValues.attrId, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 273
        rValue.put( ("endpointInt"), Integer.parseInt(rattrKeyValues.endpoint, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 274

        StringBuilder parseRattrString = new StringBuilder(rattrKeyValues.value) // library marker matterTools.parseDescriptionAsDecodedMap, line 276
        Object decodedValue = parseToValue(parseRattrString) // library marker matterTools.parseDescriptionAsDecodedMap, line 277
        rValue.put("decodedValue", decodedValue)  // library marker matterTools.parseDescriptionAsDecodedMap, line 278
        return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 279
    } catch(AssertionError e)  { // library marker matterTools.parseDescriptionAsDecodedMap, line 280
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 281
    } catch(e) { // library marker matterTools.parseDescriptionAsDecodedMap, line 282
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 283
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 284
} // library marker matterTools.parseDescriptionAsDecodedMap, line 285

// ~~~~~ end include (18) matterTools.parseDescriptionAsDecodedMap ~~~~~

// ~~~~~ start include (7) matterTools.matterHelperUtilities ~~~~~
library ( // library marker matterTools.matterHelperUtilities, line 1
        base: "driver", // library marker matterTools.matterHelperUtilities, line 2
        author: "jvm33", // library marker matterTools.matterHelperUtilities, line 3
        category: "matter", // library marker matterTools.matterHelperUtilities, line 4
        description: "Formats Matter Commands", // library marker matterTools.matterHelperUtilities, line 5
        name: "matterHelperUtilities", // library marker matterTools.matterHelperUtilities, line 6
        namespace: "matterTools", // library marker matterTools.matterHelperUtilities, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.matterHelperUtilities, line 8
		version: "0.0.1" // library marker matterTools.matterHelperUtilities, line 9
) // library marker matterTools.matterHelperUtilities, line 10
import groovy.transform.Field // library marker matterTools.matterHelperUtilities, line 11
import  hubitat.matter.DataType // library marker matterTools.matterHelperUtilities, line 12

// Matter payloads need hex parameters of greater than 2 characters to be pair-reversed. // library marker matterTools.matterHelperUtilities, line 14
// This function takes a list of parameters and pair-reverses those longer than 2 characters. // library marker matterTools.matterHelperUtilities, line 15
// Alternatively, it can take a string and pair-revers that. // library marker matterTools.matterHelperUtilities, line 16
// Thus, e.g., ["0123", "456789", "10"] becomes "230189674510" and "123456" becomes "563412" // library marker matterTools.matterHelperUtilities, line 17
private String byteReverseParameters(String oneString) { byteReverseParameters([] << oneString) } // library marker matterTools.matterHelperUtilities, line 18
private String byteReverseParameters(List<String> parameters) { // library marker matterTools.matterHelperUtilities, line 19
	StringBuilder rStr = new StringBuilder(64) // library marker matterTools.matterHelperUtilities, line 20
	for (hexString in parameters) { // library marker matterTools.matterHelperUtilities, line 21
		if (hexString.length() % 2) throw new Exception("In method byteReverseParameters, trying to reverse a hex string that is not an even number of characters in length. Error in Hex String: ${hexString}, All method parameters were ${parameters}.") // library marker matterTools.matterHelperUtilities, line 22

		for(Integer i = hexString.length() -1 ; i > 0 ; i -= 2) { // library marker matterTools.matterHelperUtilities, line 24
			rStr << hexString[i-1..i] // library marker matterTools.matterHelperUtilities, line 25
		}	 // library marker matterTools.matterHelperUtilities, line 26
	} // library marker matterTools.matterHelperUtilities, line 27
	return rStr // library marker matterTools.matterHelperUtilities, line 28
} // library marker matterTools.matterHelperUtilities, line 29

// Performs a refresh on a designated endpoint / cluster / attribute (all specified in Integer) // library marker matterTools.matterHelperUtilities, line 31
// Does a wildcard refresh if parameters are not specified (ep=FFFF / cluster=FFFFFFFF/ endpoint=FFFFFFFF is the Matter wildcard designation // library marker matterTools.matterHelperUtilities, line 32
void refreshMatter(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 33
    try { // library marker matterTools.matterHelperUtilities, line 34
        Map inputs = [ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF] << params // library marker matterTools.matterHelperUtilities, line 35
        assert inputs.ep instanceof Integer         // Make sure the type is as expected!  // library marker matterTools.matterHelperUtilities, line 36
        assert inputs.clusterInt instanceof Integer || inputs.clusterInt instanceof Long // library marker matterTools.matterHelperUtilities, line 37
        assert inputs.attrInt instanceof Integer || inputs.attrInt instanceof Long // library marker matterTools.matterHelperUtilities, line 38

       // Groovy Slashy String form of a GString  https://docs.groovy-lang.org/latest/html/documentation/#_slashy_string // library marker matterTools.matterHelperUtilities, line 40
        String cmd = /he rattrs [{"ep":"${inputs.ep}","cluster":"${inputs.clusterInt}","attr":"${inputs.attrInt}"}]/ // library marker matterTools.matterHelperUtilities, line 41

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 43
    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 44
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 45
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 46
        log.error "<pre>${e}<br><br>when processing refreshMatter with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 47
    }    // library marker matterTools.matterHelperUtilities, line 48
} // library marker matterTools.matterHelperUtilities, line 49


void writeClusterAttribute(clusterId, attributeId, hexValue, dataType) {  // library marker matterTools.matterHelperUtilities, line 52
    writeClusterAttribute( // library marker matterTools.matterHelperUtilities, line 53
            ep: null, // library marker matterTools.matterHelperUtilities, line 54
            clusterInt: Integer.parseInt( clusterId, 16), // library marker matterTools.matterHelperUtilities, line 55
            attributeInt: Integer.parseInt( attributeId, 16),  // library marker matterTools.matterHelperUtilities, line 56
            hexValue:hexValue,  // library marker matterTools.matterHelperUtilities, line 57
            hubitatDataType: dataType  // library marker matterTools.matterHelperUtilities, line 58
    )  // library marker matterTools.matterHelperUtilities, line 59
} // library marker matterTools.matterHelperUtilities, line 60
void writeClusterAttribute(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 61
    try { // library marker matterTools.matterHelperUtilities, line 62
	    Map inputs = [ep: null, clusterInt: null , attributeInt: null , hexValue: null] << params // library marker matterTools.matterHelperUtilities, line 63
        assert inputs.ep instanceof Integer // library marker matterTools.matterHelperUtilities, line 64
        assert inputs.clusterInt instanceof Integer // library marker matterTools.matterHelperUtilities, line 65
        assert inputs.attributeInt instanceof Integer // library marker matterTools.matterHelperUtilities, line 66

        List<Map<String, String>> attrWriteRequests = [] // library marker matterTools.matterHelperUtilities, line 68
            attrWriteRequests.add(matter.attributeWriteRequest(inputs.ep, inputs.clusterInt, inputs.attributeInt, getHubitatDataType(*:inputs), inputs.hexValue)) // library marker matterTools.matterHelperUtilities, line 69
        String cmd = matter.writeAttributes(attrWriteRequests) // library marker matterTools.matterHelperUtilities, line 70

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 72
    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 73
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 74
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 75
        log.error "<pre>${e}<br><br>when processing writeClusterAttribute with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 76
    }    // library marker matterTools.matterHelperUtilities, line 77
} // library marker matterTools.matterHelperUtilities, line 78

void readClusterAttribute(clusterId, attributeId) {readClusterAttribute(clusterId:clusterId, attributeId:attributeId)} // library marker matterTools.matterHelperUtilities, line 80
void readClusterAttribute(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 81
    try { // library marker matterTools.matterHelperUtilities, line 82
	    Map inputs = [ep:null, clusterInt:null, attributeInt:null ] << params // library marker matterTools.matterHelperUtilities, line 83
        assert inputs.ep instanceof Integer // library marker matterTools.matterHelperUtilities, line 84
        assert inputs.clusterInt instanceof Integer || instance.clusterInt instanceof Long // library marker matterTools.matterHelperUtilities, line 85
        assert inputs.attributeInt instanceof Integer || instance.attributeInt instanceof Long // library marker matterTools.matterHelperUtilities, line 86

        List<Map<String, String>> attributePaths = [] // library marker matterTools.matterHelperUtilities, line 88
        attributePaths.add(matter.attributePath(inputs.ep, inputs.clusterInt, inputs.attributeInt)) // library marker matterTools.matterHelperUtilities, line 89

        String cmd = matter.readAttributes(attributePaths) // library marker matterTools.matterHelperUtilities, line 91

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 93

    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 95
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 96
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 97
        log.error "<pre>${e}<br><br>when processing readClusterAttribute with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 98
    }    // library marker matterTools.matterHelperUtilities, line 99
} // library marker matterTools.matterHelperUtilities, line 100

// ~~~~~ end include (7) matterTools.matterHelperUtilities ~~~~~
