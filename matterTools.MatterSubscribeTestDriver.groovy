import groovy.transform.Field
import hubitat.matter.DataType
// nclude matterTools.matterHelperUtilities

metadata {
    definition (name: "Matter Subscribe Test Driver", namespace: "matterTools", author: "jvm33") {
        capability "Configuration"
        capability "Refresh"
    }
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String nodeReportRawDescriptionString) {
    log.debug "Received string to parse: ${nodeReportRawDescriptionString}"

}

void configure(){
    String cmd = 'he cleanSubscribe 0x01 0x0040 [ {"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    
    log.debug "${device.displayName}: Subscribing to device attribute reports with a 1 second minimum report delay, refresh every 0x0040 = 64 seconds using command ${cmd}."
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void refresh() {
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}

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