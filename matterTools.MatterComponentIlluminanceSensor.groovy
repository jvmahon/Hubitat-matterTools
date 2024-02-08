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
        
        attribute "Status", "number"
		attribute "Order", "string"
		attribute "Description", "string"
		attribute "WiredAssessedInputVoltage", "number"
		attribute "WiredAssessedInputFrequency", "number"
		attribute "WiredCurrentType", "string"
		attribute "WiredAssessedCurrent", "number"
		attribute "WiredNominalVoltage", "number"
		attribute "WiredMaximumCurrent", "number"
		attribute "WiredPresent", "string"
		attribute "ActiveWiredFaults", "string"
		attribute "BatVoltage", "number"
		attribute "battery", "number"
		attribute "BatTimeRemaining", "number"
		attribute "BatChargeLevel", "number"
		attribute "BatReplacementNeeded", "string"
		attribute "BatReplaceability", "string"
		attribute "BatPresent", "string"
		attribute "ActiveBatFaults", "string"
		attribute "BatReplacementDescription", "string"
		attribute "BatCommonDesignation", "string"
		attribute "BatANSIDesignation", "string"
		attribute "BatIECDesignation", "string"
		attribute "BatApprovedChemistry", "string"
		attribute "BatCapacity", "number"
		attribute "BatQuantity", "number"
		attribute "BatChargeState", "string"
		attribute "BatTimeToFullCharge", "number"
		attribute "BatFunctionalWhileCharging", "string"
		attribute "BatChargingCurrent", "string"
		attribute "ActiveBatChargeFaults", "string"
		attribute "EndpointList", "string"

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
    parent?.componentRefresh(this.device)
}

void refresh() {
    parent?.componentRefresh(this.device)
}