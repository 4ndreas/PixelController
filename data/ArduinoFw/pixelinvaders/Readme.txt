PixelInvaders Firmware for Arduino compatible boards

You need to install those Arduino Libraries:
-TimerOne
-LPD6803.lib, bitbanging version, https://github.com/neophob/LPD6803-RGB-Pixels
 OR 
 LPD6803 SPI, hardware spi version, https://github.com/neophob/neophob_lpd6803spi

There are two versions of the firmware, a (slower) bitbanging Firmware (neoLedLPD6803) and 
a faster SPI firmware (neoLedLPD6803Spi).

SPI limit you to use specific ports on a Arduino board and may not work if you use for example an Ethernet Shield.
