import groovy.transform.Field
import hubitat.matter.DataType


 // Tools for creation of child devices
 // Identify methods supporting named parameters, endpoints, and child devices
 // On/Off cluster methods supporting named parameters, endpoints, and child devices
 // Level cluster methods supporting named parameters, endpoints, and child devices
 // Color Cluster methods supporting named parameters, endpoints, and child devices
 // Mode Select Cluster Methods to support Mode Select commands
 // Converts the data from parseAsMap to Hubitat event form




metadata {
    definition (name: "Inovelli VTM31-SN Advanced Device Driver", namespace: "matterTools", author: "jvm33") {
		capability "Initialize"
		capability "Refresh"
        capability "Configuration"
        
        command "identify",     [[name: "Identify", type:"NUMBER", description:"Put device into Identify mode (seconds)."]]
        command "initialize",   [[name: "Initialize",  description:"Called at boot time to confirm child devices exist and to initialize Matter."]]
        command "configure",    [[name: "Configure",   description:"Create child devices and set up Matter subscriptions."]]
        command "refresh",      [[name: "Refresh",     description:"Refresh device data."]]

        // command "unsubscribeAll", [[name: "Step 1 - Unsubscribe from Attribute Reports", description:"Unsubscribe from Attribute Change Reports"]] // For debugging purposes
        // command "resubscribeAll", [[name: "Step 2 - Subscribe To Attribute Reports", description:"Stubscribe to Attribute Change Reports"]] // / For debugging purposes
    }
    
    preferences {
        input(name:"logEnable", type:"bool", title:"<b>Enable debug logging</b>", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"<b>Enable descriptionText logging</b>", defaultValue:true)
           input( name: "ModeSelect_1", type:"enum", options:getModeSelectOptions(1),  title: (getModeSelectLabel(1) ) )   
           input( name: "ModeSelect_2", type:"enum", options:getModeSelectOptions(2),  title: (getModeSelectLabel(2) ) )   
           input( name: "ModeSelect_3", type:"enum", options:getModeSelectOptions(3),  title: (getModeSelectLabel(3) ) )   
           input( name: "ModeSelect_4", type:"enum", options:getModeSelectOptions(4),  title: (getModeSelectLabel(4) ) )   
           input( name: "ModeSelect_5", type:"enum", options:getModeSelectOptions(5),  title: (getModeSelectLabel(5) ) )   
           input( name: "ModeSelect_6", type:"enum", options:getModeSelectOptions(6),  title: (getModeSelectLabel(6) ) )   
    }
    
    fingerprint endpointId:"01", inClusters:"0003,0004,0005,0006,0008,001D,0040,0050,122FFC31", outClusters:"", model:"VTM31-SN", manufacturer:"Inovelli", controllerType:"MAT"
}



// This "parse" method handles Hubitat SendEvent type messages (not the description raw strings originating from the device). 
// It would be preferable if this had a different name so as to not cause confusion with the "parse" method for the description Strings from devices, but
// Hubitat's convntion has been to include a parse() routine with this function in Generic Component drivers (child device drivers) so for compatibility with 
// existing Generic Component drivers, the name is used. This parse method accepts a list of one or more SendEvent-type Maps and determines how those Hubitat sendEvent Maps should be handled.
// The List of SendEvent Maps may include event Maps that are not needed by a particular driver (as determined based on the attributes of the driver)
// and those "extra" Maps are filtered out and discarded. This allows a more generic "event Map" producting method (e.g., matterTools.createListOfMatterSendEventMaps) to produce a 
// collection of potential SendEvent maps which a driver then filters to remove those that are not needed.
void parse(List<Map> sendEventTypeOfEvents) {
    try {
		List updateLocalStateOnlyAttributes = ["Binding", "UserLabelList", "FixedLabelList"]
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
		}
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } 
}

// This parser handles the Matter event message originating from Hubitat.
void parse(String description) {
    try {
        Map decodedDescMap = parseDescriptionAsDecodedMap(description) // Using parser from matterTools.parseDescriptionAsDecodedMap
        storeRetrievedData(decodedDescMap)

        if ( decodedDescMap.clusterInt  == 0x0050 ) {
             handleModeSelectClusterUpdate(decodedDescMap)
             return
        }
        
        List<Map> hubEvents = getHubitatEvents(decodedDescMap)
        if (hubEvents.is(null)) {  return }
        sendEventsToEndpointByParse(events:hubEvents, ep:(decodedDescMap.endpointInt))

    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void updated(){
    log.info "${device.displayName}: Processing Preference changes..."
    if (logEnable) {
		log.info "${device.displayName}: Debug logging enabled for 30 minutes"
		runIn(1800,logsOff)
	}

    for (ep in 1..6) {
        String modeSettingValue = device.getSetting("ModeSelect_${ep}")
        if (! modeSettingValue.is( null )) {
            Integer updatedMode = Integer.parseInt(modeSettingValue, 10)
            if (updatedMode == getModeSelectCurrentMode(ep)) continue
            if (txtEnable) log.info "${device.displayName}: Setting preference ${getModeSelectLabel(ep)} to value ${getModeSelectOptions(ep).get(updatedMode)}"
            changeToMode(ep:ep, mode:updatedMode)
        }
    }
}

void logsOff(){
    if (txtEnable) "${device.displayName}: Turning off Debug logging."
    device.updateSetting("logEnable", [value:"false",type:"bool"])
}

void configure(){
    if (txtenable) log.info  "${device.displayName}: Initializing device setup"
    if (logEnable) log.debug "${device.displayName}: Checking for child devices and subscribing to Matter event reports"
    // clearStoredDeviceData()
    // unsubscribeAll()
    checkAndCreateChildDevices()
    if (! getStoredDeviceData()) {
        resubscribeAll()
        refresh()
    }
}

// See Matter Core Spec. Section 2.11.2.2 - Interaction Model Limits for the number of subscriptions permitted.
// Guaranteed to support at least 3 Subscriber Interactions, each with at least 3 attribute/event paths.
void resubscribeAll(){
    // An error in 
    /*
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(0xFFFF, 0x0003, 0xFFFFFFFF)) // Identify
    attributePaths.add(matter.attributePath(0xFFFF, 0x0006, 0xFFFFFFFF)) // On/Off
    attributePaths.add(matter.attributePath(0xFFFF, 0x0008, 0xFFFFFFFF)) // Level Select
    attributePaths.add(matter.attributePath(0xFFFF, 0x0300, 0xFFFFFFFF)) // Color Control
    attributePaths.add(matter.attributePath(0xFFFF, 0x0050, 0xFFFFFFFF)) // Mode Select
    String cmd = matter.subscribe(3, 0x0700, attributePaths)
    */
    
    String cmd = 'he subscribe 0x01 0x700 [ {"ep":"0xFFFF","cluster":"0x0003","attr":"0x0000"}, \
                                            {"ep":"0xFFFF","cluster":"0x0006","attr":"0x0000"}, \
                                            {"ep":"0xFFFF","cluster":"0x0008","attr":"0xFFFFFFFF"}, \
                                            {"ep":"0xFFFF","cluster":"0x0300","attr":"0x0000"}, \
                                            {"ep":"0xFFFF","cluster":"0x0300","attr":"0x0001"}, \
                                            {"ep":"0xFFFF","cluster":"0x0050","attr":"0xFFFFFFFF"}]'
    
    if (logEnable) log.debug "${device.displayName}: Subscribing to device attribute reports with a 1 second minimum report delay, refresh at least every 30 minutes."
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}
void unsubscribeAll(){
    if (logEnable) log.debug "${device.displayName}: Unsubscribing from Matter attribute changes."
    sendHubCommand(new hubitat.device.HubAction(matter.unsubscribe(), hubitat.device.Protocol.MATTER)) // unsubscribe

}
void eventSubscribe(){
    if (logEnable) log.debug "${device.displayName}: Subscribing to device event reports with a 1 second minimum report delay, refresh at least every 30 minutes."
    String cmd = 'he subscribe 0x0001 0x0700 [{"ep":"0xFFFF","cluster":"0x003B","evt":"0x01"},{"ep":"0xFFFF","cluster":"0x003B","evt":"0x03"},{"ep":"0xFFFF","cluster":"0x003B","evt":"0x05"}]'
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

void componentInitialize(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void initialize(){ 
    if (logEnable) log.debug "${device.displayName}: Initialize called!"
    configure()
}

void componentRefresh(com.hubitat.app.DeviceWrapper cd) { refreshMatter(ep:getEndpointIdInt(cd)) }
void refresh() {
    if (txtEnable) log.info "${device.displayName}: Refreshing all device data."
    refreshMatter(ep:0xFFFF, clusterInt: 0x0040, attrInt: 0x0000) // get the labels first just in case child devices are to be created!
    refreshMatter(ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF)
}
// ~~~~~ start include (17) matterTools.matterEnumTypes ~~~~~
library ( // library marker matterTools.matterEnumTypes, line 1
        base: "driver", // library marker matterTools.matterEnumTypes, line 2
        author: "jvm33", // library marker matterTools.matterEnumTypes, line 3
        category: "matter", // library marker matterTools.matterEnumTypes, line 4
        description: "Create Hubitat Events from Matter Attribute Data", // library marker matterTools.matterEnumTypes, line 5
        name: "matterEnumTypes", // library marker matterTools.matterEnumTypes, line 6
        namespace: "matterTools", // library marker matterTools.matterEnumTypes, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.matterEnumTypes, line 8
		version: "0.0.1" // library marker matterTools.matterEnumTypes, line 9
) // library marker matterTools.matterEnumTypes, line 10
import groovy.transform.Field // library marker matterTools.matterEnumTypes, line 11
import groovy.transform.CompileStatic // library marker matterTools.matterEnumTypes, line 12

// Identify Cluster 0x0003 Enum Data Types (Matter Cluster Spec. Section 1.2.5) // library marker matterTools.matterEnumTypes, line 14
@Field static Map IdentifyTypeEnum =   [ 0:"None",     1:"LightOutput",    2:"VisibleIndicator",    3:"AudibleBeep",    4:"Display",    5:"Actuator"] // Cluste 0x0003 // library marker matterTools.matterEnumTypes, line 15
@Field static Map EffectIdentifierEnumType = [ 0:"Blink", 1:"Breathe", 2:"Okay", 0x0B:"ChannelChange", 0xFE:"FinishEffect", 0xFF:"StopEffect"] // library marker matterTools.matterEnumTypes, line 16
@Field static Map EffectVariantEnumType = [0:"Default"] // library marker matterTools.matterEnumTypes, line 17

// OnOff Cluster 0x0006 Enum Data Types (Matter Cluster Spec. Section 1.2.5) // library marker matterTools.matterEnumTypes, line 19
@Field static Map StartUpOnOffEnum =   [ 0:"Off",     1:"On",    2:"Toggle"] // Cluster 0x0006 // library marker matterTools.matterEnumTypes, line 20
@Field static Map EffectIdentifierEnum = [0:"DelayedAllOff", 1:"DyingLIght"] // library marker matterTools.matterEnumTypes, line 21
@Field static Map DelayedAllOffEffectVariantEnum = [0:"DelayedAllOffEffectVariantEnum", 1:"NoFade", 2:"DelayedOffSlowFade"] // library marker matterTools.matterEnumTypes, line 22
@Field static Map DyingLightEffectVariantEnumType = [0:"DyingLightFadeOff"] // library marker matterTools.matterEnumTypes, line 23

// Level Cluster 0x0008 Enum Data Types (Matter Cluster Spec. Section 1.6.5) // library marker matterTools.matterEnumTypes, line 25
// Haven't needed any yet, so not added! // library marker matterTools.matterEnumTypes, line 26

// Color Cluster 0x0300 Enum Data Types (Matter Cluster Spec. Section 3.2) // library marker matterTools.matterEnumTypes, line 28
// None Defined! // library marker matterTools.matterEnumTypes, line 29

// Basic Information Cluster 0x0028 Enum Data Types (Matter Cluster Spec. Section 11.1.4) // library marker matterTools.matterEnumTypes, line 31
@Field static Map ProductFinishEnumType = [0:"Other", 1:"Matte", 2:"Satin", 3:"Polished", 4:"Rugged", 5:"Fabric"] // library marker matterTools.matterEnumTypes, line 32
@Field static Map ColorEnumType = [ // library marker matterTools.matterEnumTypes, line 33
	0:"Black", 1:"Navy", 2:"Green", 3:"Teal", 4:"Maroon", 5:"Purple", 6:"Olive", 7:"Gray",  // library marker matterTools.matterEnumTypes, line 34
	8:"Blue", 9:"Lime", 10:"Aqua", 11:"Red", 12:"Fuscia", 13:"Yellow", 14:"White",  // library marker matterTools.matterEnumTypes, line 35
	15:"Nickel", 16:"Chrome", 17:"Brass", 18:"Copper", 19:"Silver", 20:"Gold" // library marker matterTools.matterEnumTypes, line 36
    ] // library marker matterTools.matterEnumTypes, line 37

// Air Quality Cluster 0x005B Enum Data Types (Matter Cluster Spec. Section 2.9.5) // library marker matterTools.matterEnumTypes, line 39
@Field static Map AirQualityEnumType = [ 0:"Unknown",     1:"Good",    2:"Fair",    3:"Moderate",    4:"Poor",    5:"VeryPoor",    6:"ExtremelyPoor"] // library marker matterTools.matterEnumTypes, line 40

// Smoke CO Alarm Cluster (Matter Cluster Spec. Section 2.11.5) // library marker matterTools.matterEnumTypes, line 42
@Field static Map AlarmStateEnum = [0:"Normal", 1:"Warning", 2:"Critical" ] // library marker matterTools.matterEnumTypes, line 43
@Field static Map SensitivityEnum = [0:"High", 1:"Standard", 2:"Low"] // library marker matterTools.matterEnumTypes, line 44
@Field static Map ExpressedStateEnum = [0:"Normal", 1:"SmokeAlarm", 2:"COAlarm", 3:"BatteryAlert", 4:"Testing", 5:"HardwareFault", 6:"EndOfService", 7:"InterconnectSmoke", 8:"InterconnectCO"] // library marker matterTools.matterEnumTypes, line 45
@Field static Map MuteStateEnum = [0:"NotMuted", 1:"Muted" ] // library marker matterTools.matterEnumTypes, line 46
@Field static Map EndOfServiceEnum = [0:"Normal", 1:"Expired"] // library marker matterTools.matterEnumTypes, line 47
@Field static Map ContaminationStateEnum = [0:"Normal", 1:"Low", 2:"Warning", 3:"Critical" ] // library marker matterTools.matterEnumTypes, line 48

// Thread Network Diagnostics Cluster 0x0035 (Matter **Core** Spec. Section 11.13.5) // library marker matterTools.matterEnumTypes, line 50
@Field static Map NetworkFaultEnum = [0:"Unspecified", 1:"LinkDown", 2:"HardwareFailure", 3:"NetworkJammed"] // library marker matterTools.matterEnumTypes, line 51
// ConnectionStatusEnum - same as WiFi // library marker matterTools.matterEnumTypes, line 52
@Field static Map RoutingRoleEnum = [0:"Unspecified", 1:"Unassigned", 2:"SleepyEndDevice", 3:"EndDevice", 4:"REED", 5:"Router", 6:"Leader"] // library marker matterTools.matterEnumTypes, line 53
// (Many other types not included) // library marker matterTools.matterEnumTypes, line 54


// Wi-Fi NEtwork Diagnostics Cluster 0x0036 (Matter **Core** Spec. Section 11.14.5) // library marker matterTools.matterEnumTypes, line 57
@Field static Map SecurityTypeEnum = [0:"Unspecified", 1:"None", 2:"WEP", 3:"WPA", 4:"WPA2", 5:"WPA3"] // library marker matterTools.matterEnumTypes, line 58
@Field static Map WiFiVersionEnum = [0:"a", 1:"b", 2:"g", 3:"n", 4:"ac", 5:"ax", 6:"ah"] // library marker matterTools.matterEnumTypes, line 59
@Field static Map AssociationFailureCauseEnum = [0:"Unknown", 1:"AssociationFailed", 2:"AuthenticationFailed", 3:"SsidNotFound"] // library marker matterTools.matterEnumTypes, line 60
@Field static Map ConnectionStatusEnum = [0:"Connected", 1:"NotConnected"] // library marker matterTools.matterEnumTypes, line 61

// Ehternet Network Diagnostics Cluster 0x0037 (Matter **Core** Spec. Section 11.15.5) // library marker matterTools.matterEnumTypes, line 63
@Field static Map PHYRateEnum = [0:"Rate10M", 1:"Rate100M", 2:"Rate1G", 3:"Rate2_5G", 4:"Rate5G", 5:"Rate10G", 6:"Rate40G", 7:"Rate100G", 8:"Rate200G", 9:"Rate400G"] // library marker matterTools.matterEnumTypes, line 64


// PowerSource Cluster 0x002F (Matter **Core** Spec. Section 11.7.5) // library marker matterTools.matterEnumTypes, line 67
@Field static Map WiredFaultEnum = [0:"Unspecified", 1:"OverVoltage", 2:"UnderVoltage"] // library marker matterTools.matterEnumTypes, line 68
@Field static Map BatFaultEnum = [0:"Unspecified", 1:"OverTemp", 2:"UnderTemp"] // library marker matterTools.matterEnumTypes, line 69
@Field static Map BatChargeFaultEnum = [ // library marker matterTools.matterEnumTypes, line 70
	0:"Unspecified", 1:"AmbientTooHot", 2:"AmbientTooCold", 3:"BatteryTooHot", 4:"BatteryTooCold", 5:"BatteryAbsent",  // library marker matterTools.matterEnumTypes, line 71
	6:"BatteryOverVoltage", 7:"BatteryUnderVoltage", 8:"ChargerOverVoltage", 9:"ChargerUnderVoltage", 10:"SafetyTimeout",  // library marker matterTools.matterEnumTypes, line 72
	11:"ChargerOverCurrent", 12:"UnexpectedVoltage", 13:"ExpectedVoltage", 14:"GroundFault", 15:"ChargeSignalFailure", 16:"SafetyTimeout"  // library marker matterTools.matterEnumTypes, line 73
    ] // library marker matterTools.matterEnumTypes, line 74
@Field static Map PowerSourceStatusEnum = [ 0:"Unspecified", 1:"Active", 2:"Standby", 3:"Unavailable"] // library marker matterTools.matterEnumTypes, line 75
@Field static Map WiredCurrentTypeEnum = [ 0:"AC", 1:"DC"] // library marker matterTools.matterEnumTypes, line 76
@Field static Map BatChargeLevelEnum = [ 0:"OK", 1:"Warning", 2:"Critical"] // library marker matterTools.matterEnumTypes, line 77
@Field static Map BatReplaceabilityEnum = [ 0:"Unspecified", 1:"NotReplaceable", 2:"UserReplaceable", 3:"FactoryReplaceable"] // library marker matterTools.matterEnumTypes, line 78
@Field static Map BatCommonDesignationEnum = [  // library marker matterTools.matterEnumTypes, line 79
	0:"Unspecified", 1:"AAA", 2:"AA", 3:"C", 4:"D", 5:"4v5", 6:"6v0", 7:"9v0",  // library marker matterTools.matterEnumTypes, line 80
	8:"1_2AA", 9:"AAAA", 10:"A", 11:"B", 12:"F", 13:"N", 14:"No6", 15:"SubC", 16:"A23", // library marker matterTools.matterEnumTypes, line 81
	17:"A27", 18:"BA5800", 19:"Duplex", 20:"4SR44", 21:"523", 22:"531", 23:"15V0", 24:"22v5", // library marker matterTools.matterEnumTypes, line 82
	25:"30v0", 26:"45v0", 27: "67v5", 28:"J", 29:"CR123A", 30:"CR2", 31:"2CR5", 32:"CR_P2", 33:"CR_V3", // library marker matterTools.matterEnumTypes, line 83
	34:"SR41", 35:"SR43", 36:"SR44", 37:"SR45", 38:"SR48", 39:"SR54", 40:"SR55", 41:"SR57", 42:"SR58", // library marker matterTools.matterEnumTypes, line 84
	43:"SR59", 44:"SR60", 45:"SR63", 46:"SR64", 47:"SR65", 48:"SR66", 49:"SR67", 50:"SR68", 51:"SR69", // library marker matterTools.matterEnumTypes, line 85
	52:"SR516", 53:"SR731", 54:"SR712", 55:"LR932", 56:"A5", 57:"A10", 58:"A13",  // library marker matterTools.matterEnumTypes, line 86
	59:"A312", 60:"A675", 61:"AC41E", 62:"10180", 63:"10280", 64:"10440", 65:"14250", 66:"14430", // library marker matterTools.matterEnumTypes, line 87
	67:"14500", 68:"14650", 69:"15270", 70:"16340", 71:"RCR123A", 72:"17500", 73:"17670", 74:"18350", // library marker matterTools.matterEnumTypes, line 88
	75:"18500", 76:"18650", 77:"19670", 78:"2550", 79:"26650", 80:"32600" // library marker matterTools.matterEnumTypes, line 89
    ] // library marker matterTools.matterEnumTypes, line 90
@Field static Map BatApprovedChemistryEnum = [ // library marker matterTools.matterEnumTypes, line 91
	0:"Unspecified",  // library marker matterTools.matterEnumTypes, line 92
	1:"Alkaline", 2:"LithiumCarbonFluoride", 3:"LithiumChromiumOxide", 4:"LithiumCopperOxide", 5:"LithiumIronDisulfide",  // library marker matterTools.matterEnumTypes, line 93
	6:"LithiumManganeseDioxide", 7:"LithiumThionylChloride", 8:"Magnesium", 9:"MercuryOxide", 10:"NickelOxyhydride",  // library marker matterTools.matterEnumTypes, line 94
	11:"SilverOxide", 12:"ZincAir", 13:"ZincCarbon", 14:"ZincChloride", 15:"ZincManganeseDioxide",  // library marker matterTools.matterEnumTypes, line 95
	16:"LeadAcid", 17:"LithiumCobaltOxide", 18:"LithiumIon", 19:"LithiumIonPolymer", 20:"LithiumIronPhosphate",  // library marker matterTools.matterEnumTypes, line 96
	21:"LithiumSulfur", 22:"LithiumTitanate", 23:"NickelCadmium", 24:"NickelHydrogen", 25:"NickelIron",  // library marker matterTools.matterEnumTypes, line 97
	26:"NickelMetalHydride", 27:"NickelZinc", 28:"SilverZinc", 29:"SodiumIon", 30:"SodiumSulfur",  // library marker matterTools.matterEnumTypes, line 98
	31:"ZincBromide", 32:"ZincCerium", // library marker matterTools.matterEnumTypes, line 99
    ]     // library marker matterTools.matterEnumTypes, line 100
@Field static Map BatChargeStateEnum = [ 0:"Unknown", 1:"IsCharging", 2:"IsAtFullCharge", 3:"IsNotCharging", 4:"IsDischarging", 5:"IsTransitioning", ] // library marker matterTools.matterEnumTypes, line 101

// Concentration Measurement Cluster 0x040C (Matter Spec. Section 2.10.5) // library marker matterTools.matterEnumTypes, line 103
@Field static Map MeasurementUnitEnum =   [ 0:"PPM", 1:"PPB", 2:"PPT", 3:"MGM3", 4:"UGM3", 5:"NGM3", 6:"PM3" ] // library marker matterTools.matterEnumTypes, line 104
@Field static Map MeasurementMediumEnum = [ 0:"Air", 1:"Water", 2:"Soil" ] // library marker matterTools.matterEnumTypes, line 105
@Field static Map LevelValueEnum =        [ 0:"Unknown", 1:"Low", 2:"Medium", 3:"High", 4:"Critical" ] // library marker matterTools.matterEnumTypes, line 106



// ~~~~~ end include (17) matterTools.matterEnumTypes ~~~~~

// ~~~~~ start include (21) matterTools.InovelliEndpointAndChildDeviceTools ~~~~~
library ( // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 1
        base: "driver", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 2
        author: "jvm33", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 3
        category: "matter", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 4
        description: "Child device Support Functions", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 5
        name: "InovelliEndpointAndChildDeviceTools", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 6
        namespace: "matterTools", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 8
		version: "0.5.0" // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 9
) // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 10

void checkAndCreateChildDevices(){ // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 12
    try{ // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 13
       if (!getChildDeviceListByEndpoint(ep:1)) addChildDevice("matterTools",  "Matter Generic Component Dimmer", "${device.deviceNetworkId}-ep0x0001" , [isComponent:false, name:null, label: "Load Control", endpointId:"0001"]) // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 14
       if (!getChildDeviceListByEndpoint(ep:6)) addChildDevice("matterTools" , "Matter Generic Component RGBW",   "${device.deviceNetworkId}-ep0x0006" , [isComponent:false, name:null, label: "LED Alert Strip", endpointId:"0006"]) // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 15
    } catch(e){ // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 16
        log.error "<pre>${e}<br><br>when processing checkAndCreateChildDevices:<br>${getStackTrace(e) }" // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 17
    } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 18
} // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 19

// This next function will generally override device.getEndpointId() // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 21
Integer getEndpointIdInt(com.hubitat.app.DeviceWrapper thisDevice) { // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 22
	String rValue =  thisDevice?.getDataValue("endpointId") ?:   thisDevice?.endpointId  // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 23
    if (rValue.is( null )) {  // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 24
        log.error "Device ${thisDevice.displayName} does not have a defined endpointId. Fix this!" // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 25
        return null // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 26
    } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 27
	return Integer.parseInt(rValue, 16) // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 28
} // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 29

// Get all the child devices for a specified endpoint.  This allows for possibility of multiple child devices per endpoint. // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 31
// May want to simplify this to only allow 1 child device per endpoint. // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 32
List<com.hubitat.app.DeviceWrapper> getChildDeviceListByEndpoint( Map params = [:] ) { // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 33
    Map inputs = [ep: null ] << params // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 34
    assert inputs.ep instanceof Integer // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 35
	childDevices.findAll{ getEndpointIdInt(it) == inputs.ep } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 36
} // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 37

// Uses a parse routine to manage sendEvent message distribution // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 39
// The passing of a sendEvent event to a parse routine is a technique used in Hubitat's Generic Component drivers, so its adopted here. // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 40
void sendEventsToEndpointByParse(Map params = [:]) { // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 41
    Map inputs = [ events: null , ep: null ] << params // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 42
    assert inputs.events instanceof List // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 43
    assert inputs.ep instanceof Integer // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 44

	List<com.hubitat.app.DeviceWrapper> targetDevices = getChildDeviceListByEndpoint(ep:(inputs.ep)) // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 46
	// if ((inputs.ep == getEndpointIdInt(device)) || (inputs.ep == 0) )  { targetDevices += this } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 47
	if (inputs.ep == 0)  { targetDevices += this } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 48

	targetDevices.each { it.parse(inputs.events) } // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 50
} // library marker matterTools.InovelliEndpointAndChildDeviceTools, line 51

// ~~~~~ end include (21) matterTools.InovelliEndpointAndChildDeviceTools ~~~~~

// ~~~~~ start include (8) matterTools.identifyClusterMethods0x0003 ~~~~~
/*  // library marker matterTools.identifyClusterMethods0x0003, line 1
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.2 "Identify Cluster" // library marker matterTools.identifyClusterMethods0x0003, line 2
Dependencies: Need to import the following // library marker matterTools.identifyClusterMethods0x0003, line 3
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function // library marker matterTools.identifyClusterMethods0x0003, line 4

Library also assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt).  // library marker matterTools.identifyClusterMethods0x0003, line 6
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using // library marker matterTools.identifyClusterMethods0x0003, line 7
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.identifyClusterMethods0x0003, line 8
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.identifyClusterMethods0x0003, line 9
See matterTools.commonDriverMethods library for example // library marker matterTools.identifyClusterMethods0x0003, line 10
*/ // library marker matterTools.identifyClusterMethods0x0003, line 11

library ( // library marker matterTools.identifyClusterMethods0x0003, line 13
        base: "driver", // library marker matterTools.identifyClusterMethods0x0003, line 14
        author: "jvm33", // library marker matterTools.identifyClusterMethods0x0003, line 15
        category: "matter", // library marker matterTools.identifyClusterMethods0x0003, line 16
        description: "Identify Cluster 0x0003 Tools", // library marker matterTools.identifyClusterMethods0x0003, line 17
        name: "identifyClusterMethods0x0003", // library marker matterTools.identifyClusterMethods0x0003, line 18
        namespace: "matterTools", // library marker matterTools.identifyClusterMethods0x0003, line 19
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.identifyClusterMethods0x0003, line 20
		version: "0.0.1" // library marker matterTools.identifyClusterMethods0x0003, line 21
) // library marker matterTools.identifyClusterMethods0x0003, line 22
import hubitat.helper.HexUtils // library marker matterTools.identifyClusterMethods0x0003, line 23

// Implements Cluster Spec Section 1.2.7, Identify command 0x00 // library marker matterTools.identifyClusterMethods0x0003, line 25
// Note that identifyTime is in seconds - many Matter commands use tenths! // library marker matterTools.identifyClusterMethods0x0003, line 26
void componentIdentify(com.hubitat.app.DeviceWrapper cd, timeInSeconds) { identify(ep:getEndpointIdInt(cd), identifyTime:timeInSeconds) } // library marker matterTools.identifyClusterMethods0x0003, line 27
void identify(timeInSeconds){     identify(identifyTime:timeInSeconds as Integer) } // library marker matterTools.identifyClusterMethods0x0003, line 28
void identify( Map params = [:] ){ // library marker matterTools.identifyClusterMethods0x0003, line 29
    try { // library marker matterTools.identifyClusterMethods0x0003, line 30
        Map inputs = [ep: getEndpointIdInt(device), identifyTime: 10] << params // library marker matterTools.identifyClusterMethods0x0003, line 31
        assert inputs.identifyTime instanceof Integer // library marker matterTools.identifyClusterMethods0x0003, line 32
        assert inputs.ep instanceof Integer // library marker matterTools.identifyClusterMethods0x0003, line 33

        String timeStringHex = HexUtils.integerToHexString(inputs.identifyTime, 2) //  is uint16 - two byte (4 Octet) field. // library marker matterTools.identifyClusterMethods0x0003, line 35

        List<Map<String, String>> fields = [] // library marker matterTools.identifyClusterMethods0x0003, line 37
       fields.add(matter.cmdField(DataType.UINT16,  0, (timeStringHex[2..3] + timeStringHex[0..1]) )) // IdentifyTime uint16 0-65534, byte reversed. "0A00" means 10 seconds // library marker matterTools.identifyClusterMethods0x0003, line 38

        String cmd = matter.invoke(inputs.ep, 0x0003, 0x0000, fields) // command 0x0000 is the identify command, it has a IdentifyTime parameter // library marker matterTools.identifyClusterMethods0x0003, line 40
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))   // library marker matterTools.identifyClusterMethods0x0003, line 41
    } catch(AssertionError e) { // library marker matterTools.identifyClusterMethods0x0003, line 42
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e)}" // library marker matterTools.identifyClusterMethods0x0003, line 43
    } catch(e){ // library marker matterTools.identifyClusterMethods0x0003, line 44
        log.error "<pre>${e}<br><br>when processing identify with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.identifyClusterMethods0x0003, line 45
    } // library marker matterTools.identifyClusterMethods0x0003, line 46
} // library marker matterTools.identifyClusterMethods0x0003, line 47

