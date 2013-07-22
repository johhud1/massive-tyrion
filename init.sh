#!/system/bin/sh -v
export PATH=$PATH:/data/mybin
/system/bin/ifconfig wlan0 down
/system/bin/ifconfig wlan0 up
/system/bin/ifconfig wlan0 192.168.2.$1 netmask 255.255.255.0
/data/mybin/iwconfig wlan0 essid nexusbac
/data/mybin/iwconfig wlan0 mode ad-hoc
echo done
