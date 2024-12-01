/*
Copyright James V. Mahon
Distribution requires permission / Not for Commercial distribution
*/
#include matterTools.parseDescriptionAsDecodedMap
#include matterTools.matterHelperUtilities

import groovy.transform.Field
@Field Map getFanLevel = [
	"low": 33
	,"medium": 66
	,"high": 100
	,"on" : 100
	,"off": 0
]


metadata {
    definition (name: "Inovelli Fan Module Test Driver", namespace: "matterTools", author: "jvm33") {
        capability "FanControl"
        capability "Initialize"
        capability "Refresh"
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "setLevel", [[name: "Fan Speed Persent*",type:"NUMBER", description:"Fan speed level in %"]]

        
        attribute "level", "number"
        
        command "matterUnsubscribe", [[name:"Unsubscribe Device", description:"A debugging tool. Choose this to unsubscribe from Matter attribute reports."]]
    }
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String description) {
    Map decodedDescriptionMap = parseDescriptionAsDecodedMap(description) // Using parser from matterTools.parseDescriptionAsDecodedMap
    log.info "${device.displayName}: Received report: <font color='blue'>${decodedDescriptionMap}"
    if (decodedDescriptionMap.clusterInt == 0x0202) {
        Map rValue = [:]
        switch (decodedDescriptionMap.attrInt){
            case 0x0000:
                Map speeds =  [0:"off", 1:"low", 2:"medium", 3:"high", 4:"on", 5:"auto", 6:"smart"]
                rValue = [name:"speed" , value: speeds.get(decodedDescriptionMap.decodedValue)]
                break
            case 0x0003:
                rValue = [name:"level" , value:decodedDescriptionMap.decodedValue]
                break
        }
        sendEvent(rValue)
    }
}

void matterUnsubscribe(){
    log.info "${device.displayName}: Clearing Matter device subscriptions"
    if (txtEnable) log.info "${device.displayName}: Unsubscribing from Matter attribute changes."
    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) // unsubscribe
}

void initialize(){
    sendEvent([name: "supportedFanSpeeds", value: ["low", "medium", "high", "off", "on"]])
    log.info "${device.displayName}: Subscribing to reports for Fan cluster with a 0 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0000 0x0700 [{"ep":"0xFFFF","cluster":"0x0202","attr":"0xFFFFFFFF"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}


void refresh() {
    log.info "${device.displayName}: Refreshing Mode Select Data on All Endpoints."
    refreshMatter(ep:0xFFFF, clusterInt: 0x0050, attrInt: 0xFFFFFFFF)
}

void setSpeed(fanspeed){
    Integer speed =  ["low":1, "medium-low":1, "medium":2, "medium-high":2, "high":3, "on":4, "off":0, "auto":5].get(fanspeed)
    supportedFanSpeeds = {}
    List<String> cmds = []
    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest("02", 0x202, 0x0000, DataType.UINT8, intToHexStr(speed,1)))
    cmds.add(matter.writeAttributes(attrWriteRequests))
    log.debug "For fanspeed ${fanspeed}, value ${speed},  Sending write command ${cmds}"
    sendToDevice(cmds)
    
}

void cycleSpeed() {
    log.error "cycle speed setting not implemented"
}


void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}