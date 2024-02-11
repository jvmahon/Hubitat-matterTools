metadata {
    definition(name: "Matter Component RGB Bulb", namespace: "matterTools", author: "jvm", component: true) {
        capability "Bulb"
        capability "Refresh"
        capability "Switch"
		capability "SwitchLevel"
        capability "ChangeLevel"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        
        command "OnWithTimedOff", [[name: "On Time in Seconds*", type:"NUMBER", description:"Turn on device for a specified number of seconds"]]
        command "toggleOnOff" 
        
        // Debugging Commands
        command "CleanupData"
        command "clearLeftoverStates"
        command "removeAllSettings"
        
   
        // Identify Cluster
        attribute "IdentifyTime", "number"
        attribute "IdentifyType", "string"
        
        // Switch Cluster
        attribute "OnTime", "number"
        attribute "OffWaitTime", "number"
        attribute "StartUpOnOff", "string"
        
        // Level Cluster
        attribute "OnOffTransitionTime", "number"
        attribute "OnTransitionTime", "number"
        attribute "OffTransitionTime", "number"

        attribute "RemainingTime", "number"
        attribute "MinLevel", "number"
        attribute "MaxLevel", "number"
        attribute "OnLevel", "number"
        attribute "DefaultMoveRate", "number"
        
        
        // Color Cluster
        attribute "colorCapabilities", "string"
        attribute "ColorTemperatureMinKelvin", "number"
        attribute "ColorTemperatureMaxKelvin", "number"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

import groovy.transform.Field

@Field static List updateLocalStateOnlyAttributes = ["OnOffTransitionTime", "OnTransitionTime", "OffTransitionTime", 
                                           "colorCapabilities","colorTemperatureMin", "colorTemperatureMax", 
                                           "minLevel", "maxLevel"]
void CleanupData() {
    log.info "Clearing Up state data"
    this.state.clear()
}
void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void parse(List description) {
    description.each {
        if (device.hasAttribute (it.name)) {
            if (txtEnable && (device.currentValue(it.name) != it.value)) log.info it.descriptionText // Log if txtEnable and the value is changing
            if (updateLocalStateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
            } else {
                sendEvent(it)
            }
        }
    }
}

void refresh() { parent?.componentRefresh(ep: getEndpoint() ) }


Integer getEndpoint() { Integer.parseInt(getDataValue("endpointId") ) }
                                         
void on() {  parent?.on(ep: getEndpoint() ) }
void off() { parent?.off(ep: getEndpoint() ) }
void toggleOnOff() { parent?.toggleOnOff(ep: getEndpoint()) }
void OnWithTimedOff(timeInSeconds, guardTime = 0) {
    parent?.onWithTimedOff(ep: getEndpoint(), 
                           onTime10ths:(timeInSeconds * 10) as Integer, 
                           ((timeInSeconds + guardTime) * 10) as Integer)
}
                                         
                                         
void setLevel(level) { parent?.setLevel(ep: getEndpoint(), level:level) }

void setLevel(level, ramp) { parent?.setLevel(ep: getEndpoint(), level:level as Integer, transitionTime10ths:(ramp* 10) as Integer ) }

void startLevelChange(direction) { parent?.startLevelChange(ep: getEndpoint(), direction:direction) }

void stopLevelChange() { parent?.stopLevelChange(ep: getEndpoint()) }

void setColor(colormap){  parent?.setColor(ep: getEndpoint(), 
                                           *:colormap) }

void setHue(hue) { parent?.setHue(ep: getEndpoint(), 
                                  hue: hue as Integer) }

void setSaturation(saturation) { 
    parent?.setSaturation(ep: getEndpoint(), 
                          saturation:saturation as Integer) }

void setColorTemperature(colortemperature, level = null, transitionTime = null) { 
    parent?.setColorTemperature(ep: getEndpoint(), 
                                colortemperature:colortemperature, 
                                level:level as Integer, 
                                transitionTime10ths: (transitionTime.is(null)) ? null : (transitionTime * 10)) 
}
/*
void on() {  parent?.componentOn(this.device) }

void off() { parent?.componentOff(this.device) }

void setLevel(level) { parent?.componentSetLevel(this.device, level) }

void setLevel(level, ramp) { parent?.componentSetLevel(this.device, level, ramp) }

void startLevelChange(direction) { parent?.componentStartLevelChange(this.device, direction) }

void stopLevelChange() { parent?.componentStopLevelChange(this.device) }

void setColor(colormap){  parent?.componentSetColor(this.device, colormap) }

void setHue(hue) { parent?.componentSetHue(this.device, hue) }

void setSaturation(saturation) { parent?.componentSetSaturation(this.device, saturation) }

void setColorTemperature(colortemperature, level, transitionTime) { parent?.componentSetColorTemperature(this.device, colortemperature, level, transitionTime) }
*/

void clearLeftoverStates() {
	// Can't modify state from within state.each{}, so first collect what is unwanted, then remove in a separate unwanted.each
	state.keySet().each{state.remove( it ) }
}

void removeAllSettings() {
    if (logEnable) log.debug "settings before clearing: " + settings
    // Copy keys set first to avoid any chance of concurrent modification
    def keys = new HashSet(settings.keySet())
    keys.each{ key -> device.removeSetting(key) }
     if (logEnable) log.debug "settings after clearing: " + settings
}

