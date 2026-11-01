"""Conversion regressions without a downloaded model or TensorFlow runtime."""
import contextlib
import importlib.util
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import patch

import numpy as np


def load_converter():
    torch = types.ModuleType("torch")
    nn = types.ModuleType("torch.nn")
    nn.Module = object
    torch.nn = nn
    torch.no_grad = contextlib.nullcontext
    spec = importlib.util.spec_from_file_location("conversion_under_test", Path(__file__).with_name("convert_mobileclip.py"))
    module = importlib.util.module_from_spec(spec)
    with patch.dict(sys.modules, {"torch": torch, "torch.nn": nn}):
        spec.loader.exec_module(module)
    return module


conversion = load_converter()


class Tensor:
    def __init__(self, values):
        self.values = np.asarray(values)

    def unsqueeze(self, dim):
        return Tensor(np.expand_dims(self.values, dim))

    def detach(self):
        return self

    def cpu(self):
        return self

    def numpy(self):
        return self.values


def basis(index):
    result = np.zeros((1, 512), dtype=np.float32)
    result[0, index] = 1
    return result


class Interpreter:
    def __init__(self, model_type, corrupt=False):
        self.model_type = model_type
        self.corrupt = corrupt
        self.invocations = 0
        self.input = None

    def allocate_tensors(self):
        pass

    def get_input_details(self):
        if self.model_type == "image":
            return [{"shape": np.array([1, 256, 256, 3]), "dtype": np.float32, "index": 0}]
        return [{"shape": np.array([1, 77]), "dtype": np.int64, "index": 0}]

    def get_output_details(self):
        return [{"shape": np.array([1, 512]), "dtype": np.float32, "index": 1}]

    def set_tensor(self, index, values):
        self.input = values

    def invoke(self):
        self.invocations += 1

    def get_tensor(self, index):
        return basis(2 if self.corrupt else (0 if self.model_type == "image" else 1))


class Model:
    def encode_image(self, tensor):
        return Tensor(basis(0))

    def encode_text(self, tokens):
        return Tensor(basis(1))


class ConversionParityTest(unittest.TestCase):
    def run_sanity(self, corrupt=False):
        image, text = Interpreter("image"), Interpreter("text", corrupt=corrupt)
        tensorflow = types.SimpleNamespace(lite=types.SimpleNamespace(
            Interpreter=lambda model_path: image if "image" in model_path else text))
        with patch.dict(sys.modules, {"tensorflow": tensorflow}):
            conversion.sanity_check(
                Model(), lambda image: Tensor(np.zeros((3, 256, 256), dtype=np.float32)),
                lambda texts: Tensor(np.zeros((1, 77), dtype=np.int64)),
                Path("image.tflite"), Path("text.tflite"),
            )
        return image, text

    def test_sanity_executes_both_exported_encoders(self):
        image, text = self.run_sanity()
        self.assertEqual(2, image.invocations)
        self.assertEqual(4, text.invocations)
        self.assertEqual((1, 256, 256, 3), image.input.shape)
        self.assertEqual(np.dtype("int64"), text.input.dtype)

    def test_sanity_rejects_corrupted_converted_text_embeddings(self):
        with self.assertRaisesRegex(ValueError, "differs after conversion"):
            self.run_sanity(corrupt=True)

    def test_validation_never_skips_allocation_failure(self):
        class Broken(Interpreter):
            def allocate_tensors(self):
                raise RuntimeError("unsupported op")
        tensorflow = types.SimpleNamespace(lite=types.SimpleNamespace(
            Interpreter=lambda model_path: Broken("image")))
        with patch.dict(sys.modules, {"tensorflow": tensorflow}):
            with self.assertRaisesRegex(RuntimeError, "unsupported op"):
                conversion.validate_tflite(Path("image.tflite"))

    def test_rejects_zero_and_nonfinite_embeddings(self):
        for embedding in [np.zeros(512), np.full(512, np.nan)]:
            with self.assertRaisesRegex(ValueError, "Invalid embedding"):
                conversion._normalize_embedding(embedding)


if __name__ == "__main__":
    unittest.main()
