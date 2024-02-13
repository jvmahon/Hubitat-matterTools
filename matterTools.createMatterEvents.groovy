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
@Field static Closure HexTempToName = {String pv -> ColorTempToName(toInt(pv))}
@Field static Closure HexMiredsToKelvin =  {String pv -> 1000000 / toInt(pv) as Integer}
@Field static Closure HexHueToName = {String pv -> HuePercent2Name( HexToPercent(pv) % 100 ) }
@Field static Closure HexToTemp = { String pv -> toInt(pv) / 100 } // This needs to be modified to account for negative numbers, but the standards are unclear!
@Field static Closure KelvinTempToName = {k ->
    if (k <= 2000) return "Sodium"
    if (k <= 2100) return "Starlight"
    if (k <= 23400) return "Sunrise"
    if (k <= 2800) return "Incandescent"
    if (k <= 3300) return "Soft White"
    if (k <= 3500) return "Warm white"
    if (k <= 4100) return "Moonlight"
    if (k <= 5000) return "Horizon"
    if (k <= 5500) return "Daylight"
    if (k <= 6000) return "Electronic"
    if (k <= 6500) return "Skylight"
    return "Polar"
}


/*
For Closure values in the following structure:
pv = parsed Map value field (descMap.value)
dn = device name - provided as a string
dv = device value - usually the content of the event map's "value" field after pv has been converted by the closure in the "value" field, below..
*/
@Field static Map globalAllEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events
    0x0003:[ // Identify Cluster
        0x0000:[[attribute:"IdentifyTime", value:{ pv -> toInt(pv)  },     units:"seconds"			]],
        0x0001:[[attribute:"IdentifyType", value:{ pv -> IdentifyTypeEnum.get(toInt(pv))  }			]],
    ],
    0x0006:[ // Switch Cluster
        0x0000:[[attribute:"switch",             value:{ pv -> toInt(pv) ? "on" : "off" }     		],
                [attribute:"OnOff",              value:{ pv -> toBool(pv) }                     	]],
        0x4000:[[attribute:"GlobalSceneControl", value:{ pv -> toBool(pv) }                     	]],
        0x4001:[[attribute:"OnTime",             value:{ pv -> toInt(pv) / 10 },  units:"seconds" 	]],
        0x4002:[[attribute:"OffWaitTime",        value:{ pv -> toInt(pv) / 10 },  units:"seconds" 	]],
        0x4003:[[attribute:"StartUpOnOff",       value:{ pv -> StartUpOnOffEnum.get(toInt(pv))}  	]],
    ],
    0x0008:[ // Level Cluster
        0x0000:[[attribute:"level",                value:{ pv -> HexToPercent(pv)},   units:"%"       	],
                [attribute:"CurrentLevel",         value:{ pv -> toInt(pv)}, 						  	]],
        0x0001:[[attribute:"RemainingTime",        value:{ pv -> toInt(pv) / 10 },    units:"seconds" 	]],
        0x0002:[[attribute:"MinLevel",             value:{ pv -> HexToPercent(pv) },  units:"%"       	]],
        0x0003:[[attribute:"MaxLevel",             value:{ pv -> HexToPercent(pv) },  units:"%"       	]],
        0x0004:[[attribute:"CurrentFrequency",     value:{ pv -> toInt(pv)},          units:"Hz"      	]],
        0x0005:[[attribute:"MinFrequency",         value:{ pv -> toInt(pv)},          units:"Hz"      	]],
        0x0006:[[attribute:"MaxFrequency",         value:{ pv -> toInt(pv)},          units:"Hz"      	]],
        0x0010:[[attribute:"OnOffTransitionTime",  value:{ pv -> toInt(pv) / 10 },    units:"seconds" 	]],
        0x0011:[[attribute:"OnLevel",              value:{ pv -> HexToPercent(pv)},   units:"%"       	]],
        0x0012:[[attribute:"OnTransitionTime",     value:{ pv -> toInt(pv) / 10 },    units:"seconds" 	]],
        0x0013:[[attribute:"OffTransitionTime",    value:{ pv -> toInt(pv) / 10 },    units:"seconds" 	]],
        0x0014:[[attribute:"DefaultMoveRate",      value:{ pv -> toInt(pv)},     						]],
        0x000F:[[attribute:"Options",              value:{ pv -> toInt(pv)},          units:"bitmap"  	]],
        0x4000:[[attribute:"StartUpCurrentLevel",  value:{ pv -> toInt(pv)},          units:"uint16"  	]],
    ],
    0x001D:[ // Descriptor - As of Feb. 2024, not parsed correctly by Hubitat, so best to ignore!
        0x0000:[[attribute:"DeviceTypeList",       value:{ pv -> pv }		]],
        0x0001:[[attribute:"ServerList",           value:{ pv -> pv }		]],
        0x0002:[[attribute:"ClientList",           value:{ pv -> pv }		]],
        0x0003:[[attribute:"PartsList",            value:{ pv -> pv }		]],
        0x0004:[[attribute:"TagList",              value:{ pv -> pv }		]],
        ],
    0x0028:[ // Basic Information
        0x0000:[[attribute:"DataModelRevision",       value:{ pv -> toInt(pv) }		]],
        0x0001:[[attribute:"VendorName",              value:{ pv -> pv }			]],
        0x0002:[[attribute:"VendorID",                value:{ pv -> toInt(pv) }		]],
        0x0003:[[attribute:"ProductName",             value:{ pv -> pv }			]],
        0x0004:[[attribute:"ProductID",               value:{ pv -> toInt(pv) }		]],
        0x0005:[[attribute:"NodeLabel",               value:{ pv -> pv }			]],
        0x0006:[[attribute:"Location",                value:{ pv -> pv }			]],
        0x0007:[[attribute:"HardwareVersion",         value:{ pv -> toInt(pv) }		]],
        0x0008:[[attribute:"HardwareVersionString",   value:{ pv -> pv }			]],
        0x0009:[[attribute:"SoftwareVersion",         value:{ pv -> toInt(pv) }		]],
        0x000A:[[attribute:"SoftwareVersionString",   value:{ pv -> pv }			]],
        0x000B:[[attribute:"ManufacturingDate",       value:{ pv -> pv }			]],
        0x000C:[[attribute:"PartNumber",              value:{ pv -> pv }			]],
        0x000D:[[attribute:"ProductURL",              value:{ pv -> pv }			]],
        0x000E:[[attribute:"ProductLabel",            value:{ pv -> pv }			]],
        0x000F:[[attribute:"SerialNumber",            value:{ pv -> pv }			]],
        0x0010:[[attribute:"LocalConfigDisabled",     value:{ pv -> toBool(pv)}		]],
        0x0011:[[attribute:"Reachable",               value:{ pv -> toBool(pv)}		]],
        0x0012:[[attribute:"UniqueID",                value:{ pv -> pv }			]],
        0x0013:[[attribute:"CapabilityMinima",        value:{ pv -> pv }			]],
        0x0014:[[attribute:"ProductAppearance",       value:{ pv -> pv }			]],
        ],
    0x002B:[ // Localization Configuration
        0x0000:[[attribute:"ActiveLocale",         value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Locale: ${dv}"}]],
        0x0000:[[attribute:"SupportedLocales",     value:{ pv -> pv },         descriptionText: {dn, dv ->  "${dn}: Supported Locales: ${dv}"}]],
        ],
    0x002D:[ // Unit Localization
        0x0000:[[attribute:"TemperatureUnit",         value:{ pv -> [0:"Fahrenheit", 1:"Celsius", 2:"Kelvin"].get(toInt(pv)) }, descriptionText: {dn, dv ->  "${dn}: Temperature Unit: ${dv}"}]],
        ],
    0x002F:[ // Power Source Cluster
        0x0000:[[attribute:"Status",						value:{ pv -> PowerSourceStatusEnum.get(toInt(pv))}	]],
        0x0001:[[attribute:"Order",							value:{ pv -> toInt(pv)}							]],
        0x0002:[[attribute:"Description",					value:{ pv -> pv}									]],
        0x0003:[[attribute:"WiredAssessedInputVoltage",		value:{ pv -> toInt(pv) / 1000},  units:"V"			]],
        0x0004:[[attribute:"WiredAssessedInputFrequency",	value:{ pv -> toInt(pv)},	units:"Hz"				]],
        0x0005:[[attribute:"WiredCurrentType",				value:{ pv -> WiredCurrentTypeEnum.get(toInt(pv))}	]],
        0x0006:[[attribute:"WiredAssessedCurrent",			value:{ pv -> toInt(pv) / 1000}, units:"A"			]],
        0x0007:[[attribute:"WiredNominalVoltage",			value:{ pv -> toInt(pv) / 1000}, units:"V"			]],
        0x0008:[[attribute:"WiredMaximumCurrent",           value:{ pv -> toInt(pv) / 1000}, units:"A"			]],
        0x0009:[[attribute:"WiredPresent",					value:{ pv -> toBool(pv) }							]],
        0x000A:[[attribute:"ActiveWiredFaults",				value:{ pv -> pv }									]],
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
        0x0013:[[attribute:"BatReplacementDescription",		value:{ pv -> toInt(pv) }, 								descriptionText: {dn, dv ->  "${dn}: Battery Replacement Description: ${dv}"}]],
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
        0x0000:[[attribute:"Channel",              value:{ pv -> toInt(pv)}							]],
        0x0001:[[attribute:"RoutingRole",          value:{ pv -> RoutingRoleEnum.get(toInt(pv))}	]],
        0x0002:[[attribute:"NetworkName",          value:{ pv -> pv}								]],
        0x0003:[[attribute:"PanId",                value:{ pv -> toInt(pv)}							]],
        0x0004:[[attribute:"ExtendedPanId",        value:{ pv -> toLong(pv)}						]],
        0x0005:[[attribute:"MeshLocalPrefix",      value:{ pv -> pv}								]],
        0x0006:[[attribute:"OverrunCount",         value:{ pv -> toLong(pv)}						]],
        0x0007:[[attribute:"NeighborTable",        value:{ pv -> pv},         units:"list"			]],
        0x0008:[[attribute:"RouteTable",           value:{ pv -> pv},         units:"list"			]],
        0x0009:[[attribute:"PartitionId",          value:{ pv -> toInt(pv) }						]],
        0x000A:[[attribute:"Weighting",            value:{ pv -> toInt(pv) }						]],
        0x000B:[[attribute:"DataVersion",          value:{ pv -> toInt(pv) }						]],
        0x000C:[[attribute:"StableDataVersion",    value:{ pv -> toInt(pv) }						]],
        0x000D:[[attribute:"LeaderRouterId",       value:{ pv -> toInt(pv) }						]],
        0x000E:[[attribute:"DetachedRoleCount",    value:{ pv -> toInt(pv) }						]],
        0x000F:[[attribute:"ChildRoleCount",       value:{ pv -> toInt(pv) }						]],
        
        0x0010:[[attribute:"RouterRoleCount",                    value:{ pv -> toInt(pv) }		]],
        0x0011:[[attribute:"LeaderRoleCount",                    value:{ pv -> toInt(pv) }		]],
        0x0012:[[attribute:"AttachedAttemptCount",               value:{ pv -> toInt(pv) }		]],
        0x0013:[[attribute:"PartitionIdChangeCount",             value:{ pv -> toInt(pv) }		]],
        0x0014:[[attribute:"BetterPartitionAttachAttemptCount",  value:{ pv -> toInt(pv) }		]],
        0x0015:[[attribute:"ParentChangeCount",                  value:{ pv -> toInt(pv) }		]],
        0x0016:[[attribute:"TxTotalCount",                       value:{ pv -> toInt(pv) }		]],
        0x0017:[[attribute:"TxUnicastCount",                     value:{ pv -> toInt(pv) }		]],
        0x0018:[[attribute:"TxBroadcastCount",                   value:{ pv -> toInt(pv) }		]],
        0x0019:[[attribute:"TxAckRequestedCount",                value:{ pv -> toInt(pv) }		]],
        0x001A:[[attribute:"TxAcked",                            value:{ pv -> toInt(pv) }		]],
        0x001B:[[attribute:"TxNoAckRequestedCount",              value:{ pv -> toInt(pv) }		]],
        0x001C:[[attribute:"TxDataCount",                        value:{ pv -> toInt(pv) }		]],
        0x001D:[[attribute:"TxDataPollCount",                    value:{ pv -> toInt(pv) }		]],
        0x001E:[[attribute:"TxBeaconCount",                      value:{ pv -> toInt(pv) }		]],
        0x001F:[[attribute:"TxBeaconRequestCount",               value:{ pv -> toInt(pv) }		]],
        
        0x0020:[[attribute:"TxOtherCount",                  value:{ pv -> toInt(pv) }	]],
        0x0021:[[attribute:"TxRetryCount",                  value:{ pv -> toInt(pv) }	]],
        0x0022:[[attribute:"TxDirectMaxRetryExpiryCount",   value:{ pv -> toInt(pv) } 	]],
        0x0023:[[attribute:"TxIndirectMaxRetryExpiryCount", value:{ pv -> toInt(pv) } 	]],
        0x0024:[[attribute:"TxErrCcaCount",                 value:{ pv -> toInt(pv) } 	]],
        0x0025:[[attribute:"TxErrAbortCount",               value:{ pv -> toInt(pv) } 	]],
        0x0026:[[attribute:"TxErrBusyChannelCount",         value:{ pv -> toInt(pv) } 	]],
        0x0027:[[attribute:"RxTotalCount",                  value:{ pv -> toInt(pv) } 	]],
        0x0028:[[attribute:"RxUnicastCount",                value:{ pv -> toInt(pv) } 	]],
        0x0029:[[attribute:"RxBroadcastCount",              value:{ pv -> toInt(pv) } 	]],
        0x002A:[[attribute:"RxDataCount",                   value:{ pv -> toInt(pv) } 	]],
        0x002B:[[attribute:"RxDataPollCount",               value:{ pv -> toInt(pv) } 	]],
        0x002C:[[attribute:"RxBeaconCount",                 value:{ pv -> toInt(pv) } 	]],
        0x002D:[[attribute:"RxBeaconRequestCount",          value:{ pv -> toInt(pv) } 	]],
        0x002E:[[attribute:"RxOtherCount",                  value:{ pv -> toInt(pv) } 	]],
        0x002F:[[attribute:"RxAddressFilteredCount",        value:{ pv -> toInt(pv) } 	]],
        
        0x0030:[[attribute:"RxDestAddrFilteredCount",       value:{ pv -> toInt(pv) }	]],
        0x0031:[[attribute:"RxDuplicatedCount",             value:{ pv -> toInt(pv) }	]],
        0x0032:[[attribute:"RxErrNoFrameCount",             value:{ pv -> toInt(pv) }	]],
        0x0033:[[attribute:"RxErrUnknownNeighborCount",     value:{ pv -> toInt(pv) }	]],
        0x0034:[[attribute:"RxErrInvalidSrcAddrCount",      value:{ pv -> toInt(pv) }	]],
        0x0035:[[attribute:"RxErrSecCount",                 value:{ pv -> toInt(pv) }	]],
        0x0036:[[attribute:"RxErrFcsCount",                 value:{ pv -> toInt(pv) }	]],
        0x0037:[[attribute:"RxErrOtherCount",               value:{ pv -> toInt(pv) }	]],
        0x0038:[[attribute:"ActiveTimestamp",               value:{ pv -> toInt(pv) }	]],
        0x0039:[[attribute:"PendingTimestamp",              value:{ pv -> toInt(pv) }	]],
        0x003A:[[attribute:"Delay",                         value:{ pv -> toInt(pv) }		]],
        // 0x003B:[[attribute:"SecurityPolicy",                value:{ pv -> toInt(pv) }	]],
        // 0x003C:[[attribute:"ChannelPage0Mask",              value:{ pv -> toInt(pv) }	]],
        // 0x003D:[[attribute:"OperationalDatasetComponents",  value:{ pv -> toInt(pv) }	]],
        // 0x003E:[[attribute:"ActiveNEtworkFaults",           value:{ pv -> toInt(pv) }	]],
        ],
    0x0036:[ // WiFi Diagnostics
        0x0000:[[attribute:"BSSID",                   value:{ pv -> pv}									]],
        0x0001:[[attribute:"SecurityType",            value:{ pv -> SecurityTypeEnum.get(toInt(pv))}	]],
        0x0002:[[attribute:"WiFiVersion",             value:{ pv -> WiFiVersionEnum.get(toInt(pv))}		]],
        0x0003:[[attribute:"ChannelNumber",           value:{ pv -> toInt(pv)}			]],
        0x0004:[[attribute:"RSSI",                    value:{ pv -> toInt(pv) - 256}	]],
        0x0005:[[attribute:"BeaconLostCount",         value:{ pv -> toInt(pv)}			]],
        0x0006:[[attribute:"BeaconRxCount",           value:{ pv -> toInt(pv)}			]],
        0x0007:[[attribute:"PacketMulticastRxCount",  value:{ pv -> toInt(pv)}			]],
        0x0008:[[attribute:"PacketMulticastTxCount",  value:{ pv -> toInt(pv)}			]],
        0x0009:[[attribute:"PacketUnicastRxCount",    value:{ pv -> toInt(pv)}			]],
        0x000A:[[attribute:"PacketUnicastTxCount",    value:{ pv -> toInt(pv)}			]],
        0x000B:[[attribute:"CurrentMaxRate",          value:{ pv -> toLong(pv)}			]],
        0x000C:[[attribute:"OverrunCount",            value:{ pv -> toLong(pv)}			]],
        ],
    0x003B:[ // Boolean State
        0x0000:[[attribute:"StateVale",    value:{ pv -> toBool(pv)}, descriptionText: {dn, dv ->  "${dn}: Boolean State ${dv}"}],
                [attribute:"contact",    value:{ pv -> toInt(pv) ? "closed" : "open"}, descriptionText: {dn, dv ->  "${dn}: Contact Sensor State is: ${dv}"}]], //
        ],
    0x0045:[ // Generic Switch Cluster
        0x0000:[[attribute:"NumberOfPositions",    value:{ pv -> toInt(pv)}		]], // Number of Plsitions
        0x0001:[[attribute:"CurrentPosition",      value:{ pv -> toInt(pv)}		]], // Current Position
        0x0002:[[attribute:"MultiPressMax",        value:{ pv -> toInt(pv)}		]], // Multi-Press Max
        ],
    0x0300:[ // Color Control Cluster.  Only covering the most common ones for Hue at the moment!
		0x0000:[ // Hue
			    [attribute:"hue",                       value:{pv -> HexToPercent(pv)}, units:"%" ], 	//  This is the Hubitat name/value
			    [attribute:"colorName",                 value:{pv -> HexHueToName(pv) } ],           	// generate color name when hue is reported
			    [attribute:"CurrentHue",                value:{pv -> toInt(pv)}, units:"uint8" ],     	// This is the Matter name / value
                [attribute:"colorTemperature",          value: null] 									// Nullify colorTemperature if you sent a RGB color
               ],
        0x0001:[[attribute:"saturation",                value: {pv -> HexToPercent(pv)}, units:"%" ],  	//  This is the Hubitat name/value
                [attribute:"CurrentSaturation",         value: {pv -> toInt(pv)}, units:"uint8" ]      	// This is the Matter name / value
               ],
        0x0002:[[attribute:"RemainingTime",             value: {pv -> toInt(pv)/10}, units:"seconds" ]],
        0x0007:[[attribute:"colorTemperature",          value: {pv -> HexMiredsToKelvin(pv)}, , unit: "°K"], 
                [attribute:"ColorTemperatureMireds",    value: {pv -> toInt(pv)},  unit: "Mireds"],
                [attribute:"colorName",                 value: {pv -> KelvinTempToName(HexMiredsToKelvin(pv)) } ],
                [attribute:"hue",                       value: null ],
                [attribute:"saturation",                value: null ],
               ],
        0x0008:[[attribute:"colorMode",                 value: {pv -> [0:"RGB", 1:"CurrentXY", 2:"CT"].get(toInt(pv))} ], // This is how Hubitat names it
                [attribute:"ColorMode",                 value: {pv -> toInt(pv)} ] // This is how Matter names it!
               ],
        0x000F:[[attribute:"Options",                   value: { pv ->toInt(pv) }] ],
        0x400A:[[attribute:"ColorCapabilities",         value: { pv -> List capability = []; 
                                                                log.debug "Determining a color capability";
                                                                if (toInt(pv) & 0b0000_0001) capability << "HS"; 
                                                                if (toInt(pv) & 0b0000_0010) capability << "EHUE"; 
                                                                if (toInt(pv) & 0b0000_0100) capability << "CL"; 
                                                                if (toInt(pv) & 0b0000_1000) capability << "XY";  
                                                                if (toInt(pv) & 0b0001_0000) capability << "CT"; 
                                                                return capability
                                                      } ]
               ],
        0x400B:[[attribute:"ColorTemperatureMinKelvin",  value: {pv -> HexMiredsToKelvin(pv)}, unit: "°K"] ],
        0x400C:[[attribute:"ColorTemperatureMaxKelvin",  value: {pv -> HexMiredsToKelvin(pv)}, unit: "°K"] ],
        ],
    0x0400:[ // Illuminance Measurement
        0x0000:[ [attribute:"illuminance",             value:{pv -> HexToLux(pv)}, units:"lx"	], // This is the Hubitat name
                 [attribute:"MeasuredValue",           value:{pv -> HexToLux(pv)}, units:"lx"	], // This is the Matter name
               ],
        0x0001:[ [attribute:"MinMeasuredValueLux",     value:{pv -> HexToLux(pv)}, units:"lx"	]],
        0x0002:[ [attribute:"MaxMeasuredValueLux",     value:{pv -> HexToLux(pv)}, units:"lx"	]],
        0x0003:[ [attribute:"LuxMeasurementTolerance", value:{pv -> HexToLux(pv)}, units:"lx"	]],
        0x0004:[ [attribute:"LightSensorType",         value:{pv -> [0:"Photodiode", 1:"CMOS"].get(toInt(pv)) } 	]], 
        ],
    0x0402:[ // Temperature Measurement
        0x0000:[ [attribute:"temperature",              value:{pv -> HexToTemp(pv)}, units:"°C"			],
                 [attribute:"TempMeasuredValue",        value:{pv -> HexToTemp(pv) / 100}, units:"°C"	],
               ],
		0x0001:[ [attribute:"TempMinMeasuredValue",     value:{pv -> HexToTemp(pv) / 100}, units:"°C"	]],
        0x0002:[ [attribute:"TempMaxMeasuredValue",     value:{pv -> HexToTemp(pv) / 100}, units:"°C"	]],
        0x0003:[ [attribute:"TempTolerance", 		    value:{pv -> toInt(pv) / 100}, units:"°C"		]],
    ],
    // 0x0403:[ // Pressure Measurement. Add if a supporting device comes to market for this!
    // 0x0404:[ // Flow Measurement. Add if a supporting device comes to market for this!
    0x0405:[ // Relative Humidty Measurement
        0x0000:[[attribute:"MeasuredValue",     value:{pv -> toInt(pv)/100}, units:"%" 	]],
        0x0001:[[attribute:"MinMeasuredValue",  value:{pv -> toInt(pv)/100}, units:"%" 	]],
        0x0002:[[attribute:"MaxMeasuredValue",  value:{pv -> toInt(pv)/100}, units:"%" 	]],
        0x0003:[[attribute:"Tolerance",         value:{pv -> toInt(pv)/100}, units:"%"	]],
    ],
    0x0406:[ // Occupancy Measurement
        0x0000:[[attribute:"motion",               		value:{pv -> toInt(pv) ? "active" : "inactive"}		], 
                [attribute:"presence",             		value:{pv -> toInt(pv) ? "active" : "inactive"}		],
                [attribute:"Occupancy",             	value:{pv -> toInt(pv)}, units:"bitmap"				]],
        0x0001:[[attribute:"OccupancySensorType",  		value:{pv -> [0:"PIR", 1:"Ultrasonic", 2:"PIRAndUltrasonic", 3:"PhysicalContact"].get(toInt(pv))}	]],
        0x0001:[[attribute:"OccupancySensorTypeBitmap", value:{pv -> toInt(pv)}		]],
        0x0010:[[attribute:"PIROccupiedToUnoccupiedDelay", 					value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0011:[[attribute:"PIRUnoccupiedToOccupiedDelay", 					value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0012:[[attribute:"PIRUnoccupiedToOccupiedThreshold", 				value:{pv -> toInt(pv)}, units:"events"		]],
        0x0020:[[attribute:"UltrasonicOccupiedToUnoccupiedDelay", 			value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0031:[[attribute:"UltrasonicUnoccupiedToOccupiedDelay", 			value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0032:[[attribute:"UltrasonicUnoccupiedToOccupiedThreshold", 		value:{pv -> toInt(pv)}, units:"events"		]],
        0x0030:[[attribute:"PhysicalContactOccupiedToUnoccupiedDelay", 		value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0031:[[attribute:"PhysicalContactUnoccupiedToOccupiedDelay", 		value:{pv -> toInt(pv)}, units:"seconds"	]],
        0x0032:[[attribute:"PhysicalContactUnoccupiedToOccupiedThreshold", 	value:{pv -> toInt(pv)}, units:"events"		]],
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
        0x0000:[[attribute:"AirQuality", value:{pv -> AirQualityEnumType.get(toInt(pv)) }	]],
    ],   
	0x005C:[ // Smoke and CO Alarm
        0x0000:[[attribute:"ExpressedState", 	value:{pv -> ExpressedStateEnum.get(toInt(pv))}			]],
        0x0001:[[attribute:"SmokeState", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}				]],
        0x0002:[[attribute:"COState", 			value:{pv -> AlarmStateEnum.get(toInt(pv))}				]],
        0x0003:[[attribute:"BatteryAlert", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}				]],
        0x0004:[[attribute:"DeviceMuted", 		value:{pv -> MuteStateEnum.get(toInt(pv))}				]],
        0x0005:[[attribute:"TestInProgress", 			value:{pv -> toInt(pv) as Boolean }				]],
        0x0006:[[attribute:"HardwareFaultAlert",		value:{pv -> toInt(pv) as Boolean }				]],
        0x0007:[[attribute:"EndOfServiceAlert", 		value:{pv -> EndOfServiceEnum.get(toInt(pv))}	]],
        0x0008:[[attribute:"InterconnectSmokeAlarm", 	value:{pv -> AlarmStateEnum.get(toInt(pv))}			]],
        0x0009:[[attribute:"InterconnectCOAlarm", 		value:{pv -> AlarmStateEnum.get(toInt(pv))}			]],
        0x000A:[[attribute:"ContaminationState", 		value:{pv -> ContaminationStateEnum.get(toInt(pv))}	]],
        0x000B:[[attribute:"SmokeSensitivityLevel", 	value:{pv -> SensitivityEnum.get(toInt(pv))}		]],
        0x000C:[[attribute:"ExpiryDate", 				value:{pv -> pv}, units:"epoch-s"					]],
    ],
    // Concentration Measurement Cluster (Matter Spec Section 2.10) are too complex and beyond this library
    0x130AFC01:[ // Eve Energy Custom Cluster
        0x130A0008:[[attribute:"voltage",             value:{pv -> toInt(pv) / 1000}, units:"V"		]],
        0x130A0009:[[attribute:"amperage",            value:{pv -> toInt(pv) / 1000}, units:"A"		]],
        0x130A000A:[[attribute:"power",               value:{pv -> toInt(pv) / 1000}, units:"W"		]],
        0x130A000B:[[attribute:"EveWattsAccumulated", value:{pv -> toInt(pv)}						]],
        0x130A000E:[[attribute:"EveWattAccumulatedControlPoint", value:{pv -> toInt(pv)}			]]
   ],
]

List getHubitatEvents(Map descMap) {
    try {
        List rEvents = globalAllEventsMap.get(descMap.clusterInt)
		    ?.get(descMap.attrInt)
			    ?.collect{ Map rValue = [:]
                        if (it.value instanceof Closure) {
                              rValue << [name:it.attribute, value:it.value(descMap.value)] 
                        } else {
                              rValue << [name:it.attribute, value: it.value] 
                        }
                          
                          
						rValue << ( it.units             ? [units:(it.units)]    : [:] )
						// rValue << ( it.descriptionText   ? [descriptionText:it.descriptionText(device.displayName, rValue.value)] : [:])
                          if (it.descriptionText && (it.descriptionText instanceof Closure) && false ) {
                              // For the moment, a 'false' is included in if test to force the else statement.
                              // To be implemented - if a closure is there, apply that to customize the description
                          } else {
                                String newDescription = "${rValue.name} attribute set to ${rValue.value}"
                                if (it.units) { newDescription = newDescription + " " + it.units }
                          }
                        rValue << ( [descriptionText:newDescription])
						rValue << ( it.isStateChange     ? [isStateChange:true]  : [:] )
                        rValue << ( [clusterInt : (descMap.clusterInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device
                        rValue << ( [attrInt : (descMap.attrInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra attribute info for external device
                        rValue << ( [endpointInt : (Integer.parseInt(descMap.endpoint, 16))]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device

                      String rawDataAsJSON = new JsonBuilder(descMap.value).toString() // Event is sent on Hubitat's Event stream to external devices, so let's include original data in JSON form for external device
                      rValue << ( [rawValue: rawDataAsJSON ]) 
					}
        return rEvents
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

