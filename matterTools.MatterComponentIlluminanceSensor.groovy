metadata {
    definition(name: "Matter Component Illuminance Sensor", namespace: "matterTools", author: "jvm", component: true) {
        capability "Initialize"
		capability "Sensor"
        capability "Refresh"
        capability "IlluminanceMeasurement"
		capability "Battery"
        
        attribute "MinMeasuredValueLux", "number"
        attribute "MaxMeasuredValueLux", "number"
        attribute "LuxMeasurementTolerance", "number"
        attribute "LightSensorType", "string"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void parseInChildDriver(descMap){
    log.info "Attempting to parse unknown item in the child driver: ${device.displayName}"
    
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

void refresh() {
    parent?.componentRefresh(this.device)
}