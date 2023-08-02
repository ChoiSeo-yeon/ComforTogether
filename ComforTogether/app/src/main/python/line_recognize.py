import cv2
import numpy as np
import os

class RecognizeLine() :
    def __init__(self) :
        self.kel = []

    def read_images_with_extension(self, directory):
        image_list = []
        for filename in os.listdir(directory):
            if filename.endswith('.jpg'):
                image_path = os.path.join(directory, filename)
                if image_path is not None:
                    print(image_path)
                    image_list.append(image_path)
        return image_list

    def DectectEdge(self, frame):
        frame = frame.copy()
        frame = cv2.resize(frame, dsize = (0, 0), fx = 0.3, fy = 0.3, interpolation = cv2.INTER_AREA)
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        # red 
        lower_yellow1 = np.array([0, 52, 175])
        upper_yellow1 = np.array([0, 255, 255])
        mask1 = cv2.inRange(hsv, lower_yellow1, upper_yellow1)
        lower_yellow2 = np.array([0, 52, 175])
        upper_yellow2 = np.array([180, 255, 255])
        mask2 = cv2.inRange(hsv, lower_yellow2, upper_yellow2)
        mask = mask1+mask2

        edge = cv2.Canny(mask, 150, 300)
        #cv2.imshow("1", edge); cv2.waitKey(0)

        return edge

    def RegionOfInterest(self, image) :
        mask = np.zeros_like(image)
        h, w = image.shape
        if len(image.shape) > 2 :
            channel_count = img.shape[2]
            ignore_mask_color = (255,)*channel_count
        else :
            ignore_mask_color = 255
        polygon = np.array([[
            (int(w*(1/9)), h), (int(w*(1/3)), int(h*(1/2))),
            (int(w*(2/3)), int(h*(1/2))), (int(w*(8/9)), h),]], np.int32)
        cv2.fillPoly(mask, polygon, ignore_mask_color)
        masked_image = cv2.bitwise_and(image, mask)
        #cv2.imshow("2", mask); cv2.waitKey(0)
        return masked_image

    def DrawLines(self, image, lines, color = [0, 255, 0], thickness = 5) :
        if len(lines) > 0 :
            for line in lines :
                for x1, y1, x2, y2 in line :
                    cv2.line(image, (x1, y1), (x2, y2), color, thickness)
            return 1
        elif lines == self.kel :
            return 0

    def HoughLines(self, image, rho, theta, threshold, min_len, max_gap) :
        lines = cv2.HoughLinesP(image, rho, theta, threshold, np.array([]),
                                minLineLength = min_len,
                                maxLineGap = max_gap)
        if lines is None :
            lines = self.kel
        line_image = np.zeros((image.shape[0], image.shape[1], 3), dtype = np.uint8)
        signal = self.DrawLines(line_image, lines)
        if signal == 1 :
            #cv2.imshow("3", line_image); cv2.waitKey(0)
            return 1, line_image
        else :
            return 0, 0
    
    def WeightedImg(self, image, initial_img, alpha, beta, lamda) :
        return cv2.addWeighted(initial_img, alpha, image, beta, lamda)


