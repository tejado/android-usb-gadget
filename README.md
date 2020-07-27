# USB Gadget Tool

<img src="static/usb-gadget-tool-3.png" align="right" height="500" alt="USB Gadget Tool Screenshot">  

Convert your Android to any USB device you like!  
USB Gadget Tool allows you to create and activate USB device roles, like a mouse or a keyboard. Connected USB hosts (e.g. a normal computer) will then identify your Android device only under that role.
It can also be used to deactivate the standard USB Gadget (including mtp, adb, etc.), e.g. for security reasons.  

Following USB gadgets are integrated:
* Keyboard & Mouse (/dev/hidg0, /dev/hidg1)
* FIDO CTAP (/dev/hidg0; for WebAuthn)
* CCID (/dev/ccid_ctrl, /dev/ccid_bulk)

## Use-Cases
* [Authorizer](https://github.com/tejado/Authorizer)
* [hid-gadget-test](https://github.com/pelya/android-keyboard-gadget)

## Roadmap
* root command logging in App
* Creating & enabling USB Gadgets during boot
* Import custom USB Gadget profiles
* Adding functions to USB Gadgets
* Example USB Gadget usage (USB Gadget Tool currently only manages USB Gadgets, not implementations of these)

## How does it work?
USB Gadget Tool uses ConfigFS - an userspace API inside the Linux Kernel - for creation of arbitrary USB composite devices.
https://www.kernel.org/doc/Documentation/filesystems/configfs/configfs.txt
