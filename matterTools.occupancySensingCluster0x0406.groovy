/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 2.7 ("Occupancy Sensing Cluster")

Library  assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt). 
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using
        descMap = matter.parseDescriptionAsMap(description)
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16)))
See matterTools.commonDriverMethods library for example
*/
library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Occupancy Sensing Cluster 0x0406 Tools",
        name: "occupancySensingCluster0x0406",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

List<Map> processClusterResponse_0406(Map descMap) {
	List<Map> hubEvents = []
    switch(descMap.attrInt) {
		case 0x0000: // Occupancy
			hubEvents << [name:"motion", value: (descMap.value ? "active" : "inactive"), isStateChange:false]
			hubEvents << [name:"presence", value: (descMap.value ? "present" : "not present"), isStateChange:false]
			break
		case 0x0001: // OccupancySensorType
            String sensorType = [0:"PIR", 1:"Ultrasonice", 2:"PIRAndUltrasonic", 3:"PhysicalContact"].get(descMap.value)
			hubEvents << [name:"OccupancySensorType", value: sensorType, descriptionText: "${device.displayName} sensor type is: ${sensorType}", isStateChange:false]                    
            break
		case 0x0002: // OccupancySensorTypeBitmap - duplicative of #1
            break
		case 0x0010: //  PIR OccupiedToUnoccupiedDelay
        case 0x0020: //  Ultrasonic OccupiedToUnoccupiedDelay
        case 0x0030: //  Physical OccupiedToUnoccupiedDelay
			hubEvents << [name:"OccupiedToUnoccupiedDelay", value: descMap.value, units:"seconds", isStateChange:false]                    
            break
		case 0x0011: // PIR UnoccupiedToOccupiedDelay
        case 0x0021: // Ultrasonic UnoccupiedToOccupiedDelay
        case 0x0031: // Physical UnoccupiedToOccupiedDelay
			hubEvents << [name:"UnoccupiedToUnoccupiedDelay", value: descMap.value, units:"seconds", isStateChange:false] 
		case 0x0012: // PIR UnoccupiedToOccupiedThreshold
        case 0x0022: // Ultrasonic UnoccupiedToOccupiedThreshold
        case 0x0032: // Physical UnoccupiedToOccupiedThreshold
			hubEvents << [name:"UnoccupiedToUnoccupiedThreshold", value: descMap.value, isStateChange:false]                       
        break
	}
    return hubEvents
}    

void refresh_0406(Map params = [:]){
    try { 
        Map inputs = [ep: 0xFFFF] << params
        assert inputs.ep instanceof Integer // Default value of FFFF is wildcard meaning all endpoints!
        String hexEP = HexUtils.integerToHexString(inputs.ep, 2) 
        String cmd = 'he rattrs [{"ep":"0x' + hexEP + '","cluster":"0x0406","attr":"0xFFFFFFFF"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }  
}
