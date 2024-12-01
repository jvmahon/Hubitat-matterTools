/*
This has been pared down from matterTools.createListOfMatterSendEventMaps.  The original would also work!
*/
library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Create Hubitat Events from Matter Attribute Data for Inovelli VTM31-SN",
        name: "createListOfMatterSendEventMaps",
        namespace: "inovelliTools",
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
@Field static Map globalAllAttributeEventsMap = [ // Map of clusterInt provides Map of attributeInt provides List of one or more Maps of events
    0x0003:[ // Identify Cluster
        0x0000:[[attribute:"IdentifyTime", units:"seconds"				]],
        0x0001:[[attribute:"IdentifyType", valueTransform: { [0:"None", 1:"LightOutput", 2:"VisibleIndicator", 3:"AudibleBeep", 4:"Display", 5:"Actuator"].get(it) } ]],
    ],
    0x0006:[ // Switch Cluster
        0x0000:[[attribute:"switch",             valueTransform: { it ? "on" : "off" }     		]],
    ],
    0x0008:[ // Level Cluster
        0x0000:[[attribute:"level",                valueTransform: this.&HexToPercent, 	    units:"%"       	]],
        0x0001:[[attribute:"RemainingTime",        valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0010:[[attribute:"OnOffTransitionTime",  valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0011:[[attribute:"OnLevel",              valueTransform: this.&HexToPercent,  	units:"%"       	]],
        0x0012:[[attribute:"OnTransitionTime",     valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0013:[[attribute:"OffTransitionTime",    valueTransform: this.&toTenths,    		units:"seconds" 	]],
        0x0014:[[attribute:"DefaultMoveRate",    						                                        ]],
    ],
    0x003B:[ // Generic Switch Cluster
        0x0002:[[attribute:"MultiPressMax",        		]],
        ],
    0x0050: [ // Mode Select Cluster
        0x0000:[[attribute:"Description"          ]],
        0x0002:[[attribute:"SupportedModes"       ]],
        0x0003:[[attribute:"CurrentMode"         ]],
        ],
    0x0202:[ // Fan Control Cluster
        0x0000:[[attribute:"speed",                valueTransform: { ["off", "low", "medium", "high","on", "auto", "smart" ].get(it)  }       	]],
        0x0003:[[attribute:"level",       units:"%" 	]],
        0x0004:[[attribute:"SpeedMax",]],
        0x0005:[[attribute:"SpeedSetting",]],
        ],
    0x0300:[ // Color Control Cluster.  Only covering the most common ones for Hue at the moment!
		0x0000:[ // Hue
			    [attribute:"hue",  valueTransform: this.&HexToPercent, units:"%" 	], 	//  This is the Hubitat name/value
               ],
        0x0001:[[attribute:"saturation", valueTransform: this.&HexToPercent, units:"%"  ],  	//  This is the Hubitat name/value
               ],
        ],
]


List getHubitatEventsFromAttributeReport(Map descMap) {
    try {
        Integer retrieveThisCluster = descMap.clusterInt
        
        List rEvents = globalAllAttributeEventsMap.get(retrieveThisCluster)
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

