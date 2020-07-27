#!/bin/sh

CONFIGFS_DIR="/config"
GADGETS_PATH="${CONFIGFS_DIR}/usb_gadget"

GADGET="ccid3"
GADGET_PATH=${GADGETS_PATH}/${GADGET}

CONFIG_PATH="$GADGET_PATH/configs/c.1/"
STRINGS_PATH="$GADGET_PATH/strings/0x409/"

mkdir -p $CONFIG_PATH
mkdir -p $STRINGS_PATH

mkdir -p $GADGET_PATH/functions/ccid.usb0
cd $GADGET_PATH/functions/ccid.usb0


cd $GADGET_PATH
echo 0xa4ac > idVendor
echo 0x0525 > idProduct

cd $STRINGS_PATH
echo "tejado" > manufacturer
echo "CCID" > product
echo "42" > serialnumber

cd $CONFIG_PATH
echo 30 > MaxPower
echo "CCID Configuration" > strings/0x409/configuration

ln -s ${GADGET_PATH}/functions/ccid.usb0 $CONFIG_PATH/ccid.usb0
