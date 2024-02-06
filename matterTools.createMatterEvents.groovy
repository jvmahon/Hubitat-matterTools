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

@Field static Closure toInt = { v -> Integer.parseInt(v, 16)} // Does a hex to Integer conversion.
@Field static Closure computeLux = { v ->Math.pow( 10, ((Integer.parseInt(v, 16) -1) / 10000) ) as Integer} // Converts Mireds to Kelvin
@Field static Closure scaleHueSat = { v -> Math.round( v / 2.54) }
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
    (97..101):'Red'
]
@Field static Closure HuePercent2Name = { hue -> (colorRGBName.find{ entry -> entry.key.contains(hue as Integer)}).value}
@Field static Closure HexHue2Name = {hex -> HuePercent2Name(scaleHueSat(toInt(hex))) }
@Field static Closure HexTempToName = {pv -> "DummyName"}
@Field static Closure HexMiredsToKelvin =  {pv -> 1000000 / toInt(pv)}
/*
For Closure values in the following structure:
pv = parsed Map value field (descMap.value)
dn = device name - provided as a string
dv = device value - usually the content of the event map's "value" field after pv has been converted by the closure in the "value" field, below..
*/
@Field static Map globalAllEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events
    0x0006:[
        0x0000:[[attribute:"switch",         value:{ pv -> toInt(pv) ? "on" : "off" }, descriptionText: {dn, dv ->  "${dn}: turned ${dv}"}],
                [attribute:"OnOff",          value:{ pv -> toInt(pv) ? true : false }, descriptionText: {dn, dv ->  "${dn}: On State ${dv}"}]],
        0x4001:[[attribute:"OnTime",         value:{ pv -> toInt(pv) / 10 },     units:"seconds" ]],
        0x4002:[[attribute:"OffWaitTime",    value:{ pv -> toInt(pv) / 10 },    units:"seconds" ]],
        0x4003:[[attribute:"StartUpOnOff",   value:{ pv -> ["00":"Off", "01":"On", "02":"Toggle"].get(pv)}]],
    ],
    0x0008:[
        0x0000:[[attribute:"level",         value:{ pv -> Math.round(toInt(pv)/2.54) },     Units:"%",  descriptionText: {dn, dv ->  "${dn}: turned ${dv}"}],
                [attribute:"CurrentLevel",  value:{ pv -> toInt(pv)},          units:"raw", descriptionText: {dn, dv ->  "${dn}: On State boolean: ${dv}"}]],
        0x0001:[[attribute:"RemainingTime", value:{ pv -> toInt(pv) / 10 },    units:"seconds" ]],
        0x0002:[[attribute:"MinLevel",      value:{ pv -> toInt(pv) / 10 },    units:"seconds" ]],
        0x0003:[[attribute:"MaxLevel", value:{ pv -> ["00":"Off", "01":"On", "02":"Toggle"].get(pv)}]],
    ],
    0x0300:[
		0x0000: [ // Hue
			[attribute:"hue", value:{pv -> scaleHueSat(toInt(pv))}, units:"%", descriptionText:{dn, dv ->  "${dn}: set to hue ${dv}%"}],
			[attribute:"colorName", value:{pv -> HexHue2Name(pv) }, descriptionText:{dn, dv ->  "${dn}: set to ${dv}"}]
			],
        0x0001: [ [attribute:"saturation", value: {pv -> scaleHueSat(toInt(pv))}, units:"%", descriptionText: {dn, dv ->  "${dn}: set to Saturation ${dv}%"}] ],
        0x0002: [ [attribute:"remainingColorTransitionTime", value: {pv -> toInt(pv)/10}, units:"seconds", descriptionText: {dn, dv ->  "${dn}: Remaining Color Transition Time ${dv} seconds"} ] ],
        0x0007: [ [attribute:"colorTemperature", value: {pv -> HexMiredsToKelvin(pv)}, descriptionText: {dn, dv ->  "${dn}: was set to new color temperature: ${dv} Kelvin"}, unit: "°K"], 
                  [attribute:"colorName", value: {pv -> HexTempToName(pv)}, descriptionText: {dn, dv ->  "${dn}: Color Name set to ${dv}"}] ],
        0x0008: [[attribute:"colorMode", value: {pv -> [0:"RGB", 1:"CurrentXY", 2:"CT"].get(toInt(pv))}, descriptionText: {dn, dv ->  "${dn}: Color Name set to ${dv}"}] ],
        0x400A: [], 
        0x400B: [], 
        0x400C: []
        ],
    0x0400:[ // Illuminance Measurement
        0x0000:[ [attribute:"illuminance",             value:{pv -> this.computeLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
                 [attribute:"MeasuredValue",           value:{pv -> this.computeLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
               ],
        0x0001:[ [attribute:"MinMeasuredValueLux",     value:{pv -> computeLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Minimum measurable lx is ${dv}"}],  	],
        0x0002:[ [attribute:"MaxMeasuredValueLux",     value:{pv -> computeLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Maximum measurable lx is ${dv}"}],    	],
        0x0003:[ [attribute:"LuxMeasurementTolerance", value:{pv -> computeLux(pv)}, units:"lx", descriptionText:{dn, dv -> "${dn}: Measurement tolerance lx is ${dv}"}], 	],
        0x0004:[ [attribute:"LightSensorType", value:{pv -> [0:"Photodiode", 1:"CMOS"].get(toInt(pv)) },isStateChange:false] ], 
        ],
    0x0402:[ // Temperature Measurement
        0x0000:[ [attribute:"illuminance",          value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
                 [attribute:"TempMeasuredValue",    value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measured lx is ${dv}" }],
               ],
		0x0001:[ [attribute:"TempMinMeasuredValue", value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Minimum measurable lx is ${dv}"}],  	],
        0x0002:[ [attribute:"TempMaxMeasuredValue", value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Maximum measurable lx is ${dv}"}],    	],
        0x0003:[ [attribute:"TempTolerance", 		value:{pv -> toInt(pv)}, units:"C", descriptionText:{dn, dv -> "${dn}: Measurement tolerance lx is ${dv}"}], 	],
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
                [attribute:"Occupancy",             	value:{pv -> toInt(pv)}, units:"bitmap", descriptionText:{dn, dv -> "${dn}: Occupancy bitmap: 0b${Integer.toBinaryString(dv)}"}]],
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
    0x005B:[ // Air Quality
        0x0000:[[attribute:"AirQuality", value:{pv -> [0:"Unknown", 1:"Good", 2:"Fair", 3:"Moderate", 4:"Poor", 5:"VeryPoor", 6:"ExtremelyPoor"].get(toInt(pv)) }, descriptionText:{dn, dv -> "${dn}: Air Quality is ${dv}"}], ],
    ],   
	0x005C:[ // Smoke and CO Alarm
        0x0000:[[attribute:"ExpressedState", 	value:{pv -> [0:"Normal", 1:"SmokeAlarm", 2:"COAlarm", 3:"BatteryAlert", 4:"Testing", 5:"HardwareFault", 6:"EndOfService", 7:"InterconnectSmoke", 8:"InterconnectCO"].get(toInt(pv))}, descriptionText:{dn, dv -> "${dn}: Expressed State is: ${dv}"}]],
        0x0001:[[attribute:"SmokeState", 		value:{pv -> [0:"Normal", 1:"Warning", 2:"Critical" ].get(toInt(pv))}, 	descriptionText:{dn, dv -> "${dn}: Smoke State is: ${dv}"}]],
        0x0002:[[attribute:"COState", 			value:{pv -> [0:"Normal", 1:"Warning", 2:"Critical"].get(toInt(pv))}, 	descriptionText:{dn, dv -> "${dn}: CO State is: ${dv}"}]],
        0x0003:[[attribute:"BatteryAlert", 		value:{pv -> [0:"Normal", 1:"Warning", 2:"Critical"].get(toInt(pv))}, 	descriptionText:{dn, dv -> "${dn}: Battery Alert is: ${dv}"}]],
        0x0004:[[attribute:"DeviceMuted", 		value:{pv -> [0:"NotMuted", 1:"Muted" ].get(toInt(pv))}, 	descriptionText:{dn, dv -> "${dn}: Device is: ${dv}"}]],
        0x0005:[[attribute:"TestInProgress", 			value:{pv -> toInt(pv) as Boolean }, 		descriptionText:{dn, dv -> "${dn}: Test In Progress is: ${dv}"}]],
        0x0006:[[attribute:"HardwareFaultAlert",		value:{pv -> toInt(pv) as Boolean }, 		descriptionText:{dn, dv -> "${dn}: Hardware Fault is: ${dv}"}]],
        0x0007:[[attribute:"EndOfServiceAlert", 		value:{pv -> [0:"Normal", 1:"Expired"].get(toInt(pv))}, 	descriptionText:{dn, dv -> "${dn}: Service Lifetime is: ${dv}"}]],
        0x0008:[[attribute:"InterconnectSmokeAlarm", 	value:{pv -> [0:"Normal", 1:"Warning", 2:"Critical"].get(toInt(pv))}, 			descriptionText:{dn, dv -> "${dn}: Interconnect Smoke Alarm State is: ${dv}"}]],
        0x0009:[[attribute:"InterconnectCOAlarm", 		value:{pv -> [0:"Normal", 1:"Warning", 2:"Critical"].get(toInt(pv))}, 			descriptionText:{dn, dv -> "${dn}: Interconnect CO Alarm State State is: ${dv}"}]],
        0x000A:[[attribute:"ContaminationState", 		value:{pv -> [0:"Normal", 1:"Low", 2:"Warning", 3:"Critical" ].get(toInt(pv))}, descriptionText:{dn, dv -> "${dn}: Contamination State is: ${dv}"}]],
        0x000B:[[attribute:"SmokeSensitivityLevel", 	value:{pv -> [0:"High", 1:"Standard", 2:"Low"].get(toInt(pv))}, 				descriptionText:{dn, dv -> "${dn}: Smoke Sensitivity Level is: ${dv}"}]],
        0x000C:[[attribute:"ExpiryDate", 				value:{pv -> pv}, units:"epoch-s", 			descriptionText:{dn, dv -> "${dn}: Expiry date epoch-s is: ${dv}"}]],
    ],
    // Concentration Measurement Cluster (Matter Spec Section 2.10) are too complex and beyond this library
]


List getHubitatEvents(Map descMap) {
    List rEvents = globalAllEventsMap.get(descMap.clusterInt)
		?.get(descMap.attrInt)
			?.collect{ Map rValue = [:]
						rValue << [name:it.attribute, value:it.value(descMap.value)] 
						rValue << ( it.units             ? [units:(it.units)]    : [:] )
						rValue << ( it.descriptionText   ? [descriptionText:it.descriptionText(device.displayName, rValue.value)] : [:])
						rValue << ( it.isStateChange     ? [isStateChange:true]  : [:] )
					}
}

