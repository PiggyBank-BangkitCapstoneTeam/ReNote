import json
import os
from google.cloud import pubsub_v1
from google.cloud import storage
from google.api_core.exceptions import ClientError
from dotenv import load_dotenv
from io import BytesIO

# Load file .env
load_dotenv()

# Paths to models
YOLO_MODEL_PATH = "./trained_models/YOLO/model_train_renfred_7/weights/best.pt"
OCR_BEST_WEIGHTS = "./trained_models/OCR/keras_ocr_model/model/best.weights.h5"
CONF_LIMIT = 0.3
ML_MODEL = None

class ReNote_MLConnector:
	def __init__(self):
		self.CloudStorage = storage.Client()

		CloudStorage_UserMediaBucket = os.getenv("CloudStorage_UserMediaBucket")
		if CloudStorage_UserMediaBucket is None:
			raise ValueError("[MLConnector] Cloud Storage diaktifkan tetapi CloudStorage_UserMediaBucket tidak diisi")
		self.CloudStorageBucket = self.CloudStorage.bucket(CloudStorage_UserMediaBucket)

	def Initialize(self):
		self.InitializePubSub()

	def InitializeModel(self):
		global ML_MODEL

		print("[MLConnector] Berusaha load model machine learning...")
		from inference.InferenceModel import InferenceModel
		ML_MODEL = InferenceModel(YOLO_MODEL_PATH, OCR_BEST_WEIGHTS, CONF_LIMIT, False)
		print("[MLConnector] Model machine learning berhasil di load")

	def InitializePubSub(self):
		PUBSUB_ProjectId = os.getenv("PUBSUB_ProjectId")
		if PUBSUB_ProjectId is None:
			raise ValueError("[MLConnector] PUBSUB_ProjectId harus diisi")
		
		PUBSUB_ML_RequestTopicId = os.getenv("PUBSUB_ML_RequestTopicId")
		if PUBSUB_ML_RequestTopicId is None:
			raise ValueError("[MLConnector] PUBSUB_ML_RequestTopicId harus diisi")
		
		PUBSUB_ML_RequestSubscriptionId = os.getenv("PUBSUB_ML_RequestSubscriptionId")
		if PUBSUB_ML_RequestSubscriptionId is None:
			raise ValueError("[MLConnector] PUBSUB_ML_RequestSubscriptionId harus diisi")
		
		PUBSUB_ML_ResponseTopicId = os.getenv("PUBSUB_ML_ResponseTopicId")
		if PUBSUB_ML_ResponseTopicId is None:
			raise ValueError("[MLConnector] PUBSUB_ML_ResponseTopicId harus diisi")
		
		if os.getenv("PUBSUB_ML_ResponseSubscriptionId") is None:
			raise ValueError("[MLConnector] PUBSUB_ML_ResponseSubscriptionId harus diisi")
		
		self.pubsub_publisher = pubsub_v1.PublisherClient()
		self.pubsub_subscriber = pubsub_v1.SubscriberClient()

		self.pubsub_RequestTopic = self.pubsub_subscriber.topic_path(
			PUBSUB_ProjectId,
            PUBSUB_ML_RequestTopicId
		)
		self.pubsub_ResponseTopic = self.pubsub_publisher.topic_path(
            PUBSUB_ProjectId,
            PUBSUB_ML_ResponseTopicId
        )

		self.pubsub_RequestSubscription = self.pubsub_subscriber.subscription_path(
			PUBSUB_ProjectId,
            PUBSUB_ML_RequestSubscriptionId
		)

		self.pubsub_SubscriptionFuture = self.pubsub_subscriber.subscribe(self.pubsub_RequestSubscription, self.onScanRequest)

	def onScanRequest(self, message):
		global ML_MODEL

		message.ack()

		print("[MLConnector] New Scan Request")

		try:
			json_string = message.data.decode('utf-8')
			job_data = json.loads(json_string)
		except Exception as e:
			print("[MLConnector] Error parsing JSON: {}".format(e))

			self.sendResponse({
                "success": False, 
                "error": "Gagal parsing JSON request"
            })
			return
		
		if job_data["photo_id"] is None:
			print("[MLConnector] Request tidak memiliki photo_id")

			self.sendResponse({
                "success": False, 
                "error": "Photo_id harus diisi"
            })
			return

		if job_data["note_id"] is None:
			print("[MLConnector] Request tidak memiliki note_id")

			self.sendResponse({
                "success": False, 
                "error": "Note_id harus diisi"
            })
			return

		if ML_MODEL is None:
			print("[MLConnector] Model machine learning belum selesai di load")

			self.sendResponse({
				"success": False, 
				"error": "Model machine learning belum selesai di load"
			})
			return
		
		blob = self.CloudStorageBucket.blob(job_data["photo_id"])
		
		try:
			bytes = blob.download_as_bytes()
		except Exception as e:
			if isinstance(e, ClientError):
				if e.code == 404:
					print("[MLConnector] File foto yang ingin discan tidak ditemukan, sudah dihapus user?")

					self.sendResponse({
						"success": False,
						"error": "File tidak ditemukan"
					})
					return
			
			print("[MLConnector] Error downloading file: {}".format(e))

			self.sendResponse({
                "success": False,
                "error": "Gagal mendownload file foto"
            })
			return

		try:
			result = ML_MODEL.predict(image_path=BytesIO(bytes))

			self.sendResponse({
				"success": True,
				"note_id": job_data["note_id"],
				"photo_id": job_data["photo_id"],
				"result": result
			})
			print("[MLConnector] Prediksi berhasil")
			return
		
		except Exception as e:
			print("[MLConnector] Error melakukan prediksi: {}".format(e))

			self.sendResponse({
                "success": False,
                "error": "Gagal melakukan prediksi menggunakan model machine learning"
            })
			return
		
	def sendResponse(self, response: str | dict):
		# If obj, json encode
		if isinstance(response, dict):
			response = json.dumps(response)

		self.pubsub_publisher.publish(self.pubsub_ResponseTopic, response.encode())

	def daemonize(self):
		if self.pubsub_SubscriptionFuture is None:
			raise ValueError("[MLConnector] SubscriptionFuture is None")
		
		try:
			self.pubsub_SubscriptionFuture.result()
			print("[MLConnector] Daemon berhenti")
		except KeyboardInterrupt:
			print("[MLConnector] ML Connector stopped due to KeyboardInterrupt")
			self.pubsub_SubscriptionFuture.cancel()

if __name__ == "__main__":
	MLConnector = ReNote_MLConnector()
	MLConnector.Initialize()
	MLConnector.InitializeModel()
	MLConnector.daemonize()