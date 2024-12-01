# Inovelli VTM31SN Hubitat Driver (Custom)
Additional Details to Come


## A Word of Caution
This driver is experimental. Use at your own risk.

The Inovelli VTM31 can also be used with Hubitat's built-in "Matter Generic Driver" driver. That driver will allow basic control of the dimming endpoint.


## Features
This driver supports several advanced features for the VTM31 SN including:
* Support for controlling (a) the main electrical dimmer Load as well (b) separately controlling the LED bar in "Alert" mode.
* Ability to configure device features from the driver
* Button Tapping (coming in a future update, after Hubitat gets further along in their Matter implementation)

At this time, Hubitat's Matter implementation is still under early development and there are a number of issues that remain for them to resolve. 
There appear to be some issues with subscribing to device reports and other features, so reliability may be an issue.

## Installation

1 Install all 4 files in your Hubitat "Driver Code" library, here:

![image](https://github.com/user-attachments/assets/d5a1fda5-8b60-4eb7-9653-23e5dd1c72bb)

2 Pair your  VTM31SN to Hubitat.  It will install with a default driver of "Device". You are going to need to change this:
![image](https://github.com/user-attachments/assets/c9c521ce-ea01-45b4-93e2-fea2b8110d24)

3 Change the default driver to "Inovelli VTM31-SN Advanced Device Driver and save the change":
![image](https://github.com/user-attachments/assets/be97cb3f-4a83-4573-90c9-aed7f3ee0178)

4 At the top of the page, select "Initialize", then wait about 15 seconds and refresh your screen (Ctrl + F5 for Chrome).
![image](https://github.com/user-attachments/assets/df75cf1f-31ad-4fc9-b506-6935c39d73cd)

5. If everything installed correctly, you will see 2 new child devices shown at the bottom of the screen
![image](https://github.com/user-attachments/assets/cb973d04-dd73-4396-b399-6a64e5e3fd98)
These two child devices are how you will control the Inovelli switch.
* The first child device ("Load Control") is used for control of the load (i.e., your lights!).
* The second child device ("LED Alert Strip") allows you to control the LED bar as an alert indicator. For example, changing its color to indicate an alarm or other event happening in your home.
  
6. Also, if everthing went correctly, your "Preferences" section will show controls for setting device configuration features.
  ![image](https://github.com/user-attachments/assets/e9245157-6fb0-443f-a841-27600449125e)

7. If each of these controls displays a message saying to Refresh (rather than displaying what the control does), you need to click the "Refresh" button on again.
