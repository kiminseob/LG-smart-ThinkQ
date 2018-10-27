import sys
import binascii
import struct
import time
from bluepy.btle import UUID, Peripheral,DefaultDelegate

humidity=-1
battery="loading"
temperature=-1

class MyDelegate(DefaultDelegate):
     #Constructor
    def __init__(self, params):
        DefaultDelegate.__init__(self)
      
     #notification이 활성화되면 불리는 함수
    def handleNotification(self, cHandle, data):
         global humidity,battery,temperature
        
         #print ("Notification Handle: 0x" + format(cHandle,'02X') + " Value: "+ format(data[0]))
         if data[0]==107:
             humidity = format( ( (data[4]<<8) | (data[3]&0xFF) ) / 100 )
             temperature= format( ( (data[2]<<8) | (data[1]&0xFF) ) /100)
             
         else:
             battery  = format(data[0])

def enableNotify(handler):
    p.writeCharacteristic(handler, struct.pack('<bb', 0x01, 0x00) ,False)
def disableNotify(handler):
    p.writeCharacteristic(handler, struct.pack('<bb', 0x00, 0x00) ,False)    

#UUID 가져온다.
washer_service_uuid = UUID("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
battery_service_uuid = UUID("180F")

#washer service Characteristics의 센서에서 전송하는 정보이며 Access Permisson은 Notify 
washerTX_char_uuid = UUID("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
battery_level_uuid = UUID("00002a19-0000-1000-8000-00805f9b34fb")

#if len(sys.argv) != 2:
#  print ("Fatal, must pass device address:", sys.argv[0], "<device address="">")
#  quit()

p = Peripheral("c7:74:31:9A:F8:D1","random")
p.setDelegate( MyDelegate(p) )

#WasherService, BatteryService를 가져온다.
WasherService=p.getServiceByUUID(washer_service_uuid)
BatteryService =p.getServiceByUUID(battery_service_uuid)

#TX의 Characteristics를 가져온다.
WasherTX_C = WasherService.getCharacteristics(washerTX_char_uuid)[0]
#battey의 Characteristics를 가져온다.
Battery_C = BatteryService.getCharacteristics(battery_level_uuid)[0]


# Client Characteristic Descriptor의 handler
# 0x13 은 washerTX, 0x0F는 battery
 # notifications의 비트를 1로 바꿔 활성화한다.



# 메인 루프 -----------------------
def Humidity():
    global humidity
     # notifications의 비트를 1로 바꿔 활성화한다.
    enableNotify(0x13)
    #print ("습도  ON")
    while True:
        if  p.waitForNotifications(1.0):
            # handleNotification() 함수가 불린다.
            disableNotify(0x13)
            #print ("습도 OFF")
            return humidity
           
                
def Temperature():
    global temperature
   
    enableNotify(0x13)
    while True:
        if  p.waitForNotifications(1.0):
            # handleNotification() 함수가 불린다.
            disableNotify(0x13)
            return temperature
       
        
def Battery(): #배터리 notify
    global Battery
    count=0
    # notifications의 비트를 1로 바꿔 활성화한다.
    enableNotify(0x0F)
    #print ("배터리 ON")
    while count<10:
        if  p.waitForNotifications(1.0):
            #handleNotification() 함수가 불린다.
            disableNotify(0x0F)
            #print ("배터리 OFF")
            return battery
        time.sleep(1)
        count+=1
    #print ("배터리 못받음 ")   
    disableNotify(0x0F)   
    return battery
