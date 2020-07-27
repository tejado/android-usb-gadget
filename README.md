# USB Gadget Tool
Convert your Android to any USB device you like!

Following USB gadgets are integrated:
* Keyboard & Mouse (/dev/hidg0 & /dev/hidg1)
* FIDO CTAP (/dev/hidg0 - for WebAuthn)
* CCID (Chip Card Interface Device)

## Roadmap
* root command logging in App
* Creating & enabling USB Gadgets during boot
* Import custom USB Gadget profiles

## How does it work?
USB Gadget Tool uses ConfigFS - an userspace API inside the Linux Kernel - for creation of arbitrary USB composite devices.
https://www.kernel.org/doc/Documentation/filesystems/configfs/configfs.txt