#include matterTools.matterHelperUtilities

metadata {
   definition (name: "Aqara P2 Matter Test", namespace: "matterTools", author: "My Name") { 
   capability "Configuration"
   capability "Refresh"
   
   }
    
command "unsubscribeAll"
    command "SubscribeEvents"
    
   preferences {
      // None for now
   }
}

void unsubscribeAll(){
    String cmd = matter.unsubscribe()
    log.debug "Sending command to Unsubscribe for all events: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void parse(String description){
    Map descMap = matter.parseDescriptionAsMap(description)
    log.debug descMap
}

void SubscribeEvents(){
     String cmd = 'he subscribe 0x0000 0x0700 [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","evt":"0xFFFFFFFF"}]'
    log.info "Subscribing to events using command: ${cmd}"
     sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void refresh() {
    log.info "${device.displayName} refreshing all device data"
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}

void configure(){
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0xFFFFFFFF","attr":"0xFFFFFFFF"}]'
   log.info "Sending command to Subscribe for all events with a 1 second minimum time, refresh at 30 Minute maximum: " + cmd
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
   
}