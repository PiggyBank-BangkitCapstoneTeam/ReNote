from fastapi import FastAPI, HTTPException
from contextlib import asynccontextmanager
from pydantic import BaseModel
from inference.InferenceModel import InferenceModel
from fastapi.responses import HTMLResponse
import os


# Paths to models
YOLO_MODEL_PATH = "./trained_models/YOLO/model_train_renfred_1/weights/best.pt"
OCR_BEST_WEIGHTS = "./trained_models/OCR/CRNN_Model_Agus_v4/weight/weights.h5"
CONF_LIMIT = 0.3
model = None


class PredictRequest(BaseModel):
    image_path: str


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    print("Loading the model...")
    try:
        model = InferenceModel(YOLO_MODEL_PATH, OCR_BEST_WEIGHTS, CONF_LIMIT, False)
        print("Model loaded successfully!")
        yield
    except Exception as e:
        print(f"Error during model initialization: {e}")
        raise HTTPException(status_code=500, detail="Failed to initialize the model.")
    finally:
        print("Cleaning up resources...")
        if model:
            del model
        print("Resources cleaned up!")


app = FastAPI(lifespan=lifespan)

# ROUTES ==================================================================================


@app.get("/", response_class=HTMLResponse)
def read_root():
    return """
    <html>
        <head>
            <title>API for Predicting Receipts to Text</title>
        </head>
        <body>
            <h1>API for Predicting Receipts to Text</h1>
            <p>This API helps you to predict and extract text from receipt images using deep learning models.</p>
            <h2>Endpoints:</h2>
            <ul>
                <li><b>POST /predict/</b>: Predict text from a receipt image.</li>
            </ul>
            <h2>Parameters:</h2>
            <ul>
                <li><b>image_path</b> (str): Path to the image of the receipt.</li>
            </ul>
            <div>
                <h2>Example Request:</h2>
                <pre>
                {
                    "image_path": "sample_image/8.jpg"
                }
                </pre>
                <h2>Response:</h2>
                <pre>
                {
                    "success": true,
                    "result": {
                        "item": [
                            "COCA COLA  1.0M",
                            "KATSU 3  2.0M",
                        ],
                        "total": "412.400",
                        "date_time": "12/05/2024",
                        "shop": "WARUNG PASTA"
                    }
                }
                </pre> 
            </div>
        </body>
    </html>
    """


@app.post("/predict/")
async def predict(request: PredictRequest):
    global model
    if model is None:
        raise HTTPException(status_code=500, detail="Model is not loaded")

    normalized_path = os.path.normpath(request.image_path)

    if not os.path.exists(normalized_path):
        raise HTTPException(
            status_code=404, detail=f"{request.image_path} is not found"
        )

    try:
        result = model.predict(image_path=normalized_path)
        return {"success": True, "result": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