// ~~~~~ end include (8) matterTools.identifyClusterMethods0x0003 ~~~~~

// ~~~~~ start include (4) matterTools.OnOffClusterMethods0x0006 ~~~~~
/*  // library marker matterTools.OnOffClusterMethods0x0006, line 1
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.5 "On/Off Cluster" // library marker matterTools.OnOffClusterMethods0x0006, line 2
Dependencies: Need to import the following // library marker matterTools.OnOffClusterMethods0x0006, line 3
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function if you have not defined your own! // library marker matterTools.OnOffClusterMethods0x0006, line 4

Library also assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt).  // library marker matterTools.OnOffClusterMethods0x0006, line 6
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using // library marker matterTools.OnOffClusterMethods0x0006, line 7
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.OnOffClusterMethods0x0006, line 8
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.OnOffClusterMethods0x0006, line 9
See matterTools.commonDriverMethods library for example // library marker matterTools.OnOffClusterMethods0x0006, line 10
*/ // library marker matterTools.OnOffClusterMethods0x0006, line 11

library ( // library marker matterTools.OnOffClusterMethods0x0006, line 13
        base: "driver", // library marker matterTools.OnOffClusterMethods0x0006, line 14
        author: "jvm33", // library marker matterTools.OnOffClusterMethods0x0006, line 15
        category: "matter", // library marker matterTools.OnOffClusterMethods0x0006, line 16
        description: "On Off Cluster 0x0006 Tools", // library marker matterTools.OnOffClusterMethods0x0006, line 17
        name: "OnOffClusterMethods0x0006", // library marker matterTools.OnOffClusterMethods0x0006, line 18
        namespace: "matterTools", // library marker matterTools.OnOffClusterMethods0x0006, line 19
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.OnOffClusterMethods0x0006, line 20
		version: "0.0.1" // library marker matterTools.OnOffClusterMethods0x0006, line 21
) // library marker matterTools.OnOffClusterMethods0x0006, line 22

import hubitat.helper.HexUtils // library marker matterTools.OnOffClusterMethods0x0006, line 24

Boolean supportsOffTimer(){ // library marker matterTools.OnOffClusterMethods0x0006, line 26
    if (logEnable) log.debug "supportsOffTime function not fully implemented in matterTools.OnOffCluster0x0006. Defaults to 'true' as this is Mandatory in Lighting device types" // library marker matterTools.OnOffClusterMethods0x0006, line 27
    return true // library marker matterTools.OnOffClusterMethods0x0006, line 28
} // library marker matterTools.OnOffClusterMethods0x0006, line 29


// off implements Matter 1.2 Cluster Spec Section 1.5.7.1, Off command // library marker matterTools.OnOffClusterMethods0x0006, line 32
void componentOff(com.hubitat.app.DeviceWrapper cd){ off(ep:getEndpointIdInt(cd)) } // "component" variant for legacy Generic Component child device driver support // library marker matterTools.OnOffClusterMethods0x0006, line 33
void off( Map params = [:] ){ // library marker matterTools.OnOffClusterMethods0x0006, line 34
    try {  // library marker matterTools.OnOffClusterMethods0x0006, line 35
        Map inputs = [ ep:getEndpointIdInt(device) ] << params // library marker matterTools.OnOffClusterMethods0x0006, line 36
        assert inputs.ep instanceof Integer  // Use Integer, not Hex!  // library marker matterTools.OnOffClusterMethods0x0006, line 37
        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x00), hubitat.device.Protocol.MATTER)) // library marker matterTools.OnOffClusterMethods0x0006, line 38
    } catch (AssertionError e) { // library marker matterTools.OnOffClusterMethods0x0006, line 39
        log.error "Incorrect parameter type or value used in off() method.<br><pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 40
    } catch(e){ // library marker matterTools.OnOffClusterMethods0x0006, line 41
        log.error "<pre>${e}<br><br>when processing off with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 42
    }    // library marker matterTools.OnOffClusterMethods0x0006, line 43
} // library marker matterTools.OnOffClusterMethods0x0006, line 44

