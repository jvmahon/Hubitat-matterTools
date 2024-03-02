metadata {
    definition(name: "Matter Generic Component RGBW", namespace: "matterTools", author: "jvm", component: true) {
        capability "Bulb"
        capability "Refresh"
        capability "Switch"
		capability "SwitchLevel"
        capability "ChangeLevel"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        
        command "on"  , [[name: "Remain on for (seconds)", type:"NUMBER", description:"Turn off the device after the specified number of seconds"]]
        command "toggleOnOff" 
       
   
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
        
        command "testSetOffState"

        
        // Color Cluster
        attribute "colorCapabilities", "string"
        attribute "ColorTemperatureMinKelvin", "number"
        attribute "ColorTemperatureMaxKelvin", "number"
    }
    preferences {
        input(name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true)
        input(name: "showAdvancedOptions", type: "bool", title: "<b>Advanced Options</b>", description: '<i>Turn On, then Save Preferences to show Advanced Options configuration</i>', defaultValue: false )
        input(name: "powerAppliedState", type: "enum", title:"<b>When Powered On State</b>", description: "<i>Set state when power is first applied to device</i>", options:[ 0:"Off", 1:"On", 2:"Toggle"])
        
        if (advancedOptions == true) {
            input(name: 'autoOffTimer', type: 'bool', title: '<b>Use Automatic Off Timer</b>', defaultValue: false, required: true, description: '<i>Automatically turn Off after set time.</i>')
            input(name:'offTime', type:'number', title:"Automatic Off Time (in seconds)", defaultValue:0)
        }
    }
}
#include matterTools.getExpandedColorNames


void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

// This parse routine handles Hubitat SendEvent type messages (not the description raw strings originating from the device). 
// Hubitat's convntion is to include a parse() routine with this function in Generic Component drivers (child device drivers).
// It accepts a List of one or more SendEvent-type Maps and operates to determine how those Hubitat sendEvent Maps should be handled.
// The List of SendEvent Maps may include event Maps that are not needed by a particular driver (as determined based on the attributes of the driver)
// and those "extra" Maps are discarded. This allows a more generic "event Map" producting method (e.g., matterTools.createListOfMatterSendEventMaps)
void parse(List sendEventTypeOfEvents) {
     List updateLocalStateOnlyAttributes = ["OnOffTransitionTime", "OnTransitionTime", "OffTransitionTime", 
                                           "ColorCapabilities","ColorTemperatureMinKelvin", "ColorTemperatureMaxKelvin", 
                                           "MinLevel", "MaxLevel", "DefaultMoveRate", "OffWaitTime", "OnLevel", "OnTime", "StartUpOnOff"]
    sendEventTypeOfEvents.each {
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
        if(it.name == "StartUpOnOff") {setStartupOnOffInputControl(it)}  
    }
    // Always check and reset the color name after any update. 
    // In reality, only need to do it after a hue, saturation, or color temperature change, 
    // but for code simplicity, just let sendEvent handle that filtering!
    setColorName()
}


void testSetOffState(){
    setStartupOnOffInputControl([name:"StartUpOnOff", value:0])
}

void setStartupOnOffInputControl(event){
    if(event.value.is(null)) {
        device.removeSetting("powerAppliedState") 
        return
    } else {
        String newPowerStateValue = [ 0:"Off", 1:"On", 2:"Toggle"].get(event.value as Integer)
        device.updateSetting("powerAppliedState", [type:"enum", value:newPowerStateValue ])
    }
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
    log.info "Power state setting is: ${device.getSetting("powerAppliedState")}"
}

void setColorName(){
    String color
    switch (device.currentValue("colorMode")){
        case "RGB":
            // color = convertHueToGenericColorName(device.currentValue("hue") as Integer )
            color = getColorNameFromHSV(    hue:(device.currentValue("hue") as Integer), 
                                            saturation:(device.currentValue("saturation") as Integer),
                                            level:(device.currentValue("level") as Integer),
                                       )
            break
        case "CT":
            color = convertTemperatureToGenericColorName( device.currentValue("colorTemperature") as Integer )
            break
    }
    if (device.currentValue("colorName") != color ) { 
        if(txtEnable) log.info "${device.displayName} set to color: ${color}"
        sendEvent(name:"colorName", value:color) 
    }
}

void refresh() { parent?.refreshMatter(ep: getEndpoint() ) }

Integer getEndpoint() { Integer.parseInt(getDataValue("endpointId") ) }

void on() { parent?.on(ep: getEndpoint() ) }
void on(turnOffAfterSeconds) {   parent?.onWithTimedOff(ep:getEndpoint(), onTime10ths:(turnOffAfterSeconds * 10 as Integer) )}

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

