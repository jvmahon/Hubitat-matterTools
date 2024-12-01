import hubitat.matter.DataType

metadata {
    definition (name: "Matter onLevel Attribute Write Test Driver", namespace: "matterTools", author: "jvm33") {
    }
    command "Write_hex_25"
    command "Write_hex_50"
    command "Write_hex_FF"
    command "Write_null"
    command "cool"
    command "read_back"
}


void parse(String description) {
    log.debug matter.parseDescriptionAsMap(description)
}

void Write_hex_25(){
    log.info "${device.displayName}: Writing hex 25 to onLevel..."

    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.UINT8, "25"))
    
    String cmd = matter.writeAttributes(attrWriteRequests)
    log.debug "Setting Matter onLevel to ${MatterOnLevel} using command : ${cmd}"
    
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void Write_hex_50(){
    log.info "${device.displayName}: Writing hex 50 to onLevel..."

    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.UINT8, "50"))
    
    String cmd = matter.writeAttributes(attrWriteRequests)
    log.debug "Setting Matter onLevel to ${MatterOnLevel} using command : ${cmd}"
    
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}
void Write_hex_FF(){
    log.info "${device.displayName}: Writing hex FF to onLevel..."

    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.UINT8, "FF"))
    
    String cmd = matter.writeAttributes(attrWriteRequests)
    log.debug "Setting Matter onLevel to ${MatterOnLevel} using command : ${cmd}"
    
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void Write_null(){
    log.info "${device.displayName}: Writing hex Null to onLevel..."

    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.NULL, ""))
    
    String cmd = matter.writeAttributes(attrWriteRequests)
    log.debug "Setting Matter onLevel to ${MatterOnLevel} using command : ${cmd}"
    
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void cool() {
    List<String> cmds = []
    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0008, 0x0011, DataType.UINT8, intToHexStr(0x03,1)))
    cmds.add(matter.writeAttributes(attrWriteRequests))
    
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(device.endpointId, 0x0008, 0x0011))
    cmds.add(matter.readAttributes(attributePaths))
    sendToDevice(cmds)
}

void read_back() {
    List attributePaths = []
    attributePaths.add (matter.attributePath(0x01, 0x0008, 0x0011))
    String cmd = matter.readAttributes(attributePaths)
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
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
    