// on implements Matter 1.2 Cluster Spec Section 1.5.7.2, On command // library marker matterTools.OnOffClusterMethods0x0006, line 46
void componentOn(com.hubitat.app.DeviceWrapper cd){ on( ep:getEndpointIdInt(cd)) } // "component" variant for legacy Generic Component child device driver support // library marker matterTools.OnOffClusterMethods0x0006, line 47
void on( Map params = [:] ){ // library marker matterTools.OnOffClusterMethods0x0006, line 48
    try {  // library marker matterTools.OnOffClusterMethods0x0006, line 49
        Map inputs = [ ep:getEndpointIdInt(device)] << params // library marker matterTools.OnOffClusterMethods0x0006, line 50
        assert inputs.ep instanceof Integer // Use Integer, not Hex! // library marker matterTools.OnOffClusterMethods0x0006, line 51

        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x01 ), hubitat.device.Protocol.MATTER))   // library marker matterTools.OnOffClusterMethods0x0006, line 53
    } catch (AssertionError e) { // library marker matterTools.OnOffClusterMethods0x0006, line 54
        log.error "Incorrect parameter type or value used in on() method.<br><pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 55
    } catch(e){ // library marker matterTools.OnOffClusterMethods0x0006, line 56
        log.error "<pre>${e}<br><br>when processing on with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 57
    }      // library marker matterTools.OnOffClusterMethods0x0006, line 58
} // library marker matterTools.OnOffClusterMethods0x0006, line 59


// toggleOnOff implements Matter 1.2 Cluster Spec Section 1.5.7.3, Toggle command // library marker matterTools.OnOffClusterMethods0x0006, line 62
// Omission of a "component" version is intentional since it is not needed for legacy Generic Child driver support // library marker matterTools.OnOffClusterMethods0x0006, line 63
// child device drivers can directly call the named parameter function supplying its endpoint in the call. // library marker matterTools.OnOffClusterMethods0x0006, line 64
void toggleOnOff( Map params = [:] ){ // library marker matterTools.OnOffClusterMethods0x0006, line 65
    try {  // library marker matterTools.OnOffClusterMethods0x0006, line 66
        Map inputs = [ ep:getEndpointIdInt(device)] << params // library marker matterTools.OnOffClusterMethods0x0006, line 67
        assert inputs.ep instanceof Integer // Use Integer, not Hex! // library marker matterTools.OnOffClusterMethods0x0006, line 68
        sendHubCommand(new hubitat.device.HubAction(matter.invoke(inputs.ep, 0x0006, 0x02), hubitat.device.Protocol.MATTER))   // library marker matterTools.OnOffClusterMethods0x0006, line 69
    } catch (AssertionError e) { // library marker matterTools.OnOffClusterMethods0x0006, line 70
        log.error "Incorrect parameter type or value used in toggleOnOff() method.<br><pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 71
    } catch(e){ // library marker matterTools.OnOffClusterMethods0x0006, line 72
        log.error "<pre>${e}<br><br>when processing toggleOnOff with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 73
    }      // library marker matterTools.OnOffClusterMethods0x0006, line 74
} // library marker matterTools.OnOffClusterMethods0x0006, line 75


//offWithEffect implements Matter 1.2 Cluster Spec Section 1.5.7.4, OffWithEffect command // library marker matterTools.OnOffClusterMethods0x0006, line 78
// Omission of a "component" version is intentional since it is not needed for legacy Generic Child driver support // library marker matterTools.OnOffClusterMethods0x0006, line 79
// child device drivers can directly call the named parameter function supplying its endpoint in the call. // library marker matterTools.OnOffClusterMethods0x0006, line 80
void offWithEffect( Map params = [:] ){  // library marker matterTools.OnOffClusterMethods0x0006, line 81
    try {  // library marker matterTools.OnOffClusterMethods0x0006, line 82
        Map inputs = [ ep: getEndpointIdInt(device), effectIdentifier: 0, effectVariant:0] << params // library marker matterTools.OnOffClusterMethods0x0006, line 83
        assert inputs.ep instanceof Integer // Use Integer, not Hex!  // library marker matterTools.OnOffClusterMethods0x0006, line 84
        assert inputs.effectIdentifier instanceof Integer && (0..1).contains(inputs.effectIdentifier) // library marker matterTools.OnOffClusterMethods0x0006, line 85
        assert (inputs.effectVariant instanceof Integer)  && (0..2).contains(inputs.effectVariant ) // library marker matterTools.OnOffClusterMethods0x0006, line 86

        List<Map<String, String>> fields = [] // library marker matterTools.OnOffClusterMethods0x0006, line 88
            fields.add(matter.cmdField(DataType.UINT8,  0, HexUtils.integerToHexString(inputs.effectIdentifier, 1) )) // effectIdentifier // library marker matterTools.OnOffClusterMethods0x0006, line 89
            fields.add(matter.cmdField(DataType.UINT16, 1, HexUtils.integerToHexString(inputs.effectVariant   , 1)  )) // effectVariant // library marker matterTools.OnOffClusterMethods0x0006, line 90

        String cmd = matter.invoke(inputs.ep, 0x0006, 0x40, fields) // library marker matterTools.OnOffClusterMethods0x0006, line 92
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.OnOffClusterMethods0x0006, line 93
    } catch (AssertionError e) { // library marker matterTools.OnOffClusterMethods0x0006, line 94
        log.error "Incorrect parameter type or value used in offWithEffect() method.<br><pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 95
    } catch(e){ // library marker matterTools.OnOffClusterMethods0x0006, line 96
        log.error "<pre>${e}<br><br>when processing offWithEffect with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 97
    }      // library marker matterTools.OnOffClusterMethods0x0006, line 98
} // library marker matterTools.OnOffClusterMethods0x0006, line 99


//onWithTimedOff implements Matter 1.2 Cluster Spec Section 1.5.7.6, OnWithTimedOff command // library marker matterTools.OnOffClusterMethods0x0006, line 102
// Omission of a "component" version is intentional since it is not needed for legacy Generic Child driver support // library marker matterTools.OnOffClusterMethods0x0006, line 103
// child device drivers can directly call the named parameter function supplying its endpoint in the call. // library marker matterTools.OnOffClusterMethods0x0006, line 104
void onWithTimedOff( Map params = [:] ){  // library marker matterTools.OnOffClusterMethods0x0006, line 105
    try {  // library marker matterTools.OnOffClusterMethods0x0006, line 106
        Map inputs = [ ep: getEndpointIdInt(device), onTime10ths: 10, offWaitTime10ths:0] << params // library marker matterTools.OnOffClusterMethods0x0006, line 107
        assert inputs.ep instanceof Integer // Use Integer, not Hex!  // library marker matterTools.OnOffClusterMethods0x0006, line 108
        assert inputs.onTime10ths instanceof Integer // library marker matterTools.OnOffClusterMethods0x0006, line 109
        assert inputs.offWaitTime10ths instanceof Integer // Doesn't seem to do anything! // library marker matterTools.OnOffClusterMethods0x0006, line 110

        String hexOnTime10ths =       HexUtils.integerToHexString(inputs.onTime10ths, 2)  // library marker matterTools.OnOffClusterMethods0x0006, line 112
        String hexOffWaitTime10ths =  HexUtils.integerToHexString(inputs.offWaitTime10ths, 2)  // library marker matterTools.OnOffClusterMethods0x0006, line 113

        List<Map<String, String>> fields = [] // library marker matterTools.OnOffClusterMethods0x0006, line 115
            fields.add(matter.cmdField(DataType.UINT8,  0, "00")) // OnOffControlBitmap // library marker matterTools.OnOffClusterMethods0x0006, line 116
            fields.add(matter.cmdField(DataType.UINT16, 1, (hexOnTime10ths[2..3] + hexOnTime10ths[0..1]) )) // OnTime, byte swapped // library marker matterTools.OnOffClusterMethods0x0006, line 117
            fields.add(matter.cmdField(DataType.UINT16, 2, (hexOffWaitTime10ths[2..3] + hexOffWaitTime10ths[0..1]) )) // OffWaitTime - guarded wait time, byte swapped // library marker matterTools.OnOffClusterMethods0x0006, line 118

        String cmd = matter.invoke(inputs.ep, 0x0006, 0x42, fields) // library marker matterTools.OnOffClusterMethods0x0006, line 120
        if (logEnable) log.debug "${device.displayName}: Turning on timed Off using parameters ${inputs}" // library marker matterTools.OnOffClusterMethods0x0006, line 121
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.OnOffClusterMethods0x0006, line 122
    } catch (AssertionError e) { // library marker matterTools.OnOffClusterMethods0x0006, line 123
        log.error "Incorrect parameter type or value used in onWithTimedOff() method.<br><pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 124
    } catch(e){ // library marker matterTools.OnOffClusterMethods0x0006, line 125
        log.error "<pre>${e}<br><br>when processing onWithTimedOff with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.OnOffClusterMethods0x0006, line 126
    }      // library marker matterTools.OnOffClusterMethods0x0006, line 127
} // library marker matterTools.OnOffClusterMethods0x0006, line 128

// ~~~~~ end include (4) matterTools.OnOffClusterMethods0x0006 ~~~~~

// ~~~~~ start include (6) matterTools.levelClusterMethods0x0008 ~~~~~
/*  // library marker matterTools.levelClusterMethods0x0008, line 1
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.6 ("Level Control Cluster") // library marker matterTools.levelClusterMethods0x0008, line 2
Dependencies: Need to import the following // library marker matterTools.levelClusterMethods0x0008, line 3
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function // library marker matterTools.levelClusterMethods0x0008, line 4
    matterTools.concurrentRuntimeDataStorage // needed to retrieve stored timing data // library marker matterTools.levelClusterMethods0x0008, line 5

Library also assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt).  // library marker matterTools.levelClusterMethods0x0008, line 7
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using // library marker matterTools.levelClusterMethods0x0008, line 8
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.levelClusterMethods0x0008, line 9
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.levelClusterMethods0x0008, line 10
See matterTools.commonDriverMethods library for example // library marker matterTools.levelClusterMethods0x0008, line 11
*/ // library marker matterTools.levelClusterMethods0x0008, line 12

library ( // library marker matterTools.levelClusterMethods0x0008, line 14
        base: "driver", // library marker matterTools.levelClusterMethods0x0008, line 15
        author: "jvm33", // library marker matterTools.levelClusterMethods0x0008, line 16
        category: "matter", // library marker matterTools.levelClusterMethods0x0008, line 17
        description: "level Control Cluster 0x0008 Tools", // library marker matterTools.levelClusterMethods0x0008, line 18
        name: "levelClusterMethods0x0008", // library marker matterTools.levelClusterMethods0x0008, line 19
        namespace: "matterTools", // library marker matterTools.levelClusterMethods0x0008, line 20
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.levelClusterMethods0x0008, line 21
		version: "0.0.1" // library marker matterTools.levelClusterMethods0x0008, line 22
) // library marker matterTools.levelClusterMethods0x0008, line 23
import groovy.transform.Field // library marker matterTools.levelClusterMethods0x0008, line 24
import groovy.transform.CompileStatic // library marker matterTools.levelClusterMethods0x0008, line 25
import java.lang.Math // library marker matterTools.levelClusterMethods0x0008, line 26
import hubitat.helper.HexUtils // library marker matterTools.levelClusterMethods0x0008, line 27

// Gets the OnOffTransitionTime parameter from stored data. // library marker matterTools.levelClusterMethods0x0008, line 29
// Needs the  // library marker matterTools.levelClusterMethods0x0008, line 30
Integer getOnOffTransitionTime(Map params = [:]){ // library marker matterTools.levelClusterMethods0x0008, line 31
    Map inputs = [ep:null] << params // library marker matterTools.levelClusterMethods0x0008, line 32
    assert inputs.ep instanceof Integer // library marker matterTools.levelClusterMethods0x0008, line 33
    String storedData = getStoredAttributeData(endpointInt:inputs.ep, clusterInt:0x0008, attrInt:0x0010) // library marker matterTools.levelClusterMethods0x0008, line 34
    return (storedData ? Integer.parseInt(storedData, 16) : (null as Integer)) // library marker matterTools.levelClusterMethods0x0008, line 35
} // library marker matterTools.levelClusterMethods0x0008, line 36

// Following functions implement Matter Spec 1.6.7.6 "MoveWithOnOFf and Section 1.6.7.2 "Move" commands" commands. // library marker matterTools.levelClusterMethods0x0008, line 38
// Move command 0x01 is used if moveWithOnOff is set to 'false' // library marker matterTools.levelClusterMethods0x0008, line 39
// MoveWithOnOff command 0x05 is used if movewithOnOff is set to true (default). // library marker matterTools.levelClusterMethods0x0008, line 40
void componentStartLevelChange(com.hubitat.app.DeviceWrapper cd, direction, rate = 50 ) { startLevelChange(ep:getEndpointIdInt(cd), direction:direction, rate:rate) } // library marker matterTools.levelClusterMethods0x0008, line 41
void startLevelChange(direction){ startLevelChange(ep:getEndpointIdInt(device), direction:direction)} // library marker matterTools.levelClusterMethods0x0008, line 42
void startLevelChange(Map params = [:] ){ // library marker matterTools.levelClusterMethods0x0008, line 43
    try {  // library marker matterTools.levelClusterMethods0x0008, line 44
        Map inputs = [ep:null, direction: null, rate:50, moveWithOnOff:true] << params // library marker matterTools.levelClusterMethods0x0008, line 45
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer // library marker matterTools.levelClusterMethods0x0008, line 46
        if (inputs.rate instanceof BigDecimal) inputs.rate = inputs.rate as Integer // library marker matterTools.levelClusterMethods0x0008, line 47
        assert inputs.rate instanceof Integer  // rate corresponds to parameter Rate of command 1.6.7.2. Measured in units, not time (1-254 per second) // library marker matterTools.levelClusterMethods0x0008, line 48
        assert ["up", "down"].contains(inputs.direction) // library marker matterTools.levelClusterMethods0x0008, line 49
        assert inputs.moveWithOnOff instanceof Boolean // library marker matterTools.levelClusterMethods0x0008, line 50

        String MoveMode = (inputs.direction == "up") ? "00" : "01" // library marker matterTools.levelClusterMethods0x0008, line 52
        String MoveRate = HexUtils.integerToHexString(inputs.rate, 1) // Units per second in hex. Default rate of 50 means about 5 seconds for 0 -> 100% transition // library marker matterTools.levelClusterMethods0x0008, line 53

        List<Map<String, String>> fields = [] // library marker matterTools.levelClusterMethods0x0008, line 55
        fields.add(matter.cmdField(DataType.UINT8, 0, MoveMode)) // MoveMode = 00 = Up, 01 = down per Matter spec 1.6.7.2 // library marker matterTools.levelClusterMethods0x0008, line 56
        fields.add(matter.cmdField(DataType.UINT8, 1, MoveRate)) // Move rate in units per second // library marker matterTools.levelClusterMethods0x0008, line 57
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8 // library marker matterTools.levelClusterMethods0x0008, line 58
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8 // library marker matterTools.levelClusterMethods0x0008, line 59

        String cmd = matter.invoke(inputs.ep, 0x0008, (inputs.moveWithOnOff ? 0x05 : 0x01), fields) // Move Up or Down // library marker matterTools.levelClusterMethods0x0008, line 61
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))      // library marker matterTools.levelClusterMethods0x0008, line 62
    } catch (AssertionError e) { // library marker matterTools.levelClusterMethods0x0008, line 63
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 64
    } catch(e){ // library marker matterTools.levelClusterMethods0x0008, line 65
        log.error "<pre>${e}<br><br>when processing startLevelChange with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 66
    } // library marker matterTools.levelClusterMethods0x0008, line 67
} // library marker matterTools.levelClusterMethods0x0008, line 68

// Following functions implement Matter Spec 1.6.7.4 "Stop" command. // library marker matterTools.levelClusterMethods0x0008, line 70
void componentStopLevelChange(com.hubitat.app.DeviceWrapper cd) { stopLevelChange(ep:getEndpointIdInt(cd)) } // library marker matterTools.levelClusterMethods0x0008, line 71
void stopLevelChange(direction){ stopLevelChange(ep:getEndpointIdInt(device))} // library marker matterTools.levelClusterMethods0x0008, line 72
void stopLevelChange( Map params = [:] ){ // library marker matterTools.levelClusterMethods0x0008, line 73
    try {  // library marker matterTools.levelClusterMethods0x0008, line 74
        Map inputs = [ ep:null ] << params // library marker matterTools.levelClusterMethods0x0008, line 75
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer // library marker matterTools.levelClusterMethods0x0008, line 76

        List<Map<String, String>> fields = [] // library marker matterTools.levelClusterMethods0x0008, line 78
        fields.add(matter.cmdField(DataType.UINT8,  0, "00")) // OptionMask, map8 // library marker matterTools.levelClusterMethods0x0008, line 79
        fields.add(matter.cmdField(DataType.UINT8,  1, "00"))  // OptionsOverride, map8 // library marker matterTools.levelClusterMethods0x0008, line 80
        String cmd = matter.invoke(inputs.ep, 0x0008, 0x03, fields) // Stop! // library marker matterTools.levelClusterMethods0x0008, line 81
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))      // library marker matterTools.levelClusterMethods0x0008, line 82
    } catch (AssertionError e) { // library marker matterTools.levelClusterMethods0x0008, line 83
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 84
    } catch(e){ // library marker matterTools.levelClusterMethods0x0008, line 85
        log.error "<pre>${e}<br><br>when processing stopLevelChange with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 86
    } // library marker matterTools.levelClusterMethods0x0008, line 87
} // library marker matterTools.levelClusterMethods0x0008, line 88

