#!/usr/bin/env python3
"""
Convert MobileCLIP-S0 from PyTorch to two separate TFLite models:
  1. Image encoder  -> mobileclip_s0_image_encoder_fp16.tflite
  2. Text encoder   -> mobileclip_s0_text_encoder_fp16.tflite

Requirements:
  pip install torch torchvision mobileclip onnx onnx2tf tensorflow numpy pillow

Usage:
  python scripts/convert_mobileclip.py

Output files are placed in app/src/main/assets/models/.
"""

import os
import sys
import shutil
import urllib.request
from pathlib import Path

import torch
import torch.nn as nn
import numpy as np

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
MODELS_DIR = ASSETS_DIR / "models"
WORK_DIR = PROJECT_ROOT / "scripts" / "_build"

MODEL_URL = "https://docs-assets.developer.apple.com/ml-research/datasets/mobileclip/mobileclip_s0.pt"
MODEL_PT = WORK_DIR / "mobileclip_s0.pt"

IMAGE_SIZE = 256
CONTEXT_LENGTH = 77
EMBED_DIM = 512


def download_checkpoint():
    """Download MobileCLIP-S0 weights if not already cached."""
    if MODEL_PT.exists():
        print(f"[✓] Checkpoint already exists: {MODEL_PT}")
        return
    WORK_DIR.mkdir(parents=True, exist_ok=True)
    print(f"[↓] Downloading MobileCLIP-S0 checkpoint...")
    urllib.request.urlretrieve(MODEL_URL, MODEL_PT)
    print(f"[✓] Downloaded to {MODEL_PT}")


def load_model():
    """Load and reparameterize MobileCLIP-S0."""
    import copy
    import mobileclip

    model, _, preprocess = mobileclip.create_model_and_transforms(
        "mobileclip_s0", pretrained=str(MODEL_PT)
    )
    model.eval()

    model = copy.deepcopy(model)
    reparameterized = set()
    for module in model.modules():
        if hasattr(module, "reparameterize") and id(module) not in reparameterized:
            reparameterized.add(id(module))
            try:
                module.reparameterize()
            except AttributeError:
                pass

    tokenizer = mobileclip.get_tokenizer("mobileclip_s0")
    return model, preprocess, tokenizer


class ImageEncoderWrapper(nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, image):
        return self.model.encode_image(image)


class TextEncoderWrapper(nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, text):
        return self.model.encode_text(text)


def export_image_encoder_onnx(model):
    """Export image encoder to ONNX."""
    onnx_path = WORK_DIR / "mobileclip_s0_image_encoder.onnx"
    if onnx_path.exists():
        print(f"[✓] Image encoder ONNX already exists: {onnx_path}")
        return onnx_path

    print("[→] Exporting image encoder to ONNX...")
    wrapper = ImageEncoderWrapper(model)
    wrapper.eval()
    dummy_input = torch.randn(1, 3, IMAGE_SIZE, IMAGE_SIZE)

    torch.onnx.export(
        wrapper,
        dummy_input,
        str(onnx_path),
        opset_version=14,
        input_names=["image"],
        output_names=["embedding"],
        dynamic_axes=None,
    )
    print(f"[✓] Image encoder ONNX exported: {onnx_path}")
    return onnx_path


def export_text_encoder_onnx(model):
    """Export text encoder to ONNX."""
    onnx_path = WORK_DIR / "mobileclip_s0_text_encoder.onnx"
    if onnx_path.exists():
        print(f"[✓] Text encoder ONNX already exists: {onnx_path}")
        return onnx_path

    print("[→] Exporting text encoder to ONNX...")
    wrapper = TextEncoderWrapper(model)
    wrapper.eval()
    dummy_input = torch.randint(0, 49408, (1, CONTEXT_LENGTH), dtype=torch.long)

    torch.onnx.export(
        wrapper,
        dummy_input,
        str(onnx_path),
        opset_version=14,
        input_names=["input_ids"],
        output_names=["text_embedding"],
        dynamic_axes=None,
    )
    print(f"[✓] Text encoder ONNX exported: {onnx_path}")
    return onnx_path


