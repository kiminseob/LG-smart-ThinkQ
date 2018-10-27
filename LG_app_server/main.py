import bluedata
import sys
from socket import *
import threading
import time
#기본포트
ECHO_PORT = 50000 + 7

#버퍼사이즈
BUFSIZE = 1024

humidity=0
battery="loading"
temperature=0
def notify_thread():
    global humidity,battery,temperature
    while True:
        humidity = bluedata.Humidity()
        temperature = bluedata.Temperature()
        battery=bluedata.Battery()
        time.sleep(5)
    
def server():
    
    global battery,humidity,temperature
    #기본포트로설정
    port = ECHO_PORT
    # 소켓 생성 (UDP = SOCK_DGRAM, TCP = SOCK_STREAM)
    s = socket(AF_INET, SOCK_DGRAM)
    #포트설정
    s.bind(('',port))
    
    #쓰레드 동작
    t = threading.Thread(target=notify_thread)
    t.start()
    
    print('UDP echo server ready')
    
    while True:
        # 클라이언트로 메시지가 도착하면 다음 줄로 넘어가고
        # 그렇지 않다면 대기(Blocking)
        temp, addr = s.recvfrom(BUFSIZE)
        
        Join_String = humidity +":"+ battery +":" +str(temperature)
        print(Join_String)
        Join_as_bytes = str.encode(Join_String)
        #battery_as_bytes = str.encode(battery)
        s.sendto(Join_as_bytes,addr)
       
    
        #print ('server received {0} from {1}'data, .format(addr))
 

#메인함수
if __name__ == "__main__":
    
    #서버함수호출
    server()
