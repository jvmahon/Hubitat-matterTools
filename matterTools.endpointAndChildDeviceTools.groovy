library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Child device Support Functions",
        name: "endpointAndChildDeviceTools",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.5.0"
)

import hubitat.helper.HexUtils

/*
Following method selects a component driver types for an endpoint based on Matter device type and installed driver options
*/

Map getComponentDriverByDeviceType(Map params = [:] ){
    Map inputs = [deviceType:null] << params
    assert inputs.deviceType instanceof Integer
     // The following list can have multiple choices for each device type. Put them in ordered rank!  Lower priority is better!
    Map<List<Map>> componentDriver = [
        0x0015:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Contact Sensor",         properties:[isComponent:false, name:null, label: null] ] ], // Contact Sensor
        // 0x0016:[[priority:1, namespace:"matterTools",  name:"Matter Device Management",                 properties:[isComponent:true,  name:null, label: null] ] ], // Device Management
        0x0076:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Smoke Detector",         properties:[isComponent:false, name:null, label: null] ] ], // Smoke CO Alarm
        0x0100:[[priority:1, namespace:"matterTools" , name:"Matter Generic Component Switch",          properties:[isComponent:false, name:null, label: null] ],  // On/Off Light,
                [priority:2, namespace:"hubitat" ,     name:"Generic Component Switch",                 properties:[isComponent:false, name:null, label: null] ] 
               ],
        0x0101:[[priority:1, namespace:"matterTools",  name:"Matter Generic Component Dimmer",          properties:[isComponent:false, name:null, label: null] ],  // Dimmable Light
                [priority:2, namespace:"hubitat" ,     name:"Generic Component Dimmer",                 properties:[isComponent:false, name:null, label: null] ] 
               ],
        0x0106:[[priority:1, namespace:"matterTools",  name:"Matter Generic Component Illuminance Sensor",      properties:[isComponent:false, name:null, label: null] ] ], // Illuminance Sensor
        0x0107:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Motion Sensor",          properties:[isComponent:false, name:null, label: null] ] ], // Occupancy Sensor (Motion Sensor)    
        0x010A:[[priority:1, namespace:"matterTools" , name:"Matter Generic Component Switch",          properties:[isComponent:false, name:null, label: null] ],  // On/Off Plug-In Unit,
                [priority:2, namespace:"hubitat" ,     name:"Generic Component Switch",                 properties:[isComponent:false, name:null, label: null] ] 
               ],
        0x010B:[[priority:1, namespace:"matterTools",  name:"Matter Generic Component Dimmer",          properties:[isComponent:false, name:null, label: null] ],  // Dimmable Plug-In
                [priority:2, namespace:"hubitat" ,     name:"Generic Component Dimmer",                 properties:[isComponent:false, name:null, label: null] ] 
               ],
        0x010C:[[priority:1, namespace:"hubitat" ,     name:"Generic Component CT",                     properties:[isComponent:false, name:null, label: null] ] ], // Color Temp Light
        0x010D:[[priority:1, namespace:"matterTools" , name:"Matter Generic Component RGBW",                properties:[isComponent:false, name:null, label: null] ],  // Extended Color Light
                [priority:2, namespace:"hubitat" ,     name:"Generic Component RGBW",                   properties:[isComponent:false, name:null, label: null] ]
               ],
        0x0302:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Temperature Sensor",     properties:[isComponent:false, name:null, label: null] ] ], // Temperature Sensor
        0x0307:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Humidity Sensor",        properties:[isComponent:false, name:null, label: null] ] ], // Humidity Sensor
        0x0850:[[priority:1, namespace:"hubitat" ,     name:"Generic Component Contact Sensor",         properties:[isComponent:false, name:null, label: null] ] ], // On/Off Sensor
    ]
 
    List<Map> matterDriverCandidatesList = componentDriver.get(inputs.deviceType)
    
    List<Map> allInstalledDrivers = getInstalledDrivers()
        
    // Go through list of candidate drivers and select the one with the lowest priority value that is also installed in Hubitat
    List childDeviceDriverCandidates = matterDriverCandidatesList?.findAll({ driver -> allInstalledDrivers.find{ ((it.name == driver.name) && (it.namespace == driver.namespace))} })
    // Return lowest priority
    return childDeviceDriverCandidates?.min{it.priority}
}

Boolean isUtilityNode(decodedDescMap){
    List utilityNodes = [0x0011, 0x0012, 0x0013, 0x0014, 0x0016, 0x0510, 0x050D ]
    return decodedDescMap.decodedValue.any{ it[0] in utilityNodes }
}

com.hubitat.app.DeviceWrapper checkAndCreateChildDevices(decodedDescMap){
    try{
        com.hubitat.app.DeviceWrapper cd = childDevices.find{ getEndpointIdInt(it) == decodedDescMap.endpointInt }
		if(cd) { return cd } // Do nothing if child devices already exist for this endpoint
        
		if (isUtilityNode(decodedDescMap)) return
        
        // if the deviceType has multiple entries, pick the highest values
        // multiple values are usually only present for superset device types (Matter Core Spec. 9.2.5), and in those cases
        // the highest value is typically the most sophisticated of the device types (e.g., On/Off Light < Dimmable Light < RGBW Light)
        Integer deviceType = decodedDescMap.decodedValue.collect({it[0]})?.max()

        if (deviceType.is(null)) {
            if (logEnable) log.debug "Received a null device type when trying to create a child device: Report was: ${decodedDescMap}"
            return
        }
        // No child devices for Generic Switch and Mode Select!
        if (deviceType in [0x000E, 0x000F, 0x0027, 0x0050, 0x0103, 0x0104, 0x0105]) {
            if(logEnable) log.debug "${device.displayName}: No child device created for endpoint ${decodedDescMap.endpointInt} Matter device type ${deviceType}. This is expected."
            return null
        }
   
		// Get a list of all candidate drivers that work for this endpoint type, in preference rank
		Map  childDeviceDriver = getComponentDriverByDeviceType(deviceType:deviceType)
		assert childDeviceDriver
			
		log.info "Creating child device for endpoint ${decodedDescMap.endpointInt} using driver: ${childDeviceDriver.name}"
			
		String childDeviceNetworkID = device.deviceNetworkId + "-ep0x${ HexUtils.integerToHexString(decodedDescMap.endpointInt, 2).padLeft(4, "0") }"
			
		childDeviceDriver.properties.endpointId = HexUtils.integerToHexString( decodedDescMap.endpointInt, 2)
        
        // Add a label using endpoint
        childDeviceDriver.properties.label = "Matter device endpoint ${decodedDescMap.endpointInt} with device type ${deviceType}"
			
		cd = addChildDevice(childDeviceDriver.namespace, childDeviceDriver.name, childDeviceNetworkID, childDeviceDriver.properties) 
		return cd // return newly created child device or null
    } catch (AssertionError e) {
        log.error "Attempting to create a child device, but unable to find a suitable child device driver candidate for Matter device type ${deviceType}."
    } catch(e){
        log.error "<pre>${e}<br><br>when processing checkAndCreateChildDevices with inputs ${decodedDescMap}<br><br>Stack trace:<br>${getStackTrace(e) }"
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
