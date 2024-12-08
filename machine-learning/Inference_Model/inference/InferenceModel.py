import cv2
import matplotlib.pyplot as plt
from PIL import Image, ImageEnhance
import json
import re
import numpy as np

from yolo.YoloInferenceModel import YoloInferenceModel
from ocr.OcrInferenceModel import OcrInferenceModel


class InferenceModel:
    def __init__(
        self, yolo_model_path, ocr_weights_path, conf_limit=0.5, use_augments=False
    ):
        self.yolo_model = None
        self.ocr_model = None
        self.conf_limit = conf_limit
        self.use_augments = use_augments
        self.initialize_models(yolo_model_path, ocr_weights_path)

    def initialize_models(self, yolo_model_path, ocr_weights_path):
        if self.yolo_model is None:
            self.yolo_model = YoloInferenceModel(
                yolo_model_path, conf_limit=self.conf_limit
            )
        if self.ocr_model is None:
            self.ocr_model = OcrInferenceModel(ocr_weights_path)

    def augment_image(self, image):
        gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        pil_img = Image.fromarray(gray_image)
        enhancer = ImageEnhance.Contrast(pil_img)
        enhanced_image = enhancer.enhance(1.5)
        enhancer = ImageEnhance.Brightness(enhanced_image)
        enhanced_image = enhancer.enhance(1.2)
        augmented_image = np.array(enhanced_image)
        denoised_image = cv2.fastNlMeansDenoising(augmented_image, None, 10, 7, 11)
        denoised_image_3d = np.repeat(denoised_image[:, :, np.newaxis], 3, axis=2)
        return denoised_image_3d

    def process_cropped_images(self):
        results = {}
        for label, data in self.cropped_images.items():
            if label not in results:
                results[label] = []
            for image_data in data:
                image = image_data["image"]
                if self.use_augments:
                    image = self.augment_image(image)
                predicted_text = self.ocr_model.predict(image)
                results[label].append(predicted_text)
        return results

    def visualize_results(self):
        results = self.process_cropped_images()
        for label, predicted_texts in results.items():
            for idx, predicted_text in enumerate(predicted_texts):
                image = self.cropped_images[label][idx]["image"]
                plt.figure(figsize=(6, 6))
                plt.imshow(image.squeeze(), cmap="gray")
                plt.title(f"Predicted {label}: {predicted_text}")
                plt.axis("off")

    def result_to_json(self, results):
        processed_results = {
            label: texts[0] if len(texts) == 1 else texts
            for label, texts in results.items()
        }
        return processed_results

    def validate_cropped_image(self):
        print("error here!")
        return "receipt" in self.process_cropped_images()

    def process_text(self, data):
        def clean_text(text):
            cleaned_text = re.sub(r"\[START\]|\[END\]|\[UNK\]", "", text)
            return cleaned_text.strip()

        processed_data = {}
        for classes, value in data.items():

            if "receipt" in classes:
                continue
            if isinstance(value, list):
                processed_data[classes] = [clean_text(item) for item in value]
            else:
                processed_data[classes] = clean_text(value)

        if "item" not in processed_data:
            processed_data["item"] = []

        if "shop" not in processed_data:
            processed_data["shop"] = ""

        if "total" in processed_data:
            total_text = processed_data["total"]
            total_number = re.sub(r"[^0-9.]", "", total_text)
            processed_data["total"] = total_number
        else:
            processed_data["total"] = ""

        if "date_time" in processed_data:
            date_text = processed_data["date_time"]
            date_match = re.search(r"\d{2}[-/]\d{2}[-/]\d{4}", date_text)
            if date_match:
                processed_data["date_time"] = date_match.group(0)
            else:
                processed_data["date_time"] = ""
        else:
            processed_data["total"] = ""

        return processed_data

    def predict(self, image_path):
        image = Image.open(image_path)
        numpy_image = np.array(image)

        self.cropped_images = self.yolo_model.process_image(numpy_image)
        print(f"cropped_image len: {len(self.cropped_images)}")

        if not self.validate_cropped_image():
            not_found_response = {"status": "error", "message": "Receipt not found"}
            return not_found_response

        final_result = self.process_cropped_images()
        final_result_json = self.result_to_json(final_result)
        processed_text = self.process_text(final_result_json)

        print(f"processed_text: {processed_text}")

        return processed_text
