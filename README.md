#VGCamera - valerino's glass camera
##Google glass camera as it should have been

#Features:
* Can be controlled totally hands-free (i.e. ideal for surgeons)
* Can be controlled via touchpad too
* Always shows what the camera is looking at (preview mode), no more shooting pictures blindly!
* Zoom, can take zoomed pictures and/or videos
* Configurable quality (high/low) for both videos and pictures
* Autosave (restart preview immediately after taken picture/video)
* Geotagging
* Sharing of the taken media on social networks / email
* Visible overlays to identify operation modes and selected options
* Stores the set configuration options across usages

#Modes of operation
There are 2 modes of operation : 

* 'Preview mode' (while recording or before taking a picture)
* 'Taken mode' (after recording has been stopped or picture taken).

Both modes can be controlled either handsfree and/or with the touchpad simultaneously (i.e. zoom with the touchpad, zoom more handsfree, take picture handsfree, save with the touchpad, and so on).

##Hands-free
###Preview mode
 1. Starts saying 'VGCamera', app will start in Preview mode
 2. Take picture with 'Ok Glass' -> 'Take a picture' as usual
 3. Start video recording with 'Ok Glass' -> 'Record a video' as usual
 4. Stop video recording with 'Ok Glass' -> 'Stop'
 5. Zoom with 'Ok Glass' -> 'Zoom' -> 'In' / 'Out'
 6. Toggle Maximum Zoom with 'Ok Glass' -> 'Zoom' -> 'Max ON' / 'Max OFF'
 7. Toggle Smooth Zoom with 'Ok Glass' -> 'Zoom' -> 'Smooth ON' / 'Smooth OFF'
 8. Toggle Autosave with 'Ok Glass' -> 'Options' -> 'Autosave ON' / 'Autosave OFF'    
 9. Toggle Geotagging with 'Ok glass' -> 'Options' -> 'Geotag ON' / 'Geotag OFF'    
 10. Toggle Overlay with 'Ok glass' -> 'Options' -> 'Overlay ON' / 'Overlay OFF'    
 11. Close app with 'Close'

###Taken mode
If Autosave is not set, after taking a picture (or video recording has been stopped), saying 'Ok Glass' you're presented with another menu which allows to:

1. 'Save' : save the taken media
2. 'Discard': discard the taken media and get back to preview mode
3. 'Share': share the taken media on social networks / email

##Touchpad
###Preview mode
1. Single finger tap on the touchpad shows scrollable configuration toggles for Geotagging, Autosave, Max zoom, Smooth zoom and Overlays
2. Swipe left/right zooms in/out
3. Double finger tap / Pressing the camera button takes a picture
4. Three finger tap / Long pressing the camera button starts video recording
5. While in video recording mode, long tap / Pressing the camera button stops recording

###Taken mode
If Autosave is not set, after taking a picture (or video recording has been stopped), single tapping the touchpad you're presented with scrollable cards to Save, Discard or Share the taken media.

#Currently implemented:
* Everything is fully functional for taking pictures and recording videos (saved in /sdcard/DCIM/Camera), but no display and share yet

#TODO:
* Sharing
* Find a way to use built-in autobackup, unfortunately taking pictures using the Camera API seems to not trigger the autobackup correctly even when saving the media to the DCIM/Camera folder and triggering the MediaScanner to refresh. If some Google engineer reads, please please please tell me how to overcome this, there's a long standing bug filed (https://code.google.com/p/google-glass-api/issues/detail?id=588). Or, i will simply do it my way and reverse your original camera app :)
* Find a way to display the taken media in the timeline using the Mirror API (since Google removed the capability of simply creating static cards in the timeline). Or, i will revert to use my own internal viewer.
* Some more features: take-picture burst mode, timed videos, face recognition, qrcode scanning
* Provide head-scrolling menus (at the moment, 'Ok google' menus are fixed to 6 elements and non scrollable)
* Remove 'Ok google' and use custom commands ?

#Compile with:
The usual android studio (needs Android SDK 19 with the Glass Preview SDK)

#Bye!
cowabunga, ciao, bacetti :)

And please, Google .... you rock .... reconsider Glass, they're awesome!