def convert_onnx_to_tflite(onnx_path, tflite_name):
    """Convert ONNX model to FP16-quantized TFLite using onnx2tf."""
    import onnx2tf

    tflite_out = MODELS_DIR / tflite_name
    if tflite_out.exists():
        print(f"[✓] TFLite model already exists: {tflite_out}")
        return tflite_out

    print(f"[→] Converting {onnx_path.name} to TFLite (FP16)...")
    output_dir = WORK_DIR / f"tf_{onnx_path.stem}"

    onnx2tf.convert(
        input_onnx_file_path=str(onnx_path),
        output_folder_path=str(output_dir),
        copy_onnx_input_output_names_to_tflite=True,
        non_verbose=True,
        output_signaturedefs=True,
        quant_type="fp16",
    )

    candidates = list(output_dir.rglob("*_float16.tflite"))
    if not candidates:
        candidates = list(output_dir.rglob("*.tflite"))
    if not candidates:
        raise FileNotFoundError(f"No .tflite files found in {output_dir}")

    src = candidates[0]
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, tflite_out)
    print(f"[✓] TFLite model saved: {tflite_out}")
    return tflite_out


def validate_tflite(tflite_path, model_type="image", onnx_stem=None):
    """Validate the exact exported file; an unsupported runtime is a failure."""
    import tensorflow as tf

    print(f"\n[→] Validating {tflite_path.name}...")
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError("Expected one input and one embedding output")
    actual = tuple(inputs[0]["shape"])
    if model_type == "image":
        if actual not in ((1, 3, IMAGE_SIZE, IMAGE_SIZE), (1, IMAGE_SIZE, IMAGE_SIZE, 3)):
            raise ValueError(f"Unexpected image input shape: {actual}")
        allowed_types = (np.float32, np.float16)
    elif model_type == "text":
        if actual != (1, CONTEXT_LENGTH):
            raise ValueError(f"Unexpected text input shape: {actual}")
        allowed_types = (np.int32, np.int64)
    else:
        raise ValueError(f"Unknown model type: {model_type}")
    if inputs[0]["dtype"] not in allowed_types:
        raise ValueError(f"Unsupported {model_type} input dtype: {inputs[0]['dtype']}")
    if tuple(outputs[0]["shape"]) != (1, EMBED_DIM):
        raise ValueError(f"Unexpected embedding shape: {outputs[0]['shape']}")
    if outputs[0]["dtype"] not in (np.float16, np.float32):
        raise ValueError(f"Unsupported embedding dtype: {outputs[0]['dtype']}")
    return interpreter


def _normalize_embedding(values):
    values = np.asarray(values, dtype=np.float32).reshape(-1)
    norm = np.linalg.norm(values)
    if values.size != EMBED_DIM or not np.isfinite(values).all() or not np.isfinite(norm) or norm <= 0:
        raise ValueError("Invalid embedding: expected 512 finite values with a positive norm")
    return values / norm


def _run_tflite(interpreter, values):
    input_detail = interpreter.get_input_details()[0]
    values = np.asarray(values)
    shape = tuple(input_detail["shape"])
    if values.ndim == 4 and values.shape != shape and shape[-1] == 3:
        values = values.transpose(0, 2, 3, 1)
    if values.shape != shape:
        raise ValueError(f"Input shape mismatch: {values.shape} != {shape}")
    interpreter.set_tensor(input_detail["index"], values.astype(input_detail["dtype"]))
    interpreter.invoke()
    return _normalize_embedding(interpreter.get_tensor(interpreter.get_output_details()[0]["index"]))


def _assert_embedding_parity(reference, converted, label, min_cosine=0.99):
    reference = _normalize_embedding(reference)
    converted = _normalize_embedding(converted)
    cosine = float(reference @ converted)
    if cosine < min_cosine:
        raise ValueError(f"{label} differs after conversion: cosine={cosine:.6f}, required>={min_cosine}")
    return cosine


def _assert_similarity_parity(pt_images, pt_texts, tf_images, tf_texts, max_error=0.03):
    reference = np.asarray(pt_images) @ np.asarray(pt_texts).T
    converted = np.asarray(tf_images) @ np.asarray(tf_texts).T
    error = float(np.max(np.abs(reference - converted)))
    if not np.isfinite(error) or error > max_error:
        raise ValueError(f"Image/text similarities differ after conversion: max error={error:.6f}")
    return error


