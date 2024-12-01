/*
Copyright James V. Mahon
Distribution requires permission / Not for Commercial distribution
*/

// #include matterTools.getExpandedColorNames
import groovy.transform.Field
import hubitat.helper.HexUtils
import hubitat.matter.DataType

@Field Map supportedFanSpeeds =    [	"off": 0, "low": 1, "medium": 2, "high": 3, "on":4]

metadata {
    definition(name: "Matter Inovelli Fan Component", namespace: "matterTools", author: "jvm", component: true) {
        capability "Actuator"
        capability "Refresh"
        capability "Switch"
		capability "SwitchLevel"
        capability "FanControl"
        capability "Initialize"
        
        command "setLevel", [[name: "Fan speed percentage", type:"NUMBER", description:"Set Fan Speed as a percentage"]]
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: supportedFanSpeeds.collect {k,v -> k}]]
        
        // Identify Cluster
        attribute "IdentifyTime", "number"
      
    }
    preferences {
        input(name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false)
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
    }
}



void on() { parent?.on(ep:getEndpoint()) }

void off() { parent?.off(ep: getEndpoint() ) }

void setSpeed(fanSpeed){
    List<Map<String, String>> attrWriteRequests = []
    fanSpeedAsInteger = supportedFanSpeeds.get(fanSpeed)
    attrWriteRequests.add(matter.attributeWriteRequest(getEndpoint(), 0x0202, 0x0000, DataType.UINT8, HexUtils.integerToHexString((Integer) fanSpeedAsInteger, 1)))
    
    String cmd = matter.writeAttributes(attrWriteRequests)
   
    parent?.sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void cycleSpeed(){
    switch (device.currentValue("speed")){
        case "off":
            setSpeed("low");
            break;
        case "low":
            setSpeed("medium");
            break;
        case "medium":
            setSpeed("high");
            break;
        case "high":
            setSpeed("off");
            break;
    }   
}

void refresh() { parent?.refreshMatter(ep: getEndpoint() ) }

Integer getEndpoint() { Integer.parseInt(getDataValue("endpointId") ) }

                                   
void setLevel(level) { 
    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(getEndpoint(), 0x0202, 0x0002, DataType.UINT8, HexUtils.integerToHexString((Integer) level, 1)))
    
    parent?.sendHubCommand(new hubitat.device.HubAction(matter.writeAttributes(attrWriteRequests), hubitat.device.Protocol.MATTER))       
}

void initialize(){
    sendEvent( [name: "supportedFanSpeeds", value: ["off", "low", "medium", "high", "on", "auto"]])
    if (logEnable) log.debug "Current fan speeds are: ${device.currentValue("supportedFanSpeeds")}"

}

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
    log.debug "Fan received event sendEventTypeOfEvents ${sendEventTypeOfEvents}"
    try {
		List updateLocalStateOnlyAttributes = ["OnOffTransitionTime", "OnTransitionTime", "OffTransitionTime", "MinLevel", "MaxLevel", 
											   "ColorCapabilities","ColorTemperatureMinKelvin", "ColorTemperatureMaxKelvin", 
                                               "DefaultMoveRate", "OffWaitTime", "OnLevel", "Binding", "UserLabelList", "FixedLabelList", "VisibleIndicator", 
                                               "DeviceTypeList", "ServerList", "ClientList", "PartsList", "TagList", "SpeedMax"]
		sendEventTypeOfEvents.each {
            if ((it.clusterInt == 8) && (it.attrInt == 0)) return; // This is a temporary work-around due to Invelli VTM35 firmare 1.0.0 failure to properly report the CurrentLevel attribute of the Level Control cluster.
			if (device.hasAttribute (it.name)) {
				if (txtEnable) {
					if(device.currentValue(it.name) == it.value) {
						log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") )+" (unchanged)" // Log if txtEnable and the value is the same
					} else {
						log.info ((it.descriptionText) ? (it.descriptionText) : ("${device.displayName}: ${it.name} set to ${it.value}") ) // Log if txtEnable and the value is the same
					}
				}
                sendEvent(it)
            } else if (updateLocalStateOnlyAttributes.contains(it.name)) {
                device.updateDataValue(it.name, "${it.value}")
			}
		}
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing parse with inputs ${sendEventTypeOfEvents}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 
}

void updated() {
    if (logEnable) {
		log.info "${device.displayName}: Debug logging enabled for 30 minutes"
		runIn(1800,logsOff)
	}
}

void logsOff(){
    if (txtEnable) "${device.displayName}: Turning off Debug logging."
    device.updateSetting("logEnable", [value:"false",type:"bool"])
}

