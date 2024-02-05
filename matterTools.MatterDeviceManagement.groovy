metadata {
    definition(name: "Matter Device Management", namespace: "matterTools", author: "jvm", component: true) {
        
        command "recreateChildDevices"
        command "createEveMotionChildDevices"
        command "writeClusterAttribute", [	[name:"clusterId*", type:"STRING", description:"Cluster Number in hex form"],
									[name:"attributeId*", type:"STRING", description:"Attribute Number in hex form"], 
									[name:"hexValue*", type:"STRING", description:"Value to be written in hex form"], 
									[name:"dataType*", type:"ENUM", description:"Data Type of Value to be Written", constraints: [0:"DATATYPE.INT8", 1:"DATATYPE.INT16"]]  ]
									
        command "readClusterAttribute", [	[name:"clusterId", type:"STRING"],
									[name:"attributeId", type:"STRING"] ] 
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void parse(List hubitatEventMaps) {
    hubitatEventMaps.each {
        if (device.hasAttribute(it.name)) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void initialize() {
    refresh()
}

void recreateChildDevices(){
    parent?.recreateChildDevices()
}

void createEveMotionChildDevices(){
    parent?.createEveMotionChildDevices()
}

void writeClusterAttribute(clusterId, attributeId, hexValue, dataType) { 
    parent?.writeClusterAttribute(
            ep: null,
            clusterInt: Integer.parseInt( clusterId, 16),
            attributeInt: Integer.parseInt( attributeId, 16), 
            hexValue:hexValue, 
            hubitatDataType: dataType 
    ) 
}

void readClusterAttribute(clusterId, attributeId) {
    parent?.readClusterAttribute(clusterId:clusterId, attributeId:attributeId)
}

void refresh() {
    parent?.componentRefresh(this.device)
}