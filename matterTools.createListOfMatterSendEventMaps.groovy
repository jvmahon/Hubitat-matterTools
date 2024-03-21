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

@Field static Closure toTenths = { it / 10}      // Hex to .1 conversion.
@Field static Closure toCenti =  { it / 100}     // Hex to .01 conversion.
@Field static Closure toMilli =  { it / 1000}    // Hex to .001 conversion.
@Field static Closure HexToPercent = { it ? Math.max( Math.round(it / 2.54) , 1) : 0 } // the Math.max check ensures that a value of 1/2.54 does not get changes to 0
@Field static Closure HexToLux =          { Math.pow( 10, (it - 1) / 10000)  as Integer} // convert Matter value to illumination in lx. See Matter Cluster Spec Section 2.2.5.1
@Field static Closure MiredsToKelvin = { ( (it > 0) ? (1000000 / it) : null ) as Integer}

/*
For Closure values in the following structure:
pv = parsed Map value field (descMap.value)
dn = device name - provided as a string
dv = device value - usually the content of the event map's "value" field after pv has been converted by the closure in the "value" field, below..
*/
@Field static Map globalAllEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events
    0x0003:[ // Identify Cluster
        0x0000:[[attribute:"IdentifyTime", units:"seconds"				]],
        0x0001:[[attribute:"IdentifyType", valueTransform: { IdentifyTypeEnum.get(it)  }			]],
    ],
    0x0004:[ // Groups Cluster
        0x0000:[[attribute:"NameSupport"  ]],
    ],
    0x0006:[ // Switch Cluster
        0x0000:[[attribute:"switch",             valueTransform: { it ? "on" : "off" }     		],
                [attribute:"OnOff", 				]],
        0x4000:[[attribute:"GlobalSceneControl",	]],
        0x4001:[[attribute:"OnTime",             valueTransform: this.&toTenths,  units:"seconds" 		]],
        0x4002:[[attribute:"OffWaitTime",        valueTransform: this.&toTenths,  units:"seconds" 		]],
        0x4003:[[attribute:"StartUpOnOff",       valueTransform: { StartUpOnOffEnum.get(it)}  	]],
    ],
    0x0008:[ // Level Cluster
        0x0000:[[attribute:"level",                valueTransform: this.&HexToPercent, 	    units:"%"       	],
                [attribute:"CurrentLevel",   					  			                                    ]],
        0x0001:[[attribute:"RemainingTime",        valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0002:[[attribute:"MinLevel",             valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0003:[[attribute:"MaxLevel",             valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0004:[[attribute:"CurrentFrequency",     	units:"Hz"      	                                        ]],
        0x0005:[[attribute:"MinFrequency",       	units:"Hz"      	                                        ]],
        0x0006:[[attribute:"MaxFrequency",        	units:"Hz"      	                                        ]],
        0x0010:[[attribute:"OnOffTransitionTime",  valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0011:[[attribute:"OnLevel",              valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0012:[[attribute:"OnTransitionTime",     valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0013:[[attribute:"OffTransitionTime",    valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0014:[[attribute:"DefaultMoveRate",    						                                        ]],
        0x000F:[[attribute:"Options",                                                                           ]],
        0x4000:[[attribute:"StartUpCurrentLevel"                                                                ]],
    ],
    0x001D:[
        0x0000:[[attribute:"DeviceTypeList",	]],
        0x0001:[[attribute:"ServerList",		]],
        0x0002:[[attribute:"ClientList",		]],
        0x0003:[[attribute:"PartsList",			]],
        0x0004:[[attribute:"TagList",			]],
        ],
    0x001E:[
        0x0000:[[attribute:"Binding",	]],
        ],
    0x0028:[ // Basic Information
        0x0000:[[attribute:"DataModelRevision",		]],
        0x0001:[[attribute:"VendorName",			]],
        0x0002:[[attribute:"VendorID",              ]],
        0x0003:[[attribute:"ProductName",           ]],
        0x0004:[[attribute:"ProductID",             ]],
        0x0005:[[attribute:"NodeLabel",             ]],
        0x0006:[[attribute:"Location",              ]],
        0x0007:[[attribute:"HardwareVersion",       ]],
        0x0008:[[attribute:"HardwareVersionString", ]],
        0x0009:[[attribute:"SoftwareVersion",       ]],
        0x000A:[[attribute:"SoftwareVersionString", ]],
        0x000B:[[attribute:"ManufacturingDate",     ]],
        0x000C:[[attribute:"PartNumber",            ]],
        0x000D:[[attribute:"ProductURL",            ]],
        0x000E:[[attribute:"ProductLabel",          ]],
        0x000F:[[attribute:"SerialNumber",          ]],
        0x0010:[[attribute:"LocalConfigDisabled",   ]],
        0x0011:[[attribute:"Reachable",             ]],
        0x0012:[[attribute:"UniqueID",              ]],
        0x0013:[[attribute:"CapabilityMinima",      ]],
        0x0014:[[attribute:"ProductAppearance",     ]],
        ],
    0x002B:[ // Localization Configuration
        0x0000:[[attribute:"ActiveLocale",         ]],
        0x0000:[[attribute:"SupportedLocales",     ]],
        ],
    0x002D:[ // Unit Localization
        0x0000:[[attribute:"TemperatureUnit",         valueTransform: { [0:"Fahrenheit", 1:"Celsius", 2:"Kelvin"].get(toInt(it)) }	]],
        ],
    0x002F:[ // Power Source Cluster
        0x0000:[[attribute:"Status",						valueTransform: { PowerSourceStatusEnum.get(it)}	]],
        0x0001:[[attribute:"Order",														                        ]],
        0x0002:[[attribute:"Description",													                    ]],
        0x0003:[[attribute:"WiredAssessedInputVoltage",		valueTransform: this.&toMilli,  units:"V"			]],
        0x0004:[[attribute:"WiredAssessedInputFrequency",	units:"Hz"				                            ]],
        0x0005:[[attribute:"WiredCurrentType",				valueTransform: { WiredCurrentTypeEnum.get(it)}	    ]],
        0x0006:[[attribute:"WiredAssessedCurrent",			valueTransform: this.&toMilli, units:"A"			]],
        0x0007:[[attribute:"WiredNominalVoltage",			valueTransform: this.&toMilli, units:"V"			]],
        0x0008:[[attribute:"WiredMaximumCurrent",           valueTransform: this.&toMilli, units:"A"			]],
        0x0009:[[attribute:"WiredPresent"			                                                            ]],
        0x000A:[[attribute:"ActiveWiredFaults"		                                                            ]],
        0x000B:[[attribute:"BatVoltage",					valueTransform: this.&toMilli, units:"V", 		descriptionText: {"Battery Voltage is: ${it}"}                  ]],
        0x000C:[[attribute:"BatPercentRemaining",    		valueTransform: { it / 2 }, units:"%", 			descriptionText: {"Battery Percent Remaining is: ${it}"}        ],
				[attribute:"battery",    					valueTransform: { it / 2 }, units:"%", 			descriptionText: {"Battery Percent Remaining is: ${it}"}        ]],
        0x000D:[[attribute:"BatTimeRemaining",       		    units:"seconds", 			                descriptionText: {"Battery Time Remaining is: ${it}"}           ]],
        0x000E:[[attribute:"BatChargeLevel",    			valueTransform: { BatChargeLevelEnum.get((it)) }, 	descriptionText: {"Battery Charge Level is: ${it}"}         ]],
        0x000F:[[attribute:"BatReplacementNeeded",       	      						                    descriptionText: {"Battery Replacement Needed: ${it}"}          ]],
        0x0010:[[attribute:"BatReplaceability",				valueTransform: { BatReplaceabilityEnum.get(it)}, descriptionText: {"Battery Replaceability is: ${it}"}         ]],
        0x0011:[[attribute:"BatPresent",                          						descriptionText: {"Battery Present: ${it}"}                                         ]],
        0x0012:[[attribute:"ActiveBatFaults",               							descriptionText: {"Active Battery Faults are: ${it}"}                               ]],
        0x0013:[[attribute:"BatReplacementDescription",		 							descriptionText: {"Battery Replacement Description: ${it}"}                         ]],
        0x0014:[[attribute:"BatCommonDesignation",  		valueTransform: { BatCommonDesignationEnum.get(it)}, descriptionText: {"Battery Common  Designation: ${it}"}    ]],
        0x0015:[[attribute:"BatANSIDesignation",										descriptionText: {"Battery ANSI C18 Designation: ${it}"}                            ]],
        0x0016:[[attribute:"BatIECDesignation",             							descriptionText: {"Battery IEC 60086 Designation: ${it}"}                           ]],
        0x0017:[[attribute:"BatApprovedChemistry",      	valueTransform: { BatApprovedChemistryEnum.get(it)}, descriptionText: {"Battery Approved Chemistry: ${it}"}     ]],
        0x0018:[[attribute:"BatCapacity",                       units:"mAh", 				descriptionText: {"Battery Capacity: ${it} mAH"}                                    ]],
        0x0019:[[attribute:"BatQuantity",                	 							descriptionText: {"Battery Quantity: ${it}"}                                        ]],
        0x001A:[[attribute:"BatChargeState",                valueTransform: { BatChargeState.get(it) }, 		descriptionText: {"Battery Charge State: ${it}"}            ]],
        0x001B:[[attribute:"BatTimeToFullCharge",               units:"seconds", 			descriptionText: {"Battery Time To Full Charge: ${it} seconds"}                     ]],
        0x001C:[[attribute:"BatFunctionalWhileCharging",          						descriptionText: {"Battery Functional While Charging: ${it}"}                       ]],
        0x001D:[[attribute:"BatChargingCurrent",             							descriptionText: {"Battery Charging Current: ${it}"}                                ]],
        0x001E:[[attribute:"ActiveBatChargeFaults",         							descriptionText: {"Active Battery Charge Faults: ${it}"}                            ]],
        0x001F:[[attribute:"EndpointList",               								descriptionText: {"Power Source Endpoint List: ${it}"}                              ]],
        ],

    0x0035:[ // Thread Diagnostics
        0x0000:[[attribute:"Channel",              						]],
        0x0001:[[attribute:"RoutingRole", valueTransform: { RoutingRoleEnum.get(it)}	]],
        0x0002:[[attribute:"NetworkName",          						]],
        0x0003:[[attribute:"PanId",                					    ]],
        0x0004:[[attribute:"ExtendedPanId",        						]],
        0x0005:[[attribute:"MeshLocalPrefix",      						]],
        0x0006:[[attribute:"OverrunCount",         						]],
        0x0007:[[attribute:"NeighborTable",                			    ]],
        0x0008:[[attribute:"RouteTable",                           		]],
        0x0009:[[attribute:"PartitionId",          						]],
        0x000A:[[attribute:"Weighting",            						]],
        0x000B:[[attribute:"DataVersion",          						]],
        0x000C:[[attribute:"StableDataVersion",    						]],
        0x000D:[[attribute:"LeaderRouterId",       						]],
        0x000E:[[attribute:"DetachedRoleCount",    						]],
        0x000F:[[attribute:"ChildRoleCount",       						]],
        
        0x0010:[[attribute:"RouterRoleCount",                    		]],
        0x0011:[[attribute:"LeaderRoleCount",                    		]],
        0x0012:[[attribute:"AttachedAttemptCount",               		]],
        0x0013:[[attribute:"PartitionIdChangeCount",             		]],
        0x0014:[[attribute:"BetterPartitionAttachAttemptCount",  		]],
        0x0015:[[attribute:"ParentChangeCount",                  		]],
        0x0016:[[attribute:"TxTotalCount",                       		]],
        0x0017:[[attribute:"TxUnicastCount",                     		]],
        0x0018:[[attribute:"TxBroadcastCount",                   		]],
        0x0019:[[attribute:"TxAckRequestedCount",                		]],
        0x001A:[[attribute:"TxAcked",                            		]],
        0x001B:[[attribute:"TxNoAckRequestedCount",              		]],
        0x001C:[[attribute:"TxDataCount",                        		]],
        0x001D:[[attribute:"TxDataPollCount",                    		]],
        0x001E:[[attribute:"TxBeaconCount",                      		]],
        0x001F:[[attribute:"TxBeaconRequestCount",               		]],
        
        0x0020:[[attribute:"TxOtherCount",                  	]],
        0x0021:[[attribute:"TxRetryCount",                  	]],
        0x0022:[[attribute:"TxDirectMaxRetryExpiryCount",    	]],
        0x0023:[[attribute:"TxIndirectMaxRetryExpiryCount",  	]],
        0x0024:[[attribute:"TxErrCcaCount",                  	]],
        0x0025:[[attribute:"TxErrAbortCount",                	]],
        0x0026:[[attribute:"TxErrBusyChannelCount",          	]],
        0x0027:[[attribute:"RxTotalCount",                   	]],
        0x0028:[[attribute:"RxUnicastCount",                 	]],
        0x0029:[[attribute:"RxBroadcastCount",               	]],
        0x002A:[[attribute:"RxDataCount",                    	]],
        0x002B:[[attribute:"RxDataPollCount",                	]],
        0x002C:[[attribute:"RxBeaconCount",                  	]],
        0x002D:[[attribute:"RxBeaconRequestCount",           	]],
        0x002E:[[attribute:"RxOtherCount",                   	]],
        0x002F:[[attribute:"RxAddressFilteredCount",         	]],
        0x0030:[[attribute:"RxDestAddrFilteredCount",       	]],
        0x0031:[[attribute:"RxDuplicatedCount",             	]],
        0x0032:[[attribute:"RxErrNoFrameCount",             	]],
        0x0033:[[attribute:"RxErrUnknownNeighborCount",     	]],
        0x0034:[[attribute:"RxErrInvalidSrcAddrCount",      	]],
        0x0035:[[attribute:"RxErrSecCount",                 	]],
        0x0036:[[attribute:"RxErrFcsCount",                 	]],
        0x0037:[[attribute:"RxErrOtherCount",               	]],
        0x0038:[[attribute:"ActiveTimestamp",               	]],
        0x0039:[[attribute:"PendingTimestamp",              	]],
        0x003A:[[attribute:"Delay",                         	]],
        // 0x003B:[[attribute:"SecurityPolicy",                	]],
        // 0x003C:[[attribute:"ChannelPage0Mask",              	]],
        // 0x003D:[[attribute:"OperationalDatasetComponents",  	]],
        // 0x003E:[[attribute:"ActiveNetworkFaults",           	]],
        ],
    0x0036:[ // WiFi Diagnostics
        0x0000:[[attribute:"BSSID",                   								    ]],
        0x0001:[[attribute:"SecurityType", valueTransform: { SecurityTypeEnum.get(it)}	]],
        0x0002:[[attribute:"WiFiVersion", valueTransform: { WiFiVersionEnum.get(it)}	]],
        0x0003:[[attribute:"ChannelNumber",           			                        ]],
        0x0004:[[attribute:"RSSI",                   								    ]], 
        0x0005:[[attribute:"BeaconLostCount",         			                        ]],
        0x0006:[[attribute:"BeaconRxCount",           			                        ]],
        0x0007:[[attribute:"PacketMulticastRxCount",  			                        ]],
        0x0008:[[attribute:"PacketMulticastTxCount",  			                        ]],
        0x0009:[[attribute:"PacketUnicastRxCount",    			                        ]],
        0x000A:[[attribute:"PacketUnicastTxCount",    			                        ]],
        0x000B:[[attribute:"CurrentMaxRate",          			                	    ]],
        0x000C:[[attribute:"OverrunCount",            			                	    ]],
        ],
    0x003B:[ // Generic Switch Cluster
        0x0000:[[attribute:"NumberOfPositions",    		]],
        0x0001:[[attribute:"CurrentPosition",      		]],
        0x0002:[[attribute:"MultiPressMax",        		]],
        ],
    0x0040:[ // Fixed Label Cluster, Core Spec 9.8
        0x0000:[[attribute:"FixedLabelList",    		]], // Note attribute name change to prevent confusion between 0x0040 and 0x0041
        ],
    0x0041:[ // Fixed Label Cluster, Core Spec 9.9
        0x0000:[[attribute:"UserLabelList",    		]], // Note attribute name change to prevent confusion between 0x0040 and 0x0041
        ],
    0x0045:[ // Boolean State
        0x0000:[[attribute:"StateVale",     ],
                [attribute:"contact",    valueTransform: { it ? "closed" : "open"} ]], //
        ],
    0x0046: [ // ICD Management Cluster
        0x0000:[[attribute:"IdleModeInterval"          ]],
        0x0001:[[attribute:"ActiveModeInterval"        ]],
        0x0002:[[attribute:"ActiveModeThreshold"       ]],
        0x0003:[[attribute:"RegisteredClients"         ]],
        0x0004:[[attribute:"ICDCounter"                ]],
        0x0005:[[attribute:"ClientsSupportedPerFabric" ]],
        ],
    0x0050: [ // Mode Select Cluster
        0x0000:[[attribute:"Description"          ]],
        0x0001:[[attribute:"StandardNamespace"        ]],
        0x0002:[[attribute:"SupportedModes"       ]],
        0x0003:[[attribute:"CurrentMode"         ]],
        0x0004:[[attribute:"StartUpMode"                ]],
        0x0005:[[attribute:"OnMode" ]],
        ],
    0x0300:[ // Color Control Cluster.  Only covering the most common ones for Hue at the moment!
		0x0000:[ // Hue
			    [attribute:"hue",  valueTransform: this.&HexToPercent, units:"%" 	], 	//  This is the Hubitat name/value
			    [attribute:"CurrentHue",					                        ],     	// This is the Matter name / value
               ],
        0x0001:[[attribute:"saturation", valueTransform: this.&HexToPercent, units:"%"  ],  	//  This is the Hubitat name/value
                [attribute:"CurrentSaturation",                                         ]      	// This is the Matter name / value
               ],
        0x0002:[[attribute:"RemainingTime", valueTransform: this.&toTenths, units:"seconds" ]],
        0x0003:[[attribute:"CurrentX",                  ]],
        0x0004:[[attribute:"CurrentY",                  ]],
        0x0005:[[attribute:"DriftCompensation",         ]],
        0x0006:[[attribute:"CompensationText",          ]],
        
        0x0007:[[attribute:"colorTemperature", valueTransform: this.&MiredsToKelvin, units: "°K"], 
                [attribute:"ColorTemperatureMireds",    units: "Mireds"],
               ],
        0x0008:[[attribute:"colorMode",                 valueTransform: {[0:"RGB", 1:"CurrentXY", 2:"CT"].get(it)} ], // This is how Hubitat names it
                [attribute:"ColorMode",                  	] // This is how Matter names it!
               ],
        0x000F:[[attribute:"Options",                    ]],
        
        0x0010:[[attribute:"NumberOfPrimaries",                  ]],
        
        0x0011:[[attribute:"Primary1X",                  ]],
        0x0012:[[attribute:"Primary1Y",         ]],
        0x0013:[[attribute:"Primary1Intensity",          ]],   
        
        0x0015:[[attribute:"Primary2X",                    ]],
        0x0016:[[attribute:"Primary2Y",          ]],
        0x0017:[[attribute:"Primary2Intensity",            ]],
        
        0x0019:[[attribute:"Primary3X",         ]],
        0x001A:[[attribute:"Primary3Y",              ]],
        0x001B:[[attribute:"Primary3Intensity",  ]],
  
        0x0020:[[attribute:"Primary4X",                  ]],
        0x0021:[[attribute:"Primary4Y",         ]],
        0x0022:[[attribute:"Primary4Intensity",          ]], 
        
        0x0024:[[attribute:"Primary5X",                    ]],
        0x0025:[[attribute:"Primary5Y",          ]],
        0x0026:[[attribute:"Primary5Intensity",            ]],
        
        0x0028:[[attribute:"Primary6X",         ]],
        0x0029:[[attribute:"Primary6Y",              ]],
        0x002A:[[attribute:"Primary6Intensity",  ]],  
        
        0x0020:[[attribute:"WhitePointX",                  ]],
        0x0021:[[attribute:"WhitePointY",         ]],
        
        0x0030:[[attribute:"ColorPointRX",          ]],       
        0x0031:[[attribute:"ColorPointRY",                    ]],
        0x0032:[[attribute:"ColorPointRIntensity",          ]],
        
        0x0033:[[attribute:"ColorPointGX",            ]],
        0x0034:[[attribute:"ColorPointGY",         ]],
        0x0036:[[attribute:"ColorPointGIntensity",              ]],
        
        0x0037:[[attribute:"ColorPointBX",            ]],
        0x0038:[[attribute:"ColorPointBY",         ]],
        0x003A:[[attribute:"ColorPointBIntensity",              ]],
        
    
        
        0x4001:[[attribute:"EnhancedColorMode",          ]],
        0x4002:[[attribute:"ColorLoopActive",            ]],
        0x4003:[[attribute:"ColorLoopDirection",         ]],
        0x4004:[[attribute:"ColorLoopTime",              ]],
        0x4005:[[attribute:"ColorLoopStartEnhancedHue",  ]],
        0x4006:[[attribute:"ColorLoopStoredEnhancedHue", ]],

        0x400A:[[attribute:"ColorCapabilities",         valueTransform: { List capability = []; 
                                                                if (it & 0b0000_0001) capability << "HS"; 
                                                                if (it & 0b0000_0010) capability << "EHUE"; 
                                                                if (it & 0b0000_0100) capability << "CL"; 
                                                                if (it & 0b0000_1000) capability << "XY";  
                                                                if (it & 0b0001_0000) capability << "CT"; 
                                                                return capability
                                                      } ]
               ],
        0x400B:[[attribute:"ColorTemperaturePhysicalMinMireds",  units: "Mireds" ],
                [attribute:"ColorTemperatureMaxKelvin",              valueTransform: this.&MiredsToKelvin, units: "°K" ]],
        0x400C:[[attribute:"ColorTemperaturePhysicalMaxMireds",  units: "°M" ],
                [attribute:"ColorTemperatureMinKelvin",              valueTransform: this.&MiredsToKelvin, units: "°K" ]],
        0x400D:[[attribute:"CoupleColorTempToLevelMinMireds",    ]],
        0x4010:[[attribute:"StartUpColorTemperatureMireds",      units: "°M" ],
                [attribute:"StartUpColorTemperatureKelvin",              valueTransform: this.&MiredsToKelvin, units: "°K" ]],
        ],
    0x0400:[ // Illuminance Measurement
        0x0000:[ [attribute:"illuminance",             valueTransform: this.&HexToLux, units:"lx"	], // This is the Hubitat name
                 [attribute:"MeasuredValue",           valueTransform: this.&HexToLux, units:"lx"	], // This is the Matter name
               ],
        0x0001:[ [attribute:"MinMeasuredValueLux",     valueTransform: this.&HexToLux, units:"lx"	]],
        0x0002:[ [attribute:"MaxMeasuredValueLux",     valueTransform: this.&HexToLux, units:"lx"	]],
        0x0003:[ [attribute:"LuxMeasurementTolerance", valueTransform: this.&HexToLux, units:"lx"	]],
        0x0004:[ [attribute:"LightSensorType",         valueTransform: {[0:"Photodiode", 1:"CMOS"].get(it) } 	]], 
        ],
    0x0402:[ // Temperature Measurement
        0x0000:[ [attribute:"temperature",              valueTransform: this.&toCenti, units:"°C"	],
                 [attribute:"TempMeasuredValue",        valueTransform: this.&toCenti, units:"°C"	],
               ],
		0x0001:[ [attribute:"TempMinMeasuredValue",     valueTransform: this.&toCenti, units:"°C"	]],
        0x0002:[ [attribute:"TempMaxMeasuredValue",     valueTransform: this.&toCenti, units:"°C"	]],
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
        0x0000:[[attribute:"motion",               			valueTransform: { it ? "active" : "inactive" }	], 
                [attribute:"presence",             			valueTransform: { it ? "active" : "inactive" }	],
                [attribute:"Occupancy",             		units:"bitmap"				]],
        0x0001:[[attribute:"OccupancySensorType",  			valueTransform: { [0:"PIR", 1:"Ultrasonic", 2:"PIRAndUltrasonic", 3:"PhysicalContact"].get(it)}	]],
        0x0001:[[attribute:"OccupancySensorTypeBitmap",                     		]],
        0x0010:[[attribute:"PIROccupiedToUnoccupiedDelay", 					units:"seconds"	]],
        0x0011:[[attribute:"PIRUnoccupiedToOccupiedDelay", 					units:"seconds"	]],
        0x0012:[[attribute:"PIRUnoccupiedToOccupiedThreshold", 				units:"events"	]],
        0x0020:[[attribute:"UltrasonicOccupiedToUnoccupiedDelay", 			units:"seconds"	]],
        0x0031:[[attribute:"UltrasonicUnoccupiedToOccupiedDelay", 			units:"seconds"	]],
        0x0032:[[attribute:"UltrasonicUnoccupiedToOccupiedThreshold", 		units:"events"	]],
        0x0030:[[attribute:"PhysicalContactOccupiedToUnoccupiedDelay", 		units:"seconds"	]],
        0x0031:[[attribute:"PhysicalContactUnoccupiedToOccupiedDelay", 		units:"seconds"	]],
        0x0032:[[attribute:"PhysicalContactUnoccupiedToOccupiedThreshold", 	units:"events"	]],
    ],
    // 0x0407:[ // Leaf Wetness Measurement. Add if a device comes to market for this!
    // 0x0408:[ // Soil Moisture Measurement. Add if a device comes to market for this!
    // 0x040C: concentrationMeasurementCluster, // CO
    0x040C: [
        0x0000:[[attribute:"MeasuredValue",                   ]],
        0x0001:[[attribute:"MinMeasuredValue",                ]],
        0x0002:[[attribute:"MaxMeasuredValue",                ]],
        0x0003:[[attribute:"PeakMeasuredValue",               ]],
        0x0004:[[attribute:"PeakMeasuredValueWindow", units:"seconds" ]],
        0x0005:[[attribute:"AverageMeasuredValue",            ]],
        0x0006:[[attribute:"AverageMeasuredValueWindow",      ]],
        0x0007:[[attribute:"Uncertainty",                     ]],
        0x0008:[[attribute:"MeasurementUnit",               valueTransform: { MeasurementUnitEnum.get(it) }     ]],
        0x0009:[[attribute:"MeasurementMedium",             valueTransform: { MeasurementMediumEnum.get(it) }   ]],
        0x000A:[[attribute:"LevelValue",                    valueTransform: { LevelValueEnum.get(it) }          ]]
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
        0x0000:[[attribute:"AirQuality", valueTransform: { AirQualityEnumType.get(it) }	]],
    ],   
	0x005C:[ // Smoke and CO Alarm
        0x0000:[[attribute:"ExpressedState", 	        valueTransform: { ExpressedStateEnum.get(it) }			]],
        0x0001:[[attribute:"SmokeState", 		        valueTransform: { AlarmStateEnum.get(it) }				]],
        0x0002:[[attribute:"COState", 			        valueTransform: { AlarmStateEnum.get(it) }				]],
        0x0003:[[attribute:"BatteryAlert", 		        valueTransform: { AlarmStateEnum.get(it) }				]],
        0x0004:[[attribute:"DeviceMuted", 		        valueTransform: { MuteStateEnum.get(it) }				]],
        0x0005:[[attribute:"TestInProgress", 			     				                					]],
        0x0006:[[attribute:"HardwareFaultAlert",		     				                					]],
        0x0007:[[attribute:"EndOfServiceAlert", 		valueTransform: { EndOfServiceEnum.get(it) }	        ]],
        0x0008:[[attribute:"InterconnectSmokeAlarm", 	valueTransform: { AlarmStateEnum.get(it) }			    ]],
        0x0009:[[attribute:"InterconnectCOAlarm", 		valueTransform: { AlarmStateEnum.get(it) }			    ]],
        0x000A:[[attribute:"ContaminationState", 		valueTransform: { ContaminationStateEnum.get(it) }	    ]],
        0x000B:[[attribute:"SmokeSensitivityLevel", 	valueTransform: { SensitivityEnum.get(it) }		        ]],
        0x000C:[[attribute:"ExpiryDate", 				units:"epoch-s"					        				]],
    ],
    0x040C:[// Concentration Measurement Cluster (Matter Spec Section 2.10)
        0x0000:[[attribute:"MeasuredValue"                                                              ]],
        0x0001:[[attribute:"MinMeasuredValue"                                                           ]],
        0x0002:[[attribute:"MaxMeasuredValue"                                                           ]],
        0x0003:[[attribute:"PeakMeasuredValue"                                                          ]],
        0x0004:[[attribute:"PeakMeasuredValueWindow"                                                    ]],
        0x0005:[[attribute:"AverageMeasuredValue"                                                       ]],
        0x0006:[[attribute:"AverageMeasuredValueWindow"                                                 ]],
        0x0007:[[attribute:"Uncertainty"                                                                ]],
        0x0008:[[attribute:"MeasurementUnit", 	    valueTransform: { MeasurementUnitEnum.get(it) }	    ]],
        0x0009:[[attribute:"MeasurementMedium", 	valueTransform: { MeasurementMediumEnum.get(it) }	]],
        0x000A:[[attribute:"LevelValue", 	        valueTransform: { LevelValueEnum.get(it) }	        ]],
        ],
    0x130AFC01:[ // Eve Energy Custom Cluster
        0x130A0008:[[attribute:"voltage",             units:"V"		]], 
        0x130A0009:[[attribute:"amperage",            units:"A"		]], 
        0x130A000A:[[attribute:"power",               units:"W"		]], 
        0x130A000B:[[attribute:"EveWattAccumulated", 				]], 
        0x130A000E:[[attribute:"EveWattAccumulatedControlPoint", 	]]  
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
                          
                        // Now figure out the value for the event using the valueTransform, but first check for null so you don't throw an error applying the transform!  
                         if (descMap.decodedValue.is(null)) {
                              rValue <<[value:null]
                         } else if ((it.containsKey("valueTransform")) && (it.valueTransform instanceof Closure)) {
                              rValue << [value:(it.valueTransform(descMap.decodedValue))] // if valueTransform is a closure, apply the transform Closure to the data received from the node
                         } else {
                              rValue << [value: (descMap.decodedValue)]  // else just copy the decoded value
                        }

						rValue << ( it.units ? [units:(it.units)]  : [:] )

						// Now let's form a descriptionText string
                        // If you have a descriptionText field and it is a closure, then form the description text using
                        // that Closure supplied with the event's value (the value then can be used in the description)
                        // Else, for a description string using the attribute name and add the value
                          String newDescription
                          if (it.descriptionText && (it.descriptionText instanceof Closure)) {
                              newDescription = it.descriptionText(rValue.value)
                          } else {
                                newDescription = "${StringUtils.splitByCharacterTypeCamelCase(rValue.name).join(" ")} attribute set to ${rValue.value}"
                                if (it.units) { newDescription = newDescription + " " + it.units }
                          }
                        rValue << ( [descriptionText:newDescription])
						rValue << ( it.isStateChange ? [isStateChange:true]  : [:] ) // Was an isStateChange clause stated, if so, copy it if it is true. False is implied.
                        rValue << ( [clusterInt : (descMap.clusterInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device
                        rValue << ( [attrInt : (descMap.attrInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra attribute info for external device
                        rValue << ( [endpointInt : (descMap.endpointInt)]) // Event is sent on Hubitat's Event stream to external devices, so let's include some extra cluster info for external device
                        rValue << ( [jsonValue: (new JsonBuilder(descMap.decodedValue)) ]) // Event is sent on Hubitat's Event stream to external devices, so let's include original data in JSON form for external device
					}
        return rEvents
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing getHubitatEvents inputs ${descMap}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }     
}

