library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Create Hubitat Events from Matter Attribute Data",
        name: "createMatterEvents",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)

import java.lang.Math
import groovy.transform.Field
import groovy.json.JsonBuilder


@Field static Map colorRGBName = [
    (0..4): 'Red',
    (5..13):'Orange',
    (14..21):'Yellow',
    (22..29):'Chartreuse',
    (30..38):'Green',
    (29..46):'Spring',
    (47..54):'Cyan',
    (55..63):'Azure',
    (64..71):'Blue',
    (72..79):'Violet',
    (80..88):'Magenta',
    (89..96):'Rose',
    (97..100):'Red' // hue gets modded with 100 so true range should be 0 - 99.
]

@Field static Map concentrationMeasurementCluster = [
    0x0000:[],
    0x0001:[],
    0x0002:[],
    0x0003:[],
    0x0004:[],
    0x0005:[],
    0x0006:[],
    0x0007:[],
    0x0008:[],
    0x0009:[],
    0x000A:[]
]


// Each of the following Matter-defined Enums can be indexed by either the Hex value, or its integer equivalent
@Field static Map IdentifyTypeEnum =   [ 0:"None",     1:"LightOutput",    2:"VisibleIndicator",    3:"AudibleBeep",    4:"Display",    5:"Actuator"] // Cluste 0x0003
@Field static Map StartUpOnOffEnum =   [ 0:"Off",     1:"On",    2:"Toggle"] // Cluster 0x0006
@Field static Map AirQualityEnumType = [ 0:"Unknown",     1:"Good",    2:"Fair",    3:"Moderate",    4:"Poor",    5:"VeryPoor",    6:"ExtremelyPoor"]
@Field static Map ExpressedStateEnum = [0:"Normal", 1:"SmokeAlarm", 2:"COAlarm", 3:"BatteryAlert", 4:"Testing", 5:"HardwareFault", 6:"EndOfService", 7:"InterconnectSmoke", 8:"InterconnectCO"]
@Field static Map AlarmStateEnum = [0:"Normal", 1:"Warning", 2:"Critical" ]
@Field static Map MuteStateEnum = [0:"NotMuted", 1:"Muted" ]
@Field static Map ContaminationStateEnum = [0:"Normal", 1:"Low", 2:"Warning", 3:"Critical" ]
@Field static Map EndOfServiceEnum = [0:"Normal", 1:"Expired"]
@Field static Map SensitivityEnum = [0:"High", 1:"Standard", 2:"Low"]
@Field static Map SecurityTypeEnum = [0:"Unspecified", 1:"Nne", 2:"WEP", 3:"WPA", 4:"WPA2", 5:"WPA3"]
@Field static Map WiFiVersionEnum = [0:"a", 1:"b", 2:"g", 3:"n", 4:"ac", 5:"ax", 6:"ah"]
@Field static Map NetworkFaultEnum = [0:"Unspecified", 1:"LinkDown", 2:"HardwareFailure", 3:"NetworkJammed"]
@Field static Map ConnectionStatusEnum = [0:"Connected", 1:"NotConnected"]
@Field static Map RoutingRoleEnumType = [0:"Unspecified", 1:"Unassigned", 2:"SleepyEndDevice", 3:"EndDevice", 4:"REED", 5:"Router", 6:"Leader"]
//Enums for the Basic Cluster
@Field static Map ProductfinishEnumType = [0:"Other", 1:"Matte", 2:"Satin", 3:"Polished", 4:"Rugged", 5:"Fabric"]
@Field static Map colorEnum = [
	0:"Black", 1:"Navy", 2:"Green", 3:"Teal", 4:"Maroon", 5:"Purple", 6:"Olive", 7:"Gray", 
	8:"Blue", 9:"Lime", 10:"Aqua", 11:"Red", 12:"Fuscia", 13:"Yellow", 14:"White", 
	15:"Nickel", 16:"Chrome", 17:"Brass", 18:"Copper", 19:"Silver", 20:"Gold"
    ]
// Enums for the PowerSource Cluster
@Field static Map WiredFaultEnum = [0:"Unspecified", 1:"OverVoltage", 2:"UnderVoltage"]
@Field static Map BatFaultEnum = [0:"Unspecified", 1:"OverTemp", 2:"UnderTemp"]
@Field static Map BatChargeFaultEnum = [
	0:"Unspecified", 1:"AmbientTooHot", 2:"AmbientTooCold", 3:"BatteryTooHot", 4:"BatteryTooCold", 5:"BatteryAbsent", 
	6:"BatteryOverVoltage", 7:"BatteryUnderVoltage", 8:"ChargerOverVoltage", 9:"ChargerUnderVoltage", 10:"SafetyTimeout", 
	11:"ChargerOverCurrent", 12:"UnexpectedVoltage", 13:"ExpectedVoltage", 14:"GroundFault", 15:"ChargeSignalFailure", 16:"SafetyTimeout" 
    ]
@Field static Map BatChargePermissionsEnum = [ 0:"Disabled", 1:"ChargingEnabled", 2:"DischargingEnabled", 3:"DisabledError", 4:"DisabledDiagnostics" ]
@Field static Map BatChargeStateEnum = [ 0:"Unknown", 1:"IsCharging", 2:"IsAtFullCharge", 3:"IsNotCharging", 4:"IsDischarging", 5:"IsTransitioning", ]
@Field static Map PowerSourceStatusEnum = [ 0:"Unspecified", 1:"Active", 2:"Standby", 3:"Unavailable"]
@Field static Map WiredCurrentTypeEnum = [ 0:"AC", 1:"DC"]
@Field static Map BatChargeLevelEnum = [ 0:"OK", 1:"Warning", 2:"Critical"]
@Field static Map BatReplaceabilityEnum = [ 0:"Unspecified", 1:"NotReplaceable", 2:"UserReplaceable", 3:"FactoryReplaceable"]
@Field static Map BatCommonDesignationEnum = [ 
	0:"Unspecified", 1:"AAA", 2:"AA", 3:"C", 4:"D", 5:"4v5", 6:"6v0", 7:"9v0", 
	8:"1_2AA", 9:"AAAA", 10:"A", 11:"B", 12:"F", 13:"N", 14:"No6", 15:"SubC", 16:"A23",
	17:"A27", 18:"BA5800", 19:"Duplex", 20:"4SR44", 21:"523", 22:"531", 23:"15V0", 24:"22v5",
	25:"30v0", 26:"45v0", 27: "67v5", 28:"J", 29:"CR123A", 30:"CR2", 31:"2CR5", 32:"CR_P2", 33:"CR_V3",
	34:"SR41", 35:"SR43", 36:"SR44", 37:"SR45", 38:"SR48", 39:"SR54", 40:"SR55", 41:"SR57", 42:"SR58",
	43:"SR59", 44:"SR60", 45:"SR63", 46:"SR64", 47:"SR65", 48:"SR66", 49:"SR67", 50:"SR68", 51:"SR69",
	52:"SR516", 53:"SR731", 54:"SR712", 55:"LR932", 56:"A5", 57:"A10", 58:"A13", 
	59:"A312", 60:"A675", 61:"AC41E", 62:"10180", 63:"10280", 64:"10440", 65:"14250", 66:"14430",
	67:"14500", 68:"14650", 69:"15270", 70:"16340", 71:"RCR123A", 72:"17500", 73:"17670", 74:"18350",
	75:"18500", 76:"18650", 77:"19670", 78:"2550", 79:"26650", 80:"32600"
    ]
