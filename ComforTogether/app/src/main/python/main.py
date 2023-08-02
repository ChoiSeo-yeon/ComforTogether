import cv2
from line_recognize import RecognizeLine
from object_detect import DetectObject
#from send_img import SendImg
import numpy as np

find_line = RecognizeLine()
find_object = DetectObject()

#working은 local에서 이미지로 돌릴 때
def working_lineRecognize() :
    image_directory = 'C:/Users/dpffl/Desktop/comfortogether/img'
    images = find_line.read_images_with_extension(image_directory)
    for image in images :
        image = cv2.imread(image)
        img = image.copy()
        #print(img.shape)
        edge =  find_line.DectectEdge(img)
        mask = find_line.RegionOfInterest(edge)
        signal, img = find_line.HoughLines(mask, 2, np.pi/180, 90, 120, 150)
        print(signal)
        #return signal
        
#working_lineRecognize()

def working_DetectObject() :
      
    # Load class names.
      classesFile = "C:/Users/dpffl/Desktop/comfortogether/comfortogether/python/coco.names"
      classes = find_object.class_file(classesFile)
  
      frame = cv2.imread("C:/Users/dpffl/Desktop/comfortogether/img/bike_example.jpg")
      
      modelWeights = "C:/Users/dpffl/Desktop/comfortogether/best.onnx"
      net = cv2.dnn.readNet(modelWeights)
      
      detections = find_object.pre_process(frame, net)
      img = find_object.post_process(frame.copy(), detections, classes)
      
      #t, _ = net.getPerfProfile()
      #label = 'Inference time: %.2f ms' % (t * 1000.0 /  cv2.getTickFrequency())
      #print(label)
    
      return img

#working_DetectObject()

def main():
    img = SendImg()
    img = cv2.imread(img)
    copy = img.copy()
    
    #line recogize
    edge = find_line.DectectEdge(copy)
    mask = find_line.RegionOfInterest(edge)
    signal, copy = find_line.HoughLines(mask, 2, np.pi/180, 90, 120, 150)
    
    #object detect
    classesFile = "coco.names"
    classes = find_object.class_file(classesFile)

    modelWeights = "best.onnx"
    net = cv2.dnn.readNet(modelWeights)
      
    detections = find_object.pre_process(copy, net)
    img, label = find_object.post_process(copy.copy(), detections, classes)

    return signal, label
   
if __name__ == "__main__":
	main()