void componentSetLevel(com.hubitat.app.DeviceWrapper cd, cdLevel, cdDuration = null) { // library marker matterTools.levelClusterMethods0x0008, line 90
	if (cd.hasCapability("FanControl") ) { // library marker matterTools.levelClusterMethods0x0008, line 91
            throw new Exception("FanControl function not yet implemented in matterTools.levelCluster0x0008") // library marker matterTools.levelClusterMethods0x0008, line 92
			// need to add a function like ... setSpeed(level:cdLevel, speed:levelToSpeed(cdLevel as Integer), ep:getEndpointIdInt(cd)) // library marker matterTools.levelClusterMethods0x0008, line 93
		} else {  // library marker matterTools.levelClusterMethods0x0008, line 94
			setLevel(ep:getEndpointIdInt(cd), level:(cdLevel as Integer), transitionTime10ths: cdDuration.is(null) ? (null as Integer): ((cdDuration * 10) as Integer))  // library marker matterTools.levelClusterMethods0x0008, line 95
		} // library marker matterTools.levelClusterMethods0x0008, line 96
} // library marker matterTools.levelClusterMethods0x0008, line 97

// setLevel implements Matter 1.2 Cluster Spec Section 1.6.7.6, MoveToLevelWithOnOff command 0x04 // library marker matterTools.levelClusterMethods0x0008, line 99
void setLevel(inputLevel) {  setLevel(ep: getEndpointIdInt(device), level:(inputLevel as Integer) )}  // library marker matterTools.levelClusterMethods0x0008, line 100
void setLevel(inputLevel, durationSeconds) {  // library marker matterTools.levelClusterMethods0x0008, line 101
    setLevel(ep: getEndpointIdInt(device), level:(inputLevel as Integer), transitionTime10ths: (durationSeconds * 10 as Integer)) // convert time from seconds to 10ths of a second! // library marker matterTools.levelClusterMethods0x0008, line 102
} // library marker matterTools.levelClusterMethods0x0008, line 103
void setLevel(inputLevel, durationSeconds, remainOnTimeSeconds) {  // library marker matterTools.levelClusterMethods0x0008, line 104
    setLevel(ep: getEndpointIdInt(device), level:(inputLevel as Integer),  // library marker matterTools.levelClusterMethods0x0008, line 105
             transitionTime10ths: durationSeconds.is(null) ? (null as Integer) : (durationSeconds * 10 as Integer), // convert time from seconds to 10ths of a second! // library marker matterTools.levelClusterMethods0x0008, line 106
             onTime10ths: remainOnTimeSeconds.is(null) ? (null as Integer)     : (remainOnTimeSeconds * 10 as Integer), // convert time from seconds to 10ths of a second! // library marker matterTools.levelClusterMethods0x0008, line 107
            )  // library marker matterTools.levelClusterMethods0x0008, line 108
} // library marker matterTools.levelClusterMethods0x0008, line 109
void setLevel( Map params = [:] ) { // library marker matterTools.levelClusterMethods0x0008, line 110
    try {  // library marker matterTools.levelClusterMethods0x0008, line 111
        Map inputs = [ep: null , level: null , transitionTime10ths: null, onTime10ths: null ] << params // library marker matterTools.levelClusterMethods0x0008, line 112
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer // library marker matterTools.levelClusterMethods0x0008, line 113
        if (inputs.level instanceof BigDecimal) inputs.level = inputs.level as Integer // Web UI send BigDecimal but want Integer! Fix that. // library marker matterTools.levelClusterMethods0x0008, line 114
        assert inputs.level instanceof Integer // library marker matterTools.levelClusterMethods0x0008, line 115
        inputs.level = Math.round(Math.max(Math.min(inputs.level, 100), 0)) // level is a % and must be between 0 and 100 // library marker matterTools.levelClusterMethods0x0008, line 116

        // if level == 0, switch this to an "off" command. You could turn off by setting level to 0 // library marker matterTools.levelClusterMethods0x0008, line 118
        // but then the next turn-on level will often result in a 1% turn-on value which is generally not what was intended // library marker matterTools.levelClusterMethods0x0008, line 119
        // by converting to an "off" , the prior level will be restored for the next "on" action. // library marker matterTools.levelClusterMethods0x0008, line 120
        if (inputs.level == 0) { off(*:inputs) ; return } // library marker matterTools.levelClusterMethods0x0008, line 121

        // Per Matter Spec, if transitionTime is null, use OnOffTransitionTime attribute value. // library marker matterTools.levelClusterMethods0x0008, line 123
        // get that from previously retrieved data using getOnOffTransitionTime function or use 0 if that is unavailable.  // library marker matterTools.levelClusterMethods0x0008, line 124
        // transitionTime is nullable. See Matter cluster spec 0008, Section 1.6.7.1, and core spec section 7.18 (Data Types), // library marker matterTools.levelClusterMethods0x0008, line 125
        // so following the spec, you should be able to juse use a null value (FFFF) and not retrieve the stored value, but that doesn't work. // library marker matterTools.levelClusterMethods0x0008, line 126
        if (inputs.transitionTime10ths.is(null)) { inputs.transitionTime10ths = getOnOffTransitionTime(ep: inputs.ep) ?: 0   } // library marker matterTools.levelClusterMethods0x0008, line 127
        if (inputs.transitionTime10ths instanceof BigDecimal) inputs.transitionTime10ths = inputs.transitionTime10ths as Integer // library marker matterTools.levelClusterMethods0x0008, line 128
        assert inputs.transitionTime10ths instanceof Integer  // library marker matterTools.levelClusterMethods0x0008, line 129

        String hexLevel = HexUtils.integerToHexString((Integer) ( inputs.level  * 2.54), 1) // library marker matterTools.levelClusterMethods0x0008, line 131
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 ) // Time is in 10ths of a second! FFFF is the null value. // library marker matterTools.levelClusterMethods0x0008, line 132

        List<Map<String, String>> fields = [] // library marker matterTools.levelClusterMethods0x0008, line 134
        fields.add(matter.cmdField(DataType.UINT8, 0, hexLevel)) // Level // library marker matterTools.levelClusterMethods0x0008, line 135
        fields.add(matter.cmdField(DataType.UINT16, 1, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]) )) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped // library marker matterTools.levelClusterMethods0x0008, line 136
        fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8 // library marker matterTools.levelClusterMethods0x0008, line 137
        fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8 // library marker matterTools.levelClusterMethods0x0008, line 138
        if (logEnable) log.debug "fields are ${fields}" // library marker matterTools.levelClusterMethods0x0008, line 139
        String cmd = matter.invoke(inputs.ep, 0x0008, 0x04, fields) // Move To Level with On/Off // library marker matterTools.levelClusterMethods0x0008, line 140
        if (logEnable) log.debug "sending command with transitionTime10ths value ${inputs.transitionTime10ths}: ${cmd}" // library marker matterTools.levelClusterMethods0x0008, line 141
          sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))      // library marker matterTools.levelClusterMethods0x0008, line 142
        if (inputs.onTime10ths && (inputs.level > 0) ) { // library marker matterTools.levelClusterMethods0x0008, line 143
            onWithTimedOff(*:inputs) // library marker matterTools.levelClusterMethods0x0008, line 144
        } // library marker matterTools.levelClusterMethods0x0008, line 145
    } catch (AssertionError e) { // library marker matterTools.levelClusterMethods0x0008, line 146
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 147
    } catch(e){ // library marker matterTools.levelClusterMethods0x0008, line 148
        log.error "<pre>${e}<br><br>when processing setLevel with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.levelClusterMethods0x0008, line 149
    } // library marker matterTools.levelClusterMethods0x0008, line 150
} // library marker matterTools.levelClusterMethods0x0008, line 151


// ~~~~~ end include (6) matterTools.levelClusterMethods0x0008 ~~~~~

// ~~~~~ start include (1) matterTools.ColorClusterMethods0x0300 ~~~~~
/*  // library marker matterTools.ColorClusterMethods0x0300, line 1
Reference: Matter Application Cluster Specification Version 1.2, Section 3.2 // library marker matterTools.ColorClusterMethods0x0300, line 2
Dependencies: Need to import the following // library marker matterTools.ColorClusterMethods0x0300, line 3
    matterTools.endpointAndChildDeviceTools   // needed for getEndpointIdInt() function // library marker matterTools.ColorClusterMethods0x0300, line 4

Library also assumes that descMap also includes the endpoint as an integer (descMap.endpointIdInt).  // library marker matterTools.ColorClusterMethods0x0300, line 6
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using // library marker matterTools.ColorClusterMethods0x0300, line 7
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.ColorClusterMethods0x0300, line 8
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.ColorClusterMethods0x0300, line 9
See matterTools.commonDriverMethods library for example // library marker matterTools.ColorClusterMethods0x0300, line 10
*/ // library marker matterTools.ColorClusterMethods0x0300, line 11
library ( // library marker matterTools.ColorClusterMethods0x0300, line 12
        base: "driver", // library marker matterTools.ColorClusterMethods0x0300, line 13
        author: "jvm33", // library marker matterTools.ColorClusterMethods0x0300, line 14
        category: "matter", // library marker matterTools.ColorClusterMethods0x0300, line 15
        description: "Color Cluster 0x0300 Tools", // library marker matterTools.ColorClusterMethods0x0300, line 16
        name: "ColorClusterMethods0x0300", // library marker matterTools.ColorClusterMethods0x0300, line 17
        namespace: "matterTools", // library marker matterTools.ColorClusterMethods0x0300, line 18
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.ColorClusterMethods0x0300, line 19
		version: "0.0.1" // library marker matterTools.ColorClusterMethods0x0300, line 20
) // library marker matterTools.ColorClusterMethods0x0300, line 21

import java.lang.Math // library marker matterTools.ColorClusterMethods0x0300, line 23
import hubitat.helper.HexUtils // library marker matterTools.ColorClusterMethods0x0300, line 24

// Implements Matter Command MoveToHue (Command 0x00), Matter 1.2 Spec, Section 3.2.11.4 // library marker matterTools.ColorClusterMethods0x0300, line 26
void componentSetHue(com.hubitat.app.DeviceWrapper cd, hue) { setHue(hue:hue, ep:getEndpointIdInt(cd)) } // Component should provide hue as integer 0..100. No need to scale it. // library marker matterTools.ColorClusterMethods0x0300, line 27
void setHue(hue){ setHue(ep:getEndpointIdInt(device), hue:hue as Integer) } // Component should provide hue as integer 0..100. No need to scale it. // library marker matterTools.ColorClusterMethods0x0300, line 28
void setHue( Map params = [:] ){ // library marker matterTools.ColorClusterMethods0x0300, line 29
    try { // library marker matterTools.ColorClusterMethods0x0300, line 30
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue:null] << params // library marker matterTools.ColorClusterMethods0x0300, line 31
        assert inputs.ep instanceof Integer  // library marker matterTools.ColorClusterMethods0x0300, line 32
        // Hubitat sets hue in percent. Hue is a color wheel, so you could do a (hue % 100) and get to same place, but checking range is better way to catch errors. // library marker matterTools.ColorClusterMethods0x0300, line 33
        // If a developer is using raw (0..254) Matter values, or degrees 0..360, this is likely to eventuall trigger. // library marker matterTools.ColorClusterMethods0x0300, line 34
	    assert (inputs.hue >= 0) && (inputs.hue <= 100) : "Hue must be expresssed as a percent 0..100 %" // library marker matterTools.ColorClusterMethods0x0300, line 35
        if (inputs.transitionTime10ths instanceof BigDecimal) inputs.transitionTime10ths = inputs.transitionTime10ths as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 36
        assert inputs.transitionTime10ths instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 37
        // don't need to assertion check level - it will get tested in the setLevel method! // library marker matterTools.ColorClusterMethods0x0300, line 38

        Integer targetHue = Math.round(Math.max(Math.min((Integer) inputs.hue, 100), 0) * 2.54) as Integer // library marker matterTools.ColorClusterMethods0x0300, line 40

        String hexHue =                 HexUtils.integerToHexString(targetHue, 1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 42
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 ) // library marker matterTools.ColorClusterMethods0x0300, line 43

        List<Map<String, String>> fields = [] // library marker matterTools.ColorClusterMethods0x0300, line 45
            fields.add(matter.cmdField(DataType.UINT8,   0, hexHue)) // Hue uint8 0-254 // library marker matterTools.ColorClusterMethods0x0300, line 46
            fields.add(matter.cmdField(DataType.UINT8,   1, "00")) // Direction 00 = Shortest // library marker matterTools.ColorClusterMethods0x0300, line 47
            fields.add(matter.cmdField(DataType.UINT16,  2, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]))) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped // library marker matterTools.ColorClusterMethods0x0300, line 48
            fields.add(matter.cmdField(DataType.UINT8,   3, "00")) // OptionMask, map8 // library marker matterTools.ColorClusterMethods0x0300, line 49
            fields.add(matter.cmdField(DataType.UINT8,   4, "00"))  // OptionsOverride, map8 // library marker matterTools.ColorClusterMethods0x0300, line 50
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x00, fields) // Move To Hue Command is 0x00. Matter Spec. Section 3.2.11.4 // library marker matterTools.ColorClusterMethods0x0300, line 51
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))   // library marker matterTools.ColorClusterMethods0x0300, line 52

        if (inputs.level) setLevel(*:inputs)     // library marker matterTools.ColorClusterMethods0x0300, line 54

    } catch (AssertionError e) { // library marker matterTools.ColorClusterMethods0x0300, line 56
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 57
    } catch(e){ // library marker matterTools.ColorClusterMethods0x0300, line 58
        log.error "<pre>${e}<br><br>when processing setHue with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 59
    }  // library marker matterTools.ColorClusterMethods0x0300, line 60
} // library marker matterTools.ColorClusterMethods0x0300, line 61

// Implements Matter Command "MoveToSaturation" (command 0x03), Matter 1.2 Spec, Section 3.2.11.7  // library marker matterTools.ColorClusterMethods0x0300, line 63
void componentSetSaturation(com.hubitat.app.DeviceWrapper cd, saturation) { setSaturation(saturation:saturation, ep:getEndpointIdInt(cd)) } // library marker matterTools.ColorClusterMethods0x0300, line 64
void setSaturation(saturation){ setSaturation (ep:getEndpointIdInt(device), saturation:saturation as Integer) } // library marker matterTools.ColorClusterMethods0x0300, line 65
void setSaturation( Map params = [:] ){ // library marker matterTools.ColorClusterMethods0x0300, line 66
    try { // library marker matterTools.ColorClusterMethods0x0300, line 67
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, saturation:null, level:null ] << params // library marker matterTools.ColorClusterMethods0x0300, line 68
        assert inputs.ep instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 69
	    assert (inputs.saturation >= 0) && (inputs.saturation <= 100) // hubitat specifies saturation in percent // library marker matterTools.ColorClusterMethods0x0300, line 70
        if (inputs.transitionTime10ths instanceof BigDecimal) inputs.transitionTime10ths = inputs.transitionTime10ths as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 71
        assert inputs.transitionTime10ths instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 72
        // don't need to assertion check level - it will get tested in the setLevel method! // library marker matterTools.ColorClusterMethods0x0300, line 73
 	    Integer targetSat = Math.round(Math.max(Math.min((Integer) inputs.saturation, 100), 0) * 2.54) // library marker matterTools.ColorClusterMethods0x0300, line 74

 	    String hexSat = HexUtils.integerToHexString(targetSat, 1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 76
        String hexTransitionTime10ths = HexUtils.integerToHexString(inputs.transitionTime10ths, 2 ) // library marker matterTools.ColorClusterMethods0x0300, line 77

        List<Map<String, String>> fields = [] // library marker matterTools.ColorClusterMethods0x0300, line 79
            fields.add(matter.cmdField(DataType.UINT8,   0, hexSat)) // Saturation uint8 0-254 // library marker matterTools.ColorClusterMethods0x0300, line 80
            fields.add(matter.cmdField(DataType.UINT16,  1, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]) )) // TransitionTime uint16 0-65534, byte swapped // library marker matterTools.ColorClusterMethods0x0300, line 81
            fields.add(matter.cmdField(DataType.UINT8, 2, "00")) // OptionMask, map8 // library marker matterTools.ColorClusterMethods0x0300, line 82
            fields.add(matter.cmdField(DataType.UINT8, 3, "00"))  // OptionsOverride, map8 // library marker matterTools.ColorClusterMethods0x0300, line 83
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x03, fields) // Move To Saturation Command is 0x03. Matter Spec. Section 3.2.11.4 // library marker matterTools.ColorClusterMethods0x0300, line 84
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))   // library marker matterTools.ColorClusterMethods0x0300, line 85

        if (inputs.level) setLevel(*:inputs)     // library marker matterTools.ColorClusterMethods0x0300, line 87

    } catch (AssertionError e) { // library marker matterTools.ColorClusterMethods0x0300, line 89
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 90
    } catch(e){ // library marker matterTools.ColorClusterMethods0x0300, line 91
        log.error "<pre>${e}<br><br>when processing setSaturation with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 92
    } 		 // library marker matterTools.ColorClusterMethods0x0300, line 93
} // library marker matterTools.ColorClusterMethods0x0300, line 94

