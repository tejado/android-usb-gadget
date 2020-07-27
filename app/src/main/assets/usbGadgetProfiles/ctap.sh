#!/bin/sh

CONFIGFS_DIR="/config"
GADGETS_PATH="${CONFIGFS_DIR}/usb_gadget"

GADGET="ctap3"
GADGET_PATH=${GADGETS_PATH}/${GADGET}

CONFIG_PATH="$GADGET_PATH/configs/c.1/"
STRINGS_PATH="$GADGET_PATH/strings/0x409/"

mkdir -p $CONFIG_PATH
mkdir -p $STRINGS_PATH

mkdir -p $GADGET_PATH/functions/hid.usb0
cd $GADGET_PATH/functions/hid.usb0

# HID protocol (according to USB spec: 1 for keyboard)
echo 0 > protocol
# device subclass
echo 0 > subclass
# number of bytes per record
echo 64 > report_length

# writing report descriptor
echo -ne \\x06\\xD0\\xF1\\x09\\x01\\xA1\\x01\\x09\\x20\\x15\\x00\\x26\\xFF\\x00\\x75\\x08\\x95\\x40\\x81\\x02\\x09\\x21\\x15\\x00\\x26\\xFF\\x00\\x75\\x08\\x95\\x40\\x91\\x02\\xC0 > report_desc

cd $GADGET_PATH
echo '0xa4ac' > idVendor
echo '0x0525' > idProduct

echo '0x0512' > bcdDevice
echo '0x0200' > bcdUSB
echo 0 > bDeviceProtocol
echo 0 > bDeviceSubClass
echo 8 > bMaxPacketSize0

cd $STRINGS_PATH
echo "tejado" > manufacturer
echo "CTAP" > product
echo "42" > serialnumber

cd $CONFIG_PATH
echo 30 > MaxPower
echo "HID Configuration" > strings/0x409/configuration

ln -s ${GADGET_PATH}/functions/hid.usb0 $CONFIG_PATH/hid.usb0