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
Following method returns a ranked list of component driver types that may be suitable for  an endpoint.
Code can then check, in order, whether particular component driver types are installed and go to the next ne if not
*/

List<Map> getComponentDriverByEndpointType(Map params = [:] ){
    Map inputs = [epType:null] << params
    assert inputs.epType instanceof Integer
     // The following list can have multiple choices for each device type. Put them in ordered rank!
    Map<List<Map>> componentDriver = [
        0x0015:[[namespace:"hubitat" ,     name:"Generic Component Contact Sensor",         properties:[isComponent:false, name:null, label: null] ] ], // Contact Sensor
        0x0016:[[namespace:"matterTools" , name:"Matter Device Management",                 properties:[isComponent:true,  name:null, label: null] ] ], // Device Management
        0x0076:[[namespace:"hubitat" ,     name:"Generic Component Smoke Detector",         properties:[isComponent:false, name:null, label: null] ] ], // Smoke CO Alarm
        0x0100:[[namespace:"hubitat" ,     name:"Generic Component Switch",                 properties:[isComponent:false, name:null, label: null] ] ], // On/Off Light,
        0x0101:[[namespace:"hubitat" ,     name:"Generic Component Dimmer",                 properties:[isComponent:false, name:null, label: null] ] ], // Dimmable Light
        0x0103:[[namespace:"hubitat" ,     name:"Generic Component Switch",                 properties:[isComponent:false, name:null, label: null] ] ], // On/Off Light Switch
        0x0104:[[namespace:"hubitat" ,     name:"Generic Component Dimmer",                 properties:[isComponent:false, name:null, label: null] ] ], // Dimmer Switch
        0x0105:[[namespace:"hubitat" ,     name:"Generic Component RGBW",                   properties:[isComponent:false, name:null, label: null] ] ], // Color Dimmer Switch
        0x0106:[[namespace:"matterTools" , name:"Matter Generic Component Illuminance Sensor",      properties:[isComponent:false, name:null, label: null] ] ], // Illuminance Sensor
        0x0107:[[namespace:"hubitat" ,     name:"Generic Component Motion Sensor",          properties:[isComponent:false, name:null, label: null] ] ], // Occupancy Sensor (Motion Sensor)    
        0x010A:[[namespace:"hubitat" ,     name:"Generic Component Switch",                 properties:[isComponent:false, name:null, label: null] ] ], // On/Off Plug-In Unit
        0x010B:[[namespace:"hubitat" ,     name:"Generic Component Dimmer",                 properties:[isComponent:false, name:null, label: null] ] ], // Dimmable Plug-In
        0x010C:[[namespace:"hubitat" ,     name:"Generic Component CT",                     properties:[isComponent:false, name:null, label: null] ] ], // Color Temp Light
        0x010D:[[namespace:"matterTools" , name:"Matter Generic Component RGBW",                properties:[isComponent:false, name:null, label: null] ],
                [namespace:"hubitat" ,     name:"Generic Component RGBW",                   properties:[isComponent:false, name:null, label: null] ]], // Extended Color Light
        0x0302:[[namespace:"hubitat" ,     name:"Generic Component Temperature Sensor",     properties:[isComponent:false, name:null, label: null] ] ], // Temperature Sensor
        0x0307:[[namespace:"hubitat" ,     name:"Generic Component Humidity Sensor",        properties:[isComponent:false, name:null, label: null] ] ], // Humidity Sensor
        0x0850:[[namespace:"hubitat" ,     name:"Generic Component Contact Sensor",         properties:[isComponent:false, name:null, label: null] ] ], // On/Off Sensor
    ]
 
    return componentDriver.get(inputs.epType)
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

// Get all the child devices for a specified endpoint.
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

/////////////////////////////////////////////////////////////////////////
//////        Create and Manage Child Devices for Endpoints       ///////
/////////////////////////////////////////////////////////////////////////

// Basically, a debugging routine - if the "child" was created by another driver and doesn't have the proper formatting of endpointId and endpointSubindexId, delete that child.
void deleteUnwantedChildDevices() {	
	// Delete child devices that doesn't use the proper ID form (having both an endpointId and endpointSubindexId ).
	getChildDevices()?.each {
		if ( getEndpointIdInt(it).is(null)) { deleteChildDevice(it.deviceNetworkId) }			
	}
}

void deleteAllChildDevices(){
    getChildDevices()?.each{ deleteChildDevice(it.deviceNetworkId) }
}

void recreateChildDevices(){
    addNewChildDevice(endpointType:0x010D, ep:1)
}

void addNewChildDevice(Map params = [:]) {
    try {
        Map inputs = [endpointType: null, ep:null] << params
	    assert inputs.endpointType instanceof Integer
        assert inputs.ep instanceof Integer
        
        if (getChildDevices().find{ getEndpointIdInt(it) == inputs.ep } ) {
            if (txtEnable) log.info "For device ${device.displayName}, attempted to create child device for endpoint ${inputs.ep} but one already existed."
            return
        }
        
        // Get a list of all candidate drivers that work for this endpoint type, in preferenc rank
        List<Map>  matterDriverCandidatesList = getComponentDriverByEndpointType(epType:inputs.endpointType)
      
        List<Map> allInstalledDrivers = getInstalledDrivers()
        
        // Go through list of candidate drivers and select the first one that is also installed in Hubitat
        Map childDeviceDriver = matterDriverCandidatesList.find{ driver -> allInstalledDrivers.find{ ((it.name == driver.name) && (it.namespace == driver.namespace))} } 
        
        assert !childDeviceDriver.is(null) : "Unable to find a suitable child device driver candidate."
        
        log.info "Creating child device for endpoint ${inputs.ep} using driver: ${childDeviceDriver.name}"
        
        String childDeviceNetworkID = device.deviceNetworkId + "-ep0x${ HexUtils.integerToHexString(inputs.ep, 2).padLeft(4, "0") }"
        
        childDeviceDriver.properties.endpointId = HexUtils.integerToHexString( inputs.ep, 2)
        
	    com.hubitat.app.ChildDeviceWrapper cd = addChildDevice(childDeviceDriver.namespace, childDeviceDriver.name, childDeviceNetworkID, childDeviceDriver.properties)
        
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }   
}

/////////////////////////////////////////////////////////////////