def sanity_check(model, preprocess, tokenizer, img_tflite, txt_tflite):
    """Run both exported encoders and compare them with PyTorch."""
    from PIL import Image, ImageDraw

    image_interpreter = validate_tflite(img_tflite, model_type="image")
    text_interpreter = validate_tflite(txt_tflite, model_type="text")
    images = []
    for size, color in [((320, 180), (200, 100, 50)), ((180, 320), (30, 80, 200))]:
        image = Image.new("RGB", size, color=color)
        ImageDraw.Draw(image).rectangle((size[0] // 4, size[1] // 4, size[0] * 3 // 4, size[1] * 3 // 4), fill=(40, 180, 60))
        images.append(image)
    texts = ["a warm orange photo", "a blue picture with a green rectangle", "кот на пляже", "семья на отдыхе"]
    pt_images, tf_images, pt_texts, tf_texts = [], [], [], []
    with torch.no_grad():
        for index, image in enumerate(images):
            tensor = preprocess(image).unsqueeze(0)
            reference = _normalize_embedding(model.encode_image(tensor).detach().cpu().numpy())
            converted = _run_tflite(image_interpreter, tensor.detach().cpu().numpy())
            cosine = _assert_embedding_parity(reference, converted, f"Image {index}")
            print(f"  Image {index} PyTorch/TFLite cosine: {cosine:.6f}")
            pt_images.append(reference)
            tf_images.append(converted)
        for text in texts:
            tokens = tokenizer([text])
            reference = _normalize_embedding(model.encode_text(tokens).detach().cpu().numpy())
            converted = _run_tflite(text_interpreter, tokens.detach().cpu().numpy())
            cosine = _assert_embedding_parity(reference, converted, f"Text {text!r}")
            print(f"  Text {text!r} PyTorch/TFLite cosine: {cosine:.6f}")
            pt_texts.append(reference)
            tf_texts.append(converted)
    error = _assert_similarity_parity(pt_images, pt_texts, tf_images, tf_texts)
    print(f"[✓] Both exported encoders ran successfully; maximum similarity error: {error:.6f}")


def copy_tokenizer_vocab():
    """Copy the BPE vocabulary file to assets."""
    import mobileclip
    import gzip

    tokenizer_dir = ASSETS_DIR / "tokenizer"
    dest = tokenizer_dir / "bpe_simple_vocab_16e6.txt.gz"
    if dest.exists():
        print(f"[✓] Tokenizer vocab already exists: {dest}")
        return

    mc_dir = Path(mobileclip.__file__).parent
    candidates = list(mc_dir.rglob("bpe_simple_vocab_16e6.txt.gz"))
    if not candidates:
        try:
            import open_clip
            oc_dir = Path(open_clip.__file__).parent
            candidates = list(oc_dir.rglob("bpe_simple_vocab_16e6.txt.gz"))
        except ImportError:
            pass

    if not candidates:
        raise FileNotFoundError("CLIP BPE vocabulary not found in mobileclip/open_clip packages")

    tokenizer_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(candidates[0], dest)
    print(f"[✓] Tokenizer vocab copied: {dest}")


def main():
    WORK_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    download_checkpoint()
    model, preprocess, tokenizer = load_model()

    img_onnx = export_image_encoder_onnx(model)
    txt_onnx = export_text_encoder_onnx(model)

    img_tflite = convert_onnx_to_tflite(img_onnx, "mobileclip_s0_image_encoder_fp16.tflite")
    txt_tflite = convert_onnx_to_tflite(txt_onnx, "mobileclip_s0_text_encoder_fp16.tflite")

    copy_tokenizer_vocab()

    sanity_check(model, preprocess, tokenizer, img_tflite, txt_tflite)

    print("\n" + "=" * 60)
    print("Conversion complete!")
    print(f"  Image encoder: {img_tflite}")
    print(f"  Text encoder:  {txt_tflite}")
    print(f"  Tokenizer:     {ASSETS_DIR / 'tokenizer' / 'bpe_simple_vocab_16e6.txt.gz'}")
    print("=" * 60)


if __name__ == "__main__":
    main()
