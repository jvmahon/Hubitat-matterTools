library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Create Hubitat Events from Matter Attribute Data",
        name: "matterEnumTypes",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import groovy.transform.Field
import groovy.transform.CompileStatic

// Identify Cluster 0x0003 Enum Data Types (Matter Cluster Spec. Section 1.2.5)
@Field static Map IdentifyTypeEnum =   [ 0:"None",     1:"LightOutput",    2:"VisibleIndicator",    3:"AudibleBeep",    4:"Display",    5:"Actuator"] // Cluste 0x0003
@Field static Map EffectIdentifierEnumType = [ 0:"Blink", 1:"Breathe", 2:"Okay", 0x0B:"ChannelChange", 0xFE:"FinishEffect", 0xFF:"StopEffect"]
@Field static Map EffectVariantEnumType = [0:"Default"]

// OnOff Cluster 0x0006 Enum Data Types (Matter Cluster Spec. Section 1.2.5)
@Field static Map StartUpOnOffEnum =   [ 0:"Off",     1:"On",    2:"Toggle"] // Cluster 0x0006
@Field static Map EffectIdentifierEnum = [0:"DelayedAllOff", 1:"DyingLIght"]
@Field static Map DelayedAllOffEffectVariantEnum = [0:"DelayedAllOffEffectVariantEnum", 1:"NoFade", 2:"DelayedOffSlowFade"]
@Field static Map DyingLightEffectVariantEnumType = [0:"DyingLightFadeOff"]

// Level Cluster 0x0008 Enum Data Types (Matter Cluster Spec. Section 1.6.5)
// Haven't needed any yet, so not added!

// Color Cluster 0x0300 Enum Data Types (Matter Cluster Spec. Section 3.2)
// None Defined!

// Basic Information Cluster 0x0028 Enum Data Types (Matter Cluster Spec. Section 11.1.4)
@Field static Map ProductFinishEnumType = [0:"Other", 1:"Matte", 2:"Satin", 3:"Polished", 4:"Rugged", 5:"Fabric"]
@Field static Map ColorEnumType = [
	0:"Black", 1:"Navy", 2:"Green", 3:"Teal", 4:"Maroon", 5:"Purple", 6:"Olive", 7:"Gray", 
	8:"Blue", 9:"Lime", 10:"Aqua", 11:"Red", 12:"Fuscia", 13:"Yellow", 14:"White", 
	15:"Nickel", 16:"Chrome", 17:"Brass", 18:"Copper", 19:"Silver", 20:"Gold"
    ]

// Air Quality Cluster 0x005B Enum Data Types (Matter Cluster Spec. Section 2.9.5)
@Field static Map AirQualityEnumType = [ 0:"Unknown",     1:"Good",    2:"Fair",    3:"Moderate",    4:"Poor",    5:"VeryPoor",    6:"ExtremelyPoor"]

// Concentration Measurement Clusters (Matter Cluster Spec. Section 2.10.5)
@Field static Map MeasurementUnitEnum =   [0:"PPM", 1:"PPB", 2:"PPT", 3:"MGM3", 4:"UGM3", 5:"NGM3", 6:"PM3", 7:"BQM3"]
@Field static Map MeasurementMediumEnum =  [0:"Air", 1:"Water", 2:"Soil"]
@Field static Map LevelValueEnum =   [0:"Unknown", 1:"Low", 2:"Medium", 3:"High", 4:"Critical"]

// Smoke CO Alarm Cluster (Matter Cluster Spec. Section 2.11.5)
@Field static Map AlarmStateEnum = [0:"Normal", 1:"Warning", 2:"Critical" ]
@Field static Map SensitivityEnum = [0:"High", 1:"Standard", 2:"Low"]
@Field static Map ExpressedStateEnum = [0:"Normal", 1:"SmokeAlarm", 2:"COAlarm", 3:"BatteryAlert", 4:"Testing", 5:"HardwareFault", 6:"EndOfService", 7:"InterconnectSmoke", 8:"InterconnectCO"]
@Field static Map MuteStateEnum = [0:"NotMuted", 1:"Muted" ]
@Field static Map EndOfServiceEnum = [0:"Normal", 1:"Expired"]
@Field static Map ContaminationStateEnum = [0:"Normal", 1:"Low", 2:"Warning", 3:"Critical" ]

// Thread Network Diagnostics Cluster  (Matter **Core** Spec. Section 11.13.5)
@Field static Map NetworkFaultEnum = [0:"Unspecified", 1:"LinkDown", 2:"HardwareFailure", 3:"NetworkJammed"]
// ConnectionStatusEnum - same as WiFi
@Field static Map RoutingRoleEnumType = [0:"Unspecified", 1:"Unassigned", 2:"SleepyEndDevice", 3:"EndDevice", 4:"REED", 5:"Router", 6:"Leader"]
// (Many other types not included)


// Wi-Fi NEtwork Diagnostics Cluster  (Matter **Core** Spec. Section 11.14.5)
@Field static Map SecurityTypeEnum = [0:"Unspecified", 1:"None", 2:"WEP", 3:"WPA", 4:"WPA2", 5:"WPA3"]
@Field static Map WiFiVersionEnum = [0:"a", 1:"b", 2:"g", 3:"n", 4:"ac", 5:"ax", 6:"ah"]
@Field static Map AssociationFailureCauseEnum = [0:"Unknown", 1:"AssociationFailed", 2:"AuthenticationFailed", 3:"SsidNotFound"]
@Field static Map ConnectionStatusEnum = [0:"Connected", 1:"NotConnected"]

// PowerSource Cluster  (Matter **Core** Spec. Section 11.7.5)
@Field static Map WiredFaultEnum = [0:"Unspecified", 1:"OverVoltage", 2:"UnderVoltage"]
@Field static Map BatFaultEnum = [0:"Unspecified", 1:"OverTemp", 2:"UnderTemp"]
@Field static Map BatChargeFaultEnum = [
	0:"Unspecified", 1:"AmbientTooHot", 2:"AmbientTooCold", 3:"BatteryTooHot", 4:"BatteryTooCold", 5:"BatteryAbsent", 
	6:"BatteryOverVoltage", 7:"BatteryUnderVoltage", 8:"ChargerOverVoltage", 9:"ChargerUnderVoltage", 10:"SafetyTimeout", 
	11:"ChargerOverCurrent", 12:"UnexpectedVoltage", 13:"ExpectedVoltage", 14:"GroundFault", 15:"ChargeSignalFailure", 16:"SafetyTimeout" 
    ]
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
@Field static Map BatChargeStateEnum = [ 0:"Unknown", 1:"IsCharging", 2:"IsAtFullCharge", 3:"IsNotCharging", 4:"IsDischarging", 5:"IsTransitioning", ]