@Field static Map BatApprovedChemistryEnum = [
	0:"Unspecified", 
	1:"Alkaline", 2:"LithiumCarbonFluoride", 3:"LithiumChromiumOxide", 4:"LithiumCopperOxide", 5:"LithiumIronDisulfide", 
	6:"LithiumManganeseDioxide", 7:"LithiumThionylChloride", 8:"Magnesium", 9:"MercuryOxide", 10:"NickelOxyhydride", 
	11:"SilverOxide", 12:"ZincAir", 13:"ZincCarbon", 14:"ZincChloride", 15:"ZincManganeseDioxide", 
	16:"LeadAcid", 17:"LithiumCobaltOxide", 18:"LithiumIon", 19:"LithiumIonPolymer", 20:"LithiumIronPhosphate", 
	21:"LithiumSulfur", 22:"LithiumTitanate", 23:"NickelCadmium", 24:"NickelHydrogen", 25:"NickelIron", 
	26:"NickelMetalHydride", 27:"NickelZinc", 28:"SilverZinc", 29:"SodiumIon", 30:"SodiumSulfur", 
	31:"ZincBromide", 32:"ZincCerium",
    ]    
// //////////////////////

@Field static Map RoutingRoleEnum = [0:"Unspecified", 1:"Unassigned", 2:"SleepyEndDevice", 3:"EndDevice", 4:"REED", 5:"Router", 6:"Leader"]


@Field static Closure toInt =  { String pv -> Integer.parseInt(pv, 16)} // Hex to Integer conversion.
@Field static Closure toBool = { String pv -> toInt(pv) ? true : false} // Hex to Integer conversion.
@Field static Closure toLong = { String pv -> Long.parseLong(pv, 16) } // Hex to Long

@Field static Closure HexToPercent = { String pv -> Math.round(Integer.parseInt(pv, 16) / 2.54) as Integer}

@Field static Closure HexToLux = { String pv -> Math.pow( 10, (toInt(pv) -1) / 10000)  as Integer} // convert Matter value to illumination in lx. See Matter Cluster Spec Section 2.2.5.1
@Field static Closure HuePercent2Name = { Integer pv -> (colorRGBName.find{ entry -> entry.key.contains(pv as Integer)}).value}
@Field static Closure HexTempToName = {String pv -> "DummyName"}
@Field static Closure HexMiredsToKelvin =  {String pv -> 1000000 / toInt(pv) as Integer}
@Field static Closure HexHueToName = {String pv -> HuePercent2Name( HexToPercent(pv) % 100 ) }


