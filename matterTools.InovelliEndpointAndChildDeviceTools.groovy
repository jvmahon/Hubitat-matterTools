library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Child device Support Functions",
        name: "InovelliEndpointAndChildDeviceTools",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.7.0"
)

void checkAndCreateChildDevices(){
    try{
        switch (getDataValue("model")){
            case "VTM30-SN":
               if (!getChildDeviceListByEndpoint(ep:1)) addChildDevice("matterTools",  "Matter Generic Component Switch", "${device.deviceNetworkId}-ep0x0001" , [isComponent:false, name:null, label: "Load Control", endpointId:"0001"])
               if (!getChildDeviceListByEndpoint(ep:6)) addChildDevice("matterTools" , "Matter Generic Component RGBW",   "${device.deviceNetworkId}-ep0x0006" , [isComponent:false, name:null, label: "LED Notification Bar", endpointId:"0006"])
                break;
            case "VTM31-SN":
               if (!getChildDeviceListByEndpoint(ep:1)) addChildDevice("matterTools",  "Matter Generic Component Dimmer", "${device.deviceNetworkId}-ep0x0001" , [isComponent:false, name:null, label: "Load Control", endpointId:"0001"])
               if (!getChildDeviceListByEndpoint(ep:6)) addChildDevice("matterTools" , "Matter Generic Component RGBW",   "${device.deviceNetworkId}-ep0x0006" , [isComponent:false, name:null, label: "LED Notification Bar", endpointId:"0006"])
                break;
            case "VTM35-SN":
               if (!getChildDeviceListByEndpoint(ep:1)) addChildDevice("matterTools",  "Matter Inovelli Fan Component", "${device.deviceNetworkId}-ep0x0001" , [isComponent:false, name:null, label: "Fan Control", endpointId:"0001"])
               if (!getChildDeviceListByEndpoint(ep:6)) addChildDevice("matterTools" , "Matter Generic Component RGBW",   "${device.deviceNetworkId}-ep0x0006" , [isComponent:false, name:null, label: "LED Alert Strip", endpointId:"0006"])
                break;
            case "VTM36":
               if (!getChildDeviceListByEndpoint(ep:1)) addChildDevice("matterTools",  "Matter Generic Component Dimmer", "${device.deviceNetworkId}-ep0x0001" , [isComponent:false, name:null, label: "Load Control", endpointId:"0001"])
               if (!getChildDeviceListByEndpoint(ep:2)) addChildDevice("matterTools" , "Matter Inovelli Fan Component",   "${device.deviceNetworkId}-ep0x0002" , [isComponent:false, name:null, label: "Fan", endpointId:"0002"])
                break;
            }
        } catch(e){
        log.error "<pre>${e}<br><br>when processing checkAndCreateChildDevices:<br>${getStackTrace(e) }"
    }
}

// This next function will generally override device.getEndpointId()
Integer getEndpointIdInt(com.hubitat.app.DeviceWrapper thisDevice) {
	String rValue =  thisDevice?.getDataValue("endpointId") ?:   thisDevice?.endpointId 
    if (rValue.is( null )) { 
        log.error "Device ${thisDevice.displayName} does not have a defined endpointId. Fix this!"
        return null
    }
	return Integer.parseInt(rValue, 16)
}

// Get all the child devices for a specified endpoint.  This allows for possibility of multiple child devices per endpoint.
// May want to simplify this to only allow 1 child device per endpoint.
List<com.hubitat.app.DeviceWrapper> getChildDeviceListByEndpoint( Map params = [:] ) {
    Map inputs = [ep: null ] << params
    assert inputs.ep instanceof Integer
	childDevices.findAll{ getEndpointIdInt(it) == inputs.ep }
}

// Uses a parse routine to manage sendEvent message distribution
// The passing of a sendEvent event to a parse routine is a technique used in Hubitat's Generic Component drivers, so its adopted here.
void sendEventsToEndpointByParse(Map params = [:]) {
    Map inputs = [ events: null , ep: null ] << params
    assert inputs.events instanceof List
    assert inputs.ep instanceof Integer

	List<com.hubitat.app.DeviceWrapper> targetDevices = getChildDeviceListByEndpoint(ep:(inputs.ep))
	// if ((inputs.ep == getEndpointIdInt(device)) || (inputs.ep == 0) )  { targetDevices += this }
	if (inputs.ep == 0)  { targetDevices += this }

	targetDevices.each { it.parse(inputs.events) }
}
