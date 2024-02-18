/*
Requires importing matterTools.MatterEnumTypes
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Create Hubitat Events from Matter Attribute Data",
        name: "createListOfMatterSendEventMaps",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import java.lang.Math
import groovy.transform.Field
import groovy.json.JsonBuilder
import org.apache.commons.lang3.StringUtils


// //////////////////////

@Field static Map RoutingRoleEnum = [0:"Unspecified", 1:"Unassigned", 2:"SleepyEndDevice", 3:"EndDevice", 4:"REED", 5:"Router", 6:"Leader"]

@Field static Closure toInt =    { Integer.parseInt(it, 16)}           // Hex to Integer conversion, unsigned
@Field static Closure toTenths = { Integer.parseInt(it, 16) / 10}      // Hex to .1 conversion.
@Field static Closure toCenti =  { Integer.parseInt(it, 16) / 100}     // Hex to .01 conversion.
@Field static Closure toMilli =  { Integer.parseInt(it, 16) / 1000}    // Hex to .001 conversion.

@Field static Closure toBool = { Integer.parseInt(it, 16) ? true : false} // Hex to Integer conversion.
@Field static Closure toLong = { Long.parseLong(it, 16) } // Hex to Long

@Field static Closure HexToPercent = { Math.round(Integer.parseInt(it, 16) / 2.54) as Integer}

@Field static Closure HexToLux =          { Math.pow( 10, (Integer.parseInt(it, 16) -1) / 10000)  as Integer} // convert Matter value to illumination in lx. See Matter Cluster Spec Section 2.2.5.1
@Field static Closure HuePercent2Name =   { colorRGBName.find({ entry -> entry.key.contains(it as Integer)}).value}
@Field static Closure HexTempToName =     { ColorTempToName( Integer.parseInt(it, 16) )}
@Field static Closure HexMiredsToKelvin = { (1000000 / Integer.parseInt(it, 16)) as Integer}
@Field static Closure HexHueToName =      { HuePercent2Name( HexToPercent(it) % 100 ) }
// @Field static Closure HexHueToName = {this.convertHueToGenericColorName(Integer.parseInt(it, 16)) }
@Field static Closure HexToTemp =         { Integer.parseInt(it, 16)  / 100 } // This needs to be modified to account for negative numbers, but the standards are unclear!
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
        0x0000:[[attribute:"IdentifyTime", valueTransform: this.&toInt,     units:"seconds"				]],
        0x0001:[[attribute:"IdentifyType", valueTransform: { IdentifyTypeEnum.get(toInt(it))  }			]],
    ],
    0x0006:[ // Switch Cluster
        0x0000:[[attribute:"switch",             valueTransform: { toInt(it) ? "on" : "off" }     		],
                [attribute:"OnOff",              valueTransform: this.&toBool                          	]],
        0x4000:[[attribute:"GlobalSceneControl", valueTransform: this.&toBool                     		]],
        0x4001:[[attribute:"OnTime",             valueTransform: this.&toTenths,  units:"seconds" 		]],
        0x4002:[[attribute:"OffWaitTime",        valueTransform: this.&toTenths,  units:"seconds" 		]],
        0x4003:[[attribute:"StartUpOnOff",       valueTransform: { StartUpOnOffEnum.get(toInt(it))}  	]],
    ],
    0x0008:[ // Level Cluster
        0x0000:[[attribute:"level",                valueTransform: this.&HexToPercent, 	    units:"%"       	],
                [attribute:"CurrentLevel",         valueTransform: this.&toInt,  					  			]],
        0x0001:[[attribute:"RemainingTime",        valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0002:[[attribute:"MinLevel",             valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0003:[[attribute:"MaxLevel",             valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0004:[[attribute:"CurrentFrequency",     valueTransform: this.&toInt,         	units:"Hz"      	]],
        0x0005:[[attribute:"MinFrequency",         valueTransform: this.&toInt,         	units:"Hz"      	]],
        0x0006:[[attribute:"MaxFrequency",         valueTransform: this.&toInt,         	units:"Hz"      	]],
        0x0010:[[attribute:"OnOffTransitionTime",  valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0011:[[attribute:"OnLevel",              valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0012:[[attribute:"OnTransitionTime",     valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0013:[[attribute:"OffTransitionTime",    valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0014:[[attribute:"DefaultMoveRate",      valueTransform: this.&toInt,     						]],
        0x000F:[[attribute:"Options",              valueTransform: this.&toInt,          units:"bitmap"  	]],
        0x4000:[[attribute:"StartUpCurrentLevel",  valueTransform: this.&toInt,          units:"uint16"  	]],
    ],
    0x001D:[ // Descriptor - As of Feb. 2024, not parsed correctly by Hubitat, so best to ignore!
        0x0000:[[attribute:"DeviceTypeList",       valueTransform: { it }		]],
        0x0001:[[attribute:"ServerList",           valueTransform: { it }		]],
        0x0002:[[attribute:"ClientList",           valueTransform: { it }		]],
        0x0003:[[attribute:"PartsList",            valueTransform: { it }		]],
        0x0004:[[attribute:"TagList",              valueTransform: { it }		]],
        ],
    0x0028:[ // Basic Information
        0x0000:[[attribute:"DataModelRevision",       valueTransform: this.&toInt		]],
        0x0001:[[attribute:"VendorName",              valueTransform: { it }			]],
        0x0002:[[attribute:"VendorID",                valueTransform: this.&toInt		]],
        0x0003:[[attribute:"ProductName",             valueTransform: { it }			]],
        0x0004:[[attribute:"ProductID",               valueTransform: this.&toInt		]],
        0x0005:[[attribute:"NodeLabel",               valueTransform: { it }			]],
        0x0006:[[attribute:"Location",                valueTransform: { it }			]],
        0x0007:[[attribute:"HardwareVersion",         valueTransform: this.&toInt		]],
        0x0008:[[attribute:"HardwareVersionString",   valueTransform: { it }			]],
        0x0009:[[attribute:"SoftwareVersion",         valueTransform: this.&toInt		]],
        0x000A:[[attribute:"SoftwareVersionString",   valueTransform: { it }			]],
        0x000B:[[attribute:"ManufacturingDate",       valueTransform: { it }			]],
        0x000C:[[attribute:"PartNumber",              valueTransform: { it }			]],
        0x000D:[[attribute:"ProductURL",              valueTransform: { it }			]],
        0x000E:[[attribute:"ProductLabel",            valueTransform: { it }			]],
        0x000F:[[attribute:"SerialNumber",            valueTransform: { it }			]],
        0x0010:[[attribute:"LocalConfigDisabled",     valueTransform: this.&toBool		]],
        0x0011:[[attribute:"Reachable",               valueTransform: this.&toBool		]],
        0x0012:[[attribute:"UniqueID",                valueTransform: { it }			]],
        0x0013:[[attribute:"CapabilityMinima",        valueTransform: { it }			]],
        0x0014:[[attribute:"ProductAppearance",       valueTransform: { it }			]],
        ],
    0x002B:[ // Localization Configuration
        0x0000:[[attribute:"ActiveLocale",         valueTransform: { it }		]],
        0x0000:[[attribute:"SupportedLocales",     valueTransform: { it }		]],
        ],
    0x002D:[ // Unit Localization
        0x0000:[[attribute:"TemperatureUnit",         valueTransform: { [0:"Fahrenheit", 1:"Celsius", 2:"Kelvin"].get(toInt(it)) }	]],
        ],
    0x002F:[ // Power Source Cluster
        0x0000:[[attribute:"Status",						valueTransform: { PowerSourceStatusEnum.get(toInt(it))}	]],
        0x0001:[[attribute:"Order",							valueTransform: this.&toInt							    ]],
        0x0002:[[attribute:"Description",					valueTransform: { it }									]],
        0x0003:[[attribute:"WiredAssessedInputVoltage",		valueTransform: this.&toMilli,  units:"V"			    ]],
        0x0004:[[attribute:"WiredAssessedInputFrequency",	valueTransform: this.&toInt,	units:"Hz"				]],
        0x0005:[[attribute:"WiredCurrentType",				valueTransform: { WiredCurrentTypeEnum.get(toInt(it))}	]],
        0x0006:[[attribute:"WiredAssessedCurrent",			valueTransform: this.&toMilli, units:"A"			    ]],
        0x0007:[[attribute:"WiredNominalVoltage",			valueTransform: this.&toMilli, units:"V"			    ]],
        0x0008:[[attribute:"WiredMaximumCurrent",           valueTransform: this.&toMilli, units:"A"			    ]],
        0x0009:[[attribute:"WiredPresent",					valueTransform: this.&toBool     						]],
        0x000A:[[attribute:"ActiveWiredFaults",				valueTransform: { it }									]],
        0x000B:[[attribute:"BatVoltage",					valueTransform: this.&toMilli, units:"V", 				descriptionText: {"Battery Voltage is: ${it}"}                  ]],
        0x000C:[[attribute:"BatPercentRemaining",    		valueTransform: { toInt(it) / 2 }, units:"%", 			descriptionText: {"Battery Percent Remaining is: ${it}"}        ],
				[attribute:"battery",    					valueTransform: { toInt(it) / 2 }, units:"%", 			descriptionText: {"Battery Percent Remaining is: ${it}"}        ]
				],
        0x000D:[[attribute:"BatTimeRemaining",       		valueTransform: this.&toInt, units:"seconds", 			descriptionText: {"Battery Time Remaining is: ${it}"}           ]],
        0x000E:[[attribute:"BatChargeLevel",    			valueTransform: { BatChargeLevelEnum.get(toInt(it)) }, 	descriptionText: {"Battery Charge Level is: ${it}"}             ]],
        0x000F:[[attribute:"BatReplacementNeeded",       	valueTransform: this.&toBool,      						descriptionText: {"Battery Replacement Needed: ${it}"}          ]],
        0x0010:[[attribute:"BatReplaceability",				valueTransform: { BatReplaceabilityEnum.get(toInt(it))}, descriptionText: {"Battery Replaceability is: ${it}"}          ]],
        0x0011:[[attribute:"BatPresent",                    valueTransform: this.&toBool,      						descriptionText: {"Battery Present: ${it}"}                     ]],
        0x0012:[[attribute:"ActiveBatFaults",               valueTransform: { it }, 								descriptionText: {"Active Battery Faults are: ${it}"}           ]],
        0x0013:[[attribute:"BatReplacementDescription",		valueTransform: this.&toInt, 							descriptionText: {"Battery Replacement Description: ${it}"}     ]],
        0x0014:[[attribute:"BatCommonDesignation",  		valueTransform: { BatCommonDesignationEnum.get(toInt(it))}, descriptionText: {"Battery Common  Designation: ${it}"}     ]],
        0x0015:[[attribute:"BatANSIDesignation",			valueTransform: { it }, 								descriptionText: {"Battery ANSI C18 Designation: ${it}"}        ]],
        0x0016:[[attribute:"BatIECDesignation",             valueTransform: { it }, 								descriptionText: {"Battery IEC 60086 Designation: ${it}"}       ]],
        0x0017:[[attribute:"BatApprovedChemistry",      	valueTransform: { BatApprovedChemistryEnum.get(toInt(it))}, descriptionText: {"Battery Approved Chemistry: ${it}"}      ]],
        0x0018:[[attribute:"BatCapacity",                   valueTransform: this.&toInt, units:"mAh", 				descriptionText: {"Battery Capacity: ${it} mAH"}                ]],
        0x0019:[[attribute:"BatQuantity",                	valueTransform: this.&toInt, 							descriptionText: {"Battery Quantity: ${it}"}                    ]],
        0x001A:[[attribute:"BatChargeState",                valueTransform: { BatChargeState.get(toInt(it)) }, 		descriptionText: {"Battery Charge State: ${it}"}                ]],
        0x001B:[[attribute:"BatTimeToFullCharge",           valueTransform: this.&toInt, units:"seconds", 			descriptionText: {"Battery Time To Full Charge: ${it} seconds"} ]],
        0x001C:[[attribute:"BatFunctionalWhileCharging",    valueTransform: this.&toBool,      						descriptionText: {"Battery Functional While Charging: ${it}"}   ]],
        0x001D:[[attribute:"BatChargingCurrent",            valueTransform: this.&toInt, 							descriptionText: {"Battery Charging Current: ${it}"}            ]],
        0x001E:[[attribute:"ActiveBatChargeFaults",         valueTransform: { it }, 								descriptionText: {"Active Battery Charge Faults: ${it}"}        ]],
        0x001F:[[attribute:"EndpointList",               	valueTransform: { it }, 								descriptionText: {"Power Source Endpoint List: ${it}"}          ]],
        ],

    0x0035:[ // Thread Diagnostics
        0x0000:[[attribute:"Channel",              valueTransform: this.&toInt						]],
        0x0001:[[attribute:"RoutingRole",          valueTransform: { RoutingRoleEnum.get(toInt(it))}	]],
        0x0002:[[attribute:"NetworkName",          valueTransform: { it }								]],
        0x0003:[[attribute:"PanId",                valueTransform: this.&toInt					    ]],
        0x0004:[[attribute:"ExtendedPanId",        valueTransform: this.&toLong						]],
        0x0005:[[attribute:"MeshLocalPrefix",      valueTransform: { it }								]],
        0x0006:[[attribute:"OverrunCount",         valueTransform: this.&toLong						]],
        0x0007:[[attribute:"NeighborTable",        valueTransform: { it },         			        ]],
        0x0008:[[attribute:"RouteTable",           valueTransform: { it },                 			]],
        0x0009:[[attribute:"PartitionId",          valueTransform: this.&toInt						]],
        0x000A:[[attribute:"Weighting",            valueTransform: this.&toInt						]],
        0x000B:[[attribute:"DataVersion",          valueTransform: this.&toInt						]],
        0x000C:[[attribute:"StableDataVersion",    valueTransform: this.&toInt						]],
        0x000D:[[attribute:"LeaderRouterId",       valueTransform: this.&toInt						]],
        0x000E:[[attribute:"DetachedRoleCount",    valueTransform: this.&toInt						]],
        0x000F:[[attribute:"ChildRoleCount",       valueTransform: this.&toInt						]],
        
        0x0010:[[attribute:"RouterRoleCount",                    valueTransform: this.&toInt		]],
        0x0011:[[attribute:"LeaderRoleCount",                    valueTransform: this.&toInt		]],
        0x0012:[[attribute:"AttachedAttemptCount",               valueTransform: this.&toInt		]],
        0x0013:[[attribute:"PartitionIdChangeCount",             valueTransform: this.&toInt		]],
        0x0014:[[attribute:"BetterPartitionAttachAttemptCount",  valueTransform: this.&toInt		]],
        0x0015:[[attribute:"ParentChangeCount",                  valueTransform: this.&toInt		]],
        0x0016:[[attribute:"TxTotalCount",                       valueTransform: this.&toInt		]],
        0x0017:[[attribute:"TxUnicastCount",                     valueTransform: this.&toInt		]],
        0x0018:[[attribute:"TxBroadcastCount",                   valueTransform: this.&toInt		]],
        0x0019:[[attribute:"TxAckRequestedCount",                valueTransform: this.&toInt		]],
        0x001A:[[attribute:"TxAcked",                            valueTransform: this.&toInt		]],
        0x001B:[[attribute:"TxNoAckRequestedCount",              valueTransform: this.&toInt		]],
        0x001C:[[attribute:"TxDataCount",                        valueTransform: this.&toInt		]],
        0x001D:[[attribute:"TxDataPollCount",                    valueTransform: this.&toInt		]],
        0x001E:[[attribute:"TxBeaconCount",                      valueTransform: this.&toInt		]],
        0x001F:[[attribute:"TxBeaconRequestCount",               valueTransform: this.&toInt		]],
        
        0x0020:[[attribute:"TxOtherCount",                  valueTransform: this.&toInt		]],
        0x0021:[[attribute:"TxRetryCount",                  valueTransform: this.&toInt		]],
        0x0022:[[attribute:"TxDirectMaxRetryExpiryCount",   valueTransform: this.&toInt 	]],
        0x0023:[[attribute:"TxIndirectMaxRetryExpiryCount", valueTransform: this.&toInt 	]],
        0x0024:[[attribute:"TxErrCcaCount",                 valueTransform: this.&toInt 	]],
        0x0025:[[attribute:"TxErrAbortCount",               valueTransform: this.&toInt 	]],
        0x0026:[[attribute:"TxErrBusyChannelCount",         valueTransform: this.&toInt 	]],
        0x0027:[[attribute:"RxTotalCount",                  valueTransform: this.&toInt 	]],
        0x0028:[[attribute:"RxUnicastCount",                valueTransform: this.&toInt 	]],
        0x0029:[[attribute:"RxBroadcastCount",              valueTransform: this.&toInt 	]],
        0x002A:[[attribute:"RxDataCount",                   valueTransform: this.&toInt 	]],
        0x002B:[[attribute:"RxDataPollCount",               valueTransform: this.&toInt 	]],
        0x002C:[[attribute:"RxBeaconCount",                 valueTransform: this.&toInt 	]],
        0x002D:[[attribute:"RxBeaconRequestCount",          valueTransform: this.&toInt 	]],
        0x002E:[[attribute:"RxOtherCount",                  valueTransform: this.&toInt 	]],
        0x002F:[[attribute:"RxAddressFilteredCount",        valueTransform: this.&toInt 	]],
        
        0x0030:[[attribute:"RxDestAddrFilteredCount",       valueTransform: this.&toInt		]],
        0x0031:[[attribute:"RxDuplicatedCount",             valueTransform: this.&toInt		]],
        0x0032:[[attribute:"RxErrNoFrameCount",             valueTransform: this.&toInt		]],
        0x0033:[[attribute:"RxErrUnknownNeighborCount",     valueTransform: this.&toInt		]],
        0x0034:[[attribute:"RxErrInvalidSrcAddrCount",      valueTransform: this.&toInt		]],
        0x0035:[[attribute:"RxErrSecCount",                 valueTransform: this.&toInt		]],
        0x0036:[[attribute:"RxErrFcsCount",                 valueTransform: this.&toInt		]],
        0x0037:[[attribute:"RxErrOtherCount",               valueTransform: this.&toInt		]],
        0x0038:[[attribute:"ActiveTimestamp",               valueTransform: this.&toInt		]],
        0x0039:[[attribute:"PendingTimestamp",              valueTransform: this.&toInt		]],
        0x003A:[[attribute:"Delay",                         valueTransform: this.&toInt		]],
        // 0x003B:[[attribute:"SecurityPolicy",                valueTransform: this.&toInt	]],
        // 0x003C:[[attribute:"ChannelPage0Mask",              valueTransform: this.&toInt	]],
        // 0x003D:[[attribute:"OperationalDatasetComponents",  valueTransform: this.&toInt	]],
        // 0x003E:[[attribute:"ActiveNEtworkFaults",           valueTransform: this.&toInt	]],
        ],
    0x0036:[ // WiFi Diagnostics
        0x0000:[[attribute:"BSSID",                   valueTransform: { it }									]],
        0x0001:[[attribute:"SecurityType",            valueTransform: { SecurityTypeEnum.get(toInt(it))}	    ]],
        0x0002:[[attribute:"WiFiVersion",             valueTransform: { WiFiVersionEnum.get(toInt(it))}	    	]],
        0x0003:[[attribute:"ChannelNumber",           valueTransform: this.&toInt			                    ]],
        0x0004:[[attribute:"RSSI",                    valueTransform: { toInt(it) - 256}	                    ]], // signed int8, always negative!
        0x0005:[[attribute:"BeaconLostCount",         valueTransform: this.&toInt			                    ]],
        0x0006:[[attribute:"BeaconRxCount",           valueTransform: this.&toInt			                    ]],
        0x0007:[[attribute:"PacketMulticastRxCount",  valueTransform: this.&toInt			                    ]],
        0x0008:[[attribute:"PacketMulticastTxCount",  valueTransform: this.&toInt			                    ]],
        0x0009:[[attribute:"PacketUnicastRxCount",    valueTransform: this.&toInt			                    ]],
        0x000A:[[attribute:"PacketUnicastTxCount",    valueTransform: this.&toInt			                    ]],
        0x000B:[[attribute:"CurrentMaxRate",          valueTransform: this.&toLong			                	]],
        0x000C:[[attribute:"OverrunCount",            valueTransform: this.&toLong			                	]],
        ],
    0x003B:[ // Boolean State
        0x0000:[[attribute:"StateVale",    valueTransform: this.&toBool ],
                [attribute:"contact",    valueTransform: { toInt(it) ? "closed" : "open"} ]], //
        ],
    0x0045:[ // Generic Switch Cluster
        0x0000:[[attribute:"NumberOfPositions",    valueTransform: this.&toInt		]], // Number of Plsitions
        0x0001:[[attribute:"CurrentPosition",      valueTransform: this.&toInt		]], // Current Position
        0x0002:[[attribute:"MultiPressMax",        valueTransform: this.&toInt		]], // Multi-Press Max
        ],
    0x0300:[ // Color Control Cluster.  Only covering the most common ones for Hue at the moment!
		0x0000:[ // Hue
			    [attribute:"hue",                       valueTransform: this.&HexToPercent,     units:"%" 	], 	//  This is the Hubitat name/value
			    [attribute:"CurrentHue",                valueTransform: this.&toInt,            units:"uint8" ],     	// This is the Matter name / value
               ],
        0x0001:[[attribute:"saturation",                valueTransform: this.&HexToPercent,      units:"%" ],  	//  This is the Hubitat name/value
                [attribute:"CurrentSaturation",         valueTransform: this.&toInt,             units:"uint8" ]      	// This is the Matter name / value
               ],
        0x0002:[[attribute:"RemainingTime",             valueTransform: this.&toTenths,             units:"seconds" ]],
        0x0007:[[attribute:"colorTemperature",          valueTransform: this.&HexMiredsToKelvin,    unit: "°K"], 
                [attribute:"ColorTemperatureMireds",    valueTransform: this.&toInt,                      unit: "Mireds"],
               ],
        0x0008:[[attribute:"colorMode",                 valueTransform: {[0:"RGB", 1:"CurrentXY", 2:"CT"].get(toInt(it))} ], // This is how Hubitat names it
                [attribute:"ColorMode",                 valueTransform: this.&toInt 	] // This is how Matter names it!
               ],
        0x000F:[[attribute:"Options",                   valueTransform: this.&toInt ]],
        0x400A:[[attribute:"ColorCapabilities",         valueTransform: { List capability = []; 
                                                                if (toInt(it) & 0b0000_0001) capability << "HS"; 
                                                                if (toInt(it) & 0b0000_0010) capability << "EHUE"; 
                                                                if (toInt(it) & 0b0000_0100) capability << "CL"; 
                                                                if (toInt(it) & 0b0000_1000) capability << "XY";  
                                                                if (toInt(it) & 0b0001_0000) capability << "CT"; 
                                                                return capability
                                                      } ]
               ],
        0x400B:[[attribute:"ColorTemperatureMinKelvin",  valueTransform: this.&HexMiredsToKelvin, unit: "°K" ]],
        0x400C:[[attribute:"ColorTemperatureMaxKelvin",  valueTransform: this.&HexMiredsToKelvin, unit: "°K" ]],
        ],
    0x0400:[ // Illuminance Measurement
        0x0000:[ [attribute:"illuminance",             valueTransform: this.&HexToLux, units:"lx"	], // This is the Hubitat name
                 [attribute:"MeasuredValue",           valueTransform: this.&HexToLux, units:"lx"	], // This is the Matter name
               ],
        0x0001:[ [attribute:"MinMeasuredValueLux",     valueTransform: this.&HexToLux, units:"lx"	]],
        0x0002:[ [attribute:"MaxMeasuredValueLux",     valueTransform: this.&HexToLux, units:"lx"	]],
        0x0003:[ [attribute:"LuxMeasurementTolerance", valueTransform: this.&HexToLux, units:"lx"	]],
        0x0004:[ [attribute:"LightSensorType",         valueTransform: {[0:"Photodiode", 1:"CMOS"].get(toInt(it)) } 	]], 
        ],
    0x0402:[ // Temperature Measurement
        0x0000:[ [attribute:"temperature",              valueTransform: { HexToTemp(it) / 100}, units:"°C"	],
                 [attribute:"TempMeasuredValue",        valueTransform: { HexToTemp(it) / 100}, units:"°C"	],
               ],
		0x0001:[ [attribute:"TempMinMeasuredValue",     valueTransform: { HexToTemp(it) / 100}, units:"°C"	]],
        0x0002:[ [attribute:"TempMaxMeasuredValue",     valueTransform: { HexToTemp(it) / 100}, units:"°C"	]],
        0x0003:[ [attribute:"TempTolerance", 		    valueTransform: this.&toCenti, units:"°C"		    ]],
    ],
    // 0x0403:[ // Pressure Measurement. Add if a supporting device comes to market for this!
    // 0x0404:[ // Flow Measurement. Add if a supporting device comes to market for this!
    0x0405:[ // Relative Humidty Measurement
        0x0000:[[attribute:"MeasuredValue",     valueTransform: this.&toCenti, units:"%" 	]],
        0x0001:[[attribute:"MinMeasuredValue",  valueTransform: this.&toCenti, units:"%" 	]],
        0x0002:[[attribute:"MaxMeasuredValue",  valueTransform: this.&toCenti, units:"%" 	]],
        0x0003:[[attribute:"Tolerance",         valueTransform: this.&toCenti, units:"%"	]],
    ],
    0x0406:[ // Occupancy Measurement
        0x0000:[[attribute:"motion",               			valueTransform: { toInt(it) ? "active" : "inactive" }	], 
                [attribute:"presence",             			valueTransform: { toInt(it) ? "active" : "inactive" }	],
                [attribute:"Occupancy",             		valueTransform: this.&toInt, units:"bitmap"				]],
        0x0001:[[attribute:"OccupancySensorType",  			valueTransform: { [0:"PIR", 1:"Ultrasonic", 2:"PIRAndUltrasonic", 3:"PhysicalContact"].get(toInt(it))}	]],
        0x0001:[[attribute:"OccupancySensorTypeBitmap",                     valueTransform: this.&toInt		]],
        0x0010:[[attribute:"PIROccupiedToUnoccupiedDelay", 					valueTransform: this.&toInt, units:"seconds"	]],
        0x0011:[[attribute:"PIRUnoccupiedToOccupiedDelay", 					valueTransform: this.&toInt, units:"seconds"	]],
        0x0012:[[attribute:"PIRUnoccupiedToOccupiedThreshold", 				valueTransform: this.&toInt, units:"events"		]],
        0x0020:[[attribute:"UltrasonicOccupiedToUnoccupiedDelay", 			valueTransform: this.&toInt, units:"seconds"	]],
        0x0031:[[attribute:"UltrasonicUnoccupiedToOccupiedDelay", 			valueTransform: this.&toInt, units:"seconds"	]],
        0x0032:[[attribute:"UltrasonicUnoccupiedToOccupiedThreshold", 		valueTransform: this.&toInt, units:"events"		]],
        0x0030:[[attribute:"PhysicalContactOccupiedToUnoccupiedDelay", 		valueTransform: this.&toInt, units:"seconds"	]],
        0x0031:[[attribute:"PhysicalContactUnoccupiedToOccupiedDelay", 		valueTransform: this.&toInt, units:"seconds"	]],
        0x0032:[[attribute:"PhysicalContactUnoccupiedToOccupiedThreshold", 	valueTransform: this.&toInt, units:"events"		]],
    ],
    // 0x0407:[ // Leaf Wetness Measurement. Add if a device comes to market for this!
    // 0x0408:[ // Soil Moisture Measurement. Add if a device comes to market for this!
    // 0x040C: concentrationMeasurementCluster, // CO
    0x040C: [
        0x0000:[[attribute:"MeasuredValue",                 valueTransform: { it }  ]], // Check if Hubitat provides a float!
        0x0001:[[attribute:"MinMeasuredValue",              valueTransform: { it }  ]], // Check if Hubitat provides a float!
        0x0002:[[attribute:"MaxMeasuredValue",              valueTransform: { it }  ]], // Check if Hubitat provides a float!
        0x0003:[[attribute:"PeakMeasuredValue",             valueTransform: { it }  ]], // Check if Hubitat provides a float!
        0x0004:[[attribute:"PeakMeasuredValueWindow",       valueTransform: this.&toInt , unit:"seconds" ]],
        0x0005:[[attribute:"AverageMeasuredValue",          valueTransform: { it }  ]],
        0x0006:[[attribute:"AverageMeasuredValueWindow",    valueTransform: { it }  ]],
        0x0007:[[attribute:"Uncertainty",                   valueTransform: { it }  ]],
        0x0008:[[attribute:"MeasurementUnit",               valueTransform: { MeasurementUnitEnum.get(toInt(it)) }     ]],
        0x0009:[[attribute:"MeasurementMedium",             valueTransform: { MeasurementMediumEnum.get(toInt(it)) }   ]],
        0x000A:[[attribute:"LevelValue",                    valueTransform: { LevelValueEnum.get(toInt(it)) }          ]]
    ],
    // 0x040D: concentrationMeasurementCluster, // CO2. Aliases to 0x040C
    // 0x0413: concentrationMeasurementCluster, // NO2. Aliases to 0x040C
    // 0x0415: concentrationMeasurementCluster, // O3. Aliases to 0x040C
    // 0x042A: concentrationMeasurementCluster, // PM2.5. Aliases to 0x040C
    // 0x042B: concentrationMeasurementCluster, // Formaldehyde. Aliases to 0x040C
    // 0x042C: concentrationMeasurementCluster, // PM1. Aliases to 0x040C
    // 0x042D: concentrationMeasurementCluster, // PM10. Aliases to 0x040C
    // 0x042E: concentrationMeasurementCluster, // TVOC. Aliases to 0x040C
    // 0x042F: concentrationMeasurementCluster, // Radon (Rn). Aliases to 0x040C
    0x005B:[ // Air Quality
        0x0000:[[attribute:"AirQuality", valueTransform: { AirQualityEnumType.get(toInt(it)) }	]],
    ],   
	0x005C:[ // Smoke and CO Alarm
        0x0000:[[attribute:"ExpressedState", 	        valueTransform: { ExpressedStateEnum.get(toInt(it)) }			]],
        0x0001:[[attribute:"SmokeState", 		        valueTransform: { AlarmStateEnum.get(toInt(it)) }				]],
        0x0002:[[attribute:"COState", 			        valueTransform: { AlarmStateEnum.get(toInt(it)) }				]],
        0x0003:[[attribute:"BatteryAlert", 		        valueTransform: { AlarmStateEnum.get(toInt(it)) }				]],
        0x0004:[[attribute:"DeviceMuted", 		        valueTransform: { MuteStateEnum.get(toInt(it)) }				]],
        0x0005:[[attribute:"TestInProgress", 			valueTransform: this.&toBool     				                ]],
        0x0006:[[attribute:"HardwareFaultAlert",		valueTransform: this.&toBool     				                ]],
        0x0007:[[attribute:"EndOfServiceAlert", 		valueTransform: { EndOfServiceEnum.get(toInt(it)) }	            ]],
        0x0008:[[attribute:"InterconnectSmokeAlarm", 	valueTransform: { AlarmStateEnum.get(toInt(it)) }			    ]],
        0x0009:[[attribute:"InterconnectCOAlarm", 		valueTransform: { AlarmStateEnum.get(toInt(it)) }			    ]],
        0x000A:[[attribute:"ContaminationState", 		valueTransform: { ContaminationStateEnum.get(toInt(it)) }	    ]],
        0x000B:[[attribute:"SmokeSensitivityLevel", 	valueTransform: { SensitivityEnum.get(toInt(it)) }		        ]],
        0x000C:[[attribute:"ExpiryDate", 				valueTransform: { it }, units:"epoch-s"					        ]],
    ],
    // Concentration Measurement Cluster (Matter Spec Section 2.10) are too complex and beyond this library
    0x130AFC01:[ // Eve Energy Custom Cluster
        0x130A0008:[[attribute:"voltage",             valueTransform: this.&toMilli, units:"V"		]], // Probably needs to account for negatives!
        0x130A0009:[[attribute:"amperage",            valueTransform: this.&toMilli, units:"A"		]], // Probably needs to account for negatives!
        0x130A000A:[[attribute:"power",               valueTransform: this.&toMilli, units:"W"		]], // Probably needs to account for negatives!
        0x130A000B:[[attribute:"EveWattsAccumulated", valueTransform: this.&toInt					]], // Probably needs to account for negatives!
        0x130A000E:[[attribute:"EveWattAccumulatedControlPoint", valueTransform: this.&toInt		]]  // Probably needs to account for negatives!
   ],
]

List getHubitatEvents(Map descMap) {
    try {
        // Certain clusters have the same set of matching attributes. For those clusters, rather than storing
        // multiple duplicated copies of their transform data, one copy is stored (the "first copy"), and other clusters
        // that have the same attributes (the "aliased clusters") are mapped to the first copy
        Map aliasedCluster = [ 0x040D:0x040C, 0x0413:0x040C, 0x0415:0x040C, 0x042A:0x040C, // Concentraton Measurement Clusters
            0x042B:0x040C, 0x042C:0x040C, 0x042D:0x040C, 0x042E:0x040C, 0x042F:0x040C // More Concentraton Measurement Clusters
            ]
        // The next line determines if you should use an aliased cluster mapping for descMap.clusterInt, or just use clusterInt
        Integer retrieveThisCluster = (descMap.clusterInt in aliasedCluster) ? aliasedCluster.get(descMap.clusterInt) : descMap.clusterInt
        
        List rEvents = globalAllEventsMap.get(retrieveThisCluster)
		    ?.get(descMap.attrInt)
			    ?.collect{ Map rValue = [:]
                        rValue << [name:it.attribute] // First copy the attribute string as the name of the event
                          
                        // Now figure out the value for the event using the valueTransform  
                        if (it.valueTransform instanceof Closure) {
                              rValue << [value:it.valueTransform(descMap.value)] // if valueTransform is a closure, apply the closure to the data received from the node
                        } else {
                              rValue << [value: it.valueTransform]  // else just copy valueTransform's value into it.value value
                        }
                          
                          
						rValue << ( it.units ? [units:(it.units)]  : [:] )
                          
                          
						// Now let's form a descriptionText string
                          String newDescription
                          if (it.descriptionText && (it.descriptionText instanceof Closure)) {
                              // If you have a descriptionText field and it is a closure, then form the description text using
                              // that Closure supplied with the value (the value then can be used in the description)
                              newDescription = it.descriptionText(rValue.value)
                          } else {
                                newDescription = "${StringUtils.splitByCharacterTypeCamelCase(rValue.name).join(" ")} attribute set to ${rValue.value}"
                                // newDescription = "${rValue.name} attribute set to ${rValue.value}"
                                if (it.units) { newDescription = newDescription + " " + it.units }
                          }
                        rValue << ( [descriptionText:newDescription])
                          
                        // Was an isStateChange clause stated, if so, copy it if it is true. False is implied.
						rValue << ( it.isStateChange ? [isStateChange:true]  : [:] )
                          
                          
                        rValue << ( [clusterInt : (descMap.clusterInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device
                        rValue << ( [attrInt : (descMap.attrInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra attribute info for external device
                        rValue << ( [endpointInt : (Integer.parseInt(descMap.endpoint, 16))]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device

                      String rawDataAsJSON = new JsonBuilder(descMap.value).toString() // Event is sent on Hubitat's Event stream to external devices, so let's include original data in JSON form for external device
                      rValue << ( [rawValue: rawDataAsJSON ]) 
					}
        if (txtEnable) log.debug "Generated events in createListOfMatterSendEventMaps from inputs ${descMap} using retrieve cluster ${retrieveThisCluster} are : ${rEvents}"
        return rEvents
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing description string ${description}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

