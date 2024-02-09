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

// Performs a refresh on a designated endpoint / cluster / attribute (all specified in Integer)
// Does a wildcard refresh if parameters are not specified (ep=FFFF / cluster=FFFFFFFF/ endpoint=FFFFFFFF is the Matter wildcard designation
void refreshMatter(Map params = [:]) {
    try {
        Map inputs = [ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF] << params
        assert inputs.ep instanceof Integer         // Make sure the type is as expected! 
        assert inputs.clusterInt instanceof Integer || inputs.clusterInt instanceof Long
        assert inputs.attrInt instanceof Integer || inputs.attrInt instanceof Long
        
        String cmd = 'he rattrs [{"ep":"' + inputs.ep  +'","cluster":"' + inputs.clusterInt    + '","attr":"' + inputs.attrInt    + '"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
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
        assert inputs.ep instanceof Integer
        assert inputs.clusterInt instanceof Integer
        assert inputs.attributeInt instanceof Integer
        
        List<Map<String, String>> attrWriteRequests = []
            attrWriteRequests.add(matter.attributeWriteRequest(inputs.ep, inputs.clusterInt, inputs.attributeInt, getHubitatDataType(*:inputs), inputs.hexValue))
        String cmd = matter.writeAttributes(attrWriteRequests)

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

void readClusterAttribute(clusterId, attributeId) {readClusterAttribute(clusterId:clusterId, attributeId:attributeId)}
void readClusterAttribute(Map params = [:]) {
    try {
	    Map inputs = [ep:null, clusterInt:null, attributeInt:null ] << params
        assert inputs.ep instanceof Integer
        assert inputs.clusterInt instanceof Integer || instance.clusterInt instanceof Long
        assert inputs.attributeInt instanceof Integer || instance.attributeInt instanceof Long
    
        List<Map<String, String>> attributePaths = []
        attributePaths.add(matter.attributePath(inputs.ep, inputs.clusterInt, inputs.attributeInt))

        String cmd = matter.readAttributes(attributePaths)
	
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
        
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}