// Implements Matter Command "MoveToHueAndSaturation" (Command 0x06), Matter 1.2 Spec, Section 3.2.11.10 // library marker matterTools.ColorClusterMethods0x0300, line 96
void componentSetColor(com.hubitat.app.DeviceWrapper cd, Map colormap) { setColor(*:colormap, ep:getEndpointIdInt(cd)) } // library marker matterTools.ColorClusterMethods0x0300, line 97
void setColor(Map params = [:]){ // UI passes a Map so trying to set defaults here doesn't work. // library marker matterTools.ColorClusterMethods0x0300, line 98
    try { // library marker matterTools.ColorClusterMethods0x0300, line 99
        Map inputs = [ep:getEndpointIdInt(device), transitionTime10ths: 0, hue: null, saturation: null, level: null] << params // library marker matterTools.ColorClusterMethods0x0300, line 100
        assert inputs.ep instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 101
	    assert (inputs.saturation instanceof Integer) && (inputs.saturation >= 0) && (inputs.saturation <= 100) // library marker matterTools.ColorClusterMethods0x0300, line 102
        // Hubitat sets hue in percent. Hue is a color wheel, so you could do a (hue % 100) and get to same place, but checking range is better way to catch errors. // library marker matterTools.ColorClusterMethods0x0300, line 103
        // If a developer is using raw (0..254) Matter values, or degrees 0..360, this is likely to eventuall trigger. // library marker matterTools.ColorClusterMethods0x0300, line 104
	    assert (inputs.hue instanceof Integer) && (inputs.hue >= 0) && (inputs.hue <= 100) // library marker matterTools.ColorClusterMethods0x0300, line 105

        if (inputs.transitionTime10ths instanceof BigDecimal) inputs.transitionTime10ths = inputs.transitionTime10ths as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 107
        assert inputs.transitionTime10ths instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 108
         // don't need to assertion check level - it will get tested in the setLevel method! // library marker matterTools.ColorClusterMethods0x0300, line 109

        String hexHue =                 HexUtils.integerToHexString( (Integer) Math.round(inputs.hue * 2.54) ,  1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 111
 	    String hexSat =                 HexUtils.integerToHexString( (Integer) Math.round(inputs.saturation * 2.54),  1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 112
        String hexTransitionTime10ths = HexUtils.integerToHexString( inputs.transitionTime10ths,  2 ) // library marker matterTools.ColorClusterMethods0x0300, line 113

        List<Map<String, String>> fields = [] // library marker matterTools.ColorClusterMethods0x0300, line 115
            fields.add(matter.cmdField(DataType.UINT8,   0, hexHue)) // Hue uint8 0-254 // library marker matterTools.ColorClusterMethods0x0300, line 116
            fields.add(matter.cmdField(DataType.UINT8,   1, hexSat)) // Saturation uint8 0-254 // library marker matterTools.ColorClusterMethods0x0300, line 117
            // TransitionTime in 0.1 Seconds, uint16 0-65534, byte byte swap it for encoding! // library marker matterTools.ColorClusterMethods0x0300, line 118
            fields.add(matter.cmdField(DataType.UINT16,  2, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]) ))  // library marker matterTools.ColorClusterMethods0x0300, line 119
            fields.add(matter.cmdField(DataType.UINT8, 3, "00")) // OptionMask, map8 // library marker matterTools.ColorClusterMethods0x0300, line 120
            fields.add(matter.cmdField(DataType.UINT8, 4, "00"))  // OptionsOverride, map8 // library marker matterTools.ColorClusterMethods0x0300, line 121
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x0006, fields) // library marker matterTools.ColorClusterMethods0x0300, line 122
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))   // library marker matterTools.ColorClusterMethods0x0300, line 123

        if (inputs.level) setLevel(*:inputs) // library marker matterTools.ColorClusterMethods0x0300, line 125

    } catch (AssertionError e) { // library marker matterTools.ColorClusterMethods0x0300, line 127
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 128
    } catch(e){ // library marker matterTools.ColorClusterMethods0x0300, line 129
        log.error "<pre>${e}<br><br>when processing setColor with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 130
    }    // library marker matterTools.ColorClusterMethods0x0300, line 131
} // library marker matterTools.ColorClusterMethods0x0300, line 132

// Implements Matter Command MoveToColorTemperature (Command 0x0A), Matter 1.2 Spec, Section 3.2.11.14 // library marker matterTools.ColorClusterMethods0x0300, line 134
// Following functions are to be called from web UI and child component device. // library marker matterTools.ColorClusterMethods0x0300, line 135
void componentSetColorTemperature(cd, cdColortemperature, cdLevel = null, cdTransitionTime = null) {  // library marker matterTools.ColorClusterMethods0x0300, line 136
        setColorTemperature( ep:getEndpointIdInt(cd), colortemperature: cdColortemperature as Integer, level: cdLevel as Integer, transitionTime10ths: ((cdTransitionTime ?: 0) * 10) as Integer ) // library marker matterTools.ColorClusterMethods0x0300, line 137
        } // library marker matterTools.ColorClusterMethods0x0300, line 138
void setColorTemperature(colortemperature, level = null, transitionTime = null) {  // library marker matterTools.ColorClusterMethods0x0300, line 139
        setColorTemperature( ep:getEndpointIdInt(device), colortemperature: colortemperature as Integer, level: level as Integer, transitionTime10ths: ((transitionTime ?: 0) * 10) as Integer ) // library marker matterTools.ColorClusterMethods0x0300, line 140
        } // library marker matterTools.ColorClusterMethods0x0300, line 141
void setColorTemperature( Map params = [:] ){ // library marker matterTools.ColorClusterMethods0x0300, line 142
    try { // library marker matterTools.ColorClusterMethods0x0300, line 143
        Map inputs = [ep: null, colortemperature:null, transitionTime10ths: null, level:null] << params // library marker matterTools.ColorClusterMethods0x0300, line 144
        assert inputs.ep instanceof Integer // library marker matterTools.ColorClusterMethods0x0300, line 145
        // For color Temperature, 15.3 Kelvin is the minimum supported by Matter based on ColorTemperatureMireds accepted range 0xFEFF. MMatter Spec. Section 3.2.11.14. // library marker matterTools.ColorClusterMethods0x0300, line 146
        if (inputs.colortemperature instanceof BigDecimal) inputs.colortemperature = inputs.colortemperature as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 147
        assert (inputs.colortemperature instanceof Integer) && (inputs.colortemperature > 15)  // library marker matterTools.ColorClusterMethods0x0300, line 148
        if (inputs.level instanceof BigDecimal) inputs.level = inputs.level as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 149
        assert inputs.level instanceof Integer || inputs.level.is(null) // library marker matterTools.ColorClusterMethods0x0300, line 150
        if (inputs.transitionTime10ths instanceof BigDecimal) inputs.transitionTime10ths = inputs.transitionTime10ths as Integer // Web UI may send BigDecimal. Need Integer. // library marker matterTools.ColorClusterMethods0x0300, line 151
        assert inputs.transitionTime10ths instanceof Integer || inputs.transitionTime10ths.is(null)   // library marker matterTools.ColorClusterMethods0x0300, line 152
        // don't need to assertion check level - it will get tested in the setLevel method! // library marker matterTools.ColorClusterMethods0x0300, line 153

        Integer targetMireds = (1000000 / inputs.colortemperature) // Matter works in Mireds, Hubitat in Kelvin. Convert Hubitat input from Kelvin to Mireds // library marker matterTools.ColorClusterMethods0x0300, line 155

 	    String hexMireds =                 HexUtils.integerToHexString( targetMireds,  2 ) // // library marker matterTools.ColorClusterMethods0x0300, line 157
        String hexTransitionTime10ths =    HexUtils.integerToHexString( (inputs.transitionTime10ths ?: 0),  2 ) // If transitionTime10ths not stated, use 0 // library marker matterTools.ColorClusterMethods0x0300, line 158

        List<Map<String, String>> fields = [] // library marker matterTools.ColorClusterMethods0x0300, line 160
            fields.add(matter.cmdField(DataType.UINT16, 0, (hexMireds[2..3] + hexMireds[0..1]) )) // ColorTemperatureMireds // library marker matterTools.ColorClusterMethods0x0300, line 161
            fields.add(matter.cmdField(DataType.UINT16, 1, (hexTransitionTime10ths[2..3] + hexTransitionTime10ths[0..1]) )) // TransitionTime uint16 0-65534, byte swapped // library marker matterTools.ColorClusterMethods0x0300, line 162
            fields.add(matter.cmdField(DataType.UINT8,  2, "00")) // OptionMask, map8 // library marker matterTools.ColorClusterMethods0x0300, line 163
            fields.add(matter.cmdField(DataType.UINT8,  3, "00"))  // OptionsOverride, map8 // library marker matterTools.ColorClusterMethods0x0300, line 164
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x0A, fields) // Move To Color Temperature Command is 0x0A. Matter Spec. Section 3.2.11.14 // library marker matterTools.ColorClusterMethods0x0300, line 165
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.ColorClusterMethods0x0300, line 166

        if (inputs.level) setLevel(*:inputs)    // No need to set out each parameter. I used consistent naming, so can pass the orignal paraemters using a spread // library marker matterTools.ColorClusterMethods0x0300, line 168

    } catch (AssertionError e) { // library marker matterTools.ColorClusterMethods0x0300, line 170
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 171
    } catch(e){ // library marker matterTools.ColorClusterMethods0x0300, line 172
        log.error "<pre>${e}<br><br>when processing setColorTemperature with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 173
    } // library marker matterTools.ColorClusterMethods0x0300, line 174
}     // library marker matterTools.ColorClusterMethods0x0300, line 175

// Implements Matter Command ColorLoopSet command (Command 0x44), Matter 1.2 Spec, Section 3.2.11.19 // library marker matterTools.ColorClusterMethods0x0300, line 177

void setColorLoop( Map params = [:] ){ // library marker matterTools.ColorClusterMethods0x0300, line 179
    try { // library marker matterTools.ColorClusterMethods0x0300, line 180
        Map inputs = [ep:getEndpointIdInt(device), updateFlags: 0x0F, action:0x01, direction:0x01, time:30, startHue:0 ] << params // library marker matterTools.ColorClusterMethods0x0300, line 181
        assert inputs.ep instanceof Integer  // library marker matterTools.ColorClusterMethods0x0300, line 182


        String HexUpdateFlags =     HexUtils.integerToHexString(inputs.updateFlags, 1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 185
        String HexAction =          HexUtils.integerToHexString(inputs.action, 1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 186
        String HexDirection =       HexUtils.integerToHexString(inputs.direction, 1) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 187
        String HexTime =            HexUtils.integerToHexString(inputs.time, 2) // 2 Bytes // library marker matterTools.ColorClusterMethods0x0300, line 188
        String HexStartHue =        HexUtils.integerToHexString(inputs.startHue, 2) // 1 Byte // library marker matterTools.ColorClusterMethods0x0300, line 189

        List<Map<String, String>> fields = [] // library marker matterTools.ColorClusterMethods0x0300, line 191
            fields.add(matter.cmdField(DataType.UINT8,   0, HexUpdateFlags)) //  // library marker matterTools.ColorClusterMethods0x0300, line 192
            fields.add(matter.cmdField(DataType.UINT8,   1, HexAction)) // Direction 00 = Shortest // library marker matterTools.ColorClusterMethods0x0300, line 193
            fields.add(matter.cmdField(DataType.UINT8,   2, HexDirection))  // library marker matterTools.ColorClusterMethods0x0300, line 194
            fields.add(matter.cmdField(DataType.UINT16,  3, HexTime[2..3] + HexTime[0..1] )) // Time in seconds, byte swapped // library marker matterTools.ColorClusterMethods0x0300, line 195
            fields.add(matter.cmdField(DataType.UINT16,  4, HexStartHue[2..3] + HexStartHue[0..1] )) // Hue in 16 bits // library marker matterTools.ColorClusterMethods0x0300, line 196

            fields.add(matter.cmdField(DataType.UINT8,   5, "00")) // OptionMask, map8 // library marker matterTools.ColorClusterMethods0x0300, line 198
            fields.add(matter.cmdField(DataType.UINT8,   6, "00"))  // OptionsOverride, map8 // library marker matterTools.ColorClusterMethods0x0300, line 199
        String cmd = matter.invoke(inputs.ep, 0x0300, 0x44, fields) // Move To Hue Command is 0x00. Matter Spec. Section 3.2.11.4 // library marker matterTools.ColorClusterMethods0x0300, line 200
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))   // library marker matterTools.ColorClusterMethods0x0300, line 201


    } catch (AssertionError e) { // library marker matterTools.ColorClusterMethods0x0300, line 204
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 205
    } catch(e){ // library marker matterTools.ColorClusterMethods0x0300, line 206
        log.error "<pre>${e}<br><br>when processing setColorLoop with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.ColorClusterMethods0x0300, line 207
    }  // library marker matterTools.ColorClusterMethods0x0300, line 208
} // library marker matterTools.ColorClusterMethods0x0300, line 209

// ~~~~~ end include (1) matterTools.ColorClusterMethods0x0300 ~~~~~

// ~~~~~ start include (19) matterTools.modeSelectClusterMethods0x0050 ~~~~~
/*  // library marker matterTools.modeSelectClusterMethods0x0050, line 1
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.8 ("Mode Select Cluster") // library marker matterTools.modeSelectClusterMethods0x0050, line 2
*/ // library marker matterTools.modeSelectClusterMethods0x0050, line 3

library ( // library marker matterTools.modeSelectClusterMethods0x0050, line 5
        base: "driver", // library marker matterTools.modeSelectClusterMethods0x0050, line 6
        author: "jvm33", // library marker matterTools.modeSelectClusterMethods0x0050, line 7
        category: "matter", // library marker matterTools.modeSelectClusterMethods0x0050, line 8
        description: "mode Select Control Cluster 0x0050 Tools", // library marker matterTools.modeSelectClusterMethods0x0050, line 9
        name: "modeSelectClusterMethods0x0050", // library marker matterTools.modeSelectClusterMethods0x0050, line 10
        namespace: "matterTools", // library marker matterTools.modeSelectClusterMethods0x0050, line 11
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.modeSelectClusterMethods0x0050, line 12
		version: "0.0.1" // library marker matterTools.modeSelectClusterMethods0x0050, line 13
) // library marker matterTools.modeSelectClusterMethods0x0050, line 14
import hubitat.helper.HexUtils // library marker matterTools.modeSelectClusterMethods0x0050, line 15

// Following functions implement Matter Spec 1.8.7 "ChangeToMode" command. // library marker matterTools.modeSelectClusterMethods0x0050, line 17
void changeToMode( Map params = [:] ) { // library marker matterTools.modeSelectClusterMethods0x0050, line 18
    try {  // library marker matterTools.modeSelectClusterMethods0x0050, line 19
        Map inputs = [ep: null , mode: null] << params // library marker matterTools.modeSelectClusterMethods0x0050, line 20
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer // library marker matterTools.modeSelectClusterMethods0x0050, line 21
        assert inputs.mode instanceof Integer // library marker matterTools.modeSelectClusterMethods0x0050, line 22

        String hexMode = HexUtils.integerToHexString((Integer) inputs.mode, 1) // library marker matterTools.modeSelectClusterMethods0x0050, line 24
        List<Map<String, String>> fields = [] // library marker matterTools.modeSelectClusterMethods0x0050, line 25
        fields.add(matter.cmdField(DataType.UINT8, 0, hexMode)) // Mode // library marker matterTools.modeSelectClusterMethods0x0050, line 26
        String cmd = matter.invoke(inputs.ep, 0x0050, 0x00, fields) // ChangeToMode // library marker matterTools.modeSelectClusterMethods0x0050, line 27
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))      // library marker matterTools.modeSelectClusterMethods0x0050, line 28
    } catch (AssertionError e) { // library marker matterTools.modeSelectClusterMethods0x0050, line 29
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.modeSelectClusterMethods0x0050, line 30
    } catch(e){ // library marker matterTools.modeSelectClusterMethods0x0050, line 31
        log.error "<pre>${e}<br><br>when processing changeToMode with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.modeSelectClusterMethods0x0050, line 32
    } // library marker matterTools.modeSelectClusterMethods0x0050, line 33
} // library marker matterTools.modeSelectClusterMethods0x0050, line 34

Map getModeSelectOptions(Integer ep){ // library marker matterTools.modeSelectClusterMethods0x0050, line 36
    getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0002)?.collectEntries({[ (it[1]), (it[0]) ] })  // library marker matterTools.modeSelectClusterMethods0x0050, line 37
} // library marker matterTools.modeSelectClusterMethods0x0050, line 38

Integer getModeSelectCurrentMode(Integer ep){ // library marker matterTools.modeSelectClusterMethods0x0050, line 40
    return getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0003) // library marker matterTools.modeSelectClusterMethods0x0050, line 41
} // library marker matterTools.modeSelectClusterMethods0x0050, line 42

String getModeSelectLabel(Integer ep){ // library marker matterTools.modeSelectClusterMethods0x0050, line 44
    "<b>${getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0000) ?: "Initialize Device then Refresh Browser"}</b>" // library marker matterTools.modeSelectClusterMethods0x0050, line 45
} // library marker matterTools.modeSelectClusterMethods0x0050, line 46

void handleModeSelectClusterUpdate(decodedDescriptionMap){ // library marker matterTools.modeSelectClusterMethods0x0050, line 48
    switch(decodedDescriptionMap.attrInt){ // library marker matterTools.modeSelectClusterMethods0x0050, line 49
        case 0x0003: // Current Mode // library marker matterTools.modeSelectClusterMethods0x0050, line 50
            String settingsName = "ModeSelect_${decodedDescriptionMap.endpointInt}" // library marker matterTools.modeSelectClusterMethods0x0050, line 51
            String newValue = "${decodedDescriptionMap.decodedValue}" // Hubitat oddity - all device.settings keys are strings, so newValue must be a number in string format, even though the index to menuItems was originally an Intger number // library marker matterTools.modeSelectClusterMethods0x0050, line 52
            device.updateSetting(settingsName, [value:newValue, type:"enum"]) // library marker matterTools.modeSelectClusterMethods0x0050, line 53
            break // library marker matterTools.modeSelectClusterMethods0x0050, line 54
        } // library marker matterTools.modeSelectClusterMethods0x0050, line 55
} // library marker matterTools.modeSelectClusterMethods0x0050, line 56

// ~~~~~ end include (19) matterTools.modeSelectClusterMethods0x0050 ~~~~~

