metadata {
    definition(name: "Matter Component Illuminance Sensor", namespace: "matterTools", author: "jvm", component: true) {
        // capability "Initialize"
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
import groovy.transform.Field

@Field static List updateLocalStateOnlyAttributes = [ "WiredAssessedInputVoltage", "WiredAssessedInputFrequency", "WiredCurrentType", "WiredAssessedCurrent",
"WiredNominalVoltage", "WiredMaximumCurrent", "WiredPresent", "ActiveWiredFaults", "BatVoltage",
"battery", "BatTimeRemaining", "BatChargeLevel", "BatReplacementNeeded", "BatReplaceability", 
"BatPresent", "ActiveBatFaults", "BatReplacementDescription", "BatCommonDesignation", "BatANSIDesignation",
"BatIECDesignation", "BatApprovedChemistry", "BatCapacity", "BatQuantity", "BatChargeState",
"BatTimeToFullCharge", "BatFunctionalWhileCharging", "BatChargingCurrent", "ActiveBatChargeFaults", "EndpointList" ]

void parse(List description) {
    description.each {
        if (device.hasAttribute (it.name)) {
            if (txtEnable) {
                if(device.currentValue(it.name) == it.value) {
                    log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") )+" (unchanged)" // Log if txtEnable and the value is the same
                } else {
                    log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") ) // Log if txtEnable and the value is the same
                }
            }
            if (updateLocalStateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
            } else {
                sendEvent(it)
            }
        }
    }
}

void initialize() {
    parent?.componentRefresh(this.device)
}

void refresh() { parent?.refreshMatter(ep: getEndpoint() ) }