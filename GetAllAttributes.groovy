metadata {
   definition (name: "Get All Attributes and Events", namespace: "matterTools", author: "jvm33") {
      capability "Refresh"
      capability "Configuration"
      command "unsubscribeAll"
      command "showStoredAttributeData"
      // command "eventPaths"
   }
}
import hubitat.matter.DataType
import java.util.concurrent.* 
import groovy.transform.Field
import groovy.json.JsonBuilder

@Field static ConcurrentHashMap globalDataStorage = new ConcurrentHashMap(32, 0.75, 1) // Intended to Store info. that does not change. Default is static

// Stores attribute values in nested ConcurrentHashMaps. Because this code retrieves many attributes at once, use ConcurrentHashMaps to ensure thread safety.
void storeRetrievedData(Map descMap){
    String netId = device?.getDeviceNetworkId()
    
    globalDataStorage.get(netId, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
        .get(descMap.endpoint, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
            .get(descMap.cluster, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
                .put(descMap.attrId, descMap.value)
}

// Retrieves a particular attribute from those previously received.
Object getStoredAttributeData(Map params = [:]){
    Map inputs = [endpoint:null, cluster:null, attrId:null] << params
    try { 
        assert inputs.keySet().containsAll(userInputs.keySet()) // check that all keys in userInputs are found in the inputs map, meaning the function was called with expected inputs.
        assert inputs.endpoint instanceof String && inputs.endpoint.matches("[0-9A-F]+") // String must be a hex value.
        assert inputs.cluster instanceof String  && inputs.cluster.matches("[0-9A-F]+")  // String must be a hex value.
        assert inputs.attrId instanceof String   && inputs.attrId.matches("[0-9A-F]+")   // String must be a hex value.
    } catch(AssertionError e) {
            log.error "<pre>${e}"
    }
    
    String netId = device?.getDeviceNetworkId()
    
    globalDataStorage.get(netId, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1))
        ?.get(descMap.endpoint)
            ?.get(descMap.cluster)
                ?.get(descMap.attrId)

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
    // This is a wildcard subscribe. Subscribes to all endpoints, all clusters, all attributes
    String cmd = 'he subscribe 0x00 0xFF [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
    log.info "Sending command to Subscribe for all attributes with a 0 second minimum time: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    
    /*
    cmd = 'he subscribe 0x00 0xFF [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","evt":"0xFFFFFFFF"}]'
    log.info "Sending command to Subscribe for all events with a 0 second minimum time: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
    */
}

void eventPaths(){  // not currently used
    List eventPaths = []
    eventPaths.add(matter.eventPath("FFFF", 0xFFFF, 0xFFFF))
    log.debug matter.subscribe(0, 0x00FF, eventPaths)
}

def parse(String description) {
    def descMap
    try {
        descMap = matter.parseDescriptionAsMap(description)
        storeRetrievedData(descMap)
    } catch (e) {
        log.error "Caught error ${e} trying to parse descripiton ${description}"
    }
}
