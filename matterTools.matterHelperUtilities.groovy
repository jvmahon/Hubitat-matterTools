library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Formats Matter Commands",
        name: "matterHelperUtilities",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

import groovy.transform.Field
import  hubitat.matter.DataType

@Field static final Map matterAttributeInfo = [
    0x0003:[
        0x0000:[name:"IdentifyTime", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName: null ], 
        0x0001:[name:"IdentifyType", hubitatDataType:DataType.UINT8, access:"R", hubitatEventName: null ], 
        ],
    0x0006:[
        0x0000:[name:"OnOff",              hubitatDataType:DataType.UINT16,    access:"R", hubitatEventName:"switch"], 
        0x0001:[name:"GlobalSceneControl", hubitatDataType:DataType.UINT16,    access:"R", hubitatEventName: null ], 
        0x0002:[name:"OnTime",             hubitatDataType:DataType.UINT16,    access:"RW", hubitatEventName: null ], 
        0x0003:[name:"OffWaitTime",        hubitatDataType:DataType.UINT16,    access:"RW", hubitatEventName: null ], 
        0x0004:[name:"StartUpOnOff",       hubitatDataType:DataType.UINT8,     access:"RW", hubitatEventName: null ], 
       ],
    0x0008:[
        0x0000:[name:"CurrentLevel",            hubitatDataType:DataType.UINT8,  access:"R", hubitatEventName:"level"], 
        0x0001:[name:"RemainingTime",           hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0002:[name:"MinLevel",                hubitatDataType:DataType.UINT8,  access:"R", hubitatEventName:""], 
        0x0003:[name:"MaxLevel",                hubitatDataType:DataType.UINT8,  access:"R", hubitatEventName:""], 
        0x0004:[name:"CurrentFrequency",        hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0005:[name:"MinFrequency",            hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0006:[name:"MaxFrequency",            hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0010:[name:"OnOffTransitionTime",     hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0011:[name:"OnLevel",                 hubitatDataType:DataType.UINT8,  access:"RW", hubitatEventName:""], 
        0x0012:[name:"OnTransitionTime",        hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0013:[name:"OffTransitionTime",       hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0014:[name:"DefaultMoveRate",         hubitatDataType:DataType.UINT8,  access:"RW", hubitatEventName:""], 
        0x000F:[name:"Options",                 hubitatDataType:DataType.UINT8,  access:"RW", hubitatEventName:""], 
        0x4000:[name:"StartUpCurrentLevel",     hubitatDataType:DataType.UINT8,  access:"RW", hubitatEventName:""]
       ],
    0x0300:[
        0x0000:[name:"CurrentHue",               hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:"hue"], 
        0x0001:[name:"CurrentSaturation",        hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:"saturation"], 
        0x0002:[name:"RemainingTime",            hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0003:[name:"CurrentX",                 hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0004:[name:"CurrentY",                 hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0005:[name:"DriftCompensation",        hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:""],
        0x0006:[name:"CompensationText",         hubitatDataType:DataType.ARRAY, access:"R", hubitatEventName:""],  // Uncertain about this hubitatDataType!
        0x0007:[name:"ColorTemperatureMireds",   hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x0008:[name:"ColorMode",                hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:""],
        0x000F:[name:"Options",                  hubitatDataType:DataType.UINT8,  access:"RW", hubitatEventName:""], 

        0x4000:[name:"EnhancedCurrentHue",            hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x4001:[name:"EnhancedColorMode",             hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:""], 
        0x4002:[name:"ColorLoopActive",               hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:""], 
        0x4003:[name:"ColorLoopDirection",            hubitatDataType:DataType.UINT8, access:"R", hubitatEventName:""], 
        0x4004:[name:"ColorLoopTime",                 hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x4005:[name:"ColorLoopStartEnhancedHue",     hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x4006:[name:"ColorLoopStoredEnhancedHue",    hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        
        0x400A:[name:"ColorCapabilities",             hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x400B:[name:"ColorTempPhysicalMinMireds",    hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x400C:[name:"ColorTempPhysicalMaxMireds",    hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        0x400D:[name:"ColorLoopStoredEnhancedHue",    hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""], 
        
        0x4010:[name:"CoupleColorTempToLevelMinMireds", hubitatDataType:DataType.UINT16, access:"R", hubitatEventName:""]

       ],
/* the follwoing is a template to add new types.
      0xXXXX:[
        0x0000:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0001:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0002:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0003:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0004:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0005:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0006:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0007:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0008:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x0009:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000A:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000B:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000C:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000D:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000E:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""], 
        0x000F:[name:"", hubitatDataType:DataType.UINT16, access:"RW", hubitatEventName:""]
       ], 
*/  
]

Integer getHubitatDataType(Map inputs = [:]){
    assert inputs.keySet().containsAll(["clusterInt", "attrInt"])
    return matterAttributeInfo.get(inputs.clusterInt).get(inputs.attrInt).get("hubitatDataType")
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

void writeClusterAttribute(clusterId, attributeId, hexValue, dataType) { 
    writeClusterAttribute(
            ep: null,
            clusterInt: Integer.parseInt( clusterId, 16),
            attributeInt: Integer.parseInt( attributeId, 16), 
            hexValue:hexValue, 
            hubitatDataType: dataType 
    ) 
}
void writeClusterAttribute(Map params = [:]) {
    try {
	    Map inputs = [ep: null, clusterInt: null , attributeInt: null , hexValue: null] << params
        assert inputs.keySet().containsAll(params.keySet()) // checks that all user-specified parameters use permitted labels.
        assert inputs.ep instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.attributeInt instanceof Integer
        
        List<Map<String, String>> attrWriteRequests = []
            attrWriteRequests.add(matter.attributeWriteRequest(inputs.ep, inputs.clusterInt, inputs.attributeInt, getHubitatDataType(*:inputs), inputs.hexValue))
        String cmd = matter.writeAttributes(attrWriteRequests)

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<pre>${e}"
        log.error getStackTrace(e)
        throw(e)   
    } catch(e) {
        log.error "Caught error in function writeClusterAttribute: <pre>${e}"
        log.error getStackTrace(e)
        throw(e)
    }
}

void readClusterAttribute(clusterId, attributeId) {readClusterAttribute(clusterId:clusterId, attributeId:attributeId)}
void readClusterAttribute(Map params = [:]) {
    try {
	    Map inputs = [ep:null, clusterInt:null, attributeInt:null ] << params
        assert inputs.ep instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.attributeInt instanceof Integer
    
        List<Map<String, String>> attributePaths = []
        attributePaths.add(matter.attributePath(inputs.ep, inputs.clusterInt, inputs.attributeInt))

        String cmd = matter.readAttributes(attributePaths)
	
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
        
    } catch(AssertionError e) {
        log.error "<pre>${e}"
        log.error getStackTrace(e)
        throw(e)   
    } catch(e) {
        log.error "Caught error in function writeClusterAttribute: <pre>${e}"
        log.error getStackTrace(e)
        throw(e)
    }
}
