library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Methods Common to Matter Drivers",
        name: "parseDescriptionAsDecodedMap",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

Boolean isReservedValue(Integer controlOctet){ 
    // Per Matter Spec Appendix A.6, values greater than 0b11000 are reserved, except for 0b00011000 which is End-of-Container
    return  ( ((controlOctet & 0b00011111) >= (0b11000)) && !(controlOctet == 0b00011111))
}

String HexToString(String hexStringToDecode){
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int i = 0; i < hexStringToDecode.length(); i += 2) {
      String str = hexStringToDecode.substring(i, i + 2);
      int byteVal = Integer.parseInt(str, 16);
      baos.write(byteVal);
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
            rValue = null
            break;
        case 0b001: // Context-specific, 1 octet
            rValue = Integer.parseInt(valueString[0..1] , 16)
            valueString.delete(0,2)
            break
        case 0b010: // Common Profile, 2 octets
            rValue = valueString[0..3] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0,4)
            break
        case 0b011: // Common Profile, 4 octets
            rValue = valueString[0..7] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0,8)
            break
        case 0b100: // Implicit Profile, 2 octets
            rValue = valueString[0..3] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0,4)
            break
        case 0b101: // Implicit Profile, 4 octets
            rValue = valueString[0..7] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0,8)
            break
        case 0b110: // Fully-Qualified form, 6 octets
            rValue = valueString[0..11] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0,12)
            break
        case 0b111: // Fully-Qualified form, 8 octets
            rValue = valueString[0..15] // Not really sure how this should be represented. For now, using a string!
            valueString.delete(0, 16)
            break
    }
    return rValue
}

// Strings are immutable in groovy, but StringBuilder strings are not. 
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type strin
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
			rValue = Long.parseLong(byteReverseParameters(valueString[0..15]), 16) // Parse the next 8 octets
			if (rValue & 0x8000000000000000) log.warn "Careful - in method parseToValue:getElementValue processing an unsigned long but first bit indicates negative."
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
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]))
			valueString = valueString.delete(0, 2)
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                 
		case 0b01101: // UTF-8 String, 2-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]))
			valueString = valueString.delete(0, 4)
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                             
		case 0b01110: // UTF-8 String, 4-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]))
			valueString = valueString.delete(0, 8)
			rValue = HexToString(valueString[0..(length*2-1)])
			valueString = valueString.delete(0, (length*2))
			break;                             
		case 0b01111: // UTF-8 String, 8-octet length
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]))
			valueString = valueString.delete(0, 16)
			rValue = HexToString(valueString[0..((int)length*2-1)])
			valueString = valueString.delete(0, ((int)length*2))
			break;  
		 
		case 0b10000: // Octet String, 1-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]))
			valueString = valueString.delete(0, 2)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10001: // Octet String, 2-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]))
			valueString = valueString.delete(0, 4)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10010: // Octet String, 4-octet length
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]))
			valueString = valueString.delete(0, 8)
			rValue = new byte[length]
			for(i = 0; i<length; i++) { 
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte
			}
			valueString = valueString.delete(0, ((int)length*2))
			break;
		case 0b10011: // Octet String, 8-octet length
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]))
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
        Map rValue = parseRattrDescription(description)
        rValue.put( ("clusterInt"),  Integer.parseInt(rValue.cluster, 16) )
        rValue.put( ("attrInt"),     Integer.parseInt(rValue.attrId, 16) )
        rValue.put( ("endpointInt"), Integer.parseInt(rValue.endpoint, 16) )
    
        StringBuilder parseRattrString = new StringBuilder(rValue.value)
        Object decodedValue = parseToValue(parseRattrString)
        rValue.put("decodedValue", decodedValue) 
        return rValue
    } catch(AssertionError e)  {
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}"
    } catch(e) {
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}"
    }
}