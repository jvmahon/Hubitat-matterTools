metadata {
    definition(name: "Matter Device Management", namespace: "matterTools", author: "jvm", component: true) {
        
        command "writeClusterAttribute", [	[name:"clusterId*", type:"STRING", description:"Cluster Number in hex form"],
									[name:"attributeId*", type:"STRING", description:"Attribute Number in hex form"], 
									[name:"hexValue*", type:"STRING", description:"Value to be written in hex form"], 
									[name:"dataType*", type:"ENUM", description:"Data Type of Value to be Written", constraints: [0:"DATATYPE.INT8", 1:"DATATYPE.INT16"]]  ]
									
        command "readClusterAttribute", [	[name:"clusterId", type:"STRING"],
									[name:"attributeId", type:"STRING"] ] 
        
		// Thread Attributes    
		attribute "Channel", "number"
		attribute "RoutingRole", "string"
		attribute "NetworkName", "string"
		attribute "PanId", "number"
		attribute "ExtendedPanId", "number"
		attribute "MeshLocalPrefix", "string"
		attribute "OverrunCount", "number"
		attribute "NeighborTable", "string"
		attribute "RouteTable", "string"
		attribute "PartitionId", "number"
		attribute "Weighting", "number"
		attribute "DataVersion", "number"
		attribute "StableDataVersion", "number"
		attribute "LeaderRouterId", "number"
				
		// WiFi Attributes
		attribute "BSSID", "string"
		attribute "SecurityType", "string"
		attribute "WiFiVersion",  "string"
		attribute "ChannelNumber", "number"
		attribute "RSSI", "number"
		attribute "BeaconLostCount", "number"
		attribute "BeaconRxCount", "number"
		attribute "PacketMulticastRxCount", "number"
		attribute "PacketMulticastTxCount", "number"
		attribute "PacketUnicastRxCount", "number"
		attribute "PacketUnicastTxCount", "number"
		attribute "CurrentMaxRate", "number"
		attribute "OverrunCount", "number"
				
		attribute "ActiveLocale", "string"
		attribute "SupportedLocales", "string"        
		attribute "TemperatureUnit", "string"      

				// Basic Cluster
		attribute "DataModelRevision", "string"
		attribute "VendorName", "string"
		attribute "VendorID", "string"
		attribute "ProductName", "string"
		attribute "ProductID", "string"
		attribute "NodeLabel",  "string"
		attribute "Location", "string"
		attribute "HardwareVersion",  "string"
		attribute "HardwareVersionString", "string"
		attribute "SoftwareVersion",   "string"
		attribute "SoftwareVersionString", "string"
		attribute "ManufacturingDate",  "string"
		attribute "PartNumber",  "string"
		attribute "ProductURL", "string"
		attribute "ProductLabel",  "string"
		attribute "SerialNumber",  "string"
		attribute "LocalConfigDisabled",  "string"
		attribute "Reachable",  "string"
		attribute "UniqueID",  "string"
		attribute "CapabilityMinima",  "string"
		attribute "ProductAppearance",  "string"
        
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void parse(List description) {
    description.each {device.updateDataValue(it.name, "${it.value}") }
}

void initialize() {
    refresh()
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