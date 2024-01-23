metadata {
   definition (name: "Get and Store All Attributes", namespace: "matterTools", author: "jvm33") {
      capability "Refresh"
      capability "Configuration"
      command "unsubscribeAll"
      command "showStoredAttributeData"
   }
}
import hubitat.matter.DataType
import java.util.concurrent.* 
import groovy.transform.Field
import groovy.json.JsonBuilder

@Field static ConcurrentHashMap globalDataStorage = new ConcurrentHashMap(32, 0.75, 1) // Intended to Store info. that does not change. Default is static

void storeRetrievedData(Map descMap){
    String netId = device?.getDeviceNetworkId()
    
    globalDataStorage.get(netId, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
        .get(descMap.endpoint, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
            .get(descMap.cluster, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
                .put(descMap.attrId, descMap.value)

}

void showStoredAttributeData(){
    String netId = device?.getDeviceNetworkId()
    log.info "<pre> ${new JsonBuilder(globalDataStorage.get(netId)).toPrettyString()}"
    // log.info globalDataStorage.get(netId)
    
}

def configure(){
    subscribeAll()
}

def refresh(){
    log.info "Refreshing all endpoints, all clusters, all attributes: "
    String cmd = 'he rattrs [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void unsubscribeAll(){
    String cmd = matter.unsubscribe()
    log.info "Sending command to Unsubscribe from all attribute reports: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void subscribeAll(){
    String cmd = 'he subscribe 0x01 0xFF [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    log.info "Sending command to Subscribe for all events with a 1 second minimum time: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

def parse(String description) {
    log.info " " // blank line insert
    log.info "In parse function, received description: ${description}"
   def descMap = matter.parseDescriptionAsMap(description)
    log.info "Parsed message in Mapped form: ${descMap}"
    storeRetrievedData(descMap)

}