// ~~~~~ start include (20) inovelliTools.createListOfMatterSendEventMaps ~~~~~
/* // library marker inovelliTools.createListOfMatterSendEventMaps, line 1
This has been pared down from matterTools.createListOfMatterSendEventMaps.  The original would also work! // library marker inovelliTools.createListOfMatterSendEventMaps, line 2
*/ // library marker inovelliTools.createListOfMatterSendEventMaps, line 3
library ( // library marker inovelliTools.createListOfMatterSendEventMaps, line 4
        base: "driver", // library marker inovelliTools.createListOfMatterSendEventMaps, line 5
        author: "jvm33", // library marker inovelliTools.createListOfMatterSendEventMaps, line 6
        category: "matter", // library marker inovelliTools.createListOfMatterSendEventMaps, line 7
        description: "Create Hubitat Events from Matter Attribute Data for Inovelli VTM31-SN", // library marker inovelliTools.createListOfMatterSendEventMaps, line 8
        name: "createListOfMatterSendEventMaps", // library marker inovelliTools.createListOfMatterSendEventMaps, line 9
        namespace: "inovelliTools", // library marker inovelliTools.createListOfMatterSendEventMaps, line 10
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker inovelliTools.createListOfMatterSendEventMaps, line 11
		version: "0.0.1" // library marker inovelliTools.createListOfMatterSendEventMaps, line 12
) // library marker inovelliTools.createListOfMatterSendEventMaps, line 13
import java.lang.Math // library marker inovelliTools.createListOfMatterSendEventMaps, line 14
import groovy.transform.Field // library marker inovelliTools.createListOfMatterSendEventMaps, line 15
import groovy.json.JsonBuilder // library marker inovelliTools.createListOfMatterSendEventMaps, line 16
import org.apache.commons.lang3.StringUtils // library marker inovelliTools.createListOfMatterSendEventMaps, line 17
// ////////////////////// // library marker inovelliTools.createListOfMatterSendEventMaps, line 18

@Field static Closure toTenths = { it / 10}      // Hex to .1 conversion. // library marker inovelliTools.createListOfMatterSendEventMaps, line 20
@Field static Closure toCenti =  { it / 100}     // Hex to .01 conversion. // library marker inovelliTools.createListOfMatterSendEventMaps, line 21
@Field static Closure toMilli =  { it / 1000}    // Hex to .001 conversion. // library marker inovelliTools.createListOfMatterSendEventMaps, line 22
@Field static Closure HexToPercent = { it ? Math.max( Math.round(it / 2.54) , 1) : 0 } // the Math.max check ensures that a value of 1/2.54 does not get changes to 0 // library marker inovelliTools.createListOfMatterSendEventMaps, line 23
@Field static Closure HexToLux =          { Math.pow( 10, (it - 1) / 10000)  as Integer} // convert Matter value to illumination in lx. See Matter Cluster Spec Section 2.2.5.1 // library marker inovelliTools.createListOfMatterSendEventMaps, line 24
@Field static Closure MiredsToKelvin = { ( (it > 0) ? (1000000 / it) : null ) as Integer} // library marker inovelliTools.createListOfMatterSendEventMaps, line 25

