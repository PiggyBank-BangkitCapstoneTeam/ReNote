import tensorflow as tf


class CTCloss(tf.keras.losses.Loss):
    """CTCLoss object for training the model"""

    def __init__(
        self, reduction=tf.keras.losses.Reduction.AUTO, name: str = "CTCloss"
    ) -> None:
        super(CTCloss, self).__init__(name=name)
        self.reduction = reduction
        self.loss_fn = tf.keras.backend.ctc_batch_cost

    def __call__(
        self, y_true: tf.Tensor, y_pred: tf.Tensor, sample_weight=None
    ) -> tf.Tensor:
        """Compute the training batch CTC loss value"""
        batch_len = tf.cast(tf.shape(y_true)[0], dtype="int64")
        input_length = tf.cast(tf.shape(y_pred)[1], dtype="int64")
        label_length = tf.cast(tf.shape(y_true)[1], dtype="int64")

        input_length = input_length * tf.ones(shape=(batch_len, 1), dtype="int64")
        label_length = label_length * tf.ones(shape=(batch_len, 1), dtype="int64")

        # Calculate CTC loss using the built-in ctc_batch_cost function
        loss = self.loss_fn(y_true, y_pred, input_length, label_length)

        # Apply reduction (if necessary)
        if self.reduction == tf.keras.losses.Reduction.NONE:
            return loss
        elif self.reduction == tf.keras.losses.Reduction.SUM:
            return tf.reduce_sum(loss)
        elif self.reduction == tf.keras.losses.Reduction.AUTO:
            return tf.reduce_mean(loss)
        else:
            return loss