/*
For Closure values in the following structure:
pv = parsed Map value field (descMap.value)
dn = device name - provided as a string
dv = device value - usually the content of the event map's "value" field after pv has been converted by the closure in the "value" field, below..
*/
@Field static Map globalAllEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events
    0x0003:[ // Identify Cluster
        0x0000:[[attribute:"IdentifyTime", value:{ pv -> toInt(pv)  },     units:"seconds", descriptionText: {dn, dv ->  "${dn}: will identify for ${dv} more seconds."}] ],
        0x0001:[[attribute:"IdentifyType", value:{ pv -> IdentifyTypeEnum.get(toInt(pv))  }, descriptionText: {dn, dv ->  "${dn}: identify device type is ${dv}"}] ],
    ],
    0x0006:[ // Switch Cluster
        0x0000:[[attribute:"switch",             value:{ pv -> toInt(pv) ? "on" : "off" },         descriptionText: {dn, dv ->  "${dn}: turned ${dv}"}],
                [attribute:"OnOff",              value:{ pv -> toBool(pv) },         descriptionText: {dn, dv ->  "${dn}: On State ${dv}"}]],
        0x4000:[[attribute:"GlobalSceneControl", value:{ pv -> toBool(pv) },     descriptionText: {dn, dv ->  "${dn}: Global Scene Status ${dv}"}] ],
        0x4001:[[attribute:"OnTime",             value:{ pv -> toInt(pv) / 10 }, units:"seconds" ]],
        0x4002:[[attribute:"OffWaitTime",        value:{ pv -> toInt(pv) / 10 }, units:"seconds" ]],
        0x4003:[[attribute:"StartUpOnOff",       value:{ pv -> StartUpOnOffEnum.get(toInt(pv))},          descriptionText: {dn, dv ->  "${dn}: Startup state is ${dv}"}] ],
    ],
    0x0008:[ // Level Cluster
        0x0000:[[attribute:"level",                value:{ pv -> HexToPercent(pv)},   units:"%",       descriptionText: {dn, dv ->  "${dn}: level set to ${dv}%"}],
                [attribute:"CurrentLevel",         value:{ pv -> toInt(pv)},          units:"uint8",   descriptionText: {dn, dv ->  "${dn}: Current Matter Level: ${dv} units"}]],
        0x0001:[[attribute:"RemainingTime",        value:{ pv -> toInt(pv) / 10 },    units:"seconds", descriptionText: {dn, dv ->  "${dn}: Remaining transition time is ${dv} seconds"} ]],
        0x0002:[[attribute:"MinLevel",             value:{ pv -> HexToPercent(pv) },  units:"%" ,      descriptionText: {dn, dv ->  "${dn}: Minimum level setting is ${dv}%"}]],
        0x0003:[[attribute:"MaxLevel",             value:{ pv -> HexToPercent(pv) },  units:"%" ,      descriptionText: {dn, dv ->  "${dn}: Maximum level setting is ${dv}%"}]],
        0x0004:[[attribute:"CurrentFrequency",     value:{ pv -> toInt(pv)},          units:"Hz",      descriptionText: {dn, dv ->  "${dn}: Current frequency is ${dv} Hz"}]],
        0x0005:[[attribute:"MinFrequency",         value:{ pv -> toInt(pv)},          units:"Hz",      descriptionText: {dn, dv ->  "${dn}: Minimum frequency is ${dv} Hz"}]],
        0x0006:[[attribute:"MaxFrequency",         value:{ pv -> toInt(pv)},          units:"Hz",      descriptionText: {dn, dv ->  "${dn}: Maximum frequency is ${dv} Hz"}]],
        0x0010:[[attribute:"OnOffTransitionTime",  value:{ pv -> toInt(pv) / 10 },    units:"seconds", descriptionText: {dn, dv ->  "${dn}: Off transition time is ${dv} seconds"}]],
        0x0011:[[attribute:"OnLevel",              value:{ pv -> HexToPercent(pv)},   units:"%",       descriptionText: {dn, dv ->  "${dn}: On level is: ${dv}%"}]],
        0x0012:[[attribute:"OnTransitionTime",     value:{ pv -> toInt(pv) / 10 },    units:"seconds", descriptionText: {dn, dv ->  "${dn}: On transition time is ${dv} seconds"}]],
        0x0013:[[attribute:"OfftransitionTime",    value:{ pv -> toInt(pv) / 10 },    units:"seconds", descriptionText: {dn, dv ->  "${dn}: Off transition time is ${dv} seconds"}]],
        0x0014:[[attribute:"DefaultMoveRate",      value:{ pv -> toInt(pv)},          units:"uint8",   descriptionText: {dn, dv ->  "${dn}: Default move rate in device units per second: ${dv}"}]],
        0x000F:[[attribute:"Options",              value:{ pv -> toInt(pv)},          units:"bitmap",  descriptionText: {dn, dv ->  "${dn}: Options bitmap value is ${dv}"}]],
        0x4000:[[attribute:"StartUpCurrentLevel",  value:{ pv -> toInt(pv)},          units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Start Up Current Level Matter attribute value is: ${dv} units"}]],
    ],
    0x001D:[ // Descriptor - As of Feb. 2024, not parsed correctly by Hubitat, so best to ignore!
        0x0000:[[attribute:"DeviceTypeList",       value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Device Type List: ${dv}"}]],
        0x0001:[[attribute:"ServerList",           value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Server List: ${dv}"}]],
        0x0002:[[attribute:"ClientList",           value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Client List: ${dv}"}]],
        0x0003:[[attribute:"PartsList",            value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Parts List: ${dv}"}]],
        0x0004:[[attribute:"TagList",              value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Tag List: ${dv}"}]],
        ],
    0x0028:[ // Basic Information
        0x0000:[[attribute:"DataModelRevision",       value:{ pv -> toInt(pv) },  descriptionText: {dn, dv ->  "${dn}: Data ModelRevision is: ${dv}"}]],
        0x0001:[[attribute:"VendorName",              value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Vendor Name is: ${dv}"}]],
        0x0002:[[attribute:"VendorID",                value:{ pv -> toInt(pv) },  descriptionText: {dn, dv ->  "${dn}: Vendor ID is: ${dv}"}]],
        0x0003:[[attribute:"ProductName",             value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Product Name is: ${dv}"}]],
        0x0004:[[attribute:"ProductID",               value:{ pv -> toInt(pv) },  descriptionText: {dn, dv ->  "${dn}: Product ID is: ${dv}"}]],
        0x0005:[[attribute:"NodeLabel",               value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Node Label is: ${dv}"}]],
        0x0006:[[attribute:"Location",                value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Location is: ${dv}"}]],
        0x0007:[[attribute:"HardwareVersion",         value:{ pv -> toInt(pv) },  descriptionText: {dn, dv ->  "${dn}: Hardware Version is: ${dv}"}]],
        0x0008:[[attribute:"HardwareVersionString",   value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Hardware Version String is: ${dv}"}]],
        0x0009:[[attribute:"SoftwareVersion",         value:{ pv -> toInt(pv) },  descriptionText: {dn, dv ->  "${dn}: Software Version is: ${dv}"}]],
        0x000A:[[attribute:"SoftwareVersionString",   value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Software Version String is: ${dv}"}]],
        0x000B:[[attribute:"ManufacturingDate",       value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Manufacturing Date is: ${dv}"}]],
        0x000C:[[attribute:"PartNumber",              value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Part Number is: ${dv}"}]],
        0x000D:[[attribute:"ProductURL",              value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Product URL is: ${dv}"}]],
        0x000E:[[attribute:"ProductLabel",            value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Product Label is: ${dv}"}]],
        0x000F:[[attribute:"SerialNumber",            value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Serial Number is: ${dv}"}]],
        0x0010:[[attribute:"LocalConfigDisabled",     value:{ pv -> toBool(pv)},  descriptionText: {dn, dv ->  "${dn}: Local Config Disabled is: ${dv}"}]],
        0x0011:[[attribute:"Reachable",               value:{ pv -> toBool(pv)},  descriptionText: {dn, dv ->  "${dn}: Reachable is: ${dv}"}]],
        0x0012:[[attribute:"UniqueID",                value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: UniqueID is: ${dv}"}]],
        0x0013:[[attribute:"CapabilityMinima",        value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Capability Minima is: ${dv}"}]],
        0x0014:[[attribute:"ProductAppearance",       value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Product Appearance is: ${dv}"}]],
        ],
    0x002B:[ // Localization Configuration
        0x0000:[[attribute:"ActiveLocale",         value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Locale: ${dv}"}]],
        0x0000:[[attribute:"SupportedLocales",     value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Supported Locales: ${dv}"}]],
        ],
    0x002D:[ // Unit Localization
        0x0000:[[attribute:"TemperatureUnit",         value:{ pv -> [0:"Fahrenheit", 1:"Celsius", 2:"Kelvin"].get(toInt(pv)) }, descriptionText: {dn, dv ->  "${dn}: Temperature Unit: ${dv}"}]],
        ],
    0x002F:[ // Power Source Cluster
        0x0000:[[attribute:"Status",						value:{ pv -> PowerSourceStatusEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Status is: ${dv}"}]],
        0x0001:[[attribute:"Order",							value:{ pv -> toInt(pv)}, 							descriptionText: {dn, dv ->  "${dn}: Order is: ${dv}"}]],
        0x0002:[[attribute:"Description",					value:{ pv -> pv},									descriptionText: {dn, dv ->  "${dn}: Description: ${dv}"}]],
        0x0003:[[attribute:"WiredAssessedInputVoltage",		value:{ pv -> toInt(pv) / 1000},  units:"V",  		descriptionText: {dn, dv ->  "${dn}: Wired Assessed Input Voltage: ${dv} Volts"}]],
        0x0004:[[attribute:"WiredAssessedInputFrequency",	value:{ pv -> toInt(pv)},	units:"Hz", 			descriptionText: {dn, dv ->  "${dn}: Wired Assessed Input Frequency is: ${dv}"}]],
        0x0005:[[attribute:"WiredCurrentType",				value:{ pv -> WiredCurrentTypeEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Wired Current Type is: ${dv}"}]],
        0x0006:[[attribute:"WiredAssessedCurrent",			value:{ pv -> toInt(pv) / 1000}, units:"A", 		descriptionText: {dn, dv ->  "${dn}: Wired Assessed Current: ${dv} Amps"}]],
        0x0007:[[attribute:"WiredNominalVoltage",			value:{ pv -> toInt(pv) / 1000}, units:"V",         descriptionText: {dn, dv ->  "${dn}: Wired Nominal Voltage is: ${dv} Volts"}]],
        0x0008:[[attribute:"WiredMaximumCurrent",           value:{ pv -> toInt(pv) / 1000}, units:"A",        	descriptionText: {dn, dv ->  "${dn}: Wired Maximum Current is: ${dv} Amps"}]],
        0x0009:[[attribute:"WiredPresent",					value:{ pv -> toBool(pv) }, 						descriptionText: {dn, dv ->  "${dn}: Wired Present is: ${dv}"}]],
        0x000A:[[attribute:"ActiveWiredFaults",				value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Active Wired Faults is: ${dv}"}]],
        0x000B:[[attribute:"BatVoltage",					value:{ pv -> toInt(pv) / 1000 }, units:"V", 		descriptionText: {dn, dv ->  "${dn}: Battery Voltage is: ${dv}"}]],
        0x000C:[[attribute:"BatPercentRemaining",    		value:{ pv -> toInt(pv) / 2 }, units:"%", 			descriptionText: {dn, dv ->  "${dn}: Battery Percent Remaining is: ${dv}"}],
				[attribute:"battery",    					value:{ pv -> toInt(pv) / 2 }, units:"%", 			descriptionText: {dn, dv ->  "${dn}: Battery Percent Remaining is: ${dv}"}]
				],
        0x000D:[[attribute:"BatTimeRemaining",       		value:{ pv -> toInt(pv) }, units:"seconds", 		descriptionText: {dn, dv ->  "${dn}: Battery Time Remaining is: ${dv}"}]],
        0x000E:[[attribute:"BatChargeLevel",    			value:{ pv -> BatChargeLevelEnum.get(toInt(pv)) }, 	descriptionText: {dn, dv ->  "${dn}: Battery Charge Level is: ${dv}"}]],
        0x000F:[[attribute:"BatReplacementNeeded",       	value:{ pv -> toBool(pv) }, 						descriptionText: {dn, dv ->  "${dn}: Battery Replacement Needed: ${dv}"}]],
        0x0010:[[attribute:"BatReplaceability",				value:{ pv -> BatReplaceabilityEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Battery Replaceability is: ${dv}"}]],
        0x0011:[[attribute:"BatPresent",                    value:{ pv -> toBool(pv) }, 						descriptionText: {dn, dv ->  "${dn}: Battery Present: ${dv}"}]],
        0x0012:[[attribute:"ActiveBatFaults",               value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Active Battery Faults are: ${dv}"}]],
        0x0013:[[attribute:"BatReplacementDescription",		value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Battery Replacement Description: ${dv}"}]],
        0x0014:[[attribute:"BatCommonDesignation",  		value:{ pv -> BatCommonDesignationEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Battery Common  Designation: ${dv}"}]],
        0x0015:[[attribute:"BatANSIDesignation",			value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Battery ANSI C18 Designation: ${dv}"}]],
        0x0016:[[attribute:"BatIECDesignation",             value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Battery IEC 60086 Designation: ${dv}"}]],
        0x0017:[[attribute:"BatApprovedChemistry",      	value:{ pv -> BatApprovedChemistryEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Battery Approved Chemistry: ${dv}"}]],
        0x0018:[[attribute:"BatCapacity",                   value:{ pv -> toInt(pv) }, units:"mAh", 			descriptionText: {dn, dv ->  "${dn}: Battery Capacity: ${dv} mAH"}]],
        0x0019:[[attribute:"BatQuantity",                	value:{ pv -> toInt(pv) }, 							descriptionText: {dn, dv ->  "${dn}: Battery Quantity: ${dv}"}]],
        0x001A:[[attribute:"BatChargeState",                value:{ pv -> BatChargeState.get(toInt(pv)) }, 		descriptionText: {dn, dv ->  "${dn}: Battery Charge State: ${dv}"}]],
        0x001B:[[attribute:"BatTimeToFullCharge",           value:{ pv -> toInt(pv) }, units:"seconds", 		descriptionText: {dn, dv ->  "${dn}: Battery Time To Full Charge: ${dv} seconds"}]],
        0x001C:[[attribute:"BatFunctionalWhileCharging",    value:{ pv -> toBool(pv) }, 						descriptionText: {dn, dv ->  "${dn}: Battery Functional While Charging: ${dv}"}]],
        0x001D:[[attribute:"BatChargingCurrent",            value:{ pv -> toInt(pv) }, 							descriptionText: {dn, dv ->  "${dn}: Battery Charging Current: ${dv}"}]],
        0x001E:[[attribute:"ActiveBatChargeFaults",         value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Active Battery Charge Faults: ${dv}"}]],
        0x001F:[[attribute:"EndpointList",               	value:{ pv -> pv }, 								descriptionText: {dn, dv ->  "${dn}: Power Source Endpoint List: ${dv}"}]],
        ],

    0x0035:[ // Thread Diagnostics
        0x0000:[[attribute:"Channel",              value:{ pv -> toInt(pv)}, units:"octstr",  descriptionText: {dn, dv ->  "${dn}: Channel is: ${dv}"}]],
        0x0001:[[attribute:"RoutingRole",          value:{ pv -> RoutingRoleEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Routing Role is: ${dv}"}]],
        0x0002:[[attribute:"NetworkName",          value:{ pv -> pv},         units:"string",  descriptionText: {dn, dv ->  "${dn}: Network Name is: ${dv}"}]],
        0x0003:[[attribute:"PanId",                value:{ pv -> toInt(pv)},  units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Pan Id: ${dv}"}]],
        0x0004:[[attribute:"ExtendedPanId",        value:{ pv -> toLong(pv)}, units:"uint64",    descriptionText: {dn, dv ->  "${dn}: Extended Pan Id is: ${dv}"}]],
        0x0005:[[attribute:"MeshLocalPrefix",      value:{ pv -> pv}, units:"ipv6pre",  descriptionText: {dn, dv ->  "${dn}: Mesh Local Prefix is: ${dv}"}]],
        0x0006:[[attribute:"OverrunCount",         value:{ pv -> toLong(pv)}, descriptionText: {dn, dv ->  "${dn}: Overrun Count: ${dv}"}]],
        0x0007:[[attribute:"NeighborTable",        value:{ pv -> pv},         units:"list",  descriptionText: {dn, dv ->  "${dn}: Neighbor Table is: ${dv}"}]],
        0x0008:[[attribute:"RouteTable",           value:{ pv -> pv},         units:"list",  descriptionText: {dn, dv ->  "${dn}: Route Table is: ${dv}"}]],
        0x0009:[[attribute:"PartitionId",          value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Partition Id is: ${dv}"}]],
        0x000A:[[attribute:"Weighting",            value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Weighting is: ${dv}"}]],
        0x000B:[[attribute:"DataVersion",          value:{ pv -> toInt(pv) }, units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Data Version is: ${dv}"}]],
        0x000C:[[attribute:"StableDataVersion",    value:{ pv -> toInt(pv) }, units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Stable Data Version is: ${dv}"}]],
        0x000D:[[attribute:"LeaderRouterId",       value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Leader Router Id Id is: ${dv}"}]],
        0x000E:[[attribute:"DetachedRoleCount",    value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Detached Role Count is: ${dv}"}]],
        0x000F:[[attribute:"ChildRoleCount",       value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Child Role Count is: ${dv}"}]],
        
        0x0010:[[attribute:"RouterRoleCount",                    value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Router Role Count is: ${dv}"}]],
        0x0011:[[attribute:"LeaderRoleCount",                    value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Leader Role Count is: ${dv}"}]],
        0x0012:[[attribute:"AttachedAttemptCount",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Attached Attempt Count is: ${dv}"}]],
        0x0013:[[attribute:"PartitionIdChangeCount",             value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Partition Id Change Count is: ${dv}"}]],
        0x0014:[[attribute:"BetterPartitionAttachAttemptCount",  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Better Partition Attach Attempt Count is: ${dv}"}]],
        0x0015:[[attribute:"ParentChangeCount",                  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Parent Change Count is: ${dv}"}]],
        0x0016:[[attribute:"TxTotalCount",                       value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Total Count is: ${dv}"}]],
        0x0017:[[attribute:"TxUnicastCount",                     value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Unicast Count is: ${dv}"}]],
        0x0018:[[attribute:"TxBroadcastCount",                   value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Broadcast Count is: ${dv}"}]],
        0x0019:[[attribute:"TxAckRequestedCount",                value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Ack Requested Count is: ${dv}"}]],
        0x001A:[[attribute:"TxAcked",                            value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Acked is: ${dv}"}]],
        0x001B:[[attribute:"TxNoAckRequestedCount",              value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx NoAck Requested Count is: ${dv}"}]],
        0x001C:[[attribute:"TxDataCount",                        value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Data Count is: ${dv}"}]],
        0x001D:[[attribute:"TxDataPollCount",                    value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx DataPoll Count is: ${dv}"}]],
        0x001E:[[attribute:"TxBeaconCount",                      value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Beacon Count is: ${dv}"}]],
        0x001F:[[attribute:"TxBeaconRequestCount",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Beacon Request Count is: ${dv}"}]],
        
        0x0020:[[attribute:"TxOtherCount",                  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Other Count is: ${dv}"}]],
        0x0021:[[attribute:"TxRetryCount",                  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Retry Count is: ${dv}"}]],
        0x0022:[[attribute:"TxDirectMaxRetryExpiryCount",   value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Direct Max Retry Expiry Count is: ${dv}"}]],
        0x0023:[[attribute:"TxIndirectMaxRetryExpiryCount", value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Indirect  Max Retry Expiry Count is: ${dv}"}]],
        0x0024:[[attribute:"TxErrCcaCount",                 value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Err Cca Count is: ${dv}"}]],
        0x0025:[[attribute:"TxErrAbortCount",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Err Abort Count is: ${dv}"}]],
        0x0026:[[attribute:"TxErrBusyChannelCount",         value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Tx Err Busy Channel Count is: ${dv}"}]],
        0x0027:[[attribute:"RxTotalCount",                  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Total Count is: ${dv}"}]],
        0x0028:[[attribute:"RxUnicastCount",                value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Unicast Count is: ${dv}"}]],
        0x0029:[[attribute:"RxBroadcastCount",              value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Broadcast Count is: ${dv}"}]],
        0x002A:[[attribute:"RxDataCount",                   value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Data Count is: ${dv}"}]],
        0x002B:[[attribute:"RxDataPollCount",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Data Poll Count is: ${dv}"}]],
        0x002C:[[attribute:"RxBeaconCount",                 value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Beacon Count is: ${dv}"}]],
        0x002D:[[attribute:"RxBeaconRequestCount",          value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Beacon Request Count is: ${dv}"}]],
        0x002E:[[attribute:"RxOtherCount",                  value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Other Count is: ${dv}"}]],
        0x002F:[[attribute:"RxAddressFilteredCount",        value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Address Filtered Count is: ${dv}"}]],
        
        0x0030:[[attribute:"RxDestAddrFilteredCount",       value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Dest Addr Filtered Count is: ${dv}"}]],
        0x0031:[[attribute:"RxDuplicatedCount",             value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Duplicated Count is: ${dv}"}]],
        0x0032:[[attribute:"RxErrNoFrameCount",             value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err No Frame Count is: ${dv}"}]],
        0x0033:[[attribute:"RxErrUnknownNeighborCount",     value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err Unknown Neighbor Count is: ${dv}"}]],
        0x0034:[[attribute:"RxErrInvalidSrcAddrCount",      value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err Invalid Src Addr Count is: ${dv}"}]],
        0x0035:[[attribute:"RxErrSecCount",                 value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err Sec Count is: ${dv}"}]],
        0x0036:[[attribute:"RxErrFcsCount",                 value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err Fcs Count is: ${dv}"}]],
        0x0037:[[attribute:"RxErrOtherCount",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Rx Err Other Count is: ${dv}"}]],
        0x0038:[[attribute:"ActiveTimestamp",               value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Active Timestamp is: ${dv}"}]],
        0x0039:[[attribute:"PendingTimestamp",              value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Pending Timestamp is: ${dv}"}]],
        0x003A:[[attribute:"Delay",                         value:{ pv -> toInt(pv) }, descriptionText: {dn, dv ->  "${dn}: Delay is: ${dv}"}]],
        // 0x003B:[[attribute:"SecurityPolicy",                value:{ pv -> toInt(pv) }, units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Data Version is: ${dv}"}]],
        // 0x003C:[[attribute:"ChannelPage0Mask",              value:{ pv -> toInt(pv) }, units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Stable Data Version is: ${dv}"}]],
        // 0x003D:[[attribute:"OperationalDatasetComponents",  value:{ pv -> toInt(pv) }, units:"uint32",  descriptionText: {dn, dv ->  "${dn}: Weighting is: ${dv}"}]],
        // 0x003E:[[attribute:"ActiveNEtworkFaults",           value:{ pv -> toInt(pv) }, units:"uint16",  descriptionText: {dn, dv ->  "${dn}: Data Version is: ${dv}"}]],
        ],
    0x0036:[ // WiFi Diagnostics
        0x0000:[[attribute:"BSSID",                   value:{ pv -> pv}, units:"octstr",      descriptionText: {dn, dv ->  "${dn}: BSSID: ${dv}"}]],
        0x0001:[[attribute:"SecurityType",            value:{ pv -> SecurityTypeEnum.get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Security Type: ${dv}"}]],
        0x0002:[[attribute:"WiFiVersion",             value:{ pv -> WiFiVersionEnum.get(toInt(pv))},  descriptionText: {dn, dv ->  "${dn}: WiFi Version 802.11${dv}"}]],
        0x0003:[[attribute:"ChannelNumber",           value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: WiFi Channel: ${dv}"}]],
        0x0004:[[attribute:"RSSI",                    value:{ pv -> toInt(pv) - 256},          descriptionText: {dn, dv ->  "${dn}: WiFi RSSI is: ${dv}"}]],
        0x0005:[[attribute:"BeaconLostCount",         value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Becon Lost Count is: ${dv}"}]],
        0x0006:[[attribute:"BeaconRxCount",           value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Beacon Rx Count: ${dv}"}]],
        0x0007:[[attribute:"PacketMulticastRxCount",  value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Packet Multicast Rx Count is: ${dv}"}]],
        0x0008:[[attribute:"PacketMulticastTxCount",  value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Packet Multicast Tx Count is: ${dv}"}]],
        0x0009:[[attribute:"PacketUnicastRxCount",    value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Packet Unicast Rx Count is: ${dv}"}]],
        0x000A:[[attribute:"PacketUnicastTxCount",    value:{ pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: Packet Unicast Tx Count is: ${dv}"}]],
        0x000B:[[attribute:"CurrentMaxRate",          value:{ pv -> toLong(pv)},              descriptionText: {dn, dv ->  "${dn}: Current Max Rate is: ${dv}"}]],
        0x000C:[[attribute:"OverrunCount",            value:{ pv -> toLong(pv)},              descriptionText: {dn, dv ->  "${dn}: Overrun Count is: ${dv}"}]],
        ],
    0x003B:[ // Generic Switch Cluster
        0x0000:[[attribute:"NumberOfPositions",    value:{ pv -> toInt(pv)}, descriptionText: {dn, dv ->  "${dn}: Number of Switch Positions: ${dv}"}]], // Number of Plsitions
        0x0001:[[attribute:"CurrentPosition",      value:{ pv -> toInt(pv)}, descriptionText: {dn, dv ->  "${dn}: Current Switch Position: ${dv}"}]], // Current Position
        0x0002:[[attribute:"MultiPressMax",        value:{ pv -> toInt(pv)}, descriptionText: {dn, dv ->  "${dn}: Maximum Presses for Multipress: ${dv}"}]], // Multi-Press Max
        ],

    0x0300:[ // Color Control Cluster.  Only covering the most common ones at the moment!
		0x0000: [ // Hue
			[attribute:"hue", value:{pv -> HexToPercent(pv)}, units:"%",     descriptionText:{dn, dv ->  "${dn}: Hue set to ${dv}%"}], //  This is the Hubitat name/value
			[attribute:"colorName", value:{pv -> HexHueToName(pv) },         descriptionText:{dn, dv ->  "${dn}: Color set to ${dv}"}], // generate color name when hue is reported
			[attribute:"CurrentHue", value:{pv -> toInt(pv)}, units:"uint8", descriptionText:{dn, dv ->  "${dn}: Hue as value 0..254: ${dv}"}] // This is the Matter name / value
            ],
        0x0001: [ [attribute:"saturation",         value: {pv -> HexToPercent(pv)}, units:"%",  descriptionText: {dn, dv ->  "${dn}: Saturation set to ${dv}%"}], 
                  [attribute:"CurrentSaturation",  value:{pv -> toInt(pv)}, units:"uint8",      descriptionText:{dn, dv ->  "${dn}: Current saturation (0..254): ${dv}"}]
                ],
        0x0002: [ [attribute:"RemainingTime",     value: {pv -> toInt(pv)/10}, units:"seconds", descriptionText: {dn, dv ->  "${dn}: Remaining Color Transition Time ${dv} seconds"} ] ],
        0x0007: [ [attribute:"colorTemperature",  value: {pv -> HexMiredsToKelvin(pv)},         descriptionText: {dn, dv ->  "${dn}: color temperature: ${dv} Kelvin"}, unit: "°K"], 
                  [attribute:"ColorTemperatureMireds", value: {pv -> toInt(pv)},                descriptionText: {dn, dv ->  "${dn}: color temperature: ${dv} Mireds"}, unit: "Mireds"],
                  [attribute:"colorName",         value: {pv -> HexTempToName(pv)},             descriptionText: {dn, dv ->  "${dn}: Color set to ${dv}"}] 
                ],
        0x0008: [[attribute:"colorMode",          value: {pv -> [0:"RGB", 1:"CurrentXY", 2:"CT"].get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Color Name set to ${dv}"}], // This is how Hubitat names it
                 [attribute:"ColorMode",          value: {pv -> toInt(pv)}, descriptionText: {dn, dv ->  "${dn}: Color Mode as a Matter integer value ${dv}"}] // This is how Matter names it!
                ],
        0x400B:[[attribute:"ColorTemperatureMinKelvin",  value: {pv -> HexMiredsToKelvin(pv)}, descriptionText: {dn, dv ->  "${dn}: Minimum device color temperature supported is: ${dv} Kelvin"}, unit: "°K"]
                ],
        0x400C:[[attribute:"ColorTemperatureMaxKelvin",  value: {pv -> HexMiredsToKelvin(pv)}, descriptionText: {dn, dv ->  "${dn}: Maximum device color temperature supported is: ${dv} Kelvin"}, unit: "°K"]
                ],
        ],

    0x0400:[ // Illuminance Measurement
        0x0000:[ [attribute:"illuminance",             value:{pv -> HexToLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }], // This is the Hubitat name
                 [attribute:"MeasuredValue",           value:{pv -> HexToLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }], // This is the Matter name
               ],
        0x0001:[ [attribute:"MinMeasuredValueLux",     value:{pv -> HexToLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Minimum measurable illuminance ${dv} lx"}],  	],
        0x0002:[ [attribute:"MaxMeasuredValueLux",     value:{pv -> HexToLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Maximum measurable illuminance ${dv} lx"}],    	],
        0x0003:[ [attribute:"LuxMeasurementTolerance", value:{pv -> HexToLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measurement illuminance tolerance is ${dv} lx"}], 	],
        0x0004:[ [attribute:"LightSensorType",         value:{pv -> [0:"Photodiode", 1:"CMOS"].get(toInt(pv)) },isStateChange:false] ], 
        ],
    0x0402:[ // Temperature Measurement
        0x0000:[ [attribute:"illuminance",              value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
                 [attribute:"TempMeasuredValue",        value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
               ],
		0x0001:[ [attribute:"TempMinMeasuredValue",     value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Minimum measurable lx is ${dv}"}],  	],
        0x0002:[ [attribute:"TempMaxMeasuredValue",     value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Maximum measurable lx is ${dv}"}],    	],
        0x0003:[ [attribute:"TempTolerance", 		    value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measurement tolerance lx is ${dv}"}], 	],
    ],
    // 0x0403:[ // Pressure Measurement. Add if a supporting device comes to market for this!
    // 0x0404:[ // Flow Measurement. Add if a supporting device comes to market for this!
    0x0405:[ // Relative Humidty Measurement
        0x0000:[[attribute:"MeasuredValue",     value:{pv -> toInt(pv)/100}, units:"%", descriptionText:{dn, dv -> "${dn}: Measured humidity is ${dv}% RH"}],  ],
        0x0001:[[attribute:"MinMeasuredValue",  value:{pv -> toInt(pv)/100}, units:"%", descriptionText:{dn, dv -> "${dn}: Minimum measurable value is ${dv}% RH"}],  ],
        0x0002:[[attribute:"MaxMeasuredValue",  value:{pv -> toInt(pv)/100}, units:"%", descriptionText:{dn, dv -> "${dn}: Maximum measurable vlaue is ${dv}% RH"}],  ],
        0x0003:[[attribute:"Tolerance",         value:{pv -> toInt(pv)/100}, units:"%", descriptionText:{dn, dv -> "${dn}: Measurment tolerance is ${dv}% RH"}],  ],
    ],
    0x0406:[ // Occupancy Measurement
        0x0000:[[attribute:"motion",               		value:{pv -> toInt(pv) ? "active" : "inactive"}], 
                [attribute:"presence",             		value:{pv -> toInt(pv) ? "active" : "inactive"}],
                [attribute:"Occupancy",             	value:{pv -> toInt(pv)}, units:"bitmap", descriptionText:{dn, dv -> "${dn}: Occupancy bitmap: 0b${Integer.toBinaryString(dv).padLeft(8, "0")}"}]],
        0x0001:[[attribute:"OccupancySensorType",  		value:{pv -> [0:"PIR", 1:"Ultrasonic", 2:"PIRAndUltrasonic", 3:"PhysicalContact"].get(toInt(pv))}, descriptionText:{dn, dv -> "${dn}: Sensor type is: ${dv}"}] ],
        0x0001:[[attribute:"OccupancySensorTypeBitmap", value:{pv -> toInt(pv)}, descriptionText:{dn, dv -> "${dn}: Sensor type bitmap: 0b${Integer.toBinaryString(dv)}"}] ],
        0x0010:[[attribute:"PIROccupiedToUnoccupiedDelay", 					value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Occupied to Unoccupied delay is ${dv} seconds"}]],
        0x0011:[[attribute:"PIRUnoccupiedToOccupiedDelay", 					value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied delay is ${dv} seconds"}] ],
        0x0012:[[attribute:"PIRUnoccupiedToOccupiedThreshold", 				value:{pv -> toInt(pv)}, units:"events",  descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied threashold is ${dv} events"}]],
        0x0020:[[attribute:"UltrasonicOccupiedToUnoccupiedDelay", 			value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Occupied to Unoccupied delay is ${dv} seconds"}]],
        0x0031:[[attribute:"UltrasonicUnoccupiedToOccupiedDelay", 			value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied delay is ${dv} seconds"}] ],
        0x0032:[[attribute:"UltrasonicUnoccupiedToOccupiedThreshold", 		value:{pv -> toInt(pv)}, units:"events",  descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied threashold is ${dv} events"}]],
        0x0030:[[attribute:"PhysicalContactOccupiedToUnoccupiedDelay", 		value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Occupied to Unoccupied delay is ${dv} seconds"}]],
        0x0031:[[attribute:"PhysicalContactUnoccupiedToOccupiedDelay", 		value:{pv -> toInt(pv)}, units:"seconds", descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied delay is ${dv} seconds"}] ],
        0x0032:[[attribute:"PhysicalContactUnoccupiedToOccupiedThreshold", 	value:{pv -> toInt(pv)}, units:"events",  descriptionText:{dn, dv -> "${dn}: Unoccupied to Occupied threashold is ${dv} events"}]],
    ],
    // 0x0407:[ // Leaf Wetness Measurement. Add if a device comes to market for this!
    // 0x0408:[ // Soil Moisture Measurement. Add if a device comes to market for this!
    // 0x040C: concentrationMeasurementCluster, // CO
    // 0x040D: concentrationMeasurementCluster, // CO2
    // 0x0413: concentrationMeasurementCluster, // NO2
    // 0x0415: concentrationMeasurementCluster, // O3
    // 0x042A: concentrationMeasurementCluster, // PM2.5
    // 0x042B: concentrationMeasurementCluster, // Formaldehyde
    // 0x042C: concentrationMeasurementCluster, // PM1
    // 0x042D: concentrationMeasurementCluster, // PM10
    // 0x042E: concentrationMeasurementCluster, // TVOC
    // 0x042F: concentrationMeasurementCluster, // Radon (Rn)
    0x005B:[ // Air Quality
        0x0000:[[attribute:"AirQuality", value:{pv -> AirQualityEnumType.get(toInt(pv)) }, descriptionText:{dn, dv -> "${dn}: Air Quality is ${dv}"}], ],
    ],   
	0x005C:[ // Smoke and CO Alarm
        0x0000:[[attribute:"ExpressedState", 	value:{pv -> ExpressedStateEnum.get(toInt(pv))},                descriptionText:{dn, dv -> "${dn}: Expressed State is: ${dv}"}]],
        0x0001:[[attribute:"SmokeState", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}, 	                descriptionText:{dn, dv -> "${dn}: Smoke State is: ${dv}"}]],
        0x0002:[[attribute:"COState", 			value:{pv -> AlarmStateEnum.get(toInt(pv))}, 	                descriptionText:{dn, dv -> "${dn}: CO State is: ${dv}"}]],
        0x0003:[[attribute:"BatteryAlert", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}, 	                descriptionText:{dn, dv -> "${dn}: Battery Alert is: ${dv}"}]],
        0x0004:[[attribute:"DeviceMuted", 		value:{pv -> MuteStateEnum.get(toInt(pv))}, 	                descriptionText:{dn, dv -> "${dn}: Device is: ${dv}"}]],
        0x0005:[[attribute:"TestInProgress", 			value:{pv -> toInt(pv) as Boolean }, 		            descriptionText:{dn, dv -> "${dn}: Test In Progress is: ${dv}"}]],
        0x0006:[[attribute:"HardwareFaultAlert",		value:{pv -> toInt(pv) as Boolean }, 		            descriptionText:{dn, dv -> "${dn}: Hardware Fault is: ${dv}"}]],
        0x0007:[[attribute:"EndOfServiceAlert", 		value:{pv -> EndOfServiceEnum.get(toInt(pv))}, 	        descriptionText:{dn, dv -> "${dn}: Service Lifetime is: ${dv}"}]],
        0x0008:[[attribute:"InterconnectSmokeAlarm", 	value:{pv -> AlarmStateEnum.get(toInt(pv))}, 			descriptionText:{dn, dv -> "${dn}: Interconnect Smoke Alarm State is: ${dv}"}]],
        0x0009:[[attribute:"InterconnectCOAlarm", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}, 			descriptionText:{dn, dv -> "${dn}: Interconnect CO Alarm State State is: ${dv}"}]],
        0x000A:[[attribute:"ContaminationState", 		value:{pv -> ContaminationStateEnum.get(toInt(pv))},    descriptionText:{dn, dv -> "${dn}: Contamination State is: ${dv}"}]],
        0x000B:[[attribute:"SmokeSensitivityLevel", 	value:{pv -> SensitivityEnum.get(toInt(pv))}, 			descriptionText:{dn, dv -> "${dn}: Smoke Sensitivity Level is: ${dv}"}]],
        0x000C:[[attribute:"ExpiryDate", 				value:{pv -> pv}, units:"epoch-s", 			            descriptionText:{dn, dv -> "${dn}: Expiry date epoch-s is: ${dv}"}]],
    ],
    // Concentration Measurement Cluster (Matter Spec Section 2.10) are too complex and beyond this library
    0x130AFC01:[ // Eve Energy Custom Cluster
        0x130A0008:[[attribute:"voltage",             value:{pv -> toInt(pv) / 1000}, units:"V",                descriptionText:{dn, dv -> "${dn}: Voltage is: ${dv} Volts"}]],
        0x130A0009:[[attribute:"amperage",            value:{pv -> toInt(pv) / 1000}, units:"A",                descriptionText:{dn, dv -> "${dn}: Current is: ${dv} Amps"}]],
        0x130A000A:[[attribute:"power",               value:{pv -> toInt(pv) / 1000}, units:"W",                descriptionText:{dn, dv -> "${dn}: Watts: ${dv} watts"}]],
        0x130A000B:[[attribute:"EveWattsAccumulated", value:{pv -> toInt(pv)},                                  descriptionText:{dn, dv -> "${dn}: Watts Accumulated: ${dv}"} ]],
        0x130A000E:[[attribute:"EveWattAccumulatedControlPoint", value:{pv -> toInt(pv)},                       descriptionText:{dn, dv -> "${dn}: Eve Watts Control Point: ${dv}"}]]
   ],
]


List getHubitatEvents(Map descMap) {
    List rEvents = globalAllEventsMap.get(descMap.clusterInt)
		?.get(descMap.attrInt)
			?.collect{ Map rValue = [:]
						rValue << [name:it.attribute, value:it.value(descMap.value)] 
						rValue << ( it.units             ? [units:(it.units)]    : [:] )
						rValue << ( it.descriptionText   ? [descriptionText:it.descriptionText(device.displayName, rValue.value)] : [:])
						rValue << ( it.isStateChange     ? [isStateChange:true]  : [:] )
                        rValue << ( [clusterInt : (descMap.clusterInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device
                        rValue << ( [attrInt : (descMap.attrInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra attribute info for external device
                        rValue << ( [endpointInt : (Integer.parseInt(descMap.endpoint, 16))]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device

                      String rawDataAsJSON = new JsonBuilder(descMap.value).toString() // Event is sent on Hubitat's Event stream to external devices, so let's include original data in JSON form for external device
                      rValue << ( [rawValue: rawDataAsJSON ]) 
					}
    return rEvents
}

