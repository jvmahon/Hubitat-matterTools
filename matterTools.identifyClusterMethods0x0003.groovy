/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.2 "Identify Cluster"
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
        description: "Identify Cluster 0x0003 Tools",
        name: "identifyClusterMethods0x0003",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import hubitat.helper.HexUtils

// Implements Cluster Spec Section 1.2.7, Identify command 0x00
// Note that identifyTime is in seconds - many Matter commands use tenths!
void identify(timeInSeconds){     identify(identifyTime:timeInSeconds as Integer) }
void identify( Map params = [:] ){
    try {
        Map inputs = [ep: getEndpointIdInt(device), identifyTime: 10] << params
        assert inputs.keySet().containsAll(["ep", "identifyTime"]) // checks that required parameters are present.
        assert inputs.identifyTime instanceof Integer
        assert inputs.ep instanceof Integer
        
        String timeStringHex = HexUtils.integerToHexString(inputs.identifyTime, 2) //  is uint16 - two byte (4 Octet) field.
                  
        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT16,  0, byteReverseParameters(timeStringHex) )) // IdentifyTime uint16 0-65534, byte reversed. "0A00" means 10 seconds
    
        String cmd = matter.invoke(inputs.ep, 0x0003, 0x0000, fields) // command 0x0000 is the identify command, it has a IdentifyTime parameter
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))  
    } catch(AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e)}"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}