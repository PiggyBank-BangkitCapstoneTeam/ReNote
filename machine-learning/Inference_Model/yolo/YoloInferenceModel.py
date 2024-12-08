import time

start_time = time.time()

from ultralytics import YOLO
import matplotlib.pyplot as plt
import cv2

print(f"Yolo import time: {time.time() - start_time:.2f} seconds")


class YoloInferenceData:
    def __init__(self, box, class_names):
        self.coords = list(map(int, box.xyxy[0]))
        self.class_name = class_names.get(int(box.cls[0]), "unknown")
        self.confidence = float(box.conf[0])


class YoloInferenceModel:
    def __init__(self, model_path, conf_limit=0.5):

        self.model = YOLO(model_path, task="segment")
        self.conf_limit = conf_limit
        self.singleton_class = ["total", "receipt", "shop", "date_time", "item_labels"]
        self.original_image = []
        self.mask = {}

    def process_image(self, image):
        self.cropped_images = {}
        self.original_image = []
        self.detection_boxes = []
        singleton_class_items = {}
        if image.shape[-1] == 4:  # Check if the image has 4 channels (RGBA)
            image = cv2.cvtColor(image, cv2.COLOR_RGBA2RGB)

        inference_results = list(self.model(image, self.conf_limit))[0]
        boxes = inference_results.boxes
        class_names = inference_results.names
        self.original_image = inference_results.orig_img

        for box in boxes:
            box_data = YoloInferenceData(box, class_names)

            if self.conf_limit > box_data.confidence:
                continue

            if box_data.class_name in self.singleton_class:
                self.add_singleton_class_item(singleton_class_items, box_data)
            else:
                self.detection_boxes.append(box_data)

        self.detection_boxes.extend(singleton_class_items.values())
        self.crop_detection_box()

        return self.cropped_images

    def add_singleton_class_item(self, singleton_class_items, box_data):
        existing_item = singleton_class_items.get(box_data.class_name)
        if not existing_item or box_data.confidence > existing_item.confidence:
            singleton_class_items[box_data.class_name] = box_data

    def crop_detection_box(self):
        for detection in self.detection_boxes:
            x1, y1, x2, y2 = detection.coords
            class_name = detection.class_name
            cropped_img = self.original_image[y1:y2, x1:x2]

            if class_name not in self.cropped_images:
                self.cropped_images[class_name] = []

            self.cropped_images[class_name].append(
                {"image": cropped_img, "confidence": detection.confidence}
            )

    def visualize_cropped_image(self):
        for class_name, images_data in self.cropped_images.items():
            for data in images_data:
                plt.figure(figsize=(4, 4))
                plt.imshow(data["image"])
                plt.title(
                    f"class_name: {class_name} (Confidence: {data['confidence']:.2f})"
                )
                plt.axis("off")
                plt.show()
