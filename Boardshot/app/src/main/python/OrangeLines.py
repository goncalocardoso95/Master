import cv2
import numpy as np

def getStartPosition(image):
    image = cv2.imread(image)
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

    mask_orange = mask = cv2.inRange(hsv,(10, 100, 20), (25, 200, 255))

    orange_area = -1

    orangecnts = cv2.findContours(mask_orange.copy(),
                              cv2.RETR_EXTERNAL,
                              cv2.CHAIN_APPROX_SIMPLE)[-2]



    if len(orangecnts)>0:
        orange_area = max(orangecnts, key=cv2.contourArea)
        (xg,yg,wg,hg) = cv2.boundingRect(orange_area)
        tuple1 = (xg, yg)
        tuple2 = (xg + wg, yg + hg)
        orange_area = abs(tuple1[0] - tuple2[0]) * abs(tuple1[1] - tuple2[1])
        cv2.rectangle(image,(xg,yg),(xg+wg, yg+hg),(255,0,0),9)
    return orange_area