/* 
Reference: Matter Core Specification Version 1.2 ("Matter Core Spec"), Section 9.5, "Descriptor Cluster"
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
        description: "Descriptor Cluster 0x001D Tools",
        name: "descriptorCluster0x001D",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import hubitat.helper.HexUtils

// Implements Parsing for Descriptor Cluster, Core Spec Section 9.5
// Hubitat does not parse messages correctly. Finish implementing when Hubitat fixes issues!
List<Map> processClusterResponse_001D(Map descMap){
	List<Map> hubEvents = []

	switch(descMap.attrInt) {
		case 0x0000: // DeviceTypeList
			// hubEvents << [name:"deviceType", value: Integer.parseInt(descMap.value, 16), descriptionText: "${device.displayName} will identify itself for ${Integer.parseInt(descMap.value, 16)} more seconds", isStateChange:false]
			break
        case 0x0004: // TagList
            break
		default:
			break
	}
    return hubEvents
}	

void refresh_001D(Map params = [:] ) {
    // No reason to refresh here since do nothing with the results!
}

