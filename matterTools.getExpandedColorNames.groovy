library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Color Name Transform",
        name: "getExpandedColorNames",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import groovy.transform.Field

@Field static List colorNames = [
	[name:"Alice Blue",    		r:0xF0, g:0xF8, b:0xFF],
	[name:"Antique White",    	r:0xFA, g:0xEB, b:0xD7],
	[name:"Aqua",    			r:0x00, g:0xFF, b:0xFF], // Matter Spec 11.1.4.2
	[name:"Aquamarine",    		r:0x7F, g:0xFF, b:0xD4],
	[name:"Azure",    			r:0xF0, g:0xFF, b:0xFF],
	[name:"Beige",    			r:0xF5, g:0xF5, b:0xDC],
	[name:"Bisque",    			r:0xFF, g:0xE4, b:0xC4],
	[name:"Black",    			r:0x00, g:0x00, b:0x00], // Matter Spec 11.1.4.2
	[name:"Blanched Almond",    r:0xFF, g:0xEB, b:0xCD],
	[name:"Blue",    			r:0x00, g:0x00, b:0xFF], // Matter Spec 11.1.4.2
	[name:"Blue Violet",    	r:0x8A, g:0x2B, b:0xE2],
	[name:"Brown",    			r:0xA5, g:0x2A, b:0x2A],
	[name:"Burly Wood",    		r:0xDE, g:0xB8, b:0x87],
	[name:"Cadet Blue",    		r:0x5F, g:0x9E, b:0xA0],
	[name:"Chartreuse",    		r:0x7F, g:0xFF, b:0x00],
	[name:"Chocolate",    		r:0xD2, g:0x69, b:0x1E],
	[name:"Coral",    			r:0xFF, g:0x7F, b:0x50],
	[name:"Cornflower Blue",    r:0x64, g:0x95, b:0xED],
	[name:"Cornsilk",    		r:0xFF, g:0xF8, b:0xDC],
	[name:"Crimson",    		r:0xDC, g:0x14, b:0x3C],
	[name:"Cyan",    			r:0x00, g:0xFF, b:0xFF],
	[name:"Dark Blue",    		r:0x00, g:0x00, b:0x8B],
	[name:"Dark Cyan",    		r:0x00, g:0x8B, b:0x8B],
	[name:"Dark Goldenrod",    	r:0xB8, g:0x86, b:0x0B],
	[name:"Dark Gray",    		r:0xA9, g:0xA9, b:0xA9],
	[name:"Dark Green",    		r:0x00, g:0x64, b:0x00],
	[name:"Dark Khaki",    		r:0xBD, g:0xB7, b:0x6B],
	[name:"Dark Magenta",    	r:0x8B, g:0x00, b:0x8B],
	[name:"Dark Olive Green",   r:0x55, g:0x6B, b:0x2F],
	[name:"Dark Orange",    	r:0xFF, g:0x8C, b:0x00],
	[name:"Dark Orchid",    	r:0x99, g:0x32, b:0xCC],
	[name:"Dark Red",    		r:0x8B, g:0x00, b:0x00],
	[name:"Dark Salmon",    	r:0xE9, g:0x96, b:0x7A],
	[name:"Dark Sea Green",    	r:0x8F, g:0xBC, b:0x8F],
	[name:"Dark Slate Blue",    r:0x48, g:0x3D, b:0x8B],
	[name:"Dark Slate Gray",    r:0x2F, g:0x4F, b:0x4F],
	[name:"Dark Turquoise",    	r:0x00, g:0xCE, b:0xD1],
	[name:"Dark Violet",    	r:0x94, g:0x00, b:0xD3],
	[name:"Deep Pink",    		r:0xFF, g:0x14, b:0x93],
	[name:"Deep Sky Blue",    	r:0x00, g:0xBF, b:0xFF],
	[name:"Dim Gray",    		r:0x69, g:0x69, b:0x69],
	[name:"Dodger Blue",    	r:0x1E, g:0x90, b:0xFF],
	[name:"Fire Brick",    		r:0xB2, g:0x22, b:0x22],
	[name:"Floral White",    	r:0xFF, g:0xFA, b:0xF0],
	[name:"Forest Green",    	r:0x22, g:0x8B, b:0x22],
	[name:"Fuchsia",    		r:0xFF, g:0x00, b:0xFF], // Matter Spec 11.1.4.2
	[name:"Gainsboro",    		r:0xDC, g:0xDC, b:0xDC],
	[name:"Ghost White",    	r:0xF8, g:0xF8, b:0xFF],
	[name:"Gold",    			r:0xFF, g:0xD7, b:0x00],
	[name:"Goldenrod",    		r:0xDA, g:0xA5, b:0x20],
	[name:"Gray",    			r:0x80, g:0x80, b:0x80], // Matter Spec 11.1.4.2
	[name:"Green",    			r:0x00, g:0x80, b:0x00], // Matter Spec 11.1.4.2
	[name:"Green Yellow",    	r:0xAD, g:0xFF, b:0x2F],
	[name:"Honey Dew",    		r:0xF0, g:0xFF, b:0xF0],
	[name:"Hot Pink",    		r:0xFF, g:0x69, b:0xB4],
	[name:"Indian Red",    		r:0xCD, g:0x5C, b:0x5C],
	[name:"Indigo",    			r:0x4B, g:0x00, b:0x82],
	[name:"Ivory",    			r:0xFF, g:0xFF, b:0xF0],
	[name:"Khaki",    			r:0xF0, g:0xE6, b:0x8C],
	[name:"Lavender",    		r:0xE6, g:0xE6, b:0xFA],
	[name:"Lavender Blush",    	r:0xFF, g:0xF0, b:0xF5],
	[name:"Lawn Green",    		r:0x7C, g:0xFC, b:0x00],
	[name:"Lemon Chiffon",    	r:0xFF, g:0xFA, b:0xCD],
	[name:"Light Blue",    		r:0xAD, g:0xD8, b:0xE6],
	[name:"Light Coral",    	r:0xF0, g:0x80, b:0x80],
	[name:"Light Cyan",    		r:0xE0, g:0xFF, b:0xFF],
	[name:"Light Goldenrod Yellow", r:0xFA, g:0xFA, b:0xD2],
	[name:"Light Gray",    		r:0xD3, g:0xD3, b:0xD3],
	[name:"Light Green",    	r:0x90, g:0xEE, b:0x90],
	[name:"Light Pink",    		r:0xFF, g:0xB6, b:0xC1],
	[name:"Light Salmon",    	r:0xFF, g:0xA0, b:0x7A],
	[name:"Light SeaGreen",    	r:0x20, g:0xB2, b:0xAA],
	[name:"Light SkyBlue",    	r:0x87, g:0xCE, b:0xFA],
	[name:"Light SlateGray",    r:0x77, g:0x88, b:0x99],
	[name:"Light SteelBlue",    r:0xB0, g:0xC4, b:0xDE],
	[name:"Light Yellow",    	r:0xFF, g:0xFF, b:0xE0],
	[name:"Lime",    			r:0x00, g:0xFF, b:0x00], // Matter Spec 11.1.4.2
	[name:"LimeGreen",    		r:0x32, g:0xCD, b:0x32],
	[name:"Linen",    			r:0xFA, g:0xF0, b:0xE6],
	[name:"Magenta",    		r:0xFF, g:0x00, b:0xFF],
	[name:"Maroon",    			r:0x80, g:0x00, b:0x00], // Matter Spec 11.1.4.2 - Error in Matter Spec. 1.2 - Purple / Maroon are set the same in the spec! Value here is correct!
	[name:"Medium Aqua Marine", r:0x66, g:0xCD, b:0xAA],
	[name:"Medium Blue",    	r:0x00, g:0x00, b:0xCD],
	[name:"Medium Orchid",    	r:0xBA, g:0x55, b:0xD3],
	[name:"Medium Purple",    	r:0x93, g:0x70, b:0xDB],
	[name:"Medium Sea Green",   r:0x3C, g:0xB3, b:0x71],
	[name:"Medium Slate Blue",  r:0x7B, g:0x68, b:0xEE],
	[name:"Medium Spring Green", r:0x00, g:0xFA, b:0x9A],
	[name:"Medium Turquoise",   r:0x48, g:0xD1, b:0xCC],
	[name:"Medium Violet Red",  r:0xC7, g:0x15, b:0x85],
	[name:"Midnight Blue",    	r:0x19, g:0x19, b:0x70],
	[name:"Mint Cream",    		r:0xF5, g:0xFF, b:0xFA],
	[name:"Misty Rose",    		r:0xFF, g:0xE4, b:0xE1],
	[name:"Moccasin",    		r:0xFF, g:0xE4, b:0xB5],
	[name:"Navajo White",    	r:0xFF, g:0xDE, b:0xAD],
	[name:"Navy",    			r:0x00, g:0x00, b:0x00], // Matter Spec 11.1.4.2
	[name:"Old Lace",    		r:0xFD, g:0xF5, b:0xE6],
	[name:"Olive",    			r:0x80, g:0x80, b:0x00],  // Matter Spec 11.1.4.2
	[name:"Olive Drab",    		r:0x6B, g:0x8E, b:0x23],
	[name:"Orange",    			r:0xFF, g:0xA5, b:0x00],
	[name:"Orange Red",    		r:0xFF, g:0x45, b:0x00],
	[name:"Orchid",    			r:0xDA, g:0x70, b:0xD6],
	[name:"Pale Goldenrod",    	r:0xEE, g:0xE8, b:0xAA],
	[name:"Pale Green",    		r:0x98, g:0xFB, b:0x98],
	[name:"Pale Turquoise",    	r:0xAF, g:0xEE, b:0xEE],
	[name:"Pale Violet Red",    r:0xDB, g:0x70, b:0x93],
	[name:"Papaya Whip",   		r:0xFF, g:0xEF, b:0xD5],
	[name:"Peach Puff",    		r:0xFF, g:0xDA, b:0xB9],
	[name:"Peru",    			r:0xCD, g:0x85, b:0x3F],
	[name:"Pink",    			r:0xFF, g:0xC0, b:0xCB],
	[name:"Plum",    			r:0xDD, g:0xA0, b:0xDD],
	[name:"Powder Blue",    	r:0xB0, g:0xE0, b:0xE6],
	[name:"Purple",    			r:0x80, g:0x00, b:0x80], // Matter Spec 11.1.4.2
	[name:"Red",    			r:0xFF, g:0x00, b:0x00], // Matter Spec 11.1.4.2
	[name:"Rosy Brown",    		r:0xBC, g:0x8F, b:0x8F],
	[name:"Royal Blue",    		r:0x41, g:0x69, b:0xE1],
	[name:"Saddle Brown",    	r:0x8B, g:0x45, b:0x13],
	[name:"Salmon",    			r:0xFA, g:0x80, b:0x72],
	[name:"Sandy Brown",    	r:0xF4, g:0xA4, b:0x60],
	[name:"Sea Green",    		r:0x2E, g:0x8B, b:0x57],
	[name:"Seashell",    		r:0xFF, g:0xF5, b:0xEE],
	[name:"Sienna",    			r:0xA0, g:0x52, b:0x2D],
	[name:"Silver",    			r:0xC0, g:0xC0, b:0xC0],
	[name:"Sky Blue",    		r:0x87, g:0xCE, b:0xEB],
	[name:"Slate Blue",    		r:0x6A, g:0x5A, b:0xCD],
	[name:"Slate Gray",    		r:0x70, g:0x80, b:0x90],
	[name:"Snow",    			r:0xFF, g:0xFA, b:0xFA],
	[name:"Spring Green",    	r:0x00, g:0xFF, b:0x7F],
	[name:"Steel Blue",    		r:0x46, g:0x82, b:0xB4],
	[name:"Tan",    			r:0xD2, g:0xB4, b:0x8C],
	[name:"Teal",    			r:0x00, g:0x80, b:0x80], // Matter Spec 11.1.4.2
	[name:"Thistle",    		r:0xD8, g:0xBF, b:0xD8],
	[name:"Tomato",    			r:0xFF, g:0x63, b:0x47],
	[name:"Turquoise",    		r:0x40, g:0xE0, b:0xD0],
	[name:"Violet",    			r:0xEE, g:0x82, b:0xEE],
	[name:"Wheat",    			r:0xF5, g:0xDE, b:0xB3],
	[name:"White",    			r:0xFF, g:0xFF, b:0xFF], // Matter Spec 11.1.4.2
	[name:"White Smoke",    	r:0xF5, g:0xF5, b:0xF5],
	[name:"Yellow",    			r:0xFF, g:0xFF, b:0x00], // Matter Spec 11.1.4.2
	[name:"Yellow Green",    	r:0x9A, g:0xCD, b:0x32],
]

