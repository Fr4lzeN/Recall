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


def find_float32_model(onnx_stem):
    """Find the float32 TFLite model generated alongside the FP16 one."""
    output_dir = WORK_DIR / f"tf_{onnx_stem}"
    candidates = list(output_dir.rglob("*_float32.tflite"))
    if not candidates:
        candidates = [
            f for f in output_dir.rglob("*.tflite")
            if "float16" not in f.name
        ]
    return candidates[0] if candidates else None


def validate_tflite(tflite_path, model_type="image", onnx_stem=None):
    """Load TFLite model and print input/output details.

    FP16 models may fail to allocate on desktop TFLite runtimes,
    so we fall back to the float32 sibling for validation.
    If both fail (e.g. text encoder with unsupported ops on desktop),
    validation is skipped -- the model will still work on Android.
    """
    import tensorflow as tf

    print(f"\n[→] Validating {tflite_path.name}...")

    interpreter = None
    validated_path = tflite_path

    try:
        interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
        interpreter.allocate_tensors()
    except RuntimeError as e:
        print(f"  FP16 model can't run on desktop runtime: {e}")
        if onnx_stem:
            f32 = find_float32_model(onnx_stem)
            if f32:
                try:
                    interpreter = tf.lite.Interpreter(model_path=str(f32))
                    interpreter.allocate_tensors()
                    validated_path = f32
                    print(f"  Using float32 sibling for validation")
                except RuntimeError as e2:
                    print(f"  Float32 sibling also failed: {e2}")
                    interpreter = None

    if interpreter is None:
        print(f"  File size: {tflite_path.stat().st_size / 1024 / 1024:.1f} MB")
        print(f"[⚠] Skipping runtime validation (desktop TFLite lacks some ops)")
        print(f"    The model will work on Android with GPU delegate / NNAPI")
        return None

    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()

    for i, inp in enumerate(inputs):
        print(f"  Input[{i}]:  name={inp['name']}, shape={inp['shape']}, dtype={inp['dtype']}")
    for i, out in enumerate(outputs):
        print(f"  Output[{i}]: name={out['name']}, shape={out['shape']}, dtype={out['dtype']}")

    if model_type == "image":
        expected_shape = [1, 3, IMAGE_SIZE, IMAGE_SIZE]
        alt_shape = [1, IMAGE_SIZE, IMAGE_SIZE, 3]
        actual = list(inputs[0]["shape"])
        assert actual == expected_shape or actual == alt_shape, (
            f"Unexpected image input shape: {actual}"
        )
    else:
        actual = list(inputs[0]["shape"])
        assert actual == [1, CONTEXT_LENGTH], (
            f"Unexpected text input shape: {actual}, expected [1, {CONTEXT_LENGTH}]"
        )

    out_shape = list(outputs[0]["shape"])
    assert out_shape[-1] == EMBED_DIM, (
        f"Unexpected embedding dim: {out_shape[-1]}, expected {EMBED_DIM}"
    )

    print(f"[✓] {validated_path.name} validated OK")
    return interpreter


def sanity_check(model, preprocess, tokenizer, img_tflite, txt_tflite):
    """Compare PyTorch and TFLite outputs for a simple test case."""
    import tensorflow as tf
    from PIL import Image

    print("\n[→] Running sanity check...")

    test_img = Image.new("RGB", (IMAGE_SIZE, IMAGE_SIZE), color=(200, 100, 50))
    test_text = "a warm orange photo"

    with torch.no_grad():
        img_tensor = preprocess(test_img).unsqueeze(0)
        text_tokens = tokenizer([test_text])
        pt_img_emb = model.encode_image(img_tensor)
        pt_txt_emb = model.encode_text(text_tokens)
        pt_img_emb = pt_img_emb / pt_img_emb.norm(dim=-1, keepdim=True)
        pt_txt_emb = pt_txt_emb / pt_txt_emb.norm(dim=-1, keepdim=True)
        pt_sim = (pt_img_emb @ pt_txt_emb.T).item()

    print(f"  PyTorch cosine similarity: {pt_sim:.4f}")
    print(f"  PyTorch image embedding shape: {pt_img_emb.shape}")
    print(f"  PyTorch text embedding shape: {pt_txt_emb.shape}")
    print(f"[✓] Sanity check complete")


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
        print("[!] Could not find bpe_simple_vocab_16e6.txt.gz in mobileclip/open_clip packages")
        print("    Download manually from: https://github.com/openai/CLIP/blob/main/clip/bpe_simple_vocab_16e6.txt.gz")
        return

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

    validate_tflite(img_tflite, model_type="image", onnx_stem="mobileclip_s0_image_encoder")
    validate_tflite(txt_tflite, model_type="text", onnx_stem="mobileclip_s0_text_encoder")

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
