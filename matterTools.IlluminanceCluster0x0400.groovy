/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 2.2 ("Illuminance Measurement Cluster")

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
        description: "Illuminance Measure Cluster 0x0400 Tools",
        name: "IlluminanceCluster0x0400",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import java.lang.Math

Integer computeLux(value){ Math.pow( 10, ((Integer.parseInt(value, 16) -1) / 10000) ) as Integer}

List<Map> processClusterResponse_0400(Map descMap) {
	List<Map> hubEvents = []
    switch(descMap.attrInt) {
		case 0x0000: // illuminance
			hubEvents << [name:"illuminance", value: computeLux(descMap.value), units:"lx", descriptionText: "${device.displayName} measured ${computeLux(descMap.value)} ls", isStateChange:false]
			break
		case 0x0001: // MinMeasuredValue
			hubEvents << [name:"MinMeasuredValueLux", value: computeLux(descMap.value), units:"lx", descriptionText: "${device.displayName} minimum measurable lux is ${computeLux(descMap.value)}.", isStateChange:false]                    
            break
		case 0x0002: // MaxMeasuredValue
			hubEvents << [name:"MaxMeasuredValueLux", value: computeLux(descMap.value), units:"lx", descriptionText: "${device.displayName} Minimum measurable lux is ${computeLux(descMap.value)}.", isStateChange:false]                    
            break
		case 0x0003: // Tolerance
			hubEvents << [name:"LuxMeasurementTolerance", value: computeLux(descMap.value), units:"lx", descriptionText: "${device.displayName} Lux measurement tolerance is ${computeLux(descMap.value)} lx.", isStateChange:false]                    
            break
		case 0x0004: // LightSensorType
			hubEvents << [name:"LightSensorType", value: [0:"Photodiode", 1:"CMOS"].get(descMap.value), isStateChange:false]                    
        break
	}
    return hubEvents
}    

void refresh_0400(Map params = [:]){
    try { 
        Map inputs = [ep: 0xFFFF] << params
        assert inputs.ep instanceof Integer // Default value of FFFF is wildcard meaning all endpoints!
        String hexEP = HexUtils.integerToHexString(inputs.ep, 2) 
        String cmd = 'he rattrs [{"ep":"0x' + hexEP + '","cluster":"0x0400","attr":"0xFFFFFFFF"}]'
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    } catch(AssertionError e) {
        log.error "<br><pre> ${getStackTrace(e)}"
        log.error "<pre>${e}"
    }  
}
