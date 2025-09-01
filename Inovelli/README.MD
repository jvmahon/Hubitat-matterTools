# Inovelli White Series Hubitat Driver (Custom)
Additional Details to Come

This driver set supports the Inovelli VTM30, VTM31, VTM35, and VTM36 switches.

The Hubitat 2.4 firmware version has updated their interface. This documentation has not been updated yet.


## Features
This driver supports several advanced features for the VTM30, VTM31, VTM36 and VTM36 switches including:
* Support for controlling (a) the main electrical Load (switch for VTM30, dimmer for VTM31, and both dimmer and fan for VTM35/VTM36) as well (b) separately controlling the LED bar in "Alert" mode for the VTM30, VTM31, and VTM35.
* Ability to configure device features from the driver
* Button Tapping support (Single Tap, Double Tap, Hold, Release) for VTM30, VTM31, VTM35.
* Temperature and Humidity support for FTM30.

At this time, Hubitat's Matter implementation is still under early development and there are a number of issues that remain for them to resolve. 
There appear to be some issues with subscribing to device reports and other features, so reliability may be an issue.

## Installation

### New Installations

1 Unpack the Zip file and Install all of the driver files in your Hubitat "Driver Code" library, here:

<img width="200" alt="image" src="https://github.com/user-attachments/assets/1fb26f4a-b781-482c-aa31-bc128891cb71" />

2 Pair your  device to Hubitat.  Hubitat *should* select the correct driver ("Inovelli White Series Matter Advanced Device Driver"). Confirm (or update) the selected device driver following these steps:

First, select the device
<img width="1873" height="570" alt="image" src="https://github.com/user-attachments/assets/6e9a5f61-5b86-45a5-9614-cbb111a3e6a6" />

Then, from its "Device info" tab, confirm (or update) the driver type and save the change.
<img width="1897" height="657" alt="image" src="https://github.com/user-attachments/assets/336ef29e-6369-49e3-b0d0-8a0ac93e6a29" />

3 From the device's "Commands" tab, run the "Initialize" command, then wait about 15 seconds and refresh your screen (Ctrl + F5 for Chrome).
<img width="1878" height="672" alt="image" src="https://github.com/user-attachments/assets/b6470605-6f3b-430e-b316-b6419e5c04d8" />


5. If everything installed correctly, you will see 2 child devices on the "Child devices" tab.
<img width="1455" height="325" alt="image" src="https://github.com/user-attachments/assets/e3c0379c-dcd4-4bff-800a-db3646e4d4c1" />

For example, for the VTM31, there will be two child devices. These two child devices are how you will control the Inovelli switch.
* The first child device ("Load Control") is used for control of the load (i.e., your lights!).
* The second child device ("LED Notification Bar") allows you to control the LED bar as an alert indicator. For example, changing its color to indicate an alarm or other event happening in your home.
  
6. Also, if everthing went correctly, your "Preferences" section will show controls for setting device configuration features.
<img width="1861" height="776" alt="image" src="https://github.com/user-attachments/assets/35f0a93c-3d2c-440a-ac28-49fabf367193" />


7. If each of these controls displays a message saying to Refresh (rather than displaying what the control does), you need to run the "Initialize" command again.

### Updating a Prior Installation

1. Follow the instructions for "New Installations", then

2. If you have VTM31 switches, for the Load Control child device of each switch, change the driver to "Matter Generic Component Tapable Dimmer" to enable button tapping support.

3. If you have the VTM35 switch, for the Load Control child device of each switch, change the driver to "Matter Inovelli Fan Tapable Component" to enable button tapping support.

4. Restart Hubitat.