String getColorNameFromRGB(Map inputs = [:] ) {
    assert inputs.r instanceof Integer && inputs.g instanceof Integer && inputs.b instanceof Integer
    assert inputs.r >=0 && inputs.r <=0xFF 
    assert inputs.g >=0 && inputs.g <=0xFF
    assert inputs.b >=0 && inputs.b <=0xFF
    // Find mininum distance to a color using next nearest neighbor calculation - don't need to do the squareroot part though
    // Geometric distance between two points in 3D space is calculated as SQRT( (X-x)**2 + (Y-y)**2 + (Z-z)**) where X,Y,Z is a first point 
    // and (x,y,z) is the second.  That concept is used to find distance between colors.
    // Can skip applying the SQRT part in this case, as the minimum can be determined without it
   return colorNames.min { (inputs.r - it.r)**2 + (inputs.g - it.g)**2 + (inputs.b - it.b)**2 }.name
}

String getColorNameFromHSV(Map inputs = [:]){
    assert inputs.hue instanceof Integer && inputs.saturation instanceof Integer && inputs.level instanceof Integer
    inputs.hue %= 100 // hue maps to degrees on a circle. Hubitat treats it as a % with each % being 3.6 degres. Values greater than 100% wrap around so Mod by 100 gets to same place
    if (inputs.hue < 0) inputs.hue += 100 // hue maps to degrees on a circle. A negative value gets to the same place by adding 100% and treating as a positive value
    List rgbList = hubitat.helper.ColorUtils.hsvToRGB([inputs.hue, inputs.saturation, inputs.level])
    Map colors = [r:(rgbList[0]), g:(rgbList[1]), b:(rgbList[2])]
    getColorNameFromRGB(colors)
}