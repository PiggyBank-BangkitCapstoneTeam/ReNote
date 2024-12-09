import time

start_time = time.time()

import tensorflow as tf
from mltu.utils.text_utils import ctc_decoder

print(f"Ocr import time: {time.time() - start_time:.2f} seconds")


class OcrInferenceModel:
    def __init__(self, model_weights):
        self.char_set = list("0123456789abcdefghijklmnopqrstuvwxyz.,!?@$&()[]{}:;/- ")
        self.vocab_size = 56
        self.build_params = {
            "height": 31,
            "width": 300,
            "color": False,
            "filters": (64, 128, 256, 256, 512, 512, 512),
            "rnn_units": (128, 128),
            "dropout": 0.25,
            "rnn_steps_to_discard": 2,
            "pool_size": 2,
            "stn": False,
        }
        ocr_model = self.load_pretrained_model(self.vocab_size, **self.build_params)
        ocr_model.load_weights(model_weights)
        self.model = ocr_model

    def load_pretrained_model(self, vocab_size, **kwargs):
        height = kwargs.get("height")
        width = kwargs.get("width")
        color = kwargs.get("color", False)
        filters = kwargs.get("filters")
        rnn_units = kwargs.get("rnn_units")
        pool_size = kwargs.get("pool_size")

        assert len(filters) == 7, "7 CNN filters must be provided."
        assert len(rnn_units) == 2, "2 RNN units must be provided."

        # Input layer
        inputs = tf.keras.Input(
            shape=(height, width, 3 if color else 1), name="input_image"
        )

        # Image normalization (scaling pixel values to the range [0, 1])
        x = tf.keras.layers.Lambda(lambda x: x / 255.0, name="image_normalization")(
            inputs
        )

        # Backbone (Convolutional layers)
        x = tf.keras.layers.Permute((2, 1, 3))(x)  # Swap width and height
        x = tf.keras.layers.Conv2D(
            filters[0], (3, 3), activation="relu", padding="same", name="conv_1"
        )(x)
        x = tf.keras.layers.Conv2D(
            filters[1], (3, 3), activation="relu", padding="same", name="conv_2"
        )(x)
        x = tf.keras.layers.Conv2D(
            filters[2], (3, 3), activation="relu", padding="same", name="conv_3"
        )(x)
        x = tf.keras.layers.BatchNormalization(name="bn_3")(x)
        x = tf.keras.layers.MaxPooling2D(
            pool_size=(pool_size, pool_size), name="maxpool_3"
        )(x)

        x = tf.keras.layers.Conv2D(
            filters[3], (3, 3), activation="relu", padding="same", name="conv_4"
        )(x)
        x = tf.keras.layers.Conv2D(
            filters[4], (3, 3), activation="relu", padding="same", name="conv_5"
        )(x)
        x = tf.keras.layers.BatchNormalization(name="bn_5")(x)
        x = tf.keras.layers.MaxPooling2D(
            pool_size=(pool_size, pool_size), name="maxpool_5"
        )(x)

        x = tf.keras.layers.Conv2D(
            filters[5], (3, 3), activation="relu", padding="same", name="conv_6"
        )(x)
        x = tf.keras.layers.Conv2D(
            filters[6], (3, 3), activation="relu", padding="same", name="conv_7"
        )(x)
        x = tf.keras.layers.BatchNormalization(name="bn_7")(x)

        x = tf.keras.layers.Reshape(
            target_shape=(
                width // pool_size**2,
                (height // pool_size**2) * filters[-1],
            ),
            name="reshape",
        )(x)

        x = tf.keras.layers.Dense(rnn_units[0], activation="relu", name="fc_9")(x)

        # First RNN layer
        rnn_1_forward = tf.keras.layers.LSTM(
            rnn_units[0],
            return_sequences=True,
            kernel_initializer="he_normal",
            name="lstm_10",
        )(x)
        rnn_1_backward = tf.keras.layers.LSTM(
            rnn_units[0],
            return_sequences=True,
            kernel_initializer="he_normal",
            go_backwards=True,
            name="lstm_10_back",
        )(x)
        rnn_1 = tf.keras.layers.Add()([rnn_1_forward, rnn_1_backward])

        # Second RNN layer
        rnn_2_forward = tf.keras.layers.LSTM(
            rnn_units[1],
            return_sequences=True,
            kernel_initializer="he_normal",
            name="lstm_11",
        )(rnn_1)
        rnn_2_backward = tf.keras.layers.LSTM(
            rnn_units[1],
            return_sequences=True,
            kernel_initializer="he_normal",
            go_backwards=True,
            name="lstm_11_back",
        )(rnn_1)
        x = tf.keras.layers.Concatenate()([rnn_2_forward, rnn_2_backward])

        # Dense layer for classification
        output = tf.keras.layers.Dense(
            vocab_size + 1,  # +1 for the CTC blank token
            activation="softmax",
            name="fc_12",
        )(x)

        # Model definition
        model = tf.keras.Model(inputs=inputs, outputs=output, name="keras_ocr_model")

        return model

    def get_vocab(self):
        char_to_num = tf.keras.layers.StringLookup(
            vocabulary=list(self.char_set), mask_token=None
        )
        return char_to_num.get_vocabulary()

    def preprocess_image(self, image):
        image = tf.convert_to_tensor(image, dtype=tf.int32)
        image.set_shape([None, None, 3])
        image = tf.image.resize(
            image, (self.build_params["height"], self.build_params["width"])
        )
        image = tf.image.rgb_to_grayscale(image)
        image = tf.expand_dims(image, axis=0)
        return image

    def predict(self, image):
        image = self.preprocess_image(image)
        print(f"Prediction input shape: {image.shape}")
        prediction = self.model.predict(image)
        decoded_text = ctc_decoder(prediction, self.get_vocab())[0]
        return decoded_text
