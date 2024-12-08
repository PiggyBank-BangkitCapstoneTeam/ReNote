import time

start_time = time.time()

import tensorflow as tf
from mltu.utils.text_utils import ctc_decoder

print(f"Ocr import time: {time.time() - start_time:.2f} seconds")


class OcrInferenceModel:
    def __init__(self, model_weights):
        vocab_size = 82
        image_size = (64, 256, 1)
        ocr_model = self.load_crnn_model(image_size, vocab_size)
        ocr_model.load_weights(model_weights)
        self.model = ocr_model
        self.char_set = list(
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.,!?@$&()[]{}:;/- "
        )

    def load_crnn_model(self, input_dim, output_dim, activation="relu", dropout=0.3):
        inputs = tf.keras.layers.Input(shape=input_dim, name="input")
        inputs_normalized = tf.keras.layers.Lambda(lambda x: x / 255.0)(
            inputs
        )  # Normalizing input to [0, 1]

        c_1 = tf.keras.layers.Conv2D(
            32, (3, 3), activation=activation, padding="same", name="conv_1"
        )(inputs_normalized)
        c_2 = tf.keras.layers.Conv2D(
            32, (3, 3), activation=activation, padding="same", name="conv_2"
        )(c_1)
        c_3 = tf.keras.layers.Conv2D(
            64, (3, 3), activation=activation, padding="same", name="conv_3"
        )(c_2)
        bn_3 = tf.keras.layers.BatchNormalization(name="bn_3")(c_3)
        p_3 = tf.keras.layers.MaxPooling2D(pool_size=(2, 2), name="maxpool_3")(bn_3)

        c_4 = tf.keras.layers.Conv2D(
            64, (3, 3), activation=activation, padding="same", name="conv_4"
        )(p_3)
        c_5 = tf.keras.layers.Conv2D(
            64, (3, 3), activation=activation, padding="same", name="conv_5"
        )(c_4)
        bn_5 = tf.keras.layers.BatchNormalization(name="bn_5")(c_5)
        p_5 = tf.keras.layers.MaxPooling2D(pool_size=(2, 2), name="maxpool_5")(bn_5)

        c_6 = tf.keras.layers.Conv2D(
            128, (3, 3), activation=activation, padding="same", name="conv_6"
        )(p_5)
        c_7 = tf.keras.layers.Conv2D(
            128, (3, 3), activation=activation, padding="same", name="conv_7"
        )(c_6)
        bn_7 = tf.keras.layers.BatchNormalization(name="bn_7")(c_7)

        reshaped_output = tf.keras.layers.Reshape(
            (bn_7.shape[1] * bn_7.shape[2], bn_7.shape[-1])
        )(bn_7)

        # Add Bidirectional LSTM layers to capture sequential patterns
        blstm_8 = tf.keras.layers.Bidirectional(
            tf.keras.layers.LSTM(
                32, kernel_initializer="he_normal", return_sequences=True
            )
        )(reshaped_output)
        blstm_8 = tf.keras.layers.Bidirectional(
            tf.keras.layers.LSTM(
                64, kernel_initializer="he_normal", return_sequences=True
            )
        )(reshaped_output)
        do_10 = tf.keras.layers.Dropout(dropout, name="dropout")(blstm_8)

        output = tf.keras.layers.Dense(
            output_dim + 1, activation="softmax", name="output"
        )(do_10)

        # Create and return the model
        model = tf.keras.Model(inputs=inputs, outputs=output)
        return model

    def get_vocab(self):
        char_to_num = tf.keras.layers.StringLookup(
            vocabulary=list(self.char_set), mask_token=None
        )
        return char_to_num.get_vocabulary()

    def preprocess_image(self, image):
        image = tf.convert_to_tensor(image, dtype=tf.int32)
        image.set_shape([None, None, 3])  # Ensure the shape is defined
        image = tf.image.resize(image, (64, 256))
        image = tf.image.rgb_to_grayscale(image)
        image = tf.expand_dims(image, axis=0)
        return image

    def predict(self, image):
        image = self.preprocess_image(image)
        print(f"Prediction input shape: {image.shape}")
        prediction = self.model.predict(image)
        decoded_text = ctc_decoder(prediction, self.get_vocab())[0]
        return decoded_text