/* // library marker inovelliTools.createListOfMatterSendEventMaps, line 27
For Closure values in the following structure: // library marker inovelliTools.createListOfMatterSendEventMaps, line 28
pv = parsed Map value field (descMap.value) // library marker inovelliTools.createListOfMatterSendEventMaps, line 29
dn = device name - provided as a string // library marker inovelliTools.createListOfMatterSendEventMaps, line 30
dv = device value - usually the content of the event map's "value" field after pv has been converted by the closure in the "value" field, below.. // library marker inovelliTools.createListOfMatterSendEventMaps, line 31
*/ // library marker inovelliTools.createListOfMatterSendEventMaps, line 32
@Field static Map globalAllEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events // library marker inovelliTools.createListOfMatterSendEventMaps, line 33
    0x0003:[ // Identify Cluster // library marker inovelliTools.createListOfMatterSendEventMaps, line 34
        0x0000:[[attribute:"IdentifyTime", units:"seconds"				]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 35
        0x0001:[[attribute:"IdentifyType", valueTransform: { IdentifyTypeEnum.get(it)  }			]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 36
    ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 37
    0x0006:[ // Switch Cluster // library marker inovelliTools.createListOfMatterSendEventMaps, line 38
        0x0000:[[attribute:"switch",             valueTransform: { it ? "on" : "off" }     		]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 39
    ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 40
    0x0008:[ // Level Cluster // library marker inovelliTools.createListOfMatterSendEventMaps, line 41
        0x0000:[[attribute:"level",                valueTransform: this.&HexToPercent, 	    units:"%"       	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 42
        0x0001:[[attribute:"RemainingTime",        valueTransform: this.&toTenths,    		units:"seconds" 	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 43
        0x0010:[[attribute:"OnOffTransitionTime",  valueTransform: this.&toTenths,    		units:"seconds" 	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 44
        0x0011:[[attribute:"OnLevel",              valueTransform: this.&HexToPercent,  	units:"%"       	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 45
        0x0012:[[attribute:"OnTransitionTime",     valueTransform: this.&toTenths,    		units:"seconds" 	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 46
        0x0013:[[attribute:"OffTransitionTime",    valueTransform: this.&toTenths,    		units:"seconds" 	]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 47
        0x0014:[[attribute:"DefaultMoveRate",    						                                        ]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 48
    ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 49
    0x003B:[ // Generic Switch Cluster // library marker inovelliTools.createListOfMatterSendEventMaps, line 50
        0x0002:[[attribute:"MultiPressMax",        		]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 51
        ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 52
    0x0050: [ // Mode Select Cluster // library marker inovelliTools.createListOfMatterSendEventMaps, line 53
        0x0000:[[attribute:"Description"          ]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 54
        0x0002:[[attribute:"SupportedModes"       ]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 55
        0x0003:[[attribute:"CurrentMode"         ]], // library marker inovelliTools.createListOfMatterSendEventMaps, line 56
        ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 57
    0x0300:[ // Color Control Cluster.  Only covering the most common ones for Hue at the moment! // library marker inovelliTools.createListOfMatterSendEventMaps, line 58
		0x0000:[ // Hue // library marker inovelliTools.createListOfMatterSendEventMaps, line 59
			    [attribute:"hue",  valueTransform: this.&HexToPercent, units:"%" 	], 	//  This is the Hubitat name/value // library marker inovelliTools.createListOfMatterSendEventMaps, line 60
               ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 61
        0x0001:[[attribute:"saturation", valueTransform: this.&HexToPercent, units:"%"  ],  	//  This is the Hubitat name/value // library marker inovelliTools.createListOfMatterSendEventMaps, line 62
               ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 63
        ], // library marker inovelliTools.createListOfMatterSendEventMaps, line 64
] // library marker inovelliTools.createListOfMatterSendEventMaps, line 65

List getHubitatEvents(Map descMap) { // library marker inovelliTools.createListOfMatterSendEventMaps, line 67
    try { // library marker inovelliTools.createListOfMatterSendEventMaps, line 68
        Integer retrieveThisCluster = descMap.clusterInt // library marker inovelliTools.createListOfMatterSendEventMaps, line 69

        List rEvents = globalAllEventsMap.get(retrieveThisCluster) // library marker inovelliTools.createListOfMatterSendEventMaps, line 71
		    ?.get(descMap.attrInt) // library marker inovelliTools.createListOfMatterSendEventMaps, line 72
			    ?.collect{ Map rValue = [:] // library marker inovelliTools.createListOfMatterSendEventMaps, line 73
                        rValue << [name:it.attribute] // First copy the attribute string as the name of the event // library marker inovelliTools.createListOfMatterSendEventMaps, line 74

                        // Now figure out the value for the event using the valueTransform, but first check for null so you don't throw an error applying the transform!   // library marker inovelliTools.createListOfMatterSendEventMaps, line 76
                         if (descMap.decodedValue.is(null)) { // library marker inovelliTools.createListOfMatterSendEventMaps, line 77
                              rValue <<[value:null] // library marker inovelliTools.createListOfMatterSendEventMaps, line 78
                         } else if ((it.containsKey("valueTransform")) && (it.valueTransform instanceof Closure)) { // library marker inovelliTools.createListOfMatterSendEventMaps, line 79
                              rValue << [value:(it.valueTransform(descMap.decodedValue))] // if valueTransform is a closure, apply the transform Closure to the data received from the node // library marker inovelliTools.createListOfMatterSendEventMaps, line 80
                         } else { // library marker inovelliTools.createListOfMatterSendEventMaps, line 81
                              rValue << [value: (descMap.decodedValue)]  // else just copy the decoded value // library marker inovelliTools.createListOfMatterSendEventMaps, line 82
                        } // library marker inovelliTools.createListOfMatterSendEventMaps, line 83

						rValue << ( it.units ? [units:(it.units)]  : [:] ) // library marker inovelliTools.createListOfMatterSendEventMaps, line 85

						// Now let's form a descriptionText string // library marker inovelliTools.createListOfMatterSendEventMaps, line 87
                        // If you have a descriptionText field and it is a closure, then form the description text using // library marker inovelliTools.createListOfMatterSendEventMaps, line 88
                        // that Closure supplied with the event's value (the value then can be used in the description) // library marker inovelliTools.createListOfMatterSendEventMaps, line 89
                        // Else, for a description string using the attribute name and add the value // library marker inovelliTools.createListOfMatterSendEventMaps, line 90
                          String newDescription // library marker inovelliTools.createListOfMatterSendEventMaps, line 91
                          if (it.descriptionText && (it.descriptionText instanceof Closure)) { // library marker inovelliTools.createListOfMatterSendEventMaps, line 92
                              newDescription = it.descriptionText(rValue.value) // library marker inovelliTools.createListOfMatterSendEventMaps, line 93
                          } else { // library marker inovelliTools.createListOfMatterSendEventMaps, line 94
                                newDescription = "${StringUtils.splitByCharacterTypeCamelCase(rValue.name).join(" ")} attribute set to ${rValue.value}" // library marker inovelliTools.createListOfMatterSendEventMaps, line 95
                                if (it.units) { newDescription = newDescription + " " + it.units } // library marker inovelliTools.createListOfMatterSendEventMaps, line 96
                          } // library marker inovelliTools.createListOfMatterSendEventMaps, line 97
                        rValue << ( [descriptionText:newDescription]) // library marker inovelliTools.createListOfMatterSendEventMaps, line 98
						rValue << ( it.isStateChange ? [isStateChange:true]  : [:] ) // Was an isStateChange clause stated, if so, copy it if it is true. False is implied. // library marker inovelliTools.createListOfMatterSendEventMaps, line 99
                        rValue << ( [clusterInt : (descMap.clusterInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device // library marker inovelliTools.createListOfMatterSendEventMaps, line 100
                        rValue << ( [attrInt : (descMap.attrInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra attribute info for external device // library marker inovelliTools.createListOfMatterSendEventMaps, line 101
                        rValue << ( [endpointInt : (descMap.endpointInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device // library marker inovelliTools.createListOfMatterSendEventMaps, line 102
                        rValue << ( [jsonValue: (new JsonBuilder(descMap.decodedValue)) ]) // Event is sent on Hubitat's Event stream to external devices, so let's include original data in JSON form for external device // library marker inovelliTools.createListOfMatterSendEventMaps, line 103
					} // library marker inovelliTools.createListOfMatterSendEventMaps, line 104
        return rEvents // library marker inovelliTools.createListOfMatterSendEventMaps, line 105
    } catch (AssertionError e) { // library marker inovelliTools.createListOfMatterSendEventMaps, line 106
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker inovelliTools.createListOfMatterSendEventMaps, line 107
    } catch(e){ // library marker inovelliTools.createListOfMatterSendEventMaps, line 108
        log.error "<pre>${e}<br><br>when processing getHubitatEvents inputs ${descMap}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker inovelliTools.createListOfMatterSendEventMaps, line 109
    }      // library marker inovelliTools.createListOfMatterSendEventMaps, line 110
} // library marker inovelliTools.createListOfMatterSendEventMaps, line 111


// ~~~~~ end include (20) inovelliTools.createListOfMatterSendEventMaps ~~~~~

// ~~~~~ start include (7) matterTools.matterHelperUtilities ~~~~~
library ( // library marker matterTools.matterHelperUtilities, line 1
        base: "driver", // library marker matterTools.matterHelperUtilities, line 2
        author: "jvm33", // library marker matterTools.matterHelperUtilities, line 3
        category: "matter", // library marker matterTools.matterHelperUtilities, line 4
        description: "Formats Matter Commands", // library marker matterTools.matterHelperUtilities, line 5
        name: "matterHelperUtilities", // library marker matterTools.matterHelperUtilities, line 6
        namespace: "matterTools", // library marker matterTools.matterHelperUtilities, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.matterHelperUtilities, line 8
		version: "0.0.1" // library marker matterTools.matterHelperUtilities, line 9
) // library marker matterTools.matterHelperUtilities, line 10
import groovy.transform.Field // library marker matterTools.matterHelperUtilities, line 11
import  hubitat.matter.DataType // library marker matterTools.matterHelperUtilities, line 12

// Matter payloads need hex parameters of greater than 2 characters to be pair-reversed. // library marker matterTools.matterHelperUtilities, line 14
// This function takes a list of parameters and pair-reverses those longer than 2 characters. // library marker matterTools.matterHelperUtilities, line 15
// Alternatively, it can take a string and pair-revers that. // library marker matterTools.matterHelperUtilities, line 16
// Thus, e.g., ["0123", "456789", "10"] becomes "230189674510" and "123456" becomes "563412" // library marker matterTools.matterHelperUtilities, line 17
private String byteReverseParameters(String oneString) { byteReverseParameters([] << oneString) } // library marker matterTools.matterHelperUtilities, line 18
private String byteReverseParameters(List<String> parameters) { // library marker matterTools.matterHelperUtilities, line 19
	StringBuilder rStr = new StringBuilder(64) // library marker matterTools.matterHelperUtilities, line 20
	for (hexString in parameters) { // library marker matterTools.matterHelperUtilities, line 21
		if (hexString.length() % 2) throw new Exception("In method byteReverseParameters, trying to reverse a hex string that is not an even number of characters in length. Error in Hex String: ${hexString}, All method parameters were ${parameters}.") // library marker matterTools.matterHelperUtilities, line 22

		for(Integer i = hexString.length() -1 ; i > 0 ; i -= 2) { // library marker matterTools.matterHelperUtilities, line 24
			rStr << hexString[i-1..i] // library marker matterTools.matterHelperUtilities, line 25
		}	 // library marker matterTools.matterHelperUtilities, line 26
	} // library marker matterTools.matterHelperUtilities, line 27
	return rStr // library marker matterTools.matterHelperUtilities, line 28
} // library marker matterTools.matterHelperUtilities, line 29

// Performs a refresh on a designated endpoint / cluster / attribute (all specified in Integer) // library marker matterTools.matterHelperUtilities, line 31
// Does a wildcard refresh if parameters are not specified (ep=FFFF / cluster=FFFFFFFF/ endpoint=FFFFFFFF is the Matter wildcard designation // library marker matterTools.matterHelperUtilities, line 32
void refreshMatter(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 33
    try { // library marker matterTools.matterHelperUtilities, line 34
        Map inputs = [ep:0xFFFF, clusterInt: 0xFFFFFFFF, attrInt: 0xFFFFFFFF] << params // library marker matterTools.matterHelperUtilities, line 35
        assert inputs.ep instanceof Integer         // Make sure the type is as expected!  // library marker matterTools.matterHelperUtilities, line 36
        assert inputs.clusterInt instanceof Integer || inputs.clusterInt instanceof Long // library marker matterTools.matterHelperUtilities, line 37
        assert inputs.attrInt instanceof Integer || inputs.attrInt instanceof Long // library marker matterTools.matterHelperUtilities, line 38

       // Groovy Slashy String form of a GString  https://docs.groovy-lang.org/latest/html/documentation/#_slashy_string // library marker matterTools.matterHelperUtilities, line 40
        String cmd = /he rattrs [{"ep":"${inputs.ep}","cluster":"${inputs.clusterInt}","attr":"${inputs.attrInt}"}]/ // library marker matterTools.matterHelperUtilities, line 41

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 43
    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 44
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 45
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 46
        log.error "<pre>${e}<br><br>when processing refreshMatter with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 47
    }    // library marker matterTools.matterHelperUtilities, line 48
} // library marker matterTools.matterHelperUtilities, line 49


void writeClusterAttribute(clusterId, attributeId, hexValue, dataType) {  // library marker matterTools.matterHelperUtilities, line 52
    writeClusterAttribute( // library marker matterTools.matterHelperUtilities, line 53
            ep: null, // library marker matterTools.matterHelperUtilities, line 54
            clusterInt: Integer.parseInt( clusterId, 16), // library marker matterTools.matterHelperUtilities, line 55
            attributeInt: Integer.parseInt( attributeId, 16),  // library marker matterTools.matterHelperUtilities, line 56
            hexValue:hexValue,  // library marker matterTools.matterHelperUtilities, line 57
            hubitatDataType: dataType  // library marker matterTools.matterHelperUtilities, line 58
    )  // library marker matterTools.matterHelperUtilities, line 59
} // library marker matterTools.matterHelperUtilities, line 60
void writeClusterAttribute(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 61
    try { // library marker matterTools.matterHelperUtilities, line 62
	    Map inputs = [ep: null, clusterInt: null , attributeInt: null , hexValue: null] << params // library marker matterTools.matterHelperUtilities, line 63
        assert inputs.ep instanceof Integer // library marker matterTools.matterHelperUtilities, line 64
        assert inputs.clusterInt instanceof Integer // library marker matterTools.matterHelperUtilities, line 65
        assert inputs.attributeInt instanceof Integer // library marker matterTools.matterHelperUtilities, line 66

        List<Map<String, String>> attrWriteRequests = [] // library marker matterTools.matterHelperUtilities, line 68
            attrWriteRequests.add(matter.attributeWriteRequest(inputs.ep, inputs.clusterInt, inputs.attributeInt, getHubitatDataType(*:inputs), inputs.hexValue)) // library marker matterTools.matterHelperUtilities, line 69
        String cmd = matter.writeAttributes(attrWriteRequests) // library marker matterTools.matterHelperUtilities, line 70

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 72
    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 73
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 74
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 75
        log.error "<pre>${e}<br><br>when processing writeClusterAttribute with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 76
    }    // library marker matterTools.matterHelperUtilities, line 77
} // library marker matterTools.matterHelperUtilities, line 78

void readClusterAttribute(clusterId, attributeId) {readClusterAttribute(clusterId:clusterId, attributeId:attributeId)} // library marker matterTools.matterHelperUtilities, line 80
void readClusterAttribute(Map params = [:]) { // library marker matterTools.matterHelperUtilities, line 81
    try { // library marker matterTools.matterHelperUtilities, line 82
	    Map inputs = [ep:null, clusterInt:null, attributeInt:null ] << params // library marker matterTools.matterHelperUtilities, line 83
        assert inputs.ep instanceof Integer // library marker matterTools.matterHelperUtilities, line 84
        assert inputs.clusterInt instanceof Integer || instance.clusterInt instanceof Long // library marker matterTools.matterHelperUtilities, line 85
        assert inputs.attributeInt instanceof Integer || instance.attributeInt instanceof Long // library marker matterTools.matterHelperUtilities, line 86

        List<Map<String, String>> attributePaths = [] // library marker matterTools.matterHelperUtilities, line 88
        attributePaths.add(matter.attributePath(inputs.ep, inputs.clusterInt, inputs.attributeInt)) // library marker matterTools.matterHelperUtilities, line 89

        String cmd = matter.readAttributes(attributePaths) // library marker matterTools.matterHelperUtilities, line 91

        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER)) // library marker matterTools.matterHelperUtilities, line 93

    } catch (AssertionError e) { // library marker matterTools.matterHelperUtilities, line 95
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 96
    } catch(e){ // library marker matterTools.matterHelperUtilities, line 97
        log.error "<pre>${e}<br><br>when processing readClusterAttribute with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.matterHelperUtilities, line 98
    }    // library marker matterTools.matterHelperUtilities, line 99
} // library marker matterTools.matterHelperUtilities, line 100

// ~~~~~ end include (7) matterTools.matterHelperUtilities ~~~~~

// ~~~~~ start include (10) matterTools.concurrentRuntimeDataStorage ~~~~~
/*  // library marker matterTools.concurrentRuntimeDataStorage, line 1
Library to store runtime generate data in a concurrency-safe manner. // library marker matterTools.concurrentRuntimeDataStorage, line 2
This is primarily used for storing cluster and attribute data. // library marker matterTools.concurrentRuntimeDataStorage, line 3
A concurrentHashMap is used to avoid conflicting writes where multiple attributes may be reported from a node and get processed simultaneously // library marker matterTools.concurrentRuntimeDataStorage, line 4
In newer Hubitat drivers, could also use atomicState writes, but this can be faster // library marker matterTools.concurrentRuntimeDataStorage, line 5

Library assumes that descMap also includes the endpointId as an integer (descMap.endpointIdInt).  // library marker matterTools.concurrentRuntimeDataStorage, line 7
This isn't part of the standard "descMap" parsing, but descMap can be augmented immediately after the parseDescriptionAsMap using // library marker matterTools.concurrentRuntimeDataStorage, line 8
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.concurrentRuntimeDataStorage, line 9
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.concurrentRuntimeDataStorage, line 10
See matterTools.commonDriverMethods library for example // library marker matterTools.concurrentRuntimeDataStorage, line 11
*/ // library marker matterTools.concurrentRuntimeDataStorage, line 12

library ( // library marker matterTools.concurrentRuntimeDataStorage, line 14
        base: "driver", // library marker matterTools.concurrentRuntimeDataStorage, line 15
        author: "jvm33", // library marker matterTools.concurrentRuntimeDataStorage, line 16
        category: "matter", // library marker matterTools.concurrentRuntimeDataStorage, line 17
        description: "Methods Common to Matter Drivers", // library marker matterTools.concurrentRuntimeDataStorage, line 18
        name: "concurrentRuntimeDataStorage", // library marker matterTools.concurrentRuntimeDataStorage, line 19
        namespace: "matterTools", // library marker matterTools.concurrentRuntimeDataStorage, line 20
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.concurrentRuntimeDataStorage, line 21
		version: "0.0.0" // library marker matterTools.concurrentRuntimeDataStorage, line 22
) // library marker matterTools.concurrentRuntimeDataStorage, line 23
import java.util.concurrent.*  // library marker matterTools.concurrentRuntimeDataStorage, line 24
import groovy.transform.Field // library marker matterTools.concurrentRuntimeDataStorage, line 25
import groovy.json.JsonBuilder // library marker matterTools.concurrentRuntimeDataStorage, line 26

/* // library marker matterTools.concurrentRuntimeDataStorage, line 28
The globalDataStorage variable is the "root' of the data structure used in this library // library marker matterTools.concurrentRuntimeDataStorage, line 29
Since this is an @Field static variable (shared by all devices that use the driver), the first level of 'get' into this // library marker matterTools.concurrentRuntimeDataStorage, line 30
variable will be a device's unique network ID. The next level is where the current "device" really starts and begins with a map of one or more endpoint IDs as keys. // library marker matterTools.concurrentRuntimeDataStorage, line 31
After tha, for each endpoint, there is another map keyed by cluster IDs and for each cluster ID, a map keyed by attribute and then the value for the attribute. // library marker matterTools.concurrentRuntimeDataStorage, line 32

The resultant structure is a Map as follows // library marker matterTools.concurrentRuntimeDataStorage, line 34
globalDataStorage = [netID#1:[EndpointID#0:[ClusterID#1:[AttributeID#1:value, AttributeID#2:value, etc.], ClusterID#2:[... attribute ID structure ...]], EndpointID#1[repeat sub-structure as per #0]: // library marker matterTools.concurrentRuntimeDataStorage, line 35

globalDataStorage =  // library marker matterTools.concurrentRuntimeDataStorage, line 37
    [  netID#1: // library marker matterTools.concurrentRuntimeDataStorage, line 38
            EndpointID#0:[ // library marker matterTools.concurrentRuntimeDataStorage, line 39
                clusterID#1:[attributeID#1:value, AttributeID#2:value, etc], // library marker matterTools.concurrentRuntimeDataStorage, line 40
                clusterID#2:[attributeID#1:value, AttributeID#2:value, etc],                // library marker matterTools.concurrentRuntimeDataStorage, line 41
                (repeat Cluster ID structure as needed for other clusters) // library marker matterTools.concurrentRuntimeDataStorage, line 42
                        ], // library marker matterTools.concurrentRuntimeDataStorage, line 43
            EndpointID#1:[ // library marker matterTools.concurrentRuntimeDataStorage, line 44
                clusterID#1:[attributeID#1:value, AttributeID#2:value, etc], // library marker matterTools.concurrentRuntimeDataStorage, line 45
                clusterID#2:[attributeID#1:value, AttributeID#2:value, etc],                // library marker matterTools.concurrentRuntimeDataStorage, line 46
                (repeat Cluster ID structure as needed for other clusters) // library marker matterTools.concurrentRuntimeDataStorage, line 47
                        ], // library marker matterTools.concurrentRuntimeDataStorage, line 48
            (and repeat Endpoint ID structure for other endpoints as needed) // library marker matterTools.concurrentRuntimeDataStorage, line 49
    ], // library marker matterTools.concurrentRuntimeDataStorage, line 50
      netID #2, 3, 4, etc.: // library marker matterTools.concurrentRuntimeDataStorage, line 51
            (repeat as per above) // library marker matterTools.concurrentRuntimeDataStorage, line 52
    ] // library marker matterTools.concurrentRuntimeDataStorage, line 53

But you don't actually create this structure, its done by the storeRetrievedData call, below! // library marker matterTools.concurrentRuntimeDataStorage, line 55
*/ // library marker matterTools.concurrentRuntimeDataStorage, line 56
@Field static ConcurrentHashMap globalDataStorage = new ConcurrentHashMap(16, 0.75, 1) // library marker matterTools.concurrentRuntimeDataStorage, line 57

/* Following function is placed  after the driver parse routine's parseDescriptionAsMap to store most receent retrieved attributes so you can use when needed. // library marker matterTools.concurrentRuntimeDataStorage, line 59
Typical usage: // library marker matterTools.concurrentRuntimeDataStorage, line 60
        descMap = matter.parseDescriptionAsMap(description) // library marker matterTools.concurrentRuntimeDataStorage, line 61
        descMap.put("endpointInt", (Integer.parseInt(descMap.endpoint, 16))) // library marker matterTools.concurrentRuntimeDataStorage, line 62
        storeRetrievedData(descMap) // library marker matterTools.concurrentRuntimeDataStorage, line 63
*/ // library marker matterTools.concurrentRuntimeDataStorage, line 64

void storeRetrievedData(Map descMap){ // library marker matterTools.concurrentRuntimeDataStorage, line 66
    def valueToStore // library marker matterTools.concurrentRuntimeDataStorage, line 67
    if (descMap.containsKey("decodedValue")) { // This is for use with my custom parser that produced fully decoded values // library marker matterTools.concurrentRuntimeDataStorage, line 68
        valueToStore = descMap.decodedValue // library marker matterTools.concurrentRuntimeDataStorage, line 69
    } else if (descMap.containsKey("value")) { // library marker matterTools.concurrentRuntimeDataStorage, line 70
        valueToStore = descMap.value // library marker matterTools.concurrentRuntimeDataStorage, line 71
    }  // library marker matterTools.concurrentRuntimeDataStorage, line 72
    if (valueToStore.is(null)) return // Java ConcurrentHashMaps can't store null values! // library marker matterTools.concurrentRuntimeDataStorage, line 73
    globalDataStorage.get(device.getDeviceNetworkId(), new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this Network ID or create a blank of one doesn't exist // library marker matterTools.concurrentRuntimeDataStorage, line 74
        .get(descMap.endpointInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this Endpoint or create a blank of one doesn't exist // library marker matterTools.concurrentRuntimeDataStorage, line 75
            .get(descMap.clusterInt, new ConcurrentHashMap<String,ConcurrentHashMap>(8, 0.75, 1)) // Get Map for this cluster or create a blank of one doesn't exist // library marker matterTools.concurrentRuntimeDataStorage, line 76
                .put(descMap.attrInt, valueToStore) // And then put the attribute value into the map for this attribute / cluster / endpoint / network ID // library marker matterTools.concurrentRuntimeDataStorage, line 77
} // library marker matterTools.concurrentRuntimeDataStorage, line 78

// Following function retrieves the last-stored data for a particular attribute. // library marker matterTools.concurrentRuntimeDataStorage, line 80
def getStoredAttributeData(Map params = [:] ){ // library marker matterTools.concurrentRuntimeDataStorage, line 81
    try {  // library marker matterTools.concurrentRuntimeDataStorage, line 82
        Map inputs = [endpointInt:null, ep:null, clusterInt:null, attrInt:null]  << params // library marker matterTools.concurrentRuntimeDataStorage, line 83
        assert ( inputs.ep.is(null) || inputs.endpointInt.is(null)) // library marker matterTools.concurrentRuntimeDataStorage, line 84

        if ( (!inputs.ep.is(null)) && (inputs.ep instanceof Integer) ) inputs.endpointInt = inputs.ep // if ep label was used, copy ep to endpointInt!  // library marker matterTools.concurrentRuntimeDataStorage, line 86
        assert inputs.endpointInt instanceof Integer // library marker matterTools.concurrentRuntimeDataStorage, line 87
        assert inputs.clusterInt instanceof Integer // library marker matterTools.concurrentRuntimeDataStorage, line 88
        assert inputs.attrInt instanceof Integer // library marker matterTools.concurrentRuntimeDataStorage, line 89


        if (device.is(null)) return // can be Null if called from Metadata // library marker matterTools.concurrentRuntimeDataStorage, line 92
        return globalDataStorage.get(device.getDeviceNetworkId()) // First, get the data sub-Map for this specific node using deviceNetworkId // library marker matterTools.concurrentRuntimeDataStorage, line 93
            ?.get(inputs.endpointInt) // library marker matterTools.concurrentRuntimeDataStorage, line 94
                ?.get(inputs.clusterInt) // library marker matterTools.concurrentRuntimeDataStorage, line 95
                    ?.get(inputs.attrInt)        // library marker matterTools.concurrentRuntimeDataStorage, line 96

    } catch (AssertionError e) { // library marker matterTools.concurrentRuntimeDataStorage, line 98
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.concurrentRuntimeDataStorage, line 99
    } catch(e){ // library marker matterTools.concurrentRuntimeDataStorage, line 100
        log.error "<pre>${e}<br><br>when processing getStoredAttributeData with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }" // library marker matterTools.concurrentRuntimeDataStorage, line 101
    }    // library marker matterTools.concurrentRuntimeDataStorage, line 102
} // library marker matterTools.concurrentRuntimeDataStorage, line 103

Map getStoredDeviceData() { return  globalDataStorage.get(device.getDeviceNetworkId()) } // library marker matterTools.concurrentRuntimeDataStorage, line 105

void clearStoredDeviceData() { globalDataStorage.get(device.getDeviceNetworkId())?.clear() } // library marker matterTools.concurrentRuntimeDataStorage, line 107


// Returns the device data as JSON. See JsonBuilder class: https://docs.groovy-lang.org/latest/html/gapi/groovy/json/JsonBuilder.html // library marker matterTools.concurrentRuntimeDataStorage, line 110
JsonBuilder getStoredDeviceDataAsJSON(){ return new JsonBuilder( getStoredDeviceData() ) } // library marker matterTools.concurrentRuntimeDataStorage, line 111

/* // library marker matterTools.concurrentRuntimeDataStorage, line 113
A utility function to pretty-print all stored data in json form. Useful for debugging // library marker matterTools.concurrentRuntimeDataStorage, line 114
Endpoint, Cluster, and Attribute Map Keys are in Integer format, so, for example,  // library marker matterTools.concurrentRuntimeDataStorage, line 115
a "10" in as map key means ten, not sixteen. // library marker matterTools.concurrentRuntimeDataStorage, line 116

The values for the attributes themselves, however, are as-received from the Hubitat parse routine. // library marker matterTools.concurrentRuntimeDataStorage, line 118
So "10" in the attribute's value field may be hexidecimal meaning sixteen if Hubitat parsed it into hex form,  // library marker matterTools.concurrentRuntimeDataStorage, line 119
or likewise, could be an Integer meaning ten if Hubitat supplied it as an Integer.  // library marker matterTools.concurrentRuntimeDataStorage, line 120
This is a case where you need to know what Hubitat has done! // library marker matterTools.concurrentRuntimeDataStorage, line 121
*/ // library marker matterTools.concurrentRuntimeDataStorage, line 122
void prettyPrintStoredAttributeData(){ // library marker matterTools.concurrentRuntimeDataStorage, line 123
    log.info "<br><pre>${ getStoredDeviceDataAsJSON()?.toPrettyString() }" // library marker matterTools.concurrentRuntimeDataStorage, line 124
} // library marker matterTools.concurrentRuntimeDataStorage, line 125

// ~~~~~ end include (10) matterTools.concurrentRuntimeDataStorage ~~~~~

// ~~~~~ start include (18) matterTools.parseDescriptionAsDecodedMap ~~~~~
library ( // library marker matterTools.parseDescriptionAsDecodedMap, line 1
        base: "driver", // library marker matterTools.parseDescriptionAsDecodedMap, line 2
        author: "jvm33", // library marker matterTools.parseDescriptionAsDecodedMap, line 3
        category: "matter", // library marker matterTools.parseDescriptionAsDecodedMap, line 4
        description: "Methods Common to Matter Drivers", // library marker matterTools.parseDescriptionAsDecodedMap, line 5
        name: "parseDescriptionAsDecodedMap", // library marker matterTools.parseDescriptionAsDecodedMap, line 6
        namespace: "matterTools", // library marker matterTools.parseDescriptionAsDecodedMap, line 7
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter", // library marker matterTools.parseDescriptionAsDecodedMap, line 8
		version: "0.0.1" // library marker matterTools.parseDescriptionAsDecodedMap, line 9
) // library marker matterTools.parseDescriptionAsDecodedMap, line 10

// Per Matter Spec Appendix A.6, values greater than 0b11000 are reserved, except for 0b00011000 which is End-of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 12
Boolean isReservedValue(Integer controlOctet){  // library marker matterTools.parseDescriptionAsDecodedMap, line 13
    return  ( ((controlOctet & 0b00011111) >= (0b11000)) && !(controlOctet == 0b00011111)) // library marker matterTools.parseDescriptionAsDecodedMap, line 14
} // library marker matterTools.parseDescriptionAsDecodedMap, line 15

String HexToString(String hexStr){ // library marker matterTools.parseDescriptionAsDecodedMap, line 17
    ByteArrayOutputStream baos = new ByteArrayOutputStream(); // library marker matterTools.parseDescriptionAsDecodedMap, line 18
    for (int i = 0; i < hexStr.length(); i += 2) { // library marker matterTools.parseDescriptionAsDecodedMap, line 19
      baos.write(Integer.parseInt(hexStr.substring(i, i + 2), 16)); // library marker matterTools.parseDescriptionAsDecodedMap, line 20
    }  // library marker matterTools.parseDescriptionAsDecodedMap, line 21
    return baos.toString() // library marker matterTools.parseDescriptionAsDecodedMap, line 22
    // return new String(baos.toByteArray() );      // library marker matterTools.parseDescriptionAsDecodedMap, line 23
} // library marker matterTools.parseDescriptionAsDecodedMap, line 24

// Strings are immutable in groovy, but StringBuilder strings are not.  // library marker matterTools.parseDescriptionAsDecodedMap, line 26
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string. // library marker matterTools.parseDescriptionAsDecodedMap, line 27
Object getTagValue(StringBuilder valueString, Integer tagControl){ // library marker matterTools.parseDescriptionAsDecodedMap, line 28
    Object rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 29
    switch(tagControl){ // library marker matterTools.parseDescriptionAsDecodedMap, line 30
        case 0b000: // 0 Octets // library marker matterTools.parseDescriptionAsDecodedMap, line 31
            rValue = null; break; // library marker matterTools.parseDescriptionAsDecodedMap, line 32
        case 0b001: // Context-specific, 1 octet // library marker matterTools.parseDescriptionAsDecodedMap, line 33
            rValue = Integer.parseInt(valueString[0..1] , 16); valueString.delete(0,2); break // library marker matterTools.parseDescriptionAsDecodedMap, line 34
        case 0b010: // Common Profile, 2 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 35
            rValue = valueString[0..3]; valueString.delete(0,4);  break // library marker matterTools.parseDescriptionAsDecodedMap, line 36
        case 0b011: // Common Profile, 4 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 37
            rValue = valueString[0..7]; valueString.delete(0,8); break // library marker matterTools.parseDescriptionAsDecodedMap, line 38
        case 0b100: // Implicit Profile, 2 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 39
            rValue = valueString[0..3]; valueString.delete(0,4); break // library marker matterTools.parseDescriptionAsDecodedMap, line 40
        case 0b101: // Implicit Profile, 4 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 41
            rValue = valueString[0..7]; valueString.delete(0,8); break // library marker matterTools.parseDescriptionAsDecodedMap, line 42
        case 0b110: // Fully-Qualified form, 6 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 43
            rValue = valueString[0..11];  valueString.delete(0,12); break // library marker matterTools.parseDescriptionAsDecodedMap, line 44
        case 0b111: // Fully-Qualified form, 8 octets. Not really sure how this should be represented. For now, using a string! // library marker matterTools.parseDescriptionAsDecodedMap, line 45
            rValue = valueString[0..15];  valueString.delete(0, 16); break // library marker matterTools.parseDescriptionAsDecodedMap, line 46
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 47
    return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 48
} // library marker matterTools.parseDescriptionAsDecodedMap, line 49

// Strings are immutable in groovy, but StringBuilder strings are not.  // library marker matterTools.parseDescriptionAsDecodedMap, line 51
// Since the valueString is changed within the function, it needs to be passed as a StringBuilder type string // library marker matterTools.parseDescriptionAsDecodedMap, line 52
Object getElementValue(StringBuilder valueString, Integer elementType){ // library marker matterTools.parseDescriptionAsDecodedMap, line 53
    Object rValue = null // library marker matterTools.parseDescriptionAsDecodedMap, line 54
    try { // library marker matterTools.parseDescriptionAsDecodedMap, line 55
		switch(elementType){ // library marker matterTools.parseDescriptionAsDecodedMap, line 56
		case 0b00000: // Signed Integer, 1-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 57
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 58
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet // library marker matterTools.parseDescriptionAsDecodedMap, line 59
			if(rValue & 0x80) rValue = rValue - 256 // Make into a negative if greater than 0x80 // library marker matterTools.parseDescriptionAsDecodedMap, line 60
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 61
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 62
		case 0b00001: // Signed Integer, 2-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 63
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 64
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 65
			if(rValue & 0x8000) rValue = rValue - 65536 // Make into a negative if greater than 0x8000 // library marker matterTools.parseDescriptionAsDecodedMap, line 66
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 67
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 68
		case 0b00010: // Signed Integer, 4-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 69
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 70
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) as Integer // Parse the next 4 octets. Need to parse as Long then change to Integer or can get a numeric exception on negative numbers (odd!) // library marker matterTools.parseDescriptionAsDecodedMap, line 71
			// if(rValue & 0x8000_0000) rValue = rValue - 0xFFFF_FFFF -1 // Make into a negative if greater than 0x8000_0000 // library marker matterTools.parseDescriptionAsDecodedMap, line 72
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 73
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 74
		case 0b00011: // Signed Integer, 8-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 75
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 76
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) as Long // Parse the next 8 octets then change to long. // library marker matterTools.parseDescriptionAsDecodedMap, line 77
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 78
			return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 79
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 80

		case 0b00100: // Unsigned Integer, 1-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 82
			assert valueString.length() >= 2 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 83
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // Parse the next octet // library marker matterTools.parseDescriptionAsDecodedMap, line 84
			valueString = valueString.delete(0, 2) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 85
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 86
		case 0b00101: // Unsigned Integer, 2-Octet // library marker matterTools.parseDescriptionAsDecodedMap, line 87
			assert valueString.length() >= 4 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 88
			rValue = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // Parse the next 2 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 89
			valueString = valueString.delete(0, 4) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 90
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 91
		case 0b00110: // Unsigned Integer, 4-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values! // library marker matterTools.parseDescriptionAsDecodedMap, line 92
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 93
			rValue = Long.parseLong(byteReverseParameters(valueString[0..7]), 16) // Parse the next 4 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 94
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 95
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 96
		case 0b00111: // Unsigned Integer, 8-Octet - Need to return as an 8 Octet Long, since normal 4 Octet Integer can't fit all unsigned values! // library marker matterTools.parseDescriptionAsDecodedMap, line 97
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 98
			rValue = (new BigInteger(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets as BigInteger. // library marker matterTools.parseDescriptionAsDecodedMap, line 99
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 100
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 101

		case 0b01000: // Boolean False // library marker matterTools.parseDescriptionAsDecodedMap, line 103
			rValue = false;  // library marker matterTools.parseDescriptionAsDecodedMap, line 104
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 105
			case 0b01001: // Boolean True // library marker matterTools.parseDescriptionAsDecodedMap, line 106
			rValue = true;  // library marker matterTools.parseDescriptionAsDecodedMap, line 107
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 108

		case 0b01010: // Floating Point, 4-Octet Value // library marker matterTools.parseDescriptionAsDecodedMap, line 110
			assert valueString.length() >= 8 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 111
			rValue = Float.intBitsToFloat(Integer.parseInt(byteReverseParameters(valueString[0..7]), 16)) // Parse the next 4 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 112
			valueString = valueString.delete(0, 8) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 113
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 114
		case 0b01011: // Floating Point, 8-Octet Value // library marker matterTools.parseDescriptionAsDecodedMap, line 115
			assert valueString.length() >= 16 // If this fails, length is too short. Raise an assertion error! // library marker matterTools.parseDescriptionAsDecodedMap, line 116
			rValue = Double.longBitsToDouble(Long.parseLong(byteReverseParameters(valueString[0..15]), 16)) // Parse the next 8 octets // library marker matterTools.parseDescriptionAsDecodedMap, line 117
			valueString = valueString.delete(0, 16) // Trim valueString to remove the octets that were just processed // library marker matterTools.parseDescriptionAsDecodedMap, line 118
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 119

		case 0b01100: // UTF-8 String, 1-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 121
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 122
			valueString = valueString.delete(0, 2) // library marker matterTools.parseDescriptionAsDecodedMap, line 123
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 124
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 125
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 126
			break;                  // library marker matterTools.parseDescriptionAsDecodedMap, line 127
		case 0b01101: // UTF-8 String, 2-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 128
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 129
			valueString = valueString.delete(0, 4) // library marker matterTools.parseDescriptionAsDecodedMap, line 130
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 131
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 132
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 133
			break;                              // library marker matterTools.parseDescriptionAsDecodedMap, line 134
		case 0b01110: // UTF-8 String, 4-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 135
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 136
			valueString = valueString.delete(0, 8) // library marker matterTools.parseDescriptionAsDecodedMap, line 137
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 138
			rValue = HexToString(valueString[0..(length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 139
			valueString = valueString.delete(0, (length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 140
			break;                              // library marker matterTools.parseDescriptionAsDecodedMap, line 141
		case 0b01111: // UTF-8 String, 8-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 142
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 143
			valueString = valueString.delete(0, 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 144
            if (length == 0) { rValue = ""; break } // library marker matterTools.parseDescriptionAsDecodedMap, line 145
			rValue = HexToString(valueString[0..((int)length*2-1)]) // library marker matterTools.parseDescriptionAsDecodedMap, line 146
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 147
			break;   // library marker matterTools.parseDescriptionAsDecodedMap, line 148

		case 0b10000: // Octet String, 1-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 150
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..1]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 151
			valueString = valueString.delete(0, 2) // library marker matterTools.parseDescriptionAsDecodedMap, line 152
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 153
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 154
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 155
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 156
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 157
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 158
		case 0b10001: // Octet String, 2-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 159
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..3]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 160
			valueString = valueString.delete(0, 4) // library marker matterTools.parseDescriptionAsDecodedMap, line 161
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 162
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 163
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 164
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 165
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 166
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 167
		case 0b10010: // Octet String, 4-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 168
			Integer length = Integer.parseInt(byteReverseParameters(valueString[0..7]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 169
			valueString = valueString.delete(0, 8) // library marker matterTools.parseDescriptionAsDecodedMap, line 170
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 171
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 172
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 173
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 174
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 175
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 176
		case 0b10011: // Octet String, 8-octet length // library marker matterTools.parseDescriptionAsDecodedMap, line 177
			Long length = Long.parseLong(byteReverseParameters(valueString[0..15]), 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 178
			valueString = valueString.delete(0, 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 179
			rValue = new byte[length] // library marker matterTools.parseDescriptionAsDecodedMap, line 180
			for(i = 0; i<length; i++) {  // library marker matterTools.parseDescriptionAsDecodedMap, line 181
			 rValue[i] = Integer.parseInt(valueString[(i*2)..(i*2+1)], 16) as Byte // library marker matterTools.parseDescriptionAsDecodedMap, line 182
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 183
			valueString = valueString.delete(0, ((int)length*2)) // library marker matterTools.parseDescriptionAsDecodedMap, line 184
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 185

		case 0b10100: // Null // library marker matterTools.parseDescriptionAsDecodedMap, line 187
			rValue = null;  // library marker matterTools.parseDescriptionAsDecodedMap, line 188
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 189

		case 0b10101: // Structure // library marker matterTools.parseDescriptionAsDecodedMap, line 191
			 rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 192

			// Now add each sub-element to the structure. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 194
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 195
			    // Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 196
			    rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 197
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 198
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 199
			if(rValue.every{it instanceof Map}){ // library marker matterTools.parseDescriptionAsDecodedMap, line 200
			     rValue = rValue.collectEntries({ it }) // library marker matterTools.parseDescriptionAsDecodedMap, line 201
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 202
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 203
		case 0b10110: // Array // library marker matterTools.parseDescriptionAsDecodedMap, line 204
			rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 205
			// Now add each sub-element to the Array. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 206
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 207
			    // Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 208
			    rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 209
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 210
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 211
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 212
		case 0b10111: // List // library marker matterTools.parseDescriptionAsDecodedMap, line 213
			rValue = [] // library marker matterTools.parseDescriptionAsDecodedMap, line 214
			// Now add each sub-element to the List. Maximum 100 times through the loop! // library marker matterTools.parseDescriptionAsDecodedMap, line 215
			for(int i = 0; (Integer.parseInt(valueString[0..1], 16) != 0b00011000) && (i<100); i++) { // IF the next Octet is not the End-Of-Container // library marker matterTools.parseDescriptionAsDecodedMap, line 216
			// Recursively process the contents and push into the map rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 217
			rValue << parseToValue(valueString) // library marker matterTools.parseDescriptionAsDecodedMap, line 218
			} // library marker matterTools.parseDescriptionAsDecodedMap, line 219
			valueString = valueString.delete(0,2) // Reached End-Of-Container, so trim that off! // library marker matterTools.parseDescriptionAsDecodedMap, line 220
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 221
		case 0b00011000: // End of container // library marker matterTools.parseDescriptionAsDecodedMap, line 222
			log.error "end-of-container encountered. Should have been caught in the struture, list, or array processing loop. What happened?" // library marker matterTools.parseDescriptionAsDecodedMap, line 223
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 224
		case 0b11001: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 225
		case 0b11010: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 226
		case 0b11011: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 227
		case 0b11100: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 228
		case 0b11101: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 229
		case 0b11110: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 230
		case 0b11111: // Reserved // library marker matterTools.parseDescriptionAsDecodedMap, line 231
			log.error "Received a Reserved value - Whaaaat?"; break // library marker matterTools.parseDescriptionAsDecodedMap, line 232
			rValue= null // library marker matterTools.parseDescriptionAsDecodedMap, line 233
			break; // library marker matterTools.parseDescriptionAsDecodedMap, line 234
		}    // library marker matterTools.parseDescriptionAsDecodedMap, line 235
        return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 236
    } catch(AssertionError e)  { // library marker matterTools.parseDescriptionAsDecodedMap, line 237
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 238
        return null // library marker matterTools.parseDescriptionAsDecodedMap, line 239
    }catch(e) { // library marker matterTools.parseDescriptionAsDecodedMap, line 240
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 241
        return null // library marker matterTools.parseDescriptionAsDecodedMap, line 242
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 243
} // library marker matterTools.parseDescriptionAsDecodedMap, line 244

// This parser handles the Matter event message originating from Hubitat. // library marker matterTools.parseDescriptionAsDecodedMap, line 246
// valueString is the string description.value originally passed to the driver's parse(description) method from Hubitat // library marker matterTools.parseDescriptionAsDecodedMap, line 247
Object parseToValue(StringBuilder valueString) { // library marker matterTools.parseDescriptionAsDecodedMap, line 248
    if(valueString?.length() < 2) return null // library marker matterTools.parseDescriptionAsDecodedMap, line 249
    Integer controlOctet = Integer.parseInt(valueString[0..1], 16) // library marker matterTools.parseDescriptionAsDecodedMap, line 250
    assert !(isReservedValue(controlOctet)) // Should never get a reserved value! // library marker matterTools.parseDescriptionAsDecodedMap, line 251
    Integer elementType = controlOctet & 0b00011111 // library marker matterTools.parseDescriptionAsDecodedMap, line 252
    Integer tagControl  = (controlOctet & 0b11100000) >> 5 // library marker matterTools.parseDescriptionAsDecodedMap, line 253
    valueString.delete(0,2) // Delete the control octet since its been convereted to tagControl and ElementType // library marker matterTools.parseDescriptionAsDecodedMap, line 254
    Object tag = getTagValue(valueString, tagControl) // library marker matterTools.parseDescriptionAsDecodedMap, line 255
    Object element = getElementValue(valueString, elementType) // library marker matterTools.parseDescriptionAsDecodedMap, line 256
    return (tag.is(null)) ? (element) : [(tag):(element)] // library marker matterTools.parseDescriptionAsDecodedMap, line 257
} // library marker matterTools.parseDescriptionAsDecodedMap, line 258

Map parseRattrDescription(description){ // library marker matterTools.parseDescriptionAsDecodedMap, line 260
    assert (description[0..8] == "read attr")  // library marker matterTools.parseDescriptionAsDecodedMap, line 261
    return description.substring( description.indexOf("-") +1).split(",") // library marker matterTools.parseDescriptionAsDecodedMap, line 262
                        .collectEntries{ entry -> def pair = entry.split(":");   // library marker matterTools.parseDescriptionAsDecodedMap, line 263
                            [(pair.first().trim()):(pair.last().trim())]  // library marker matterTools.parseDescriptionAsDecodedMap, line 264
                        }   // library marker matterTools.parseDescriptionAsDecodedMap, line 265
} // library marker matterTools.parseDescriptionAsDecodedMap, line 266

Map parseDescriptionAsDecodedMap(description){ // library marker matterTools.parseDescriptionAsDecodedMap, line 268
    try { // library marker matterTools.parseDescriptionAsDecodedMap, line 269
        Map rattrKeyValues = parseRattrDescription(description) // library marker matterTools.parseDescriptionAsDecodedMap, line 270
        Map rValue = [:] // library marker matterTools.parseDescriptionAsDecodedMap, line 271
        rValue.put( ("clusterInt"),  Integer.parseInt(rattrKeyValues.cluster, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 272
        rValue.put( ("attrInt"),     Integer.parseInt(rattrKeyValues.attrId, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 273
        rValue.put( ("endpointInt"), Integer.parseInt(rattrKeyValues.endpoint, 16) ) // library marker matterTools.parseDescriptionAsDecodedMap, line 274

        StringBuilder parseRattrString = new StringBuilder(rattrKeyValues.value) // library marker matterTools.parseDescriptionAsDecodedMap, line 276
        Object decodedValue = parseToValue(parseRattrString) // library marker matterTools.parseDescriptionAsDecodedMap, line 277
        rValue.put("decodedValue", decodedValue)  // library marker matterTools.parseDescriptionAsDecodedMap, line 278
        return rValue // library marker matterTools.parseDescriptionAsDecodedMap, line 279
    } catch(AssertionError e)  { // library marker matterTools.parseDescriptionAsDecodedMap, line 280
        log.error "In method parseDescriptionAsDecodedMap, Assertion failed with <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 281
    } catch(e) { // library marker matterTools.parseDescriptionAsDecodedMap, line 282
        log.error "In method parseDescriptionAsDecodedMap, error is <pre>${e}" // library marker matterTools.parseDescriptionAsDecodedMap, line 283
    } // library marker matterTools.parseDescriptionAsDecodedMap, line 284
} // library marker matterTools.parseDescriptionAsDecodedMap, line 285

// ~~~~~ end include (18) matterTools.parseDescriptionAsDecodedMap ~~~~~
