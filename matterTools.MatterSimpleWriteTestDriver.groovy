import hubitat.matter.DataType
import hubitat.helper.HexUtils

metadata {
    definition (name: "Matter Simple Write Test Driver", namespace: "matterTools", author: "jvm33") {
		capability "Refresh"
    }
    
    preferences {
        input( name: "MatterOnLevel", type:"number", title:"Turn On Level (%)", description: "When turning on light, set to this level.", range:"1..100")   
    }
}


void parse(String description) {
    log.debug matter.parseDescriptionAsMap(description)
}

void updated(){
    log.info "${device.displayName}: Processing Preference changes..."
    Integer MatterOnLevel = device.getSetting("MatterOnLevel") * 2.54

    if (MatterOnLevel) {
        String MatterOnLevelHex = HexUtils.integerToHexString(MatterOnLevel, 1)
        List<Map<String, String>> attrWriteRequests = []
        attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.UINT8, MatterOnLevelHex))
        String cmd = matter.writeAttributes(attrWriteRequests)
        log.debug "Setting Matter onLevel to ${MatterOnLevel} using command : ${cmd}"
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    
    }
}

void refresh() {
    refreshMatter(ep:0x01, clusterInt: 0x0008, attrInt: 0x0011) 
}
    

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