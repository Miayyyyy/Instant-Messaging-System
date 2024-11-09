import json
import socket
import struct
import threading
import uuid

def task(s):
    print("task 开始")
    while True:
        command = struct.unpack('>I',s.recv(4))[0] #接收command并解析
        num = struct.unpack('>I',s.recv(4))[0]
        print(command)
        if command == 0x232a:
            print("收到下线通知，退出登录")
            s.close()


imei = str(uuid.uuid1())

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(("127.0.0.1",9000))

t1 = threading.Thread(target=task,args=(s,))
t1.start()
##data
command = 0x2328
version = 2
clientType = 3
print(clientType)
messageType = 0x0
appId = 10000
userId = 'lld'

##data to bytes
commandByte = command.to_bytes(4,'big')
versionByte = version.to_bytes(4,'big')
messageTypeByte = messageType.to_bytes(4,'big')
clientTypeByte = clientType.to_bytes(4,'big')
appIdByte = appId.to_bytes(4,'big')
imeiBytes = bytes(imei,"utf-8")
imeiLength = len(imeiBytes)
imeiLengthByte = imeiLength.to_bytes(4,'big')
data = {"userId":userId, "appId":appId, "clientType":clientType, "imei":imei}
jsonData = json.dumps(data);
body = bytes(jsonData, "utf-8")
body_len = len(body)
bodyLenByte = body_len.to_bytes(4,'big')

s.sendall(commandByte+versionByte + clientTypeByte+messageTypeByte+appIdByte+imeiLengthByte + bodyLenByte + imeiBytes + body)
# for x in range(100):
#     s.sendall(commandByte+versionByte + clientTypeByte+messageTypeByte+appIdByte+imeiLengthByte + bodyLenByte + imeiBytes + body)

while(True):
    i = 1 + 1